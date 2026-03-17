package com.game4j.core.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏上下文环境
 * <p>
 * 用于管理全局的单例 Bean 实例，提供依赖注入和获取功能。
 * 类似于 Spring 的 ApplicationContext，但更轻量级。
 * </p>
 */
public class GameContext {

    /**
     * 存储所有注册的 Bean 实例
     */
    private static final Map<Class<?>, Object> BEANS = new ConcurrentHashMap<>();

    /**
     * 注册 Bean
     * <p>
     * 将一个实例注册到上下文中。
     * </p>
     * 
     * @param clazz Bean 的类型
     * @param instance Bean 的实例
     * @throws IllegalArgumentException 如果参数为空或类型不匹配
     * @throws IllegalStateException 如果该类型的 Bean 已经注册
     */
    public static void registerBean(Class<?> clazz, Object instance) {
        if (clazz == null) {
            throw new IllegalArgumentException("Register bean class cannot be null");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Register bean instance cannot be null");
        }
        if (!clazz.isInstance(instance)) {
            throw new IllegalArgumentException(
                    "Instance [" + instance.getClass().getName() + "] is not assignable to [" + clazz.getName() + "]"
            );
        }

        Object existing = BEANS.putIfAbsent(clazz, instance);
        if (existing != null) {
            // 策略：发现重复注册时抛出异常，防止意外覆盖
            throw new IllegalStateException(
                    "Bean of type [" + clazz.getName() + "] is already registered: " + existing
            );
        }

        // 可选：如果希望支持接口查找，可以在此处扫描 instance 实现的所有接口并注册映射
        // registerInterfaces(clazz, instance);
    }

    /**
     * 获取 Bean
     * <p>
     * 根据类型获取已注册的 Bean 实例。
     * </p>
     * 
     * @param clazz Bean 的类型
     * @param <T> Bean 的泛型类型
     * @return Bean 实例
     * @throws IllegalArgumentException 如果 clazz 为 null
     * @throws IllegalStateException 如果找不到对应类型的 Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Get bean class cannot be null");
        }
        T bean = (T) BEANS.get(clazz);
        if (bean == null) {
            throw new IllegalStateException("No bean found for type: " + clazz.getName());
        }
        return bean;
    }

    /**
     * 尝试获取 Bean，如果没有则返回 null
     * <p>
     * 安全的获取方式，不会抛出异常。
     * </p>
     * 
     * @param clazz Bean 的类型
     * @param <T> Bean 的泛型类型
     * @return Bean 实例，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanNullable(Class<T> clazz) {
        if (clazz == null) return null;
        return (T) BEANS.get(clazz);
    }

    /**
     * 检查是否包含某个 Bean
     * 
     * @param clazz Bean 的类型
     * @return 如果包含则返回 true，否则返回 false
     */
    public static boolean containsBean(Class<?> clazz) {
        return clazz != null && BEANS.containsKey(clazz);
    }

    /**
     * 获取所有已注册的 Bean
     * <p>
     * 返回一个只读的 Map 视图，防止外部修改内部状态。
     * </p>
     * 
     * @return 包含所有 Bean 的 Map
     */
    public static Map<Class<?>, Object> getAllBeans() {
        return Collections.unmodifiableMap(BEANS);
    }

    /**
     * 清空容器
     * <p>
     * 清除所有注册的 Bean，通常用于测试或热重载场景。
     * </p>
     */
    public static void clear() {
        BEANS.clear();
    }
}
