# Java Streams API — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas  
> Difficulty: Hatio = baseline, FedEx/NPCI = harder

---

## Conceptual Questions

### Q1. What is the difference between intermediate and terminal operations in Java Streams?

**Intermediate operations** return a new Stream and are lazy (not executed until a terminal operation is invoked). Examples: `filter()`, `map()`, `flatMap()`, `sorted()`, `distinct()`, `peek()`.

**Terminal operations** trigger the stream pipeline and produce a result or side-effect. Examples: `collect()`, `forEach()`, `reduce()`, `count()`, `findFirst()`, `anyMatch()`.

**Key insight:** Streams are evaluated lazily. If you chain 5 intermediate operations but never call a terminal operation, zero elements are processed.

```java
// Nothing happens — no terminal operation
list.stream().filter(x -> x > 5).map(x -> x * 2);

// NOW it executes
list.stream().filter(x -> x > 5).map(x -> x * 2).collect(Collectors.toList());
```

---

### Q2. How does `flatMap()` differ from `map()`? When would you use each?

| | `map()` | `flatMap()` |
|-|---------|-------------|
| Input → Output | `T → R` | `T → Stream<R>` |
| Result structure | Stream of wrapped values | Flattened stream |
| Use when | 1:1 transformation | 1:many transformation |

```java
// map: ["hello", "world"] → [["h","e","l","l","o"], ["w","o","r","l","d"]]
// flatMap: ["hello", "world"] → ["h","e","l","l","o","w","o","r","l","d"]

List<String> flat = words.stream()
    .flatMap(w -> Arrays.stream(w.split("")))
    .collect(Collectors.toList());
```

**Real use:** At FedEx, if each shipment has a list of scan events, and you need all events across all shipments → `flatMap()`.

---

### Q3. Explain `reduce()` — its three overloaded forms and when to use each.

```java
// 1. reduce(BinaryOperator) → Optional<T>
Optional<Integer> sum = list.stream().reduce(Integer::sum);

// 2. reduce(identity, BinaryOperator) → T
int sum = list.stream().reduce(0, Integer::sum);

// 3. reduce(identity, BiFunction, BinaryOperator) → U  [for parallel streams]
int totalLength = list.stream().reduce(0, (acc, s) -> acc + s.length(), Integer::sum);
```

**Form 3 is critical for parallel streams** — the combiner merges partial results from different threads. If you use form 2 with a type change (accumulator type ≠ stream type), it won't compile.

**Tradeoff:** `reduce()` produces a single value. If you need a mutable container (List, Map), use `collect()` instead — it's more efficient (avoids creating intermediate immutable objects).

---

### Q4. What are `Collectors.groupingBy()` and `Collectors.partitioningBy()`? How do they differ?

```java
// groupingBy — groups by a classifier function → Map<K, List<V>>
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));

// partitioningBy — splits by predicate → Map<Boolean, List<V>> (always exactly 2 keys)
Map<Boolean, List<Employee>> seniorSplit = employees.stream()
    .collect(Collectors.partitioningBy(e -> e.getYears() > 5));
```

`partitioningBy` always returns both `true` and `false` keys (even if one list is empty). `groupingBy` only has keys for groups that exist.

**Downstream collectors:** Both support a second argument for downstream aggregation:
```java
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));
```

---

### Q5. When should you use `parallelStream()`? What are the risks?

**Use when:**
- Large dataset (10,000+ elements, benchmark to confirm)
- CPU-bound operations (no I/O, no shared mutable state)
- Operations are associative and stateless
- The common ForkJoinPool isn't saturated by other tasks

**Risks:**
- **Shared mutable state** → race conditions
- **Ordering** → `forEachOrdered` is needed if order matters (kills parallelism benefit)
- **Common ForkJoinPool** → all parallel streams in the JVM share it. One slow stream blocks others.
- **Overhead** → for small collections, thread coordination overhead > processing time

**At NPCI:** Processing millions of UPI transaction records for daily reconciliation? `parallelStream()` with a custom ForkJoinPool:
```java
ForkJoinPool custom = new ForkJoinPool(8);
custom.submit(() ->
    transactions.parallelStream()
        .filter(t -> t.getStatus() == SETTLED)
        .collect(Collectors.groupingBy(Transaction::getBankCode))
).get();
```

---

## Scenario-Based Questions

### Q6. At FedEx, your SEFS-PDDV service receives 500K+ scan events per hour from STARV scanners. You need to deduplicate events by tracking number and extract the latest status per package. How would you implement this with Streams?

```java
Map<String, ScanEvent> latestByPackage = scanEvents.stream()
    .collect(Collectors.toMap(
        ScanEvent::getTrackingNumber,
        Function.identity(),
        (existing, replacement) -> 
            existing.getTimestamp().isAfter(replacement.getTimestamp()) 
                ? existing : replacement
    ));
```

**Why `toMap` with merge function over `groupingBy`?**  
`groupingBy` would create `Map<String, List<ScanEvent>>` holding ALL events in memory, then you'd still need to find max. `toMap` with merge keeps only the latest — O(1) space per tracking number vs O(n).

**Complexity:** O(n) time, O(k) space where k = unique tracking numbers.

**Gotcha:** If you need to preserve insertion order, use `Collectors.toMap(..., LinkedHashMap::new)` as the 4th argument.

---

### Q7. At Hatio/BillDesk, you're processing daily financial settlement records. Each record has `merchantId`, `amount`, and `status` (SUCCESS/FAILED/PENDING). Generate a summary: total amount per merchant for successful transactions, sorted by amount descending.

```java
Map<String, Double> settlementSummary = records.stream()
    .filter(r -> r.getStatus() == Status.SUCCESS)
    .collect(Collectors.groupingBy(
        SettlementRecord::getMerchantId,
        Collectors.summingDouble(SettlementRecord::getAmount)
    ))
    .entrySet().stream()
    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
    .collect(Collectors.toMap(
        Map.Entry::getKey, 
        Map.Entry::getValue,
        (a, b) -> a, 
        LinkedHashMap::new
    ));
```

**Why LinkedHashMap?** Regular HashMap doesn't preserve insertion order. After sorting, we need an ordered map. `LinkedHashMap::new` as the map supplier preserves the sorted order.

**Tradeoff:** If you only need top-10 merchants, don't sort the entire map. Use:
```java
.sorted(...).limit(10).collect(...)
```

---

### Q8. At NPCI, you're building a fraud detection module that analyzes UPI transaction patterns. Given a stream of transactions, find all users who made more than 10 transactions in the last 5 minutes with amounts exceeding ₹50,000 total.

```java
Instant fiveMinAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

Map<String, DoubleSummaryStatistics> suspicious = transactions.stream()
    .filter(t -> t.getTimestamp().isAfter(fiveMinAgo))
    .collect(Collectors.groupingBy(
        Transaction::getUserId,
        Collectors.summarizingDouble(Transaction::getAmount)
    ))
    .entrySet().stream()
    .filter(e -> e.getValue().getCount() > 10 && e.getValue().getSum() > 50000)
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

**`DoubleSummaryStatistics`** gives count, sum, min, max, average in one pass — no need for multiple grouping operations.

**In production:** This would be a sliding window in Kafka Streams/Flink, not a batch stream. But the Streams API logic for the filtering/aggregation pattern translates directly.

---

### Q9. You're migrating the UHG My Practice Profile API from REST to GraphQL. The old REST endpoint returns a list of providers, each with nested specialties and locations. How would you use Streams to transform the REST response DTO into a flat GraphQL-friendly structure?

```java
List<ProviderFlat> flatProviders = providers.stream()
    .flatMap(provider -> provider.getSpecialties().stream()
        .flatMap(specialty -> provider.getLocations().stream()
            .map(location -> new ProviderFlat(
                provider.getNpi(),
                provider.getName(),
                specialty.getCode(),
                specialty.getDescription(),
                location.getAddress(),
                location.getState()
            ))
        )
    )
    .collect(Collectors.toList());
```

**This is a Cartesian product** — each provider × specialties × locations. If a provider has 3 specialties and 2 locations, they produce 6 flat rows.

**Complexity:** O(P × S × L) where P=providers, S=avg specialties, L=avg locations. For 1000 providers with 3 specialties and 2 locations = 6000 rows. Acceptable.

**Gotcha:** If specialties or locations can be empty, use `Optional` or default to singleton list to avoid producing zero rows for that provider.

---

### Q10. At FedEx, you need to generate a daily report that groups Sort Package Scan events by facility → shift → scan type, with counts. The data arrives as a flat list. How would you build this nested grouping?

```java
Map<String, Map<String, Map<ScanType, Long>>> report = scanEvents.stream()
    .collect(Collectors.groupingBy(
        ScanEvent::getFacilityCode,
        Collectors.groupingBy(
            ScanEvent::getShift,
            Collectors.groupingBy(
                ScanEvent::getScanType,
                Collectors.counting()
            )
        )
    ));
```

**Multi-level groupingBy** composes cleanly. Each level adds a downstream collector.

**Tradeoff:** Deeply nested maps are hard to work with. If this report is consumed by a UI, consider flattening into a `ReportRow` record:
```java
record ReportRow(String facility, String shift, ScanType scanType, long count) {}
```
Then use `flatMap` on the nested map entries to produce `List<ReportRow>`.

---

### Q11. Your microservice at UHG processes Kafka messages containing provider updates. Some messages have null fields (e.g., missing phone numbers). Write a stream pipeline that safely processes these, skipping nulls without throwing NPE.

```java
List<String> validPhones = providerUpdates.stream()
    .map(ProviderUpdate::getPhoneNumber)     // might return null
    .filter(Objects::nonNull)                 // remove nulls
    .map(String::trim)
    .filter(phone -> !phone.isEmpty())
    .filter(phone -> phone.matches("\\d{10}"))
    .collect(Collectors.toList());
```

**Alternative with Optional (Java 9+):**
```java
List<String> validPhones = providerUpdates.stream()
    .map(ProviderUpdate::getPhoneNumber)
    .flatMap(Optional::ofNullable)  // converts null → empty stream, non-null → stream of 1
    .map(String::trim)
    .filter(phone -> phone.matches("\\d{10}"))
    .collect(Collectors.toList());
```

**`flatMap(Optional::ofNullable)`** is more idiomatic but slightly less readable for teams not used to it.

---

### Q12. At Hatio, your payment gateway processes transactions in batches. You receive a `List<BatchResult>` where each batch contains a `List<Transaction>`. You need the top 5 highest-value FAILED transactions across ALL batches. How?

```java
List<Transaction> top5Failed = batches.stream()
    .flatMap(batch -> batch.getTransactions().stream())
    .filter(t -> t.getStatus() == Status.FAILED)
    .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
    .limit(5)
    .collect(Collectors.toList());
```

**Performance note:** `sorted()` on the full stream is O(n log n). For "top K" problems, a heap-based approach is O(n log k). But Streams API doesn't have a built-in top-K. For interview purposes, `sorted().limit(k)` is acceptable. In production with millions of records, use a `PriorityQueue` manually.

---

### Q13. You need to convert a `List<Employee>` into a `Map<Department, Optional<Employee>>` where the value is the highest-paid employee per department. What collector would you use?

```java
Map<String, Optional<Employee>> topEarners = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));
```

**To avoid Optional in the map values** (cleaner API):
```java
Map<String, Employee> topEarners = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDepartment,
        Function.identity(),
        BinaryOperator.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));
```

The `toMap` with merge function approach is more concise and returns non-Optional values.

---

## Coding Challenges

> Solve these in `solutions/` directory. Problem statement only — no hints.

### Challenge 1: Transaction Analyzer
**File:** `solutions/TransactionAnalyzer.java`  
Given a list of `Transaction(String id, String merchantId, double amount, String status, LocalDateTime timestamp)`, implement:
1. `Map<String, DoubleSummaryStatistics> summaryByMerchant(List<Transaction> txns)` — group by merchant, return statistics for SUCCESS transactions only
2. `List<Transaction> topNByAmount(List<Transaction> txns, int n)` — return top N transactions by amount, across all statuses
3. `Map<String, Long> countByStatusPerMerchant(List<Transaction> txns)` — return a map where key = "merchantId:status", value = count

### Challenge 2: Event Stream Processor
**File:** `solutions/EventStreamProcessor.java`  
Given a list of `ScanEvent(String trackingNumber, String facility, Instant timestamp, String eventType)`:
1. `Map<String, ScanEvent> latestEventPerPackage(List<ScanEvent> events)` — deduplicate, keeping only the latest event per tracking number
2. `Map<String, List<String>> facilityRouteMap(List<ScanEvent> events)` — for each tracking number, return the ordered list of facilities it passed through (sorted by timestamp)
3. `List<String> findStuckPackages(List<ScanEvent> events, Duration threshold)` — find tracking numbers where the latest event is older than `threshold`

### Challenge 3: Word Frequency Counter
**File:** `solutions/WordFrequencyCounter.java`  
Given a list of strings (sentences):
1. `Map<String, Long> topKWords(List<String> sentences, int k)` — return the top K most frequent words (case-insensitive, ignore punctuation), ordered by frequency descending
2. `Map<Character, Long> charFrequency(List<String> sentences)` — character frequency across all sentences (ignore spaces)
3. `String longestPalindromeWord(List<String> sentences)` — find the longest palindrome word across all sentences

### Challenge 4: Parallel Stream Benchmark
**File:** `solutions/ParallelStreamBenchmark.java`  
Implement two versions of: "Given a list of 1 million random integers, find all prime numbers and return their sum."
1. `long sumPrimesSequential(List<Integer> numbers)` — using sequential stream
2. `long sumPrimesParallel(List<Integer> numbers)` — using parallel stream
3. Add a `main()` method that benchmarks both and prints execution times. Discuss in comments: when does parallel actually win?

---

## Gotchas & Edge Cases

### Q14. What happens if you reuse a Stream?

```java
Stream<String> stream = list.stream().filter(s -> s.length() > 3);
stream.forEach(System.out::println);
stream.count(); // ❌ IllegalStateException: stream has already been operated upon or closed
```

**Streams are single-use.** After a terminal operation, the stream is closed. You must create a new stream from the source.

---

### Q15. What's the difference between `findFirst()` and `findAny()`?

- `findFirst()` — returns the **first element** in encounter order. Deterministic.
- `findAny()` — returns **any element**. In sequential streams, behaves like `findFirst()`. In parallel streams, returns whichever element completes first (non-deterministic).

**Use `findAny()` with parallel streams** when you don't care about order — it's faster because it doesn't need to coordinate ordering.

---

### Q16. Can Streams modify the source collection?

**No — and doing so causes `ConcurrentModificationException`:**
```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
list.stream().forEach(s -> list.remove(s)); // ❌ ConcurrentModificationException
```

**Even `peek()` should not modify state** — it's meant for debugging only. Using it to mutate elements is an anti-pattern that breaks with parallel streams.

---

### Q17. What is a "stateful" intermediate operation and why does it matter for parallel streams?

**Stateful operations** need information about other elements to process the current element:
- `sorted()` — needs all elements
- `distinct()` — needs to track seen elements
- `limit()` / `skip()` — needs position information

**In parallel streams**, stateful operations force synchronization barriers, reducing parallelism benefits. `sorted()` on a parallel stream may be slower than sequential because it must merge sorted sub-results.

**Stateless operations** (`filter`, `map`, `flatMap`) process each element independently → perfect for parallelism.

---

### Q18. What's the difference between `Stream.of()` and `Arrays.stream()`?

```java
int[] arr = {1, 2, 3};
Stream.of(arr);           // → Stream<int[]> (one element!)
Arrays.stream(arr);       // → IntStream (three elements)

String[] strArr = {"a", "b", "c"};
Stream.of(strArr);        // → Stream<String> (works correctly for Object arrays)
Arrays.stream(strArr);    // → Stream<String> (same result)
```

**Gotcha:** `Stream.of(primitiveArray)` wraps the entire array as one element. Always use `Arrays.stream()` for primitive arrays, or use `IntStream.of()`.

---

### Q19. At FedEx, a junior developer wrote this to count packages per status. What's wrong?

```java
Map<String, Integer> counts = new HashMap<>();
events.parallelStream().forEach(e -> {
    counts.merge(e.getStatus(), 1, Integer::sum);
});
```

**Problem:** `HashMap` is not thread-safe. `parallelStream()` + `forEach` + mutable shared state = **race condition**. Results will be wrong and inconsistent.

**Fix 1:** Use a proper collector:
```java
Map<String, Long> counts = events.parallelStream()
    .collect(Collectors.groupingBy(ScanEvent::getStatus, Collectors.counting()));
```

**Fix 2:** If you must use a map (don't), use `ConcurrentHashMap`. But collectors are always preferred.

---

### Q20. What is short-circuit evaluation in Streams? Name all short-circuit operations.

Short-circuit operations **don't need to process the entire stream** to produce a result.

**Short-circuit terminal operations:**
- `findFirst()`, `findAny()` — return as soon as one match is found
- `anyMatch()`, `allMatch()`, `noneMatch()` — return as soon as result is determined

**Short-circuit intermediate operation:**
- `limit(n)` — stops after n elements

```java
// Processes only until the first match — NOT the entire list
boolean hasExpired = packages.stream()
    .anyMatch(p -> p.getDeliveryDate().isBefore(LocalDate.now()));
```

**At NPCI:** When checking if ANY transaction in a batch is flagged for fraud, `anyMatch()` returns immediately on first hit — critical for latency in real-time payment processing.

---

### Q14. Stream.toList() (Java 16) vs Collectors.toList() vs Collectors.toUnmodifiableList()

**Answer:**

```java
List<String> names = employees.stream().map(Employee::getName).collect(Collectors.toList());
// ✅ Returns mutable ArrayList — can add/remove after
// ✅ Allows null elements

List<String> names2 = employees.stream().map(Employee::getName).toList();
// ✅ Shorthand (Java 16+) — cleaner syntax
// ❌ Returns UNMODIFIABLE list — throws UnsupportedOperationException on add/remove
// ✅ Allows null elements

List<String> names3 = employees.stream().map(Employee::getName).collect(Collectors.toUnmodifiableList());
// ❌ Returns UNMODIFIABLE list
// ❌ Throws NullPointerException if ANY element is null
// Available since Java 10
```

| Method | Since | Mutable? | Nulls OK? | Type |
|--------|-------|----------|-----------|------|
| `Collectors.toList()` | Java 8 | ✅ Yes | ✅ Yes | `ArrayList` |
| `Stream.toList()` | Java 16 | ❌ No | ✅ Yes | Unmodifiable |
| `Collectors.toUnmodifiableList()` | Java 10 | ❌ No | ❌ No (NPE) | Unmodifiable |

**Rule of thumb:**
- Need to modify later? → `Collectors.toList()`
- Just reading the result? → `.toList()` (Java 16+, cleanest)
- Strict immutability + no nulls guarantee? → `Collectors.toUnmodifiableList()`

---

### Q15. Explain the Collector interface. How do you write a custom Collector?

**Answer:**

A `Collector` has 4 components:
```
supplier()      → creates the mutable container (e.g., new ArrayList)
accumulator()   → adds an element to the container
combiner()      → merges two containers (for parallel streams)
finisher()      → final transformation (optional)
characteristics → UNORDERED, CONCURRENT, IDENTITY_FINISH
```

```java
// Built-in example (what Collectors.toList() does internally):
Collector.of(
    ArrayList::new,           // supplier
    ArrayList::add,           // accumulator
    (left, right) -> { left.addAll(right); return left; },  // combiner
    Collector.Characteristics.IDENTITY_FINISH
);

// Custom Collector — join strings with delimiter (like Collectors.joining):
Collector<String, StringJoiner, String> myJoining =
    Collector.of(
        () -> new StringJoiner(", "),    // supplier
        StringJoiner::add,               // accumulator
        StringJoiner::merge,             // combiner
        StringJoiner::toString           // finisher
    );

String result = Stream.of("Kafka", "Redis", "Postgres")
    .collect(myJoining);  // "Kafka, Redis, Postgres"
```

---

### Q16. Stream API additions across Java 9-21

**Answer:**

```java
// Java 9: takeWhile / dropWhile (ordered streams)
Stream.of(1, 2, 3, 4, 5, 1).takeWhile(n -> n < 4);  // [1, 2, 3] — stops at first false
Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3);      // [3, 4, 5] — drops until first true

// Java 9: ofNullable (null-safe single-element stream)
Stream.ofNullable(null);   // empty stream
Stream.ofNullable("hi");   // Stream.of("hi")

// Java 9: iterate with predicate (replaces infinite + limit)
Stream.iterate(1, n -> n < 100, n -> n * 2);  // [1, 2, 4, 8, 16, 32, 64]

// Java 12: Collectors.teeing (two collectors combined)
var result = employees.stream().collect(
    Collectors.teeing(
        Collectors.counting(),                    // collector 1
        Collectors.averagingDouble(Employee::getSalary),  // collector 2
        (count, avgSalary) -> "Count: " + count + ", Avg: " + avgSalary  // merger
    )
);

// Java 16: mapMulti (alternative to flatMap, more efficient for small expansions)
Stream.of(1, 2, 3).<String>mapMulti((num, consumer) -> {
    consumer.accept("a" + num);
    consumer.accept("b" + num);
}).toList();  // ["a1", "b1", "a2", "b2", "a3", "b3"]

// Java 16: toList() (covered in Q14)
List<String> names = stream.map(String::toUpperCase).toList();
```
