package com.game4j.cluster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 集群配置类，用于配置集群的基本参数
 * 包括集群名称、端口配置、Gossip 间隔、RPC 超时等
 */
public class ClusterConfig {
    /**
     * 集群名称，默认为 "game4j-cluster"
     */
    private String clusterName = "game4j-cluster";
    
    /**
     * Hazelcast 集群通信端口，默认为 5701
     */
    private int hazelcastPort = 5701;
    
    /**
     * gRPC 服务端口，用于节点间 Gossip 通信，默认为 19090
     */
    private int grpcPort = 19090;
    
    /**
     * Gossip 协议执行间隔，默认为 2 秒
     */
    private Duration gossipInterval = Duration.ofSeconds(2);
    
    /**
     * RPC 调用超时时间，默认为 2 秒
     */
    private Duration rpcTimeout = Duration.ofSeconds(2);
    
    /**
     * 种子成员列表，用于 TCP/IP 方式加入集群
     */
    private List<String> seedMembers = new ArrayList<>();

    /** MEMBER preserves legacy deployments; CLIENT uses seed members as a control-plane address list. */
    private ClusterMode mode = ClusterMode.MEMBER;

    /**
     * 创建默认配置
     *
     * @return 默认集群配置
     */
    public static ClusterConfig defaults() {
        return new ClusterConfig();
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getHazelcastPort() {
        return hazelcastPort;
    }

    public void setHazelcastPort(int hazelcastPort) {
        this.hazelcastPort = hazelcastPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public Duration getGossipInterval() {
        return gossipInterval;
    }

    public void setGossipInterval(Duration gossipInterval) {
        this.gossipInterval = gossipInterval;
    }

    public Duration getRpcTimeout() {
        return rpcTimeout;
    }

    public void setRpcTimeout(Duration rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
    }

    public List<String> getSeedMembers() {
        return seedMembers;
    }

    public void setSeedMembers(List<String> seedMembers) {
        this.seedMembers = seedMembers;
    }

    public ClusterMode getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = ClusterMode.parse(mode);
    }

    public void setMode(ClusterMode mode) {
        this.mode = mode != null ? mode : ClusterMode.MEMBER;
    }

    public boolean isClientMode() {
        return mode == ClusterMode.CLIENT;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClusterConfig that)) {
            return false;
        }
        return hazelcastPort == that.hazelcastPort
                && grpcPort == that.grpcPort
                && Objects.equals(clusterName, that.clusterName)
                && Objects.equals(gossipInterval, that.gossipInterval)
                && Objects.equals(rpcTimeout, that.rpcTimeout)
                && mode == that.mode
                && Objects.equals(seedMembers, that.seedMembers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, hazelcastPort, grpcPort, gossipInterval, rpcTimeout, seedMembers, mode);
    }

    @Override
    public String toString() {
        return "ClusterConfig[clusterName=" + clusterName
                + ", hazelcastPort=" + hazelcastPort
                + ", grpcPort=" + grpcPort
                + ", gossipInterval=" + gossipInterval
                + ", rpcTimeout=" + rpcTimeout
                + ", mode=" + mode
                + ", seedMembers=" + seedMembers + "]";
    }
}
