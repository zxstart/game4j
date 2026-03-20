package com.game4j.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

/**
 * 统一配置文件加载器。
 * <p>
 * 优先读取 classpath 下的 game4j.yml / game4j.yaml，
 * 若不存在则回退到 game4j.properties。
 * </p>
 */
public final class ConfigFileLoader {
    private static final String[] CONFIG_FILES = new String[]{
            "game4j.yml",
            "game4j.yaml",
            "game4j.properties"
    };

    private ConfigFileLoader() {
    }

    public static Properties loadGame4jProperties() {
        ClassLoader classLoader = ConfigFileLoader.class.getClassLoader();
        Properties props = new Properties();
        for (String file : CONFIG_FILES) {
            try (InputStream in = classLoader.getResourceAsStream(file)) {
                if (in == null) {
                    continue;
                }
                if (isYaml(file)) {
                    loadYamlAsProperties(in, props);
                } else {
                    props.load(in);
                }
                return props;
            } catch (IOException ignored) {
            }
        }
        return props;
    }

    private static boolean isYaml(String fileName) {
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
    }

    private static void loadYamlAsProperties(InputStream in, Properties out) throws IOException {
        List<PathNode> path = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String noComment = stripInlineComment(line).replace("\t", "    ");
                if (noComment.isBlank()) {
                    continue;
                }

                int indent = countLeadingSpaces(noComment);
                String trimmed = noComment.trim();

                while (!path.isEmpty() && indent <= path.get(path.size() - 1).indent) {
                    path.remove(path.size() - 1);
                }

                if (trimmed.startsWith("- ")) {
                    if (path.isEmpty()) {
                        continue;
                    }
                    String listKey = buildPath(path);
                    String item = normalizeValue(trimmed.substring(2).trim());
                    appendCsv(out, listKey, item);
                    continue;
                }

                int colonIndex = trimmed.indexOf(':');
                if (colonIndex < 0) {
                    continue;
                }

                String key = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }

                if (value.isEmpty()) {
                    path.add(new PathNode(indent, key));
                    continue;
                }

                String fullKey = buildPath(path, key);
                out.setProperty(fullKey, normalizeValue(value));
            }
        }
    }

    private static String stripInlineComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (c == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int countLeadingSpaces(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static String normalizeValue(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String buildPath(List<PathNode> path) {
        Deque<String> keys = new ArrayDeque<>(path.size());
        for (PathNode node : path) {
            keys.add(node.key);
        }
        return String.join(".", keys);
    }

    private static String buildPath(List<PathNode> path, String key) {
        if (path.isEmpty()) {
            return key;
        }
        return buildPath(path) + "." + key;
    }

    private static void appendCsv(Properties out, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String existing = out.getProperty(key);
        if (existing == null || existing.isBlank()) {
            out.setProperty(key, value);
            return;
        }
        out.setProperty(key, existing + "," + value);
    }

    private static final class PathNode {
        private final int indent;
        private final String key;

        private PathNode(int indent, String key) {
            this.indent = indent;
            this.key = key;
        }
    }
}
