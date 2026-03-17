package com.game4j.core.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class MethodHandleFactory {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static MethodHandle create(Object controller, Method method) {
        try {
            MethodHandle handle = LOOKUP.unreflect(method);
            return handle.bindTo(controller);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
