package com.game4j.netty.session;

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
}
