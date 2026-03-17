package com.game4j.netty.codec;

import com.game4j.protocol.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 协议解码器
 * <p>
 * 处理网络包的拆包和解码。
 * 协议格式: Length(4 bytes) | Cmd(4 bytes) | Body(N bytes)
 * Length = 4 (Cmd size) + Body size
 * </p>
 */
public class ProtoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in,
                          List<Object> out) {

        // 检查是否有足够的字节读取长度字段 (4 bytes)
        if (in.readableBytes() < 4) {
            return;
        }

        // 标记当前读索引，以便数据不足时回滚
        in.markReaderIndex();

        // 读取消息长度
        int length = in.readInt();

        // 检查剩余字节是否足够读取完整的消息体
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        // 读取命令ID
        int cmd = in.readInt();

        // 读取消息体
        byte[] body = new byte[length - 4];
        in.readBytes(body);

        // 将解码后的消息传递给下一个处理器
        out.add(new GameMessage(cmd, body));
    }

}
