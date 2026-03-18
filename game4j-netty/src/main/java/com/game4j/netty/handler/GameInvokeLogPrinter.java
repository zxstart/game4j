package com.game4j.netty.handler;

import com.game4j.core.dispatcher.RouteDefinition;
import com.game4j.core.dispatcher.RouteInvoker;
import com.game4j.protocol.GameMessage;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由调用日志打印器
 * <p>
 * 统一封装请求调用日志格式，避免污染 I/O 主流程。
 * </p>
 */
public final class GameInvokeLogPrinter {
    private static final Logger log = LoggerFactory.getLogger(GameChannelHandler.class);
    private static final AtomicLong TRACE_ID_SEQ = new AtomicLong(System.currentTimeMillis() * 1000L);
    private static final String SERVER_NAME = "xxx";
    private static final String VERSION = "ioGameVersion";

    private GameInvokeLogPrinter() {
    }

    public static long newTraceId() {
        return TRACE_ID_SEQ.incrementAndGet();
    }

    public static void logRouteNotFound(ChannelHandlerContext ctx, GameMessage msg, long startNano, long traceId) {
        log.warn("""
                ┏━━━━━ Debug. [RouteNotFound.here] ━━━━━ [cmd:{}] ━━━━━ [{}逻辑服 - id:[{}]
                ┣ 参数: bodyLength : {}
                ┣ 响应: route not found
                ┣ 时间: {} ms (业务方法总耗时)
                ┗━━━━━ [{}] ━━━━━ [线程:{}] ━━━━━━━ [traceId:{}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """,
                formatCmd(msg.cmd()),
                SERVER_NAME,
                ctx.channel().id().asShortText(),
                msg.body() == null ? 0 : msg.body().length,
                costMs(startNano),
                VERSION,
                Thread.currentThread().getName(),
                traceId);
    }

    public static void logRouteInvoke(ChannelHandlerContext ctx,
                                      RouteDefinition route,
                                      RouteInvoker.InvocationResult result,
                                      long startNano,
                                      long traceId) {
        log.info("""
                ┏━━━━━ Debug. [({}.java).{}] ━━━━━ [cmd:{}] ━━━━━ [{}逻辑服 - id:[{}]
                ┣ userId: {}
                ┣ 参数: message : {}
                ┣ 响应: {}
                ┣ 时间: {} ms (业务方法总耗时)
                ┗━━━━━ [{}] ━━━━━ [线程:{}] ━━━━━━━ [traceId:{}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """,
                route.getController().getClass().getSimpleName(),
                "here",
                formatCmd(route.getCmd()),
                SERVER_NAME,
                ctx.channel().id().asShortText(),
                1,
                safeToString(result.request()),
                safeToString(result.response()),
                costMs(startNano),
                VERSION,
                Thread.currentThread().getName(),
                traceId);
    }

    private static String formatCmd(int cmd) {
        int module = (cmd >>> 16) & 0xFFFF;
        int action = cmd & 0xFFFF;
        return module + "-" + action + " " + cmd;
    }

    private static long costMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    private static String safeToString(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return String.valueOf(value);
        } catch (Exception e) {
            return value.getClass().getName();
        }
    }
}
