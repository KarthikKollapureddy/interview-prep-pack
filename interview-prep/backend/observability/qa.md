# Observability & Production Debugging — Interview Q&A

> 12 questions covering ELK Stack, Prometheus/Grafana, Distributed Tracing, MDC, and real debugging workflows  
> Priority: **P0** — "How do you debug production issues?" is asked in EVERY senior interview

---

## Conceptual Questions

### Q1. A user reports a slow API in production. Walk me through your debugging steps.

**Answer — Structured approach:**

```
Step 1: REPRODUCE & SCOPE
  ├── Is it one user or all users? (check error rate dashboards)
  ├── Is it one API or multiple? (check per-endpoint latency P99)
  ├── When did it start? (correlate with recent deployments)
  └── Is the issue intermittent or constant?

Step 2: CHECK DASHBOARDS (Grafana)
  ├── API response time (P50, P95, P99)
  ├── Error rate (5xx spike?)
  ├── CPU / Memory / Thread count
  ├── Database connection pool utilization
  └── JVM GC pause time

Step 3: TRACE THE REQUEST (Distributed Tracing)
  ├── Find the request by correlation ID / trace ID
  ├── Identify which service/span is slow
  ├── Check if it's the DB query, external API call, or business logic
  └── Compare slow trace with a normal trace

Step 4: CHECK LOGS (ELK / Splunk)
  ├── Search by correlation ID across all services
  ├── Look for error logs, warnings, timeout messages
  ├── Check for N+1 queries, connection pool exhaustion
  └── Look for GC pause logs

Step 5: DEEP DIVE
  ├── Slow DB query? → EXPLAIN plan, missing index, lock contention
  ├── External API slow? → Check timeout config, circuit breaker state
  ├── High CPU? → Thread dump analysis (jstack)
  ├── Memory issue? → Heap dump analysis (jmap + MAT)
  └── Connection pool exhausted? → Check HikariCP metrics

Step 6: FIX & PREVENT
  ├── Apply fix (index, cache, timeout, pool size)
  ├── Add alerting rule so this triggers BEFORE users report it
  └── Add runbook for this failure mode
```

---

### Q2. Explain the three pillars of observability.

**Answer:**

| Pillar | What | Tool | Example |
|--------|------|------|---------|
| **Logs** | Discrete events with context | ELK, Splunk, CloudWatch | `ERROR: Payment failed for orderId=123, reason=TIMEOUT` |
| **Metrics** | Numeric measurements over time | Prometheus + Grafana | `http_request_duration_seconds{endpoint="/api/orders", quantile="0.99"} = 2.3` |
| **Traces** | Request flow across services | Jaeger, Zipkin, AWS X-Ray | Order Service → Payment Service → Notification Service (total: 1.2s) |

```
Logs   → WHAT happened (detail)
Metrics → HOW MUCH is happening (aggregates, trends, alerts)
Traces  → WHERE time is spent (cross-service request flow)
```

**They work together:**
- Alert fires on metric (P99 latency > 2s)
- Check trace to find slow service
- Check logs of that service to find root cause

---

### Q3. What is MDC (Mapped Diagnostic Context)? Why is it critical for microservices?

**Answer:**
MDC adds **correlation IDs** to every log line so you can trace a single request across all services.

```java
// Filter to set MDC at request entry:
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws Exception {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();  // CRITICAL: prevent leak to next request
        }
    }
}
```

```xml
<!-- logback-spring.xml pattern with MDC: -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
```

```
Output:
2026-04-20 10:15:32 [http-nio-8080-exec-1] [abc-123-def] INFO  OrderService - Creating order for userId=42
2026-04-20 10:15:32 [http-nio-8081-exec-3] [abc-123-def] INFO  PaymentService - Processing payment for orderId=101
2026-04-20 10:15:33 [http-nio-8082-exec-2] [abc-123-def] INFO  NotificationService - Sending email for orderId=101
```

**Pass correlation ID between services:**
```java
// RestTemplate interceptor:
restTemplate.getInterceptors().add((request, body, execution) -> {
    request.getHeaders().add("X-Correlation-Id", MDC.get("correlationId"));
    return execution.execute(request, body);
});

// Kafka: put in headers
kafkaTemplate.send(new ProducerRecord<>("topic", key, value)
    .headers().add("correlationId", MDC.get("correlationId").getBytes()));
```

---

### Q4. Explain the ELK Stack (Elasticsearch + Logstash + Kibana).

**Answer:**

```
┌─────────┐     ┌──────────┐     ┌───────────────┐     ┌─────────┐
│ App Logs │────►│ Filebeat  │────►│   Logstash     │────►│ Elastic │
│ (JSON)   │     │ (shipper) │     │ (parse/enrich) │     │ search  │
└─────────┘     └──────────┘     └───────────────┘     └────┬────┘
                                                             │
                                                        ┌────▼────┐
                                                        │ Kibana  │
                                                        │ (UI)    │
                                                        └─────────┘
```

| Component | Role |
|-----------|------|
| **Filebeat** | Lightweight log shipper on each server. Tails log files, sends to Logstash. |
| **Logstash** | Parse, transform, enrich logs (grok patterns, add GeoIP, parse JSON). |
| **Elasticsearch** | Store & index logs. Full-text search, aggregations. |
| **Kibana** | Visualize: dashboards, search logs, create alerts. |

**Spring Boot structured logging (JSON):**
```xml
<!-- logback-spring.xml with JSON output for ELK: -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>correlationId</includeMdcKeyName>
    </encoder>
</appender>
```

**Interview tip:** If asked "ELK vs Splunk?" → ELK is open-source, self-hosted. Splunk is enterprise SaaS, easier but expensive. Datadog is the cloud-native alternative.

---

### Q5. How does distributed tracing work? Explain Spring Boot integration.

**Answer:**

**Concept:** Each request gets a **Trace ID**. Each service call within that request gets a **Span ID**. Together they form a trace tree.

```
Trace ID: abc-123
├── Span 1: API Gateway (10ms)
│   ├── Span 2: Order Service (50ms)
│   │   ├── Span 3: DB Query (20ms)
│   │   └── Span 4: Payment Service (200ms)  ← BOTTLENECK
│   │       └── Span 5: External Bank API (180ms)
│   └── Span 6: Notification Service (30ms)
Total: 290ms
```

**Spring Boot 3 + Micrometer Tracing (replaces Sleuth):**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev, 10% in prod
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**Automatic propagation:** Micrometer auto-injects trace/span IDs into:
- HTTP headers (`traceparent`, `b3`)
- Kafka headers
- Log MDC (`traceId`, `spanId`)

```
Log output with tracing:
2026-04-20 [traceId=abc123, spanId=def456] INFO OrderService - Creating order
2026-04-20 [traceId=abc123, spanId=ghi789] INFO PaymentService - Processing payment
→ Search Kibana for traceId=abc123 → see ALL logs for this request across ALL services
```

---

### Q6. Explain Prometheus + Grafana for monitoring.

**Answer:**

```
┌─────────────┐  scrape /actuator/prometheus   ┌────────────┐
│ Spring Boot  │◄──────────────────────────────│ Prometheus  │
│ App          │  (every 15s)                   │ (TSDB)     │
│ /actuator/   │                                └─────┬──────┘
│  prometheus  │                                      │
└─────────────┘                                ┌─────▼──────┐
                                               │  Grafana    │
                                               │ (Dashboard) │
                                               └─────┬──────┘
                                                      │ alert
                                               ┌─────▼──────┐
                                               │ AlertManager│
                                               │ → Slack/PD  │
                                               └─────────────┘
```

**Spring Boot Actuator + Micrometer:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  metrics:
    tags:
      application: order-service
```

**Custom business metrics:**
```java
@Component
public class OrderMetrics {
    private final Counter orderCounter;
    private final Timer orderTimer;

    public OrderMetrics(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .tag("type", "online")
            .description("Total orders created")
            .register(registry);
        this.orderTimer = Timer.builder("orders.processing.time")
            .description("Order processing duration")
            .register(registry);
    }

    public void recordOrder(Runnable task) {
        orderTimer.record(task);
        orderCounter.increment();
    }
}
```

**Key metrics to monitor (RED method):**
| Type | Metric | Alert Threshold |
|------|--------|----------------|
| **R**ate | `http_server_requests_seconds_count` | Sudden drop = outage |
| **E**rrors | `http_server_requests{status=~"5.."}` | Error rate > 1% |
| **D**uration | `http_server_requests_seconds{quantile="0.99"}` | P99 > 2s |
| JVM | `jvm_memory_used_bytes` | > 80% of max |
| DB Pool | `hikaricp_connections_active` | > 80% of max |
| GC | `jvm_gc_pause_seconds_sum` | > 500ms |

---

### Q7. How do you take and analyze a thread dump?

**Answer:**

```bash
# Take thread dump (app keeps running):
jstack <PID> > thread_dump.txt

# Or via actuator:
curl http://localhost:8080/actuator/threaddump > dump.json

# Or send signal:
kill -3 <PID>   # prints to stdout/stderr
```

**What to look for:**
```
"http-nio-8080-exec-1" #21 daemon prio=5 os_prio=0 tid=0x... nid=0x... BLOCKED
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.PaymentService.processPayment(PaymentService.java:45)
        - waiting to lock <0x000000076ab02f58> (a java.lang.Object)
        - locked by "http-nio-8080-exec-3"

"http-nio-8080-exec-3" #23 daemon prio=5 BLOCKED
        at com.example.OrderService.createOrder(OrderService.java:32)
        - waiting to lock <0x000000076ab02f60>
        - locked by "http-nio-8080-exec-1"
```

**Diagnosis patterns:**
| Thread State | Meaning | Action |
|-------------|---------|--------|
| Many threads `BLOCKED` | Lock contention / deadlock | Find the lock holder, reduce synchronized scope |
| Many threads `WAITING` on DB | Connection pool exhausted | Increase pool size or fix slow queries |
| Many threads `RUNNABLE` on same method | CPU-bound hotspot | Profile with async-profiler |
| Thread count growing | Thread leak | Check ExecutorService shutdown |

---

### Q8. How do you take and analyze a heap dump?

**Answer:**

```bash
# Take heap dump:
jmap -dump:live,format=b,file=heap.hprof <PID>

# Auto-dump on OOM (add to JVM args):
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.hprof

# Via actuator:
curl -o heap.hprof http://localhost:8080/actuator/heapdump
```

**Analyze with Eclipse MAT (Memory Analyzer Tool):**
```
1. Open heap.hprof in MAT
2. Check "Leak Suspects Report" (auto-generated)
3. Look at "Dominator Tree" — largest objects
4. Check "Histogram" — object count by class

Common leaks:
- HashMap/ArrayList growing unbounded → missing eviction
- Static collections → never garbage collected
- Unclosed streams/connections → resource leak
- ThreadLocal not cleaned → memory leak per thread
- Event listeners not deregistered → reference leak
```

---

## Scenario Questions

### Q9. Your service is returning 503s intermittently. How do you diagnose?

**Answer:**
```
503 = Service Unavailable → the service is overloaded or a dependency is down

1. Check Grafana: Is it ALL instances or just one? (instance-level dashboard)
2. Check if a downstream service is down → Circuit breaker open?
3. Check thread pool exhaustion:
   - Tomcat: max-threads (default 200) all busy?
   - HikariCP: all connections in use?
4. Check logs for: "Connection pool timeout", "TaskRejectedException"
5. Check K8s: Is the pod being OOMKilled? (kubectl describe pod)
6. Check GC: Full GC pauses causing request timeouts?

Fix hierarchy:
  - Immediate: Scale up pods (HPA), increase pool sizes
  - Short-term: Add circuit breaker + fallback for flaky dependency
  - Long-term: Fix root cause (slow query, memory leak, missing cache)
```

---

### Q10. How do you set up alerting for a microservices system?

**Answer:**

```yaml
# Prometheus alerting rules:
groups:
  - name: api-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Error rate > 1% for {{ $labels.instance }}"

      - alert: HighLatency
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning

      - alert: PodMemoryHigh
        expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.85
        for: 10m
```

**Alert routing (PagerDuty / Slack):**
```
Critical (5xx spike, service down) → PagerDuty → on-call engineer
Warning (high latency, memory 85%) → Slack #alerts channel
Info (deployment, scaling event) → Slack #deployments channel
```

---

### Q11. Structured Logging Best Practices

**Answer:**

```java
// ❌ BAD — unstructured, can't search/filter:
log.info("Order created for user " + userId + " with total " + total);

// ✅ GOOD — structured with key-value pairs:
log.info("Order created. userId={}, orderId={}, total={}, paymentMethod={}",
         userId, orderId, total, paymentMethod);

// ✅ BEST — structured JSON (for ELK):
// With Logstash encoder, this becomes searchable JSON fields
```

**Log levels:**
| Level | When | Example |
|-------|------|---------|
| `ERROR` | Something failed, needs attention | Payment gateway timeout, data corruption |
| `WARN` | Unexpected but handled | Retry succeeded on 2nd attempt, cache miss |
| `INFO` | Business events | Order created, user logged in, payment processed |
| `DEBUG` | Developer detail | SQL query, request/response body, cache hit/miss |
| `TRACE` | Very verbose | Method entry/exit, variable values |

**Production: INFO level. Never log sensitive data (passwords, tokens, PII, card numbers).**

---

### Q12. What is OpenTelemetry (OTel)? Why is it the future?

**Answer:**
OpenTelemetry is a **vendor-neutral** standard for logs, metrics, and traces. One SDK, export to any backend.

```
Before OTel: Each tool had its own SDK
  Jaeger SDK → Jaeger
  Prometheus SDK → Prometheus
  Datadog SDK → Datadog
  → Locked into one vendor!

With OTel: One SDK, any backend
  App → OTel SDK → OTel Collector → Jaeger / Prometheus / Datadog / whatever
  → Switch backends without changing code!
```

**Spring Boot 3 uses Micrometer + OTel bridge** — already aligned with OTel standards.

---

## Quick Reference

| Tool | Purpose | Spring Boot Integration |
|------|---------|------------------------|
| ELK / Splunk | Log aggregation & search | Logstash encoder + Filebeat |
| Prometheus | Metrics collection (TSDB) | `micrometer-registry-prometheus` |
| Grafana | Metrics dashboards & alerts | Connects to Prometheus |
| Jaeger / Zipkin | Distributed tracing | `micrometer-tracing-bridge-otel` |
| MDC | Correlation IDs in logs | `MDC.put("correlationId", id)` |
| Actuator | Health, metrics, thread dump | `spring-boot-starter-actuator` |
| async-profiler | CPU flame graphs | Attach to running JVM |
| Eclipse MAT | Heap dump analysis | Analyze .hprof files |
