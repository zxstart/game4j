package com.game4j.cluster.registry;

import com.game4j.cluster.gossip.GossipNode;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {

    private static final Map<String, GossipNode> nodes = new ConcurrentHashMap<>();

    public static void register(GossipNode node) {

        nodes.put(node.getNodeId(), node);
    }

    public static GossipNode getNode(String nodeId) {

        return nodes.get(nodeId);
    }

    public static Collection<GossipNode> getNodes() {

        return nodes.values();
    }

    public static void remove(String nodeId) {

        nodes.remove(nodeId);
    }
}
