# Java Internals & Memory Management — Interview Q&A

> 25+ questions covering JVM, GC, Memory Model, ClassLoading, String Pool  
> These are asked at EVERY product-based company (Amazon, Google, Flipkart, Razorpay)

---

## JVM Architecture

### Q1. Explain JVM architecture. What happens when you run `java MyApp`?

```
Source Code (.java) → javac → Bytecode (.class) → JVM → Machine Code

JVM Architecture:
┌─────────────────────────────────────────────────────────────────┐
│                        JVM                                       │
│  ┌──────────────┐                                                │
│  │ Class Loader  │ ← Loading → Linking → Initialization          │
│  │  Subsystem    │                                                │
│  └──────┬───────┘                                                │
│         ↓                                                        │
│  ┌──────────────────────────────────────────┐                    │
│  │         Runtime Data Areas                │                    │
│  │  ┌──────────┐  ┌────────┐  ┌──────────┐  │                    │
│  │  │  Method   │  │  Heap  │  │  Stack   │  │                    │
│  │  │  Area     │  │(shared)│  │(per thd) │  │                    │
│  │  └──────────┘  └────────┘  └──────────┘  │                    │
│  │  ┌──────────┐  ┌────────────────────┐     │                    │
│  │  │    PC    │  │ Native Method Stack│     │                    │
│  │  │ Register │  └────────────────────┘     │                    │
│  │  └──────────┘                             │                    │
│  └──────────────────────────────────────────┘                    │
│         ↓                                                        │
│  ┌──────────────┐                                                │
│  │Execution     │ ← Interpreter + JIT Compiler + GC              │
│  │Engine        │                                                │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

**Step by step:**
1. **ClassLoader** loads `.class` file into memory
2. **Bytecode Verifier** checks for security violations
3. **Interpreter** executes bytecode line by line (slow)
4. **JIT Compiler** identifies hot methods → compiles to native machine code (fast)
5. **GC** reclaims unused memory in the background

---

### Q2. Stack vs Heap memory — what goes where?

| Stack | Heap |
|-------|------|
| Method frames, local variables, references | Objects, instance variables |
| Per-thread (thread-safe by default) | Shared across all threads |
| LIFO, fixed size | Dynamic size |
| Fast allocation/deallocation | Slower, managed by GC |
| StackOverflowError if exceeded | OutOfMemoryError if exceeded |

```java
public void process() {
    int x = 10;                       // x → Stack
    String name = "Karthik";          // name (ref) → Stack, "Karthik" → String Pool (Heap)
    Employee emp = new Employee();    // emp (ref) → Stack, Employee object → Heap
    List<String> list = new ArrayList<>(); // list (ref) → Stack, ArrayList object → Heap
}
// When process() returns → Stack frame destroyed, refs gone
// Objects on Heap → eligible for GC when no more refs point to them
```

---

### Q3. What are the different memory areas in JVM?

```
1. Method Area (Metaspace in Java 8+)
   - Class metadata, static variables, constant pool
   - Shared across all threads
   
2. Heap
   - All objects and arrays
   - Divided into: Young Gen (Eden + Survivor) + Old Gen
   
3. Stack (per thread)
   - Method call frames
   - Local variables, partial results, return addresses
   
4. PC Register (per thread)
   - Address of currently executing JVM instruction
   
5. Native Method Stack (per thread)
   - For native (C/C++) method calls via JNI
```

---

## Garbage Collection

### Q4. How does Garbage Collection work? Explain generational GC.

```
Heap Layout (Generational):

┌────────────────────────────────────────────────────────────┐
│                          HEAP                               │
│  ┌───────────────────────────┐  ┌────────────────────────┐  │
│  │      Young Generation      │  │    Old Generation      │  │
│  │  ┌──────┐ ┌─────┐ ┌─────┐ │  │    (Tenured)           │  │
│  │  │ Eden │ │ S0  │ │ S1  │ │  │                        │  │
│  │  └──────┘ └─────┘ └─────┘ │  │                        │  │
│  └───────────────────────────┘  └────────────────────────┘  │
│                                                              │
│  ┌──────────────────────┐                                    │
│  │    Metaspace          │ (outside heap, in native memory)  │
│  └──────────────────────┘                                    │
└────────────────────────────────────────────────────────────┘

Flow:
1. New objects → Eden
2. Eden full → Minor GC → surviving objects → S0
3. Next Minor GC → Eden + S0 survivors → S1 (swap S0/S1)
4. After N survivals (default 15) → promoted to Old Gen
5. Old Gen full → Major GC (Full GC) → Stop-The-World pause
```

**Why generational?** "Weak Generational Hypothesis" — most objects die young. Minor GC is fast because it only scans Young Gen.

---

### Q5. Types of Garbage Collectors in Java.

| GC | Type | Best for | Pause time |
|----|------|----------|------------|
| **Serial GC** | Single-threaded | Small apps, client JVMs | Long pauses |
| **Parallel GC** | Multi-threaded | Throughput-focused servers | Medium pauses |
| **G1 GC** (default Java 9+) | Concurrent + region-based | General purpose, large heaps | Low pauses |
| **ZGC** (Java 15+) | Concurrent, sub-ms pauses | Ultra-low latency | < 1ms |
| **Shenandoah** | Concurrent, low pause | Low latency | < 10ms |

```bash
# Set GC type
java -XX:+UseG1GC -jar app.jar        # G1 (default)
java -XX:+UseZGC -jar app.jar          # ZGC for ultra-low latency
java -XX:+UseParallelGC -jar app.jar   # Parallel for throughput
```

---

### Q6. What are GC roots? How does GC determine which objects to collect?

**GC Roots** are the starting points for reachability analysis:
1. **Local variables** in active threads' stack frames
2. **Static variables** of loaded classes
3. **JNI references** from native code
4. **Active threads** themselves

```
GC Root → Object A → Object B → Object C    ← All REACHABLE (alive)

Object D → Object E                          ← UNREACHABLE (eligible for GC)
(nothing points to D)
```

**Mark-and-Sweep:**
1. **Mark:** Start from GC roots, traverse all references, mark as alive
2. **Sweep:** Delete all unmarked objects
3. **Compact:** (optional) Move alive objects together to reduce fragmentation

---

### Q7. Memory leak in Java — how is that possible with GC?

**GC can't collect objects that are still referenced**, even if you'll never use them.

```java
// ❌ Common memory leak patterns

// 1. Static collections that grow forever
static List<Object> cache = new ArrayList<>();
void processRequest(Object data) {
    cache.add(data);  // Never removed! Grows until OOM
}

// 2. Listeners/callbacks not deregistered
eventBus.register(this);  // If 'this' is never unregistered → leak

// 3. Inner classes holding reference to outer class
class Outer {
    byte[] heavyData = new byte[10_000_000];
    class Inner {  // Inner holds implicit ref to Outer
        void doStuff() { }
    }
}

// 4. Unclosed resources
Connection conn = dataSource.getConnection();
// Forgot to close → connection object stays in memory

// 5. HashMap with mutable keys
Map<MutableKey, String> map = new HashMap<>();
map.put(key, "value");
key.setId(999);  // Hash changed! Entry can never be found/removed
```

**Detection tools:** VisualVM, JProfiler, Eclipse MAT, `jmap -histo`

---

## String Pool & Immutability

### Q8. Explain String Pool. Why is String immutable?

```java
String s1 = "hello";           // Goes to String Pool
String s2 = "hello";           // Reuses same object from Pool
String s3 = new String("hello"); // NEW object on Heap (bypasses pool)

System.out.println(s1 == s2);       // true  (same reference)
System.out.println(s1 == s3);       // false (different objects)
System.out.println(s1.equals(s3));  // true  (same content)

s3 = s3.intern();              // Moves s3 to pool
System.out.println(s1 == s3);  // true (now same reference)
```

**Why immutable?**
1. **String Pool works** only because strings are immutable (safe to share)
2. **Thread safety** — immutable = inherently thread-safe
3. **Security** — class names, URLs, DB credentials as strings can't be tampered
4. **Caching hashCode** — computed once, cached, since value never changes
5. **HashMap key safety** — hash won't change after insertion

---

### Q9. `String` vs `StringBuilder` vs `StringBuffer`?

| | String | StringBuilder | StringBuffer |
|---|--------|--------------|--------------|
| Mutability | Immutable | Mutable | Mutable |
| Thread-safe | Yes (immutable) | No | Yes (synchronized) |
| Performance | Slow for concatenation | Fastest | Slower than StringBuilder |
| Use when | Few modifications | Single-threaded append | Multi-threaded append |

```java
// ❌ Bad: Creates N intermediate String objects
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i;  // O(n²) — new String each iteration!
}

// ✅ Good: O(n)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

---

## Class Loading

### Q10. Explain ClassLoader hierarchy.

```
Bootstrap ClassLoader (C/C++, loads rt.jar — java.lang.*, java.util.*)
       ↓
Extension ClassLoader (loads jre/lib/ext)
       ↓
Application ClassLoader (loads classpath — your code)
       ↓
Custom ClassLoaders (e.g., Tomcat loads each webapp in isolation)
```

**Delegation model:** Child asks parent first. If parent can't load → child tries.  
**Why?** Security — prevents user code from replacing core Java classes.

```java
// Check which ClassLoader loaded a class
System.out.println(String.class.getClassLoader());       // null (Bootstrap)
System.out.println(MyApp.class.getClassLoader());        // AppClassLoader
```

---

## Tricky Output Questions (Product Company Favorites)

### Q11. What's the output?
```java
String s1 = "abc";
String s2 = "abc";
String s3 = new String("abc");
String s4 = new String("abc").intern();

System.out.println(s1 == s2);       // true  — same pool reference
System.out.println(s1 == s3);       // false — s3 is on heap
System.out.println(s1 == s4);       // true  — intern() returns pool ref
System.out.println(s1.equals(s3));  // true  — same content
```

### Q12. What's the output?
```java
Integer a = 127;
Integer b = 127;
Integer c = 128;
Integer d = 128;

System.out.println(a == b);  // true  — Integer cache [-128, 127]
System.out.println(c == d);  // false — outside cache range, different objects
System.out.println(c.equals(d));  // true — same value
```

### Q13. What's the output?
```java
System.out.println('a' + 'b');           // 195 (97 + 98, char arithmetic)
System.out.println("a" + "b");           // "ab" (string concatenation)
System.out.println('a' + 'b' + "c");     // "195c" (195 + "c")
System.out.println("c" + 'a' + 'b');     // "cab" (string concat left to right)
```

### Q14. What's the output?
```java
try {
    return 1;
} finally {
    return 2;
}
// Returns 2! finally block's return overrides try block's return.
// ⚠️ NEVER return from finally block.
```

### Q15. What's the output?
```java
public static void main(String[] args) {
    String x = "abc";
    modify(x);
    System.out.println(x);  // "abc" — Java is PASS BY VALUE!
}
static void modify(String s) {
    s = "xyz";  // local reference changed, original unaffected
}
```

---

## Java Memory Tuning

### Q16. Common JVM flags for production.

```bash
# Heap size
-Xms512m          # Initial heap
-Xmx2g            # Maximum heap
-XX:MaxMetaspaceSize=256m  # Metaspace limit

# GC settings
-XX:+UseG1GC                    # Use G1 collector
-XX:MaxGCPauseMillis=200        # Target max pause time
-XX:+HeapDumpOnOutOfMemoryError # Dump heap on OOM
-XX:HeapDumpPath=/tmp/heapdump.hprof

# Monitoring
-XX:+PrintGCDetails             # GC logs
-Xlog:gc*                       # Java 9+ unified logging
```

### Q17. How to diagnose OutOfMemoryError?

```bash
# 1. Heap dump analysis
jmap -dump:live,format=b,file=heap.hprof <pid>
# Open in Eclipse MAT or VisualVM → Find largest objects

# 2. GC logs
-Xlog:gc*:file=gc.log  # Analyze with GCViewer

# 3. Common OOM types:
java.lang.OutOfMemoryError: Java heap space     → Increase -Xmx or fix leak
java.lang.OutOfMemoryError: Metaspace            → Increase MaxMetaspaceSize
java.lang.OutOfMemoryError: GC overhead limit    → GC using >98% time, <2% freed
java.lang.StackOverflowError                     → Infinite recursion or deep call stack
```

---

## equals(), hashCode(), and the Contract

### Q18. Explain the equals/hashCode contract. Why must you override both?

```java
// THE CONTRACT:
// 1. If a.equals(b) → a.hashCode() == b.hashCode()  (MANDATORY)
// 2. If a.hashCode() == b.hashCode() → a.equals(b) may be true or false (collisions OK)
// 3. If !a.equals(b) → hashCodes may or may not be equal

// ❌ BROKEN: Override equals but not hashCode
class Employee {
    private int id;
    private String name;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee e = (Employee) o;
        return id == e.id && Objects.equals(name, e.name);
    }
    // Missing hashCode! → HashMap won't work correctly
}

// ✅ CORRECT: Override both
@Override
public int hashCode() {
    return Objects.hash(id, name);
}
```

**What breaks without hashCode?**
```java
Set<Employee> set = new HashSet<>();
Employee e1 = new Employee(1, "Karthik");
Employee e2 = new Employee(1, "Karthik");
set.add(e1);
set.add(e2);
// Without hashCode override: set.size() = 2 (WRONG! should be 1)
// With hashCode override: set.size() = 1 (CORRECT)
```

---

## Serialization

### Q19. What is serialization? What is `serialVersionUID`?

```java
// Serialization = converting object → byte stream (for storage/network)
// Deserialization = byte stream → object

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Version control
    private String name;
    private transient String password; // NOT serialized (security)
    private static int count;          // NOT serialized (static = class-level)
}
```

**serialVersionUID:** If you modify the class (add/remove fields) and don't change this, deserialization may fail with `InvalidClassException`. Always declare it explicitly.

---

## Generics

### Q20. What is type erasure? What's the difference between `<? extends T>` and `<? super T>`?

```java
// Type erasure: Generics are compile-time only. At runtime, List<String> = List
// This is why you can't do: new T(), instanceof T, or T.class

// PECS: Producer Extends, Consumer Super

// ✅ Extends — READ from a generic collection (producer)
List<? extends Number> numbers = getNumbers();
Number n = numbers.get(0);    // OK — read as Number
// numbers.add(42);            // ❌ Can't add — don't know exact type

// ✅ Super — WRITE to a generic collection (consumer)
List<? super Integer> ints = getInts();
ints.add(42);                 // OK — Integer or any superclass
// Integer i = ints.get(0);   // ❌ Can't read as Integer — might be Number
```

---

## Concurrency Internals

### Q21. What is the Java Memory Model (JMM)?

```
Thread 1          Thread 2
┌──────────┐     ┌──────────┐
│ Local    │     │ Local    │
│ Cache    │     │ Cache    │
└────┬─────┘     └────┬─────┘
     │                │
     └───────┬────────┘
             │
     ┌───────┴────────┐
     │  Main Memory   │
     │  (Heap)        │
     └────────────────┘
```

**Problem:** Threads may cache variables locally. Changes by Thread 1 may NOT be visible to Thread 2.

**Solutions:**
- `volatile` — reads/writes go directly to main memory
- `synchronized` — establishes happens-before relationship
- `Atomic*` classes — use CAS (compare-and-swap) for lock-free thread safety

```java
// ❌ Broken double-checked locking (without volatile)
private static Instance instance;
if (instance == null) {
    synchronized(this) {
        if (instance == null) {
            instance = new Instance(); // May be partially constructed!
        }
    }
}

// ✅ Fixed with volatile
private static volatile Instance instance;
```

---

### Q22. `final` keyword — different contexts.

```java
final int x = 10;              // Variable: value can't change
final List<String> list = ...;  // Reference can't change, CONTENTS can!
list.add("hello");             // ✅ OK! Only ref is final

final void method() { }        // Method: can't be overridden
final class MyClass { }        // Class: can't be extended (e.g., String, Integer)
```

**Effectively final (Java 8+):** A variable that isn't declared final but never reassigned. Required for lambdas.
```java
String name = "Karthik";  // effectively final
Runnable r = () -> System.out.println(name);  // OK
// name = "other";  // If uncommented → compilation error in lambda
```

---

### Q23. Immutable class — how to create one?

```java
// Rules: final class, private final fields, no setters, defensive copies
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    
    // If field is mutable (like Date), return defensive copy:
    // return new Date(date.getTime());
}

// Java 16+ Records are immutable by default:
public record Money(BigDecimal amount, Currency currency) { }
```

---

### Q24. Marker interfaces vs Annotations?

| Marker Interface | Annotation |
|-----------------|------------|
| `Serializable`, `Cloneable` | `@Override`, `@Deprecated` |
| Compile-time type check | Runtime processing possible |
| Empty interface | Can carry metadata |
| Older Java pattern | Modern approach |

```java
// Marker interface — empty, just "marks" a type
public interface Serializable { }

// Annotation — can carry data
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cacheable {
    int ttl() default 300;
}
```

---

### Q25. What happens when you create `new Object()`?

```
1. ClassLoader checks if Object.class is loaded → loads if not
2. JVM allocates memory on Heap (in Eden space)
3. Memory zeroed out (fields get default values: 0, null, false)
4. Constructor chain executed (Object() → super constructors)
5. Reference returned to caller (stored on Stack)
```

Object header in memory:
```
| Mark Word (8 bytes) | Class Pointer (4-8 bytes) | Instance Data | Padding |
  - hash code                                         - fields       - align to 8 bytes
  - GC age (4 bits)
  - lock state
  - thread ID (if biased)
```
**Minimum object size:** 16 bytes (even empty object).

---

### Q22. How does JIT (Just-In-Time) Compilation work? Explain tiered compilation.

**Answer:**

Java code goes through 2 compilation stages:
```
.java → javac → .class (bytecode) → JVM interpreter → machine code (JIT)
```

**Why JIT?** Interpreter is slow. JIT compiles "hot" methods to native machine code at runtime for near-C performance.

**Tiered Compilation (default since Java 8):**
```
Level 0: Interpreter          ← all methods start here
Level 1: Simple C1 compiled   ← basic optimizations
Level 2: C1 + profiling       ← gathers statistics (branch counts, type info)
Level 3: Full C1 + profiling  ← more detailed profiling
Level 4: C2 optimized         ← aggressive optimizations (inlining, loop unrolling, escape analysis)
```

**C1 vs C2 compilers:**
| Feature | C1 (Client) | C2 (Server) |
|---------|-------------|-------------|
| Speed | Fast compilation | Slow compilation |
| Optimization | Light | Aggressive |
| Use case | Startup speed | Peak throughput |
| Triggered at | ~1,500 invocations | ~10,000 invocations |

**Key JIT Optimizations:**
- **Method inlining:** Replace method call with method body (biggest win)
- **Loop unrolling:** Reduce loop overhead by repeating body
- **Escape analysis:** If object doesn't escape method → allocate on STACK (no GC!)
- **Dead code elimination:** Remove unreachable code paths
- **Null check elimination:** Skip redundant null checks after first proven non-null

**Deoptimization:** If C2's assumption is invalidated (e.g., new class loaded that breaks type speculation), JIT decompiles back to interpreter and re-profiles.

```bash
# JVM flags to observe JIT:
-XX:+PrintCompilation          # show what JIT compiles
-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining  # show inlining decisions
-XX:CompileThreshold=5000      # customize hot method threshold
-XX:-TieredCompilation          # disable tiered (use only C2)

# GraalVM: Alternative JIT compiler written in Java (ahead-of-time compilation option)
```

**Interview Q: Why is the first request to a Java app slow?**
→ JIT hasn't compiled hot paths yet. All code runs in interpreter. After warmup (~30s-2min), performance stabilizes. Solutions: JVM warmup scripts, CDS (Class Data Sharing), AOT compilation (GraalVM native-image).
