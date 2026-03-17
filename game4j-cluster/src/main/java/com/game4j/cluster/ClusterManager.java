package com.game4j.cluster;

import com.game4j.cluster.hazelcast.ClusterNode;
import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * 集群管理器
 * <p>
 * 基于 Hazelcast 实现的集群节点管理，替代原有的 Gossip 协议。
 * </p>
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);

    private static HazelcastInstance hazelcastInstance;

    @Getter
    private static ClusterNode localNode;

    private static final Random random = new Random();

    /**
     * 初始化本地节点并加入 Hazelcast 集群
     *
     * @param node 本地节点信息
     */
    public static void init(ClusterNode node) {
        localNode = node;
        Config config = new Config();
        config.setClusterName("game4j-cluster");

        // 将节点业务信息写入 Hazelcast Member 的属性中
        config.getMemberAttributeConfig().setAttribute("nodeId", node.getNodeId());
        if (node.getNodeType() != null) {
            config.getMemberAttributeConfig().setAttribute("nodeType", node.getNodeType());
        }
        if (node.getHost() != null) {
            config.getMemberAttributeConfig().setAttribute("host", node.getHost());
        }
        config.getMemberAttributeConfig().setAttribute("port", String.valueOf(node.getPort()));

        hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        log.info("Hazelcast cluster node init : {}", node.getNodeId());
    }

    /**
     * 获取集群中所有节点
     *
     * @return 节点集合
     */
    public static Collection<ClusterNode> getNodes() {
        if (hazelcastInstance == null) {
            return new ArrayList<>();
        }
        List<ClusterNode> nodes = new ArrayList<>();
        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            nodes.add(memberToNode(member));
        }
        return nodes;
    }

    /**
     * 根据节点ID获取节点
     *
     * @param nodeId 节点ID
     * @return 节点信息，不存在返回 null
     */
    public static ClusterNode getNode(String nodeId) {
        if (hazelcastInstance == null) {
            return null;
        }
        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            String id = member.getAttribute("nodeId");
            if (nodeId.equals(id)) {
                return memberToNode(member);
            }
        }
        return null;
    }

    /**
     * 随机选择一个其他节点
     *
     * @return 随机选中的节点，如果只有自己则返回 null
     */
    public static ClusterNode randomNode() {
        List<ClusterNode> list = new ArrayList<>(getNodes());
        if (localNode != null) {
            list.removeIf(n -> n.getNodeId().equals(localNode.getNodeId()));
        }

        if (list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }

    /**
     * 关闭集群节点
     */
    public static void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    private static ClusterNode memberToNode(Member member) {
        String nodeId = member.getAttribute("nodeId");
        String nodeType = member.getAttribute("nodeType");
        String host = member.getAttribute("host");
        String portStr = member.getAttribute("port");
        int port = portStr != null ? Integer.parseInt(portStr) : 0;

        return new ClusterNode(nodeId, nodeType, host, port);
    }
}
