package com.game4j.starter;

import com.game4j.cluster.gossip.GossipManager;
import com.game4j.cluster.gossip.GossipNode;
import com.game4j.cluster.heartbeat.HeartbeatTask;
import com.game4j.netty.server.NettyServer;
import com.game4j.starter.config.ServerConfig;
import com.game4j.starter.initializer.ControllerInitializer;
import com.game4j.starter.initializer.PluginInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class GameBootstrap {
    private static final Logger log = LoggerFactory.getLogger(GameBootstrap.class);
    private final ServerConfig config;

    public GameBootstrap(String scanPackage) {
        this.config = new ServerConfig(scanPackage);
    }

    public GameBootstrap(ServerConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            log.info("Game Server Starting...");
            initPlugin();
            initController();
            startNetty();
            startGossipNode();
            log.info("Game Server Started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initPlugin() {
        log.info("Loading plugins...");
        PluginInitializer.init();
    }

    private void initController() {
        log.info("Scanning controllers...");
        ControllerInitializer.init(
                config.getScanPackage()
        );
    }

    private void startNetty() throws Exception {
        log.info("Starting Netty...");
        NettyServer server = new NettyServer();
        server.start(config.getPort());
    }

    private void startGossipNode() {
        GossipNode node = new GossipNode(
                UUID.randomUUID().toString(),
                "127.0.0.1",
                9000
        );
        GossipManager.init(node);
        HeartbeatTask.start();
    }
}
