package com.game4j.cluster.gossip;

import com.game4j.cluster.registry.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gossip 协议管理器
 * <p>
 * 负责管理 Gossip 节点的状态，包括初始化、加入集群、随机选择节点和合并节点信息。
 * </p>
 */
public class GossipManager {

    private static final Logger log =
            LoggerFactory.getLogger(GossipManager.class);

    private static GossipNode localNode;

    private static final Random random = new Random();

    /**
     * 初始化本地节点
     * 
     * @param node 本地节点信息
     */
    public static void init(GossipNode node) {

        localNode = node;

        NodeRegistry.register(node);

        log.info("Local node init : {}", node.getNodeId());
    }

    public static GossipNode getLocalNode() {
        return localNode;
    }

    /**
     * 加入集群
     * 
     * @param node 目标节点
     */
    public static void join(GossipNode node) {

        NodeRegistry.register(node);

        log.info("Node join cluster : {}", node.getNodeId());
    }

    /**
     * 随机选择一个节点
     * 
     * @return 随机选中的节点，如果列表为空则返回 null
     */
    public static GossipNode randomNode() {

        List<GossipNode> list =
                new ArrayList<>(NodeRegistry.getNodes());

        if (list.isEmpty()) {
            return null;
        }

        return list.get(random.nextInt(list.size()));
    }

    /**
     * 合并节点列表
     * <p>
     * 将接收到的节点列表合并到本地注册表中。
     * 如果是新节点则注册，如果是已知节点则更新心跳时间。
     * </p>
     * 
     * @param nodes 接收到的节点列表
     */
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
