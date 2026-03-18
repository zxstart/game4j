package com.game4j.starter.admin;

import com.game4j.cluster.ClusterManager;
import com.game4j.cluster.hazelcast.ClusterNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * 集群管理页面 HTTP 服务。
 */
public class ClusterAdminHttpServer {
    private static final Logger log = LoggerFactory.getLogger(ClusterAdminHttpServer.class);
    private static final String HTML_PATH = "/admin/cluster.html";
    private HttpServer server;
    private ClusterDashboardConfig dashboardConfig;

    public synchronized void start(ClusterDashboardConfig config) {
        if (server != null) {
            log.warn("Cluster admin http server already started");
            return;
        }
        this.dashboardConfig = config;
        try {
            InetSocketAddress address = new InetSocketAddress(config.getAdminHost(), config.getAdminPort());
            server = HttpServer.create(address, 0);
            server.createContext("/admin/cluster", this::handleClusterPage);
            server.createContext("/admin/cluster/", this::handleClusterPage);
            server.createContext("/admin/cluster/api/overview", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    writePlain(exchange, 405, "Method Not Allowed");
                    return;
                }
                writeJson(exchange, 200, buildOverviewJson());
            });
            server.createContext("/", exchange -> writePlain(exchange, 404, "Not Found"));
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            log.info("Cluster admin dashboard started at http://{}:{}/admin/cluster", config.getAdminHost(), config.getAdminPort());
        } catch (IOException ex) {
            throw new IllegalStateException("Start cluster admin dashboard failed", ex);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            dashboardConfig = null;
            log.info("Cluster admin dashboard stopped");
        }
    }

    private void handleClusterPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlain(exchange, 405, "Method Not Allowed");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("/admin/cluster".equals(path)) {
            exchange.getResponseHeaders().set("Location", "/admin/cluster/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        byte[] bytes = readHtmlBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private byte[] readHtmlBytes() throws IOException {
        try (InputStream in = ClusterAdminHttpServer.class.getResourceAsStream(HTML_PATH)) {
            if (in == null) {
                return "<html><body>cluster.html not found</body></html>".getBytes(StandardCharsets.UTF_8);
            }
            return in.readAllBytes();
        }
    }

    private String buildOverviewJson() {
        ClusterDashboardConfig cfg = dashboardConfig != null ? dashboardConfig : ClusterDashboardConfig.load();
        Collection<ClusterNode> currentNodes = ClusterManager.getNodes();
        List<ClusterNode> nodes = new ArrayList<>(currentNodes);
        nodes.sort(Comparator.comparing(ClusterNode::getNodeId, Comparator.nullsLast(String::compareTo)));
        ClusterNode localNode = ClusterManager.getLocalNode();

        StringBuilder sb = new StringBuilder(1024);
        sb.append("{");
        sb.append("\"clusterEnabled\":").append(cfg.isClusterEnabled()).append(",");
        sb.append("\"clusterName\":\"").append(escapeJson(cfg.getClusterName())).append("\",");
        sb.append("\"serverPort\":").append(cfg.getServerPort()).append(",");
        sb.append("\"nodeCount\":").append(nodes.size()).append(",");
        sb.append("\"localNodeId\":").append(jsonString(localNode != null ? localNode.getNodeId() : null)).append(",");
        sb.append("\"timestamp\":\"").append(Instant.now()).append("\",");
        sb.append("\"nodes\":[");

        for (int i = 0; i < nodes.size(); i++) {
            ClusterNode node = nodes.get(i);
            if (i > 0) {
                sb.append(",");
            }
            boolean local = localNode != null && Objects.equals(localNode.getNodeId(), node.getNodeId());
            sb.append("{")
                    .append("\"nodeId\":").append(jsonString(node.getNodeId())).append(",")
                    .append("\"nodeType\":").append(jsonString(node.getNodeType())).append(",")
                    .append("\"host\":").append(jsonString(node.getHost())).append(",")
                    .append("\"port\":").append(node.getPort()).append(",")
                    .append("\"grpcPort\":").append(node.getGrpcPort()).append(",")
                    .append("\"local\":").append(local)
                    .append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void writePlain(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
