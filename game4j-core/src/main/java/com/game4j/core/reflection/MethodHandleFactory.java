package com.game4j.core.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * MethodHandle 工厂
 * <p>
 * 用于创建 {@link MethodHandle} 实例，提供比 Java 反射更快的执行速度。
 * </p>
 */
public class MethodHandleFactory {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * 创建绑定到特定控制器实例的方法句柄
     * 
     * @param controller 控制器实例
     * @param method 目标方法
     * @return 绑定后的方法句柄
     * @throws RuntimeException 如果无法访问该方法
     */
    public static MethodHandle create(Object controller, Method method) {
        try {
            MethodHandle handle = LOOKUP.unreflect(method);
            return handle.bindTo(controller);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
