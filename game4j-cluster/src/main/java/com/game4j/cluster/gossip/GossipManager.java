package com.game4j.cluster.gossip;

import com.game4j.cluster.registry.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GossipManager {

    private static final Logger log =
            LoggerFactory.getLogger(GossipManager.class);

    private static GossipNode localNode;

    private static final Random random = new Random();

    public static void init(GossipNode node) {

        localNode = node;

        NodeRegistry.register(node);

        log.info("Local node init : {}", node.getNodeId());
    }

    public static GossipNode getLocalNode() {
        return localNode;
    }

    public static void join(GossipNode node) {

        NodeRegistry.register(node);

        log.info("Node join cluster : {}", node.getNodeId());
    }

    public static GossipNode randomNode() {

        List<GossipNode> list =
                new ArrayList<>(NodeRegistry.getNodes());

        if (list.isEmpty()) {
            return null;
        }

        return list.get(random.nextInt(list.size()));
    }

    public static void merge(List<GossipNode> nodes) {

        for (GossipNode node : nodes) {

            GossipNode exist =
                    NodeRegistry.getNode(node.getNodeId());

            if (exist == null) {

                NodeRegistry.register(node);

                log.info("Discover new node : {}",
                        node.getNodeId());

            } else {

                exist.heartbeat();
            }
        }
    }
}
