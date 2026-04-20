# System Design — High Level Design (HLD) — Interview Q&A

> 10 system design questions (FedEx / NPCI / Hatio level)  
> Focus: Architecture diagrams, scalability, trade-offs

> **⚠️ TODO:** Add a "Walk me through YOUR system architecture" section.  
> Prepare a 5-minute walkthrough of your FedEx or UHG project architecture:  
> services, communication patterns, databases, deployment, and how you'd explain it on a whiteboard.

---

## Framework: How to approach any HLD question (5 steps, 30-40 min)

```
1. CLARIFY (3 min)     — Ask scope questions. Users? Read/write ratio? SLA?
2. ESTIMATE (3 min)    — Back-of-envelope: QPS, storage, bandwidth
3. HIGH-LEVEL DESIGN (10 min) — Draw boxes: clients, LB, services, DB, cache
4. DEEP DIVE (15 min)  — Zoom into 2-3 critical components
5. WRAP UP (5 min)     — Bottlenecks, monitoring, future improvements
```

---

## Q1. Design a Package Tracking System (FedEx)

**Requirements:**
- 15M packages/day, real-time tracking
- Multiple scan events per package (pickup, hub, out-for-delivery, delivered)
- Customer can track via web/app with tracking number

**Estimates:**
- 15M packages × 6 scans avg = 90M writes/day ≈ 1000 writes/sec
- Reads: 50M tracking lookups/day ≈ 580 reads/sec (cacheable)
- Storage: 1KB per event × 90M/day × 365 = ~33TB/year

```
                    ┌──────────────┐
                    │   CDN/CF     │ ← Static assets
                    └──────┬───────┘
                           │
┌──────────┐      ┌───────┴────────┐      ┌───────────────┐
│  Mobile  │─────→│  API Gateway   │─────→│ Tracking      │
│  Web App │      │  (Rate Limit)  │      │ Query Service │
└──────────┘      └───────┬────────┘      └──────┬────────┘
                          │                       │
                 ┌────────┴────────┐        ┌─────┴──────┐
                 │  Scan Ingestion │        │   Redis    │ ← Latest status cache
                 │  Service        │        │   Cache    │
                 └────────┬────────┘        └─────┬──────┘
                          │                       │
                    ┌─────┴──────┐          ┌─────┴──────┐
                    │   Kafka    │          │ Cassandra  │ ← Event history
                    │  (Events)  │          │ (Write-opt)│
                    └─────┬──────┘          └────────────┘
                          │
                 ┌────────┴────────┐
                 │ Event Processor │─→ Push notifications (SNS)
                 │ (Consumer)      │─→ Analytics (S3 + Athena)
                 └─────────────────┘
```

**Key decisions:**
- **Cassandra** for event store — write-optimized, partition by tracking_number
- **Redis** for latest status — O(1) lookup by tracking ID
- **Kafka** for event streaming — decouples ingestion from processing
- **Push notifications** — real-time alerts on status change

---

## Q2. Design a UPI Payment System (NPCI)

**Requirements:**
- 10B transactions/month, P99 latency < 500ms
- Zero-downtime, eventual consistency for settlement
- Multi-bank interoperability

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│  Payer   │────→│ Payer's PSP  │────→│  NPCI Switch     │
│  App     │     │ (PhonePe etc)│     │ (Central Router)  │
└──────────┘     └──────────────┘     └────────┬─────────┘
                                               │
                                        ┌──────┴──────┐
                                        │  Payee Bank │
                                        │  (Verify +  │
                                        │   Credit)   │
                                        └─────────────┘
```

**Deep dive — Transaction flow:**
```
1. Payer initiates → Payer PSP → NPCI Switch
2. Switch routes to Payee Bank (based on VPA lookup)
3. Payee Bank verifies account → ACK/NACK
4. Switch returns response to Payer PSP → Payer
5. Settlement: Batch reconciliation every 30 min via NPCI
```

**Key decisions:**
- **Idempotency key** on every transaction (prevent double charge)
- **Two-phase flow:** Request + Response (not saga, because banks are external)
- **Circuit breakers** per bank — if SBI is slow, don't affect HDFC
- **Partitioned DB** by transaction date — hot partition = today
- **Active-active** across 2 data centers (DR)

---

## Q3. Design a Notification Service (cross-company)

**Requirements:** Email, SMS, Push notifications. 100M notifications/day. At-least-once delivery.

```
┌────────────┐    ┌──────────┐    ┌───────────────┐    ┌──────────────┐
│  Services  │───→│  Kafka   │───→│  Dispatcher   │───→│  Providers   │
│ (API call) │    │ (Buffer) │    │ (Route by     │    │ Email: SES   │
└────────────┘    └──────────┘    │  channel)     │    │ SMS: Twilio  │
                                  └───────┬───────┘    │ Push: FCM    │
                                          │            └──────────────┘
                                    ┌─────┴──────┐
                                    │  DynamoDB   │ ← delivery status, dedup
                                    └────────────┘
```

**Key decisions:**
- **Kafka** for buffering — absorb spikes, retry failed sends
- **Priority queues** — OTP/security > promotional
- **Rate limiting** per user (no spam)
- **Template service** — reusable templates with variable substitution
- **Delivery tracking** — webhook callbacks from providers

---

## Q4. Design a URL Shortener (classic)

**Requirements:** 100M URLs/month, custom aliases, expiry, analytics.

**Estimates:**
- Write: 100M/month ≈ 40 writes/sec
- Read: 100:1 ratio ≈ 4000 reads/sec (heavy caching)
- Storage: 500 bytes/URL × 100M/month × 5 years = 300 GB

```
POST /shorten { longUrl, customAlias?, expiresAt? }
GET /{shortCode} → 301 Redirect to longUrl

Key generation: Base62 encode(auto-increment ID) or pre-generated key pool
  - abcDEF123 (7 chars) = 62^7 = 3.5 trillion combinations

Storage: { shortCode, longUrl, userId, createdAt, expiresAt, clickCount }
Cache: Redis (shortCode → longUrl) — 90%+ cache hit rate
Analytics: Kafka → ClickHouse (real-time click analytics)
```

---

## Q5. Design an E-Commerce Order System (Hatio/BillDesk)

**Requirements:** Handle checkout, payment, inventory, order fulfillment.

```
┌──────────┐    ┌──────────┐    ┌──────────────┐    ┌──────────┐
│  Client  │───→│  API GW  │───→│ Order Service│───→│ Payment  │
└──────────┘    └──────────┘    └──────┬───────┘    │ Service  │
                                       │            └──────────┘
                                 ┌─────┴──────┐
                                 │  Kafka     │
                                 └─────┬──────┘
                          ┌────────────┼────────────┐
                    ┌─────┴──────┐  ┌──┴────────┐  ┌┴──────────┐
                    │ Inventory  │  │ Fulfillment│  │ Notification│
                    │ Service    │  │ Service    │  │ Service     │
                    └────────────┘  └───────────┘  └─────────────┘
```

**Saga pattern for checkout:**
```
1. Create Order (PENDING)
2. Reserve Inventory → success
3. Process Payment → success
4. Confirm Order (CONFIRMED)
5. Trigger Fulfillment

Compensation (if payment fails):
3'. Refund Payment
2'. Release Inventory
1'. Cancel Order (CANCELLED)
```

---

## Q6. How do you handle scaling? (General)

| Technique | When to use |
|-----------|-------------|
| **Vertical scaling** | Quick fix, single DB | 
| **Horizontal scaling** | Stateless services behind LB |
| **Database sharding** | Write-heavy, large datasets |
| **Read replicas** | Read-heavy (90%+ reads) |
| **Caching (Redis)** | Repeated reads, hot data |
| **CDN** | Static assets, geographically distributed users |
| **Message queues** | Decouple services, absorb traffic spikes |
| **CQRS** | Separate read/write models for different access patterns |

---

## Q7. CAP Theorem — explain with real examples.

```
CAP: You can only guarantee 2 of 3:
  C — Consistency (all nodes see same data)
  A — Availability (every request gets a response)
  P — Partition tolerance (system works despite network splits)

In distributed systems, P is mandatory → choose C or A:
  CP: MongoDB (default), HBase — returns error if can't guarantee consistency
  AP: Cassandra, DynamoDB — always responds, may return stale data
  
Real example (NPCI):
  Payment processing → CP (consistency critical, reject if unsure)
  Transaction history → AP (ok to show slightly stale data)
```

---

## Q8. Design Twitter / Social Media Feed

**Requirements:** 500M users, 200M DAU, tweet/post, follow, news feed, search.

**Estimates:**
- 500M tweets/day × 140 bytes avg = 70 GB/day new data
- Feed reads: 200M DAU × 10 feed views = 2B reads/day ≈ 23K reads/sec
- Write: 500M tweets/day ≈ 6K writes/sec

```
              ┌───────────────┐
              │   CDN / LB    │
              └───────┬───────┘
                      │
        ┌─────────────┼──────────────┐
   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
   │  Tweet  │   │  Feed   │   │  User   │
   │ Service │   │ Service │   │ Service │
   └────┬────┘   └────┬────┘   └────┬────┘
        │              │              │
   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
   │  Kafka  │   │  Redis  │   │  MySQL  │
   │ (events)│   │ (feed   │   │ (users, │
   └────┬────┘   │  cache) │   │ follows)│
        │        └─────────┘   └─────────┘
   ┌────┴────────────┐
   │ Fan-out Service  │
   └──────────────────┘
```

**Feed generation — two approaches:**

```
1. FAN-OUT ON WRITE (Push model)
   When user tweets → push to ALL followers' feed caches
   Pro: Feed read is O(1) — just fetch from cache
   Con: Celebrities with 50M followers → 50M writes per tweet (slow)
   
2. FAN-OUT ON READ (Pull model)  
   When user opens feed → fetch tweets from all followed users, merge, sort
   Pro: No write amplification
   Con: Slow reads (fetch from N sources, merge)
   
3. HYBRID (Twitter's actual approach)
   - Regular users (< 10K followers): fan-out on write
   - Celebrities (> 10K followers): fan-out on read
   - Feed = cached timeline + real-time celebrity fetch
```

**Database design:**
```sql
users(id, name, bio, followers_count, following_count)
tweets(id, user_id, content, media_url, created_at)  -- Sharded by user_id
follows(follower_id, followee_id, created_at)
feed_cache: Redis sorted set per user: ZADD feed:{userId} timestamp tweetId
```

---

## Q9. Design a Chat System (WhatsApp/Slack)

**Requirements:** 1-on-1 + group chat, online status, read receipts, media.

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │←──→│  WebSocket   │←──→│  Chat        │
│  (App)   │  WS │  Gateway     │     │  Service     │
└──────────┘     └──────┬───────┘     └──────┬───────┘
                        │                     │
                 ┌──────┴───────┐      ┌──────┴───────┐
                 │  Redis       │      │  Kafka       │
                 │  (sessions,  │      │  (messages)  │
                 │   presence)  │      └──────┬───────┘
                 └──────────────┘             │
                                       ┌──────┴───────┐
                                       │  Cassandra   │
                                       │  (msg store) │
                                       └──────────────┘
```

**Key design decisions:**

```
Connection: WebSocket (persistent, bidirectional) → not HTTP polling
  - Client connects to WS Gateway
  - Gateway maintains connection map: userId → WebSocket session
  - Session info stored in Redis for cross-server routing

Message flow (1-on-1):
  1. User A sends message via WebSocket
  2. WS Gateway → Chat Service → Kafka (persist + fan-out)
  3. Kafka Consumer checks: Is User B online?
     - Online: Route to B's WS Gateway → push via WebSocket
     - Offline: Store as undelivered → push notification (FCM/APNs)
  4. Write to Cassandra (message store, partition by conversation_id)

Group chat:
  - Group has member list
  - Message → fan-out to all group members
  - Small groups (< 100): fan-out on write
  - Large channels (> 100): fan-out on read

Online presence:
  - Heartbeat every 30s via WebSocket
  - Redis: SET presence:{userId} ONLINE EX 45
  - No heartbeat for 45s → considered offline

Read receipts:
  - Client sends ACK: {msgId, status: "read"}
  - Lightweight, no Kafka needed — direct update in Cassandra

Message storage:
  - Cassandra: partition by conversation_id, cluster by timestamp
  - Keep recent 30 days in hot storage, archive to S3
```

---

## Q10. Design a Search Autocomplete System

**Requirements:** 5B searches/day, return top 5 suggestions in < 100ms.

```
User types "inte" → suggestions:
  1. interview preparation
  2. internet banking
  3. internal server error
  4. intel stock price
  5. international flights

Architecture:
┌──────────┐    ┌──────────┐    ┌───────────────────┐
│  Client  │───→│  API GW  │───→│  Autocomplete     │
│          │    │          │    │  Service           │
└──────────┘    └──────────┘    └────────┬──────────┘
                                         │
                                  ┌──────┴──────┐
                                  │  Trie Cache  │ ← pre-built, in-memory
                                  │  (per node)  │
                                  └──────┬──────┘
                                         │ (miss)
                                  ┌──────┴──────┐
                                  │  Redis/     │
                                  │  ElasticSearch│
                                  └──────┬──────┘
                                         │
                                  ┌──────┴──────┐
                                  │  Analytics  │ ← search frequency data
                                  │  Pipeline   │   (Kafka → Flink → DB)
                                  └─────────────┘

Data structure: Trie (prefix tree)
  i
  ├── n
  │   ├── t
  │   │   ├── e
  │   │   │   ├── r [interview:50K, internet:40K, internal:10K]
  │   │   │   └── l [intel:8K]
  │   │   └── o [into:5K]

Optimization:
  - Pre-compute top 5 suggestions at each Trie node
  - Rebuild Trie hourly from search analytics data
  - Shard by first 2 characters: "in" → shard 1, "se" → shard 2
  - CDN/edge caching for top prefixes
```

---

## Q11. Design a File Storage System (Google Drive / S3)

**Requirements:** Upload, download, share files. 500M users, 1B files.

```
┌──────────┐    ┌──────────┐    ┌───────────────┐
│  Client  │───→│  API GW  │───→│  Metadata     │ → MySQL (file info, sharing)
│          │    │          │    │  Service      │
└──────────┘    └──────────┘    └───────┬───────┘
                                        │
                                 ┌──────┴──────┐
                                 │  Blob Store  │ → S3/MinIO (actual file bytes)
                                 │  Service     │
                                 └──────┬──────┘
                                        │
                                 ┌──────┴──────┐
                                 │  Chunking   │
                                 └─────────────┘

Upload flow (large files):
  1. Client requests upload → Metadata Service creates file record
  2. File split into chunks (4MB each) → each chunk hashed
  3. Chunks uploaded to Blob Store (S3) in parallel
  4. Deduplication: if chunk hash exists → skip upload
  5. Metadata updated: file → [chunk1_hash, chunk2_hash, ...]

Sync flow (across devices):
  - Client maintains local snapshot (last sync timestamp)
  - Long polling / WebSocket for real-time changes
  - Conflict resolution: last-writer-wins or create copy

Key decisions:
  - Chunking enables: resume interrupted uploads, deduplication, delta sync
  - Metadata in SQL (relational, ACID for sharing permissions)
  - File bytes in object store (cheap, durable, CDN-friendly)
  - Versioning: store all chunks, metadata tracks versions
```

---

## Q12. Design a Distributed Cache (Redis-like)

**Requirements:** < 1ms latency, 99.99% availability, horizontal scaling.

```
Architecture:
  Consistent hashing ring for key distribution
  
  ┌─────┐    ┌─────┐    ┌─────┐
  │Node1│←──→│Node2│←──→│Node3│  ← Gossip protocol for membership
  │ A-D │    │ E-K │    │ L-Z │
  └──┬──┘    └──┬──┘    └──┬──┘
     │          │          │
  ┌──┴──┐    ┌──┴──┐    ┌──┴──┐
  │Rep 1│    │Rep 2│    │Rep 3│   ← Replication for HA
  └─────┘    └─────┘    └─────┘

Key concepts:
  - Consistent hashing: adding/removing node moves only K/N keys
  - Replication factor: N=3 (write to 3 nodes)
  - Quorum: W+R > N for strong consistency (W=2, R=2, N=3)
  - Vector clocks for conflict resolution
  - LRU eviction when memory pressure
  - Gossip protocol for failure detection (heartbeats)
```
