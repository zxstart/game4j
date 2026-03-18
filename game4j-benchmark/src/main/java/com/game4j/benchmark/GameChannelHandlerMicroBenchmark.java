package com.game4j.benchmark;

import com.game4j.core.dispatcher.RouteDefinition;
import com.game4j.core.dispatcher.RouteDispatcher;
import com.game4j.netty.handler.GameChannelHandler;
import com.game4j.netty.session.Session;
import com.game4j.protocol.GameMessage;
import io.netty.channel.embedded.EmbeddedChannel;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

/**
 * 仅针对 GameChannelHandler 的微基准（不含真实网络 I/O）。
 * <p>
 * 通过 EmbeddedChannel 直接向 handler 写入 GameMessage，评估核心分发路径性能。
 * </p>
 */
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(1)
@State(Scope.Benchmark)
public class GameChannelHandlerMicroBenchmark {

    private static final int CMD_HIT = 0x00010001;
    private static final int CMD_MISS = 0x0001FFFF;

    @Param({"32", "256", "1024"})
    public int payloadSize;

    private EmbeddedChannel hotChannel;
    private GameMessage hitMessage;
    private GameMessage missMessage;

    @Setup(Level.Trial)
    public void setup() throws Throwable {
        RouteDefinition route = new RouteDefinition();
        route.setCmd(CMD_HIT);
        route.setController(new DummyController());
        route.setMethodHandle(buildHandle(route.getController().getClass()));
        route.setCodec(null);
        RouteDispatcher.register(route);

        byte[] payload = new byte[payloadSize];
        this.hitMessage = new GameMessage(CMD_HIT, payload);
        this.missMessage = new GameMessage(CMD_MISS, payload);

        this.hotChannel = new EmbeddedChannel(new GameChannelHandler());
        // 显式触发 active，预先绑定 session，用于测热路径。
        this.hotChannel.pipeline().fireChannelActive();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (hotChannel != null) {
            hotChannel.finishAndReleaseAll();
        }
    }

    @Benchmark
    public boolean routeHitHotSession() {
        return hotChannel.writeInbound(hitMessage);
    }

    @Benchmark
    public boolean routeMissHotSession() {
        return hotChannel.writeInbound(missMessage);
    }

    @Benchmark
    public boolean routeHitFirstPacket() {
        // 每次都创建新 channel，模拟首包 session 创建路径。
        EmbeddedChannel coldChannel = new EmbeddedChannel(new GameChannelHandler());
        try {
            return coldChannel.writeInbound(hitMessage);
        } finally {
            coldChannel.finishAndReleaseAll();
        }
    }

    private static MethodHandle buildHandle(Class<?> controllerType) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle handle = MethodHandles.lookup().findVirtual(
                controllerType,
                "handle",
                MethodType.methodType(Object.class, Session.class, byte[].class)
        );
        return handle.bindTo(new DummyController());
    }

    public static final class DummyController {
        public Object handle(Session session, byte[] body) {
            int len = body == null ? 0 : body.length;
            // 轻量业务计算，避免被完全优化为空路径。
            return len ^ 0x5A5A5A5A;
        }
    }
}
