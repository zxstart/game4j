package com.game4j.cluster.hazelcast;

import com.game4j.cluster.gossip.proto.NodeDigest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 集群节点信息
 */
@Data
@NoArgsConstructor
public class ClusterNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点唯一ID
     */
    private String nodeId;

    /**
     * 节点类型/角色
     */
    private String nodeType;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private int port;

    /**
     * gRPC 服务端口
     */
    private int grpcPort;

    public ClusterNode(String nodeId, String nodeType, String host, int port, int grpcPort) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.host = host;
        this.port = port;
        this.grpcPort = grpcPort;
    }

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

    public static ClusterNode fromDigest(NodeDigest digest) {
        return new ClusterNode(
                digest.getNodeId(),
                digest.getNodeType(),
                digest.getHost(),
                digest.getPort(),
                digest.getGrpcPort()
        );
    }
}
