package com.game4j.cluster.gossip;

import lombok.Data;

@Data
public class GossipNode {

    private String nodeId;

    private String host;

    private int port;

    private long lastHeartbeat;

    private boolean alive = true;

    public GossipNode(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void heartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
