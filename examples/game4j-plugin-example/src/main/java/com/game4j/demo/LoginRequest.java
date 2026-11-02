package com.game4j.demo;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

/** 登录请求（cmd 1001）的 Protobuf 消息体。 */
public class LoginRequest {

    @Protobuf(order = 1)
    public String username;
}
