package com.game4j.starter.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器配置类
 * <p>
 * 存储服务器的启动配置，如端口号、扫描包路径等。
 * </p>
 */
public class ServerConfig {

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

    public ServerConfig(String scanPackage) {
        this.scanPackage = scanPackage;
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
}
