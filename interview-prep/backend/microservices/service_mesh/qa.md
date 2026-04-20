# Service Mesh & Observability — Interview Q&A

> 12 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. What is a Service Mesh? Why do you need one?

A service mesh is an **infrastructure layer** that handles service-to-service communication. Instead of each service implementing retry, circuit breaking, mTLS, and tracing — the mesh handles it transparently via sidecar proxies.

```
Service A → [Envoy Proxy] → [Envoy Proxy] → Service B
              (sidecar)        (sidecar)
```

**What it provides:**
| Feature | Without Mesh | With Mesh (Istio) |
|---------|-------------|-------------------|
| mTLS | Manual cert management | Automatic |
| Retries | Resilience4j in code | Mesh config |
| Circuit breaking | Code-level | Mesh config |
| Observability | Instrument manually | Automatic tracing |
| Traffic splitting | Deployment config | Canary/A-B routing rules |
| Rate limiting | Application code | Mesh policy |

**Popular meshes:** Istio (Envoy sidecar), Linkerd, Consul Connect

---

### Q2. Istio architecture — Control Plane vs Data Plane?

```
                ┌─────────────────────┐
                │   CONTROL PLANE     │
                │  ┌─────────────┐    │
                │  │   istiod     │    │ ← Config, certs, discovery
                │  └─────────────┘    │
                └─────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ↓               ↓               ↓
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ Pod A   │    │ Pod B   │    │ Pod C   │    DATA PLANE
    │ App     │    │ App     │    │ App     │
    │ Envoy ←─┼────┼→Envoy ←─┼────┼→Envoy  │
    └─────────┘    └─────────┘    └─────────┘
```

**Control Plane (istiod):** Manages configuration, distributes certificates, service discovery  
**Data Plane (Envoy):** Sidecar proxy that intercepts all traffic, enforces policies

---

### Q3. Explain observability — the three pillars.

| Pillar | What | Tools |
|--------|------|-------|
| **Logs** | Textual event records | Splunk, ELK, CloudWatch |
| **Metrics** | Numeric measurements over time | Prometheus, AppDynamics, Grafana |
| **Traces** | Request flow across services | Jaeger, Zipkin, AppDynamics |

**At FedEx:** AppDynamics for APM (metrics + traces), Splunk for logs, PCF dashboards for health.

**Structured logging (essential for Splunk):**
```java
log.info("Payment processed", 
    kv("transactionId", txnId),
    kv("amount", amount),
    kv("merchantId", merchantId),
    kv("latencyMs", duration));
// Output: {"message":"Payment processed","transactionId":"T123","amount":500,"latencyMs":45}
```

**Distributed tracing correlates logs across services** using a traceId.

---

## Scenario-Based Questions

### Q4. At FedEx, how do you implement canary deployments with Istio?

```yaml
# Route 95% traffic to v1, 5% to v2 (canary)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: shipment-service
spec:
  hosts:
    - shipment-service
  http:
    - route:
        - destination:
            host: shipment-service
            subset: v1
          weight: 95
        - destination:
            host: shipment-service
            subset: v2
          weight: 5
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: shipment-service
spec:
  host: shipment-service
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```

**Canary process:**
1. Deploy v2 alongside v1
2. Route 5% traffic to v2
3. Monitor error rates and latency in AppDynamics
4. If healthy: gradually increase to 25% → 50% → 100%
5. If unhealthy: route 100% back to v1 (instant rollback)

---

### Q5. At NPCI, how do you ensure zero-downtime deployments?

1. **Rolling update** — Kubernetes replaces pods one by one
2. **Readiness probes** — new pod only gets traffic when ready
3. **Graceful shutdown** — drain in-flight requests before termination
4. **PodDisruptionBudget** — ensure minimum pods always available

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0       # Never have fewer pods than desired
      maxSurge: 1             # Add one new pod before removing old
  template:
    spec:
      terminationGracePeriodSeconds: 60
      containers:
        - name: upi-service
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
```

```java
// Graceful shutdown in Spring Boot
@PreDestroy
void onShutdown() {
    log.info("Received shutdown signal, draining connections...");
    // Spring Boot 2.3+ handles this automatically with:
    // server.shutdown=graceful
    // spring.lifecycle.timeout-per-shutdown-phase=30s
}
```

---

### Q6. How do you set up alerting for your microservices at FedEx?

**AppDynamics alerting strategy:**

| Metric | Warning | Critical | Action |
|--------|---------|----------|--------|
| Error rate | > 1% | > 5% | Page on-call |
| P99 latency | > 2s | > 5s | Scale up, investigate |
| CPU | > 70% | > 90% | Auto-scale |
| Memory | > 75% | > 90% | Investigate leaks |
| Kafka lag | > 1000 | > 10000 | Add consumers |
| Circuit breaker OPEN | - | Any | Investigate dependency |

**Splunk dashboard queries:**
```
index=fedex_sefs sourcetype=application error 
| stats count by service, error_code 
| where count > 100
```

---

## Gotchas & Edge Cases

### Q7. Service mesh overhead — when is it NOT worth it?

- **< 5 microservices** — overhead of mesh management outweighs benefits
- **Latency-critical paths** — each sidecar adds ~1-2ms per hop
- **Simple internal services** — if you don't need mTLS, traffic splitting, or advanced routing
- **Team expertise** — Istio has a steep learning curve

**Alternative for simpler setups:** Spring Cloud Gateway + Resilience4j + Micrometer (no mesh needed).

---

### Q8. Sidecar vs sidecar-less mesh architectures?

| | Sidecar (Istio/Envoy) | Sidecar-less (Ambient mesh) |
|---|---|---|
| Proxy | One per pod | Shared per node |
| Resource usage | Higher (proxy per pod) | Lower |
| Isolation | Per-pod L7 policies | Per-node L4, opt-in L7 |
| Maturity | Mature | Newer |

Istio's **Ambient Mesh** (GA in 2024) reduces overhead by using a shared ztunnel per node for L4, with optional waypoint proxies for L7.

---

### Q9. Spring Cloud Gateway — routing, filters, and rate limiting.

**Answer:**
Spring Cloud Gateway is the API Gateway for Spring microservices, replacing Zuul.

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE         # lb:// enables load balancing via Eureka
          predicates:
            - Path=/api/users/**
            - Method=GET,POST
          filters:
            - StripPrefix=1               # /api/users/1 → /users/1
            - AddRequestHeader=X-Request-Source, gateway
            - name: CircuitBreaker
              args:
                name: userCB
                fallbackUri: forward:/fallback/users
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # 10 requests/sec
                redis-rate-limiter.burstCapacity: 20
```

**Custom Global Filter:**
```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);  // continue to next filter
    }
    @Override
    public int getOrder() { return -1; }  // run before other filters
}
```

---

### Q10. Service Discovery with Eureka — how does it work?

**Answer:**

```
┌─────────────────┐
│  Eureka Server   │ ← Registry of all service instances
│  (Discovery)     │
└────┬────┬────────┘
     │    │
  register│    heartbeat (30s)
     │    │
┌────▼────▼──┐     ┌──────────────┐
│ User Svc   │     │ Order Svc    │
│ Instance 1 │     │ Instance 1,2 │
└────────────┘     └──────────────┘
```

**Eureka Server setup:**
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApp { ... }
```
```yaml
# Eureka Server application.yml:
server.port: 8761
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

**Eureka Client setup:**
```yaml
# Each microservice:
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

**How discovery works:**
1. Service starts → registers with Eureka (name, IP, port)
2. Eureka sends heartbeats every 30s → if missed for 90s, instance removed
3. Other services fetch registry → call by service name, not IP
4. `@LoadBalanced RestTemplate` or `WebClient` resolves service name to IP

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() { return new RestTemplate(); }

// Call by service name:
restTemplate.getForObject("http://USER-SERVICE/users/1", User.class);
// Ribbon/Spring Cloud LoadBalancer resolves USER-SERVICE → 192.168.1.10:8080
```

---

### Q11. Spring Cloud Config Server — centralized configuration.

**Answer:**
Config Server externalizes configuration for all microservices into a Git repo.

```
┌──────────────┐     ┌──────────────┐
│ Config Server │◄───│ Git Repo      │
│ :8888        │     │ (configs)     │
└──────┬───────┘     └──────────────┘
       │
  GET /user-service/dev
       │
┌──────▼───────┐
│ User Service  │ → gets user-service-dev.yml from Git via Config Server
└──────────────┘
```

**Config Server:**
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApp { ... }
```
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/config-repo
          default-label: main
```

**Client bootstrap.yml:**
```yaml
spring:
  application:
    name: user-service
  config:
    import: configserver:http://localhost:8888
  profiles:
    active: dev
```

**Dynamic refresh with `@RefreshScope`:**
```java
@RefreshScope   // re-reads config on /actuator/refresh POST
@RestController
public class AppController {
    @Value("${feature.new-ui.enabled}")
    private boolean newUiEnabled;
}
// POST /actuator/refresh → re-injects all @Value in @RefreshScope beans
// For broadcast: Spring Cloud Bus + RabbitMQ/Kafka → refreshes ALL instances
```
