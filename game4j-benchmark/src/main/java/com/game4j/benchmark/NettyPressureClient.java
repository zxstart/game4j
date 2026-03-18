package com.game4j.benchmark;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 轻量 Netty client 压测器（第二阶段）。
 * <p>
 * 协议: Length(4) | Cmd(4) | Body(N), 其中 Length = 4 + Body.length。
 * 支持:
 * 1) 固定并发连接数
 * 2) 每连接限速发送（qpsPerConn）
 * 3) 每秒输出吞吐统计
 * 4) 可选 RTT 统计（要求服务端回包 body 前 8 字节原样返回请求ID）
 * </p>
 */
public final class NettyPressureClient {
    private NettyPressureClient() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);

        NioEventLoopGroup group = new NioEventLoopGroup(config.ioThreads);

        LongAdder txMsg = new LongAdder();
        LongAdder txBytes = new LongAdder();
        LongAdder rxMsg = new LongAdder();
        LongAdder errCnt = new LongAdder();

        LongAdder rttCount = new LongAdder();
        LongAdder rttTotalNs = new LongAdder();
        AtomicLong rttMaxNs = new AtomicLong(0);

        ConcurrentHashMap<Long, Long> sendTsNs = new ConcurrentHashMap<>(1 << 16);
        AtomicLong seq = new AtomicLong(1);

        List<Channel> channels = new ArrayList<>(config.clients);
        CountDownLatch connected = new CountDownLatch(config.clients);

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                // 解码后 frame 从 cmd 字段开始
                                .addLast(new LengthFieldBasedFrameDecoder(
                                        config.maxFrameBytes, 0, 4, 0, 4))
                                .addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf frame) {
                                        rxMsg.increment();
                                        if (!config.expectResponse || frame.readableBytes() < 12) {
                                            return;
                                        }
                                        int cmd = frame.readInt();
                                        if (cmd != config.cmd) {
                                            return;
                                        }
                                        long id = frame.readLong();
                                        Long startNs = sendTsNs.remove(id);
                                        if (startNs == null) {
                                            return;
                                        }
                                        long rttNs = System.nanoTime() - startNs;
                                        rttCount.increment();
                                        rttTotalNs.add(rttNs);
                                        rttMaxNs.accumulateAndGet(rttNs, Math::max);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        errCnt.increment();
                                        ctx.close();
                                    }
                                });
                    }
                });

        for (int i = 0; i < config.clients; i++) {
            int clientIndex = i;
            bootstrap.connect(config.host, config.port).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    errCnt.increment();
                    connected.countDown();
                    return;
                }
                Channel ch = future.channel();
                synchronized (channels) {
                    channels.add(ch);
                }
                connected.countDown();
                startSender(ch, clientIndex, config, txMsg, txBytes, errCnt, seq, sendTsNs);
            });
        }

        if (!connected.await(10, TimeUnit.SECONDS)) {
            System.err.println("connect timeout, only partial clients connected");
        }

        System.out.println("# Light benchmark mode: Throughput, ops/s");
        System.out.printf("# Warmup: %d iterations, %d s each%n",
                config.warmupIterations,
                config.iterationSeconds);
        System.out.printf("# Measurement: %d iterations, %d s each%n",
                config.measurementIterations,
                config.iterationSeconds);
        System.out.printf("# Connections: %d, qpsPerConn: %d, cmd: 0x%08X, body: %d bytes%n",
                config.clients,
                config.qpsPerConn,
                config.cmd,
                config.bodySize);

        long benchStartNs = System.nanoTime();

        for (int i = 1; i <= config.warmupIterations; i++) {
            double ops = runIterationOpsPerSecond(txMsg, config.iterationSeconds);
            System.out.printf("# Warmup Iteration %3d: %.3f ops/s%n", i, ops);
        }

        double[] scores = new double[config.measurementIterations];
        for (int i = 1; i <= config.measurementIterations; i++) {
            double ops = runIterationOpsPerSecond(txMsg, config.iterationSeconds);
            scores[i - 1] = ops;
            System.out.printf("Iteration %3d: %.3f ops/s%n", i, ops);
        }

        long benchElapsedNs = System.nanoTime() - benchStartNs;

        double avg = mean(scores);
        double min = min(scores);
        double max = max(scores);
        double stdev = stddev(scores, avg);
        double ci99_9 = scores.length <= 1 ? 0.0 : 3.291 * stdev / Math.sqrt(scores.length);

        System.out.printf("%nResult \"%s\":%n", config.resultName);
        System.out.printf("  %.3f ops/s +/- %.3f ops/s%n", avg, ci99_9);
        System.out.printf("  (min, avg, max) = (%.3f, %.3f, %.3f), stdev = %.3f%n", min, avg, max, stdev);
        System.out.printf("  CI (99.9%%): [%.3f, %.3f] (normal approximation)%n",
                avg - ci99_9, avg + ci99_9);
        System.out.printf("%nBenchmark                              Mode  Cnt         Score         Error  Units%n");
        System.out.printf("NettyPressureClient.handle           thrpt  %3d  %12.3f  +/- %10.3f  ops/s%n",
                scores.length, avg, ci99_9);
        System.out.printf("%n# Run complete. Total time: %s%n", formatDuration(benchElapsedNs));

        synchronized (channels) {
            for (Channel ch : channels) {
                ch.close().syncUninterruptibly();
            }
        }
        group.shutdownGracefully().syncUninterruptibly();

        long tx = txMsg.sum();
        long rx = rxMsg.sum();
        long err = errCnt.sum();
        long rc = rttCount.sum();
        double avgRttMs = rc == 0 ? 0.0 : (rttTotalNs.sum() / 1_000_000.0 / rc);
        double maxRttMs = rttMaxNs.get() / 1_000_000.0;
        System.out.println("=== FINAL ===");
        System.out.printf(
                "tx=%,d rx=%,d err=%,d avgRttMs=%.3f maxRttMs=%.3f%n",
                tx, rx, err, avgRttMs, maxRttMs
        );
    }

    private static double runIterationOpsPerSecond(LongAdder txMsg, int seconds) throws InterruptedException {
        long before = txMsg.sum();
        long startNs = System.nanoTime();
        Thread.sleep(seconds * 1000L);
        long elapsedNs = System.nanoTime() - startNs;
        long after = txMsg.sum();
        long delta = Math.max(0L, after - before);
        if (elapsedNs <= 0L) {
            return 0.0;
        }
        return delta * 1_000_000_000.0 / elapsedNs;
    }

    private static double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static double min(double[] values) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : values) {
            min = Math.min(min, v);
        }
        return values.length == 0 ? 0.0 : min;
    }

    private static double max(double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            max = Math.max(max, v);
        }
        return values.length == 0 ? 0.0 : max;
    }

    private static double stddev(double[] values, double mean) {
        if (values.length <= 1) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double d = v - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    private static String formatDuration(long nanos) {
        long totalSec = TimeUnit.NANOSECONDS.toSeconds(nanos);
        long mm = totalSec / 60;
        long ss = totalSec % 60;
        return String.format("%02d:%02d", mm, ss);
    }

    private static void startSender(Channel ch,
                                    int clientIndex,
                                    Config config,
                                    LongAdder txMsg,
                                    LongAdder txBytes,
                                    LongAdder errCnt,
                                    AtomicLong seq,
                                    ConcurrentHashMap<Long, Long> sendTsNs) {
        byte[] templateBody = buildBody(config.bodySize, clientIndex);

        if (config.qpsPerConn <= 0) {
            ch.eventLoop().execute(() -> {
                while (ch.isActive()) {
                    if (!ch.isWritable()) {
                        ch.flush();
                        break;
                    }
                    sendOne(ch, config.cmd, templateBody, config.expectResponse, seq, sendTsNs, txMsg, txBytes, errCnt);
                }
            });
            return;
        }

        long periodNs = Math.max(1L, 1_000_000_000L / config.qpsPerConn);
        ch.eventLoop().scheduleAtFixedRate(
                () -> sendOne(ch, config.cmd, templateBody, config.expectResponse, seq, sendTsNs, txMsg, txBytes, errCnt),
                0L,
                periodNs,
                TimeUnit.NANOSECONDS
        );
    }

    private static void sendOne(Channel ch,
                                int cmd,
                                byte[] templateBody,
                                boolean expectResponse,
                                AtomicLong seq,
                                ConcurrentHashMap<Long, Long> sendTsNs,
                                LongAdder txMsg,
                                LongAdder txBytes,
                                LongAdder errCnt) {
        if (!ch.isActive()) {
            return;
        }
        long reqId = 0L;
        try {
            byte[] body = templateBody;
            if (expectResponse) {
                reqId = seq.getAndIncrement();
                body = templateBody.clone();
                putLong(body, 0, reqId);
                sendTsNs.put(reqId, System.nanoTime());
            }

            int length = 4 + body.length;
            ByteBuf out = ch.alloc().buffer(4 + length);
            out.writeInt(length);
            out.writeInt(cmd);
            out.writeBytes(body);

            long finalReqId = reqId;
            ch.writeAndFlush(out).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    txMsg.increment();
                    txBytes.add(4L + length);
                    return;
                }
                errCnt.increment();
                if (expectResponse && finalReqId != 0L) {
                    sendTsNs.remove(finalReqId);
                }
            });
        } catch (Throwable e) {
            errCnt.increment();
            if (expectResponse && reqId != 0L) {
                sendTsNs.remove(reqId);
            }
        }
    }

    private static byte[] buildBody(int size, int clientIndex) {
        byte[] body = new byte[size];
        byte[] seed = ("c" + clientIndex + "-hello").getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < body.length; i++) {
            body[i] = seed[i % seed.length];
        }
        return body;
    }

    private static void putLong(byte[] arr, int offset, long value) {
        arr[offset] = (byte) (value >>> 56);
        arr[offset + 1] = (byte) (value >>> 48);
        arr[offset + 2] = (byte) (value >>> 40);
        arr[offset + 3] = (byte) (value >>> 32);
        arr[offset + 4] = (byte) (value >>> 24);
        arr[offset + 5] = (byte) (value >>> 16);
        arr[offset + 6] = (byte) (value >>> 8);
        arr[offset + 7] = (byte) value;
    }

    static final class Config {
        String host = "127.0.0.1";
        int port = 9000;
        int clients = 200;
        int bodySize = 64;
        int cmd = 0x00010001;
        long qpsPerConn = 100;
        int ioThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxFrameBytes = 1024 * 1024;
        boolean expectResponse = false;
        int warmupIterations = 3;
        int measurementIterations = 5;
        int iterationSeconds = 5;
        String resultName = "com.game4j.benchmark.NettyPressureClient.handle";

        static Config parse(String[] args) {
            Config c = new Config();
            for (String arg : args) {
                String[] kv = arg.split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                String key = kv[0].trim();
                String value = kv[1].trim();
                switch (key) {
                    case "host" -> c.host = value;
                    case "port" -> c.port = Integer.parseInt(value);
                    case "clients" -> c.clients = Integer.parseInt(value);
                    case "duration" -> {
                        // 兼容旧参数: duration=10 => 测量 1 轮, 每轮 10 秒
                        c.measurementIterations = 1;
                        c.iterationSeconds = Integer.parseInt(value);
                    }
                    case "body" -> c.bodySize = Integer.parseInt(value);
                    case "cmd" -> c.cmd = Integer.decode(value);
                    case "qpsPerConn" -> c.qpsPerConn = Long.parseLong(value);
                    case "ioThreads" -> c.ioThreads = Integer.parseInt(value);
                    case "expectResponse" -> c.expectResponse = Boolean.parseBoolean(value);
                    case "warmupIterations" -> c.warmupIterations = Integer.parseInt(value);
                    case "measurementIterations" -> c.measurementIterations = Integer.parseInt(value);
                    case "iterationSeconds" -> c.iterationSeconds = Integer.parseInt(value);
                    case "resultName" -> c.resultName = value;
                    default -> {
                    }
                }
            }
            if (c.expectResponse && c.bodySize < 8) {
                c.bodySize = 8;
            }
            c.warmupIterations = Math.max(0, c.warmupIterations);
            c.measurementIterations = Math.max(1, c.measurementIterations);
            c.iterationSeconds = Math.max(1, c.iterationSeconds);
            return c;
        }
    }
}
