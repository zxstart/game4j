package com.game4j.starter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monotonic, allocation-light timings for the bootstrap critical path. */
public final class StartupMetrics {
    private final long startedNanos = System.nanoTime();
    private final Map<String, Long> phaseMillis = new LinkedHashMap<>();

    public synchronized void completed(String phase) {
        phaseMillis.put(phase, elapsedMillis());
    }

    public long elapsedMillis() {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    public synchronized Map<String, Long> snapshotMillis() {
        return Map.copyOf(phaseMillis);
    }
}
