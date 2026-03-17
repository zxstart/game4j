package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCalls;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class GossipRpcClient implements AutoCloseable {
    private final ManagedChannel channel;

    public GossipRpcClient(String host, int grpcPort) {
        this.channel = ManagedChannelBuilder.forAddress(host, grpcPort)
                .usePlaintext()
                .build();
    }

    public GossipExchangeResponse exchange(GossipExchangeRequest request, Duration timeout) {
        return ClientCalls.blockingUnaryCall(
                channel,
                GossipRpcContract.EXCHANGE_DESCRIPTOR,
                CallOptions.DEFAULT.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS),
                request
        );
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
