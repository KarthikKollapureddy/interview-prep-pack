# Collections Framework Internals — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas  
> Difficulty: Hatio = baseline, FedEx/NPCI = harder

---

## Conceptual Questions

### Q1. Explain the internal working of HashMap. What happens when you call `put(key, value)`?

**Step by step:**
1. Compute `hashCode()` of the key
2. Apply internal hash function: `hash = key.hashCode() ^ (key.hashCode() >>> 16)` — distributes high bits to reduce collisions
3. Calculate bucket index: `index = hash & (capacity - 1)` (bitwise AND, equivalent to modulo for power-of-2 sizes)
4. If bucket is empty → create a `Node` and place it
5. If bucket has entries (collision) → traverse the linked list/tree:
   - If key already exists (`equals()` returns true) → replace value
   - Otherwise → append new node
6. If list length > **TREEIFY_THRESHOLD (8)** and capacity ≥ 64 → convert linked list to **Red-Black tree** (O(log n) lookup instead of O(n))
7. If size > `capacity * loadFactor` (default 0.75) → **resize** (double capacity, rehash all entries)

**Complexity:**
- Best case: O(1) per operation
- Worst case (all collisions, pre-Java 8): O(n) — linked list traversal
- Worst case (Java 8+): O(log n) — tree traversal after treeification

---

### Q2. What is the difference between HashMap, LinkedHashMap, TreeMap, and ConcurrentHashMap?

| Feature | HashMap | LinkedHashMap | TreeMap | ConcurrentHashMap |
|---------|---------|--------------|---------|------------------|
| Order | No order | Insertion order (or access order) | Sorted by key (natural/comparator) | No order |
| Null key | 1 allowed | 1 allowed | ❌ (throws NPE if natural ordering) | ❌ |
| Thread-safe | ❌ | ❌ | ❌ | ✅ (segment locking) |
| Complexity | O(1) avg | O(1) avg | O(log n) | O(1) avg |
| Underlying | Array + LinkedList/RBTree | HashMap + doubly-linked list | Red-Black Tree | Array + CAS + synchronized blocks |

**When to use each:**
- **HashMap:** Default choice. Fast, unordered.
- **LinkedHashMap:** Need insertion-order iteration (e.g., LRU cache with `accessOrder=true` + `removeEldestEntry()`).
- **TreeMap:** Need sorted keys (e.g., range queries, `subMap()`, `headMap()`).
- **ConcurrentHashMap:** Multi-threaded access without external synchronization.

---

### Q3. How does ConcurrentHashMap achieve thread safety without locking the entire map?

**Java 8+ implementation:**
- Array of `Node` buckets (like HashMap)
- **CAS (Compare-And-Swap)** for insertions into empty buckets — lock-free
- **Synchronized on the head node** of each bucket for insertions into occupied buckets — fine-grained locking
- **No segment-level locking** (that was Java 7). Java 8+ locks only the specific bucket being modified
- **Concurrent reads are lock-free** — volatile reads on Node values
- `size()` is approximate during concurrent modification (uses `CounterCell` array for distributed counting)

```java
// Safe concurrent access
ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
counters.computeIfAbsent("pageViews", k -> new AtomicLong()).incrementAndGet();
// compute/merge/computeIfAbsent are atomic operations on CHM
```

**At NPCI:** ConcurrentHashMap is used for in-memory rate limiting — tracking transaction counts per VPA in real-time without lock contention.

---

### Q4. ArrayList vs LinkedList — when would you choose each? Internal structure?

| Feature | ArrayList | LinkedList |
|---------|-----------|------------|
| Internal | Dynamic array (Object[]) | Doubly-linked list (Node with prev/next) |
| Random access `get(i)` | O(1) | O(n) — traversal |
| Add at end | O(1) amortized (resize when full) | O(1) |
| Add at index | O(n) — shift elements | O(n) search + O(1) insert |
| Memory | Compact (less overhead per element) | Extra overhead (2 pointers per node) |
| Cache | Cache-friendly (contiguous memory) | Cache-unfriendly (scattered nodes) |

**In practice: Almost always use ArrayList.** LinkedList wins only when you frequently insert/remove at the beginning AND never do random access. Even then, ArrayDeque is usually better.

**At FedEx:** Scan event lists are always ArrayList — sequential processing with random access for lookups by index. LinkedList would kill performance on lists of 500K+ events.

---

### Q5. What is the difference between `fail-fast` and `fail-safe` iterators?

**Fail-fast (ArrayList, HashMap):**
```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
for (String s : list) {
    list.remove(s); // ❌ ConcurrentModificationException
}
```
Uses `modCount` — if structure changes during iteration, iterator detects it immediately.

**Fail-safe (ConcurrentHashMap, CopyOnWriteArrayList):**
```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c"));
for (String s : list) {
    list.remove(s); // ✅ No exception — iterates over snapshot
}
```
Iterates on a **copy/snapshot** of the collection. Modification during iteration is safe but you don't see the changes in the current iteration.

**Safe removal during iteration:**
```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove(); // ✅ Safe
}
// Or in Java 8+: list.removeIf(s -> s.equals("b"));
```

---

## Scenario-Based Questions

### Q6. At Hatio/BillDesk, you need an in-memory LRU cache for frequently accessed merchant configurations. The cache should hold at most 1000 entries and evict the least recently used. Implement using Collections.

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;
    
    public LRUCache(int maxSize) {
        super(maxSize, 0.75f, true); // accessOrder = true
        this.maxSize = maxSize;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}

LRUCache<String, MerchantConfig> cache = new LRUCache<>(1000);
cache.put("M001", config);
cache.get("M001"); // Moves M001 to end (most recently used)
// When 1001st entry is added, the least recently accessed is evicted
```

**Why LinkedHashMap over building from scratch?** It already maintains access-order doubly-linked list. `removeEldestEntry()` is called after every `put()`. O(1) for all operations.

**Thread-safe version:** Wrap with `Collections.synchronizedMap(new LRUCache<>(1000))` or use Caffeine/Guava cache library.

---

### Q7. At FedEx, the On Road Event Gateway processes events that need to be dispatched to 3 downstream systems in priority order. Events with priority 1 should always be processed before priority 5. Which collection would you use and why?

```java
PriorityQueue<GatewayEvent> eventQueue = new PriorityQueue<>(
    Comparator.comparingInt(GatewayEvent::getPriority)
        .thenComparing(GatewayEvent::getTimestamp) // FIFO within same priority
);

eventQueue.offer(new GatewayEvent("PICKUP", 1, Instant.now()));
eventQueue.offer(new GatewayEvent("STATUS", 5, Instant.now()));
eventQueue.offer(new GatewayEvent("DELIVERY", 1, Instant.now()));

eventQueue.poll(); // Returns PICKUP (priority 1, earlier timestamp)
```

**PriorityQueue internals:** Binary min-heap. `offer()` = O(log n), `poll()` = O(log n), `peek()` = O(1).

**Not sorted:** PriorityQueue does NOT maintain sorted order internally. Only guarantees the head is the min/max element. Iterating gives no guaranteed order.

**Thread-safe alternative:** `PriorityBlockingQueue` for producer-consumer patterns.

---

### Q8. At NPCI, you need to track unique transaction IDs across a sliding 10-minute window. You expect ~1M transactions. Which Set implementation and why?

For **uniqueness checking** on 1M elements:
- `HashSet` — O(1) contains/add. Best for pure uniqueness check. Memory: ~48 bytes per entry.
- With **time-based expiry**, combine `ConcurrentHashMap<String, Instant>` with a scheduled cleanup:

```java
ConcurrentHashMap<String, Instant> seenTxns = new ConcurrentHashMap<>();

boolean isDuplicate(String txnId) {
    Instant now = Instant.now();
    Instant prev = seenTxns.putIfAbsent(txnId, now);
    return prev != null; // already seen
}

// Scheduled cleanup every minute
void cleanup() {
    Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
    seenTxns.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
}
```

**Why not TreeSet?** O(log n) operations. For 1M entries, that's ~20 comparisons per lookup vs O(1) for HashSet. At NPCI's throughput (thousands of TPS), this difference matters.

---

### Q9. Explain `Collections.unmodifiableList()` vs `List.of()` vs `List.copyOf()`.

```java
List<String> original = new ArrayList<>(Arrays.asList("a", "b"));

// unmodifiableList — VIEW, not a copy
List<String> unmod = Collections.unmodifiableList(original);
original.add("c");
unmod.size(); // 3! Changes to original reflect in the view

// List.of — truly immutable, does not accept null
List<String> immutable = List.of("a", "b");
immutable.add("c"); // ❌ UnsupportedOperationException

// List.copyOf — immutable copy of existing collection
List<String> copy = List.copyOf(original);
original.add("d");
copy.size(); // 3 — independent copy
```

| | `unmodifiableList` | `List.of` | `List.copyOf` |
|-|-------------------|-----------|--------------|
| Copy? | No (view) | N/A (creates new) | Yes |
| Nulls | Allowed | ❌ | ❌ |
| Reflects source changes | Yes | N/A | No |
| Truly immutable | No (source can change) | Yes | Yes |

---

## Coding Challenges

### Challenge 1: Custom HashMap
**File:** `solutions/CustomHashMap.java`  
Implement a simplified HashMap from scratch:
1. `put(K key, V value)` — insert or update
2. `get(K key)` — retrieve value or null
3. `remove(K key)` — remove entry
4. Handle collisions with chaining (linked list)
5. Implement automatic resize when load factor > 0.75
6. Write tests in `main()` with collision scenarios

### Challenge 2: LRU Cache
**File:** `solutions/LRUCache.java`  
Implement an LRU cache **without extending LinkedHashMap**:
1. Use a `HashMap` + doubly-linked list (build the linked list manually)
2. `get(key)` — return value, move to front (most recent)
3. `put(key, value)` — insert, evict LRU if at capacity
4. All operations must be O(1)
5. Test with capacity=3, insert 5 items, verify eviction order

### Challenge 3: Thread-Safe Counter Map
**File:** `solutions/ThreadSafeCounter.java`  
At NPCI, implement a thread-safe word counter:
1. Use `ConcurrentHashMap` with `compute()` or `merge()`
2. Method `increment(String key)` — thread-safe increment
3. Method `topK(int k)` — return top K keys by count
4. Test with 10 threads incrementing concurrently, verify no lost updates

---

## Gotchas & Edge Cases

### Q10. What happens if you modify a key's hashCode after putting it in a HashMap?

```java
class MutableKey {
    int id;
    public int hashCode() { return id; }
    public boolean equals(Object o) { return o instanceof MutableKey mk && mk.id == id; }
}

Map<MutableKey, String> map = new HashMap<>();
MutableKey key = new MutableKey();
key.id = 1;
map.put(key, "one");

key.id = 2; // ❌ hashCode changed!
map.get(key); // null — looks in wrong bucket
map.get(new MutableKey() {{ id = 1; }}); // null — right bucket, but key in bucket now has id=2
// The entry is ORPHANED — can't be found, can't be removed, but still occupies memory
```

**Rule:** HashMap keys must be effectively immutable. Use `String`, `Integer`, records, or immutable objects as keys.

---

### Q11. What is the time complexity of `HashMap.containsValue()`?

**O(n)** — it must scan all entries across all buckets. There's no reverse index from value to key.

`containsKey()` is O(1). If you frequently look up by value, maintain a reverse map: `Map<V, K>` or use a BiMap (Guava).

---

### Q12. Why is the initial capacity of HashMap a power of 2?

Because the bucket index is calculated as `hash & (capacity - 1)` (bitwise AND). This only works correctly as a modulo replacement when capacity is a power of 2.

If you pass a non-power-of-2 capacity (e.g., `new HashMap<>(10)`), Java rounds up to the next power of 2 (16).

**Follow-up:** The load factor 0.75 is a tradeoff between space and time. Lower = fewer collisions but more memory. Higher = more collisions but less memory. 0.75 is empirically good for most workloads.

---

### Q13. What are SequencedCollections (Java 21)?

**Answer:**
Java 21 introduces three new interfaces that define **encounter order** operations (`getFirst`, `getLast`, `reversed`):

```
                  SequencedCollection
                  /                \
        SequencedSet          (List already has first/last)
              |
        SequencedMap (separate hierarchy)
```

```java
// Before Java 21 — getting first/last was inconsistent:
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
list.get(0);                    // first — OK
list.get(list.size() - 1);     // last — verbose!

LinkedHashSet<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
set.iterator().next();          // first — clunky
// last? No way without iterating all elements!

// Java 21 — uniform interface:
SequencedCollection<String> seq = new ArrayList<>(List.of("a", "b", "c"));
seq.getFirst();     // "a"
seq.getLast();      // "c"
seq.addFirst("z");  // adds at beginning → ["z", "a", "b", "c"]
seq.addLast("x");   // adds at end
seq.reversed();     // reversed VIEW (not a copy!) → ["c", "b", "a", "z"]

// SequencedMap:
SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.put("a", 1); map.put("b", 2); map.put("c", 3);
map.firstEntry();   // a=1
map.lastEntry();    // c=3
map.pollFirstEntry(); // removes and returns a=1
map.reversed();     // reversed view
```

**Which collections implement what:**
| Interface | Implementations |
|-----------|----------------|
| `SequencedCollection` | `ArrayList`, `LinkedList`, `ArrayDeque`, `LinkedHashSet`, `TreeSet` |
| `SequencedSet` | `LinkedHashSet`, `TreeSet` |
| `SequencedMap` | `LinkedHashMap`, `TreeMap` |

**Interview insight:** `HashMap` and `HashSet` do NOT implement Sequenced interfaces because they have no defined encounter order.
