package com.game4j.cluster.hazelcast;

import com.game4j.cluster.gossip.proto.NodeDigest;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 集群节点信息
 * 表示集群中的一个节点，包含节点的基本信息和通信地址
 * 实现了 Serializable 接口，支持网络传输
 */
public class ClusterNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点唯一 ID
     * 用于在集群中唯一标识一个节点
     */
    private String nodeId;

    /**
     * 节点类型/角色
     * 用于区分不同功能的节点（如管理节点、工作节点等）
     */
    private String nodeType;

    /**
     * 主机地址
     * 节点的 IP 地址或主机名
     */
    private String host;

    /**
     * 端口号
     * Hazelcast 集群通信端口
     */
    private int port;

    /**
     * gRPC 服务端口
     * 用于节点间 Gossip 协议通信
     */
    private int grpcPort;

    public ClusterNode() {
    }

    /**
     * 构造集群节点
     *
     * @param nodeId 节点 ID
     * @param nodeType 节点类型
     * @param host 主机地址
     * @param port Hazelcast 端口
     * @param grpcPort gRPC 端口
     */
    public ClusterNode(String nodeId, String nodeType, String host, int port, int grpcPort) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.host = host;
        this.port = port;
        this.grpcPort = grpcPort;
    }

    /**
     * 将节点转换为 NodeDigest 对象
     * 用于 Gossip 协议中传递节点信息
     *
     * @return 节点摘要对象
     */
    public NodeDigest toDigest() {
        NodeDigest digest = new NodeDigest();
        digest.setNodeId(nodeId);
        digest.setNodeType(nodeType);
        digest.setHost(host);
        digest.setPort(port);
        digest.setGrpcPort(grpcPort);
        digest.setLastUpdated(System.currentTimeMillis());
        return digest;
    }

    /**
     * 从 NodeDigest 创建 ClusterNode 对象
     *
     * @param digest 节点摘要对象
     * @return 集群节点对象
     */
    public static ClusterNode fromDigest(NodeDigest digest) {
        return new ClusterNode(
                digest.getNodeId(),
                digest.getNodeType(),
                digest.getHost(),
                digest.getPort(),
                digest.getGrpcPort()
        );
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClusterNode that)) {
            return false;
        }
        return port == that.port
                && grpcPort == that.grpcPort
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(nodeType, that.nodeType)
                && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeType, host, port, grpcPort);
    }

    @Override
    public String toString() {
        return "ClusterNode[nodeId=" + nodeId
                + ", nodeType=" + nodeType
                + ", host=" + host
                + ", port=" + port
                + ", grpcPort=" + grpcPort + "]";
    }
}
