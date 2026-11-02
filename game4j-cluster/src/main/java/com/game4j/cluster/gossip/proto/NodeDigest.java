package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import java.io.Serializable;
import java.util.Objects;

/**
 * 节点摘要信息
 * 用于 Gossip 协议中传递节点的基本信息，包含节点 ID、类型、地址和端口等
 */
public class NodeDigest implements Serializable {
    /**
     * 节点唯一标识符
     */
    @Protobuf(order = 1, required = true, fieldType = FieldType.STRING)
    private String nodeId;

    /**
     * 节点类型或角色，用于区分不同功能的节点
     */
    @Protobuf(order = 2, required = false, fieldType = FieldType.STRING)
    private String nodeType;

    /**
     * 节点主机地址（IP 或主机名）
     */
    @Protobuf(order = 3, required = true, fieldType = FieldType.STRING)
    private String host;

    /**
     * 节点主端口号（Hazelcast 通信端口）
     */
    @Protobuf(order = 4, required = true, fieldType = FieldType.INT32)
    private int port;

    /**
     * 节点 gRPC 服务端口，用于 Gossip 通信
     */
    @Protobuf(order = 5, required = true, fieldType = FieldType.INT32)
    private int grpcPort;

    /**
     * 节点信息最后更新时间戳（毫秒）
     * 用于判断信息的时效性，在合并时采用最新的数据
     */
    @Protobuf(order = 6, required = true, fieldType = FieldType.INT64)
    private long lastUpdated;

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

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NodeDigest that)) {
            return false;
        }
        return port == that.port
                && grpcPort == that.grpcPort
                && lastUpdated == that.lastUpdated
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(nodeType, that.nodeType)
                && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeType, host, port, grpcPort, lastUpdated);
    }

    @Override
    public String toString() {
        return "NodeDigest[nodeId=" + nodeId
                + ", nodeType=" + nodeType
                + ", host=" + host
                + ", port=" + port
                + ", grpcPort=" + grpcPort
                + ", lastUpdated=" + lastUpdated + "]";
    }
}
