package com.game4j.core.dispatcher;

import com.game4j.core.annotation.G4JRequestBody;
import com.game4j.core.annotation.G4JRoute;
import com.game4j.core.reflection.MethodHandleFactory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteBuilder {
    public static RouteDefinition build(Object controller, Method method) {
        G4JRoute route = method.getAnnotation(G4JRoute.class);
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
            if (parameter.isAnnotationPresent(G4JRequestBody.class)) {
                def.setBodyType(parameter.getType());
            }
        }
        return def;
    }
}
