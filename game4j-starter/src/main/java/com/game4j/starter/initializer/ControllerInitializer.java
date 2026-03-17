package com.game4j.starter.initializer;

import com.game4j.core.context.GameContext;
import com.game4j.core.dispatcher.RouteBuilder;
import com.game4j.core.dispatcher.RouteDefinition;
import com.game4j.core.dispatcher.RouteDispatcher;
import com.game4j.core.scanner.ControllerScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class ControllerInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(ControllerInitializer.class);

    public static void init(String basePackage) {
        try {
            List<Class<?>> controllers =
                    ControllerScanner.scan(basePackage);
            for (Class<?> clazz : controllers) {
                Object controller =
                        clazz.getDeclaredConstructor().newInstance();
                GameContext.registerBean(clazz, controller);
                registerRoutes(controller);
                log.info("Controller loaded : {}", clazz.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerRoutes(Object controller) {
        Method[] methods = controller.getClass().getDeclaredMethods();
        for (Method method : methods) {
            RouteDefinition def = RouteBuilder.build(controller, method);
            if (def != null) {
                RouteDispatcher.register(def);
                log.info("Route register cmd={}",
                        def.getCmd());
            }
        }
    }
}
