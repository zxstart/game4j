package com.game4j.core.dispatcher;

import java.lang.invoke.MethodHandle;

/**
 * 路由调用器
 * <p>
 * 负责执行路由对应的处理方法。
 * 使用 {@link MethodHandle} 进行高性能调用。
 * </p>
 */
public class RouteInvoker {

    /**
     * 调用路由方法
     * 
     * @param route 路由定义
     * @param session 会话对象
     * @param body 请求体对象
     * @throws RuntimeException 如果调用过程中发生异常
     */
    public static void invoke(RouteDefinition route,
                              Object session,
                              Object body) {
        try {
            MethodHandle handle = route.getMethodHandle();
            handle.invokeExact(session, body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
