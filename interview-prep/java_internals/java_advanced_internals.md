# Java Advanced Internals — Interview Q&A

> Concepts from [Snailclimb/JavaGuide](https://github.com/Snailclimb/JavaGuide) (155K ⭐) & [winterbe/java8-tutorial](https://github.com/winterbe/java8-tutorial) (16.8K ⭐)
> Covers: Reflection, IO/NIO, Dynamic Proxy, SPI, Unsafe, AQS, Atomic Classes
> **Priority: P1** — Advanced topics asked at mid-senior level interviews

---

## Q1. Java Reflection API — How does it work and when to use?

```
Reflection = inspect and manipulate classes, methods, fields at RUNTIME

Core Classes (java.lang.reflect):
  Class<?>    — entry point, represents a loaded class
  Method      — represents a method
  Field       — represents a field
  Constructor — represents a constructor

Three Ways to Get Class Object:
  Class<?> clazz1 = String.class;                    // compile-time
  Class<?> clazz2 = "hello".getClass();              // from instance
  Class<?> clazz3 = Class.forName("java.lang.String"); // dynamic

Common Operations:
  // Get all declared methods (including private)
  Method[] methods = clazz.getDeclaredMethods();

  // Access private field
  Field field = clazz.getDeclaredField("value");
  field.setAccessible(true);  // bypass access check
  Object value = field.get(instance);

  // Invoke method dynamically
  Method method = clazz.getMethod("length");
  int len = (int) method.invoke("hello");  // 5

  // Create instance dynamically
  Constructor<?> ctor = clazz.getConstructor(String.class);
  Object obj = ctor.newInstance("hello");

When to Use:
  ✓ Frameworks (Spring, Hibernate — dependency injection, ORM mapping)
  ✓ Serialization/Deserialization (Jackson, Gson)
  ✓ Testing (accessing private methods)
  ✓ Annotation processing at runtime

Disadvantages:
  ✗ 2-50x slower than direct calls
  ✗ Breaks encapsulation (setAccessible bypasses access control)
  ✗ No compile-time type safety
  ✗ Security restrictions in certain environments
  ✗ Breaks with Java Module System (JPMS) — strong encapsulation

Interview Tip: "Spring uses reflection to discover @Autowired fields,
  instantiate beans, and inject dependencies at runtime."
```

---

## Q2. Java Dynamic Proxy — JDK Proxy vs CGLIB.

```
Proxy = intercept method calls without modifying target code
Used heavily by Spring AOP, Hibernate lazy loading, MyBatis

JDK Dynamic Proxy (java.lang.reflect.Proxy):
  - Target MUST implement an interface
  - Creates proxy implementing same interface
  - Uses InvocationHandler for interception

  public class LoggingHandler implements InvocationHandler {
      private final Object target;

      public LoggingHandler(Object target) {
          this.target = target;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args)
              throws Throwable {
          System.out.println("Before: " + method.getName());
          Object result = method.invoke(target, args);
          System.out.println("After: " + method.getName());
          return result;
      }
  }

  // Create proxy
  UserService proxy = (UserService) Proxy.newProxyInstance(
      UserService.class.getClassLoader(),
      new Class[]{UserService.class},
      new LoggingHandler(new UserServiceImpl())
  );

CGLIB Proxy:
  - Target doesn't need interface
  - Creates subclass of target (bytecode generation)
  - Cannot proxy final classes/methods
  - Used by Spring when no interface exists

Comparison:
  ┌─────────────────┬──────────────────┬──────────────────┐
  │                 │ JDK Proxy        │ CGLIB            │
  ├─────────────────┼──────────────────┼──────────────────┤
  │ Requirement     │ Interface needed │ No interface     │
  │ Mechanism       │ InvocationHandler│ Subclassing      │
  │ Speed (create)  │ Faster           │ Slower           │
  │ Speed (invoke)  │ Slower           │ Faster           │
  │ Final classes   │ N/A              │ Cannot proxy     │
  │ Spring default  │ With interface   │ Without interface│
  └─────────────────┴──────────────────┴──────────────────┘

Spring Boot 2.x+: CGLIB is default even with interfaces
  (spring.aop.proxy-target-class=true by default)
```

---

## Q3. Java IO Models — BIO, NIO, AIO.

```
Five IO Models (from Unix):
  1. Blocking IO (BIO)      — thread blocks until data ready
  2. Non-blocking IO (NIO)  — thread polls, returns immediately
  3. IO Multiplexing         — select/poll/epoll, one thread many connections
  4. Signal-driven IO        — kernel notifies when data ready
  5. Asynchronous IO (AIO)   — kernel completes IO, notifies when done

Java IO Evolution:
  ┌───────────────┬──────────────┬──────────────┬──────────────┐
  │               │ Java IO(BIO) │ Java NIO     │ Java AIO     │
  │               │ (JDK 1.0)    │ (JDK 1.4)    │ (JDK 1.7)    │
  ├───────────────┼──────────────┼──────────────┼──────────────┤
  │ Model         │ Blocking     │ Non-blocking  │ Async        │
  │ Data          │ Stream-based │ Buffer-based  │ Buffer-based │
  │ Threading     │ 1 thread/conn│ 1 thread/many │ Callback     │
  │ API           │ InputStream  │ Channel+Buffer│ AsynchronousChannel│
  │ Use case      │ Low conn     │ High conn     │ File I/O     │
  └───────────────┴──────────────┴──────────────┴──────────────┘

Java NIO Core Components:
  1. Buffer — container for data (ByteBuffer, CharBuffer)
     ByteBuffer buf = ByteBuffer.allocate(1024);
     buf.put(data);          // write mode
     buf.flip();             // switch to read mode
     buf.get();              // read data
     buf.clear();            // reset for writing

  2. Channel — bidirectional data pipe
     FileChannel, SocketChannel, ServerSocketChannel, DatagramChannel

  3. Selector — monitors multiple channels (IO multiplexing)
     Selector selector = Selector.open();
     channel.register(selector, SelectionKey.OP_READ);
     selector.select();  // blocks until a channel is ready

NIO Selector Pattern (1 thread, 10000 connections):
  ┌──────────┐
  │ Selector │ ← monitors all channels
  └────┬─────┘
       ├── Channel 1 (ready to read)
       ├── Channel 2 (idle)
       ├── Channel 3 (ready to write)
       └── Channel N (idle)

Interview Tip: "Netty, the most popular Java network framework,
  is built on top of NIO. It powers gRPC, Spring WebFlux,
  and many high-performance servers."
```

---

## Q4. Java SPI (Service Provider Interface) — Plugin mechanism.

```
SPI = discover and load implementations at runtime
Java's built-in plugin/extension mechanism

How It Works:
  1. Define interface: public interface Parser { ... }
  2. Create implementation: public class JsonParser implements Parser { ... }
  3. Register in: META-INF/services/com.example.Parser
     → file contains: com.example.JsonParser
  4. Load:
     ServiceLoader<Parser> loader = ServiceLoader.load(Parser.class);
     for (Parser parser : loader) {
         parser.parse(data);  // discovers all implementations
     }

Real-World Uses:
  - JDBC drivers (java.sql.Driver)
  - Logging (SLF4J discovers LoggerFactory)
  - Servlet containers
  - Spring Boot auto-configuration

SPI vs API:
  API: caller uses interface, provider implements
  SPI: framework defines interface, extensions implement
       Framework discovers extensions automatically

Limitations:
  ✗ Loads ALL implementations (no lazy/selective loading)
  ✗ No dependency injection
  ✗ Spring uses its own META-INF/spring.factories (Spring SPI)
     (replaced by META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports in 3.x)
```

---

## Q5. AQS (AbstractQueuedSynchronizer) — Foundation of Java locks.

```
AQS = framework for building locks and synchronizers
  Base class for: ReentrantLock, Semaphore, CountDownLatch,
                  ReadWriteLock, CyclicBarrier

Core Mechanism:
  ┌─────────────────────────────────────────────┐
  │              AQS State (volatile int)       │
  │                                              │
  │  state = 0 → lock is free                   │
  │  state = 1 → lock is held                   │
  │  state = N → reentrant count                │
  │                                              │
  │  CLH Queue (FIFO wait queue):               │
  │  head ← [T1|waiting] ← [T2|waiting] ← tail │
  └─────────────────────────────────────────────┘

How ReentrantLock Works (via AQS):
  1. Thread A calls lock()
     → CAS(state, 0, 1) succeeds → A gets lock
  2. Thread B calls lock()
     → CAS(state, 0, 1) fails → B enters CLH queue, parks
  3. Thread A calls unlock()
     → state = 0, unparks head of queue (Thread B)
  4. Thread B wakes up, retries CAS, gets lock

Fair vs Unfair Lock:
  Fair:   new thread goes to end of queue (ordered, slower)
  Unfair: new thread tries CAS first (may cut queue, faster)
  Default: Unfair (better throughput in most cases)

Exclusive vs Shared Mode:
  Exclusive: only one thread (ReentrantLock, state = 0 or 1)
  Shared:    multiple threads (Semaphore, ReadLock)
             state = permits remaining
```

---

## Q6. Atomic Classes — Lock-free thread safety.

```
java.util.concurrent.atomic — CAS-based thread safety

Class Hierarchy:
  AtomicInteger, AtomicLong, AtomicBoolean
  AtomicReference<V>
  AtomicIntegerArray, AtomicLongArray
  AtomicStampedReference (solves ABA problem)
  LongAdder, LongAccumulator (high-contention counters)

CAS (Compare-And-Swap):
  compareAndSet(expected, update)
  → if current == expected, set to update, return true
  → else return false (retry)
  → single CPU instruction, no lock needed

  AtomicInteger counter = new AtomicInteger(0);
  counter.incrementAndGet();      // thread-safe ++
  counter.compareAndSet(5, 10);   // if 5, set to 10

ABA Problem:
  Thread 1: reads A
  Thread 2: changes A → B → A
  Thread 1: CAS succeeds (thinks nothing changed!)

  Solution: AtomicStampedReference — tracks version stamp
  atomicRef.compareAndSet(A, B, stamp, stamp + 1);

LongAdder vs AtomicLong:
  ┌─────────────────┬──────────────────┬──────────────────┐
  │                 │ AtomicLong       │ LongAdder        │
  ├─────────────────┼──────────────────┼──────────────────┤
  │ Strategy        │ Single CAS var   │ Multiple cells   │
  │ Low contention  │ Similar          │ Similar          │
  │ High contention │ Slow (CAS retry) │ Fast (spread)    │
  │ Exact read      │ Yes              │ Approximate*     │
  │ Use case        │ Single counter   │ Metrics/stats    │
  └─────────────────┴──────────────────┴──────────────────┘
  * sum() is eventually consistent during concurrent updates

  // High-contention counter — use LongAdder
  LongAdder adder = new LongAdder();
  adder.increment();
  long total = adder.sum();
```

---

## Q7. Java Unsafe Class — Low-level magic.

```
sun.misc.Unsafe — direct memory access, bypasses JVM safety

Cannot instantiate normally:
  Field f = Unsafe.class.getDeclaredField("theUnsafe");
  f.setAccessible(true);
  Unsafe unsafe = (Unsafe) f.get(null);

Capabilities:
  1. Direct Memory: allocateMemory(), freeMemory()
     → Used by Netty's off-heap ByteBuf, DirectByteBuffer
  2. CAS Operations: compareAndSwapInt()
     → Used by AtomicInteger, ConcurrentHashMap
  3. Object Creation: allocateInstance() — skip constructor
  4. Memory Barriers: loadFence(), storeFence()

Where It's Used:
  - java.util.concurrent (all CAS operations)
  - Netty (off-heap memory management)
  - Kafka (direct memory for zero-copy)
  - Jackson, Kryo (fast serialization)

Warning: Direct use is discouraged and being restricted in newer Java versions
  Java 9+: VarHandle replaces some Unsafe CAS operations
  Java 22+: Foreign Function & Memory API replaces memory access
```

---

## Q8. Java Concurrent Collections Deep Dive.

```
ConcurrentHashMap (Java 8+):
  - Array of Nodes + CAS + synchronized (per-bucket)
  - No full-table lock!
  - Tree bins when bucket > 8 elements (like HashMap)
  - Supports 16+ concurrent writers by default

  ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
  map.computeIfAbsent("key", k -> expensiveCompute(k));  // atomic
  map.merge("key", 1, Integer::sum);  // atomic increment

CopyOnWriteArrayList:
  - Copy entire array on every write
  - Zero-cost reads (no locking)
  - Use when: reads >> writes (e.g., listener lists, config)

  CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
  // Safe to iterate while another thread adds

BlockingQueue Implementations:
  ┌──────────────────────┬────────────┬─────────────────────────┐
  │ Implementation       │ Bounded    │ Use Case                │
  ├──────────────────────┼────────────┼─────────────────────────┤
  │ ArrayBlockingQueue   │ Yes        │ Fixed-size work queue   │
  │ LinkedBlockingQueue  │ Optional   │ ThreadPoolExecutor      │
  │ PriorityBlockingQueue│ No         │ Priority task processing│
  │ DelayQueue           │ No         │ Scheduled tasks         │
  │ SynchronousQueue     │ No (0 cap) │ Direct handoff          │
  └──────────────────────┴────────────┴─────────────────────────┘

  // Producer-Consumer pattern
  BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);
  queue.put(task);   // blocks if full
  queue.take();      // blocks if empty
```

---

## Q9. Java Module System (JPMS) — Java 9+.

```
JPMS = Java Platform Module System (Project Jigsaw)

Problem: JAR Hell
  - No encapsulation (all public classes accessible)
  - Version conflicts
  - Classpath ordering issues
  - Monolithic JDK (rt.jar was 60MB+)

Module = a JAR with a module-info.java
  // module-info.java
  module com.myapp.service {
      requires java.sql;            // dependency
      requires transitive com.myapp.model; // transitive dep
      exports com.myapp.service.api;       // what we expose
      opens com.myapp.service.internal to spring.core; // reflection access
      provides com.myapp.spi.Parser with com.myapp.JsonParser; // SPI
  }

Key Directives:
  requires    — declare dependency on another module
  exports     — make package available to other modules
  opens       — allow reflection access (needed for Spring, Hibernate)
  provides    — register SPI implementation
  uses        — consume SPI service

Impact on Interviews:
  Q: "Why does Spring need 'opens' in module-info?"
  A: Spring uses reflection for dependency injection.
     Without 'opens', JPMS blocks reflective access to internal packages.

  Q: "Why is JPMS not widely adopted?"
  A: Most libraries/frameworks rely on classpath-based classloading
     and deep reflection. Migration requires explicit 'opens' directives.
     Still useful for JDK itself (modular JDK, jlink custom runtimes).
```

---

## Q10. Concurrent Programming Patterns in Practice.

```
Pattern 1: Producer-Consumer with BlockingQueue
  ExecutorService producers = Executors.newFixedThreadPool(3);
  ExecutorService consumers = Executors.newFixedThreadPool(3);
  BlockingQueue<Order> queue = new LinkedBlockingQueue<>(1000);

  // Producer
  producers.submit(() -> {
      Order order = receiveOrder();
      queue.put(order);  // blocks if full
  });

  // Consumer
  consumers.submit(() -> {
      Order order = queue.take();  // blocks if empty
      processOrder(order);
  });

Pattern 2: Scatter-Gather with CompletableFuture
  CompletableFuture<Price> amazon = CompletableFuture.supplyAsync(
      () -> fetchPrice("amazon"));
  CompletableFuture<Price> flipkart = CompletableFuture.supplyAsync(
      () -> fetchPrice("flipkart"));

  CompletableFuture<Price> best = amazon.thenCombine(flipkart,
      (a, f) -> a.compareTo(f) < 0 ? a : f);

Pattern 3: Rate Limiter with Semaphore
  Semaphore limiter = new Semaphore(10); // 10 concurrent

  void handleRequest(Request req) {
      if (limiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
          try {
              process(req);
          } finally {
              limiter.release();
          }
      } else {
          throw new TooManyRequestsException();
      }
  }

Pattern 4: Read-Write with StampedLock (Java 8+)
  StampedLock lock = new StampedLock();

  // Optimistic read (no locking!)
  long stamp = lock.tryOptimisticRead();
  int x = this.x, y = this.y;
  if (!lock.validate(stamp)) {
      // Fallback to full read lock
      stamp = lock.readLock();
      try { x = this.x; y = this.y; }
      finally { lock.unlockRead(stamp); }
  }
```

---

*Sources: [Snailclimb/JavaGuide](https://github.com/Snailclimb/JavaGuide), [winterbe/java8-tutorial](https://github.com/winterbe/java8-tutorial), JDK documentation*
