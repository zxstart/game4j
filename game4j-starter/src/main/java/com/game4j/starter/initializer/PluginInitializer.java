package com.game4j.starter.initializer;

import com.game4j.plugin.GamePlugin;
import com.game4j.plugin.PluginStartupPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.EnumSet;
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

    private final List<GamePlugin> plugins = new ArrayList<>();
    private final List<GamePlugin> enabledPlugins = new ArrayList<>();
    private final EnumSet<PluginStartupPhase> completedPhases = EnumSet.noneOf(PluginStartupPhase.class);
    private boolean discovered;

    /** Performs lightweight SPI discovery once; plugins are not enabled here. */
    public synchronized void discover() {
        if (discovered) {
            return;
        }
        ServiceLoader<GamePlugin> loader =
                ServiceLoader.load(GamePlugin.class);

        for (GamePlugin plugin : loader) {
            plugins.add(plugin);
        }
        plugins.sort(Comparator.comparingInt(GamePlugin::order));
        discovered = true;
        log.info("Discovered {} plugins", plugins.size());
    }

    /** Enables only plugins assigned to the supplied phase. */
    public synchronized void enable(PluginStartupPhase phase) {
        discover();
        if (!completedPhases.add(phase)) {
            return;
        }
        for (GamePlugin plugin : plugins) {
            if (plugin.startupPhase() != phase) {
                continue;
            }
            String pluginName = plugin.name();
            try {
                if (!plugin.supports()) {
                    log.info("Plugin skipped : {}", pluginName);
                    continue;
                }
                log.info("Plugin loading : {}", pluginName);
                plugin.onLoad();
                plugin.onEnable();
                enabledPlugins.add(plugin);
                log.info("Plugin enabled : {}", pluginName);
            } catch (Exception ex) {
                log.error("Plugin init failed : {}", pluginName, ex);
            }
        }
    }

    /** Disables enabled plugins in reverse startup order. */
    public synchronized void stop() {
        for (int index = enabledPlugins.size() - 1; index >= 0; index--) {
            GamePlugin plugin = enabledPlugins.get(index);
            try {
                plugin.onDisable();
            } catch (Exception ex) {
                log.warn("Plugin stop failed : {}", plugin.name(), ex);
            }
        }
        enabledPlugins.clear();
        completedPhases.clear();
    }
}
