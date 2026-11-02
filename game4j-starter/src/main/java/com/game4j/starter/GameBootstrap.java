package com.game4j.starter;

import com.game4j.cluster.ClusterConfig;
import com.game4j.cluster.ClusterManager;
import com.game4j.cluster.hazelcast.ClusterNode;
import com.game4j.netty.server.NettyServer;
import com.game4j.starter.config.ServerConfig;
import com.game4j.starter.initializer.ControllerInitializer;
import com.game4j.starter.initializer.PluginInitializer;
import com.game4j.starter.spring.EnableGame4j;
import com.game4j.plugin.PluginStartupPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    private final PluginInitializer pluginInitializer = new PluginInitializer();
    private final StartupMetrics startupMetrics = new StartupMetrics();
    private final ExecutorService startupExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("game4j-startup-", 0).factory());
    private volatile NettyServer nettyServer;
    private volatile Thread shutdownHook;
    private volatile Future<?> asynchronousStartup;
    private volatile boolean started;
    private volatile boolean clusterStartAttempted;
    private volatile StartupState startupState = StartupState.NEW;
    private volatile String clusterNodeSummary = "Cluster disabled by config";

    public GameBootstrap(String scanPackage) {
        this.config = ServerConfig.load(scanPackage);
    }

    public GameBootstrap(ServerConfig config) {
        this.config = config;
    }

    /**
     * Starts Game4j for an {@link EnableGame4j}-annotated application class.
     * <p>
     * This is the standalone Java entry point. If no scan package is declared
     * on the annotation, the package containing {@code applicationClass} is
     * used as the controller scan root.
     * </p>
     *
     * @param applicationClass class annotated with {@link EnableGame4j}
     * @return the started bootstrap instance
     */
    public static GameBootstrap run(Class<?> applicationClass) {
        Objects.requireNonNull(applicationClass, "applicationClass must not be null");
        EnableGame4j enableGame4j = applicationClass.getAnnotation(EnableGame4j.class);
        if (enableGame4j == null) {
            throw new IllegalArgumentException(
                    applicationClass.getName() + " must be annotated with @EnableGame4j"
            );
        }

        String scanPackage = enableGame4j.scanBasePackage();
        if (scanPackage == null || scanPackage.isBlank()) {
            scanPackage = applicationClass.getPackageName();
        }
        if (scanPackage == null || scanPackage.isBlank()) {
            throw new IllegalArgumentException(
                    "@EnableGame4j requires the application class to be in a named package " +
                            "or to declare scanBasePackage"
            );
        }

        GameBootstrap bootstrap = new GameBootstrap(scanPackage.trim());
        bootstrap.start();
        return bootstrap;
    }

    /**
     * 启动服务器
     */
    public void start() {
        LocalDateTime startTime = LocalDateTime.now();
        synchronized (lifecycleLock) {
            if (started) {
                log.warn("Game server already started");
                return;
            }
            if (startupState == StartupState.STARTING || startupState == StartupState.CLUSTER_JOINING) {
                log.warn("Game server startup already in progress");
                return;
            }
            startupState = StartupState.STARTING;
        }
        try {
            pluginInitializer.discover();
            initCriticalPlugins();
            startupMetrics.completed("critical-plugins");
            initController();
            startupMetrics.completed("controllers");
            startNetty();
            startupMetrics.completed("netty-listening");
            registerShutdownHook();
            synchronized (lifecycleLock) {
                started = true;
                startupState = StartupState.LISTENING;
                clusterNodeSummary = config.isClusterEnabled()
                        ? "Cluster joining asynchronously"
                        : "Cluster disabled by config";
                asynchronousStartup = startupExecutor.submit(this::continueStartup);
            }
            LocalDateTime endTime = LocalDateTime.now();
            double consumeSeconds = startupMetrics.elapsedMillis() / 1_000.0d;
            BannerPrinter.print(
                    startTime,
                    endTime,
                    consumeSeconds,
                    clusterNodeSummary,
                    buildNettyAddress(),
                    buildDashboardAddress()
            );
            log.info("Game server listening, clusterEnabled={}, timingsMs={}", config.isClusterEnabled(),
                    startupMetrics.snapshotMillis());
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

    /** Waits for full readiness. A false result means the node remains LISTENING or DEGRADED. */
    public boolean awaitReady(Duration timeout) {
        Future<?> future = asynchronousStartup;
        if (future == null) {
            return startupState == StartupState.READY;
        }
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
        return startupState == StartupState.READY;
    }

    public StartupState getStartupState() {
        return startupState;
    }

    public StartupMetrics getStartupMetrics() {
        return startupMetrics;
    }

    private void initCriticalPlugins() {
        log.info("Loading critical plugins...");
        pluginInitializer.enable(PluginStartupPhase.CRITICAL);
    }

    private void initController() {
        log.info("Scanning controllers...");
        ControllerInitializer.init(
                config.getScanPackage(),
                config.getControllerIndexMode()
        );
    }

    private void continueStartup() {
        try {
            if (!started || startupState == StartupState.STOPPING || startupState == StartupState.STOPPED) {
                return;
            }
            if (config.isClusterEnabled()) {
                startupState = StartupState.CLUSTER_JOINING;
                clusterNodeSummary = "Cluster joining asynchronously";
                startClusterNode();
                if (!ClusterManager.awaitReady(config.getClusterReadyTimeout())) {
                    throw new IllegalStateException("Cluster was not ready within " + config.getClusterReadyTimeout());
                }
                startupMetrics.completed("cluster-ready");
            }
            pluginInitializer.enable(PluginStartupPhase.POST_LISTENING);
            startupMetrics.completed("post-listening-plugins");
            if (config.isClusterEnabled()) {
                pluginInitializer.enable(PluginStartupPhase.CLUSTER_READY);
                startupMetrics.completed("cluster-ready-plugins");
            }
            startupState = StartupState.READY;
            log.info("Game server ready; timingsMs={}", startupMetrics.snapshotMillis());
        } catch (Throwable ex) {
            startupState = StartupState.DEGRADED;
            clusterNodeSummary = "Cluster unavailable: " + ex.getClass().getSimpleName();
            log.error("Background cluster startup failed; game server remains locally listening", ex);
        }
    }

    private void startNetty() throws Exception {
//        log.info("Starting Netty...");
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
        clusterConfig.setMode(config.getClusterMode());

        try {
            clusterStartAttempted = true;
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
            if (startupState == StartupState.STOPPED || startupState == StartupState.STOPPING) {
                return;
            }
            startupState = StartupState.STOPPING;
            started = false;
        }

        if (clusterStartAttempted) {
            try {
                ClusterManager.shutdown();
            } catch (Exception ex) {
                log.warn("Shutdown cluster failed", ex);
            }
        }

        pluginInitializer.stop();

        NettyServer server = nettyServer;
        nettyServer = null;
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                log.warn("Shutdown netty server failed", ex);
            }
        }
        Future<?> future = asynchronousStartup;
        if (future != null) {
            future.cancel(true);
            asynchronousStartup = null;
        }
        startupExecutor.shutdownNow();
        startupState = StartupState.STOPPED;
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
