package com.game4j.netty.server;

import com.game4j.netty.codec.ProtoDecoder;
import com.game4j.netty.codec.ProtoEncoder;
import com.game4j.netty.handler.GameChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端
 * <p>
 * 负责启动 Netty 服务，配置 ChannelPipeline。
 * </p>
 */
@Slf4j
public class NettyServer implements AutoCloseable {
    private final Object lifecycleLock = new Object();
    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;

    /**
     * 启动服务器
     *
     * @param port 监听端口
     * @throws Exception 启动异常
     */
    public void start(int port) throws Exception {
        synchronized (lifecycleLock) {
            if (isRunning()) {
                log.warn("Netty server already started, currentPort={}", localPort());
                return;
            }
        }

        EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new ProtoDecoder())
                                .addLast(new ProtoEncoder())
                                .addLast(new GameChannelHandler());
                    }

                });
        try {
            Channel channel = bootstrap.bind(port).sync().channel();
            synchronized (lifecycleLock) {
                this.bossGroup = boss;
                this.workerGroup = worker;
                this.serverChannel = channel;
            }
            log.info("Game server started, port={}", port);
        } catch (Exception ex) {
            shutdownQuietly(worker, "worker");
            shutdownQuietly(boss, "boss");
            throw ex;
        }
    }

    public void stop() {
        EventLoopGroup boss;
        EventLoopGroup worker;
        Channel channel;
        synchronized (lifecycleLock) {
            boss = this.bossGroup;
            worker = this.workerGroup;
            channel = this.serverChannel;
            this.serverChannel = null;
            this.bossGroup = null;
            this.workerGroup = null;
        }

        if (channel == null && boss == null && worker == null) {
            return;
        }

        if (channel != null) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Exception ex) {
                log.warn("Close server channel failed", ex);
            }
        }

        shutdownGracefully(worker, "worker");
        shutdownGracefully(boss, "boss");
        log.info("Game server stopped");
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        Channel channel = serverChannel;
        return channel != null && channel.isActive();
    }

    public int localPort() {
        Channel channel = serverChannel;
        if (channel == null || channel.localAddress() == null) {
            return -1;
        }
        return ((java.net.InetSocketAddress) channel.localAddress()).getPort();
    }

    private void shutdownGracefully(EventLoopGroup group, String groupName) {
        if (group == null) {
            return;
        }
        try {
            group.shutdownGracefully(0, 10, TimeUnit.SECONDS).syncUninterruptibly();
        } catch (Exception ex) {
            log.warn("Shutdown {} event loop failed", groupName, ex);
        }
    }

    private void shutdownQuietly(EventLoopGroup group, String groupName) {
        if (group == null) {
            return;
        }
        try {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
        } catch (Exception ex) {
            log.debug("Shutdown {} event loop during rollback failed", groupName, ex);
        }
    }
}
