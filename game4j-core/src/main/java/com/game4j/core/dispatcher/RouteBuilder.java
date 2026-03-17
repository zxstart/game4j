package com.game4j.core.dispatcher;

import com.game4j.core.annotation.G4JRequestBody;
import com.game4j.core.annotation.G4JRoute;
import com.game4j.core.reflection.MethodHandleFactory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 路由构建器
 * <p>
 * 用于解析控制器方法，构建 {@link RouteDefinition} 对象。
 * 它会分析方法上的 {@link G4JRoute} 注解和参数上的 {@link G4JRequestBody} 注解。
 * </p>
 */
public class RouteBuilder {

    /**
     * 构建路由定义
     * 
     * @param controller 控制器对象
     * @param method 控制器方法
     * @return 路由定义对象，如果方法没有 @G4JRoute 注解则返回 null
     */
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
