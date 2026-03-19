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

import java.time.LocalDateTime;
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
    private final Object lifecycleLock = new Object();
    private volatile NettyServer nettyServer;
    private volatile Thread shutdownHook;
    private volatile boolean started;
    private volatile String clusterNodeSummary = "Cluster disabled by config";

    public GameBootstrap(String scanPackage) {
        this.config = ServerConfig.load(scanPackage);
    }

    public GameBootstrap(ServerConfig config) {
        this.config = config;
    }

    /**
     * 启动服务器
     */
    public void start() {
        long startNanos = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();
        synchronized (lifecycleLock) {
            if (started) {
                log.warn("Game server already started");
                return;
            }
        }
        try {
//            log.info("Game Server Starting...");
            initPlugin();
            initController();
            startNetty();
            startClusterNode();
            registerShutdownHook();
            synchronized (lifecycleLock) {
                started = true;
            }
            LocalDateTime endTime = LocalDateTime.now();
            double consumeSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0d;
            BannerPrinter.print(
                    startTime,
                    endTime,
                    consumeSeconds,
                    clusterNodeSummary,
                    buildNettyAddress(),
                    buildDashboardAddress()
            );
//            log.info("Game Server Started");
        } catch (Exception e) {
            stopInternal();
            throw new RuntimeException(e);
        }
    }

    /**
     * 手动停止服务器，通常用于测试或嵌入式运行场景。
     */
    public void stop() {
        removeShutdownHook();
        stopInternal();
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
        this.nettyServer = server;
    }

    private void startClusterNode() {
        if (!config.isClusterEnabled()) {
            clusterNodeSummary = "Cluster disabled by config";
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

        try {
            ClusterManager.init(node, clusterConfig);
        } catch (NoClassDefFoundError err) {
            if (isGrpcClassMissing(err)) {
                throw new IllegalStateException(
                        "Cluster startup failed: missing gRPC runtime classes in classpath, " +
                                "please ensure grpc-api/grpc-stub/grpc-netty-shaded are included.",
                        err
                );
            }
            if (isHazelcastClassMissing(err)) {
                throw new IllegalStateException(
                        "Cluster startup failed: missing Hazelcast runtime classes in classpath, " +
                                "please ensure com.hazelcast:hazelcast is available " +
                                "or disable cluster with -Dgame4j.cluster.enabled=false.",
                        err
                );
            }
            throw err;
        }
        log.info(
                "Cluster node started, nodeId={}, role={}, hazelcastPort={}, grpcPort={}, seeds={}",
                node.getNodeId(),
                node.getNodeType(),
                node.getPort(),
                node.getGrpcPort(),
                Objects.toString(clusterConfig.getSeedMembers())
        );
        clusterNodeSummary = String.format(
                "Cluster node started, nodeId=%s, role=%s, hazelcastPort=%d, grpcPort=%d, seeds=%s",
                node.getNodeId(),
                node.getNodeType(),
                node.getPort(),
                node.getGrpcPort(),
                Objects.toString(clusterConfig.getSeedMembers())
        );
    }

    private void registerShutdownHook() {
        synchronized (lifecycleLock) {
            if (shutdownHook != null) {
                return;
            }
            shutdownHook = new Thread(this::stopInternal, "game4j-bootstrap-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    private void removeShutdownHook() {
        synchronized (lifecycleLock) {
            if (shutdownHook == null) {
                return;
            }
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM 正在退出时移除 hook 会抛异常，属于可接受行为。
            } finally {
                shutdownHook = null;
            }
        }
    }

    private void stopInternal() {
        synchronized (lifecycleLock) {
            if (!started && nettyServer == null && !config.isClusterEnabled()) {
                return;
            }
            started = false;
        }

        if (config.isClusterEnabled()) {
            try {
                ClusterManager.shutdown();
            } catch (Exception ex) {
                log.warn("Shutdown cluster failed", ex);
            }
        }

        NettyServer server = nettyServer;
        nettyServer = null;
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                log.warn("Shutdown netty server failed", ex);
            }
        }
    }

    private boolean isGrpcClassMissing(NoClassDefFoundError err) {
        String msg = err.getMessage();
        return msg != null && msg.startsWith("io/grpc/");
    }

    private boolean isHazelcastClassMissing(NoClassDefFoundError err) {
        String msg = err.getMessage();
        return msg != null && msg.startsWith("com/hazelcast/");
    }

    private String buildNettyAddress() {
        NettyServer server = nettyServer;
        int port = server != null ? server.localPort() : config.getPort();
        String host = config.getClusterHost();
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }
        return "tcp://" + host + ":" + port;
    }

    private String buildDashboardAddress() {
        if (!config.isAdminEnabled()) {
            return "disabled";
        }
        return "http://" + config.getAdminHost() + ":" + config.getAdminPort() + "/admin/cluster";
    }
}
