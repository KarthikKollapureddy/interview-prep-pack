# Microservices Design Patterns — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What are microservices? How do they differ from monolithic architecture?

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| Deployment | Single artifact | Each service deployed independently |
| Scaling | Scale entire app | Scale individual services |
| Database | Shared DB | Database per service |
| Team structure | One team / shared codebase | Autonomous teams per service |
| Failure blast radius | One failure brings down everything | Failure isolated to one service |
| Technology | One tech stack | Polyglot (each service can use different tech) |

**At FedEx:** SEFS-PDDV is a microservice — handles scan events independently of shipping rate calculation or customer notification services.

---

### Q2. Explain the key microservices design patterns.

| Pattern | Problem | Solution |
|---------|---------|---------|
| **API Gateway** | Clients need a single entry point | Kong, Spring Cloud Gateway routes to services |
| **Service Discovery** | Services need to find each other | Eureka, Consul — register and discover |
| **Circuit Breaker** | Cascading failures | Resilience4j — fail fast when downstream is down |
| **Saga** | Distributed transactions | Choreography (events) or Orchestration (coordinator) |
| **CQRS** | Read/write scalability | Separate read and write models |
| **Event Sourcing** | Audit trail, state reconstruction | Store events, not state |
| **Strangler Fig** | Monolith migration | Gradually replace monolith endpoints with microservices |
| **Sidecar** | Cross-cutting concerns | Envoy proxy for logging, auth, metrics |
| **Bulkhead** | Resource isolation | Thread pool per dependency |

---

### Q3. API Gateway pattern — what does it do?

```
Client → API Gateway → Microservice A
                     → Microservice B
                     → Microservice C
```

**Responsibilities:**
1. **Routing** — route requests to correct service
2. **Load balancing** — distribute across instances
3. **Authentication** — validate JWT before forwarding
4. **Rate limiting** — throttle per client
5. **Request aggregation** — combine multiple service calls into one response
6. **SSL termination** — handle HTTPS

```yaml
# Spring Cloud Gateway config
spring:
  cloud:
    gateway:
      routes:
        - id: shipment-service
          uri: lb://SHIPMENT-SERVICE
          predicates:
            - Path=/api/v1/shipments/**
          filters:
            - StripPrefix=0
            - name: CircuitBreaker
              args:
                name: shipmentCB
                fallbackUri: forward:/fallback/shipments
```

**At FedEx PCF:** Spring Cloud Gateway sits in front of SEFS-PDDV and other services.

---

### Q4. Explain the Saga pattern for distributed transactions.

**Problem:** In microservices, you can't use traditional ACID transactions across services (no shared DB).

**Example — Order placement at Hatio:**
1. Order Service: Create order
2. Payment Service: Charge customer
3. Inventory Service: Reserve stock
4. Notification Service: Send confirmation

**If step 3 fails, you need compensating transactions (rollback step 2, 1).**

**Choreography (event-driven):**
```
OrderService publishes "OrderCreated"
  → PaymentService listens, charges, publishes "PaymentCompleted"
    → InventoryService listens, reserves, publishes "StockReserved"
      → NotificationService sends confirmation

On failure:
InventoryService publishes "StockReservationFailed"
  → PaymentService listens, refunds, publishes "PaymentRefunded"
    → OrderService listens, cancels order
```

**Orchestration (central coordinator):**
```java
public class OrderSagaOrchestrator {
    public void execute(Order order) {
        try {
            paymentService.charge(order);
            inventoryService.reserve(order);
            notificationService.notify(order);
        } catch (InventoryException e) {
            paymentService.refund(order);  // Compensating transaction
            order.setStatus(CANCELLED);
        }
    }
}
```

| | Choreography | Orchestration |
|---|---|---|
| Coupling | Loose | Tighter (coordinator knows all steps) |
| Debugging | Hard (events scattered) | Easier (central flow) |
| Use when | Simple flows, few steps | Complex flows, many steps |

---

## Scenario-Based Questions

### Q5. At FedEx, how would you decompose a shipping monolith into microservices?

**Domain decomposition using Bounded Contexts (DDD):**

```
Shipping Monolith → decompose into:

1. Shipment Service       — create/track shipments, STARV scan events
2. Rate Calculation Service — calculate shipping costs
3. Customer Service       — profiles, addresses, preferences
4. Notification Service   — email, SMS, push notifications
5. Billing Service        — invoicing, payment processing
6. Tracking Service       — real-time package location
7. Analytics Service      — dashboards, PDDV reports
```

**Migration strategy (Strangler Fig):**
1. Identify the most independent module (e.g., Notification)
2. Build it as a new microservice
3. Route traffic to new service via API Gateway
4. Repeat for next module
5. Eventually decommission the monolith

**Data decomposition:** Each service owns its data. Use events to sync. `Shipment Service` publishes `ShipmentCreated` → `Notification Service` and `Analytics Service` consume it.

---

### Q6. At NPCI, how do you handle service-to-service communication?

**Synchronous (REST/gRPC):**
```java
// Using WebClient (non-blocking)
@Service
public class AccountService {
    private final WebClient webClient;
    
    public Mono<AccountDetails> getAccount(String accountId) {
        return webClient.get()
            .uri("/api/accounts/{id}", accountId)
            .retrieve()
            .bodyToMono(AccountDetails.class)
            .timeout(Duration.ofSeconds(2));
    }
}
```

**Asynchronous (Kafka events):**
```java
// Publisher
kafkaTemplate.send("payment-events", new PaymentCompletedEvent(txnId, amount));

// Consumer
@KafkaListener(topics = "payment-events")
void handlePaymentCompleted(PaymentCompletedEvent event) {
    ledgerService.recordCredit(event);
}
```

| | Sync (REST) | Async (Kafka) |
|---|---|---|
| Coupling | Temporal (both must be up) | Decoupled |
| Latency | Adds up (call chain) | Eventually consistent |
| Use for | Queries, real-time needs | Events, notifications, data sync |

**NPCI recommendation:** Async for payment processing (reliability > latency). Sync for balance inquiries (need real-time data).

---

### Q7. How do you implement distributed tracing across microservices?

```yaml
# Spring Boot 3.x with Micrometer Tracing (replaces Sleuth)
management:
  tracing:
    sampling:
      probability: 1.0  # 100% of requests traced (lower in prod)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**How it works:**
1. API Gateway generates a `traceId` (propagated to all downstream calls)
2. Each service generates a `spanId` for its work
3. Headers propagated: `traceparent: 00-traceId-spanId-01`
4. All logs include traceId → search Splunk by traceId to see full request flow

```
[TraceId: abc123] Gateway → ShipmentService → InventoryService → DB
                                             → NotificationService → Kafka
```

**At FedEx:** AppDynamics provides distributed tracing + business transaction correlation.

---

## Coding Challenges

### Challenge 1: Saga Orchestrator
**File:** `solutions/SagaOrchestrator.java`  
Implement an order saga with compensating transactions:
1. OrderService.create → PaymentService.charge → InventoryService.reserve
2. If any step fails, execute compensating transactions in reverse
3. Track saga state (STARTED, PAYMENT_DONE, INVENTORY_RESERVED, COMPLETED, COMPENSATING, FAILED)
4. Demo with success and failure scenarios

### Challenge 2: Service Registry Simulation
**File:** `solutions/ServiceRegistry.java`  
Build a simple in-memory service registry:
1. Register/deregister service instances
2. Health check (heartbeat-based eviction)
3. Client-side load balancing (round-robin)
4. Simulate service failure and recovery

---

## Gotchas & Edge Cases

### Q8. What is the dual-write problem?

**Problem:** Writing to DB and publishing to Kafka — if one succeeds and the other fails, data is inconsistent.

```java
// ❌ BAD: Dual write
orderRepository.save(order);           // 1. Write to DB
kafkaTemplate.send("orders", order);   // 2. Send to Kafka — what if this fails?
```

**Solutions:**
1. **Transactional Outbox** — write event to an outbox table in the same DB transaction. A separate process polls outbox and publishes to Kafka.
2. **Change Data Capture (CDC)** — Debezium reads DB transaction log and publishes to Kafka.
3. **Event Sourcing** — event log IS the source of truth.

---

### Q9. Database per service — how do you handle queries that need data from multiple services?

**Options:**
1. **API Composition** — API Gateway or composite service calls multiple services and combines results
2. **CQRS** — maintain a read-optimized view that subscribes to events from multiple services
3. **Shared read-only replica** — services can read from a shared read DB (write to their own)
