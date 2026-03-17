package com.game4j.netty.codec;

import com.game4j.protocol.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 协议编码器
 * <p>
 * 将 GameMessage 对象编码为字节流发送给客户端。
 * </p>
 */
public class ProtoEncoder extends MessageToByteEncoder<GameMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          GameMessage msg,
                          ByteBuf out) {

        byte[] body = msg.body();

        // 写入长度: body长度 + 4字节的cmd
        out.writeInt(body.length + 4);
        // 写入命令ID
        out.writeInt(msg.cmd());
        // 写入消息体
        out.writeBytes(body);
    }

}
