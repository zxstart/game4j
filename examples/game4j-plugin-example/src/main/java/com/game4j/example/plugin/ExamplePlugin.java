package com.game4j.example.plugin;

import com.game4j.plugin.GamePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一个最小可运行的独立 Game4j 插件。
 * <p>
 * 打包后的 JAR 需要与应用及 {@code game4j-starter} 处于同一运行时 classpath，
 * Starter 会通过 Java SPI 自动发现并调用该插件。
 * </p>
 */
public final class ExamplePlugin implements GamePlugin {
    private static final Logger log = LoggerFactory.getLogger(ExamplePlugin.class);

    @Override
    public String name() {
        return "example-plugin";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void onLoad() {
        log.info("Example plugin loaded");
    }

    @Override
    public void onEnable() {
        log.info("Example plugin enabled");
    }

    @Override
    public void onDisable() {
        log.info("Example plugin disabled");
    }
}
