package com.game4j.core.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameContext {

    private static final Map<Class<?>, Object> BEANS = new ConcurrentHashMap<>();

    /**
     * 注册 Bean
     * 优化点 1: 解耦泛型 T，允许 Class<?> 和 Object 传入，解决调用时的类型推断问题
     * 优化点 2: 增加参数校验
     * 优化点 3: 增加重复注册检测
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
     * 保持泛型，方便调用者直接使用，无需强转
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
     * 尝试获取 Bean，如果没有则返回 null (避免抛异常)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanNullable(Class<T> clazz) {
        if (clazz == null) return null;
        return (T) BEANS.get(clazz);
    }

    /**
     * 检查是否包含某个 Bean
     */
    public static boolean containsBean(Class<?> clazz) {
        return clazz != null && BEANS.containsKey(clazz);
    }

    /**
     * 获取所有已注册的 Bean (返回只读视图，防止外部修改)
     */
    public static Map<Class<?>, Object> getAllBeans() {
        return Collections.unmodifiableMap(BEANS);
    }

    // 清空容器 (用于测试或热重载)
    public static void clear() {
        BEANS.clear();
    }
}
