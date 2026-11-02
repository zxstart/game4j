package com.game4j.starter.initializer;

import java.util.Locale;

/** Strategy used to discover controllers during bootstrap. */
public enum ControllerIndexMode {
    /** Require generated index resources; production's fastest and most deterministic mode. */
    INDEXED,
    /** Prefer generated indexes and retain scanning only for migration compatibility. */
    AUTO,
    /** Always scan the classpath; intended for development diagnostics only. */
    SCAN;

    public static ControllerIndexMode parse(String value, ControllerIndexMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
