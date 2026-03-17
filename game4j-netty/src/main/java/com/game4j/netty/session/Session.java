package com.game4j.netty.session;

import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.game4j.protocol.GameMessage;
import io.netty.channel.Channel;

/**
 * 玩家会话
 * <p>
 * 封装了 Netty 的 Channel，用于向客户端发送消息。
 * </p>
 */
public class Session {
    private final Channel channel;

    public Session(Channel channel) {
        this.channel = channel;
    }

    /**
     * 发送消息
     * 
     * @param msg 消息对象
     */
    public void send(Object msg) {
        channel.writeAndFlush(msg);
    }

    /**
     * 发送 Protobuf 消息
     *
     * @param cmd 协议号
     * @param msg Protobuf 对象
     */
    @SuppressWarnings("unchecked")
    public void send(int cmd, Object msg) {
        try {
            // JProtobuf 内部自带缓存，可以直接 create
            com.baidu.bjf.remoting.protobuf.Codec codec = ProtobufProxy.create(msg.getClass());
            byte[] bytes = codec.encode(msg);
            channel.writeAndFlush(new GameMessage(cmd, bytes));
        } catch (Exception e) {
            throw new RuntimeException("Encode protobuf error", e);
        }
    }
}
