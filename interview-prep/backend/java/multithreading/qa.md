# Multithreading & Concurrency — Interview Q&A

> 20 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas  
> Difficulty: Hatio = baseline, FedEx/NPCI = harder  
> **🔴 P0 GAP — This is a critical interview topic. NPCI will probe deeply.**

---

## Conceptual Questions

### Q1. What are the different ways to create a thread in Java? Which is preferred and why?

**1. Extend Thread class:**
```java
class MyThread extends Thread {
    public void run() { System.out.println("Running"); }
}
new MyThread().start();
```

**2. Implement Runnable (preferred):**
```java
Runnable task = () -> System.out.println("Running");
new Thread(task).start();
```

**3. Implement Callable (returns result + can throw checked exceptions):**
```java
Callable<Integer> task = () -> { return 42; };
Future<Integer> future = executor.submit(task);
int result = future.get(); // blocks until complete
```

**4. ExecutorService (production standard):**
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> processEvent());
executor.shutdown();
```

**Why Runnable/Callable > extending Thread:**
- Java allows only single inheritance. Extending Thread wastes it.
- Runnable separates task from execution mechanism (SRP).
- Callable can return results and throw checked exceptions.
- ExecutorService manages thread lifecycle, pooling, and scheduling.

---

### Q2. Explain the thread lifecycle (states) in Java.

```
NEW → (start()) → RUNNABLE → (gets CPU) → RUNNING
  ↓                   ↕
  ↓              BLOCKED (waiting for monitor lock)
  ↓              WAITING (wait(), join(), park())
  ↓              TIMED_WAITING (sleep(), wait(timeout), join(timeout))
  ↓
TERMINATED (run() completes or exception)
```

| State | Trigger | Exit |
|-------|---------|------|
| NEW | `new Thread()` | `start()` |
| RUNNABLE | `start()` / notified / lock acquired | Gets CPU / blocks / waits |
| BLOCKED | Waiting for `synchronized` lock | Lock acquired |
| WAITING | `wait()`, `join()`, `LockSupport.park()` | `notify()`, thread completes, `unpark()` |
| TIMED_WAITING | `sleep(ms)`, `wait(ms)`, `join(ms)` | Timeout expires or notified |
| TERMINATED | `run()` returns or throws | — |

---

### Q3. What is the difference between `synchronized`, `ReentrantLock`, and `ReadWriteLock`?

| Feature | `synchronized` | `ReentrantLock` | `ReadWriteLock` |
|---------|---------------|-----------------|-----------------|
| Type | JVM intrinsic (keyword) | java.util.concurrent class | Interface with read + write locks |
| Fairness | Not configurable | `new ReentrantLock(true)` for fair | Configurable |
| Try-lock | ❌ | `tryLock(timeout)` | `readLock().tryLock()` |
| Interruptible | ❌ | `lockInterruptibly()` | Yes |
| Condition variables | 1 per monitor (`wait/notify`) | Multiple `Condition` objects | — |
| Performance | Good for low contention | Better for high contention | Best when reads >> writes |

```java
// ReentrantLock with try-finally (CRITICAL pattern)
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // critical section
} finally {
    lock.unlock(); // MUST be in finally — otherwise deadlock on exception
}
```

**At NPCI:** `ReadWriteLock` for transaction rate-limit config — hundreds of threads read the limit, one admin thread updates it. Read lock allows concurrent reads; write lock is exclusive.

---

### Q4. Explain `volatile` keyword. When is it sufficient vs when do you need `synchronized`?

**`volatile` guarantees:**
1. **Visibility** — write to volatile var is immediately visible to all threads
2. **Ordering** — prevents instruction reordering around volatile access (happens-before relationship)

**`volatile` does NOT guarantee atomicity:**
```java
volatile int count = 0;
count++; // NOT atomic! Read → increment → write = 3 operations
// Two threads can both read 0, increment to 1, write 1 → lost update
```

**When volatile is sufficient:**
- Single writer, multiple readers (flag pattern)
- Writing a reference (e.g., `volatile Config config;` — replacing config object)

```java
// Classic volatile flag pattern
volatile boolean running = true;
// Thread 1: while (running) { doWork(); }
// Thread 2: running = false; // Thread 1 sees this immediately
```

**When you need synchronized/atomic:**
- Read-modify-write operations (count++, check-then-act)
- Multiple related variables that must be updated atomically

---

### Q5. What is `CompletableFuture`? How does it differ from `Future`?

| Feature | Future | CompletableFuture |
|---------|--------|-------------------|
| Get result | `get()` (blocking only) | `get()` + non-blocking callbacks |
| Chain operations | ❌ | `thenApply()`, `thenCompose()`, `thenAccept()` |
| Combine futures | ❌ | `allOf()`, `anyOf()`, `thenCombine()` |
| Exception handling | Must catch from `get()` | `exceptionally()`, `handle()` |
| Create completed | ❌ | `completedFuture()`, `complete()` |

```java
CompletableFuture.supplyAsync(() -> fetchProviderData(npi))
    .thenApply(data -> enrichWithSpecialties(data))
    .thenApply(data -> validateAndFormat(data))
    .thenAccept(data -> saveToDatabase(data))
    .exceptionally(ex -> { log.error("Pipeline failed", ex); return null; });
```

**At UHG:** My Practice Profile uses `CompletableFuture.allOf()` to fetch provider demographics, specialties, and locations in parallel, then merge results:
```java
CompletableFuture<Demographics> demo = CompletableFuture.supplyAsync(() -> fetchDemo(npi));
CompletableFuture<List<Specialty>> specs = CompletableFuture.supplyAsync(() -> fetchSpecs(npi));
CompletableFuture<List<Location>> locs = CompletableFuture.supplyAsync(() -> fetchLocs(npi));

CompletableFuture.allOf(demo, specs, locs).thenRun(() -> {
    ProviderProfile profile = merge(demo.join(), specs.join(), locs.join());
    cache.put(npi, profile);
});
```

---

## Scenario-Based Questions

### Q6. At FedEx, the SEFS-PDDV service processes scan events from STARV scanners. During peak hours, you get 10K events/second. The current single-threaded processor can't keep up. How would you redesign it using concurrency?

```java
// Producer-Consumer with bounded queue
BlockingQueue<ScanEvent> eventQueue = new LinkedBlockingQueue<>(50_000);

// Producer — receives from Kafka
class EventReceiver implements Runnable {
    public void run() {
        kafkaConsumer.poll().forEach(event -> {
            if (!eventQueue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                metrics.increment("events.dropped");
            }
        });
    }
}

// Consumer pool — processes events
ExecutorService processors = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);

for (int i = 0; i < numProcessors; i++) {
    processors.submit(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            ScanEvent event = eventQueue.poll(500, TimeUnit.MILLISECONDS);
            if (event != null) processEvent(event);
        }
    });
}
```

**Why `LinkedBlockingQueue` with capacity?** Unbounded queue can cause OOM if consumers can't keep up. Bounded queue applies backpressure — producer drops/requeues when full.

**Alternative:** Use Kafka consumer groups with multiple partitions — let Kafka handle the parallelism. Each partition → one consumer thread.

---

### Q7. At NPCI, a UPI transaction must complete within 800ms end-to-end. The flow is: validate (100ms) → debit (200ms) → credit (200ms) → notify (50ms). Some steps can run in parallel. How would you optimize with CompletableFuture?

```java
public CompletableFuture<TransactionResult> processTransaction(UpiRequest req) {
    return CompletableFuture.supplyAsync(() -> validate(req))    // 100ms
        .thenCompose(validated -> {
            // Debit and Credit can run in parallel after validation
            CompletableFuture<DebitResult> debit = 
                CompletableFuture.supplyAsync(() -> debit(validated));
            CompletableFuture<CreditResult> credit = 
                CompletableFuture.supplyAsync(() -> credit(validated));
            
            return debit.thenCombine(credit, (d, c) -> new TransferResult(d, c));
        })
        .thenApplyAsync(result -> notify(result))  // 50ms, non-blocking
        .orTimeout(800, TimeUnit.MILLISECONDS)       // Hard timeout
        .exceptionally(ex -> {
            if (ex.getCause() instanceof TimeoutException) {
                return TransactionResult.timeout();
            }
            return TransactionResult.failed(ex);
        });
}
```

**With parallel debit+credit:** Total = 100ms + max(200ms, 200ms) + 50ms = **350ms** (down from 550ms sequential).

**`orTimeout()`** (Java 9+) — completes exceptionally with `TimeoutException` if not done in time. Critical for SLA compliance.

---

### Q8. At Hatio, your settlement batch job processes 50K merchant settlements nightly. Currently takes 3 hours. How would you parallelize it safely, ensuring no merchant is double-processed?

```java
List<Merchant> merchants = merchantRepo.findAllForSettlement();

// Partition into chunks
List<List<Merchant>> chunks = Lists.partition(merchants, 100); // Guava

ExecutorService executor = Executors.newFixedThreadPool(10);
List<Future<BatchResult>> futures = new ArrayList<>();

for (List<Merchant> chunk : chunks) {
    futures.add(executor.submit(() -> processChunk(chunk)));
}

// Collect results
int success = 0, failed = 0;
for (Future<BatchResult> f : futures) {
    try {
        BatchResult r = f.get(30, TimeUnit.MINUTES);
        success += r.successCount();
        failed += r.failCount();
    } catch (TimeoutException e) {
        log.error("Chunk timed out");
        failed += 100;
    }
}
```

**No double-processing because:** Each chunk is a disjoint partition. No merchant appears in two chunks. No shared mutable state between chunk processors.

**Database safety:** Each `processChunk()` runs in its own transaction. If one chunk fails, others still commit.

---

### Q9. Explain the deadlock problem. How do you prevent and detect it at FedEx?

**Deadlock conditions (ALL 4 must hold):**
1. Mutual exclusion — resource held exclusively
2. Hold and wait — thread holds one resource, waits for another
3. No preemption — resources can't be forcibly taken
4. Circular wait — A waits for B, B waits for A

```java
// Classic deadlock
Object lockA = new Object(), lockB = new Object();

// Thread 1:
synchronized (lockA) { synchronized (lockB) { /* work */ } }

// Thread 2:
synchronized (lockB) { synchronized (lockA) { /* work */ } }  // ❌ DEADLOCK
```

**Prevention:**
1. **Lock ordering** — always acquire locks in the same order (e.g., alphabetical by resource ID)
2. **tryLock with timeout** — `lock.tryLock(5, SECONDS)` + retry or fail
3. **Single lock** — reduce granularity (tradeoff: less concurrency)

**Detection at FedEx:**
- Thread dump analysis: `jstack <pid>` shows "Found one Java-level deadlock"
- AppDynamics detects thread deadlocks automatically
- JMX `ThreadMXBean.findDeadlockedThreads()`

---

### Q10. What is the `ThreadLocal` class? When would you use it at NPCI?

`ThreadLocal` gives each thread its own copy of a variable — no synchronization needed.

```java
// Each thread gets its own SimpleDateFormat (which is NOT thread-safe)
private static final ThreadLocal<SimpleDateFormat> dateFormatter = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// In any thread:
String formatted = dateFormatter.get().format(new Date()); // Thread-safe!
```

**At NPCI use cases:**
1. **Transaction context propagation** — store current transaction ID for logging across method calls without passing it as a parameter
2. **Database connection per thread** — each thread gets its own connection from pool
3. **User session in web request** — Spring's `RequestContextHolder` uses ThreadLocal

**⚠️ CAUTION with thread pools:** ThreadLocal values persist across task executions on the same pooled thread. Always `remove()` after use:
```java
try {
    threadLocal.set(value);
    // process
} finally {
    threadLocal.remove(); // CRITICAL — prevents memory leaks and stale data
}
```

---

### Q11. At FedEx, you need to implement a rate limiter that allows max 100 API calls per second per client. How would you implement this thread-safely?

```java
class RateLimiter {
    private final int maxPermits;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    
    public RateLimiter(int permitsPerSecond) {
        this.maxPermits = permitsPerSecond;
        this.semaphore = new Semaphore(permitsPerSecond);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Replenish permits every second
        scheduler.scheduleAtFixedRate(() -> {
            int permitsToRelease = maxPermits - semaphore.availablePermits();
            if (permitsToRelease > 0) semaphore.release(permitsToRelease);
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }
    
    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }
}
```

**Alternative: Token Bucket with `AtomicLong`** — more precise, allows bursts:
```java
class TokenBucket {
    private final AtomicLong tokens;
    private final long maxTokens;
    private final long refillRate; // tokens per ms
    private volatile long lastRefillTime;
    
    boolean tryConsume() {
        refill();
        return tokens.getAndDecrement() > 0;
    }
}
```

---

## Coding Challenges

### Challenge 1: Producer-Consumer with BlockingQueue
**File:** `solutions/ProducerConsumer.java`  
Implement a producer-consumer system:
1. Producer generates 1000 random `Transaction` objects and puts them in a `BlockingQueue`
2. 4 Consumer threads take from the queue and "process" (print) them
3. Use a poison pill pattern to signal consumers to stop
4. Print total processed count — must equal 1000

### Challenge 2: Parallel File Processor
**File:** `solutions/ParallelFileProcessor.java`  
Simulate processing 20 files concurrently:
1. Use `ExecutorService` with a pool of 5 threads
2. Each "file" takes random 100-500ms to process
3. Use `CompletableFuture` to chain: read → transform → write
4. Collect all results with `CompletableFuture.allOf()`
5. Print total time (should be ~4x faster than sequential)

### Challenge 3: Thread-Safe Singleton
**File:** `solutions/ThreadSafeSingleton.java`  
Implement 3 different thread-safe singleton patterns:
1. Double-checked locking with volatile
2. Bill Pugh (static inner class)
3. Enum singleton
4. Write a test that spawns 100 threads requesting the instance — all must get the same object

### Challenge 4: Dining Philosophers
**File:** `solutions/DiningPhilosophers.java`  
Implement the Dining Philosophers problem:
1. 5 philosophers, 5 forks (represented as `ReentrantLock`)
2. Each philosopher: think → pick up forks → eat → put down forks
3. Prevent deadlock using lock ordering
4. Run for 10 seconds, print stats (meals eaten per philosopher)

---

## Gotchas & Edge Cases

### Q12. What is a race condition? Give an example with `i++`.

```java
int count = 0; // shared
// Thread 1: count++ (read 0, increment, write 1)
// Thread 2: count++ (read 0, increment, write 1) — happens before T1 writes
// Result: count = 1 instead of 2. LOST UPDATE.
```

`i++` is NOT atomic — it's 3 operations: read → add → write. Fix: `AtomicInteger.incrementAndGet()` or `synchronized`.

---

### Q13. What is the difference between `wait()` and `sleep()`?

| | `wait()` | `sleep()` |
|-|----------|----------|
| Releases lock? | ✅ Yes | ❌ No |
| Called on | Object (monitor) | Thread |
| Must be in synchronized block? | ✅ Yes | ❌ No |
| Wakeup | `notify()`/`notifyAll()` or timeout | Timeout only |
| Purpose | Inter-thread communication | Pause execution |

**Interview trap:** Calling `wait()` outside a `synchronized` block → `IllegalMonitorStateException`.

---

### Q14. What is the `happens-before` relationship?

The JMM (Java Memory Model) guarantees:
1. **Within a thread:** Each action happens-before the next (program order)
2. **Monitor lock:** Unlock happens-before subsequent lock of same monitor
3. **Volatile:** Write happens-before subsequent read of same variable
4. **Thread start:** `thread.start()` happens-before any action in that thread
5. **Thread join:** All actions in thread happen-before `join()` returns
6. **Transitivity:** If A happens-before B, and B happens-before C, then A happens-before C

**Why it matters:** Without happens-before, compiler/CPU can reorder instructions. Thread 2 might see Thread 1's writes in a different order than they were coded.

---

### Q15. What is a `Phaser` and how does it differ from `CountDownLatch` and `CyclicBarrier`?

| Feature | CountDownLatch | CyclicBarrier | Phaser |
|---------|---------------|---------------|--------|
| Reusable | ❌ One-shot | ✅ Resets after each barrier | ✅ Advances phases |
| Dynamic parties | ❌ Fixed at creation | ❌ Fixed at creation | ✅ `register()`/`arriveAndDeregister()` |
| Use case | Wait for N tasks to complete | N threads meet at barrier point | Multi-phase algorithms |

```java
// CountDownLatch — "wait for 3 services to initialize"
CountDownLatch latch = new CountDownLatch(3);
// Each service: latch.countDown();
latch.await(); // Main thread waits

// CyclicBarrier — "all 5 threads must reach checkpoint before any proceed"
CyclicBarrier barrier = new CyclicBarrier(5, () -> System.out.println("Phase done"));
// Each thread: barrier.await();
```

---

### Q12. Explain ThreadPoolExecutor in detail — core pool size, max pool size, queue, rejection policies.

**Answer:**

```java
// Full constructor:
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                        // corePoolSize — threads always kept alive
    20,                       // maximumPoolSize — max threads under load
    60, TimeUnit.SECONDS,     // keepAliveTime — idle time for threads > core
    new LinkedBlockingQueue<>(100),  // workQueue — task buffer
    new ThreadPoolExecutor.CallerRunsPolicy()  // rejectionPolicy
);
```

**How tasks are scheduled:**
```
1. Task arrives → core thread available? → Execute immediately
2. Core threads busy → Queue full? NO → Add to queue
3. Queue full → Current threads < maxPoolSize? → Create new thread
4. maxPoolSize reached AND queue full → Rejection policy kicks in
```

**Rejection Policies:**
| Policy | Behavior |
|--------|----------|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` |
| `CallerRunsPolicy` | The calling thread runs the task itself (back-pressure) |
| `DiscardPolicy` | Silently drops the task |
| `DiscardOldestPolicy` | Drops the oldest queued task, retries |

**Executors factory methods (shortcuts):**
```java
Executors.newFixedThreadPool(10);      // core=max=10, unbounded queue ⚠️
Executors.newCachedThreadPool();        // core=0, max=Integer.MAX_VALUE ⚠️
Executors.newSingleThreadExecutor();    // core=max=1
Executors.newScheduledThreadPool(5);    // for delayed/periodic tasks
// ⚠️ WARNING: newFixedThreadPool uses unbounded LinkedBlockingQueue → OOM risk!
// ⚠️ WARNING: newCachedThreadPool can create unlimited threads → OOM risk!
// BEST PRACTICE: Always create ThreadPoolExecutor directly with bounded queue
```

---

### Q13. Explain the Fork/Join Framework.

**Answer:**
Fork/Join is designed for **divide-and-conquer** parallelism. It splits work recursively, then joins results.

```java
class SumTask extends RecursiveTask<Long> {
    private final int[] arr;
    private final int start, end;
    private static final int THRESHOLD = 1000;

    SumTask(int[] arr, int start, int end) {
        this.arr = arr; this.start = start; this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // Base case — compute directly
            long sum = 0;
            for (int i = start; i < end; i++) sum += arr[i];
            return sum;
        }
        // Fork — split into subtasks
        int mid = (start + end) / 2;
        SumTask left = new SumTask(arr, start, mid);
        SumTask right = new SumTask(arr, mid, end);
        left.fork();           // async execute left
        long rightResult = right.compute();  // compute right in current thread
        long leftResult = left.join();       // wait for left
        return leftResult + rightResult;
    }
}

// Usage:
ForkJoinPool pool = new ForkJoinPool();  // default: Runtime.availableProcessors()
long total = pool.invoke(new SumTask(array, 0, array.length));
```

**Work Stealing:** Idle threads steal tasks from busy threads' deques → maximizes CPU utilization.

**RecursiveTask vs RecursiveAction:**
- `RecursiveTask<V>` — returns a result (like `Callable`)
- `RecursiveAction` — no return value (like `Runnable`)

**Note:** `parallelStream()` uses the common `ForkJoinPool` internally.

---

### Q14. What are Virtual Threads (Java 21)? How do they differ from Platform Threads?

**Answer:**
Virtual threads are lightweight threads managed by the JVM, not the OS. Project Loom.

```java
// Creating virtual threads:
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Running on: " + Thread.currentThread());
});

// Using ExecutorService (recommended):
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Submit 1 MILLION tasks — each gets its own virtual thread
    IntStream.range(0, 1_000_000).forEach(i ->
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));  // I/O simulation
            return fetchFromDb(i);
        })
    );
}
// Executor auto-closes, waits for all tasks
```

**Platform Threads vs Virtual Threads:**
| Feature | Platform Thread | Virtual Thread |
|---------|----------------|----------------|
| Managed by | OS kernel | JVM scheduler |
| Memory | ~1 MB stack each | ~few KB (grows as needed) |
| Max count | ~10K (OS limit) | Millions |
| Best for | CPU-bound work | I/O-bound work |
| Scheduling | OS preemptive | JVM cooperative (at blocking points) |
| Thread pool? | Yes (reuse threads) | No (create per task — they're cheap) |

**When virtual threads block (DB call, HTTP, sleep):**
```
Virtual thread → unmounts from carrier thread → carrier freed for other virtual threads
                 → when I/O completes, virtual thread remounts on ANY carrier
```

**⚠️ Gotchas:**
```java
// DON'T use synchronized — it PINS the carrier thread
synchronized (lock) { /* I/O here pins carrier thread! */ }

// DO use ReentrantLock instead:
lock.lock();
try { /* I/O here — virtual thread properly unmounts */ }
finally { lock.unlock(); }

// DON'T use ThreadLocal (creates per-virtual-thread copies = memory waste)
// DO use ScopedValues (Java 21 preview):
static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
ScopedValue.where(CURRENT_USER, user).run(() -> handleRequest());
```

**Spring Boot 3.2+ integration:**
```yaml
spring:
  threads:
    virtual:
      enabled: true   # All request handling uses virtual threads!
```

---

### Q15. Structured Concurrency (Java 21 Preview) — Why is it better?

**Answer:**

```java
// PROBLEM with CompletableFuture:
var userFuture = CompletableFuture.supplyAsync(() -> getUser(id));    // starts
var orderFuture = CompletableFuture.supplyAsync(() -> getOrder(id));  // starts
// If getOrder() FAILS → getUser() keeps running (resource leak!)
// If main thread is interrupted → both keep running (leaked threads!)

// SOLUTION: Structured Concurrency
Response handle(long id) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<User> user   = scope.fork(() -> getUser(id));
        Subtask<Order> order = scope.fork(() -> getOrder(id));

        scope.join();            // Wait for both
        scope.throwIfFailed();   // Propagate first failure

        return new Response(user.get(), order.get());
    }
    // ✅ If ANY subtask fails → all others are cancelled
    // ✅ If parent is interrupted → all subtasks cancelled
    // ✅ Thread dump shows parent-child relationship
}
```

| Scope | Behavior |
|-------|----------|
| `ShutdownOnFailure` | Cancel all if ANY fails |
| `ShutdownOnSuccess` | Cancel all when FIRST succeeds (e.g., fastest mirror) |
