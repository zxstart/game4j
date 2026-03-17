package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.MethodDescriptor;

public final class GossipRpcContract {
    public static final String SERVICE_NAME = "game4j.cluster.GossipService";
    public static final String EXCHANGE_METHOD = "Exchange";

    public static final MethodDescriptor<GossipExchangeRequest, GossipExchangeResponse> EXCHANGE_DESCRIPTOR =
            MethodDescriptor.<GossipExchangeRequest, GossipExchangeResponse>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, EXCHANGE_METHOD))
                    .setRequestMarshaller(new JProtobufMarshaller<>(GossipExchangeRequest.class))
                    .setResponseMarshaller(new JProtobufMarshaller<>(GossipExchangeResponse.class))
                    .build();

    private GossipRpcContract() {
    }
}
