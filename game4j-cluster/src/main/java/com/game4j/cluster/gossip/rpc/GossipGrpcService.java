package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.stub.ServerCalls;

public class GossipGrpcService implements BindableService {
    private final GossipRequestHandler handler;

    public GossipGrpcService(GossipRequestHandler handler) {
        this.handler = handler;
    }

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
