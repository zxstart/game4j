package com.game4j.plugin.admin;

import com.game4j.cluster.ClusterManager;
import com.game4j.cluster.hazelcast.ClusterNode;
import com.sun.management.OperatingSystemMXBean;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
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
            server.createContext("/admin/cluster/api/jvm", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    writePlain(exchange, 405, "Method Not Allowed");
                    return;
                }
                handleJvmApi(exchange);
            });
            server.createContext("/", exchange -> writePlain(exchange, 404, "Not Found"));
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            log.info("Cluster admin dashboard started at http://{}:{}/admin/cluster",
                    config.getAdminHost(), config.getAdminPort());
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

    private void handleJvmApi(HttpExchange exchange) throws IOException {
        String requestedNodeId = queryParam(exchange.getRequestURI(), "nodeId");
        if (requestedNodeId == null || requestedNodeId.isBlank()) {
            writeJson(exchange, 200, buildLocalJvmJson(null));
            return;
        }

        ClusterNode localNode = ClusterManager.getLocalNode();
        if (localNode != null && Objects.equals(localNode.getNodeId(), requestedNodeId)) {
            writeJson(exchange, 200, buildLocalJvmJson(requestedNodeId));
            return;
        }

        ClusterNode targetNode = findNodeById(requestedNodeId);
        if (targetNode == null) {
            writeJson(exchange, 404, "{\"reachable\":false,\"error\":\"node not found\",\"requestedNodeId\":"
                    + jsonString(requestedNodeId) + "}");
            return;
        }

        writeJson(exchange, 200, buildRemoteJvmJson(targetNode, requestedNodeId));
    }

    private ClusterNode findNodeById(String nodeId) {
        for (ClusterNode node : ClusterManager.getNodes()) {
            if (Objects.equals(node.getNodeId(), nodeId)) {
                return node;
            }
        }
        return null;
    }

    private String buildRemoteJvmJson(ClusterNode targetNode, String requestedNodeId) {
        ClusterDashboardConfig cfg = dashboardConfig != null ? dashboardConfig : ClusterDashboardConfig.load();
        String remoteHost = targetNode.getHost();
        int remoteAdminPort = cfg.getAdminPort();
        String remoteUrl = "http://" + remoteHost + ":" + remoteAdminPort + "/admin/cluster/api/jvm";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return buildRemoteJvmErrorJson(targetNode, requestedNodeId, "upstream status " + response.statusCode(), remoteUrl);
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return buildRemoteJvmErrorJson(targetNode, requestedNodeId, "empty upstream response", remoteUrl);
            }
            return body;
        } catch (Exception ex) {
            log.warn("Fetch remote jvm info failed: nodeId={}, host={}, adminPort={}", requestedNodeId, remoteHost, remoteAdminPort, ex);
            return buildRemoteJvmErrorJson(targetNode, requestedNodeId, ex.getMessage(), remoteUrl);
        }
    }

    private String buildRemoteJvmErrorJson(ClusterNode node, String requestedNodeId, String error, String remoteUrl) {
        return "{"
                + "\"source\":\"remote\","
                + "\"reachable\":false,"
                + "\"requestedNodeId\":" + jsonString(requestedNodeId) + ","
                + "\"nodeId\":" + jsonString(node != null ? node.getNodeId() : null) + ","
                + "\"host\":" + jsonString(node != null ? node.getHost() : null) + ","
                + "\"remoteUrl\":" + jsonString(remoteUrl) + ","
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"error\":" + jsonString(error)
                + "}";
    }

    private String buildLocalJvmJson(String requestedNodeId) {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        long processCpuTime = -1L;
        double processCpuLoad = -1D;
        double systemCpuLoad = -1D;
        long totalPhysicalMemory = -1L;
        long freePhysicalMemory = -1L;
        java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
        if (baseOsBean instanceof OperatingSystemMXBean osBean) {
            processCpuTime = osBean.getProcessCpuTime();
            processCpuLoad = osBean.getProcessCpuLoad();
            systemCpuLoad = osBean.getCpuLoad();
            totalPhysicalMemory = osBean.getTotalMemorySize();
            freePhysicalMemory = osBean.getFreeMemorySize();
        }

        ClusterNode localNode = ClusterManager.getLocalNode();
        String localNodeId = localNode != null ? localNode.getNodeId() : null;
        String localHost = localNode != null ? localNode.getHost() : null;
        List<GcEntry> gcEntries = buildGcEntries();

        StringBuilder sb = new StringBuilder(2048);
        sb.append("{");
        sb.append("\"source\":\"local\",");
        sb.append("\"reachable\":true,");
        sb.append("\"requestedNodeId\":").append(jsonString(requestedNodeId)).append(",");
        sb.append("\"nodeId\":").append(jsonString(localNodeId)).append(",");
        sb.append("\"host\":").append(jsonString(localHost)).append(",");
        sb.append("\"timestamp\":\"").append(Instant.now()).append("\",");
        sb.append("\"jvm\":{");
        sb.append("\"name\":").append(jsonString(runtimeMXBean.getVmName())).append(",");
        sb.append("\"vendor\":").append(jsonString(runtimeMXBean.getVmVendor())).append(",");
        sb.append("\"version\":").append(jsonString(runtimeMXBean.getVmVersion())).append(",");
        sb.append("\"javaVersion\":").append(jsonString(System.getProperty("java.version"))).append(",");
        sb.append("\"uptimeMs\":").append(runtimeMXBean.getUptime()).append(",");
        sb.append("\"startTimeMs\":").append(runtimeMXBean.getStartTime()).append(",");
        sb.append("\"availableProcessors\":").append(runtime.availableProcessors()).append(",");
        sb.append("\"heapUsed\":").append(heap.getUsed()).append(",");
        sb.append("\"heapCommitted\":").append(heap.getCommitted()).append(",");
        sb.append("\"heapMax\":").append(heap.getMax()).append(",");
        sb.append("\"nonHeapUsed\":").append(nonHeap.getUsed()).append(",");
        sb.append("\"nonHeapCommitted\":").append(nonHeap.getCommitted()).append(",");
        sb.append("\"nonHeapMax\":").append(nonHeap.getMax()).append(",");
        sb.append("\"threadCount\":").append(threadMXBean.getThreadCount()).append(",");
        sb.append("\"peakThreadCount\":").append(threadMXBean.getPeakThreadCount()).append(",");
        sb.append("\"daemonThreadCount\":").append(threadMXBean.getDaemonThreadCount()).append(",");
        sb.append("\"loadedClassCount\":").append(classLoadingMXBean.getLoadedClassCount()).append(",");
        sb.append("\"totalLoadedClassCount\":").append(classLoadingMXBean.getTotalLoadedClassCount()).append(",");
        sb.append("\"unloadedClassCount\":").append(classLoadingMXBean.getUnloadedClassCount()).append(",");
        sb.append("\"processCpuLoad\":").append(processCpuLoad).append(",");
        sb.append("\"systemCpuLoad\":").append(systemCpuLoad).append(",");
        sb.append("\"processCpuTimeNs\":").append(processCpuTime).append(",");
        sb.append("\"totalPhysicalMemory\":").append(totalPhysicalMemory).append(",");
        sb.append("\"freePhysicalMemory\":").append(freePhysicalMemory).append(",");
        sb.append("\"gcCollectors\":[");
        appendGcCollectorsJson(sb, gcEntries);
        sb.append("],");
        sb.append("\"memoryAreas\":[");
        appendMemoryAreasJson(sb, memoryMXBean, gcEntries);
        sb.append("]");
        sb.append("}}");
        return sb.toString();
    }

    private void appendGcCollectorsJson(StringBuilder sb, List<GcEntry> gcEntries) {
        for (int i = 0; i < gcEntries.size(); i++) {
            GcEntry gc = gcEntries.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{")
                    .append("\"key\":").append(jsonString(gc.key())).append(",")
                    .append("\"name\":").append(jsonString(gc.name())).append(",")
                    .append("\"group\":").append(jsonString(gc.group())).append(",")
                    .append("\"count\":").append(gc.count()).append(",")
                    .append("\"timeMs\":").append(gc.timeMs())
                    .append("}");
        }
    }

    private void appendMemoryAreasJson(StringBuilder sb, MemoryMXBean memoryMXBean, List<GcEntry> gcEntries) {
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        int index = 0;
        index = appendMemoryArea(sb, index, "heap", heap, chooseGcForArea("heap", true, gcEntries));

        List<MemoryPoolMXBean> memoryPools = new ArrayList<>(ManagementFactory.getMemoryPoolMXBeans());
        memoryPools.sort(Comparator.comparing(MemoryPoolMXBean::getName, Comparator.nullsLast(String::compareTo)));
        for (MemoryPoolMXBean pool : memoryPools) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) {
                continue;
            }
            boolean heapPool = pool.getType() != null && "HEAP".equals(pool.getType().name());
            String areaName = normalizeMemoryAreaName(pool.getName());
            index = appendMemoryArea(sb, index, areaName, usage, chooseGcForArea(pool.getName(), heapPool, gcEntries));
        }

        appendMemoryArea(sb, index, "nonheap", nonHeap, null);
    }

    private int appendMemoryArea(StringBuilder sb, int index, String areaName, MemoryUsage usage, GcEntry gcEntry) {
        if (usage == null) {
            return index;
        }
        if (index > 0) {
            sb.append(",");
        }
        long used = usage.getUsed();
        long committed = usage.getCommitted();
        long max = usage.getMax();
        double usageRate = -1D;
        if (max > 0) {
            usageRate = (double) used / (double) max;
        } else if (committed > 0) {
            usageRate = (double) used / (double) committed;
        }

        sb.append("{")
                .append("\"name\":").append(jsonString(areaName)).append(",")
                .append("\"used\":").append(used).append(",")
                .append("\"committed\":").append(committed).append(",")
                .append("\"max\":").append(max).append(",")
                .append("\"usageRate\":").append(usageRate).append(",");
        if (gcEntry != null) {
            sb.append("\"gcKey\":").append(jsonString(gcEntry.key())).append(",")
                    .append("\"gcCount\":").append(gcEntry.count()).append(",")
                    .append("\"gcTimeMs\":").append(gcEntry.timeMs());
        } else {
            sb.append("\"gcKey\":null,\"gcCount\":-1,\"gcTimeMs\":-1");
        }
        sb.append("}");
        return index + 1;
    }

    private List<GcEntry> buildGcEntries() {
        List<GarbageCollectorMXBean> beans = new ArrayList<>(ManagementFactory.getGarbageCollectorMXBeans());
        beans.sort(Comparator.comparing(GarbageCollectorMXBean::getName, Comparator.nullsLast(String::compareTo)));
        List<GcEntry> entries = new ArrayList<>(beans.size());
        for (GarbageCollectorMXBean bean : beans) {
            String name = bean.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = "gc." + normalizeMetricName(name);
            String group = detectGcGroup(name);
            entries.add(new GcEntry(key, name, group, bean.getCollectionCount(), bean.getCollectionTime()));
        }
        return entries;
    }

    private GcEntry chooseGcForArea(String areaName, boolean heapArea, List<GcEntry> gcEntries) {
        if (!heapArea || gcEntries.isEmpty()) {
            return null;
        }
        String group = detectMemoryGroup(areaName);
        if ("young".equals(group)) {
            return firstGcByGroup(gcEntries, "young", true);
        }
        if ("old".equals(group)) {
            return firstGcByGroup(gcEntries, "old", true);
        }
        return firstGcByGroup(gcEntries, "young", false);
    }

    private GcEntry firstGcByGroup(List<GcEntry> gcEntries, String group, boolean fallbackToFirst) {
        for (GcEntry entry : gcEntries) {
            if (Objects.equals(group, entry.group())) {
                return entry;
            }
        }
        return fallbackToFirst || gcEntries.isEmpty() ? (gcEntries.isEmpty() ? null : gcEntries.get(0)) : null;
    }

    private String detectGcGroup(String name) {
        String normalized = normalizeMetricName(name);
        if (normalized.contains("young") || normalized.contains("scavenge") || normalized.contains("new")) {
            return "young";
        }
        if (normalized.contains("old") || normalized.contains("mixed") || normalized.contains("mark_sweep")
                || normalized.contains("full")) {
            return "old";
        }
        return "other";
    }

    private String detectMemoryGroup(String areaName) {
        String normalized = normalizeMetricName(areaName);
        if (normalized.contains("eden") || normalized.contains("survivor") || normalized.contains("young")
                || normalized.contains("new")) {
            return "young";
        }
        if (normalized.contains("old") || normalized.contains("tenured")) {
            return "old";
        }
        return "heap";
    }

    private String normalizeMemoryAreaName(String rawName) {
        String normalized = normalizeMetricName(rawName);
        if (normalized.isBlank()) {
            return "unknown_pool";
        }
        return normalized;
    }

    private String normalizeMetricName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        boolean underscore = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = Character.toLowerCase(raw.charAt(i));
            if (Character.isLetterOrDigit(ch)) {
                if (underscore) {
                    sb.append('_');
                    underscore = false;
                }
                sb.append(ch);
            } else if (sb.length() > 0) {
                underscore = true;
            }
        }
        String out = sb.toString();
        if (out.endsWith("_")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private record GcEntry(String key, String name, String group, long count, long timeMs) {
    }

    private String queryParam(URI uri, String key) {
        if (uri == null || uri.getRawQuery() == null || key == null || key.isBlank()) {
            return null;
        }
        String[] pairs = uri.getRawQuery().split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String rawName = idx >= 0 ? pair.substring(0, idx) : pair;
            String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";
            String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            if (Objects.equals(name, key)) {
                return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            }
        }
        return null;
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
