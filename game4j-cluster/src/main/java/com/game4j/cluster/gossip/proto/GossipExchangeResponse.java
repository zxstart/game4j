package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gossip 交换响应
 * 用于响应 Gossip 请求，包含确认信息和接收方已知的节点列表
 */
@Data
@NoArgsConstructor
public class GossipExchangeResponse implements Serializable {
    /**
     * 响应确认标志，true 表示成功处理请求
     */
    @Protobuf(order = 1, required = true, fieldType = FieldType.BOOL)
    private boolean ack;

    /**
     * 响应消息，通常用于传递错误信息或状态描述
     */
    @Protobuf(order = 2, required = false, fieldType = FieldType.STRING)
    private String message;

    /**
     * 接收方已知的节点列表，用于同步给发送方
     */
    @Protobuf(order = 3, required = false, fieldType = FieldType.OBJECT)
    private List<NodeDigest> knownNodes = new ArrayList<>();
}
