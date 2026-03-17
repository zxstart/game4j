package com.game4j.starter.initializer;

import com.game4j.plugin.GamePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * 插件初始化器
 * <p>
 * 使用 Java SPI 机制加载并初始化实现了 GamePlugin 接口的插件。
 * </p>
 */
public class PluginInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(PluginInitializer.class);

    /**
     * 加载并初始化所有插件
     */
    public static void init() {

        ServiceLoader<GamePlugin> loader =
                ServiceLoader.load(GamePlugin.class);

        for (GamePlugin plugin : loader) {
            log.info("Plugin loading : {}",
                    plugin.getClass().getName());
            plugin.onLoad();
        }

    }
}
