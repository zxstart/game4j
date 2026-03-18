package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;

/**
 * Gossip 请求处理器接口
 * 定义处理 Gossip 交换请求的功能接口
 */
@FunctionalInterface
public interface GossipRequestHandler {
    /**
     * 处理 Gossip 交换请求
     *
     * @param request Gossip 交换请求
     * @return Gossip 交换响应
     */
    GossipExchangeResponse handle(GossipExchangeRequest request);
}
