package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 节点摘要信息
 * 用于 Gossip 协议中传递节点的基本信息，包含节点 ID、类型、地址和端口等
 */
@Data
@NoArgsConstructor
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
}
