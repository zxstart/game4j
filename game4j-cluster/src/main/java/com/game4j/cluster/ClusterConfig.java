package com.game4j.cluster;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 集群配置类，用于配置集群的基本参数
 * 包括集群名称、端口配置、Gossip 间隔、RPC 超时等
 */
@Data
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

    /**
     * 创建默认配置
     *
     * @return 默认集群配置
     */
    public static ClusterConfig defaults() {
        return new ClusterConfig();
    }
}
