package com.game4j.plugin;

/** Defines the earliest lifecycle state at which a plugin may be enabled. */
public enum PluginStartupPhase {
    /** Needed before the game socket can safely accept traffic. */
    CRITICAL,
    /** May start after the game socket is listening. */
    POST_LISTENING,
    /** Requires cluster membership or distributed metadata. */
    CLUSTER_READY
}
