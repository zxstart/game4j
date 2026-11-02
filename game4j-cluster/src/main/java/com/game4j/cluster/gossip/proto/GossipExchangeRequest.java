package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gossip 交换请求
 * 用于节点间交换节点信息，包含发送方节点信息和已知的节点列表
 */
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

    public String getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public long getSendTimestamp() {
        return sendTimestamp;
    }

    public void setSendTimestamp(long sendTimestamp) {
        this.sendTimestamp = sendTimestamp;
    }

    public NodeDigest getSenderDigest() {
        return senderDigest;
    }

    public void setSenderDigest(NodeDigest senderDigest) {
        this.senderDigest = senderDigest;
    }

    public List<NodeDigest> getKnownNodes() {
        return knownNodes;
    }

    public void setKnownNodes(List<NodeDigest> knownNodes) {
        this.knownNodes = knownNodes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GossipExchangeRequest that)) {
            return false;
        }
        return sendTimestamp == that.sendTimestamp
                && Objects.equals(fromNodeId, that.fromNodeId)
                && Objects.equals(senderDigest, that.senderDigest)
                && Objects.equals(knownNodes, that.knownNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNodeId, sendTimestamp, senderDigest, knownNodes);
    }

    @Override
    public String toString() {
        return "GossipExchangeRequest[fromNodeId=" + fromNodeId
                + ", sendTimestamp=" + sendTimestamp
                + ", senderDigest=" + senderDigest
                + ", knownNodes=" + knownNodes + "]";
    }
}
