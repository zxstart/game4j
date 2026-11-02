package com.game4j.demo;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * 用于验证 1001 -> 1002 Ping-Pong 的最小 TCP 客户端。
 */
public final class DemoClient {
    private static final int LOGIN_REQUEST_CMD = 1001;
    private static final int LOGIN_RESPONSE_CMD = 1002;
    private static final long REQUEST_INTERVAL_MILLIS = 5_000L;
    private static final Codec<LoginRequest> REQUEST_CODEC = ProtobufProxy.create(LoginRequest.class);
    private static final Codec<LoginResponse> RESPONSE_CODEC = ProtobufProxy.create(LoginResponse.class);

    private DemoClient() {
    }

    public static void main(String[] args) throws Exception {
        LoginRequest request = new LoginRequest();
        request.username = args.length > 0 ? args[0] : "demo-player";
        byte[] requestBody = REQUEST_CODEC.encode(request);

        try (Socket socket = new Socket("127.0.0.1", 19000);
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             DataInputStream input = new DataInputStream(socket.getInputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                sendRequest(output, requestBody);
                LoginResponse response = readResponse(input);
                System.out.printf("登录响应: cmd=%d, code=%d%n", LOGIN_RESPONSE_CMD, response.code);
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
            }
        }
    }

    private static void sendRequest(DataOutputStream output, byte[] requestBody) throws Exception {
        output.writeInt(Integer.BYTES + requestBody.length);
        output.writeInt(LOGIN_REQUEST_CMD);
        output.write(requestBody);
        output.flush();
    }

    private static LoginResponse readResponse(DataInputStream input) throws Exception {
        int length = input.readInt();
        if (length < Integer.BYTES) {
            throw new IllegalStateException("Invalid response length: " + length);
        }
        int cmd = input.readInt();
        byte[] responseBody = input.readNBytes(length - Integer.BYTES);
        if (responseBody.length != length - Integer.BYTES) {
            throw new IllegalStateException("Response body ended unexpectedly");
        }
        if (cmd != LOGIN_RESPONSE_CMD) {
            throw new IllegalStateException("Expected cmd 1002 but received " + cmd);
        }

        return RESPONSE_CODEC.decode(responseBody);
    }
}
