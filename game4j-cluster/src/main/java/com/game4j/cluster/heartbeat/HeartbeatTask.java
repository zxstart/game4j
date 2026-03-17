package com.game4j.cluster.heartbeat;

import com.game4j.cluster.gossip.GossipManager;
import com.game4j.cluster.gossip.GossipNode;
import com.game4j.cluster.registry.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatTask {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatTask.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final long TIMEOUT = 10000;

    public static void start() {
        scheduler.scheduleAtFixedRate(() -> {
            checkNodes();
            gossip();
        }, 3, 3, TimeUnit.SECONDS);

    }

    private static void checkNodes() {
        long now = System.currentTimeMillis();
        for (GossipNode node : NodeRegistry.getNodes()) {
            if (now - node.getLastHeartbeat() > TIMEOUT) {
                node.setAlive(false);
                log.warn("Node offline : {}", node.getNodeId());
            }
        }
    }

    private static void gossip() {
        GossipNode randomNode =
                GossipManager.randomNode();
        if (randomNode == null) {
            return;
        }
        log.info("Gossip sync with : {}",
                randomNode.getNodeId());
        // 真实实现应该通过 TCP / UDP 发送节点列表
    }
}