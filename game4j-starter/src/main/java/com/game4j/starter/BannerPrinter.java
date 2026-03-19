package com.game4j.starter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 控制台启动 Banner 输出。
 */
final class BannerPrinter {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private final AtomicBoolean printed = new AtomicBoolean(false);

    private BannerPrinter() {
    }

    static void print(
            LocalDateTime startTime,
            LocalDateTime endTime,
            double consumeSeconds,
            String clusterNodeSummary,
            String nettyAddress,
            String clusterAdminDashboardAddress) {
        Holder.INSTANCE.printOnce(startTime, endTime, consumeSeconds, clusterNodeSummary, nettyAddress, clusterAdminDashboardAddress);
    }

    private void printOnce(
            LocalDateTime startTime,
            LocalDateTime endTime,
            double consumeSeconds,
            String clusterNodeSummary,
            String nettyAddress,
            String clusterAdminDashboardAddress) {
        if (!printed.compareAndSet(false, true)) {
            return;
        }

        long pid = ProcessHandle.current().pid();
        String version = resolveVersion();
        String document = "https://github.com/zxstart";

        Runtime runtime = Runtime.getRuntime();
        double usedMemoryMb = toMb(runtime.totalMemory() - runtime.freeMemory());
        double freeMemoryMb = toMb(runtime.freeMemory());
        double totalMemoryMb = toMb(runtime.totalMemory());

        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator());
        appendLogo(builder);
        builder.append(System.lineSeparator());

        String sep = "+------------+--------------------------------+------------+--------------+----------+-------------------------+";
        builder.append(sep).append(System.lineSeparator());
        builder.append("| ").append(padRight(" game4j", 10))
                .append(" | ").append(padRight(" ", 30))
                .append(" | ").append(padRight(" Memory", 10))
                .append(" | ").append(padRight(" ", 12))
                .append(" | ").append(padRight(" Time", 8))
                .append(" | ").append(padRight(" ", 23))
                .append(" |").append(System.lineSeparator());
        builder.append(sep).append(System.lineSeparator());
        builder.append(row(" pid", String.valueOf(pid), " used", formatMb(usedMemoryMb), " start", TIME_FORMAT.format(startTime)));
        builder.append(row(" version", version, " freeMemory", formatMb(freeMemoryMb), " end", TIME_FORMAT.format(endTime)));
        builder.append(row(" document", document, " totalMemory", formatMb(totalMemoryMb), " consume", String.format(Locale.ROOT, "%.2f s", consumeSeconds)));
        builder.append(sep).append(System.lineSeparator());
        builder.append("| startup-details").append(System.lineSeparator());
        builder.append("| ").append(nonBlank(clusterNodeSummary)).append(System.lineSeparator());
        builder.append("| Netty address: ").append(nonBlank(nettyAddress)).append(System.lineSeparator());
        builder.append("| Cluster admin dashboard: ").append(nonBlank(clusterAdminDashboardAddress)).append(System.lineSeparator());
        builder.append(sep).append(System.lineSeparator());
        System.out.print(builder);
    }

    /**
     * 通过 JVM 的类加载机制, 保证只加载一次 (singleton)
     */
    private static final class Holder {
        private static final BannerPrinter INSTANCE = new BannerPrinter();
    }

    private static void appendLogo(StringBuilder builder) {
        String ln = System.lineSeparator();

        builder.append(color("   ██████╗  █████╗ ███╗   ███╗███████╗██╗  ██╗     ██╗", ANSI_CYAN)).append(ln);
        builder.append(color("  ██╔════╝ ██╔══██╗████╗ ████║██╔════╝██║  ██║     ██║", ANSI_CYAN)).append(ln);
        builder.append(color("  ██║  ███╗███████║██╔████╔██║█████╗  ███████║     ██║", ANSI_CYAN)).append(ln);
        builder.append(color("  ██║   ██║██╔══██║██║╚██╔╝██║██╔══╝  ╚════██║██   ██║", ANSI_CYAN)).append(ln);
        builder.append(color("  ╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗     ██║╚█████╔╝", ANSI_CYAN)).append(ln);
        builder.append(color("   ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝     ╚═╝ ╚════╝ ", ANSI_CYAN)).append(ln);
    }

    private static String row(
            String key1, String value1,
            String key2, String value2,
            String key3, String value3) {
        return new StringBuilder()
                .append("| ").append(padRight(key1, 10))
                .append(" | ").append(padRight(value1, 30))
                .append(" | ").append(padRight(key2, 10))
                .append(" | ").append(padRight(value2, 12))
                .append(" | ").append(padRight(key3, 8))
                .append(" | ").append(padRight(value3, 23))
                .append(" |").append(System.lineSeparator())
                .toString();
    }

    private static String padRight(String value, int width) {
        String clean = value == null ? "" : value;
        if (clean.length() >= width) {
            return clean.substring(0, width);
        }
        return clean + " ".repeat(width - clean.length());
    }

    private static String formatMb(double value) {
        return String.format(Locale.ROOT, "%.1fMB", value);
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static double toMb(long bytes) {
        return bytes / 1024.0d / 1024.0d;
    }

    private static String color(String value, String ansiCode) {
        if (!useAnsiColor()) {
            return value;
        }
        return ansiCode + value + ANSI_RESET;
    }

    private static boolean useAnsiColor() {
        String disabled = System.getenv("NO_COLOR");
        return disabled == null || disabled.isBlank();
    }

    private static String resolveVersion() {
        Package pkg = BannerPrinter.class.getPackage();
        if (pkg == null) {
            return "dev";
        }
        String version = pkg.getImplementationVersion();
        if (version == null || version.isBlank()) {
            return "dev";
        }
        return version;
    }
}
