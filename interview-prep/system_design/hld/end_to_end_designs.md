# HLD — End-to-End System Designs (Interview Deep Dives)

> **5 complete system designs** — each structured as a 45-min interview walkthrough  
> For each: Requirements → Estimates → Architecture → Deep Dive → Trade-offs → Bottlenecks

---

## How to Use This File

```
For each design below:
1. First, try solving it yourself on paper for 30 minutes
2. Then read through the solution and compare
3. Practice explaining the design out loud (simulate interview)
4. Focus on TRADE-OFFS — that's what interviewers really test

Interview timing (45 min):
  ├── 3 min  → Clarify requirements (functional + non-functional)
  ├── 3 min  → Back-of-envelope estimation
  ├── 10 min → High-level architecture (draw boxes)
  ├── 20 min → Deep dive into 2-3 critical components
  └── 5 min  → Bottlenecks, monitoring, future scale
```

---

---

# Design 1: URL Shortener (TinyURL / Bitly)

## Why This Is Asked
Tests: hashing, database design, caching, read-heavy system, analytics pipeline. Asked at: Amazon, Google, Microsoft, FedEx, all product companies.

---

## Step 1: Requirements Clarification

**Functional:**
- Given a long URL → generate a short URL (e.g., `https://tiny.url/abc123`)
- Redirect short URL → original long URL (HTTP 301/302)
- Custom aliases (optional)
- URL expiry (TTL)
- Click analytics (who, when, where)

**Non-Functional:**
- 100M new URLs/month (write)
- 10B redirects/month (read) → 100:1 read-write ratio
- Availability > 99.99% (if link is down, business loses money)
- Redirect latency < 50ms
- Short URLs should not be guessable (security)

**Questions to ask interviewer:**
- What's the length limit for short codes? (7-8 chars)
- Do we need user accounts? (Yes, for analytics)
- What's the retention period? (5 years default)
- Geographic distribution? (Global CDN)

---

## Step 2: Back-of-Envelope Estimates

```
Write: 100M URLs/month ÷ 30 ÷ 86400 ≈ 40 writes/sec
Read: 100 × 40 = 4,000 reads/sec → peak 10K reads/sec

Storage (5 years):
  100M/month × 12 × 5 = 6B URLs
  Each record: ~500 bytes (URL + metadata)
  6B × 500 bytes = 3 TB total storage

Cache:
  80/20 rule: 20% URLs get 80% traffic
  Daily reads: 10B/30 = 333M reads/day
  Cache 20% of daily: 66M × 500 bytes = 33 GB → fits in single Redis

Bandwidth:
  Read: 4000 req/sec × 500 bytes = 2 MB/sec (trivial)

Short code space:
  Base62 (a-z, A-Z, 0-9), 7 chars = 62^7 = 3.5 TRILLION combinations
  6B URLs needed → no collision concern
```

---

## Step 3: High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser/App)                      │
└───────────────────────┬──────────────────────────────────────────┘
                        │
                   ┌────┴─────┐
                   │   CDN    │  ← Cache redirects at edge (Cloudflare)
                   └────┬─────┘
                        │
               ┌────────┴────────┐
               │  Load Balancer  │  ← Round-robin, health checks
               └────────┬────────┘
                        │
          ┌─────────────┼─────────────┐
          │             │             │
   ┌──────┴──────┐ ┌───┴────┐ ┌─────┴──────┐
   │  URL Write  │ │  URL   │ │ Analytics  │
   │  Service    │ │ Redirect│ │ Service    │
   │  (Shorten)  │ │ Service │ │            │
   └──────┬──────┘ └───┬────┘ └─────┬──────┘
          │            │             │
   ┌──────┴──────┐ ┌───┴────┐ ┌─────┴──────┐
   │ Key Gen     │ │ Redis  │ │  Kafka     │
   │ Service     │ │ Cache  │ │  (clicks)  │
   │ (KGS)       │ │        │ │            │
   └──────┬──────┘ └───┬────┘ └─────┬──────┘
          │            │             │
          └────────────┼─────────────┘
                       │
              ┌────────┴────────┐
              │   MySQL / Postgres  │  ← Master-Slave replication
              │   (URL mappings)    │
              └─────────────────────┘
```

---

## Step 4: Deep Dive — Critical Components

### 4.1 Short Code Generation (The Core Problem)

**Approach 1: Hash + Truncate**
```
MD5(longUrl) → 128-bit hash → take first 7 chars of Base62
Problem: Collisions possible → need DB check + retry
Pro: Deterministic (same URL → same code)
```

**Approach 2: Auto-increment + Base62 (Simple)**
```
MySQL auto-increment ID → Base62 encode
ID 1 → "1", ID 62 → "10", ID 1000000 → "4C92"
Problem: Sequential = guessable → security concern
Pro: Zero collisions, simple
```

**Approach 3: Pre-generated Key Service (KGS) — BEST ✅**
```
Separate service pre-generates millions of unique 7-char codes.
Stores in DB with two tables:
  - unused_keys (pre-generated, ready to use)
  - used_keys (assigned to URLs)

When URL Shortener needs a code:
  1. KGS pops a batch (e.g., 1000) into memory
  2. Assigns one to the URL atomically
  3. Marks as used

Why this is best:
  - No collision possible (all pre-generated and unique)
  - No hashing overhead
  - Not guessable (random, not sequential)
  - Can be replicated for HA (each replica gets different batch)
```

```java
// KGS pseudocode
class KeyGenerationService {
    private final Queue<String> keyBuffer = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 10_000;
    
    @PostConstruct
    void preloadKeys() {
        List<String> batch = keyRepository.fetchUnusedBatch(BATCH_SIZE);
        keyRepository.markAsUsed(batch);
        keyBuffer.addAll(batch);
    }
    
    public String getNextKey() {
        String key = keyBuffer.poll();
        if (key == null) throw new KeyExhaustedException();
        if (keyBuffer.size() < BATCH_SIZE / 2) {
            // Async refill
            CompletableFuture.runAsync(this::preloadKeys);
        }
        return key;
    }
}
```

### 4.2 Database Schema

```sql
-- Main URL table (sharded by short_code)
CREATE TABLE urls (
    short_code  VARCHAR(7) PRIMARY KEY,    -- Partition key
    long_url    TEXT NOT NULL,
    user_id     BIGINT,
    created_at  TIMESTAMP DEFAULT NOW(),
    expires_at  TIMESTAMP,
    click_count BIGINT DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_expires (expires_at)
);

-- Why short_code as PK?
-- All reads are by short_code → O(1) lookup
-- Sharding: consistent hash on short_code distributes evenly
```

### 4.3 Redirect Flow (Hot Path — Must Be Fast)

```
User clicks https://tiny.url/abc123

1. CDN check → cache HIT? → 301 redirect (done, < 5ms)
2. CDN MISS → Load Balancer → Redirect Service
3. Redis check → HIT? → 301 redirect (done, < 10ms)
4. Redis MISS → DB lookup → found? 
   - Yes → cache in Redis (TTL 24h) → 301 redirect
   - No → 404 Not Found
5. Log click event to Kafka (async, non-blocking)

301 vs 302:
  301 (Permanent) → browser caches, reduces server load, loses analytics
  302 (Temporary) → browser always comes back, better analytics
  → Use 302 if analytics needed, 301 if not
```

### 4.4 Analytics Pipeline

```
Click events → Kafka → Stream Processing (Flink/Spark)
  ↓
ClickHouse / Druid (OLAP database)
  - Total clicks per URL
  - Clicks by country, device, browser, OS
  - Clicks over time (hourly/daily)
  - Referrer analysis
  
Dashboard: Grafana reading from ClickHouse
```

### 4.5 Caching Strategy

```
Layer 1: CDN (Cloudflare) → caches 301 redirects at edge
Layer 2: Application-level Redis cache
  - Key: short_code → Value: long_url
  - TTL: 24 hours (hot URLs stay cached)
  - Eviction: LRU when memory full
  - Cache-aside pattern: check cache → miss → DB → populate cache

Cache hit ratio: ~95% (most clicks go to recent/popular URLs)
95% × 4000 req/sec = 3800 served from cache → only 200 hit DB
```

---

## Step 5: Trade-offs & Decisions

| Decision | Option A | Option B | Choice & Why |
|----------|----------|----------|--------------|
| Key generation | Hash + collision check | Pre-generated KGS | **KGS** — zero collisions, no latency overhead |
| Database | SQL (MySQL) | NoSQL (DynamoDB) | **SQL** — simple key-value lookup, ACID for consistency, 3TB fits |
| Redirect code | 301 Permanent | 302 Temporary | **302** — analytics need every click tracked |
| Caching | Redis | Memcached | **Redis** — TTL support, persistence, richer API |
| Analytics | Real-time | Batch | **Real-time** via Kafka → ClickHouse |

---

## Step 6: Bottlenecks & Monitoring

```
Bottleneck 1: KGS single point of failure
  → Solution: Run 2+ KGS replicas, each gets different key ranges
  
Bottleneck 2: Database writes at scale
  → Solution: Shard by short_code (consistent hashing, 8 shards)
  
Bottleneck 3: Hot URLs overwhelming single cache node
  → Solution: Redis Cluster with read replicas per shard

Monitoring:
  - P99 redirect latency (target < 50ms)
  - Cache hit ratio (target > 90%)
  - KGS key pool size (alert if < 100K unused)
  - Error rate (4xx, 5xx)
  - QPS dashboard
```

---

---

# Design 2: Food Delivery System (Swiggy / Zomato / DoorDash)

## Why This Is Asked
Tests: real-time location, matching algorithms, payment, multi-service orchestration. Asked at: Swiggy, Zomato, Uber, Amazon, Flipkart.

---

## Step 1: Requirements Clarification

**Functional:**
- Customer: browse restaurants, search, add to cart, place order, track delivery in real-time
- Restaurant: receive orders, update status (accepted, preparing, ready)
- Delivery partner: accept delivery, navigate, update location, mark delivered
- Payment: online + COD
- Ratings & reviews
- Estimated delivery time (ETA)

**Non-Functional:**
- 1M orders/day (peak: 3x during lunch/dinner)
- Location updates every 5 seconds from 100K active delivery partners
- ETA accuracy within ±5 minutes
- Payment processing: exactly-once semantics
- 99.9% availability during peak hours

**Questions to ask:**
- How many restaurants? (~500K)
- Delivery radius? (~10 km)
- Multiple restaurants in one order? (No, single restaurant per order)

---

## Step 2: Estimates

```
Orders:
  1M orders/day ÷ 86400 ≈ 12 orders/sec avg
  Peak (2 hrs lunch + 3 hrs dinner = 5 hrs): 1M × 0.7 / 18000 = 40 orders/sec

Location updates:
  100K drivers × 1 update/5 sec = 20K writes/sec (GPS data)
  → This is the most write-heavy part

Restaurant search:
  Assume 10x orders = 10M searches/day ≈ 115 search/sec

Storage:
  Orders: 1M/day × 2KB × 365 × 3 years = 2.2 TB
  Location: 20K/sec × 100 bytes × 86400 = 170 GB/day (keep 7 days hot)
```

---

## Step 3: High-Level Architecture

```
┌─────────────┐  ┌─────────────┐  ┌──────────────┐
│  Customer   │  │  Restaurant │  │  Delivery    │
│  App        │  │  App/Tablet │  │  Partner App │
└──────┬──────┘  └──────┬──────┘  └──────┬───────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
                ┌───────┴────────┐
                │  API Gateway   │ ← Auth, Rate Limit, Routing
                │  (Kong/Nginx)  │
                └───────┬────────┘
                        │
      ┌────────┬────────┼────────┬────────┬──────────┐
      │        │        │        │        │          │
┌─────┴──┐┌───┴───┐┌───┴───┐┌───┴───┐┌───┴───┐┌────┴────┐
│ User   ││Search ││Order  ││Payment││Delivery││ Notif.  │
│Service ││Service││Service││Service││Service ││ Service │
└───┬────┘└───┬───┘└───┬───┘└───┬───┘└───┬───┘└────┬────┘
    │         │        │        │        │         │
┌───┴────┐┌───┴───┐┌───┴───────┴───┐┌───┴───┐┌────┴────┐
│MySQL   ││Elastic││    Kafka      ││Redis  ││ FCM/    │
│(users) ││Search ││  (events)    ││(location│ APNs   │
└────────┘└───────┘└──────────────┘│ cache) │└─────────┘
                                    └────────┘
```

---

## Step 4: Deep Dive

### 4.1 Restaurant Search & Discovery

```
Problem: "Show me restaurants near me that deliver biryani"
→ Geo + Text search combined

ElasticSearch index:
{
  "restaurant_id": "R123",
  "name": "Paradise Biryani",
  "cuisine": ["biryani", "mughlai", "indian"],
  "location": { "lat": 17.385, "lon": 78.486 },  // Geo-point
  "rating": 4.3,
  "avg_delivery_time": 35,
  "is_open": true,
  "price_range": 2  // $ $$ $$$ scale
}

Query: Geo-distance filter (< 10km) + text match ("biryani") + sort by relevance
→ ES handles this natively with bool + geo_distance + function_score

Ranking formula:
  score = (relevance × 0.3) + (rating × 0.3) + (proximity × 0.2) + (delivery_time × 0.2)
  
Restaurant availability:
  - Menu fetched from Restaurant Service (cached in Redis, TTL 5 min)
  - Real-time availability: restaurant toggles items on/off → Redis pub/sub to update ES
```

### 4.2 Order Flow (The Heart of the System)

```
┌──────────────────────────────────────────────────────┐
│                   ORDER STATE MACHINE                 │
│                                                      │
│  PLACED → ACCEPTED → PREPARING → READY → PICKED_UP  │
│    ↓         ↓                              ↓        │
│  REJECTED  CANCELLED                    DELIVERED     │
└──────────────────────────────────────────────────────┘

Step-by-step:
1. Customer places order
   → Order Service creates order (status: PLACED)
   → Kafka event: ORDER_PLACED
   
2. Restaurant receives notification (WebSocket push)
   → Restaurant accepts → status: ACCEPTED
   → Kafka event: ORDER_ACCEPTED
   
3. Delivery matching triggered (parallel with restaurant prep)
   → Delivery Service finds nearest available partner
   → Assign delivery → partner gets notification
   
4. Restaurant marks PREPARING → READY
   → Delivery partner gets "head to restaurant" notification
   
5. Partner picks up → PICKED_UP
   → Customer sees live tracking
   
6. Partner delivers → DELIVERED
   → Payment settled → Rating prompt

Saga pattern (compensating transactions):
  If payment fails after order accepted:
    → Cancel order → Notify restaurant → Release delivery partner
  If restaurant cancels after payment:
    → Auto-refund → Notify customer → Release delivery partner
```

### 4.3 Real-Time Delivery Tracking

```
This is the most technically interesting part.

Location ingestion:
  - Delivery app sends GPS every 5 seconds via WebSocket
  - WebSocket Gateway → Kafka topic: "driver-location" → Location Service
  - Location Service writes to Redis (in-memory, fast writes):
    GEOADD driver:locations <longitude> <latitude> <driver_id>
    
  - 20K writes/sec → Redis handles this easily
  
Customer tracking:
  - Customer opens tracking → WebSocket connection to Tracking Service
  - Tracking Service subscribes to driver's location updates
  - Redis Pub/Sub: SUBSCRIBE driver:{driverId}:location
  - Every 5 sec, push updated location to customer's WebSocket
  
ETA calculation:
  - Google Maps Directions API (or internal routing engine)
  - Factors: distance, traffic, restaurant prep time, driver speed
  - Update ETA every 30 seconds
  - ETA = restaurant_prep_remaining + pickup_travel + delivery_travel
```

### 4.4 Delivery Partner Matching Algorithm

```
When order is ACCEPTED, find the best delivery partner:

Input:
  - Restaurant location
  - Available partners within 5km of restaurant
  - Each partner's current location, ongoing deliveries

Algorithm (weighted scoring):
  score = w1 × (1/distance) + w2 × (1/current_orders) + w3 × rating + w4 × acceptance_rate
  
  w1 = 0.4 (proximity is most important)
  w2 = 0.3 (prefer partners with fewer active orders)
  w3 = 0.15 (higher-rated partners)
  w4 = 0.15 (reliable partners)

Steps:
  1. Geo query: GEORADIUS driver:locations <restaurant_lat> <restaurant_lon> 5 km
  2. Filter: only AVAILABLE or has < 2 active orders
  3. Score each → sort descending
  4. Send request to top partner → 30 sec timeout
  5. If declined → next partner → cascade

Batching optimization:
  - During peak, batch nearby orders to same partner
  - Partner picks up from 2 restaurants on same route → efficiency
```

### 4.5 Payment Flow

```
┌──────────┐    ┌──────────┐    ┌──────────────┐    ┌──────────┐
│ Customer │───→│  Order   │───→│   Payment    │───→│ Razorpay/│
│   App    │    │ Service  │    │   Service    │    │ Stripe   │
└──────────┘    └──────────┘    └──────┬───────┘    └──────────┘
                                       │
                                ┌──────┴───────┐
                                │  Wallet +    │
                                │  Ledger DB   │
                                └──────────────┘

Payment options:
  - Prepaid: UPI, Card, Wallet, NetBanking
  - COD: Cash on delivery (auto-deduct from partner's next payout)

Key design:
  - Idempotency key per order (prevent double charge)
  - Payment state machine: INITIATED → AUTHORIZED → CAPTURED → SETTLED
  - Refund: Async process → credit to original payment method
  - Restaurant payout: Daily batch settlement via bank transfer

Commission model:
  Order total = food_cost + delivery_fee + platform_fee + taxes
  Restaurant gets: food_cost - commission (20-30%)
  Delivery partner gets: delivery_fee + incentives
  Platform keeps: commission + platform_fee
```

---

## Step 5: Trade-offs

| Decision | Option A | Option B | Choice & Why |
|----------|----------|----------|--------------|
| Location store | PostgreSQL + PostGIS | Redis GEOADD | **Redis** — 20K writes/sec needs in-memory speed |
| Order events | REST (sync) | Kafka (async) | **Kafka** — decouples services, handles failures gracefully |
| Search | MySQL LIKE | ElasticSearch | **ES** — geo + text + scoring in single query |
| Tracking transport | HTTP polling | WebSocket | **WebSocket** — real-time, lower bandwidth |
| Matching | Simple nearest | Weighted scoring | **Weighted** — balances speed, fairness, partner load |
| Database | Single MySQL | Sharded by city | **Sharded** — each city is independent, reduces hotspots |

---

## Step 6: Bottlenecks & Scale

```
Bottleneck 1: Location write storm (20K/sec)
  → Redis handles this. If Redis fails → buffer in Kafka → replay.

Bottleneck 2: Dinner peak (3x normal load)
  → Auto-scale Order Service pods (K8s HPA)
  → Kafka absorbs spike, consumers catch up

Bottleneck 3: Search during peak
  → ES replicas (3 replicas per shard), query routing

Monitoring:
  - Order success rate (target > 98%)
  - ETA accuracy (predicted vs actual)
  - Partner acceptance rate
  - P99 search latency (< 200ms)
  - Payment success rate
```

---

---

# Design 3: Ride-Sharing System (Uber / Ola)

## Why This Is Asked
Tests: real-time matching, geospatial indexing, dynamic pricing, distributed transactions. Asked at: Uber, Ola, Grab, Amazon, Google.

---

## Step 1: Requirements

**Functional:**
- Rider: request ride, see nearby drivers, track ride, pay, rate
- Driver: go online/offline, accept/reject ride, navigate, see earnings
- Surge pricing during high demand
- Trip history, receipts
- ETA for pickup and drop-off

**Non-Functional:**
- 15M rides/day globally
- 5M concurrent drivers sending location every 4 sec
- Matching rider-to-driver in < 15 seconds
- 99.99% availability

---

## Step 2: Estimates

```
Rides: 15M/day ÷ 86400 ≈ 175 rides/sec
Location updates: 5M drivers × (1/4 sec) = 1.25M writes/sec ← MASSIVE
Storage: 175 rides/sec × 2KB × 86400 = 30 GB/day
```

---

## Step 3: Architecture

```
┌───────────────┐  ┌────────────────┐
│  Rider App    │  │  Driver App    │
└───────┬───────┘  └────────┬───────┘
        │                   │
        └─────────┬─────────┘
                  │
          ┌───────┴────────┐
          │  API Gateway   │
          │  (Auth + Route)│
          └───────┬────────┘
                  │
   ┌──────┬──────┼──────┬──────┬──────────┐
   │      │      │      │      │          │
┌──┴──┐┌──┴──┐┌──┴──┐┌──┴──┐┌──┴──┐┌─────┴────┐
│Rider││Match││Trip ││Price││Pay  ││ Location │
│Svc  ││ Svc ││ Svc ││ Svc ││ Svc ││ Service  │
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└─────┬────┘
   │      │      │      │      │          │
   └──────┴──────┴──────┴──────┘          │
          │                          ┌────┴─────┐
    ┌─────┴──────┐                   │ Geo-Index│
    │   Kafka    │                   │ (S2/H3 + │
    │  (events)  │                   │  Redis)  │
    └─────┬──────┘                   └──────────┘
          │
   ┌──────┴──────┐
   │  Cassandra  │ ← Trip history, location trails
   └─────────────┘
```

---

## Step 4: Deep Dive

### 4.1 Geospatial Indexing — The Core Innovation

```
Problem: 1.25M location updates/sec → how to efficiently find 
         "nearest 10 drivers within 3km of rider"?

Solution: Google S2 Geometry / Uber H3 — divide Earth into cells

How S2/H3 works:
  - Earth's surface divided into hierarchical hexagonal cells
  - Each cell has a unique ID (e.g., S2 cell at level 12 ≈ 3.31 km²)
  - Driver location → compute S2 cell ID → store in index
  
  ┌─────┬─────┬─────┐
  │ C01 │ C02 │ C03 │   Each cell contains a list of driver IDs
  ├─────┼─────┼─────┤   
  │ C04 │ C05 │ C06 │   C05: [D1, D7, D23, D45]
  ├─────┼─────┼─────┤   C06: [D3, D12]
  │ C07 │ C08 │ C09 │
  └─────┴─────┴─────┘

Finding nearby drivers:
  1. Compute rider's S2 cell ID
  2. Get neighboring cells (8 surrounding + self = 9 cells)
  3. Fetch all driver IDs from these cells → Redis SET per cell
  4. Filter by distance (Haversine formula) → sort by proximity
  5. Return top 10 nearest

Redis storage:
  Key: cell:{cellId}  →  SET of driver IDs
  When driver moves: SREM from old cell, SADD to new cell
  
Why this is fast:
  - Finding 9 cells = 9 Redis SET reads = O(1) per cell
  - Much faster than scanning all 5M drivers
  - Cell size tunable: smaller cells = more precise, more cells to check
```

### 4.2 Ride Matching Algorithm

```
Rider requests ride at location (lat, lon):

1. Find nearby available drivers (Section 4.1)
2. Filter: driver must be AVAILABLE (not in ride, not offline)
3. Score each driver:
   score = w1 × (1/distance) + w2 × (1/eta_to_pickup) + w3 × driver_rating

4. Offer to top driver → push notification
   - 15 sec timeout to accept
   - If decline → next driver in ranked list
   - If all decline → expand search radius (3km → 5km → 8km)

5. Match confirmed:
   - Driver status: AVAILABLE → EN_ROUTE_TO_PICKUP
   - Trip created: status MATCHED
   - Both rider and driver see each other's location

Advanced matching (Uber's approach):
  - Batch matching every 2 seconds instead of greedy individual
  - Solve assignment problem: minimize total pickup distance across all pending requests
  - Hungarian algorithm or min-cost max-flow
  - Better overall efficiency than greedy per-request matching
```

### 4.3 Surge Pricing (Dynamic Pricing)

```
Why: Supply-demand balancing. When demand > supply → increase price to:
  1. Incentivize more drivers to come to that area
  2. Reduce demand (some riders won't pay 2x)

How:
  Per S2 cell (or group of cells = "zone"):
  
  demand = rides_requested_last_5min in this zone
  supply = available_drivers in this zone
  
  surge_multiplier = demand / supply (capped at 3x-5x)
  
  if supply > demand → multiplier = 1.0 (no surge)
  if demand = 2 × supply → multiplier = 2.0
  
  final_price = base_fare + (per_km × distance + per_min × time) × surge_multiplier

  ┌────────────────────────────────────┐
  │        Surge Pricing Flow          │
  │                                    │
  │ Location Service → aggregates      │
  │ demand/supply per zone every 1 min │
  │          │                         │
  │  Pricing Service → computes        │
  │  multiplier per zone               │
  │          │                         │
  │  API → returns multiplier to app   │
  │  "Fares are 1.8x higher right now" │
  └────────────────────────────────────┘

Trade-off:
  Pro: Balances supply/demand, increases driver earnings during peaks
  Con: PR problem ("surge during emergencies"), regulatory concerns
  → Uber caps surge during disasters, offers flat surge in some markets
```

### 4.4 Trip Lifecycle

```
State machine:
  REQUESTED → MATCHED → DRIVER_EN_ROUTE → ARRIVED →
  TRIP_STARTED → IN_PROGRESS → COMPLETED → RATED

Trip Service stores:
  {
    trip_id, rider_id, driver_id,
    pickup_location, dropoff_location,
    status, fare_estimate, actual_fare,
    distance_km, duration_min,
    surge_multiplier,
    route: [{lat, lon, timestamp}, ...],  // Breadcrumb trail
    created_at, started_at, completed_at
  }

Live tracking (during trip):
  - Driver app sends location every 4 sec → Kafka → Location Service
  - Rider app connects via WebSocket → gets driver location pushed
  - Route polyline drawn on map (Google Maps SDK)

ETA:
  - Initially: Google Maps Directions API
  - During trip: remaining distance / average speed in current traffic
  - ML model trained on historical trips improves accuracy
```

---

## Step 5: Trade-offs

| Decision | Option A | Option B | Choice & Why |
|----------|----------|----------|--------------|
| Geo index | QuadTree in DB | S2/H3 + Redis | **S2+Redis** — 1.25M updates/sec needs in-memory |
| Location store | MySQL | Cassandra | **Cassandra** — write-optimized, time-series friendly |
| Matching | Greedy (per request) | Batch (every N sec) | **Batch** at scale, **Greedy** for low traffic |
| Pricing | Static | Dynamic surge | **Surge** — essential for supply-demand |
| Communication | REST polling | WebSocket | **WebSocket** — real-time tracking |

---

## Step 6: Bottlenecks

```
#1: Location ingestion (1.25M writes/sec)
  → Shard Redis by S2 cell region
  → Kafka topic partitioned by driver_id for ordering

#2: Matching during peak (surge areas)
  → Pre-compute driver availability per zone
  → Expand radius gradually (don't scan all drivers at once)

#3: Trip data growth
  → Cassandra with TTL for raw location data (keep 30 days)
  → Archive to S3 for ML training

Monitoring:
  - Time to match (target < 15 sec)
  - ETA accuracy (target ±3 min)
  - Driver utilization rate
  - Surge pricing distribution
```

---

---

# Design 4: Video Streaming Platform (Netflix / YouTube)

## Why This Is Asked
Tests: CDN, transcoding, adaptive streaming, recommendation, massive scale. Asked at: Netflix, Amazon, Google, Hotstar, any media company.

---

## Step 1: Requirements

**Functional:**
- Upload videos (creators)
- Stream/watch videos (viewers)
- Search videos
- Recommendations
- Watch history, likes, comments

**Non-Functional:**
- 2B monthly active users
- 1M videos uploaded/day
- 500M videos watched/day
- Buffer-free streaming (adaptive bitrate)
- Global — low latency everywhere

---

## Step 2: Estimates

```
Video upload:
  1M videos/day, avg 10 min, avg 500MB raw = 500 TB/day raw uploads
  After transcoding (5 quality levels): 500 TB × 5 = 2.5 PB/day storage
  → Need massive object storage (S3-tier)

Video streaming:
  500M watches/day = 5,800 watches/sec
  Avg bitrate 5 Mbps → 5800 × 5 Mbps = 29 Tbps total bandwidth
  → This is why CDN is essential (can't serve from origin)

Metadata:
  500M videos total × 1KB metadata = 500 GB (trivially small)
```

---

## Step 3: Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      UPLOAD PATH                              │
│                                                              │
│  Creator App → API GW → Upload Service → S3 (raw)           │
│                              ↓                               │
│                    Transcoding Pipeline (SQS/Kafka)          │
│                    ├── 240p, 360p, 480p, 720p, 1080p, 4K    │
│                    ├── Generate thumbnails                    │
│                    └── Extract subtitles (if available)       │
│                              ↓                               │
│                    S3 (processed) → Push to CDN edge          │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      WATCH PATH                               │
│                                                              │
│  Viewer → CDN (edge PoP) ←─── cache HIT → stream video      │
│              │                                               │
│           cache MISS                                         │
│              ↓                                               │
│         Origin (S3) → pull to CDN → stream to viewer         │
│                                                              │
│  Viewer → API GW → Video Metadata Service → MySQL/Cassandra │
│  Viewer → API GW → Recommendation Service → ML pipeline     │
└──────────────────────────────────────────────────────────────┘

Full architecture:
┌──────────┐    ┌──────────┐    ┌───────────────┐
│  Clients │───→│  CDN     │───→│  Origin (S3)  │
│          │    │  (CF/    │    └───────────────┘
│          │    │  Akamai) │
└──────┬───┘    └──────────┘
       │
┌──────┴───────┐
│  API Gateway │
└──────┬───────┘
       │
┌──────┼──────┬──────┬──────┬──────────┐
│      │      │      │      │          │
│Video │Upload│Search│Recom.│ User     │
│Meta  │ Svc  │ Svc  │ Svc  │ Service  │
│ Svc  │      │(ES)  │(ML)  │          │
└──────┘└─────┘└─────┘└─────┘└──────────┘
```

---

## Step 4: Deep Dive

### 4.1 Video Upload & Transcoding Pipeline

```
Upload flow:
  1. Client requests pre-signed S3 URL (secure direct upload)
  2. Client uploads raw video directly to S3 (bypasses our servers)
  3. S3 event → SQS/Kafka → triggers Transcoding Service
  
Transcoding:
  Raw video → FFmpeg → multiple quality levels:
    - 240p (400 Kbps)  → mobile on 2G/3G
    - 360p (800 Kbps)  → mobile on 4G
    - 480p (1.5 Mbps)  → tablet
    - 720p (3 Mbps)    → desktop
    - 1080p (6 Mbps)   → desktop HD
    - 4K (20 Mbps)     → smart TV
  
  Each quality → split into segments (2-10 sec chunks)
  → Enables adaptive bitrate streaming (HLS/DASH)
  
  Parallel processing: split video into chunks → transcode in parallel → merge
  Use spot instances/preemptible VMs for cost savings (transcoding is CPU-heavy)

Output structure:
  s3://videos/{videoId}/
    ├── master.m3u8          (HLS manifest)
    ├── 240p/
    │   ├── segment_001.ts
    │   ├── segment_002.ts
    │   └── ...
    ├── 720p/
    │   ├── segment_001.ts
    │   └── ...
    └── 1080p/
        └── ...
```

### 4.2 Adaptive Bitrate Streaming (ABR)

```
Problem: User's bandwidth fluctuates during playback
Solution: HLS (HTTP Live Streaming) or DASH (Dynamic Adaptive Streaming)

How it works:
  1. Player downloads master manifest (master.m3u8)
     → Contains URLs for each quality level's playlist
  
  2. Player measures available bandwidth
     → Bandwidth = 3 Mbps → select 480p stream
  
  3. Player downloads video segment-by-segment
     → After each segment, re-measure bandwidth
     → Bandwidth drops to 1 Mbps → switch to 360p
     → Bandwidth improves to 6 Mbps → switch to 1080p
  
  Result: No buffering, best possible quality at all times

  ┌──────────────────────────────────────┐
  │  Bandwidth          Quality          │
  │  ─────────          ───────          │
  │  > 20 Mbps     →    4K              │
  │  6-20 Mbps     →    1080p           │
  │  3-6 Mbps      →    720p            │
  │  1.5-3 Mbps    →    480p            │
  │  0.8-1.5 Mbps  →    360p            │
  │  < 0.8 Mbps    →    240p            │
  └──────────────────────────────────────┘
```

### 4.3 CDN Strategy

```
CDN is THE most critical component for video streaming.
Netflix uses Open Connect (custom CDN appliances placed at ISPs).

Cache hierarchy:
  L1: ISP edge (Open Connect Appliance) → < 5ms latency
  L2: Regional CDN PoP (e.g., Akamai) → < 20ms latency
  L3: Origin (S3 in us-east-1) → 100ms+ latency

Strategy:
  - Popular content (top 20%) pre-pushed to L1 during off-peak
  - Long-tail content served from L2/L3 on demand
  - Cache key = videoId + quality + segment number
  
Pre-warming:
  When new popular content releases (e.g., Stranger Things):
  → Push to all CDN edges worldwide BEFORE release time
  → Prevents origin overload
```

### 4.4 Recommendation Engine (Brief)

```
Two approaches:
  1. Collaborative filtering: "Users like you also watched X"
  2. Content-based: "Because you watched action movies"
  3. Hybrid: Combine both + trending + editorial picks

Data pipeline:
  Watch events → Kafka → Spark/Flink → Feature Store
  → ML model (trained offline, served online)
  → Personalized homepage: "Continue Watching", "Top Picks", "Trending"

Netflix's approach:
  - Every row on homepage is a different "algorithm"
  - A/B test different algorithms and row orderings
  - Personalize even the thumbnail shown for each title
```

---

## Step 5: Trade-offs

| Decision | Option A | Option B | Choice & Why |
|----------|----------|----------|--------------|
| Upload | Through our server | Direct to S3 (pre-signed) | **Pre-signed S3** — no bandwidth bottleneck on our servers |
| Transcoding | Real-time | Async pipeline | **Async** — CPU heavy, process in background |
| Streaming | Progressive download | Adaptive bitrate (HLS) | **ABR** — adapts to network, no buffering |
| CDN | 3rd party (Akamai) | Own CDN | **Own CDN** at Netflix scale, **3rd party** otherwise |
| DB for metadata | MySQL | Cassandra | **Cassandra** — billions of records, write-heavy |
| Search | MySQL FULLTEXT | ElasticSearch | **ES** — fuzzy matching, facets, relevance scoring |

---

---

# Design 5: E-Commerce Platform (Amazon / Flipkart)

## Why This Is Asked
Tests: inventory management, cart, ordering, payment, search, recommendations, catalog. The most comprehensive design question. Asked everywhere.

---

## Step 1: Requirements

**Functional:**
- Product catalog: browse, search, filter, product detail page
- Cart: add/remove/update items
- Checkout: address, payment, place order
- Order management: track, cancel, return
- Inventory management: stock counts, warehouse allocation
- Reviews & ratings
- Seller portal: list products, manage inventory

**Non-Functional:**
- 500M products
- 100M DAU, 10M orders/day
- Search: < 200ms
- Checkout: handle 10K orders/sec during flash sales
- Inventory: never oversell
- 99.99% availability for checkout

---

## Step 2: Estimates

```
Search: 100M DAU × 5 searches = 500M searches/day ≈ 5,800/sec
Product page: 100M DAU × 20 pages = 2B page views/day ≈ 23K/sec
Orders: 10M/day ≈ 115/sec (peak flash sale: 10K/sec)
Inventory updates: 10M orders × 3 items avg = 30M/day ≈ 350/sec

Storage:
  Products: 500M × 5KB = 2.5 TB
  Orders: 10M/day × 2KB × 365 × 5 years = 36.5 TB
  Images: 500M products × 10 images × 500KB = 2.5 PB
```

---

## Step 3: Architecture

```
┌──────────┐
│  Clients │
│ (Web/App)│
└────┬─────┘
     │
┌────┴─────┐     ┌──────┐
│   CDN    │     │  S3   │ ← Product images
└────┬─────┘     └──────┘
     │
┌────┴──────────┐
│  API Gateway  │ ← Auth, Rate Limit, A/B routing
└────┬──────────┘
     │
┌────┴──────────────────────────────────────────────┐
│                  MICROSERVICES                      │
│                                                    │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│  │ Product │ │  Search  │ │  Cart   │ │  Order  │ │
│  │ Catalog │ │ Service  │ │ Service │ │ Service │ │
│  │ Service │ │  (ES)    │ │ (Redis) │ │         │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ │
│       │           │           │            │      │
│  ┌────┴────┐ ┌────┴────┐ ┌───┴─────┐ ┌───┴────┐ │
│  │Inventory│ │ Payment │ │Shipping │ │ Notif. │ │
│  │ Service │ │ Service │ │ Service │ │ Service│ │
│  └─────────┘ └─────────┘ └─────────┘ └────────┘ │
└───────────────────────┬───────────────────────────┘
                        │
          ┌─────────────┼─────────────┐
          │             │             │
    ┌─────┴─────┐ ┌────┴────┐ ┌─────┴─────┐
    │  MySQL    │ │  Redis  │ │  Kafka    │
    │  (orders, │ │  (cart, │ │  (events) │
    │  catalog) │ │  cache) │ └───────────┘
    └───────────┘ └─────────┘
```

---

## Step 4: Deep Dive

### 4.1 Product Search

```
ElasticSearch with 500M documents, sharded by category.

Index mapping:
{
  "product_id": "P123",
  "title": "Sony WH-1000XM5 Wireless Headphones",
  "description": "...",
  "category": ["electronics", "headphones", "wireless"],
  "brand": "Sony",
  "price": 29990,
  "rating": 4.5,
  "review_count": 12500,
  "seller_id": "S456",
  "in_stock": true,
  "attributes": {
    "color": ["black", "silver"],
    "connectivity": "bluetooth",
    "noise_cancellation": true
  }
}

Search query: "wireless headphones under 30000"
  → ES query: bool {
      must: [multi_match("wireless headphones", fields: [title, description, category])]
      filter: [range(price: { lte: 30000 }), term(in_stock: true)]
      sort: [relevance, sponsored_boost, rating × review_count]
    }

Faceted search (left sidebar filters):
  → ES aggregations: terms agg on brand, category, price range, rating
  
Auto-suggest:
  → ES completion suggester + edge-ngram tokenizer
```

### 4.2 Cart Service

```
Cart should be:
  - Fast (add/remove feels instant)
  - Persistent (survives page refresh, app close)
  - Eventually consistent (ok if slight delay in merging)

Storage: Redis Hash per user
  Key: cart:{userId}
  Field: productId → Value: { quantity, price_at_add_time, added_at }
  
  HSET cart:U123 P456 '{"qty":2, "price":29990}'
  HGET cart:U123 P456
  HDEL cart:U123 P456
  HGETALL cart:U123  → entire cart

Why Redis?
  - Sub-ms response for all cart operations
  - TTL on cart (expire after 30 days of inactivity)
  - Persistence: Redis AOF or snapshot + backup to DB

Cart-to-DB sync:
  - For logged-in users: also persist to MySQL (async via Kafka)
  - For guest users: cart in browser localStorage → merge on login
  
Price consistency:
  - Store price at time of adding (price_at_add_time)
  - At checkout, re-validate: if price changed, show notification
```

### 4.3 Checkout & Inventory — The Hard Part

```
THE CRITICAL PROBLEM: Prevent overselling during flash sales.
  1000 people click "Buy" simultaneously on item with 50 units.
  → Must ensure exactly 50 orders succeed, 950 fail gracefully.

Approach: Optimistic locking with atomic decrement

SQL:
  UPDATE inventory SET quantity = quantity - 1 
  WHERE product_id = 'P123' AND quantity > 0;
  -- Affected rows = 1 → success, 0 → out of stock

For flash sales (10K orders/sec for one product → single row hot update):
  → Pre-load stock into Redis:
    SET inventory:P123 50
    DECR inventory:P123 → returns 49 (atomic!)
    If result < 0 → INCR back (rollback) → out of stock
    
  → After decrement succeeds in Redis, async update DB
  → Periodic sync: Redis count → DB count

Checkout flow (Saga):
  ┌───────────────────────────────────────────┐
  │ 1. Validate cart items (prices, stock)     │
  │ 2. Reserve inventory (Redis DECR)          │
  │ 3. Calculate total (items + shipping + tax)│
  │ 4. Process payment (Razorpay/Stripe)       │
  │ 5. Create order (MySQL, status: CONFIRMED) │
  │ 6. Send confirmation (Email + Push)        │
  │ 7. Trigger fulfillment (Kafka event)       │
  └───────────────────────────────────────────┘
  
  Compensation (if payment fails at step 4):
    → Release inventory (Redis INCR)
    → Don't create order
    → Show error to user
```

### 4.4 Order Service

```
Order state machine:
  CREATED → PAYMENT_PENDING → CONFIRMED → 
  PROCESSING → SHIPPED → OUT_FOR_DELIVERY → DELIVERED
                ↓
             CANCELLED → REFUND_INITIATED → REFUNDED

Database schema (sharded by user_id for query "my orders"):
  orders: { id, user_id, status, total_amount, shipping_address, payment_id, created_at }
  order_items: { id, order_id, product_id, quantity, unit_price, seller_id }
  
Kafka events:
  ORDER_CONFIRMED → Inventory Service (finalize deduction)
  ORDER_CONFIRMED → Shipping Service (create shipment)
  ORDER_CONFIRMED → Notification Service (email receipt)
  ORDER_SHIPPED → Notification Service (tracking info)
  ORDER_DELIVERED → Review Service (prompt for review after 3 days)
```

### 4.5 Handling Flash Sales

```
Flash sale: 1 million users competing for 1000 units at exact time.

Strategy:
  1. Virtual waiting room (queue):
     → At sale time, all users enter a queue
     → Queue service (SQS/Redis list) processes FIFO
     → First 1000 get to checkout page, rest shown "sold out"
  
  2. Rate limiting:
     → API Gateway limits: 1 request per user per second
     → Prevents bots from grabbing all inventory
  
  3. Pre-warm everything:
     → Cache product details, inventory count in Redis
     → Scale up Order Service pods 30 min before sale
     → DB connection pool increased
  
  4. Separate flash-sale service:
     → Dedicated microservice with its own DB/Redis
     → Doesn't affect normal e-commerce traffic
```

---

## Step 5: Trade-offs

| Decision | Option A | Option B | Choice & Why |
|----------|----------|----------|--------------|
| Cart storage | MySQL | Redis | **Redis** — speed, TTL, sub-ms; backup to DB async |
| Inventory check | DB lock | Redis atomic DECR | **Redis** for flash sales, **DB** for normal |
| Search | SQL LIKE | ElasticSearch | **ES** — faceted search, relevance, scale |
| Order DB | Single MySQL | Sharded by user_id | **Sharded** — "my orders" always hits one shard |
| Events | Sync REST | Kafka | **Kafka** — decoupling, replay, resilience |
| Flash sale | Same path as normal | Dedicated service | **Dedicated** — isolates blast radius |

---

## Step 6: Bottlenecks

```
#1: Flash sale thundering herd
  → Virtual queue + rate limiting + Redis inventory

#2: Product page latency (23K/sec)
  → Redis cache (product details) + CDN (images) + ES (reviews summary)

#3: Search at scale (5.8K/sec on 500M docs)
  → ES cluster with 10+ data nodes, 3 replicas per shard

#4: Order consistency
  → Saga with compensation, idempotency keys, dead-letter queues

Monitoring:
  - Checkout success rate (target > 99%)
  - Search P99 (target < 200ms)
  - Inventory accuracy (Redis vs DB sync)
  - Cart abandonment rate
  - Payment success rate
```

---

---

# Quick Reference: Common HLD Patterns

| Pattern | When to Use | Example |
|---------|------------|---------|
| **Event-Driven** | Decouple services, async processing | Order events, notifications |
| **CQRS** | Different read/write patterns | Product catalog (write SQL, read ES) |
| **Saga** | Distributed transactions | Checkout (inventory + payment + order) |
| **Fan-out** | One event → many consumers | Tweet → all followers' feeds |
| **Consistent Hashing** | Distribute data across nodes | Cache sharding, DB sharding |
| **Circuit Breaker** | Handle downstream failures | Payment service timeout → fallback |
| **Rate Limiting** | Protect from abuse | API Gateway, flash sale |
| **Backpressure** | Slow consumers | Kafka consumer lag monitoring |

---

# HLD Interview Cheat Sheet (Bring to Every Design Round)

```
1. CLARIFY: Users, read/write ratio, latency SLA, consistency needs
2. ESTIMATE: QPS, storage, bandwidth (show math!)
3. DRAW: Client → LB → Service → Cache → DB
4. DEEP DIVE: Pick 2-3 hardest parts
5. TRADE-OFFS: "I chose X over Y because..."
6. MONITORING: How do you know it's healthy?

Database selection:
  - Structured + ACID? → MySQL/PostgreSQL
  - Write-heavy + flexible schema? → Cassandra/DynamoDB
  - Full-text search? → ElasticSearch
  - Caching? → Redis
  - File storage? → S3/MinIO
  - Analytics? → ClickHouse/Redshift

Communication:
  - Sync request/response → REST/gRPC
  - Async events → Kafka/SQS
  - Real-time push → WebSocket
  - Batch processing → Spark/Flink
```
