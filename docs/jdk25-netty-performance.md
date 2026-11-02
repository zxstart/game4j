# JDK 25 and Netty performance guide

## Scope and priority

The project already targets JDK 25 and uses Netty 4.2.17.  The biggest throughput and tail-latency gains are not from
changing Java syntax: keep Netty I/O event loops short, select the transport and allocator for the deployment OS, and
measure with representative traffic.  Make changes in this order:

1. Keep the direct Netty dependency on the current 4.2.x maintenance release (4.2.17 at the time of
   writing), then run compatibility and load tests for each update.  Netty's release index is the authoritative current-version list.
   `grpc-netty-shaded` embeds its own Netty and must not be version-aligned by overriding `io.netty` artifacts.
   [Netty releases](https://netty.io/news/) · [current project dependency](../pom.xml)
2. On Linux/glibc, benchmark native epoll against the present NIO transport.  Netty says its native transports expose
   platform features, generate less garbage, and generally outperform NIO.  Use NIO as the portable fallback; do not
   ship the official epoll binary on musl/Alpine without a compatible build.  Treat io_uring as a separately benchmarked
   rollout: it moved into Netty 4.2, but it is not a universal replacement for epoll.
   [Native transports](https://netty.io/wiki/native-transports.html) · [Netty 4.2 migration guide](https://netty.io/wiki/netty-4.2-migration-guide.html)
3. On Java 24+, enable Netty's direct foreign-memory allocator path when the target environment permits it:
   `--enable-native-access=io.netty.common`.  Netty documents that this lets it directly use system allocation and is
   its lowest-overhead option.  Without it, Java 25 can use a shared-arena fallback, but its release handshake and JIT
   deoptimisation make it a last resort.  This requires Netty 4.2.3 or later, so the project's 4.2.17 qualifies.
   [Netty's Java 24+/Unsafe guide](https://netty.io/wiki/java-24-and-sun.misc.unsafe.html)

The current `NettyServer` already uses `MultiThreadIoEventLoopGroup` with `NioIoHandler`, which is the 4.2 direction.
For an epoll deployment, change the handler/channel pair together and make it configurable rather than assuming the
local development OS matches production.  [Netty 4.2 API](https://netty.io/4.2/api/io/netty/channel/class-use/IoEventLoop.html)

## Virtual threads: use behind Netty, not instead of Netty

A Netty `EventLoop` handles all I/O for its registered channels, often more than one.  Therefore
`GameChannelHandler.channelRead0` must not run JDBC, filesystem work, blocking RPC, locks with long waits, or CPU-heavy
game logic inline.  Decode, validate, and schedule quickly; run blocking request work on a new virtual thread per task;
then submit the completion/write back to the originating channel event loop to retain channel-state confinement and
response ordering.  Keep a semaphore or other explicit admission control around finite dependencies (database pool,
remote service); do **not** use a virtual-thread pool as a limiter.
[EventLoop contract](https://netty.io/4.2/api/io/netty/channel/EventLoop.html) · [JEP 444](https://openjdk.org/jeps/444)

Virtual threads increase useful concurrency for workloads that spend time blocked on JDK I/O; they do not make
CPU-bound handlers faster.  The existing gossip gRPC executor is a reasonable virtual-thread use only while its
service code can block and shutdown closes it.  Avoid placing Netty's own event loops on virtual threads.
[JEP 444](https://openjdk.org/jeps/444)

JDK 25 includes JEP 491's improvement: blocking in `synchronized` no longer pins a virtual-thread carrier, so do not
mechanically replace every `synchronized`.  Blocking native/JNI/FFI calls still deserve review.  Use
`-Djdk.tracePinnedThreads=full` and the JFR virtual-thread pinning event before changing code.
[JDK 24 migration notes](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-24.html) · [JEP 444 diagnostics](https://openjdk.org/jeps/444)

## JDK 25 features worth controlled adoption

* **Scoped values (production):** use `ScopedValue` for immutable trace, tenant, or security context inside a request
  and the virtual-thread children it creates.  It has lower time/space cost than `ThreadLocal` in this model, but it
  does not propagate itself across Netty's asynchronous callbacks; bridge it explicitly at the offload boundary.
  [Scoped values](https://docs.oracle.com/en/java/javase/25/core/scoped-values.html) · [JEP 506](https://openjdk.org/jeps/506)
* **Structured concurrency (preview):** appropriate for a request's parallel downstream fan-out, deadline, and
  cancellation, but keep it out of the networking foundation until preview APIs are an accepted production dependency.
  It needs `--enable-preview` at compile and runtime.  [JEP 505](https://openjdk.org/jeps/505)
* **Compact object headers (product option, not default):** A/B test
  `-XX:+UseCompactObjectHeaders` for many connections and small protocol objects.  It reduces headers from 96/128 to
  64 bits and can improve locality, but imposes a four-million-loaded-class limit.  [GC tuning guide](https://docs.oracle.com/en/java/javase/25/gctuning/other-considerations.html) · [JEP 519](https://openjdk.org/jeps/519)
* **Vector API (incubator):** only pursue after profiling identifies self-owned batch parsing, checksumming, or codec
  code as a CPU hotspot.  It does not automatically speed up `ByteBuf` or socket I/O and carries an incubator-module
  dependency.  [JEP 508](https://openjdk.org/jeps/508)
* **AOT cache/profiles:** retain the existing training process for cold-start and warm-up improvements; it is not a
  steady-state TPS optimisation.  Rebuild the cache whenever the JDK, artifacts, or flags change.
  [JEP 515](https://openjdk.org/jeps/515) · [project startup guide](startup-optimization.md)

Native Image is a GraalVM deployment choice rather than a JDK 25 HotSpot feature.  It may merit a separate
startup/RSS proof-of-concept, but reflection, generated protobuf, plugins, and class-path scanning make it unsuitable
as an assumed Netty peak-throughput upgrade.  [JDK 25 migration notes](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-25.html)

## GC and acceptance criteria

Start with fixed, container-aware heap bounds (`-Xms`/`-Xmx`) and the default G1.  G1 is the default and is intended to
balance high throughput with pause-time goals; it consumes concurrent CPU to shorten pauses.  Compare it with
generational ZGC only where P99/P99.9 latency matters more than the documented throughput cost.  Do not select a
collector from a feature list: compare the same warmed workload, heap limit, CPU quota, connection count, payload
mix, and TLS setting.
[G1 guide](https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html) · [collector selection](https://docs.oracle.com/en/java/javase/25/gctuning/available-collectors.html)

For each candidate (NIO/epoll/io_uring, allocator permission, G1/ZGC, compact headers), record warm TPS, P50/P99/P99.9
end-to-end latency, allocation rate, live heap/direct memory, GC pauses, CPU, and connection errors.  Capture JFR and
GC logs; accept a change only when it meets the target percentile without regressing sustained throughput or failure
rate.
