package com.game4j.starter.initializer;

import com.game4j.plugin.GamePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class PluginInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(PluginInitializer.class);

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
