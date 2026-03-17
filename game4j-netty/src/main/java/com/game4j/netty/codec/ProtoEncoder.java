package com.game4j.netty.codec;

import com.game4j.protocol.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtoEncoder extends MessageToByteEncoder<GameMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          GameMessage msg,
                          ByteBuf out) {

        byte[] body = msg.body();

        out.writeInt(body.length + 4);
        out.writeInt(msg.cmd());
        out.writeBytes(body);
    }

}
