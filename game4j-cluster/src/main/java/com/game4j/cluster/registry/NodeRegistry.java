package com.game4j.cluster.registry;

import com.game4j.cluster.gossip.GossipNode;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点注册表
 * <p>
 * 维护集群中所有节点的信息。
 * 使用 ConcurrentHashMap 保证线程安全。
 * </p>
 */
public class NodeRegistry {
    private static final Map<String, GossipNode> nodes = new ConcurrentHashMap<>();

    /**
     * 注册节点
     *
     * @param node 节点信息
     */
    public static void register(GossipNode node) {
        nodes.put(node.getNodeId(), node);
    }

    /**
     * 获取节点信息
     *
     * @param nodeId 节点ID
     * @return 节点信息，如果不存在返回 null
     */
    public static GossipNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取所有节点
     *
     * @return 节点集合
     */
    public static Collection<GossipNode> getNodes() {
        return nodes.values();
    }

    /**
     * 移除节点
     *
     * @param nodeId 节点ID
     */
    public static void remove(String nodeId) {
        nodes.remove(nodeId);
    }
}
