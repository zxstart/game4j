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

/**
 * 游戏服务器启动引导类
 * <p>
 * 负责整个游戏服务器的初始化和启动流程，包括：
 * 1. 加载插件
 * 2. 扫描并初始化控制器
 * 3. 启动 Netty 服务器
 * 4. 启动 Gossip 节点和心跳任务
 * </p>
 */
public class GameBootstrap {
    private static final Logger log = LoggerFactory.getLogger(GameBootstrap.class);
    private final ServerConfig config;

    public GameBootstrap(String scanPackage) {
        this.config = new ServerConfig(scanPackage);
    }

    public GameBootstrap(ServerConfig config) {
        this.config = config;
    }

    /**
     * 启动服务器
     */
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
        // 示例：启动 Gossip 节点
        // 实际生产中，这些配置应该来自配置文件
        GossipNode node = new GossipNode(
                UUID.randomUUID().toString(),
                "127.0.0.1",
                9000
        );
        GossipManager.init(node);
        HeartbeatTask.start();
    }
}
