package com.game4j.starter.admin;

import com.game4j.plugin.GamePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群管理面板插件（SPI）。
 */
public class ClusterDashboardPlugin implements GamePlugin {
    private static final Logger log = LoggerFactory.getLogger(ClusterDashboardPlugin.class);
    private static final ClusterAdminHttpServer SERVER = new ClusterAdminHttpServer();

    @Override
    public void onLoad() {
        ClusterDashboardConfig config = ClusterDashboardConfig.load();
        if (!config.isEnabled()) {
            log.info("Cluster dashboard plugin disabled by config");
            return;
        }
        SERVER.start(config);
        Runtime.getRuntime().addShutdownHook(new Thread(SERVER::stop));
    }
}
