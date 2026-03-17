package com.game4j.cluster;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import com.game4j.cluster.gossip.proto.NodeDigest;
import com.game4j.cluster.gossip.rpc.GossipGrpcService;
import com.game4j.cluster.gossip.rpc.GossipRpcClient;
import com.game4j.cluster.hazelcast.ClusterNode;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 集群管理器，整合 Hazelcast + gRPC + JProtobuf Gossip。
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);
    private static final String META_MAP_NAME = "game4j.cluster.node-meta";

    private static HazelcastInstance hazelcastInstance;
    private static IMap<String, NodeDigest> nodeMetaMap;
    private static Server grpcServer;
    private static ScheduledExecutorService scheduler;
    private static ExecutorService ioExecutor;
    private static ClusterConfig config;

    @Getter
    private static ClusterNode localNode;

    private static final Random random = new Random();

    /**
     * 初始化集群（兼容旧调用）
     *
     * @param node 本地节点信息
     */
    public static void init(ClusterNode node) {
        init(node, ClusterConfig.defaults());
    }

    /**
     * 初始化本地节点并加入 Hazelcast 集群，同时启动 Gossip gRPC 服务。
     */
    public static synchronized void init(ClusterNode node, ClusterConfig clusterConfig) {
        if (hazelcastInstance != null) {
            log.warn("cluster already initialized, node={}", localNode != null ? localNode.getNodeId() : "unknown");
            return;
        }
        localNode = node;
        config = clusterConfig != null ? clusterConfig : ClusterConfig.defaults();

        if (localNode.getGrpcPort() <= 0) {
            localNode.setGrpcPort(config.getGrpcPort());
        }
        if (localNode.getHost() == null || localNode.getHost().isBlank()) {
            localNode.setHost(localHostAddress());
        }

        Config hzConfig = new Config();
        hzConfig.setClusterName(config.getClusterName());

        // 将节点业务信息写入 Hazelcast Member 的属性中
        hzConfig.getMemberAttributeConfig().setAttribute("nodeId", localNode.getNodeId());
        if (localNode.getNodeType() != null) {
            hzConfig.getMemberAttributeConfig().setAttribute("nodeType", localNode.getNodeType());
        }
        if (localNode.getHost() != null) {
            hzConfig.getMemberAttributeConfig().setAttribute("host", localNode.getHost());
        }
        hzConfig.getMemberAttributeConfig().setAttribute("port", String.valueOf(localNode.getPort()));
        hzConfig.getMemberAttributeConfig().setAttribute("grpcPort", String.valueOf(localNode.getGrpcPort()));

        configureNetwork(hzConfig, config);

        hazelcastInstance = Hazelcast.newHazelcastInstance(hzConfig);
        nodeMetaMap = hazelcastInstance.getMap(META_MAP_NAME);
        upsertNode(localNode.toDigest());

        bindMembershipListener(hazelcastInstance.getCluster());
        startGossipRpcServer(localNode.getGrpcPort());
        startGossipScheduler();

        log.info(
                "cluster initialized: nodeId={}, hzPort={}, grpcPort={}, clusterName={}",
                localNode.getNodeId(), localNode.getPort(), localNode.getGrpcPort(), config.getClusterName()
        );
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
        Map<String, ClusterNode> merged = new HashMap<>();
        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            ClusterNode node = memberToNode(member);
            if (node.getNodeId() != null) {
                merged.put(node.getNodeId(), node);
            }
        }
        if (nodeMetaMap != null) {
            for (NodeDigest digest : nodeMetaMap.values()) {
                merged.put(digest.getNodeId(), ClusterNode.fromDigest(digest));
            }
        }
        List<ClusterNode> nodes = new ArrayList<>(merged.values());
        nodes.sort(Comparator.comparing(ClusterNode::getNodeId, Comparator.nullsLast(String::compareTo)));
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
            list.removeIf(n -> Objects.equals(n.getNodeId(), localNode.getNodeId()));
        }
        list.removeIf(n -> n.getHost() == null || n.getHost().isBlank() || n.getGrpcPort() <= 0);

        if (list.isEmpty()) {
            // 回退到 Hazelcast Member 属性，保证至少可发送到在线节点
            for (Member member : hazelcastInstance.getCluster().getMembers()) {
                ClusterNode node = memberToNode(member);
                if (node.getNodeId() != null && !Objects.equals(node.getNodeId(), localNode.getNodeId())) {
                    if (node.getHost() != null && !node.getHost().isBlank() && node.getGrpcPort() > 0) {
                        list.add(node);
                    }
                }
            }
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
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            ioExecutor = null;
        }
        if (grpcServer != null) {
            grpcServer.shutdown();
            grpcServer = null;
        }
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
        nodeMetaMap = null;
        localNode = null;
    }

    private static ClusterNode memberToNode(Member member) {
        String nodeId = member.getAttribute("nodeId");
        String nodeType = member.getAttribute("nodeType");
        String host = member.getAttribute("host");
        String portStr = member.getAttribute("port");
        String grpcPortStr = member.getAttribute("grpcPort");
        int port = portStr != null ? Integer.parseInt(portStr) : 0;
        int grpcPort = grpcPortStr != null ? Integer.parseInt(grpcPortStr) : 0;

        return new ClusterNode(nodeId, nodeType, host, port, grpcPort);
    }

    private static void configureNetwork(Config hzConfig, ClusterConfig clusterConfig) {
        NetworkConfig networkConfig = hzConfig.getNetworkConfig();
        if (localNode.getPort() > 0) {
            networkConfig.setPort(localNode.getPort());
            networkConfig.setPortAutoIncrement(false);
        } else if (clusterConfig.getHazelcastPort() > 0) {
            networkConfig.setPort(clusterConfig.getHazelcastPort());
        }

        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(!clusterConfig.getSeedMembers().isEmpty());
        for (String member : clusterConfig.getSeedMembers()) {
            join.getTcpIpConfig().addMember(member);
        }
    }

    private static void bindMembershipListener(Cluster cluster) {
        cluster.addMembershipListener(new MembershipListener() {
            @Override
            public void memberAdded(MembershipEvent membershipEvent) {
                ClusterNode node = memberToNode(membershipEvent.getMember());
                log.info("member added: {}", node.getNodeId());
                if (node.getNodeId() != null && node.getGrpcPort() > 0) {
                    upsertNode(node.toDigest());
                }
            }

            @Override
            public void memberRemoved(MembershipEvent membershipEvent) {
                String nodeId = membershipEvent.getMember().getAttribute("nodeId");
                if (nodeId != null && nodeMetaMap != null) {
                    nodeMetaMap.remove(nodeId);
                }
                log.info("member removed: {}", nodeId);
            }
        });
    }

    private static void startGossipRpcServer(int grpcPort) {
        ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            grpcServer = NettyServerBuilder.forPort(grpcPort)
                    .executor(ioExecutor)
                    .addService(new GossipGrpcService(ClusterManager::handleGossipRequest))
                    .build()
                    .start();
        } catch (IOException ex) {
            throw new IllegalStateException("start gRPC gossip server failed", ex);
        }
    }

    private static void startGossipScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("gossip-scheduler-", 0).factory());
        long period = Math.max(500L, config.getGossipInterval().toMillis());
        scheduler.scheduleWithFixedDelay(ClusterManager::gossipOnce, period, period, TimeUnit.MILLISECONDS);
    }

    private static void gossipOnce() {
        ClusterNode target = randomNode();
        if (target == null) {
            return;
        }
        ioExecutor.submit(() -> doGossipExchange(target));
    }

    private static void doGossipExchange(ClusterNode target) {
        GossipExchangeRequest request = buildRequest();
        try (GossipRpcClient client = new GossipRpcClient(target.getHost(), target.getGrpcPort())) {
            GossipExchangeResponse response = client.exchange(request, config.getRpcTimeout());
            mergeKnownNodes(response.getKnownNodes());
        } catch (Throwable ex) {
            log.debug("gossip rpc failed to {}:{} {}", target.getHost(), target.getGrpcPort(), ex.getMessage());
        }
    }

    private static GossipExchangeRequest buildRequest() {
        GossipExchangeRequest request = new GossipExchangeRequest();
        request.setFromNodeId(localNode.getNodeId());
        request.setSendTimestamp(System.currentTimeMillis());
        request.setSenderDigest(localNode.toDigest());
        request.setKnownNodes(new ArrayList<>(nodeMetaMap.values()));
        return request;
    }

    private static GossipExchangeResponse handleGossipRequest(GossipExchangeRequest request) {
        if (request.getSenderDigest() != null) {
            upsertNode(request.getSenderDigest());
        }
        mergeKnownNodes(request.getKnownNodes());

        GossipExchangeResponse response = new GossipExchangeResponse();
        response.setAck(true);
        response.setMessage("ok");
        response.setKnownNodes(new ArrayList<>(nodeMetaMap.values()));
        return response;
    }

    private static void mergeKnownNodes(List<NodeDigest> remoteNodes) {
        if (remoteNodes == null || remoteNodes.isEmpty()) {
            return;
        }
        for (NodeDigest digest : remoteNodes) {
            if (digest != null && digest.getNodeId() != null) {
                upsertNode(digest);
            }
        }
    }

    private static void upsertNode(NodeDigest incoming) {
        if (incoming == null || incoming.getNodeId() == null || nodeMetaMap == null) {
            return;
        }
        if (incoming.getLastUpdated() <= 0) {
            incoming.setLastUpdated(System.currentTimeMillis());
        }
        nodeMetaMap.compute(incoming.getNodeId(), (k, oldVal) -> {
            if (oldVal == null || incoming.getLastUpdated() >= oldVal.getLastUpdated()) {
                return incoming;
            }
            return oldVal;
        });
    }

    private static String localHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }
}
