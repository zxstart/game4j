package com.game4j.starter.initializer;

import com.game4j.plugin.GamePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

        List<GamePlugin> plugins = new ArrayList<>();
        for (GamePlugin plugin : loader) {
            plugins.add(plugin);
        }
        plugins.sort(Comparator.comparingInt(GamePlugin::order));

        for (GamePlugin plugin : plugins) {
            String pluginName = plugin.name();
            try {
                if (!plugin.supports()) {
                    log.info("Plugin skipped : {}", pluginName);
                    continue;
                }
                log.info("Plugin loading : {}", pluginName);
                plugin.onLoad();
                plugin.onEnable();
                Runtime.getRuntime().addShutdownHook(new Thread(plugin::onDisable));
                log.info("Plugin enabled : {}", pluginName);
            } catch (Exception ex) {
                log.error("Plugin init failed : {}", pluginName, ex);
            }
        }
    }
}
