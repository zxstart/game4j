package com.game4j.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 在 Java 9+ 环境下，按需自动重启当前进程并补齐 Hazelcast 推荐 JVM 参数。
 */
public final class HazelcastJvmArgsAutoConfigurator {
    private static final Logger log = LoggerFactory.getLogger(HazelcastJvmArgsAutoConfigurator.class);
    private static final String RESTART_MARKER_PROPERTY = "game4j.internal.hazelcast.jvm-args.injected";

    private static final List<String> REQUIRED_JVM_ARGS = List.of(
            "--add-modules", "java.se",
            "--add-exports", "java.base/jdk.internal.ref=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.management/sun.management=ALL-UNNAMED",
            "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED"
    );

    private HazelcastJvmArgsAutoConfigurator() {
    }

    public static void ensureHazelcastJvmArgs() {
        if (Runtime.version().feature() < 9) {
            return;
        }
        if ("true".equalsIgnoreCase(System.getProperty(RESTART_MARKER_PROPERTY))) {
            return;
        }

        List<String> currentVmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (containsAllRequiredJvmArgs(currentVmArgs)) {
            return;
        }

        String rawCommand = System.getProperty("sun.java.command");
        if (rawCommand == null || rawCommand.isBlank()) {
            log.warn("Cannot auto-inject Hazelcast JVM module args: missing sun.java.command.");
            return;
        }

        try {
            List<String> command = buildRestartCommand(currentVmArgs, rawCommand);
            new ProcessBuilder(command).inheritIO().start();
            log.info("Auto-restarted JVM with Hazelcast module access arguments.");
            System.exit(0);
        } catch (IOException e) {
            log.warn("Failed to auto-restart with Hazelcast JVM module args, continue without restart.", e);
        }
    }

    private static List<String> buildRestartCommand(List<String> currentVmArgs, String rawJavaCommand) {
        List<String> command = new ArrayList<>();
        command.add(resolveJavaExecutablePath());
        command.addAll(currentVmArgs);

        appendMissingRequiredJvmArgs(command, currentVmArgs);
        command.add("-D" + RESTART_MARKER_PROPERTY + "=true");

        List<String> appArgs = splitCommandLine(rawJavaCommand);
        if (appArgs.isEmpty()) {
            throw new IllegalStateException("Cannot restart JVM: application command is empty.");
        }

        String firstArg = appArgs.getFirst();
        if (firstArg.endsWith(".jar")) {
            command.add("-jar");
            command.add(firstArg);
            command.addAll(appArgs.subList(1, appArgs.size()));
        } else {
            command.add("-cp");
            command.add(System.getProperty("java.class.path", ""));
            command.addAll(appArgs);
        }
        return command;
    }

    private static String resolveJavaExecutablePath() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        String javaBinaryName = isWindows() ? "java.exe" : "java";
        return javaHome.resolve("bin").resolve(javaBinaryName).toString();
    }

    private static boolean containsAllRequiredJvmArgs(List<String> vmArgs) {
        for (int i = 0; i < REQUIRED_JVM_ARGS.size(); i += 2) {
            String key = REQUIRED_JVM_ARGS.get(i);
            String value = REQUIRED_JVM_ARGS.get(i + 1);
            if (!hasArg(vmArgs, key, value)) {
                return false;
            }
        }
        return true;
    }

    private static void appendMissingRequiredJvmArgs(List<String> targetCommand, List<String> currentVmArgs) {
        for (int i = 0; i < REQUIRED_JVM_ARGS.size(); i += 2) {
            String key = REQUIRED_JVM_ARGS.get(i);
            String value = REQUIRED_JVM_ARGS.get(i + 1);
            if (!hasArg(currentVmArgs, key, value)) {
                targetCommand.add(key);
                targetCommand.add(value);
            }
        }
    }

    private static boolean hasArg(List<String> vmArgs, String key, String value) {
        String compact = key + "=" + value;
        if (vmArgs.contains(compact)) {
            return true;
        }
        for (int i = 0; i < vmArgs.size() - 1; i++) {
            if (key.equals(vmArgs.get(i)) && value.equals(vmArgs.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitCommandLine(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if ((c == '"' || c == '\'') && (!inQuotes || c == quoteChar)) {
                if (inQuotes) {
                    inQuotes = false;
                    quoteChar = 0;
                } else {
                    inQuotes = true;
                    quoteChar = c;
                }
                continue;
            }
            if (Character.isWhitespace(c) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win");
    }
}
