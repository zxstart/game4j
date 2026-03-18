package com.game4j.benchmark;

import com.game4j.starter.initializer.PluginInitializer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * ClusterDashboardPlugin benchmark demo.
 * <p>
 * 默认场景：启用插件后，压测 /admin/cluster/api/overview 与 /admin/cluster/api/jvm。
 * </p>
 */
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(1)
@State(Scope.Benchmark)
public class ClusterDashboardPluginBenchmark {

    private HttpClient client;
    private URI overviewUri;
    private URI jvmUri;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        int adminPort = findFreePort();

        System.setProperty("game4j.plugin.cluster-dashboard.enabled", "true");
        System.setProperty("game4j.admin.enabled", "true");
        System.setProperty("game4j.admin.host", "127.0.0.1");
        System.setProperty("game4j.admin.port", String.valueOf(adminPort));

        // 只在 Trial 级别初始化一次，避免重复添加 shutdown hook。
        PluginInitializer.init();

        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.overviewUri = URI.create("http://127.0.0.1:" + adminPort + "/admin/cluster/api/overview");
        this.jvmUri = URI.create("http://127.0.0.1:" + adminPort + "/admin/cluster/api/jvm");

        awaitReady(overviewUri);
    }

    @Benchmark
    public int benchmarkOverviewApi() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(overviewUri)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    @Benchmark
    public int benchmarkJvmApi() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(jvmUri)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private static void awaitReady(URI uri) throws Exception {
        HttpClient probeClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        long deadline = System.currentTimeMillis() + 5000;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(1))
                        .GET()
                        .build();
                HttpResponse<String> response = probeClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ex) {
                last = ex;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Cluster dashboard plugin is not ready within timeout", last);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to allocate free port", ex);
        }
    }
}
