package com.game4j.plugin.admin;

import com.game4j.core.config.ConfigFileLoader;
import lombok.Getter;

import java.util.Properties;

/**
 * 集群管理面板配置。
 */
public class ClusterDashboardConfig {
    private static final String KEY_PLUGIN_ENABLED = "game4j.plugin.cluster-dashboard.enabled";
    private static final String KEY_ADMIN_ENABLED = "game4j.admin.enabled";
    private static final String KEY_ADMIN_HOST = "game4j.admin.host";
    private static final String KEY_ADMIN_PORT = "game4j.admin.port";
    private static final String KEY_CLUSTER_ENABLED = "game4j.cluster.enabled";
    private static final String KEY_CLUSTER_NAME = "game4j.cluster.name";
    private static final String KEY_SERVER_PORT = "game4j.server.port";

    private boolean pluginEnabled = true;
    private boolean adminEnabled = true;
    @Getter
    private String adminHost = "127.0.0.1";
    @Getter
    private int adminPort = 18080;
    @Getter
    private boolean clusterEnabled = true;
    @Getter
    private String clusterName = "game4j-cluster";
    @Getter
    private int serverPort = 9000;

    public static ClusterDashboardConfig load() {
        ClusterDashboardConfig config = new ClusterDashboardConfig();
        Properties props = ConfigFileLoader.loadGame4jProperties();
        config.applyProperties(props);
        config.applySystemProperties();
        return config;
    }

    private void applyProperties(Properties props) {
        setIfPresent(props.getProperty(KEY_PLUGIN_ENABLED), s -> pluginEnabled = Boolean.parseBoolean(s));
        setIfPresent(props.getProperty(KEY_ADMIN_ENABLED), s -> adminEnabled = Boolean.parseBoolean(s));
        setIfPresent(props.getProperty(KEY_ADMIN_HOST), s -> adminHost = s);
        setIfPresent(props.getProperty(KEY_ADMIN_PORT), s -> adminPort = parseInt(s, adminPort));
        setIfPresent(props.getProperty(KEY_CLUSTER_ENABLED), s -> clusterEnabled = Boolean.parseBoolean(s));
        setIfPresent(props.getProperty(KEY_CLUSTER_NAME), s -> clusterName = s);
        setIfPresent(props.getProperty(KEY_SERVER_PORT), s -> serverPort = parseInt(s, serverPort));
    }

    private void applySystemProperties() {
        overrideIfPresent(KEY_PLUGIN_ENABLED, s -> pluginEnabled = Boolean.parseBoolean(s));
        overrideIfPresent(KEY_ADMIN_ENABLED, s -> adminEnabled = Boolean.parseBoolean(s));
        overrideIfPresent(KEY_ADMIN_HOST, s -> adminHost = s);
        overrideIfPresent(KEY_ADMIN_PORT, s -> adminPort = parseInt(s, adminPort));
        overrideIfPresent(KEY_CLUSTER_ENABLED, s -> clusterEnabled = Boolean.parseBoolean(s));
        overrideIfPresent(KEY_CLUSTER_NAME, s -> clusterName = s);
        overrideIfPresent(KEY_SERVER_PORT, s -> serverPort = parseInt(s, serverPort));
    }

    public boolean isEnabled() {
        return pluginEnabled && adminEnabled;
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private static void setIfPresent(String value, StringConsumer consumer) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            consumer.accept(trimmed);
        }
    }

    private static void overrideIfPresent(String key, StringConsumer consumer) {
        setIfPresent(System.getProperty(key), consumer);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
