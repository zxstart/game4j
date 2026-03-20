package com.game4j.plugin;

/**
 * 游戏插件接口
 * <p>
 * 所有插件都必须实现此接口。
 * 插件通过 Java SPI 机制加载。
 * </p>
 */
public interface GamePlugin {
    /**
     * 插件名称（用于日志和诊断）。
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * 插件加载顺序，值越小越先执行。
     */
    default int order() {
        return 0;
    }

    /**
     * 插件是否启用（可按配置动态关闭）。
     */
    default boolean supports() {
        return true;
    }

    /**
     * 当插件被加载时调用。
     */
    default void onLoad() {
    }

    /**
     * 当插件被启用时调用。
     */
    default void onEnable() {
    }

    /**
     * 当插件被停用时调用。
     */
    default void onDisable() {
    }
}
