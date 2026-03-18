package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gossip 交换请求
 * 用于节点间交换节点信息，包含发送方节点信息和已知的节点列表
 */
@Data
@NoArgsConstructor
public class GossipExchangeRequest implements Serializable {
    /**
     * 发送方节点 ID
     */
    @Protobuf(order = 1, required = true, fieldType = FieldType.STRING)
    private String fromNodeId;

    /**
     * 发送时间戳（毫秒）
     */
    @Protobuf(order = 2, required = true, fieldType = FieldType.INT64)
    private long sendTimestamp;

    /**
     * 发送方节点的摘要信息
     */
    @Protobuf(order = 3, required = false, fieldType = FieldType.OBJECT)
    private NodeDigest senderDigest;

    /**
     * 发送方已知的节点列表
     */
    @Protobuf(order = 4, required = false, fieldType = FieldType.OBJECT)
    private List<NodeDigest> knownNodes = new ArrayList<>();
}
