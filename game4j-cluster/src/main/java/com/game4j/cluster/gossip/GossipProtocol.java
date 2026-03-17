package com.game4j.cluster.gossip;

import java.util.List;

/**
 * Gossip 协议数据包
 * <p>
 * 用于在节点之间交换集群信息。
 * </p>
 */
public class GossipProtocol {
    /**
     * 消息来源节点ID
     */
    private String fromNode;

    /**
     * 包含的节点列表信息
     */
    private List<GossipNode> nodes;

    public GossipProtocol(String fromNode, List<GossipNode> nodes) {
        this.fromNode = fromNode;
        this.nodes = nodes;
    }

    public String getFromNode() {
        return fromNode;
    }

    public List<GossipNode> getNodes() {
        return nodes;
    }
}
