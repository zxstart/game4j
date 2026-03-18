package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.stub.ServerCalls;

/**
 * Gossip gRPC 服务实现
 * 实现 gRPC 服务绑定，处理来自其他节点的 Gossip 交换请求
 * 使用异步方式处理请求并返回响应
 */
public class GossipGrpcService implements BindableService {
    /**
     * Gossip 请求处理器
     */
    private final GossipRequestHandler handler;

    /**
     * 构造 Gossip gRPC 服务
     *
     * @param handler 请求处理器
     */
    public GossipGrpcService(GossipRequestHandler handler) {
        this.handler = handler;
    }

    /**
     * 绑定 gRPC 服务
     * 配置 Exchange 方法的异步调用处理逻辑
     *
     * @return gRPC 服务定义
     */
    @Override
    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder(GossipRpcContract.SERVICE_NAME)
                .addMethod(
                        GossipRpcContract.EXCHANGE_DESCRIPTOR,
                        ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                            try {
                                GossipExchangeResponse response = handler.handle(request);
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                            } catch (Throwable ex) {
                                responseObserver.onError(
                                        Status.INTERNAL
                                                .withDescription("gossip exchange failed")
                                                .withCause(ex)
                                                .asRuntimeException()
                                );
                            }
                        })
                )
                .build();
    }
}
