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

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接建立时创建 Session 并绑定到 Channel
        ctx.channel().attr(AttributeKey.valueOf("session"))
                .set(new Session(ctx.channel()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                GameMessage msg) {

        // 根据命令ID查找路由
        RouteDefinition route =
                RouteDispatcher.get(msg.cmd());

        if (route == null) {
            // TODO: 可以在这里添加日志或错误处理
            return;
        }

        // 获取 Session
        Session session =
                (Session) ctx.channel()
                        .attr(AttributeKey.valueOf("session"))
                        .get();

        // 调用路由处理方法
        RouteInvoker.invoke(route, session, msg.body());
    }

}
