package com.game4j.netty.handler;

import com.game4j.core.dispatcher.RouteDefinition;
import com.game4j.core.dispatcher.RouteDispatcher;
import com.game4j.core.dispatcher.RouteInvoker;
import com.game4j.netty.session.Session;
import com.game4j.protocol.GameMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * 游戏通道处理器
 * <p>
 * 处理 Netty 的 I/O 事件。
 * 负责会话管理和消息分发。
 * </p>
 */
public class GameChannelHandler extends SimpleChannelInboundHandler<GameMessage> {
    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接建立时创建 Session 并绑定到 Channel
        ctx.channel().attr(SESSION_KEY).set(new Session(ctx.channel()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) {
        long startNano = System.nanoTime();
        long traceId = GameInvokeLogPrinter.newTraceId();

        // 根据命令ID查找路由
        RouteDefinition route = RouteDispatcher.get(msg.cmd());
        if (route == null) {
            GameInvokeLogPrinter.logRouteNotFound(ctx, msg, startNano, traceId);
            return;
        }
        // 获取 Session
        Session session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            session = new Session(ctx.channel());
            ctx.channel().attr(SESSION_KEY).set(session);
        }
        // 调用路由处理方法
        RouteInvoker.InvocationResult result = RouteInvoker.invoke(route, session, msg.body());
        GameInvokeLogPrinter.logRouteInvoke(ctx, route, result, startNano, traceId);
    }
}
