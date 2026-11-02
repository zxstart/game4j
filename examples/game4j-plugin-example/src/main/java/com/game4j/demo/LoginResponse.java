package com.game4j.demo;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

/** 登录响应（cmd 1002）的 Protobuf 消息体。 */
public class LoginResponse {

    @Protobuf(order = 1)
    public int code;
}
