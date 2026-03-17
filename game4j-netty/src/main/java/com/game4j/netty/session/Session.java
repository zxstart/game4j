package com.game4j.netty.session;

import io.netty.channel.Channel;

public class Session {

    private final Channel channel;

    public Session(Channel channel) {
        this.channel = channel;
    }

    public void send(Object msg) {

        channel.writeAndFlush(msg);
    }

}
