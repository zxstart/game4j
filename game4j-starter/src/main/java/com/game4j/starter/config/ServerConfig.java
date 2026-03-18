package com.game4j.starter.config;

import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 服务器配置类
 * <p>
 * 存储服务器的启动配置，如端口号、扫描包路径等。
 * </p>
 */
public class ServerConfig {
    private static final String DEFAULT_CONFIG_FILE = "game4j.properties";
    private static final String KEY_SERVER_PORT = "game4j.server.port";
    private static final String KEY_SERVER_SCAN_PACKAGE = "game4j.server.scan-package";
    private static final String KEY_CLUSTER_ENABLED = "game4j.cluster.enabled";
    private static final String KEY_CLUSTER_NAME = "game4j.cluster.name";
    private static final String KEY_CLUSTER_NODE_ID = "game4j.cluster.node-id";
    private static final String KEY_CLUSTER_NODE_TYPE = "game4j.cluster.node-type";
    private static final String KEY_CLUSTER_HOST = "game4j.cluster.host";
    private static final String KEY_CLUSTER_HAZELCAST_PORT = "game4j.cluster.hazelcast-port";
    private static final String KEY_CLUSTER_GRPC_PORT = "game4j.cluster.grpc-port";
    private static final String KEY_CLUSTER_SEED_MEMBERS = "game4j.cluster.seed-members";
    private static final String KEY_ADMIN_ENABLED = "game4j.admin.enabled";
    private static final String KEY_ADMIN_HOST = "game4j.admin.host";
    private static final String KEY_ADMIN_PORT = "game4j.admin.port";

    /**
     * 服务器端口，默认 9000
     */
    private int port = 9000;

    /**
     * 扫描包路径，用于自动发现控制器
     */
    private String scanPackage;

    /**
     * 是否启用集群
     */
    private boolean clusterEnabled = true;

    /**
     * 集群名称
     */
    private String clusterName = "game4j-cluster";

    /**
     * 集群节点ID，未显式设置时在启动时自动生成
     */
    private String clusterNodeId;

    /**
     * 集群节点角色
     */
    private String clusterNodeType = "game-server";

    /**
     * 本机对外地址
     */
    private String clusterHost = "127.0.0.1";

    /**
     * Hazelcast 集群端口
     */
    private int hazelcastPort = 5701;

    /**
     * Gossip gRPC 端口
     */
    private int clusterGrpcPort = 19090;

    /**
     * 集群种子地址列表，格式 host:port（Hazelcast 端口）
     */
    private List<String> seedMembers = new ArrayList<>();

    /**
     * 是否启用管理控制台（HTTP）
     */
    @Setter
    private boolean adminEnabled = true;

    /**
     * 管理控制台监听地址
     */
    @Setter
    private String adminHost = "127.0.0.1";

    /**
     * 管理控制台监听端口
     */
    @Setter
    private int adminPort = 18080;

    public ServerConfig(String scanPackage) {
        this.scanPackage = scanPackage;
    }

    /**
     * 从类路径配置文件加载，并支持 JVM -D 参数覆盖。
     */
    public static ServerConfig load(String fallbackScanPackage) {
        ServerConfig config = new ServerConfig(fallbackScanPackage);
        Properties props = new Properties();

        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // 读取失败时使用默认值
        }

        config.applyProperties(props);
        config.applySystemProperties();

        if (config.scanPackage == null || config.scanPackage.isBlank()) {
            config.scanPackage = fallbackScanPackage;
        }
        return config;
    }

    public int getPort() {
        return port;
    }

    public String getScanPackage() {
        return scanPackage;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    public void setClusterNodeId(String clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    public String getClusterNodeType() {
        return clusterNodeType;
    }

    public void setClusterNodeType(String clusterNodeType) {
        this.clusterNodeType = clusterNodeType;
    }

    public String getClusterHost() {
        return clusterHost;
    }

    public void setClusterHost(String clusterHost) {
        this.clusterHost = clusterHost;
    }

    public int getHazelcastPort() {
        return hazelcastPort;
    }

    public void setHazelcastPort(int hazelcastPort) {
        this.hazelcastPort = hazelcastPort;
    }

    public int getClusterGrpcPort() {
        return clusterGrpcPort;
    }

    public void setClusterGrpcPort(int clusterGrpcPort) {
        this.clusterGrpcPort = clusterGrpcPort;
    }

    public List<String> getSeedMembers() {
        return seedMembers;
    }

    public void setSeedMembers(List<String> seedMembers) {
        this.seedMembers = seedMembers != null ? seedMembers : new ArrayList<>();
    }

    public boolean isAdminEnabled() {
        return adminEnabled;
    }

    public String getAdminHost() {
        return adminHost;
    }

    public int getAdminPort() {
        return adminPort;
    }

    private void applyProperties(Properties props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        setIfPresent(props.getProperty(KEY_SERVER_SCAN_PACKAGE), this::setScanPackage);
        setIfPresent(props.getProperty(KEY_SERVER_PORT), s -> this.setPort(parseInt(s, this.port)));
        setIfPresent(props.getProperty(KEY_CLUSTER_ENABLED), s -> this.setClusterEnabled(Boolean.parseBoolean(s)));
        setIfPresent(props.getProperty(KEY_CLUSTER_NAME), this::setClusterName);
        setIfPresent(props.getProperty(KEY_CLUSTER_NODE_ID), this::setClusterNodeId);
        setIfPresent(props.getProperty(KEY_CLUSTER_NODE_TYPE), this::setClusterNodeType);
        setIfPresent(props.getProperty(KEY_CLUSTER_HOST), this::setClusterHost);
        setIfPresent(props.getProperty(KEY_CLUSTER_HAZELCAST_PORT), s -> this.setHazelcastPort(parseInt(s, this.hazelcastPort)));
        setIfPresent(props.getProperty(KEY_CLUSTER_GRPC_PORT), s -> this.setClusterGrpcPort(parseInt(s, this.clusterGrpcPort)));
        setIfPresent(props.getProperty(KEY_CLUSTER_SEED_MEMBERS), s -> this.setSeedMembers(parseCsv(s)));
        setIfPresent(props.getProperty(KEY_ADMIN_ENABLED), s -> this.setAdminEnabled(Boolean.parseBoolean(s)));
        setIfPresent(props.getProperty(KEY_ADMIN_HOST), this::setAdminHost);
        setIfPresent(props.getProperty(KEY_ADMIN_PORT), s -> this.setAdminPort(parseInt(s, this.adminPort)));
    }

    private void applySystemProperties() {
        overrideIfPresent(KEY_SERVER_SCAN_PACKAGE, this::setScanPackage);
        overrideIfPresent(KEY_SERVER_PORT, s -> this.setPort(parseInt(s, this.port)));
        overrideIfPresent(KEY_CLUSTER_ENABLED, s -> this.setClusterEnabled(Boolean.parseBoolean(s)));
        overrideIfPresent(KEY_CLUSTER_NAME, this::setClusterName);
        overrideIfPresent(KEY_CLUSTER_NODE_ID, this::setClusterNodeId);
        overrideIfPresent(KEY_CLUSTER_NODE_TYPE, this::setClusterNodeType);
        overrideIfPresent(KEY_CLUSTER_HOST, this::setClusterHost);
        overrideIfPresent(KEY_CLUSTER_HAZELCAST_PORT, s -> this.setHazelcastPort(parseInt(s, this.hazelcastPort)));
        overrideIfPresent(KEY_CLUSTER_GRPC_PORT, s -> this.setClusterGrpcPort(parseInt(s, this.clusterGrpcPort)));
        overrideIfPresent(KEY_CLUSTER_SEED_MEMBERS, s -> this.setSeedMembers(parseCsv(s)));
        overrideIfPresent(KEY_ADMIN_ENABLED, s -> this.setAdminEnabled(Boolean.parseBoolean(s)));
        overrideIfPresent(KEY_ADMIN_HOST, this::setAdminHost);
        overrideIfPresent(KEY_ADMIN_PORT, s -> this.setAdminPort(parseInt(s, this.adminPort)));
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

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> parseCsv(String csv) {
        List<String> items = new ArrayList<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(items::add);
        return items;
    }

    public void setScanPackage(String scanPackage) {
        this.scanPackage = scanPackage;
    }
}
