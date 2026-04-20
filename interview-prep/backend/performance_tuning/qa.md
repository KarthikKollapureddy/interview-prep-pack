# Performance Tuning & Profiling — Interview Q&A

> 10 questions covering JVM flags, GC tuning, thread/heap dump analysis, HikariCP, slow query debugging  
> Priority: **P0** — "How do you handle performance issues in production?" is a senior-level staple

---

### Q1. Explain the most important JVM flags for a production Spring Boot app.

**Answer:**

```bash
java -jar app.jar \
  # Memory:
  -Xms512m                    # Initial heap (set = Xmx for predictable GC)
  -Xmx2g                      # Max heap (never exceed 75% of container memory)
  -XX:MaxMetaspaceSize=256m    # Class metadata limit (prevent unbounded growth)

  # Garbage Collector (Java 17+):
  -XX:+UseG1GC                 # G1 GC (default in Java 9+, good all-around)
  -XX:MaxGCPauseMillis=200     # Target max GC pause (200ms)

  # OR for low-latency (Java 21):
  -XX:+UseZGC                  # ZGC: <1ms pause, good for large heaps
  -XX:+ZGenerational            # Generational ZGC (Java 21+)

  # Debugging (always enable in prod):
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/tmp/heap.hprof
  -XX:+ExitOnOutOfMemoryError  # Kill process on OOM (let K8s restart)

  # GC Logging:
  -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20m
```

**Common flags cheat sheet:**

| Flag | Purpose | Default | Recommendation |
|------|---------|---------|----------------|
| `-Xms` / `-Xmx` | Heap min/max | 1/4 of RAM | Set equal, 50-75% of container |
| `-XX:+UseG1GC` | G1 garbage collector | Java 9+ default | Good default for most apps |
| `-XX:+UseZGC` | Low-latency GC | Off | For <1ms pause requirement |
| `-XX:MaxGCPauseMillis` | GC pause target | 200ms | Lower = more GC cycles |
| `-XX:+UseStringDeduplication` | Dedupe identical Strings | Off | Saves memory for string-heavy apps |
| `-XX:ActiveProcessorCount=N` | Override CPU count | Auto-detect | Useful in containers |

**Container-specific:**
```bash
# In Docker/K8s, JVM may see host CPUs instead of container limits.
# Java 10+ auto-detects container limits, but verify:
-XX:+UseContainerSupport        # default in Java 10+
-XX:MaxRAMPercentage=75.0       # use 75% of container memory for heap
```

---

### Q2. Compare G1GC, ZGC, and Shenandoah.

**Answer:**

| Feature | G1GC | ZGC | Shenandoah |
|---------|------|-----|------------|
| Pause time | 100-500ms | <1ms (sub-millisecond) | <10ms |
| Heap size | Up to ~32GB | Up to 16TB | Up to ~32GB |
| Throughput | Best for most apps | Slightly lower | Slightly lower |
| CPU overhead | Low | Medium (concurrent) | Medium |
| Java version | 9+ (default) | 15+ (production) | 12+ |
| Best for | General purpose, most apps | Low-latency (trading, real-time) | Low-latency (alternative to ZGC) |

**G1GC phases:**
```
Young GC (minor): Collect Eden → Survivor, stop-the-world (~5-50ms)
Mixed GC: Young + old region collection
Full GC: Last resort, long pause — indicates tuning needed!
```

**How to decide:**
- Default: G1GC (works for 90% of apps)
- Need <10ms pauses: ZGC
- High throughput, don't care about pauses: Parallel GC

---

### Q3. How do you identify and fix slow database queries?

**Answer:**

**Step 1: Identify slow queries**
```sql
-- MySQL slow query log:
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- log queries taking > 1 second
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- Find slow queries:
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
```

**Step 2: Analyze with EXPLAIN**
```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 42 AND status = 'PENDING';

-- Look for:
-- type: ALL (full table scan!) → needs index
-- type: ref, eq_ref, range → good, using index
-- rows: high number → scanning too many rows
-- Extra: "Using temporary", "Using filesort" → potential performance issue
```

**Step 3: Fix patterns**

| Problem | Solution |
|---------|----------|
| Full table scan (`type: ALL`) | Add index on WHERE columns |
| N+1 query problem | Use JOIN or `@EntityGraph` in JPA |
| Missing composite index | Create index matching WHERE + ORDER BY |
| `SELECT *` on large table | Select only needed columns |
| LIKE '%pattern%' | Full-text index or search engine (Elasticsearch) |
| Large IN clause | Batch into chunks or use temp table |
| Subquery for each row | Rewrite as JOIN |

**JPA/Hibernate specific:**
```java
// Detect N+1 at dev time:
spring.jpa.properties.hibernate.generate_statistics=true

// Fix N+1:
@EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
List<Order> findByUserId(Long userId);

// OR use JOIN FETCH in JPQL:
@Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.userId = :userId")
List<Order> findOrdersWithItems(@Param("userId") Long userId);
```

---

### Q4. How do you tune HikariCP connection pool?

**Answer:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10         # max connections
      minimum-idle: 5                # min idle connections
      connection-timeout: 30000      # wait for connection (ms)
      idle-timeout: 600000           # close idle connections after 10min
      max-lifetime: 1800000          # max connection age (30min)
      leak-detection-threshold: 60000 # log warning if connection held > 60s
```

**How to size the pool:**
```
Formula (from HikariCP docs):
  connections = (core_count * 2) + effective_spindle_count

For SSD-backed DB with 4-core server:
  connections = (4 * 2) + 1 = 9 ≈ 10

Common mistake: Setting pool too large (100+).
More connections = more context switching = SLOWER overall.
```

**Monitor with Actuator:**
```
GET /actuator/metrics/hikaricp.connections.active
GET /actuator/metrics/hikaricp.connections.idle
GET /actuator/metrics/hikaricp.connections.pending
```

**Alert when:**
- `active` consistently near `max-pool-size` → pool exhaustion risk
- `pending` > 0 → requests waiting for connection → increase pool or fix slow queries
- Leak detection warnings in logs → code not closing connections

---

### Q5. How do you profile a Java application in production?

**Answer:**

**1. async-profiler (recommended for production):**
```bash
# Low-overhead CPU profiling:
./profiler.sh -d 30 -f flame.html -o flamegraph <PID>
# → Generates flame graph showing where CPU time is spent

# Allocation profiling:
./profiler.sh -e alloc -d 30 -f alloc.html <PID>
# → Shows which methods allocate the most memory
```

**2. JFR (Java Flight Recorder) — built into JDK:**
```bash
# Start recording:
jcmd <PID> JFR.start duration=60s filename=recording.jfr

# Or via JVM flag:
-XX:StartFlightRecording=duration=60s,filename=recording.jfr

# Analyze in JDK Mission Control (JMC) GUI
```

**3. Flame graph interpretation:**
```
┌──────────────────────────────────────────────────────────────┐
│                    main()                                     │
├──────────────────────────────────┬───────────────────────────┤
│       processOrder()             │       sendNotification()  │
├─────────────────┬────────────────┤                           │
│  validateOrder() │  saveToDb()    │                           │
│                  │ ████████████   │                           │ ← wide = more CPU time
│                  │ (slow DB call) │                           │
└─────────────────┴────────────────┴───────────────────────────┘

Width = time spent. Deeper = call depth. 
Look for WIDE bars — that's where the bottleneck is.
```

**4. Quick wins without profiler:**
```java
// Add timing to suspect methods:
long start = System.nanoTime();
result = expensiveOperation();
long elapsed = (System.nanoTime() - start) / 1_000_000;
if (elapsed > 100) {
    log.warn("expensiveOperation took {}ms", elapsed);
}

// Or use Micrometer Timer:
Timer.Sample sample = Timer.start(registry);
result = expensiveOperation();
sample.stop(Timer.builder("expensive.op").register(registry));
```

---

### Q6. Explain common memory leak patterns in Java.

**Answer:**

| Pattern | Example | Fix |
|---------|---------|-----|
| **Static collections** | `static Map<String, Object> cache = new HashMap<>()` growing forever | Use bounded cache (Caffeine/Guava with maxSize) |
| **Unclosed resources** | `InputStream`, `Connection`, `ResultSet` not closed | Use try-with-resources |
| **ThreadLocal not removed** | `ThreadLocal.set(...)` in web request, never cleared | `ThreadLocal.remove()` in finally block |
| **Event listeners** | Register listener, never deregister | Weak references or explicit deregister |
| **Inner class holds outer reference** | Non-static inner class retains reference to enclosing object | Make inner class static |
| **ClassLoader leak** | Redeployment in app server, old classloader retained | Restart JVM instead of hot-deploy |
| **String.substring()** (pre-Java 7u6) | Substring held reference to original char[] | Fixed in Java 7u6+ |

**Detecting leaks:**
```
1. Monitor: jvm_memory_used_bytes growing over time (sawtooth going up)
2. Heap dump: jmap -dump:live,format=b,file=heap.hprof <PID>
3. Analyze in MAT:
   - Leak Suspects report
   - Dominator Tree → biggest objects
   - Path to GC Root → who's holding the reference?
```

---

### Q7. How do you benchmark Java code properly?

**Answer:**

**Use JMH (Java Microbenchmark Harness):**
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class StringBenchmark {

    @Benchmark
    public String concatenation() {
        String s = "";
        for (int i = 0; i < 100; i++) {
            s += i;  // creates new String each iteration
        }
        return s;
    }

    @Benchmark
    public String stringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append(i);
        }
        return sb.toString();
    }
}
```

**Why not `System.nanoTime()` in a loop?**
- JIT compiler optimizes away dead code
- JVM warmup affects results
- No statistical rigor (no standard deviation, percentiles)
- JMH handles all this correctly

---

### Q8. How do you optimize Spring Boot startup time?

**Answer:**

| Optimization | Impact | How |
|-------------|--------|-----|
| Lazy initialization | High | `spring.main.lazy-initialization=true` |
| Reduce classpath scanning | Medium | Narrow `@ComponentScan` base packages |
| Disable unused auto-configs | Medium | `spring.autoconfigure.exclude` in properties |
| Use Spring Native / GraalVM | Very high | AOT compilation → <100ms startup |
| Virtual threads (Java 21) | Medium | `spring.threads.virtual.enabled=true` |
| Index of candidate components | Medium | `spring-context-indexer` at build time |

```yaml
# application.yml:
spring:
  main:
    lazy-initialization: true    # beans created on first access, not at startup
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.mail.MailAutoConfiguration
      - org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

---

### Q9. How do you handle the N+1 query problem?

**Answer:**

```java
// The problem:
List<Order> orders = orderRepository.findAll();  // 1 query
for (Order order : orders) {
    order.getItems().size();  // N queries! (one per order, lazy loading)
}
// Total: 1 + N queries (if 100 orders → 101 queries!)

// Fix 1: @EntityGraph
@EntityGraph(attributePaths = {"items"})
@Query("SELECT o FROM Order o")
List<Order> findAllWithItems();  // 1 query with JOIN

// Fix 2: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();

// Fix 3: Batch fetching
@OneToMany
@BatchSize(size = 20)  // Fetch items in batches of 20 instead of 1
private List<OrderItem> items;
// 1 query for orders + ceil(N/20) queries for items

// Fix 4: Global batch size
spring.jpa.properties.hibernate.default_batch_fetch_size=20
```

**Detection:**
```yaml
# Enable in dev:
spring.jpa.properties.hibernate.generate_statistics: true
logging.level.org.hibernate.stat: DEBUG
logging.level.org.hibernate.SQL: DEBUG
# Look for: "Session Metrics {N statements prepared}"
```

---

### Q10. Performance Tuning Checklist — what to check in order.

**Answer:**

```
Production Performance Investigation Order:
(Cheapest/quickest wins first)

1. ⬜ CHECK DASHBOARDS
   - Response time P50, P95, P99
   - Error rate
   - Throughput (requests/second)
   
2. ⬜ DATABASE (most common bottleneck)
   - Slow query log → EXPLAIN → add missing index
   - N+1 queries → JOIN FETCH / @EntityGraph
   - Connection pool utilization → tune HikariCP
   - Lock contention → optimize transactions, reduce scope
   
3. ⬜ CACHING
   - Add cache for frequently-read, rarely-changed data
   - Cache hit ratio < 80%? → review cache keys/TTL
   - Cache stampede? → Use @Cacheable with sync=true

4. ⬜ JVM
   - GC pauses → check GC logs, tune collector
   - Heap usage → right-size -Xmx
   - Thread pool exhaustion → increase/tune pool sizes
   - Memory leak → heap dump + MAT analysis

5. ⬜ NETWORK / EXTERNAL SERVICES
   - Timeout configuration → set reasonable timeouts
   - Circuit breaker → prevent cascade failure
   - Connection pooling for HTTP clients
   - Async calls where possible

6. ⬜ APPLICATION CODE
   - Flame graph (async-profiler) → identify hotspots
   - Unnecessary serialization/deserialization
   - Excessive object creation (consider object pooling)
   - Synchronization bottlenecks → reduce synchronized scope

7. ⬜ INFRASTRUCTURE
   - Scale horizontally (more instances)
   - Scale vertically (more CPU/RAM)
   - CDN for static assets
   - Read replicas for read-heavy workloads
```
