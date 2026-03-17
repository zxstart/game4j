package com.game4j.netty.codec;

import com.game4j.protocol.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * length | cmd | body
 */
public class ProtoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in,
                          List<Object> out) {

        if (in.readableBytes() < 8) {
            return;
        }

        in.markReaderIndex();

        int length = in.readInt();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        int cmd = in.readInt();

        byte[] body = new byte[length - 4];

        in.readBytes(body);

        out.add(new GameMessage(cmd, body));
    }

}
