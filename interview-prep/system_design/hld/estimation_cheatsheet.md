# System Design — Back-of-Envelope Estimation Cheat Sheet

> Formulas + 6 worked examples for QPS, storage, bandwidth, server count, and cache sizing  
> Priority: **P1** — "Let's start with some numbers" is the first thing interviewers say in system design rounds

---

## Part 1: Core Formulas

### Traffic estimation
```
QPS (Queries Per Second) = DAU × queries_per_user / 86,400
Peak QPS = QPS × 2  (or ×3 for spiky traffic)

Example:
  DAU = 10M users
  Each user makes 10 requests/day
  QPS = 10M × 10 / 86,400 ≈ 1,157 QPS
  Peak QPS ≈ 2,300 QPS
```

### Storage estimation
```
Storage/day = DAU × actions_per_user × data_per_action
Storage/year = Storage/day × 365
Storage/5yr = Storage/year × 5  (typical system design horizon)

Example:
  DAU = 10M, each user posts 2 tweets/day
  Tweet size: 280 chars × 2 bytes + metadata = 1 KB
  Storage/day = 10M × 2 × 1KB = 20 GB/day
  Storage/year = 20 GB × 365 = 7.3 TB/year
  5-year storage = 36.5 TB
```

### Bandwidth estimation
```
Incoming bandwidth = Write QPS × request_size
Outgoing bandwidth = Read QPS × response_size

Example:
  Write QPS = 1,000, request size = 1KB
  Incoming = 1,000 × 1KB = 1 MB/s
  
  Read QPS = 10,000, response size = 5KB
  Outgoing = 10,000 × 5KB = 50 MB/s = 400 Mbps
```

### Server estimation
```
Servers needed = Peak QPS / QPS_per_server

QPS per server (rough estimates):
  Web server (Spring Boot): ~1,000-5,000 QPS (depends on logic)
  DB (PostgreSQL):          ~5,000-10,000 QPS (simple queries)
  Cache (Redis):            ~100,000+ QPS per instance
  Search (Elasticsearch):   ~1,000-5,000 QPS per node
```

### Cache sizing
```
Cache size = QPS × object_size × cache_TTL (seconds)
Or: DAU × hot_data_per_user

80/20 rule: 20% of data generates 80% of reads
Cache = 20% of daily read data

Example:
  Read QPS = 10,000, response = 1KB, cache TTL = 1 hour
  Cache = 10,000 × 1KB × 3,600 = 36 GB
  With 80/20: 36 GB × 0.2 = 7.2 GB ≈ Redis with 8 GB
```

### Memory-friendly numbers to memorize
```
Powers of 2:
  2^10 = 1 KB (thousand)
  2^20 = 1 MB (million)
  2^30 = 1 GB (billion)
  2^40 = 1 TB (trillion)

Time:
  1 day    = 86,400 seconds ≈ ~100K seconds
  1 month  = 2.5M seconds
  1 year   = 31.5M seconds ≈ ~30M seconds

Data sizes:
  ASCII char    = 1 byte
  Unicode char  = 2-4 bytes
  UUID          = 16 bytes (128 bits)
  Long (Java)   = 8 bytes
  Timestamp     = 8 bytes
  IPv4          = 4 bytes
  URL           = ~100 bytes
  Tweet         = ~1 KB (with metadata)
  Image (medium)= ~200 KB
  Image (high-res)= ~2 MB
  Video (1 min) = ~50 MB (compressed)
  
Network:
  1 Gbps Ethernet = 125 MB/s
  SSD read        = 500 MB/s
  HDD read        = 100 MB/s
  RAM access      = nanoseconds
  SSD access      = microseconds (100x slower than RAM)
  Network round trip (datacenter) = 0.5 ms
  Network round trip (cross-continent) = 100 ms
```

---

## Part 2: Worked Examples

---

### Example 1: Twitter / X — Tweet Service

**Requirements:** Users post tweets, read timeline

```
Given:
  MAU   = 300M
  DAU   = 150M (50% of MAU)
  
Write load (posting tweets):
  Average user posts 2 tweets/day
  Write QPS = 150M × 2 / 86,400 = 3,472 QPS ≈ 3,500 QPS
  Peak write QPS = 3,500 × 3 = 10,500 QPS

Read load (reading timeline):
  Average user reads 50 tweets/day (scrolling)
  Read QPS = 150M × 50 / 86,400 = 86,805 QPS ≈ 87,000 QPS
  Peak read QPS = 87,000 × 2 = 174,000 QPS
  Read:Write ratio = 25:1 (read-heavy)

Storage:
  Tweet = text (280 chars × 2 bytes) + user_id (8B) + timestamp (8B) + tweet_id (8B) + metadata (100B)
        ≈ 700 bytes ≈ 1 KB
  With media URL (optional, 20% have images): avg = 1 KB per tweet
  Media storage: 20% × 150M × 2 × 200KB = 12 TB/day (images)
  Text storage: 150M × 2 × 1KB = 300 GB/day
  5-year text: 300 GB × 365 × 5 = 547 TB
  5-year media: 12 TB × 365 × 5 = 21.9 PB

Bandwidth:
  Write: 3,500 × 1KB = 3.5 MB/s
  Read: 87,000 × 1KB = 87 MB/s (text only)
  Read with images: 87,000 × 50KB avg = 4.35 GB/s (served via CDN)

Cache:
  Hot tweets (trending, celebrity): 20% of daily tweets
  Daily tweets = 300M tweets
  Cache 20% = 60M × 1KB = 60 GB → Redis cluster
  
Servers:
  Application: 174K peak QPS / 5K per server = 35 servers
  Database: Sharded by user_id → 10+ DB nodes
  Cache: 60 GB / 16 GB per Redis = 4 Redis nodes (with replicas = 8)
```

---

### Example 2: URL Shortener (bit.ly)

**Requirements:** Shorten URLs, redirect to original

```
Given:
  100M URLs created per day
  Read:Write ratio = 100:1

Write load:
  Write QPS = 100M / 86,400 = 1,157 ≈ 1,200 QPS
  Peak: 2,400 QPS

Read load (redirects):
  Read QPS = 1,200 × 100 = 120,000 QPS
  Peak: 240,000 QPS

Storage:
  Short URL: 7 chars = 7 bytes
  Long URL: ~200 bytes
  Metadata: user_id + created_at + expiry = 50 bytes
  Total per record: ~260 bytes ≈ 300 bytes
  
  Daily: 100M × 300B = 30 GB
  5-year: 30 GB × 365 × 5 = 54.75 TB

Unique IDs:
  7-char base62 (a-z, A-Z, 0-9) = 62^7 = 3.5 trillion URLs
  At 100M/day → lasts 35,000 years → sufficient

Cache:
  80/20 rule: 20% of URLs get 80% of traffic
  Daily URLs: 100M → cache 20% = 20M URLs
  20M × 300B = 6 GB → single Redis instance

Bandwidth:
  Read: 120K × 300B = 36 MB/s = 288 Mbps
  Write: 1.2K × 300B = 360 KB/s (negligible)
```

---

### Example 3: YouTube — Video Streaming

**Requirements:** Upload and stream videos

```
Given:
  DAU = 1B (YouTube scale)
  500 hours of video uploaded per minute
  Average user watches 5 videos/day

Upload:
  500 hours/min = 500 × 60 = 30,000 minutes of video per minute
  30,000 min × 50MB/min (compressed) = 1.5 TB/min upload
  Upload bandwidth: 1.5 TB / 60s = 25 GB/s

Watch:
  Read QPS: 1B × 5 / 86,400 = 57,870 ≈ 58,000 video plays/sec
  Average video = 5 min, bitrate = 5 Mbps
  Concurrent viewers ≈ 58,000 × 5min × 60s = 17.4M concurrent streams
  Streaming bandwidth: 17.4M × 5 Mbps = 87 Tbps (this is why CDNs exist!)

Storage:
  Raw video per day: 1.5 TB/min × 60 × 24 = 2.16 PB/day
  Multiple resolutions (360p, 720p, 1080p, 4K): 3x-4x = ~8 PB/day
  5-year storage: 8 PB × 365 × 5 = 14.6 EB (exabytes)

Key insight:
  - CDN is CRITICAL (serve from edge, close to user)
  - Store original + transcode to multiple resolutions
  - Chunk video for adaptive bitrate streaming (HLS/DASH)
```

---

### Example 4: WhatsApp — Messaging System

**Requirements:** Real-time 1:1 and group messaging

```
Given:
  DAU = 500M
  Each user sends 40 messages/day

Message rate:
  Messages/day = 500M × 40 = 20B messages/day
  QPS = 20B / 86,400 = 231,481 ≈ 230K messages/sec
  Peak: 460K messages/sec

Storage:
  Message = text (200B avg) + sender_id (8B) + receiver_id (8B) + 
            timestamp (8B) + message_id (16B) + status (4B) = ~250B
  Daily: 20B × 250B = 5 TB/day
  Yearly: 5 TB × 365 = 1.8 PB/year

Connections:
  500M DAU, most connected simultaneously during peak
  Peak connections ≈ 200M concurrent WebSocket/TCP connections
  Each connection = ~10 KB memory
  Memory for connections: 200M × 10KB = 2 TB RAM
  Servers for connections: 2 TB / 64 GB per server = ~32 servers
  (In practice, more for redundancy and routing)

Bandwidth:
  230K msg/sec × 250B = 57.5 MB/s = 460 Mbps
  
Key design decisions:
  - WebSocket for real-time delivery
  - Store messages until delivered, then delete (or archive)
  - Message queues per user (offline delivery)
  - End-to-end encryption (client encrypts, server is blind)
```

---

### Example 5: Payment System (like Razorpay / Stripe)

**Requirements:** Process online payments

```
Given:
  1M transactions/day (moderate scale)
  Each transaction involves: auth → capture → settlement

Transaction rate:
  TPS (Transactions Per Second) = 1M / 86,400 = 11.6 ≈ 12 TPS
  Peak TPS = 12 × 10 = 120 TPS (flash sales, month-end)
  (Visa processes ~65K TPS globally — different scale)

Latency requirement:
  Payment auth must complete in < 2 seconds
  Settlement can be batch (overnight)

Storage:
  Transaction record = payment_id (16B) + amount (8B) + currency (3B) + 
    merchant_id (16B) + customer_id (16B) + status (10B) + timestamps (24B) +
    metadata (200B) + audit fields (100B) ≈ 400B
  
  Daily: 1M × 400B = 400 MB/day
  Yearly: 400 MB × 365 = 146 GB/year → fits on single DB (but shard for HA)
  
  PLUS audit logs: 10x transaction data ≈ 1.46 TB/year

Availability:
  Must be 99.99% (4.3 min downtime/month)
  Multi-region active-active
  Every transaction must have exactly-once guarantee (idempotency key)

Key design:
  - Idempotency key per request (prevent double charge)
  - Saga pattern for distributed transactions
  - Event sourcing for complete audit trail
  - Separate OLTP (PostgreSQL) and OLAP (data warehouse) databases
  - PCI DSS compliance: encrypt card data, tokenization
```

---

### Example 6: Notification System (Push Notifications)

**Requirements:** Send push/email/SMS notifications to users

```
Given:
  DAU = 50M
  Each user receives ~10 notifications/day
  
Notification rate:
  Total = 50M × 10 = 500M notifications/day
  QPS = 500M / 86,400 = 5,787 ≈ 6,000 notifications/sec
  Peak: 18,000/sec (batch campaigns, breaking news)

Types:
  Push (mobile)  — 60% = 3,600/sec
  Email          — 30% = 1,800/sec
  SMS            — 10% = 600/sec

Storage:
  Notification record = 200 bytes
  Daily: 500M × 200B = 100 GB/day
  Retain 30 days: 3 TB

Architecture:
  Trigger → Priority Queue → Rate Limiter → Provider Router → Delivery
  
  Provider Router:
    Push → Firebase (FCM) / Apple (APNs)
    Email → AWS SES / SendGrid
    SMS → Twilio / AWS SNS

Key challenges:
  - Rate limiting per user (don't spam: max 5/hour)
  - Priority: payment confirmation > promotional
  - Deduplication (same notification shouldn't send twice)
  - User preferences (opt-out for marketing, always-on for transactions)
  - Retry with backoff (FCM/APNs may rate limit)
  - Batch campaigns: 10M emails in 1 hour = 2,778/sec sustained
```

---

## Part 3: Estimation Template

Use this framework in interviews:

```
Step 1: Clarify scale
  - DAU / MAU?
  - Read:Write ratio?
  - Data retention period?

Step 2: Traffic
  - Write QPS = DAU × writes_per_user / 86,400
  - Read QPS = Write QPS × read_write_ratio
  - Peak = QPS × 2 (or ×3)

Step 3: Storage
  - Record size (calculate field by field)
  - Daily storage = QPS × record_size × 86,400
  - 5-year projection

Step 4: Bandwidth
  - Incoming = Write QPS × request_size
  - Outgoing = Read QPS × response_size

Step 5: Cache
  - 80/20 rule: cache 20% of daily data
  - Or: QPS × object_size × TTL

Step 6: Server count
  - Peak QPS / capacity_per_server
  - Add redundancy (×1.5 to ×2)

Pro tip: Show your work.
  Interviewers want to see the PROCESS, not exact numbers.
  Round aggressively (86,400 ≈ 100K).
  State assumptions explicitly.
```

---

## Quick Reference Table

| Metric | Formula |
|--------|---------|
| **QPS** | DAU × actions/user / 86,400 |
| **Peak QPS** | QPS × 2-3 |
| **Storage/day** | records/day × record_size |
| **Bandwidth** | QPS × payload_size |
| **Cache** | 20% of daily read data |
| **Servers** | Peak QPS / capacity_per_server |
| **1 day** | 86,400 sec ≈ 100K sec |
| **1 year** | ~31.5M sec ≈ 30M sec |
