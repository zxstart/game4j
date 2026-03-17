package com.game4j.cluster.gossip;

import lombok.Data;

/**
 * Gossip 节点
 * <p>
 * 表示集群中的一个节点，包含节点ID、主机地址、端口和心跳时间等信息。
 * </p>
 */
@Data
public class GossipNode {
    /**
     * 节点唯一ID
     */
    private String nodeId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private int port;

    /**
     * 最后一次心跳时间戳
     */
    private long lastHeartbeat;

    /**
     * 节点是否存活
     */
    private boolean alive = true;

    public GossipNode(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /**
     * 更新心跳时间
     */
    public void heartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
