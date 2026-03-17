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

/**
 * 控制器初始化器
 * <p>
 * 负责扫描指定包下的控制器类，实例化并注册到 GameContext 中。
 * 同时解析控制器方法中的路由信息并注册到 RouteDispatcher。
 * </p>
 */
public class ControllerInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(ControllerInitializer.class);

    /**
     * 初始化控制器
     *
     * @param basePackage 扫描的基础包名
     */
    public static void init(String basePackage) {
        try {
            List<Class<?>> controllers = ControllerScanner.scan(basePackage);
            for (Class<?> clazz : controllers) {
                Object controller = clazz.getDeclaredConstructor().newInstance();
                // 注册到 Spring-like 上下文
                GameContext.registerBean(clazz, controller);
                // 注册路由
                registerRoutes(controller);
                log.info("Controller loaded : {}", clazz.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 注册控制器中的所有路由方法
     *
     * @param controller 控制器实例
     */
    private static void registerRoutes(Object controller) {
        Method[] methods = controller.getClass().getDeclaredMethods();
        for (Method method : methods) {
            RouteDefinition def = RouteBuilder.build(controller, method);
            if (def != null) {
                RouteDispatcher.register(def);
                log.info("Route register cmd={}", def.getCmd());
            }
        }
    }
}
