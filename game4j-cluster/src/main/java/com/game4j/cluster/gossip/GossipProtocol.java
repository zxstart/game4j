package com.game4j.cluster.gossip;

import java.util.List;

public class GossipProtocol {

    private String fromNode;

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
