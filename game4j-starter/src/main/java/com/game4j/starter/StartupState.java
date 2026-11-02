package com.game4j.starter;

/** Observable bootstrap states. LISTENING is safe for local traffic; READY includes cluster readiness. */
public enum StartupState {
    NEW,
    STARTING,
    LISTENING,
    CLUSTER_JOINING,
    READY,
    DEGRADED,
    STOPPING,
    STOPPED,
    FAILED
}
