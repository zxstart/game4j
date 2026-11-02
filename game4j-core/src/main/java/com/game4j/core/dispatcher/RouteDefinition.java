package com.game4j.core.dispatcher;

import com.baidu.bjf.remoting.protobuf.Codec;

import java.lang.invoke.MethodHandle;

/**
 * 路由定义
 * <p>
 * 封装了一个路由的所有必要信息，包括命令ID、处理方法句柄、控制器实例和请求体类型。
 * </p>
 */
public class RouteDefinition {
    /**
     * 命令ID
     */
    private int cmd;

    /**
     * 方法句柄，用于高性能调用
     */
    private MethodHandle methodHandle;

    /**
     * 控制器实例
     */
    private Object controller;

    /**
     * 请求体类型 (Protobuf类)
     */
    private Class<?> bodyType;

    /**
     * 请求体 Codec (Protobuf)
     */
    private Codec<?> codec;

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public MethodHandle getMethodHandle() {
        return methodHandle;
    }

    public void setMethodHandle(MethodHandle methodHandle) {
        this.methodHandle = methodHandle;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Class<?> getBodyType() {
        return bodyType;
    }

    public void setBodyType(Class<?> bodyType) {
        this.bodyType = bodyType;
    }

    public Codec<?> getCodec() {
        return codec;
    }

    public void setCodec(Codec<?> codec) {
        this.codec = codec;
    }
}
