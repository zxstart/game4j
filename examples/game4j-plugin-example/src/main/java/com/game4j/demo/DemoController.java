package com.game4j.demo;

import com.game4j.core.annotation.G4JController;
import com.game4j.core.annotation.G4JRequestBody;
import com.game4j.core.annotation.G4JRoute;
import com.game4j.netty.session.Session;

/**
 * 登录请求的 Ping-Pong 示例：1001 请求，1002 响应。
 */
@G4JController
public class DemoController {

    @G4JRoute(cmd = 1001)
    public void login(Session session, @G4JRequestBody LoginRequest req) {
        System.out.println("收到登录请求: " + req.username);

        // 业务处理...
        LoginResponse resp = new LoginResponse();
        resp.code = 200;

        // 直接发送对象和指令号，底层会自动完成 Protobuf 序列化和封包。
        session.send(1002, resp);
    }
}
