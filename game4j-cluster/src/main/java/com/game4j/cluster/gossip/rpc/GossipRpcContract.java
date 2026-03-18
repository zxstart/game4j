package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.MethodDescriptor;

/**
 * Gossip RPC 契约类
 * 定义 gRPC 服务的名称、方法名和方法描述符
 * 用于客户端和服务端之间的通信协议约定
 */
public final class GossipRpcContract {
    /**
     * gRPC 服务名称
     */
    public static final String SERVICE_NAME = "game4j.cluster.GossipService";
    /**
     * Exchange 方法名称
     */
    public static final String EXCHANGE_METHOD = "Exchange";

    /**
     * Exchange 方法的描述符
     * 包含方法的类型、完整名称、请求和响应的序列化器
     */
    public static final MethodDescriptor<GossipExchangeRequest, GossipExchangeResponse> EXCHANGE_DESCRIPTOR =
            MethodDescriptor.<GossipExchangeRequest, GossipExchangeResponse>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, EXCHANGE_METHOD))
                    .setRequestMarshaller(new JProtobufMarshaller<>(GossipExchangeRequest.class))
                    .setResponseMarshaller(new JProtobufMarshaller<>(GossipExchangeResponse.class))
                    .build();

    /**
     * 私有构造函数，防止实例化
     */
    private GossipRpcContract() {
    }
}
