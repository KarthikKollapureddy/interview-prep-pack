# Caching & Redis — Interview Q&A

> 12 questions covering caching strategies, Redis data structures, cache invalidation  
> Asked at: Amazon, Flipkart, Swiggy, PhonePe, Razorpay — every system design round involves caching

---

## Caching Fundamentals

### Q1. What is caching? Why cache?

```
Problem: Database queries are slow (disk I/O, network latency)
Solution: Store frequently accessed data in fast memory (RAM)

Benefits:
- Reduce latency: Redis < 1ms vs MySQL 5-50ms
- Reduce DB load: 80/20 rule — 20% of data serves 80% of requests
- Improve throughput: handle more requests with same infrastructure
- Cost savings: RAM is cheaper than scaling databases

Cache Hit Ratio = cache_hits / (cache_hits + cache_misses)
Target: 90%+ for most systems
```

---

### Q2. Caching Strategies — Read patterns.

```
1. CACHE-ASIDE (Lazy Loading) — Most common
   ┌────────┐                          ┌───────┐
   │  App   │──1. GET key──→           │ Cache │
   │        │←─2. MISS────             │(Redis)│
   │        │──3. Query──→  ┌────┐     └───────┘
   │        │←─4. Result──  │ DB │
   │        │──5. SET key─→ │    │
   └────────┘               └────┘

   Code:
   public User getUser(Long id) {
       String key = "user:" + id;
       User cached = redis.get(key);
       if (cached != null) return cached;          // Cache hit
       User user = userRepository.findById(id);     // Cache miss → DB
       redis.setex(key, 3600, user);                // Store in cache (TTL 1hr)
       return user;
   }
   
   Pro: Only caches what's needed, simple
   Con: Cache miss penalty (3 round trips), stale data possible

2. READ-THROUGH — Cache fetches from DB on miss
   App → Cache → (miss) → Cache fetches from DB → returns to App
   
   Pro: App code simpler (doesn't know about DB)
   Con: Cache library must support DB integration

3. WRITE-THROUGH — Write to cache AND DB simultaneously
   App → Cache → DB (synchronous)
   
   Pro: Cache always consistent with DB
   Con: Higher write latency (2 writes per operation)

4. WRITE-BEHIND (Write-Back) — Write to cache, async write to DB
   App → Cache → (async batch) → DB
   
   Pro: Fast writes, batch DB operations
   Con: Data loss risk if cache crashes before DB write

5. WRITE-AROUND — Write directly to DB, skip cache
   App → DB (cache populated only on reads)
   
   Pro: Don't pollute cache with rarely-read data
   Con: Cache miss on first read after write
```

**When to use which:**
| Pattern | Best For |
|---------|----------|
| Cache-Aside | General purpose, read-heavy |
| Read-Through | When cache is primary data interface |
| Write-Through | Data consistency critical |
| Write-Behind | High write throughput (analytics, logs) |
| Write-Around | Write-heavy data rarely re-read |

---

### Q3. Cache Eviction Policies.

```
LRU  (Least Recently Used)  — Evict least recently accessed. DEFAULT in Redis.
LFU  (Least Frequently Used) — Evict least frequently accessed. Good for hot data.
FIFO (First In, First Out)   — Evict oldest entry.
TTL  (Time-To-Live)          — Evict after expiration time.
Random                        — Random eviction. Surprisingly effective.

Redis eviction policies (-maxmemory-policy):
  noeviction       — Return error on write when full (default)
  allkeys-lru      — LRU across all keys
  volatile-lru     — LRU only among keys with TTL set
  allkeys-lfu      — LFU across all keys
  volatile-ttl     — Evict keys with shortest TTL first
  allkeys-random   — Random eviction

Recommendation: allkeys-lru for general caching, volatile-ttl for session data
```

---

### Q4. Cache Invalidation — The hardest problem in CS.

```
"There are only two hard things in Computer Science:
cache invalidation and naming things." — Phil Karlton

Strategies:
1. TTL-based — set expiry, tolerate staleness
   redis.setex("user:123", 3600, userData);  // auto-expires in 1hr

2. Event-driven — invalidate on write
   @CacheEvict(value = "users", key = "#user.id")
   public void updateUser(User user) { userRepo.save(user); }

3. Write-through — update cache + DB atomically

4. Versioned keys — include version in cache key
   redis.set("user:123:v7", userData);  // new version = new key

Common problems:
❌ Stale reads — cache has old data
❌ Cache stampede — many requests hit DB when cache expires
❌ Inconsistency — cache and DB out of sync

Cache stampede prevention:
  - Mutex lock: only 1 thread refreshes, others wait
  - Early refresh: refresh before TTL expires (background thread)
  - Stale-while-revalidate: serve stale data, refresh async
```

---

## Redis Deep Dive

### Q5. Redis Data Structures — when to use which?

```
STRING — Simple key-value
  SET user:123 "Karthik"
  GET user:123
  INCR page_views          → Atomic counter
  SETEX session:abc 1800 "{...}"  → Session with 30min TTL
  Use: Caching, counters, sessions, rate limiting

HASH — Object/map storage
  HSET user:123 name "Karthik" email "k@mail.com" age 28
  HGET user:123 name       → "Karthik"
  HGETALL user:123         → all fields
  Use: User profiles, product details, config (partial updates efficient)

LIST — Ordered collection (linked list)
  LPUSH notifications:user1 "New order"
  RPOP notifications:user1
  LRANGE notifications:user1 0 9    → latest 10
  Use: Message queues, activity feeds, recent items

SET — Unique unordered collection
  SADD online_users "user1" "user2" "user3"
  SISMEMBER online_users "user1"   → 1 (true)
  SINTER followers:user1 followers:user2  → mutual friends
  Use: Tags, unique visitors, mutual friends, deduplication

SORTED SET (ZSET) — Unique items with scores
  ZADD leaderboard 100 "player1" 200 "player2" 150 "player3"
  ZREVRANGE leaderboard 0 9       → top 10 players
  ZRANK leaderboard "player1"     → player's rank
  Use: Leaderboards, priority queues, time-series, rate limiting

STREAM — Append-only log (like Kafka)
  XADD events * action "purchase" user_id "123"
  XREAD COUNT 10 STREAMS events 0
  Use: Event sourcing, activity log, lightweight messaging
```

---

### Q6. Redis as a distributed cache — architecture.

```
Standalone (single node):
  App → Redis → done
  Pro: Simple. Con: Single point of failure, limited memory.

Sentinel (high availability):
  ┌─────────┐     ┌──────────┐     ┌──────────┐
  │ Sentinel │     │ Sentinel │     │ Sentinel │
  └────┬────┘     └─────┬────┘     └─────┬────┘
       │                │                │
  ┌────┴────┐     ┌─────┴────┐     ┌─────┴────┐
  │  Master │────→│ Replica 1│     │ Replica 2│
  └─────────┘     └──────────┘     └──────────┘
  
  Sentinel monitors master, promotes replica on failure.
  Pro: Auto-failover. Con: Still single-master writes.

Cluster (horizontal scaling):
  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │ Master 1 │  │ Master 2 │  │ Master 3 │
  │ slots    │  │ slots    │  │ slots    │
  │ 0-5460   │  │ 5461-10922│ │10923-16383│
  └────┬─────┘  └────┬─────┘  └────┬─────┘
       │              │              │
  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐
  │ Replica  │  │ Replica  │  │ Replica  │
  └──────────┘  └──────────┘  └──────────┘
  
  16384 hash slots distributed across masters.
  Key → CRC16(key) % 16384 → routes to correct master.
  Pro: Scale reads AND writes. Con: Complex, multi-key operations limited.
```

---

### Q7. Redis + Spring Boot integration.

```java
// 1. Dependency
// spring-boot-starter-data-redis

// 2. Configuration
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

// 3. Usage with annotations
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")  // Cache GET
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    @CachePut(value = "users", key = "#user.id")  // Update cache on save
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")  // Remove from cache
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true)  // Clear entire cache
    public void clearCache() {}
}

// 4. Manual RedisTemplate usage (for complex operations)
@Autowired private RedisTemplate<String, Object> redisTemplate;

// String operations
redisTemplate.opsForValue().set("key", "value", 1, TimeUnit.HOURS);

// Hash operations
redisTemplate.opsForHash().put("user:123", "name", "Karthik");

// Sorted set (leaderboard)
redisTemplate.opsForZSet().add("leaderboard", "player1", 100.0);
```

---

### Q8. Common Redis patterns in production.

```java
// 1. DISTRIBUTED LOCK (prevent concurrent processing)
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent("lock:order:" + orderId, "1", Duration.ofSeconds(30));
if (Boolean.TRUE.equals(locked)) {
    try {
        processOrder(orderId);
    } finally {
        redisTemplate.delete("lock:order:" + orderId);
    }
}
// Better: Use Redisson's RLock for production

// 2. RATE LIMITING (sliding window)
String key = "rate:" + userId + ":" + LocalDateTime.now().getMinute();
Long count = redisTemplate.opsForValue().increment(key);
redisTemplate.expire(key, 2, TimeUnit.MINUTES);
if (count > MAX_REQUESTS_PER_MINUTE) throw new RateLimitExceededException();

// 3. SESSION STORAGE
// application.yml:
// spring.session.store-type=redis
// Auto-stores HTTP sessions in Redis → stateless app servers

// 4. PUB/SUB (real-time notifications)
redisTemplate.convertAndSend("order-events", orderEvent);
// Subscriber:
@Component
public class OrderEventListener implements MessageListener {
    public void onMessage(Message message, byte[] pattern) {
        // handle event
    }
}

// 5. CACHE WARMING (pre-populate cache on startup)
@PostConstruct
public void warmCache() {
    List<Product> topProducts = productRepo.findTopSelling(100);
    topProducts.forEach(p -> 
        redisTemplate.opsForValue().set("product:" + p.getId(), p, 1, TimeUnit.HOURS));
}
```

---

### Q9. Redis vs Memcached vs Hazelcast.

| Feature | Redis | Memcached | Hazelcast |
|---------|-------|-----------|-----------|
| Data structures | Rich (String, Hash, List, Set, ZSet, Stream) | String only | Map, Queue, Set, List |
| Persistence | RDB + AOF | None | Yes |
| Clustering | Yes (16384 slots) | Client-side sharding | Yes (auto-partition) |
| Pub/Sub | Yes | No | Yes |
| Lua scripting | Yes | No | No |
| Memory efficiency | Higher overhead | More efficient | Higher overhead |
| Replication | Master-Replica | No | Sync/Async |
| Transactions | MULTI/EXEC | No | Yes (JTA) |
| Embedded mode | No (separate process) | No | Yes (in-process) |
| Best for | General caching, sessions, queues, leaderboards | Simple key-value caching | Java apps, distributed computing |

**Default choice:** Redis (most versatile, largest community).

---

### Q10. Cache problems in distributed systems.

```
1. CACHE STAMPEDE (Thundering Herd)
   Popular key expires → 1000 requests hit DB simultaneously
   
   Fix:
   - Mutex lock: only 1 thread refreshes
   - Probabilistic early refresh: refresh before TTL
   - Never-expire + background refresh

2. CACHE PENETRATION
   Requests for keys that DON'T EXIST in DB → every request hits DB
   (e.g., attacker queries userId=-1 repeatedly)
   
   Fix:
   - Cache null results with short TTL: redis.setex("user:-1", 60, "NULL")
   - Bloom filter: check if key possibly exists before DB query

3. CACHE BREAKDOWN
   Single hot key expires → massive traffic hits DB
   
   Fix:
   - Never expire hot keys
   - Mutex lock on that specific key
   - Logical expiration (cache stores {data, expireAt}, background refresh)

4. CACHE AVALANCHE
   Many keys expire simultaneously → DB overwhelmed
   
   Fix:
   - Random TTL jitter: TTL = baseTTL + random(0, 300)
   - Circuit breaker on DB calls
   - Multi-layer cache: L1 (in-process) + L2 (Redis)

5. HOT KEY
   Single key accessed by millions of requests → single Redis node overwhelmed
   
   Fix:
   - Local cache (Caffeine) for hot keys
   - Key replication across multiple slots
   - Read from replicas
```

---

### Q11. Cache design for FedEx tracking.

```
Scenario: 50M tracking lookups/day, most for recent packages

Cache strategy:
  Key:   tracking:{trackingNumber}
  Value: { status, lastScan, estimatedDelivery, events[] }
  TTL:   Active packages: 5 min
         Delivered (>7 days): 24 hours
         Delivered (>30 days): don't cache

Multi-layer:
  L1: Caffeine (in-process, 10K entries, 60s TTL) → p99 < 1ms
  L2: Redis Cluster (distributed, 5M entries, 5min TTL) → p99 < 5ms
  L3: Cassandra (persistence) → p99 < 50ms

Write path (scan event):
  1. Write to Cassandra
  2. Publish to Kafka
  3. Consumer updates Redis: redis.setex("tracking:FX123", 300, newStatus)
  4. Consumer sends invalidation to L1 caches (Redis Pub/Sub)

Read path:
  1. Check L1 (Caffeine) → hit? return
  2. Check L2 (Redis) → hit? populate L1, return
  3. Query L3 (Cassandra) → populate L2 + L1, return
```

---

### Q12. Interview rapid-fire: Redis commands.

```bash
# Strings
SET key value EX 3600          # Set with 1hr TTL
GET key                        # Get value
INCR counter                   # Atomic increment
MGET key1 key2 key3            # Multi-get (batch)

# Hash
HSET user:1 name "Karthik" age 28
HGET user:1 name
HGETALL user:1
HINCRBY user:1 age 1           # Increment field

# List
LPUSH queue "task1"             # Push to head
RPOP queue                      # Pop from tail (FIFO queue)
LRANGE queue 0 -1               # Get all elements

# Set
SADD tags:post1 "java" "spring" "redis"
SMEMBERS tags:post1
SINTER tags:post1 tags:post2    # Intersection

# Sorted Set
ZADD leaderboard 100 "player1"
ZREVRANGE leaderboard 0 9 WITHSCORES  # Top 10
ZRANK leaderboard "player1"

# TTL & Expiry
EXPIRE key 3600                 # Set TTL (seconds)
TTL key                         # Check remaining TTL
PERSIST key                     # Remove TTL

# Transactions
MULTI
SET key1 "a"
SET key2 "b"
EXEC                            # Atomic execution

# Pub/Sub
SUBSCRIBE channel1
PUBLISH channel1 "hello"
```
