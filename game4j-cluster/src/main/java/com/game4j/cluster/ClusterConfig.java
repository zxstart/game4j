package com.game4j.cluster;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
public class ClusterConfig {
    private String clusterName = "game4j-cluster";
    private int hazelcastPort = 5701;
    private int grpcPort = 19090;
    private Duration gossipInterval = Duration.ofSeconds(2);
    private Duration rpcTimeout = Duration.ofSeconds(2);
    private List<String> seedMembers = new ArrayList<>();

    public static ClusterConfig defaults() {
        return new ClusterConfig();
    }
}
