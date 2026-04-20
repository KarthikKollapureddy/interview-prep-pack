# Caching Strategies Deep Dive — Interview Q&A

> Concepts from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
> Covers: Cache Levels, Update Strategies, Eviction Policies, Real-world Patterns
> **Priority: P0** — Caching is discussed in EVERY system design interview

---

## Q1. Where can caching happen? (Multi-Level Caching)

```
Request flow through cache layers:

  Client → Client Cache → CDN Cache → Load Balancer
    → Web Server Cache → Application Cache → Database Cache → DB

┌─────────────────────────────────────────────────────────────┐
│ Layer              │ What                │ Example           │
├────────────────────┼─────────────────────┼───────────────────┤
│ 1. Client Cache    │ Browser/OS cache    │ HTTP Cache-Control│
│                    │ Mobile app cache    │ headers, ETag     │
│                    │                     │                   │
│ 2. CDN Cache       │ Edge server cache   │ CloudFront,       │
│                    │ Static assets       │ CloudFlare        │
│                    │                     │                   │
│ 3. Web Server      │ Reverse proxy cache │ Varnish, NGINX    │
│    Cache           │ Static + dynamic    │ proxy_cache       │
│                    │                     │                   │
│ 4. Application     │ In-memory cache     │ Redis, Memcached  │
│    Cache           │ Between app and DB  │ Hazelcast         │
│                    │                     │                   │
│ 5. Database        │ Query cache,        │ MySQL query cache │
│    Cache           │ Buffer pool         │ PG shared_buffers │
└────────────────────┴─────────────────────┴───────────────────┘

Key Insight: Cache at EVERY layer, but most interview focus is on
Application Cache (Layer 4) — Redis/Memcached between app and DB.
```

---

## Q2. Application Caching — Redis vs Memcached.

```
In-memory caches sit between your application and database:

  App Server → Redis/Memcached → Database

┌───────────────────┬───────────────────┬───────────────────┐
│ Feature           │ Redis             │ Memcached         │
├───────────────────┼───────────────────┼───────────────────┤
│ Data Structures   │ Strings, Lists,   │ Strings only      │
│                   │ Sets, Sorted Sets,│ (key → blob)      │
│                   │ Hashes, Streams   │                   │
│                   │                   │                   │
│ Persistence       │ RDB + AOF         │ None (pure cache) │
│                   │                   │                   │
│ Replication       │ Master-slave      │ No native         │
│                   │                   │                   │
│ Cluster           │ Redis Cluster     │ Client-side       │
│                   │ (auto-sharding)   │ sharding          │
│                   │                   │                   │
│ Pub/Sub           │ Yes               │ No                │
│                   │                   │                   │
│ Lua Scripting     │ Yes               │ No                │
│                   │                   │                   │
│ Memory Efficiency │ Less efficient    │ More efficient    │
│                   │ (overhead per key)│ (slab allocator)  │
│                   │                   │                   │
│ Performance       │ Single-threaded   │ Multi-threaded    │
│                   │ ~100K ops/sec     │ ~100K ops/sec     │
└───────────────────┴───────────────────┴───────────────────┘

Use Redis when: need data structures, persistence, pub/sub
Use Memcached when: simple caching, maximum memory efficiency

What to cache:
  ✓ User sessions
  ✓ Fully rendered web pages
  ✓ Activity streams / feeds
  ✓ User graph data
  ✓ API responses
  ✓ Database query results
  ✓ Computed/aggregated values
```

---

## Q3. Caching at Database Query Level vs Object Level.

```
QUERY-LEVEL CACHING:
  Hash the SQL query → store result in cache

  key = hash("SELECT * FROM users WHERE id = 123")
  value = {id: 123, name: "Karthik", ...}

  Pros:
    + Simple to implement
    + No application logic changes

  Cons:
    ✗ Hard to invalidate: complex queries with JOINs
    ✗ If ANY data in a table changes, ALL cached queries
      involving that table might be stale
    ✗ Cache key explosion (many unique queries)

─────────────────────────────────────────────────────

OBJECT-LEVEL CACHING (RECOMMENDED):
  Cache assembled objects, not raw query results

  key = "user:123"
  value = UserObject {id, name, email, preferences, ...}

  Pros:
    + Easy to invalidate: user changes → delete "user:123"
    + Async processing: workers update cached objects
    + Application-level abstraction
    + Can cache computed/assembled data

  Cons:
    ✗ More application logic to manage cache
    ✗ Need to define what constitutes an "object"

Interview Tip: "I'd cache at the object level — cache assembled
  UserProfile objects rather than raw SQL results. This makes
  invalidation straightforward: when a user updates their profile,
  we delete the user:<id> key."
```

---

## Q4. Cache-Aside (Lazy Loading) — Most common pattern.

```
Application manages both cache AND database reads/writes.
Cache does NOT interact with DB directly.

Read Flow:
  1. App checks cache for key
  2. Cache MISS → query database
  3. Store result in cache
  4. Return result

  ┌─────┐  1. GET  ┌───────┐
  │ App │─────────→│ Cache │ → Miss!
  │     │←─────────│       │
  └──┬──┘          └───────┘
     │ 2. Query
     ▼
  ┌───────┐
  │  DB   │ → Returns data
  └───────┘
     │ 3. SET to cache
     ▼
  ┌───────┐
  │ Cache │ → Now cached
  └───────┘

Code Example:
  def get_user(user_id):
      user = cache.get(f"user:{user_id}")
      if user is None:                    # Cache miss
          user = db.query("SELECT * FROM users WHERE id = %s", user_id)
          if user:
              cache.set(f"user:{user_id}", serialize(user), ttl=3600)
      return user

Advantages:
  ✓ Only requested data is cached (no wasted space)
  ✓ Simple to implement
  ✓ Cache failures don't break the system (falls through to DB)
  ✓ Memcached / Redis commonly used this way

Disadvantages:
  ✗ Cache miss = 3 trips (cache check + DB query + cache write)
  ✗ Data can become STALE (DB updated but cache not)
    → Mitigate with TTL (Time-To-Live)
  ✗ New/restarted cache node = cold cache = high latency
    → Mitigate with cache warming
```

---

## Q5. Write-Through — Always keep cache fresh.

```
App writes to cache. Cache SYNCHRONOUSLY writes to DB.
Every write goes through the cache.

Write Flow:
  1. App writes to cache
  2. Cache synchronously writes to DB
  3. Return success

  ┌─────┐  1. SET  ┌───────┐  2. WRITE  ┌───────┐
  │ App │─────────→│ Cache │───────────→│  DB   │
  │     │←─────────│       │←───────────│       │
  └─────┘  3. OK   └───────┘    OK      └───────┘

Code Example:
  def set_user(user_id, values):
      user = db.query("UPDATE users SET ... WHERE id = %s", user_id, values)
      cache.set(f"user:{user_id}", serialize(user))

Advantages:
  ✓ Cache is ALWAYS up to date (never stale!)
  ✓ Subsequent reads are fast (always a cache hit)
  ✓ Users tolerate write latency more than read latency

Disadvantages:
  ✗ SLOW writes (write to cache + write to DB synchronously)
  ✗ Most data written might NEVER be read (wasted cache space)
    → Mitigate with TTL
  ✗ New/restarted cache node won't have data until next write
    → Combine with cache-aside for reads

Best Practice: Use write-through + cache-aside together:
  - Write-through keeps cache fresh on writes
  - Cache-aside handles reads for data not yet in cache
```

---

## Q6. Write-Behind (Write-Back) — Optimize write performance.

```
App writes to cache. Cache ASYNCHRONOUSLY writes to DB.
Writes are batched and flushed periodically.

Write Flow:
  1. App writes to cache → return immediately
  2. Cache queues the write
  3. Cache asynchronously flushes to DB (batched)

  ┌─────┐  1. SET  ┌───────┐   async    ┌───────┐
  │ App │─────────→│ Cache │ ─ ─ ─ ─ ─→│  DB   │
  │     │←─────────│       │    batch    │       │
  └─────┘  2. OK!  └───────┘  write     └───────┘
           (fast)

Advantages:
  ✓ Very fast writes (no DB wait)
  ✓ Batching reduces DB write load
  ✓ Smooths out write spikes

Disadvantages:
  ✗ DATA LOSS RISK if cache crashes before flushing to DB
  ✗ More complex to implement
  ✗ Eventual consistency between cache and DB

Use when:
  - Write-heavy workloads
  - Can tolerate some data loss risk
  - Need to absorb write spikes
  - Example: analytics event tracking, activity logging
```

---

## Q7. Refresh-Ahead — Proactive cache renewal.

```
Cache automatically refreshes entries BEFORE they expire,
predicting which items will be needed.

Flow:
  1. Item accessed, near TTL expiration
  2. Cache PROACTIVELY refreshes from DB (background)
  3. Next read gets fresh data with no delay

Advantages:
  ✓ Reduced latency (no cache miss for popular items)
  ✓ Always-fresh data for hot keys

Disadvantages:
  ✗ Prediction may be wrong → wasted refreshes
  ✗ If predictions are bad, worse performance than no refresh
  ✗ More complex implementation

Use when:
  - Predictable access patterns
  - Hot/popular data that's frequently accessed
  - Low tolerance for cache-miss latency
```

---

## Q8. Cache Eviction Policies — When cache is full.

```
When cache memory is full, which entries to remove?

┌──────────────┬───────────────────────────────────────────────┐
│ Policy       │ Description                                   │
├──────────────┼───────────────────────────────────────────────┤
│ LRU          │ Least Recently Used — evict oldest access     │
│              │ Best general-purpose policy. Used by Redis.   │
│              │                                               │
│ LFU          │ Least Frequently Used — evict least accessed  │
│              │ Better for stable access patterns.            │
│              │                                               │
│ FIFO         │ First In First Out — evict oldest entry       │
│              │ Simple but ignores access patterns.           │
│              │                                               │
│ Random       │ Random eviction                               │
│              │ Surprisingly effective, very simple.          │
│              │                                               │
│ TTL-based    │ Evict expired entries first                   │
│              │ Combine with LRU for best results.            │
└──────────────┴───────────────────────────────────────────────┘

Redis Eviction Policies:
  - noeviction: return error when memory full
  - allkeys-lru: LRU across ALL keys (recommended for cache)
  - volatile-lru: LRU only on keys with TTL set
  - allkeys-lfu: LFU across ALL keys (Redis 4.0+)
  - allkeys-random: random eviction
  - volatile-ttl: evict keys with shortest TTL first

Interview Tip: "We'd use Redis with allkeys-lru eviction policy
  and a default TTL of 1 hour. This ensures we automatically
  evict cold data while keeping hot data in cache."
```

---

## Q9. Cache Invalidation — The hardest problem in CS.

```
"There are only two hard things in Computer Science:
 cache invalidation and naming things." — Phil Karlton

Strategies:

1. TTL (Time-To-Live):
   - Simplest approach
   - Set expiry: cache.set(key, value, ttl=3600)
   - Trade-off: shorter TTL = fresher data, more DB hits

2. Event-Driven Invalidation:
   - On write to DB, delete/update corresponding cache key
   - Use pub/sub: DB change → publish event → cache subscriber deletes key
   - Most precise, but requires event infrastructure

3. Write-Through Invalidation:
   - Cache is always updated on writes (never stale)
   - Simplest invalidation but slow writes

4. Version-Based:
   - Include version in cache key: "user:123:v5"
   - On update, increment version → old key naturally expires
   - No explicit deletion needed

Common Pitfall — Cache Stampede (Thundering Herd):
  Problem: Popular key expires → hundreds of requests hit DB simultaneously
  Solutions:
    a) Lock: only ONE request fetches from DB, others wait
    b) Stale-while-revalidate: serve stale data while refreshing
    c) Probabilistic early expiration: randomly refresh before TTL
    d) Never expire + background refresh

Cache Penetration:
  Problem: Requests for non-existent keys always hit DB
  Solutions:
    a) Cache NULL values (with short TTL)
    b) Bloom filter: check if key MIGHT exist before DB query

Cache Breakdown:
  Problem: Hot key expires, massive concurrent requests hit DB
  Solution: Mutex lock — only one thread refreshes, others wait
```

---

## Q10. Caching Patterns in Real System Designs.

```
Example: Design Twitter Feed

Read Path (Cache-Aside):
  1. User opens feed → check Redis for "feed:user123"
  2. Cache HIT → return cached feed (99% of the time)
  3. Cache MISS → query Feed Service → assemble feed → cache it

Write Path (Fan-out on write):
  1. User posts tweet
  2. For each follower, push tweet to their cached feed in Redis
  3. Use Redis LPUSH + LTRIM to maintain feed size

  cache.lpush("feed:follower_id", tweet_json)
  cache.ltrim("feed:follower_id", 0, 999)  # Keep latest 1000

─────────────────────────────────────────────────────────────

Example: E-commerce Product Page

Multi-level caching:
  Layer 1: CDN caches static product images
  Layer 2: NGINX caches full page HTML (popular products)
  Layer 3: Redis caches product data objects
  Layer 4: MySQL query cache for complex queries

Cache hierarchy:
  CDN (1s TTL for images) → NGINX (30s) → Redis (5 min) → MySQL

Invalidation:
  Product update → publish event → invalidate Redis key
  → CDN purge API → next request rebuilds cache chain

─────────────────────────────────────────────────────────────

Example: Session Caching
  - Store session in Redis (not on individual servers)
  - Enables stateless application servers
  - Any server can handle any user's request
  - Redis TTL = session timeout (e.g., 30 min)
  - Redis persistence ensures sessions survive restart
```

---

## Q11. Cache Disadvantages — When NOT to cache.

```
✗ Consistency: cache and DB can go out of sync
  → Cache invalidation is hard
  → Stale data can cause bugs

✗ Complexity: adds another system to manage
  → Redis/Memcached deployment, monitoring, scaling
  → Cache warming, eviction policy tuning

✗ Cost: RAM is expensive
  → Cache hit ratio must justify the cost
  → Target: 90%+ hit ratio

✗ Cold Start: empty cache = all requests hit DB
  → Solution: cache warming on deploy

Don't cache when:
  - Data changes on every request (real-time stock prices)
  - Data is unique per request (search results with pagination)
  - Security-sensitive data that shouldn't persist in memory
  - Cache hit ratio would be very low (highly unique access patterns)
```

---

*Source: Concepts synthesized from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer), Redis documentation, and production caching patterns*
