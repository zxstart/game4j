package com.game4j.core.dispatcher;

import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;

/**
 * 路由定义
 * <p>
 * 封装了一个路由的所有必要信息，包括命令ID、处理方法句柄、控制器实例和请求体类型。
 * </p>
 */
@Setter
@Getter
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
}
