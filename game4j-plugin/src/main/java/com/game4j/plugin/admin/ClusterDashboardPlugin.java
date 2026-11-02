package com.game4j.plugin.admin;

import com.game4j.plugin.GamePlugin;
import com.game4j.plugin.PluginStartupPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群管理面板插件（SPI）。
 */
public class ClusterDashboardPlugin implements GamePlugin {
    private static final Logger log = LoggerFactory.getLogger(ClusterDashboardPlugin.class);
    private static final ClusterAdminHttpServer SERVER = new ClusterAdminHttpServer();

    @Override
    public String name() {
        return "cluster-dashboard";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public PluginStartupPhase startupPhase() {
        return PluginStartupPhase.POST_LISTENING;
    }

    @Override
    public boolean supports() {
        return ClusterDashboardConfig.load().isEnabled();
    }

    @Override
    public void onEnable() {
        ClusterDashboardConfig config = ClusterDashboardConfig.load();
        SERVER.start(config);
    }

    @Override
    public void onDisable() {
        try {
            SERVER.stop();
        } catch (Exception ex) {
            log.warn("Stop cluster dashboard failed", ex);
        }
    }
}
