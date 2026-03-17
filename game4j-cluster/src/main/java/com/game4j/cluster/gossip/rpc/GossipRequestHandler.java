package com.game4j.cluster.gossip.rpc;

import com.game4j.cluster.gossip.proto.GossipExchangeRequest;
import com.game4j.cluster.gossip.proto.GossipExchangeResponse;

@FunctionalInterface
public interface GossipRequestHandler {
    GossipExchangeResponse handle(GossipExchangeRequest request);
}
