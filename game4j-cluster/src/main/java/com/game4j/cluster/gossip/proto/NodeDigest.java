package com.game4j.cluster.gossip.proto;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class NodeDigest implements Serializable {
    @Protobuf(order = 1, required = true, fieldType = FieldType.STRING)
    private String nodeId;

    @Protobuf(order = 2, required = false, fieldType = FieldType.STRING)
    private String nodeType;

    @Protobuf(order = 3, required = true, fieldType = FieldType.STRING)
    private String host;

    @Protobuf(order = 4, required = true, fieldType = FieldType.INT32)
    private int port;

    @Protobuf(order = 5, required = true, fieldType = FieldType.INT32)
    private int grpcPort;

    @Protobuf(order = 6, required = true, fieldType = FieldType.INT64)
    private long lastUpdated;
}
