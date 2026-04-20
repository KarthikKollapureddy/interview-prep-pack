# Apache Kafka — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. Explain Kafka architecture — Topics, Partitions, Brokers, Consumer Groups.

```
Kafka Cluster
├── Broker 1
│   ├── Topic: shipment-events (Partition 0)
│   └── Topic: tracking-updates (Partition 1)
├── Broker 2
│   ├── Topic: shipment-events (Partition 1)  ← replica of P0 on Broker 1
│   └── Topic: tracking-updates (Partition 0)
└── Broker 3 (follower replicas)
```

**Key concepts:**
- **Topic** — logical channel for events (e.g., `shipment-events`)
- **Partition** — ordered, immutable log within a topic. Parallelism unit.
- **Broker** — a Kafka server storing partitions
- **Producer** — sends messages to topics
- **Consumer** — reads messages from topics
- **Consumer Group** — group of consumers sharing partitions (each partition → one consumer in the group)
- **Offset** — position of a message in a partition (consumers track their offset)
- **Replication Factor** — how many copies of each partition (RF=3 → 3 brokers hold the data)

**Ordering guarantee:** Messages within a partition are ordered. Across partitions, no ordering guarantee. Use a message key to ensure related events go to the same partition.

---

### Q2. What is the difference between at-most-once, at-least-once, and exactly-once delivery?

| Semantics | Behavior | Config | Risk |
|-----------|----------|--------|------|
| **At-most-once** | Commit offset before processing | `enable.auto.commit=true` | Message loss |
| **At-least-once** | Commit offset after processing | Manual commit after success | Duplicate processing |
| **Exactly-once** | Idempotent producer + transactions | `enable.idempotence=true` + transactions | Slowest |

```java
// At-least-once (most common in production)
@KafkaListener(topics = "payments")
void process(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
    try {
        paymentService.process(record.value());
        ack.acknowledge(); // Only commit after successful processing
    } catch (Exception e) {
        // Don't ack — message will be redelivered
        log.error("Processing failed, will retry", e);
    }
}
```

**At NPCI:** At-least-once + idempotent consumers (use transaction ID as deduplication key).

---

### Q3. How does Kafka handle consumer rebalancing?

**When rebalancing happens:**
- Consumer joins/leaves group
- New partitions added to topic
- Consumer crashes (no heartbeat)

**Rebalancing process:**
1. Group Coordinator detects membership change
2. All consumers revoke partitions
3. Coordinator reassigns partitions across active consumers
4. Consumers resume from last committed offset

**Problem:** During rebalancing, no messages are consumed (brief pause).

**Strategies:**
- **Eager (default):** Revoke ALL partitions, reassign all → longer pause
- **CooperativeSticky:** Only revoke affected partitions → shorter pause

```java
props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, 
    CooperativeStickyAssignor.class.getName());
```

---

## Scenario-Based Questions

### Q4. At FedEx, how would you design Kafka topics for shipment tracking events?

```
Topics:
├── shipment.scan-events          — STARV scanner raw events (high volume)
│   Partitions: 12 (partition by tracking number)
│   Retention: 7 days
│   
├── shipment.status-changes       — processed status updates
│   Partitions: 6 (partition by customer ID)
│   Retention: 30 days
│   
├── shipment.notifications        — notification triggers
│   Partitions: 3
│   Retention: 3 days
│   
└── shipment.dlq                  — dead letter queue for failed events
    Partitions: 1
    Retention: 90 days
```

**Key decisions:**
- **Partition by tracking number** for scan events → all events for one shipment are ordered
- **Separate topics** for different consumers (tracking service, notification service, analytics)
- **DLQ** for events that fail processing after retries

```java
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic scanEventsTopic() {
        return TopicBuilder.name("shipment.scan-events")
            .partitions(12)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
            .build();
    }
}
```

---

### Q5. At NPCI, your payment events consumer is slow. How do you increase throughput?

**Step 1: Increase partitions + consumers**
```yaml
# Consumer concurrency = partition count
spring:
  kafka:
    listener:
      concurrency: 12  # One thread per partition
```

**Step 2: Batch consumption**
```java
@KafkaListener(topics = "payment-events", batch = "true")
void processBatch(List<ConsumerRecord<String, PaymentEvent>> records) {
    // Process up to 500 records at once
    List<Payment> payments = records.stream()
        .map(r -> mapper.toPayment(r.value()))
        .toList();
    paymentRepository.saveAll(payments); // Batch insert
}
```

**Step 3: Tune consumer configs**
```yaml
spring:
  kafka:
    consumer:
      fetch-min-size: 50000        # Wait for 50KB of data
      fetch-max-wait: 500           # Or 500ms, whichever comes first
      max-poll-records: 500         # Process 500 records per poll
```

**Step 4: Async processing** — acknowledge quickly, process in a separate thread pool.

---

### Q6. At Hatio, how do you handle Kafka consumer failures and dead letter queues?

```java
@Configuration
public class KafkaErrorConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // Retry 3 times with backoff, then send to DLQ
        DeadLetterPublishingRecoverer recoverer = 
            new DeadLetterPublishingRecoverer(template);
        
        BackOff backOff = new FixedBackOff(1000L, 3L); // 1s delay, 3 retries
        
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(ValidationException.class); // Don't retry validation errors
        return handler;
    }
}
```

**DLQ consumer (separate service or manual processing):**
```java
@KafkaListener(topics = "payment-events.DLT") // Spring adds .DLT suffix
void handleDlq(ConsumerRecord<String, PaymentEvent> record) {
    log.error("DLQ event: key={}, value={}", record.key(), record.value());
    alertService.notify("Payment event in DLQ: " + record.key());
}
```

---

### Q7. How do you ensure exactly-once processing with Kafka and a database?

**Idempotent consumer pattern:**
```java
@KafkaListener(topics = "payments")
@Transactional
void processPayment(PaymentEvent event) {
    // Check if already processed (idempotency key)
    if (processedEventRepo.existsByEventId(event.eventId())) {
        log.info("Duplicate event {}, skipping", event.eventId());
        return;
    }
    
    // Process
    paymentService.execute(event);
    
    // Record as processed (same transaction as business logic)
    processedEventRepo.save(new ProcessedEvent(event.eventId()));
}
```

**Alternative: Kafka Transactions (exactly-once semantics)**
```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "payment-txn-");
    return new DefaultKafkaProducerFactory<>(props);
}
```

---

## Coding Challenges

### Challenge 1: Event-Driven Order Pipeline
**File:** `solutions/KafkaOrderPipeline.java`  
Simulate an event-driven order processing pipeline:
1. Producer: sends OrderCreated events
2. Consumer 1: Validates and publishes OrderValidated
3. Consumer 2: Processes payment and publishes PaymentCompleted
4. Consumer 3: Updates inventory
5. Implement with in-memory queues simulating Kafka topics

### Challenge 2: Idempotent Consumer
**File:** `solutions/IdempotentConsumer.java`  
Implement an idempotent consumer:
1. Accept messages with event IDs
2. Track processed event IDs
3. Skip duplicates
4. Test: send same event 3 times, verify processed only once

---

## Gotchas & Edge Cases

### Q8. What happens when a Kafka consumer takes too long to process?

If processing takes longer than `max.poll.interval.ms` (default 5 min), the consumer is considered dead → **rebalance triggered** → partition reassigned → another consumer may re-process the same messages.

**Fix:** Increase `max.poll.interval.ms` or reduce `max.poll.records` so each poll batch finishes faster.

---

### Q9. Consumer lag — what is it and how do you monitor it?

**Consumer lag** = latest offset in partition - consumer's committed offset. Growing lag means the consumer can't keep up.

**Monitor:** `kafka-consumer-groups.sh --describe --group my-group` or JMX metrics exposed to Prometheus/AppDynamics.

**Fix:** Add more consumers (up to partition count), increase batch size, optimize processing logic.

---

### Q10. What is Kafka Streams? When to use it vs regular consumers?

**Answer:**
Kafka Streams is a **client library** for building real-time stream processing applications on top of Kafka. No separate cluster needed (unlike Spark/Flink).

```java
// Word count example:
StreamsBuilder builder = new StreamsBuilder();
KStream<String, String> textLines = builder.stream("input-topic");

KTable<String, Long> wordCounts = textLines
    .flatMapValues(line -> Arrays.asList(line.toLowerCase().split("\\W+")))
    .groupBy((key, word) -> word)
    .count(Materialized.as("word-counts-store"));

wordCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

KafkaStreams streams = new KafkaStreams(builder.build(), props);
streams.start();
```

**Core abstractions:**
| Abstraction | Description |
|-------------|-------------|
| `KStream` | Unbounded stream of records (like a log) — each record is independent |
| `KTable` | Changelog stream (latest value per key) — like a materialized view |
| `GlobalKTable` | Full copy on every instance (for small lookup tables) |

**KStream vs KTable:**
```
KStream: INSERT semantics — every record is a new event
  key=A, value=1 → key=A, value=2 → both exist

KTable: UPDATE semantics — latest value per key
  key=A, value=1 → key=A, value=2 → only value=2 exists
```

**When to use Kafka Streams vs regular Consumer:**
| Use Case | Regular Consumer | Kafka Streams |
|----------|-----------------|---------------|
| Simple message processing | ✅ | Overkill |
| Stateful aggregation (count, sum) | Hard (manual state) | ✅ Built-in |
| Stream joins | Very hard | ✅ KStream-KTable join |
| Windowed operations | Manual | ✅ Tumbling, hopping, session |
| Exactly-once processing | Complex | ✅ Built-in |

---

### Q11. What is Schema Registry? Why do you need it?

**Answer:**
Schema Registry stores and manages **Avro/Protobuf/JSON schemas** for Kafka messages. Ensures producers and consumers agree on data format.

```
Without Schema Registry:
  Producer sends: {"name": "Karthik", "age": 25}
  Producer changes to: {"fullName": "Karthik", "years": 25}
  Consumer BREAKS → can't deserialize old format!

With Schema Registry:
  1. Producer registers schema (v1: name, age)
  2. Schema Registry validates compatibility before allowing changes
  3. Consumer fetches schema from registry → deserializes correctly
  4. Producer tries breaking change → REJECTED by registry!
```

**Compatibility modes:**
| Mode | Rule | Use Case |
|------|------|----------|
| `BACKWARD` (default) | New schema can read old data | Consumer updated first |
| `FORWARD` | Old schema can read new data | Producer updated first |
| `FULL` | Both backward and forward | Safest, most restrictive |
| `NONE` | No checks | Development only |

**Spring Boot + Avro + Schema Registry:**
```yaml
spring:
  kafka:
    producer:
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
    consumer:
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
    properties:
      schema.registry.url: http://localhost:8081
      specific.avro.reader: true
```
