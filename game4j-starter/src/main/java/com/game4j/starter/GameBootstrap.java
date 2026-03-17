package com.game4j.starter;

import com.game4j.cluster.ClusterConfig;
import com.game4j.cluster.ClusterManager;
import com.game4j.cluster.hazelcast.ClusterNode;
import com.game4j.netty.server.NettyServer;
import com.game4j.starter.config.ServerConfig;
import com.game4j.starter.initializer.ControllerInitializer;
import com.game4j.starter.initializer.PluginInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * 游戏服务器启动引导类
 * <p>
 * 负责整个游戏服务器的初始化和启动流程，包括：
 * 1. 加载插件
 * 2. 扫描并初始化控制器
 * 3. 启动 Netty 服务器
 * 4. 启动集群节点（Hazelcast + gRPC Gossip）
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
            startClusterNode();
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

    private void startClusterNode() {
        if (!config.isClusterEnabled()) {
            log.info("Cluster disabled by config");
            return;
        }

        String nodeId = config.getClusterNodeId();
        if (nodeId == null || nodeId.isBlank()) {
            nodeId = UUID.randomUUID().toString();
        }

        ClusterNode node = new ClusterNode(
                nodeId,
                config.getClusterNodeType(),
                config.getClusterHost(),
                config.getHazelcastPort(),
                config.getClusterGrpcPort()
        );

        ClusterConfig clusterConfig = ClusterConfig.defaults();
        clusterConfig.setClusterName(config.getClusterName());
        clusterConfig.setHazelcastPort(config.getHazelcastPort());
        clusterConfig.setGrpcPort(config.getClusterGrpcPort());
        clusterConfig.setSeedMembers(new ArrayList<>(config.getSeedMembers()));

        ClusterManager.init(node, clusterConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(ClusterManager::shutdown));
        log.info(
                "Cluster node started, nodeId={}, role={}, hazelcastPort={}, grpcPort={}, seeds={}",
                node.getNodeId(),
                node.getNodeType(),
                node.getPort(),
                node.getGrpcPort(),
                Objects.toString(clusterConfig.getSeedMembers())
        );
    }
}
