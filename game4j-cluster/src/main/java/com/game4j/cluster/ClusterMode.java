package com.game4j.cluster;

import java.util.Locale;

/** Hazelcast deployment topology used by a game node. */
public enum ClusterMode {
    /** Existing behavior: this process participates in Hazelcast partitioning and membership. */
    MEMBER,
    /** Lightweight game node: connects to a separately managed Hazelcast control plane. */
    CLIENT;

    public static ClusterMode parse(String value) {
        if (value == null || value.isBlank()) {
            return MEMBER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unsupported game4j.cluster.mode: " + value + "; expected member or client");
        }
    }
}
