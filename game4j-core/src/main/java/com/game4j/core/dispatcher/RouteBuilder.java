package com.game4j.core.dispatcher;

import com.game4j.core.annotation.GameRequestBody;
import com.game4j.core.annotation.GameRoute;
import com.game4j.core.reflection.MethodHandleFactory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteBuilder {
    public static RouteDefinition build(Object controller, Method method) {
        GameRoute route = method.getAnnotation(GameRoute.class);
        if (route == null) {
            return null;
        }
        RouteDefinition def = new RouteDefinition();
        def.setCmd(route.cmd());
        def.setController(controller);
        MethodHandle handle =
                MethodHandleFactory.create(controller, method);
        def.setMethodHandle(handle);
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(GameRequestBody.class)) {
                def.setBodyType(parameter.getType());
            }
        }
        return def;
    }
}
