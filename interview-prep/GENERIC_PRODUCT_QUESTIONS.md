# Generic Product-Based Company Questions — Master Bank

> Cross-cutting questions asked at Amazon, Google, Flipkart, Razorpay, Swiggy, PhonePe, CRED, Atlassian, Microsoft  
> Source: GeeksforGeeks, InterviewBit, LeetCode Discuss, Glassdoor, Blind, GitHub forums

---

## How to Use This File

This file supplements every topic-specific `qa.md` in the repo. After studying a topic, come here for the **generic/tricky** variants that product companies love to ask.

---

## Part 1: Core Java — Generic Questions

### 1.1 String Internals (Asked at EVERY product company)

**Q: How many String objects are created?**
```java
String s1 = "hello";                // 1 object (in pool)
String s2 = new String("hello");    // 1 or 2 objects (heap + possibly pool)
String s3 = "hel" + "lo";          // 0 new objects (compile-time constant folding → same pool ref as s1)
String s4 = s1 + s2;               // 1 new object (runtime concatenation via StringBuilder)
```

**Q: Why is String immutable? (5 reasons)**
1. String Pool — safe sharing needs immutability
2. Thread safety — inherently thread-safe
3. Security — class names, URLs, credentials can't be modified
4. Caching hashCode — compute once, reuse
5. HashMap key safety — hash won't change after insertion

**Q: What is String deduplication in G1 GC?**
```
-XX:+UseStringDeduplication  (Java 8u20+, G1 GC only)
G1 finds String objects with identical char[] arrays
and makes them share the same underlying char[].
Reduces heap usage by 10-30% in typical apps.
```

### 1.2 Collections Deep-Dive

**Q: Internal working of HashMap (the #1 most asked question)**
```
1. hashCode() → hash → index = hash & (capacity - 1)
2. If bucket empty → insert Node directly
3. If bucket occupied → check key equality
   - equals() true → replace value
   - equals() false → add to linked list (or TreeMap if size > 8)
4. If size > threshold (capacity × 0.75) → resize (double capacity)

Java 8 change: LinkedList → Red-Black Tree when bucket size > 8
  → Worst case O(n) → O(log n)
```

**Q: HashMap vs ConcurrentHashMap vs Hashtable**
| Feature | HashMap | ConcurrentHashMap | Hashtable |
|---------|---------|-------------------|-----------|
| Thread-safe | No | Yes (segment locking) | Yes (full lock) |
| Null keys | 1 allowed | Not allowed | Not allowed |
| Performance | Best (single-thread) | Good (concurrent) | Poor (full sync) |
| Iterator | Fail-fast | Weakly consistent | Fail-fast |

**Q: When does HashMap's get() return wrong value?**
- When key is mutable and its hashCode changes after insertion
- When equals() and hashCode() are not overridden properly
- When two keys have same hashCode AND same equals — last one wins

**Q: ArrayList vs LinkedList — when to use which?**
| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| get(i) | O(1) | O(n) |
| add(end) | O(1) amortized | O(1) |
| add(i) | O(n) — shift | O(1) — if at node |
| memory | Compact (array) | High (node + 2 pointers) |
| Cache | Friendly | Unfriendly |
**Verdict:** Almost always ArrayList. LinkedList wins only for frequent add/remove at head.

**Q: Fail-fast vs Fail-safe iterators**
```java
// Fail-fast: ConcurrentModificationException if modified during iteration
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
for (String s : list) {
    list.remove(s);  // ❌ ConcurrentModificationException
}

// Fix: Use Iterator.remove() or CopyOnWriteArrayList
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    it.next();
    it.remove();  // ✅ Safe
}
```

### 1.3 Multithreading Essentials

**Q: Create a deadlock (write code)**
```java
Object lock1 = new Object(), lock2 = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock1) {
        Thread.sleep(100);  // ensure t2 acquires lock2
        synchronized (lock2) { System.out.println("t1"); }
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lock2) {
        Thread.sleep(100);
        synchronized (lock1) { System.out.println("t2"); }
    }
});
// Deadlock! t1 holds lock1, waits for lock2. t2 holds lock2, waits for lock1.
```

**Q: How to prevent deadlock?**
1. **Lock ordering** — always acquire locks in same order
2. **tryLock with timeout** — `ReentrantLock.tryLock(1, TimeUnit.SECONDS)`
3. **Avoid nested locks** — minimize synchronized blocks
4. **Use java.util.concurrent** — higher-level abstractions

**Q: volatile vs synchronized vs Atomic**
| | volatile | synchronized | AtomicInteger |
|--|---------|-------------|---------------|
| Visibility | Yes | Yes | Yes |
| Atomicity | No (only read/write) | Yes | Yes (CAS) |
| Blocking | No | Yes | No |
| Use for | Flags, status | Critical sections | Counters, accumulators |

**Q: Thread pool — why not create threads directly?**
```java
// ❌ Bad: new Thread for each task
new Thread(() -> handleRequest()).start();  // Expensive! ~1MB stack per thread

// ✅ Good: Thread pool reuses threads
ExecutorService pool = Executors.newFixedThreadPool(10);
pool.submit(() -> handleRequest());

// ✅ Better: Custom pool with bounded queue
new ThreadPoolExecutor(
    5,              // core threads
    20,             // max threads
    60, TimeUnit.SECONDS,  // idle timeout
    new LinkedBlockingQueue<>(100),  // bounded queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // rejection policy
);
```

**Q: CompletableFuture — chain async operations**
```java
CompletableFuture.supplyAsync(() -> fetchUser(id))
    .thenApply(user -> enrichProfile(user))
    .thenAccept(profile -> sendNotification(profile))
    .exceptionally(ex -> { log.error("Failed", ex); return null; });
```

### 1.4 Exception Handling Patterns

**Q: Checked vs Unchecked — when to use which?**
```
Checked (extends Exception):
  - Recoverable conditions
  - Caller CAN and SHOULD handle
  - Examples: IOException, SQLException

Unchecked (extends RuntimeException):
  - Programming errors
  - Caller usually CANNOT recover
  - Examples: NullPointerException, IllegalArgumentException
  
Product company rule of thumb:
  - Service layer: throw unchecked (IllegalArgumentException, custom RuntimeExceptions)
  - Integration layer: wrap checked in unchecked
  - Controller: handle via @ExceptionHandler
```

**Q: try-with-resources — how does it work internally?**
```java
// AutoCloseable interface → close() called automatically
try (Connection conn = getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // use resources
}  // close() called in REVERSE order: ps.close() → conn.close()
// Even if exception thrown → resources still closed
// Suppressed exceptions captured via addSuppressed()
```

### 1.5 Java 8-17 Features (Rapid Fire)

| Feature | Java Version | One-liner |
|---------|-------------|-----------|
| Lambdas & Streams | 8 | Functional programming in Java |
| Optional | 8 | Avoid NullPointerException |
| Default methods | 8 | Methods in interfaces |
| var (local type inference) | 10 | `var list = new ArrayList<String>()` |
| Records | 16 | Immutable data classes in 1 line |
| Sealed classes | 17 | Restrict which classes can extend |
| Pattern matching instanceof | 16 | `if (obj instanceof String s)` |
| Text blocks | 15 | Multi-line strings `"""..."""` |
| Switch expressions | 14 | `var x = switch(y) { case A -> 1; }` |

---

## Part 2: Spring Boot — Generic Questions

### 2.1 Auto-Configuration (The Most Asked Spring Boot Question)

**Q: How does Spring Boot auto-configuration work?**
```
1. @SpringBootApplication = @Configuration + @ComponentScan + @EnableAutoConfiguration
2. @EnableAutoConfiguration triggers AutoConfigurationImportSelector
3. Reads META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
4. Each auto-config class has @Conditional annotations:
   - @ConditionalOnClass — only if class on classpath
   - @ConditionalOnMissingBean — only if you haven't defined your own
   - @ConditionalOnProperty — only if property set
5. You can ALWAYS override auto-config by defining your own @Bean
```

**Q: How to disable specific auto-configuration?**
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// or in application.yml
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### 2.2 Bean Scopes & Lifecycle

**Q: Spring Bean Scopes**
```
singleton  — One instance per IoC container (DEFAULT)
prototype  — New instance every time requested
request    — One per HTTP request (web only)
session    — One per HTTP session (web only)
application — One per ServletContext
```

**Q: Bean Lifecycle**
```
1. Instantiation (constructor)
2. Populate Properties (DI)
3. BeanNameAware.setBeanName()
4. BeanFactoryAware.setBeanFactory()
5. ApplicationContextAware.setApplicationContext()
6. @PostConstruct / InitializingBean.afterPropertiesSet()
7. ──── Bean is ready ────
8. @PreDestroy / DisposableBean.destroy()
```

### 2.3 Common Pitfalls

**Q: @Transactional doesn't work — why?**
```java
// ❌ Pitfall 1: Self-invocation (no proxy)
@Service
public class OrderService {
    public void process() {
        this.save();  // Direct call — bypasses Spring proxy!
    }
    @Transactional
    public void save() { }  // Transaction NOT applied
}

// ❌ Pitfall 2: Checked exception doesn't trigger rollback
@Transactional  // Only rolls back on RuntimeException by default
public void process() throws IOException {
    throw new IOException();  // NO ROLLBACK!
}
// Fix: @Transactional(rollbackFor = Exception.class)

// ❌ Pitfall 3: Private method
@Transactional
private void save() { }  // Spring AOP can't proxy private methods
```

**Q: Field injection vs Constructor injection**
```java
// ❌ Field injection — hard to test, hidden dependencies
@Autowired
private UserRepository repo;

// ✅ Constructor injection — explicit, testable, immutable
private final UserRepository repo;
public UserService(UserRepository repo) {  // @Autowired optional if single constructor
    this.repo = repo;
}
```

**Q: N+1 query problem in JPA**
```java
// ❌ N+1: Fetches 1 query for users + N queries for orders
List<User> users = userRepo.findAll();
users.forEach(u -> u.getOrders().size());  // Triggers N lazy loads!

// ✅ Fix 1: JOIN FETCH
@Query("SELECT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithOrders();

// ✅ Fix 2: @EntityGraph
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();

// ✅ Fix 3: @BatchSize
@BatchSize(size = 20)
@OneToMany(mappedBy = "user")
private List<Order> orders;
```

### 2.4 Spring Security

**Q: How does Spring Security filter chain work?**
```
HTTP Request
    ↓
SecurityFilterChain (ordered list of filters)
    ↓
1. CorsFilter
2. CsrfFilter
3. AuthenticationFilter (UsernamePassword / JWT / OAuth2)
4. AuthorizationFilter (role/permission checks)
5. ExceptionTranslationFilter
    ↓
Controller (if all filters pass)
```

**Q: JWT authentication flow**
```
1. Login: POST /auth/login {username, password}
2. Server validates → generates JWT (header.payload.signature)
3. Client stores JWT (localStorage/cookie)
4. Subsequent requests: Authorization: Bearer <token>
5. JwtAuthenticationFilter extracts token → validates → sets SecurityContext
6. Controller accesses authenticated user
```

---

## Part 3: Microservices — Generic Questions

**Q: Monolith vs Microservices — when to choose what?**
```
Start with Monolith when:
- Small team (< 5 developers)
- Domain not well understood yet
- Speed of initial development matters

Move to Microservices when:
- Independent deployability needed
- Different scaling requirements per service
- Multiple teams need to work independently
- Different tech stacks per service needed
```

**Q: How do microservices communicate?**
```
Synchronous:
- REST (HTTP) — simple, widely used
- gRPC — fast, binary, schema-enforced (internal services)
- GraphQL — client controls response shape (BFF pattern)

Asynchronous:
- Kafka — event streaming, high throughput
- RabbitMQ — message queue, routing, lower throughput
- SQS/SNS — AWS managed messaging
```

**Q: Distributed transaction patterns**
```
1. Saga Pattern:
   - Choreography: Each service publishes events
   - Orchestration: Central coordinator manages flow
   
2. Outbox Pattern:
   - Write event to outbox table in same DB transaction
   - Separate process publishes events from outbox
   
3. Two-Phase Commit (2PC):
   - Prepare → Commit/Rollback
   - Rarely used in microservices (blocking, slow)
```

**Q: Service discovery patterns**
```
Client-side: Service queries registry (Eureka, Consul)
Server-side: Load balancer routes to healthy instances (AWS ALB)
DNS-based: Kubernetes Services (kube-dns)
```

**Q: Circuit Breaker states and transitions**
```
CLOSED → (failure threshold reached) → OPEN
OPEN → (timeout) → HALF-OPEN
HALF-OPEN → (success) → CLOSED
HALF-OPEN → (failure) → OPEN
```

---

## Part 4: Database — Generic Questions

**Q: SQL query execution order**
```
Written:  SELECT → FROM → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT
Executed: FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
```

**Q: Index types and when to use**
```
B-Tree: Default. Range queries, sorting. Most common.
Hash: Exact match only. Faster for = comparisons.
Composite: Multi-column. Follow leftmost prefix rule.
Covering: All query columns in index → no table lookup needed.
Partial: Index subset of rows (WHERE clause).
```

**Q: ACID properties**
```
A — Atomicity: All or nothing
C — Consistency: Valid state to valid state
I — Isolation: Concurrent transactions don't interfere
D — Durability: Committed data survives crashes
```

**Q: Isolation levels**
| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|-------|-----------|-------------------|-------------|
| READ_UNCOMMITTED | Yes | Yes | Yes |
| READ_COMMITTED | No | Yes | Yes |
| REPEATABLE_READ | No | No | Yes |
| SERIALIZABLE | No | No | No |

**Q: SQL vs NoSQL — when to use what?**
```
SQL (MySQL, PostgreSQL):
- Structured data with relationships
- ACID transactions needed
- Complex queries (JOINs, aggregations)
- Data integrity is critical

NoSQL (MongoDB, DynamoDB, Cassandra):
- Flexible/evolving schema
- Horizontal scaling needed
- High write throughput
- Document/key-value/graph data models
```

**Q: Database sharding strategies**
```
1. Range-based: userId 1-1M → Shard 1, 1M-2M → Shard 2
   Pro: Simple. Con: Hot spots.
   
2. Hash-based: hash(userId) % numShards
   Pro: Even distribution. Con: Resharding is painful.
   
3. Directory-based: Lookup table maps key → shard
   Pro: Flexible. Con: Single point of failure.
```

**Q: Write a query — Second highest salary**
```sql
-- Method 1: LIMIT OFFSET
SELECT DISTINCT salary FROM employees ORDER BY salary DESC LIMIT 1 OFFSET 1;

-- Method 2: Subquery
SELECT MAX(salary) FROM employees WHERE salary < (SELECT MAX(salary) FROM employees);

-- Method 3: DENSE_RANK (for Nth highest)
SELECT salary FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) as rnk FROM employees
) ranked WHERE rnk = 2;
```

---

## Part 5: System Design — Generic Questions

**Q: URL Shortener (Most asked system design question)**
```
Requirements: 100M URLs/month, read-heavy (100:1 read/write)

Design:
1. Hashing: Base62 encode auto-increment ID → 7 chars = 3.5T unique URLs
2. Database: Key-value store (DynamoDB/Redis for cache)
3. API: POST /shorten {longUrl} → shortUrl, GET /{shortCode} → 301 redirect
4. Cache: Redis for hot URLs (80/20 rule)
5. Scale: Read replicas, CDN for redirect, rate limiting
```

**Q: Design a Rate Limiter**
```
Algorithms:
1. Token Bucket — tokens refill at fixed rate, request consumes token
2. Sliding Window Log — track timestamps of requests
3. Sliding Window Counter — fixed window + weight from previous window
4. Leaky Bucket — FIFO queue, process at constant rate

Where: API Gateway (global) or per-service (local)
Storage: Redis (INCR + EXPIRE for distributed)
```

**Q: Design a Notification System**
```
Components:
1. Notification Service — receives requests
2. Priority Queue — urgent vs regular
3. Channel Adapters — Email/SMS/Push/In-app
4. Template Engine — personalized messages
5. User Preferences — opt-in/out per channel
6. Rate Limiter — prevent notification fatigue
7. Analytics — delivery tracking, open rates
```

---

## Part 6: React & Frontend — Generic Questions

**Q: Virtual DOM — how does React work internally?**
```
1. State/props change → new Virtual DOM tree created
2. Diffing: Compare new tree vs old tree (reconciliation)
3. Batch updates: Calculate minimal set of DOM operations
4. Commit phase: Apply changes to real DOM

Why? Real DOM manipulation is expensive. Batch updates = fewer reflows/repaints.
```

**Q: useCallback vs useMemo vs React.memo**
```javascript
// React.memo — skip re-render if props unchanged (component level)
const Child = React.memo(({ data }) => <div>{data}</div>);

// useMemo — memoize expensive computation (value level)
const sorted = useMemo(() => items.sort(), [items]);

// useCallback — memoize function reference (function level)
const handleClick = useCallback(() => setCount(c => c + 1), []);
```

**Q: State management — when to use what?**
```
Local state (useState): Component-specific, non-shared
Context API: Theme, auth, small shared state
Redux/Zustand: Complex shared state, time-travel debugging needed
React Query/SWR: Server state (API data, caching, sync)
URL params: Shareable/bookmarkable state
```

---

## Part 7: DevOps & Cloud — Generic Questions

**Q: Docker vs VM**
```
Docker:     OS-level virtualization, shares host kernel, lightweight, seconds to start
VM:         Hardware-level virtualization, full OS, heavy, minutes to start
```

**Q: Kubernetes core concepts**
```
Pod — smallest deployable unit (1+ containers)
Service — stable network endpoint for pods
Deployment — manages ReplicaSets, rolling updates
Ingress — HTTP routing, TLS termination
ConfigMap/Secret — external configuration
HPA — Horizontal Pod Autoscaler
```

**Q: CI/CD pipeline stages**
```
1. Source → Git push triggers pipeline
2. Build → Compile, package (Maven/Gradle/npm)
3. Test → Unit + Integration tests
4. Static Analysis → SonarQube, security scanning
5. Docker Build → Create container image
6. Push → Container registry (ECR/DockerHub)
7. Deploy → Staging (smoke tests) → Production (canary/blue-green)
8. Monitor → Health checks, alerts, rollback if needed
```

**Q: Blue-Green vs Canary vs Rolling deployment**
```
Blue-Green: Two identical environments. Switch traffic instantly.
  Pro: Zero downtime, instant rollback. Con: 2x infrastructure.
  
Canary: Route small % of traffic to new version.
  Pro: Gradual rollout, catch issues early. Con: Complex routing.
  
Rolling: Replace instances one by one.
  Pro: No extra infrastructure. Con: Mixed versions during deploy.
```

---

## Part 8: Tricky/Output Questions (Product Company Favorites)

### Java Output Questions

```java
// Q: What's the output?
public class Test {
    public static void main(String[] args) {
        System.out.println(10 + 20 + "Hello");     // "30Hello"
        System.out.println("Hello" + 10 + 20);     // "Hello1020"
        System.out.println("Hello" + (10 + 20));    // "Hello30"
    }
}
```

```java
// Q: What's the output?
public class Test {
    static int x = 10;
    static { x = 20; }
    public static void main(String[] args) {
        System.out.println(x);  // 20 — static block runs after static field init
    }
    static { x = 30; }  // This runs too!
}
// Output: 30 (static blocks execute in order of appearance)
```

```java
// Q: Can you override static methods?
class Parent {
    static void show() { System.out.println("Parent"); }
}
class Child extends Parent {
    static void show() { System.out.println("Child"); }
}
Parent p = new Child();
p.show();  // "Parent" — static methods are resolved by REFERENCE TYPE, not object type
// This is METHOD HIDING, not overriding
```

```java
// Q: What's the output?
List<Integer> list = Arrays.asList(1, 2, 3);
list.add(4);  // ❌ UnsupportedOperationException!
// Arrays.asList returns a FIXED-SIZE list backed by the array
// Use: new ArrayList<>(Arrays.asList(1, 2, 3)) for mutable list
```

---

## Part 9: Estimation Questions

**Q: How would you estimate the storage needed for a system?**
```
Framework:
1. Identify entities and their sizes
2. Estimate daily creation rate
3. Calculate for retention period

Example: Chat system (WhatsApp-like)
- Users: 500M, Message: ~100 bytes avg
- Messages/day: 50B (100 messages × 500M users)
- Daily storage: 50B × 100B = 5TB/day
- 5 years: 5TB × 365 × 5 ≈ 9PB
- With replication (3x): ~27PB
```

---

## Quick Revision: Top 50 Questions That WILL Be Asked

| # | Question | Topic |
|---|----------|-------|
| 1 | HashMap internal working | Collections |
| 2 | String immutability — why? | Core Java |
| 3 | equals vs == | Core Java |
| 4 | Abstract class vs Interface | OOP |
| 5 | SOLID principles | OOP |
| 6 | Singleton pattern (thread-safe) | Design Patterns |
| 7 | synchronized vs volatile vs Atomic | Multithreading |
| 8 | Create a deadlock | Multithreading |
| 9 | Thread pool — why and how | Multithreading |
| 10 | Stream vs Collection | Java 8 |
| 11 | Optional — how to use properly | Java 8 |
| 12 | Functional interfaces | Java 8 |
| 13 | Spring Bean lifecycle | Spring |
| 14 | @Transactional — pitfalls | Spring |
| 15 | Spring Security filter chain | Spring Security |
| 16 | REST vs gRPC vs GraphQL | API Design |
| 17 | Saga pattern | Microservices |
| 18 | Circuit breaker | Microservices |
| 19 | CAP theorem | Distributed Systems |
| 20 | ACID properties | Database |
| 21 | Indexing — B-Tree | Database |
| 22 | N+1 query problem | JPA/ORM |
| 23 | SQL query execution order | Database |
| 24 | Second highest salary query | SQL |
| 25 | Normalization (1NF, 2NF, 3NF) | Database |
| 26 | Docker vs VM | DevOps |
| 27 | Kubernetes pods/services | DevOps |
| 28 | CI/CD pipeline stages | DevOps |
| 29 | URL shortener design | System Design |
| 30 | Rate limiter design | System Design |
| 31 | Virtual DOM | React |
| 32 | useEffect cleanup | React |
| 33 | Redux data flow | React |
| 34 | Stack vs Heap | JVM |
| 35 | Garbage Collection — generational | JVM |
| 36 | Memory leak in Java | JVM |
| 37 | String Pool | JVM |
| 38 | ClassLoader hierarchy | JVM |
| 39 | Two Sum (Arrays + Hashing) | DSA |
| 40 | LRU Cache (LinkedList + HashMap) | DSA |
| 41 | Binary Search variations | DSA |
| 42 | BFS/DFS on graphs | DSA |
| 43 | DP — Coin Change / Knapsack | DSA |
| 44 | Tell me about yourself | Behavioral |
| 45 | Biggest failure | Behavioral |
| 46 | Conflict with teammate | Behavioral |
| 47 | Why this company? | Behavioral |
| 48 | Production incident story | Behavioral |
| 49 | Ownership beyond role | Behavioral |
| 50 | Salary expectations | HR |

---

> **Pro tip:** If you can confidently answer all 50 questions above, you'll clear 90% of product company interviews.
