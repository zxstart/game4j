# Startup optimization deployment

## Controller index

Add `game4j-processor` to the consuming game's annotation-processor path. The processor writes
`META-INF/game4j/controllers.idx` into each application or plugin JAR. At runtime starter merges every
visible index resource, loads only listed controller classes, and avoids directory/JAR scanning.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.game4j</groupId>
        <artifactId>game4j-processor</artifactId>
        <version>${game4j.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Set `game4j.server.controller-index-mode=indexed` in production. `auto` is the migration default and
uses ClassGraph only when no generated index exists. `scan` is for development diagnostics.

## Cluster topology

`game4j.cluster.mode=member` preserves the existing topology. For faster and smaller game nodes,
run a fixed Hazelcast control plane and configure game nodes with:

```properties
game4j.cluster.mode=client
game4j.cluster.seed-members=10.0.0.11:5701,10.0.0.12:5701,10.0.0.13:5701
```

Client mode still owns the game's gRPC Gossip listener and stores node metadata in the control plane,
but it does not become a Hazelcast partition member. The game socket becomes `LISTENING` before either
mode finishes joining; use `GameBootstrap.awaitReady(timeout)` when a caller must wait for full cluster
readiness.

## Measuring a cold start

`GameBootstrap.getStartupMetrics()` exposes cumulative milliseconds for critical plugins, controllers,
Netty listening, cluster readiness, and deferred plugin phases. For a one-off deeper investigation,
capture a JFR recording without changing application code:

```powershell
.\scripts\startup-profile.ps1 -Java $env:JAVA_HOME\bin\java.exe -RecordingPath $env:TEMP\game4j-startup.jfr <your normal java arguments>
```

## JDK 25 AOT cache

Generate a cache with exactly the JDK release, OS/CPU, class path, and JVM flags used in production.
Start the service with the training script, execute representative login, route, and cluster-join traffic,
then stop it cleanly so the cache is written:

```powershell
.\scripts\aot-train.ps1 -Java $env:JAVA_HOME\bin\java.exe -CachePath .\game4j.aot <your normal java arguments>
```

Deploy the cache next to the release and use:

```powershell
.\scripts\aot-run.ps1 -Java $env:JAVA_HOME\bin\java.exe -CachePath .\game4j.aot <your normal java arguments>
```

`AOTMode=auto` safely falls back to ordinary startup when a cache is incompatible. Rebuild the cache
after changing the JDK, application artifacts, or JVM options.
