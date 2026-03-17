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
public class GossipExchangeResponse implements Serializable {
    @Protobuf(order = 1, required = true, fieldType = FieldType.BOOL)
    private boolean ack;

    @Protobuf(order = 2, required = false, fieldType = FieldType.STRING)
    private String message;

    @Protobuf(order = 3, required = false, fieldType = FieldType.OBJECT)
    private List<NodeDigest> knownNodes = new ArrayList<>();
}
