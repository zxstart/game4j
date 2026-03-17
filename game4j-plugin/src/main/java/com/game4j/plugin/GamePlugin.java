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
     * 当插件被加载时调用
     */
    void onLoad();
}
