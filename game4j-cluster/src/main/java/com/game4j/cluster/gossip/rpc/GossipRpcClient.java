package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCalls;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gossip RPC 客户端
 * 用于向其他节点发送 Gossip 交换请求
 * 实现了 AutoCloseable 接口，支持 try-with-resources 自动关闭
 */
public class GossipRpcClient implements AutoCloseable {
    /**
     * gRPC 通信通道
     */
    private final ManagedChannel channel;

    /**
     * 构造 Gossip RPC 客户端
     *
     * @param host 目标节点主机地址
     * @param grpcPort 目标节点 gRPC 端口
     */
    public GossipRpcClient(String host, int grpcPort) {
        this.channel = ManagedChannelBuilder.forAddress(host, grpcPort)
                .usePlaintext()
                .build();
    }

    /**
     * 发送 Gossip 交换请求
     * 使用阻塞式单向调用，等待响应返回
     *
     * @param request Gossip 交换请求
     * @param timeout 超时时间
     * @return Gossip 交换响应
     */
    public GossipExchangeResponse exchange(GossipExchangeRequest request, Duration timeout) {
        return ClientCalls.blockingUnaryCall(
                channel,
                GossipRpcContract.EXCHANGE_DESCRIPTOR,
                CallOptions.DEFAULT.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS),
                request
        );
    }

    /**
     * 关闭 RPC 通道
     * 释放客户端资源
     */
    @Override
    public void close() {
        channel.shutdown();
    }
}
