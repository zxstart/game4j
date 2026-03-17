package com.game4j.cluster.hazelcast;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 集群节点信息
 */
@Data
@NoArgsConstructor
public class ClusterNode implements Serializable {
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

    public ClusterNode(String nodeId, String nodeType, String host, int port) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.host = host;
        this.port = port;
    }
}
