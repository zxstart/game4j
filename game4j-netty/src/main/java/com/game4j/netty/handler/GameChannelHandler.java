package com.game4j.netty.handler;

import com.game4j.core.dispatcher.RouteDefinition;
import com.game4j.core.dispatcher.RouteDispatcher;
import com.game4j.core.dispatcher.RouteInvoker;
import com.game4j.netty.session.Session;
import com.game4j.protocol.GameMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class GameChannelHandler extends SimpleChannelInboundHandler<GameMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        ctx.channel().attr(AttributeKey.valueOf("session"))
                .set(new Session(ctx.channel()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                GameMessage msg) {

        RouteDefinition route =
                RouteDispatcher.get(msg.cmd());

        if (route == null) {
            return;
        }

        Session session =
                (Session) ctx.channel()
                        .attr(AttributeKey.valueOf("session"))
                        .get();

        RouteInvoker.invoke(route, session, msg.body());
    }

}
