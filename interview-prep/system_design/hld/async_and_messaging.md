# Asynchronism & Messaging Patterns — Interview Q&A

> Concepts from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
> Covers: Message Queues, Task Queues, Event-Driven Architecture, Back Pressure, Kafka
> **Priority: P0** — Async processing is core to every scalable system

---

## Q1. Why Asynchronism? When to use async processing?

```
Problem: Some operations are TOO SLOW for synchronous request-response

Synchronous (bad for slow operations):
  User → Request → [Process 5 seconds] → Response
  User waits 5 seconds. Bad UX!

Asynchronous (decouple producer from consumer):
  User → Request → Queue → Response "We're working on it!"
  Background worker → Process → Notify user

Use async when:
  ✓ Operation takes > 1 second
  ✓ Operation can fail and needs retry
  ✓ Result doesn't need to be immediate
  ✓ Need to decouple services
  ✓ Need to smooth out traffic spikes

Examples:
  - Sending emails/SMS after registration
  - Processing uploaded images (resize, thumbnail)
  - Generating reports
  - Indexing data in Elasticsearch
  - Sending push notifications
  - Processing payment settlements (batch)

Two approaches:
  1. Message Queues: deferred work (do it later)
  2. Pre-computation: do work in advance (periodic aggregation)
```

---

## Q2. Message Queues — How do they work?

```
Message Queue = buffer that receives, holds, and delivers messages

Producer → Queue → Consumer

┌──────────┐    ┌───────────────────┐    ┌──────────┐
│ Producer │───→│   Message Queue   │───→│ Consumer │
│ (App)    │    │ ┌───┬───┬───┬───┐ │    │ (Worker) │
└──────────┘    │ │ M4│ M3│ M2│ M1│ │    └──────────┘
                │ └───┴───┴───┴───┘ │
                └───────────────────┘

Flow:
  1. Producer publishes a message (job) to the queue
  2. Queue holds the message durably
  3. Consumer picks up the message and processes it
  4. Consumer acknowledges completion
  5. Queue removes the message

Properties:
  - Messages persist even if consumer is down
  - Multiple consumers can process messages (competing consumers)
  - Messages processed AT LEAST once (need idempotency)
  - Order may or may not be guaranteed (depends on implementation)
```

### Message Queue Comparison
```
┌────────────────┬────────────────────────────────────────────────┐
│ Technology     │ Characteristics                                │
├────────────────┼────────────────────────────────────────────────┤
│ RabbitMQ       │ - AMQP protocol                                │
│                │ - Flexible routing (direct, topic, fanout)      │
│                │ - Message acknowledgment                        │
│                │ - Good for task queues                          │
│                │ - Complex to manage at scale                    │
│                │                                                │
│ Apache Kafka   │ - Distributed commit log                        │
│                │ - Extremely high throughput (millions/sec)       │
│                │ - Messages retained for configurable time        │
│                │ - Consumer groups for parallel processing        │
│                │ - Great for event streaming                     │
│                │ - Ordered within partitions                     │
│                │                                                │
│ Amazon SQS     │ - Fully managed (no infrastructure)             │
│                │ - Standard: at-least-once, best-effort order    │
│                │ - FIFO: exactly-once, strict order              │
│                │ - Can have higher latency                       │
│                │ - Pay per message                               │
│                │                                                │
│ Redis (Pub/Sub │ - Simple message broker                         │
│  + Streams)    │ - Pub/Sub: fire-and-forget (no persistence)     │
│                │ - Streams: persistent, consumer groups (5.0+)   │
│                │ - Lower throughput than Kafka                   │
│                │ - Good for real-time notifications              │
└────────────────┴────────────────────────────────────────────────┘
```

---

## Q3. Apache Kafka Deep Dive — Architecture & Use Cases.

```
Kafka = Distributed, partitioned, replicated commit log

Architecture:
  ┌─────────────────────────────────────────────────────┐
  │                   Kafka Cluster                     │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
  │  │ Broker 1 │  │ Broker 2 │  │ Broker 3 │          │
  │  │          │  │          │  │          │          │
  │  │ Topic A  │  │ Topic A  │  │ Topic A  │          │
  │  │ Part 0   │  │ Part 1   │  │ Part 2   │          │
  │  │ (Leader) │  │ (Leader) │  │ (Leader) │          │
  │  │          │  │          │  │          │          │
  │  │ Topic B  │  │ Topic B  │  │ Topic B  │          │
  │  │ Part 0   │  │ Part 1   │  │ Part 0   │          │
  │  │ (Replica)│  │ (Leader) │  │ (Leader) │          │
  │  └──────────┘  └──────────┘  └──────────┘          │
  └─────────────────────────────────────────────────────┘

Key Concepts:
  Topic:     Logical channel (e.g., "order-events")
  Partition: Ordered, immutable sequence within a topic
  Offset:    Position of message within a partition
  Broker:    Single Kafka server
  Producer:  Publishes messages to topics
  Consumer:  Reads messages from topics
  Consumer Group: Set of consumers that share partitions

Message Ordering:
  - Guaranteed WITHIN a partition
  - NOT guaranteed across partitions
  - Use same partition key for ordering needs
    Example: user_id as key → all events for user in same partition

Delivery Guarantees:
  - At most once:  read, commit offset, process (may lose)
  - At least once:  read, process, commit offset (may duplicate)
  - Exactly once:  idempotent producer + transactional consumer

Use Cases:
  ✓ Event streaming (clickstream, user activity)
  ✓ Log aggregation (centralized logging)
  ✓ Change Data Capture (DB → Kafka → downstream)
  ✓ Metrics collection
  ✓ Stream processing (Kafka Streams, Apache Flink)
  ✓ Microservice communication (event-driven)
```

---

## Q4. Task Queues — Processing compute-heavy jobs.

```
Task Queue = receives tasks + data, processes them, returns results

Different from Message Queue:
  Message Queue: simple message passing
  Task Queue: executes code/functions, returns results

  ┌──────────┐   submit task   ┌────────────┐
  │   App    │────────────────→│ Task Queue │
  │          │                 │  (Celery)  │
  └──────────┘                 └─────┬──────┘
                                     │ dispatch
                               ┌─────┴──────┐
                               │  Workers   │
                               │  ┌──┐ ┌──┐ │
                               │  │W1│ │W2│ │
                               │  └──┘ └──┘ │
                               └────────────┘

Popular Task Queue Frameworks:
  - Celery (Python): scheduling, retries, result backend
  - Sidekiq (Ruby): Redis-backed, multi-threaded
  - Bull (Node.js): Redis-backed job queue

Example — Image Processing Pipeline:
  1. User uploads photo
  2. API returns "Processing..." immediately
  3. Task queued: {task: "process_image", image_id: "abc123"}
  4. Worker picks up task:
     a. Resize to multiple sizes
     b. Generate thumbnails
     c. Apply filters
     d. Upload to S3
  5. Update DB: image status = "ready"
  6. Notify user via WebSocket/push

Use Cases:
  ✓ Image/video processing
  ✓ Sending emails in bulk
  ✓ Report generation
  ✓ Data import/export
  ✓ Scheduled jobs (cron replacement)
  ✓ ML model inference
```

---

## Q5. Back Pressure — Protecting systems from overload.

```
Problem: Producer is faster than consumer → queue grows unbounded
  → Queue exceeds memory → cache misses → disk reads → SLOWER

Back Pressure = limiting queue size to maintain quality of service

  ┌──────────┐ 10K msg/s  ┌──────────┐ 1K msg/s  ┌──────────┐
  │ Producer │────────────→│  Queue   │───────────→│ Consumer │
  │          │             │ FULL!    │            │          │
  └──────────┘             └──────────┘            └──────────┘
       ↑                        │
       │    "Queue full, slow   │
       │     down or retry"     │
       └────────────────────────┘

Strategies:

1. Queue Size Limit:
   - Set max queue size
   - When full, reject new messages (HTTP 503)
   - Producer retries with exponential backoff

2. Rate Limiting:
   - Limit producer's publish rate
   - Token bucket / leaky bucket algorithms
   - Return 429 Too Many Requests

3. Load Shedding:
   - Drop lower-priority messages when overloaded
   - Process critical messages first
   - Example: drop analytics events, keep payment events

4. Scaling Consumers:
   - Auto-scale consumers based on queue depth
   - Kafka: add consumers to consumer group
   - SQS: CloudWatch alarm → trigger Lambda/ECS scaling

5. Circuit Breaker:
   - If downstream is failing, stop sending requests
   - Fail fast instead of queuing indefinitely
   - States: Closed → Open → Half-Open

Exponential Backoff:
  retry_delay = base_delay × 2^attempt + random_jitter
  Attempt 1: 1s, Attempt 2: 2s, Attempt 3: 4s, ... max 60s
  Jitter prevents thundering herd on retry
```

---

## Q6. Event-Driven Architecture — Patterns.

```
Services communicate through EVENTS rather than direct calls.

Pattern 1: Event Notification
  ┌─────────┐  "OrderPlaced"  ┌───────────────┐
  │ Order   │────────────────→│ Notification  │
  │ Service │    event bus    │ Service       │
  └─────────┘                 └───────────────┘
                              ┌───────────────┐
                    ────────→│ Inventory     │
                              │ Service       │
                              └───────────────┘

Pattern 2: Event-Carried State Transfer
  Event contains ALL data needed (no callback to source)
  {"event": "UserUpdated", "user": {id, name, email, ...}}
  Consumers update their LOCAL copy of data

Pattern 3: Event Sourcing
  Store ALL events as source of truth (not just current state)
  State is derived by replaying events

  Events:
    1. AccountCreated(id=123, balance=0)
    2. MoneyDeposited(id=123, amount=1000)
    3. MoneyWithdrawn(id=123, amount=200)

  Current state: balance = 0 + 1000 - 200 = 800

  Benefits:
    ✓ Complete audit trail
    ✓ Can rebuild state at any point in time
    ✓ Can add new projections/views later
    ✓ Natural fit for CQRS

Pattern 4: CQRS (Command Query Responsibility Segregation)
  ┌─────────────┐               ┌──────────────┐
  │  Commands   │──→ Write DB ──→│   Events     │
  │ (Writes)    │   (Master)    │   (Kafka)    │
  └─────────────┘               └──────┬───────┘
                                       │
  ┌─────────────┐               ┌──────┴───────┐
  │  Queries    │←── Read DB  ←──│  Projector   │
  │ (Reads)     │  (Optimized)  │              │
  └─────────────┘               └──────────────┘

  Write model: normalized, optimized for writes
  Read model: denormalized, optimized for queries
  Events sync write model → read model
```

---

## Q7. Pub/Sub vs Point-to-Point — Messaging patterns.

```
POINT-TO-POINT (Queue):
  - One producer, one consumer per message
  - Message consumed by ONE consumer
  - Good for task distribution

  Producer → Queue → Consumer 1 (gets M1)
                   → Consumer 2 (gets M2)
                   → Consumer 3 (gets M3)

  Example: Order processing — each order processed by one worker

─────────────────────────────────────────────────────

PUB/SUB (Topic):
  - One producer, MANY subscribers
  - ALL subscribers get EVERY message
  - Good for broadcasting events

  Publisher → Topic → Subscriber 1 (gets ALL)
                    → Subscriber 2 (gets ALL)
                    → Subscriber 3 (gets ALL)

  Example: OrderPlaced event → Notification, Inventory, Analytics
           ALL services get the event

─────────────────────────────────────────────────────

Kafka: BOTH patterns
  - Pub/Sub: different consumer groups get all messages
  - Queue: consumers in SAME group share partitions (each msg once)

  Topic "orders" with 3 partitions:
    Consumer Group A (Email Service): 1 consumer → gets ALL
    Consumer Group B (Processing): 3 consumers → each gets 1/3
```

---

## Q8. Idempotency — Why it matters in async systems.

```
Idempotent = doing the same operation multiple times has same effect as once

Why needed: Messages can be delivered MORE THAN ONCE
  - Consumer processes message but crashes before ACK
  - Queue redelivers the message
  - Consumer processes it AGAIN

Non-Idempotent (dangerous):
  "Add $100 to account" → executed twice = $200 added!

Idempotent (safe):
  "Set balance to $500" → executed twice = still $500
  "Process order #12345" → check if already processed, skip if yes

Strategies:
  1. Idempotency Key:
     - Include unique ID in each message
     - Before processing, check if ID already processed
     - Store processed IDs in Redis SET or DB table

  2. Database Constraints:
     - Unique constraint on order_id prevents double-insert
     - UPSERT instead of INSERT

  3. Version/Timestamp:
     - Only apply if version is newer
     - "UPDATE ... WHERE version < new_version"

  4. Exactly-Once Processing:
     - Kafka transactions: read + process + write offset atomically
     - AWS SQS FIFO: deduplication with MessageDeduplicationId

Code Example:
  def process_payment(payment_id, amount):
      # Check idempotency
      if redis.sismember("processed_payments", payment_id):
          return "Already processed"

      # Process payment
      db.execute("INSERT INTO payments ...")
      redis.sadd("processed_payments", payment_id)
      redis.expire("processed_payments", 86400)  # 24h TTL
```

---

## Q9. Dead Letter Queue (DLQ) — Handling failed messages.

```
DLQ = queue where failed messages go after max retries

  ┌──────────┐     ┌───────────┐     ┌──────────┐
  │ Producer │────→│   Queue   │────→│ Consumer │
  └──────────┘     └─────┬─────┘     └────┬─────┘
                         │                 │ Fails!
                         │   Retry (3x)    │
                         │←────────────────┘
                         │
                    Max retries exceeded
                         │
                    ┌────┴──────┐
                    │   DLQ     │ ← Failed messages land here
                    │           │
                    └───────────┘
                         │
                    Manual review / alerting / automated fix

Why DLQ:
  ✓ Prevents poison messages from blocking the queue
  ✓ Failed messages are preserved for analysis
  ✓ Can replay messages after fixing the bug
  ✓ Alerting: DLQ depth > 0 → something is wrong

Best Practices:
  - Set max retry count (3-5 attempts)
  - Use exponential backoff between retries
  - Alert on DLQ depth
  - Include original error in DLQ message metadata
  - Review and replay DLQ messages regularly
```

---

## Q10. When NOT to use async / disadvantages.

```
Don't use async when:
  ✗ Operation is cheap and fast (< 100ms)
  ✗ User needs immediate result
  ✗ Adds unnecessary complexity
  ✗ Real-time requirements (chat, gaming)

Disadvantages of Asynchronism:
  ✗ Added complexity: queue infrastructure, monitoring
  ✗ Debugging is harder: async flows across services
  ✗ Message ordering challenges
  ✗ Exactly-once delivery is hard
  ✗ Increased latency for simple operations
  ✗ Need for idempotency
  ✗ Queue can become bottleneck if not scaled
  ✗ Monitoring: need to track queue depth, processing time, DLQ

Interview Tip: "I'd use Kafka for inter-service event streaming because
  of its high throughput and partition-based ordering. For task processing
  like image resizing, I'd use a task queue with retry and DLQ support.
  Critical to make all consumers idempotent since messages can be
  delivered more than once."
```

---

*Source: Concepts synthesized from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer), Kafka documentation, and [karanpratapsingh/system-design](https://github.com/karanpratapsingh/system-design)*
