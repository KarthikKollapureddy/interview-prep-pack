# AWS Services Breadth — Interview Q&A

> 12 questions covering SQS/SNS, ECS/EKS, RDS, DynamoDB, IAM, and service comparisons  
> Priority: **P1** — FedEx is heavy AWS. "Which AWS service would you use for X?" is common

---

### Q1. AWS services cheat sheet — when to use what.

**Answer:**

| Need | AWS Service | Key Fact |
|------|-------------|----------|
| **Compute** | EC2 | Virtual servers, full control |
| | ECS (Fargate) | Docker containers, serverless |
| | EKS | Managed Kubernetes |
| | Lambda | Serverless functions, event-driven |
| **Database** | RDS | Managed MySQL/Postgres/Oracle |
| | Aurora | MySQL/Postgres compatible, 5x faster, auto-scaling |
| | DynamoDB | NoSQL key-value, single-digit ms latency |
| | ElastiCache | Managed Redis / Memcached |
| **Messaging** | SQS | Message queue (point-to-point) |
| | SNS | Pub/Sub notifications (fan-out) |
| | EventBridge | Event bus (event-driven architecture) |
| | MSK | Managed Kafka |
| **Storage** | S3 | Object storage (files, images, backups) |
| | EBS | Block storage (EC2 disk) |
| | EFS | Shared file system (NFS) |
| **Networking** | ALB/NLB | Load balancers (HTTP/TCP) |
| | Route 53 | DNS + routing |
| | CloudFront | CDN |
| | API Gateway | REST/WebSocket API management |
| **Security** | IAM | Users, roles, policies |
| | Cognito | User auth (sign-up, login, OAuth) |
| | Secrets Manager | Store API keys, DB passwords |
| | KMS | Encryption key management |
| **Monitoring** | CloudWatch | Logs, metrics, alarms |
| | X-Ray | Distributed tracing |

---

### Q2. SQS vs SNS vs EventBridge — when to use which?

**Answer:**

```
SQS (Simple Queue Service) — Point-to-point message queue:
  Producer → [Queue] → Consumer
  - One message consumed by ONE consumer (then deleted)
  - Decouples producer/consumer
  - Guaranteed delivery (at-least-once)
  - Built-in retry + Dead Letter Queue (DLQ)

SNS (Simple Notification Service) — Pub/Sub fan-out:
  Publisher → [Topic] → Subscriber-1 (SQS)
                      → Subscriber-2 (Lambda)
                      → Subscriber-3 (Email)
  - One message delivered to ALL subscribers
  - Push-based (SNS pushes to subscribers)
  - No persistence (if subscriber is down, message lost unless backed by SQS)

EventBridge — Event bus (event-driven architecture):
  Source → [Event Bus] → Rule-1 → Target-1 (Lambda)
                       → Rule-2 → Target-2 (SQS)
  - Content-based routing (rules filter events by content)
  - Schema registry
  - Best for event-driven architectures
```

**Common pattern — SNS + SQS (fan-out with guaranteed delivery):**
```
Order Service → SNS Topic "order-created"
                  ├── SQS: Payment Queue → Payment Service
                  ├── SQS: Notification Queue → Email Service
                  └── SQS: Analytics Queue → Analytics Service

Each service gets its OWN queue (decoupled, independent retry, own DLQ)
```

| Feature | SQS | SNS | EventBridge | Kafka (MSK) |
|---------|:---:|:---:|:---:|:---:|
| Pattern | Queue (1:1) | Pub/Sub (1:N) | Event Bus (rules) | Log (1:N, replay) |
| Ordering | FIFO optional | ❌ | ❌ | ✅ Per partition |
| Replay | ❌ (message deleted) | ❌ | Archive + replay | ✅ (retention) |
| Throughput | High | Very high | High | Very high |
| Cost | Per-message | Per-message | Per-event | Per-broker/hour |
| Best for | Task queues, job processing | Notifications, fan-out | Event-driven micro | High-throughput streaming |

---

### Q3. SQS in depth — Standard vs FIFO, DLQ, visibility timeout.

**Answer:**

```
Standard Queue:
  - Nearly unlimited throughput
  - At-least-once delivery (may get duplicates)
  - Best-effort ordering (messages may arrive out of order)

FIFO Queue:
  - Exactly-once processing
  - Strict ordering (within message group)
  - 300 messages/sec (3000 with batching)
  - Name must end in .fifo
```

**Key concepts:**
```
Visibility Timeout:
  1. Consumer receives message → message becomes "invisible" for 30s (default)
  2. Consumer processes message → deletes it → done
  3. If consumer crashes → message becomes visible again after timeout → another consumer picks it up
  → Set visibility timeout > max processing time

Dead Letter Queue (DLQ):
  - After N failed attempts (maxReceiveCount), message moves to DLQ
  - DLQ is just another SQS queue
  - Monitor DLQ for failed messages → investigate, fix, replay

Long Polling (receiveMessageWaitTimeSeconds = 20):
  - Without: consumer polls every 1s → many empty responses → wasted API calls
  - With: consumer waits up to 20s for a message → fewer calls, lower cost
```

```java
// Spring Boot + SQS with Spring Cloud AWS:
@SqsListener("order-processing-queue")
public void handleOrder(OrderMessage message) {
    log.info("Processing order: {}", message.getOrderId());
    orderService.process(message);
    // If no exception → message auto-deleted
    // If exception → message returns to queue after visibility timeout
}
```

---

### Q4. ECS vs EKS vs Lambda — container and compute comparison.

**Answer:**

| Feature | ECS (Fargate) | EKS | Lambda |
|---------|:---:|:---:|:---:|
| **What** | Managed Docker containers | Managed Kubernetes | Serverless functions |
| **Infra management** | None (Fargate) | Moderate (K8s complexity) | None |
| **Scaling** | Auto (Service Auto Scaling) | Auto (HPA + Cluster Autoscaler) | Auto (per-invocation) |
| **Cold start** | ~30s (container pull) | ~30s | ~1-5s (Java can be 10s+) |
| **Cost model** | Per vCPU/memory/hour | EC2 instances + K8s fee | Per invocation + duration |
| **Long-running** | ✅ Yes | ✅ Yes | ❌ Max 15 min |
| **Startup latency** | Medium | Medium | Low (but cold starts) |
| **Best for** | Containerized microservices | Complex K8s workloads, portability | Event handlers, APIs, cron |

**Decision:**
```
Lambda when:
  ✅ Event-driven (S3 upload trigger, API Gateway, SQS consumer)
  ✅ Sporadic traffic (pay nothing when idle)
  ✅ Simple functions (< 15 min)
  ❌ Not for: long-running, high-throughput, JVM cold start sensitive

ECS Fargate when:
  ✅ Containerized Spring Boot microservices
  ✅ Don't want to manage K8s
  ✅ Steady traffic (always-on services)
  ✅ Simple deployment model

EKS when:
  ✅ Already using Kubernetes
  ✅ Need multi-cloud portability
  ✅ Complex orchestration (CronJobs, StatefulSets, custom operators)
  ✅ Team has K8s expertise
```

---

### Q5. RDS vs Aurora vs DynamoDB — database decision.

**Answer:**

| Feature | RDS (MySQL/Postgres) | Aurora | DynamoDB |
|---------|:---:|:---:|:---:|
| **Type** | Relational (managed) | Relational (AWS-optimized) | NoSQL (key-value/document) |
| **Performance** | Standard | 5x MySQL, 3x Postgres | Single-digit ms at any scale |
| **Scaling** | Vertical + read replicas | Auto-scaling storage + 15 replicas | Auto-scaling (on-demand or provisioned) |
| **Max storage** | 64 TB | 128 TB (auto-grows) | Unlimited |
| **Cost** | Low | Higher (but worth it for scale) | Per RCU/WCU or on-demand |
| **ACID** | ✅ Full SQL | ✅ Full SQL | ✅ Per-item + transactions |
| **Schema** | Fixed | Fixed | Flexible (schemaless) |
| **Best for** | Standard apps, existing MySQL/PG | High-scale relational | Key-value lookups, high write throughput |

**DynamoDB key concepts:**
```
Partition Key: Hash key for data distribution (e.g., userId)
Sort Key: Range within partition (e.g., timestamp)
GSI (Global Secondary Index): Query on non-key attributes
LSI (Local Secondary Index): Alternate sort key within same partition

Capacity modes:
  On-Demand: Pay per request (unpredictable traffic)
  Provisioned: Set RCU/WCU (predictable, cheaper)

Single-table design: Store multiple entity types in one table
  PK = "USER#123",   SK = "PROFILE"        → user profile
  PK = "USER#123",   SK = "ORDER#456"      → user's order
  PK = "ORDER#456",  SK = "ITEM#789"       → order item
```

---

### Q6. IAM — Roles, Policies, and the Principle of Least Privilege.

**Answer:**

```
IAM Hierarchy:
  Account
  └── Users (developers, admins)
  └── Groups (dev-team, ops-team)
  └── Roles (assumed by services, EC2, Lambda)
  └── Policies (JSON permission documents)

Key concepts:
  - User: Human identity with long-term credentials
  - Role: Assumed by services (EC2, Lambda, ECS) — temporary credentials
  - Policy: JSON document defining permissions
  - Principle of Least Privilege: Give MINIMUM permissions needed
```

```json
// Example: Lambda role policy — read from S3 + write to DynamoDB:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::my-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": ["dynamodb:PutItem", "dynamodb:GetItem"],
      "Resource": "arn:aws:dynamodb:us-east-1:123456:table/Orders"
    }
  ]
}
```

**Interview tips:**
- Never use root account for daily work
- Never hardcode AWS credentials in code — use IAM roles
- Use Secrets Manager or Parameter Store for API keys
- EC2/ECS/Lambda get credentials via instance role (automatic)

---

### Q7. Explain ALB vs NLB.

**Answer:**

| Feature | ALB (Application LB) | NLB (Network LB) |
|---------|:---:|:---:|
| Layer | Layer 7 (HTTP/HTTPS) | Layer 4 (TCP/UDP) |
| Routing | Path, host, header, query string | Port-based |
| Protocol | HTTP, HTTPS, WebSocket | TCP, UDP, TLS |
| Performance | Good | Ultra-low latency, millions RPS |
| Health checks | HTTP status code | TCP connection |
| SSL termination | ✅ Yes | ✅ Yes |
| Static IP | ❌ (use DNS) | ✅ Elastic IP |
| Use case | Web apps, REST APIs, microservices | gRPC, gaming, IoT, extreme throughput |

```
Spring Boot microservices behind ALB:
  Client → Route 53 (DNS) → ALB → Target Group → ECS Tasks (port 8080)
  
  ALB rules:
    /api/orders/*  → Order Service target group
    /api/users/*   → User Service target group
    /api/payments/* → Payment Service target group
```

---

### Q8. How does AWS Secrets Manager work with Spring Boot?

**Answer:**

```xml
<!-- Spring Cloud AWS: -->
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-secrets-manager</artifactId>
</dependency>
```

```yaml
# application.yml — reference secret by name:
spring:
  config:
    import: aws-secretsmanager:/prod/myapp/db-credentials

  datasource:
    url: jdbc:mysql://mydb.cluster.us-east-1.rds.amazonaws.com:3306/mydb
    username: ${db-username}     # pulled from Secrets Manager
    password: ${db-password}     # pulled from Secrets Manager
```

**Why Secrets Manager over env variables?**
- Auto-rotation (rotate DB password without redeploying)
- Encrypted at rest (KMS)
- Audit trail (CloudTrail logs who accessed what)
- Cross-account sharing

---

### Q9. CloudWatch — Logs, Metrics, Alarms.

**Answer:**

```
CloudWatch Logs:
  - Application logs (stdout from ECS/Lambda)
  - Log Groups → Log Streams → Log Events
  - Insights: query logs with SQL-like syntax
  - Retention: configure per group (1 day → forever)

CloudWatch Metrics:
  - AWS service metrics (CPU, memory, request count)
  - Custom metrics (business KPIs)
  - Namespace → Metric → Dimensions → Datapoints

CloudWatch Alarms:
  - Trigger on metric threshold
  - Actions: SNS notification, Auto Scaling, Lambda
  - States: OK → ALARM → INSUFFICIENT_DATA
```

```bash
# CloudWatch Insights query — find slow API calls:
fields @timestamp, @message
| filter @message like /took \d{4,}ms/
| sort @timestamp desc
| limit 20

# Error rate last 1 hour:
fields @timestamp
| filter @message like /ERROR/
| stats count() as errorCount by bin(5m)
```

```java
// Custom CloudWatch metric from Spring Boot:
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;

    public void recordOrderCreated(String region) {
        registry.counter("orders.created", "region", region).increment();
    }
}
// With micrometer-registry-cloudwatch dependency, metrics auto-publish
```

---

### Q10. API Gateway — features and patterns.

**Answer:**

```
AWS API Gateway types:
  REST API     — full-featured (caching, WAF, usage plans, API keys)
  HTTP API     — lighter, cheaper, faster (recommended for most)
  WebSocket API — real-time two-way communication

Common pattern — API Gateway + Lambda:
  Client → API Gateway → Lambda → DynamoDB
  
  API Gateway handles:
    ✅ Authentication (Cognito, JWT, custom authorizer)
    ✅ Rate limiting (per-client usage plans)
    ✅ Request/response transformation
    ✅ Caching (reduce Lambda invocations)
    ✅ CORS
    ✅ Throttling (burst + steady-state limits)
```

```
API Gateway vs Spring Cloud Gateway:
  AWS API Gateway: managed, serverless, good with Lambda
  Spring Cloud Gateway: self-hosted, runs on WebFlux, full control, works with any backend
  
  Use AWS API Gateway when: Lambda + serverless architecture
  Use Spring Cloud Gateway when: ECS/EKS microservices, need custom filters
```

---

### Q11. AWS Well-Architected Framework — 6 pillars.

**Answer:**

| Pillar | Key Principles | Example |
|--------|---------------|---------|
| **Operational Excellence** | Automate, observe, improve | CI/CD pipelines, CloudWatch dashboards |
| **Security** | Least privilege, encrypt, audit | IAM roles, KMS, CloudTrail |
| **Reliability** | Auto-recover, scale, backup | Multi-AZ RDS, auto-scaling, DLQ |
| **Performance Efficiency** | Right-size, experiment | Use Fargate over EC2 if possible |
| **Cost Optimization** | Pay for what you use | Spot instances, Lambda for sporadic, reserved for steady |
| **Sustainability** | Minimize environmental impact | Right-size instances, use serverless |

---

### Q12. Design a typical AWS architecture for a Spring Boot microservices app.

**Answer:**

```
                        ┌──────────────┐
                        │   Route 53   │  (DNS)
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │  CloudFront  │  (CDN for static assets)
                        └──────┬───────┘
                               │
                    ┌──────────▼──────────┐
                    │    ALB (HTTPS)      │  (Application Load Balancer)
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
    ┌─────▼──────┐     ┌──────▼─────┐     ┌───────▼─────┐
    │ Order Svc  │     │ User Svc   │     │Payment Svc  │
    │ (ECS)      │     │ (ECS)      │     │(ECS)        │
    └─────┬──────┘     └──────┬─────┘     └───────┬─────┘
          │                   │                    │
          │     ┌─────────────┼────────────┐       │
          │     │             │            │       │
    ┌─────▼─────▼─┐   ┌──────▼────┐  ┌────▼───────▼──┐
    │ Aurora RDS  │   │ElastiCache │  │   SQS/SNS    │
    │ (Multi-AZ)  │   │(Redis)     │  │  (Messaging) │
    └─────────────┘   └───────────┘  └──────────────┘

Security:
  - IAM roles per service (ECS task roles)
  - Secrets Manager for DB passwords
  - WAF on ALB (SQL injection, rate limiting)
  - VPC with private subnets for services + DB
  - Public subnet only for ALB

Monitoring:
  - CloudWatch Logs (container stdout)
  - CloudWatch Metrics + Alarms
  - X-Ray for distributed tracing
```
