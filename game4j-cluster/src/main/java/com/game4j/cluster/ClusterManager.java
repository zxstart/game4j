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
 * 负责集群节点的初始化、成员管理、Gossip 协议传播等核心功能
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);
    private static final String HAZELCAST_LOGGING_TYPE_KEY = "hazelcast.logging.type";
    private static final String HAZELCAST_LOGGING_TYPE_SLF4J = "slf4j";
    private static final String HAZELCAST_LOGGING_DETAILS_ENABLED_KEY = "hazelcast.logging.details.enabled";
    private static final String HAZELCAST_PHONE_HOME_ENABLED_KEY = "hazelcast.phone.home.enabled";
    private static final String HAZELCAST_DIAGNOSTICS_ENABLED_KEY = "hazelcast.diagnostics.enabled";
    /**
     * 存储节点元数据的 Hazelcast 分布式 Map 名称
     */
    private static final String META_MAP_NAME = "game4j.cluster.node-meta";

    /**
     * Hazelcast 实例，用于集群管理和分布式数据存储
     */
    private static HazelcastInstance hazelcastInstance;
    /**
     * 节点元数据分布式 Map，存储集群中所有节点的信息摘要
     */
    private static IMap<String, NodeDigest> nodeMetaMap;
    /**
     * gRPC 服务器，用于处理节点间的 Gossip 通信
     */
    private static Server grpcServer;
    /**
     * 定时调度器，用于周期性执行 Gossip 协议
     */
    private static ScheduledExecutorService scheduler;
    /**
     * IO 执行器，用于处理 gRPC 请求的异步执行
     */
    private static ExecutorService ioExecutor;
    /**
     * 当前集群配置
     */
    private static ClusterConfig config;

    /**
     * 本地节点信息
     */
    @Getter
    private static ClusterNode localNode;

    private static final Random random = new Random();

    /**
     * 初始化集群（兼容旧调用）
     * 使用默认配置初始化集群
     *
     * @param node 本地节点信息
     */
    public static void init(ClusterNode node) {
        init(node, ClusterConfig.defaults());
    }

    /**
     * 初始化本地节点并加入 Hazelcast 集群，同时启动 Gossip gRPC 服务。
     * 该方法会配置 Hazelcast 网络、注册成员监听器、启动 gRPC 服务器和定时 Gossip 任务
     *
     * @param node 本地节点信息
     * @param clusterConfig 集群配置，为 null 时使用默认配置
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

        String hazelcastLoggingType = ensureHazelcastLoggingTypeConfigured();
        String hazelcastLoggingDetailsEnabled = ensureHazelcastPropertyConfigured(
                HAZELCAST_LOGGING_DETAILS_ENABLED_KEY,
                "false"
        );
        String hazelcastPhoneHomeEnabled = ensureHazelcastPropertyConfigured(
                HAZELCAST_PHONE_HOME_ENABLED_KEY,
                "false"
        );
        String hazelcastDiagnosticsEnabled = ensureHazelcastPropertyConfigured(
                HAZELCAST_DIAGNOSTICS_ENABLED_KEY,
                "false"
        );

        Config hzConfig = new Config();
        hzConfig.setClusterName(config.getClusterName());
        hzConfig.setProperty(HAZELCAST_LOGGING_TYPE_KEY, hazelcastLoggingType);
        hzConfig.setProperty(HAZELCAST_LOGGING_DETAILS_ENABLED_KEY, hazelcastLoggingDetailsEnabled);
        hzConfig.setProperty(HAZELCAST_PHONE_HOME_ENABLED_KEY, hazelcastPhoneHomeEnabled);
        hzConfig.setProperty(HAZELCAST_DIAGNOSTICS_ENABLED_KEY, hazelcastDiagnosticsEnabled);

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
     * 合并 Hazelcast 成员信息和 Gossip 协议传播的节点元数据
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
     * 根据节点 ID 获取节点
     * 优先从 Hazelcast 成员属性中查找
     *
     * @param nodeId 节点 ID
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
     * 优先从 Gossip 元数据中选择，其次回退到 Hazelcast Member 属性
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
     * 依次关闭调度器、执行器、gRPC 服务器和 Hazelcast 实例
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

    /**
     * 将 Hazelcast Member 转换为 ClusterNode 对象
     * 从 Member 属性中提取节点信息
     *
     * @param member Hazelcast 成员
     * @return 集群节点对象
     */
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

    /**
     * 配置 Hazelcast 网络设置
     * 包括端口配置和 TCP/IP 加入配置
     *
     * @param hzConfig Hazelcast 配置对象
     * @param clusterConfig 集群配置
     */
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

    /**
     * 绑定 Hazelcast 成员监听器
     * 监听节点加入和离开事件，并更新节点元数据
     *
     * @param cluster Hazelcast 集群对象
     */
    private static void bindMembershipListener(Cluster cluster) {
        cluster.addMembershipListener(new MembershipListener() {
            @Override
            public void memberAdded(MembershipEvent membershipEvent) {
                ClusterNode node = memberToNode(membershipEvent.getMember());
                log.info(
                        "cluster member joined: nodeId={}, host={}, hzPort={}, grpcPort={}, members={}",
                        node.getNodeId(),
                        node.getHost(),
                        node.getPort(),
                        node.getGrpcPort(),
                        membershipEvent.getCluster().getMembers().size()
                );
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
                log.info(
                        "cluster member left: nodeId={}, members={}",
                        nodeId,
                        membershipEvent.getCluster().getMembers().size()
                );
            }
        });
    }

    /**
     * 启动 Gossip RPC 服务器
     * 使用 Netty 作为底层传输，处理来自其他节点的 Gossip 请求
     *
     * @param grpcPort gRPC 服务端口
     */
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

    /**
     * 启动 Gossip 定时调度器
     * 按照配置的间隔周期性执行 Gossip 协议，传播节点信息
     */
    private static void startGossipScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("gossip-scheduler-", 0).factory());
        long period = Math.max(500L, config.getGossipInterval().toMillis());
        scheduler.scheduleWithFixedDelay(ClusterManager::gossipOnce, period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一次 Gossip 协议
     * 随机选择一个目标节点并异步交换节点信息
     */
    private static void gossipOnce() {
        ClusterNode target = randomNode();
        if (target == null) {
            return;
        }
        ioExecutor.submit(() -> doGossipExchange(target));
    }

    /**
     * 执行 Gossip 信息交换
     * 向目标节点发送当前节点信息并合并对方返回的节点列表
     *
     * @param target 目标节点
     */
    private static void doGossipExchange(ClusterNode target) {
        GossipExchangeRequest request = buildRequest();
        try (GossipRpcClient client = new GossipRpcClient(target.getHost(), target.getGrpcPort())) {
            GossipExchangeResponse response = client.exchange(request, config.getRpcTimeout());
            mergeKnownNodes(response.getKnownNodes());
        } catch (Throwable ex) {
            log.debug("gossip rpc failed to {}:{} {}", target.getHost(), target.getGrpcPort(), ex.getMessage());
        }
    }

    /**
     * 构建 Gossip 请求
     * 包含本地节点信息和当前已知的节点列表
     *
     * @return Gossip 交换请求
     */
    private static GossipExchangeRequest buildRequest() {
        GossipExchangeRequest request = new GossipExchangeRequest();
        request.setFromNodeId(localNode.getNodeId());
        request.setSendTimestamp(System.currentTimeMillis());
        request.setSenderDigest(localNode.toDigest());
        request.setKnownNodes(new ArrayList<>(nodeMetaMap.values()));
        return request;
    }

    /**
     * 处理收到的 Gossip 请求
     * 更新发送方节点信息并合并其已知的节点列表
     *
     * @param request Gossip 交换请求
     * @return Gossip 交换响应
     */
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

    /**
     * 合并远程节点列表到本地元数据
     * 遍历远程节点列表并更新到分布式 Map 中
     *
     * @param remoteNodes 远程节点列表
     */
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

    /**
     * 插入或更新节点信息
     * 使用乐观时间戳策略，只更新更新的或相同时间的数据
     *
     * @param incoming 传入的节点摘要信息
     */
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

    /**
     * 获取本地主机地址
     * 当无法获取时回退到 127.0.0.1
     *
     * @return 本地主机 IP 地址
     */
    private static String localHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }

    /**
     * 默认让 Hazelcast 走 SLF4J，避免回退到 JUL 导致控制台输出风格不一致。
     * 若用户已显式配置，则尊重用户配置。
     */
    private static String ensureHazelcastLoggingTypeConfigured() {
        String value = System.getProperty(HAZELCAST_LOGGING_TYPE_KEY);
        if (value == null || value.isBlank()) {
            value = HAZELCAST_LOGGING_TYPE_SLF4J;
            System.setProperty(HAZELCAST_LOGGING_TYPE_KEY, value);
        }
        return value;
    }

    /**
     * 确保 Hazelcast 系统属性有默认值。
     * 如果用户通过 -D 显式配置，则优先使用用户值。
     */
    private static String ensureHazelcastPropertyConfigured(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = defaultValue;
            System.setProperty(key, value);
        }
        return value;
    }
}
