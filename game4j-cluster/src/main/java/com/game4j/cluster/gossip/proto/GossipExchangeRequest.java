package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class GossipExchangeRequest implements Serializable {
    @Protobuf(order = 1, required = true, fieldType = FieldType.STRING)
    private String fromNodeId;

    @Protobuf(order = 2, required = true, fieldType = FieldType.INT64)
    private long sendTimestamp;

    @Protobuf(order = 3, required = false, fieldType = FieldType.OBJECT)
    private NodeDigest senderDigest;

    @Protobuf(order = 4, required = false, fieldType = FieldType.OBJECT)
    private List<NodeDigest> knownNodes = new ArrayList<>();
}
