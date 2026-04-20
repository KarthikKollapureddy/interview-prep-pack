# Java 8 → 21 Features — Complete Reference

> Every major feature from Java 8 to 21 in tabular format  
> 2-line description + code example for each  
> Use this as a quick-revision cheat sheet before interviews

---

## Java 8 (March 2014) — **The Big Bang Release**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Lambda Expressions** | Anonymous function syntax replacing verbose anonymous classes. Enables functional programming in Java. | `list.forEach(x -> System.out.println(x));` |
| 2 | **Functional Interfaces** | Single abstract method interfaces (`@FunctionalInterface`). Core types: `Predicate<T>`, `Function<T,R>`, `Consumer<T>`, `Supplier<T>`. | `Predicate<String> isEmpty = s -> s.isEmpty();` |
| 3 | **Method References** | Shorthand for lambdas that call existing methods. Four kinds: `Class::staticMethod`, `obj::instanceMethod`, `Class::instanceMethod`, `Class::new`. | `list.stream().map(String::toUpperCase).collect(Collectors.toList());` |
| 4 | **Stream API** | Declarative pipeline for processing collections with map/filter/reduce. Supports lazy evaluation and parallelism. | `list.stream().filter(x -> x > 5).mapToInt(Integer::intValue).sum();` |
| 5 | **Optional\<T\>** | Container to avoid NullPointerException. Forces explicit handling of absent values. | `Optional.ofNullable(name).orElse("Unknown");` |
| 6 | **Default Methods** | Interface methods with a body using `default` keyword. Enables interface evolution without breaking implementations. | `interface List { default void sort(Comparator c) { ... } }` |
| 7 | **Static Methods in Interfaces** | Interfaces can have static utility methods. Replaces the need for companion utility classes. | `interface Comparator { static <T> Comparator<T> naturalOrder() { ... } }` |
| 8 | **Date/Time API (`java.time`)** | Immutable, thread-safe date/time classes replacing `Date`/`Calendar`. Core: `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Instant`, `Duration`, `Period`. | `LocalDate.now().plusDays(7);  Duration.between(start, end).toHours();` |
| 9 | **CompletableFuture** | Async programming with composable stages. Supports `thenApply`, `thenCompose`, `allOf`, `anyOf`. | `CompletableFuture.supplyAsync(() -> fetchData()).thenApply(d -> process(d));` |
| 10 | **Nashorn JS Engine** | JavaScript engine for embedding JS in Java (deprecated in 11, removed in 15). Replaced Rhino engine. | `ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");` |
| 11 | **Collectors Utility** | Rich set of stream terminal operations: `toList`, `toMap`, `groupingBy`, `partitioningBy`, `joining`. | `map.stream().collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));` |
| 12 | **StringJoiner** | Utility to join strings with delimiter, prefix, suffix. Used internally by `String.join()`. | `new StringJoiner(", ", "[", "]").add("a").add("b").toString(); // [a, b]` |

---

## Java 9 (September 2017) — **Modularity**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Module System (Jigsaw)** | Encapsulate packages into modules with explicit dependencies via `module-info.java`. Improves security and startup time. | `module com.myapp { requires java.sql; exports com.myapp.api; }` |
| 2 | **JShell (REPL)** | Interactive Read-Eval-Print Loop for Java. Great for quick prototyping and learning. | `$ jshell` → `jshell> "hello".toUpperCase()` → `$1 ==> "HELLO"` |
| 3 | **Collection Factory Methods** | Immutable collection creation with `List.of()`, `Set.of()`, `Map.of()`. Cleaner than `Collections.unmodifiable*`. | `List<String> names = List.of("Alice", "Bob");  // throws UnsupportedOperationException on add` |
| 4 | **Optional Enhancements** | `ifPresentOrElse()`, `or()` for chaining Optionals, `stream()` to convert to Stream. | `opt.ifPresentOrElse(System.out::println, () -> log("empty"));` |
| 5 | **Stream Enhancements** | `takeWhile()`, `dropWhile()` for ordered streams. `ofNullable()` for null-safe single-element streams. | `Stream.of(1,2,3,4,5).takeWhile(n -> n < 4); // 1, 2, 3` |
| 6 | **Private Interface Methods** | Interfaces can have `private` and `private static` helper methods. Reduces code duplication in default methods. | `interface Logger { private void log(String msg) { System.out.println(msg); } }` |
| 7 | **Try-With-Resources Enhancement** | Effectively final variables can be used directly in try-with-resources without redeclaring. | `BufferedReader br = new BufferedReader(...); try (br) { ... }` |
| 8 | **Process API** | `ProcessHandle` for inspecting and managing OS processes. Get PID, parent, children, command. | `ProcessHandle.current().pid();  ProcessHandle.allProcesses().count();` |
| 9 | **HTTP Client (Incubator)** | Modern HTTP client replacing `HttpURLConnection`. Supports HTTP/2 and WebSocket. (Standardized in Java 11) | `HttpClient.newHttpClient().send(request, BodyHandlers.ofString());` |

---

## Java 10 (March 2018) — **Local Variable Type Inference**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **`var` (Local Variable)** | Compiler infers type from right-hand side. Only for local variables, not fields, parameters, or return types. | `var list = new ArrayList<String>();  var stream = list.stream();` |
| 2 | **Unmodifiable Collection Copies** | `List.copyOf()`, `Set.copyOf()`, `Map.copyOf()` create immutable copies of existing collections. | `var immutable = List.copyOf(mutableList);  // changes to mutableList don't affect immutable` |
| 3 | **Collectors.toUnmodifiableList()** | Stream collector that returns an immutable list. Throws NPE on null elements. | `list.stream().filter(x -> x > 0).collect(Collectors.toUnmodifiableList());` |
| 4 | **Optional.orElseThrow()** | No-arg `orElseThrow()` added — clearer alternative to `get()`. Throws `NoSuchElementException`. | `String name = optionalName.orElseThrow(); // instead of .get()` |

---

## Java 11 (September 2018) — **First LTS after 8**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **HTTP Client (Standard)** | `java.net.http.HttpClient` standardized from incubator. Supports sync/async, HTTP/2, WebSocket. | `HttpClient.newHttpClient().sendAsync(request, BodyHandlers.ofString()).thenAccept(r -> ...);` |
| 2 | **String New Methods** | `isBlank()`, `lines()`, `strip()` (Unicode-aware trim), `repeat()`. | `"  ".isBlank(); // true    "hello\nworld".lines().count(); // 2    "ha".repeat(3); // "hahaha"` |
| 3 | **`var` in Lambdas** | `var` allowed in lambda parameters for annotation support. | `list.stream().map((@NotNull var s) -> s.toUpperCase());` |
| 4 | **Files Utility Methods** | `Files.readString()` and `Files.writeString()` for simple file I/O. | `String content = Files.readString(Path.of("file.txt"));` |
| 5 | **`Optional.isEmpty()`** | Opposite of `isPresent()`. Cleaner null checking. | `if (optional.isEmpty()) { handleMissing(); }` |
| 6 | **Removed: Java EE & CORBA** | `javax.xml`, `javax.activation`, CORBA removed. Use Jakarta EE / Maven dependencies instead. | Add `jakarta.xml.bind:jakarta.xml.bind-api` to pom.xml |

---

## Java 12-13 (2019) — **Switch Expressions Preview**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Switch Expressions (Preview)** | Switch as an expression returning a value. Arrow syntax (`->`) eliminates fall-through. | `String result = switch(day) { case MON, TUE -> "Work"; case SAT, SUN -> "Rest"; default -> "?"; };` |
| 2 | **Text Blocks (Preview in 13)** | Multi-line string literals using `"""`. Preserves formatting, strips common indentation. | `String json = """ {"name": "Karthik", "role": "dev"} """;` |
| 3 | **Compact Number Formatting** | Locale-aware compact number format (1K, 1M, 1B). | `NumberFormat.getCompactNumberInstance(Locale.US, Style.SHORT).format(1000); // "1K"` |

---

## Java 14 (March 2020) — **Records & Pattern Matching Preview**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Records (Preview)** | Immutable data carriers with auto-generated `equals()`, `hashCode()`, `toString()`, accessors. Replaces boilerplate POJOs. | `record Point(int x, int y) {}   var p = new Point(1, 2); p.x(); // 1` |
| 2 | **Pattern Matching for `instanceof` (Preview)** | Eliminates explicit casting after instanceof check. Binds variable in one step. | `if (obj instanceof String s) { System.out.println(s.length()); }` |
| 3 | **Switch Expressions (Standard)** | Finalized from preview. Supports `->` arrow labels and `yield` for multi-line cases. | `int result = switch(s) { case "a" -> 1; case "b" -> { yield 2; } default -> 0; };` |
| 4 | **Helpful NullPointerExceptions** | NPE messages now tell exactly which variable was null. Huge debugging improvement. | `a.b.c.d` → `Cannot invoke "C.getD()" because "a.getB().getC()" is null` |
| 5 | **Text Blocks (Second Preview)** | Added `\` line continuation and `\s` space escape. | `String s = """ Hello \s World""";` |

---

## Java 15 (September 2020)

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Text Blocks (Standard)** | Finalized from preview. Widely used for JSON, SQL, HTML templates in code. | `String sql = """ SELECT * FROM users WHERE status = 'ACTIVE' """;` |
| 2 | **Sealed Classes (Preview)** | Restrict which classes can extend/implement a type. Enables exhaustive pattern matching. | `sealed interface Shape permits Circle, Rectangle {}` |
| 3 | **Hidden Classes** | Classes that can't be discovered by name. Used by frameworks for dynamic proxy generation. | Framework-level feature for bytecode generation. |

---

## Java 16 (March 2021) — **Records & Pattern Matching Standard**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Records (Standard)** | Finalized. Can have compact constructors, custom methods, implement interfaces. Cannot extend classes. | `record Employee(String name, int age) { Employee { if (age < 0) throw new IllegalArgumentException(); } }` |
| 2 | **Pattern Matching for `instanceof` (Standard)** | Finalized. Works with `&&` in conditions, scoping rules defined. | `if (obj instanceof String s && s.length() > 5) { process(s); }` |
| 3 | **Stream.toList()** | Returns an unmodifiable list directly from stream. Shorter than `collect(Collectors.toList())`. | `var names = employees.stream().map(Employee::name).toList();` |
| 4 | **Day Period Support** | `DateTimeFormatter` supports "in the morning", "in the afternoon" with `B` pattern. | `DateTimeFormatter.ofPattern("h B").format(LocalTime.of(14, 0)); // "2 in the afternoon"` |

---

## Java 17 (September 2021) — **LTS Release**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Sealed Classes (Standard)** | Finalized. `sealed`, `non-sealed`, `final` modifiers. Compiler enforces exhaustive switch. | `sealed interface Shape permits Circle, Rect {} final class Circle implements Shape {}` |
| 2 | **Pattern Matching for Switch (Preview)** | Switch can match types, not just constants. Combined with sealed classes for exhaustive matching. | `switch(shape) { case Circle c -> area(c); case Rect r -> area(r); }` |
| 3 | **Enhanced Pseudo-Random Generators** | New `RandomGenerator` interface hierarchy. `SplittableRandom`, `L64X128MixRandom`, etc. | `var rng = RandomGeneratorFactory.of("L64X128MixRandom").create();` |
| 4 | **Removed: Applet API** | `java.applet` package deprecated for removal. End of an era. | N/A |
| 5 | **`instanceof` Pattern in Switch** | Guarded patterns with `when` keyword (later renamed from `&&`). | `case String s when s.length() > 5 -> process(s);` |

---

## Java 18-19 (2022) — **UTF-8 Default, Foreign Function Preview**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **UTF-8 by Default** | `Charset.defaultCharset()` returns UTF-8 on all platforms. No more platform-specific encoding issues. | All `new String(bytes)`, `Files.readString()` default to UTF-8 |
| 2 | **Simple Web Server** | Built-in minimal HTTP file server for testing. One command to serve files. | `$ jwebserver -p 8080 -d /path/to/files` |
| 3 | **Record Patterns (Preview)** | Deconstruct records in pattern matching. Extract components directly. | `if (obj instanceof Point(int x, int y)) { System.out.println(x + y); }` |
| 4 | **Virtual Threads (Preview in 19)** | Lightweight threads managed by JVM, not OS. Millions of concurrent threads. Project Loom. | `Thread.ofVirtual().start(() -> fetchData());` |
| 5 | **Structured Concurrency (Incubator)** | Treat related tasks as a unit. If one fails, cancel siblings. Cleaner than raw CompletableFuture. | `try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { ... }` |
| 6 | **Foreign Function & Memory API (Preview)** | Call native code (C/C++) without JNI. Safer, simpler, faster. | `var linker = Linker.nativeLinker(); var strlen = linker.downcallHandle(...);` |

---

## Java 20 (March 2023) — **Previews Evolving**

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Scoped Values (Incubator)** | Immutable, inheritable per-thread values. Safer alternative to ThreadLocal for virtual threads. | `ScopedValue.where(USER, currentUser).run(() -> handleRequest());` |
| 2 | **Record Patterns (2nd Preview)** | Enhanced nested record deconstruction in switch and instanceof. | `case Rect(Point(var x1, var y1), Point(var x2, var y2)) -> ...` |
| 3 | **Pattern Matching for Switch (4th Preview)** | Refined guard syntax: `when` keyword instead of `&&`. | `case String s when s.startsWith("A") -> process(s);` |

---

## Java 21 (September 2023) — **LTS Release** ⭐

| # | Feature | Description | Example |
|---|---------|-------------|---------|
| 1 | **Virtual Threads (Standard)** | Lightweight threads (Project Loom) finalized. 1 million+ concurrent threads. No OS thread per virtual thread. | `try (var executor = Executors.newVirtualThreadPerTaskExecutor()) { executor.submit(() -> callApi()); }` |
| 2 | **Pattern Matching for Switch (Standard)** | Finalized. Type patterns, guarded patterns (`when`), null handling in switch. Exhaustive with sealed types. | `String desc = switch(obj) { case Integer i -> "int: " + i; case String s when s.length() > 5 -> "long string"; case null -> "null!"; default -> "other"; };` |
| 3 | **Record Patterns (Standard)** | Finalized. Deconstruct records in switch and instanceof with nested patterns. | `if (obj instanceof Point(int x, int y)) { return Math.sqrt(x*x + y*y); }` |
| 4 | **Sequenced Collections** | New interfaces: `SequencedCollection`, `SequencedSet`, `SequencedMap`. Define encounter order with `addFirst/Last`, `getFirst/Last`, `reversed()`. | `list.addFirst("A"); list.getLast(); list.reversed().forEach(System.out::println);` |
| 5 | **String Templates (Preview)** | Embedded expressions in strings with template processors. Type-safe alternative to string concatenation. | `String msg = STR."Hello \{name}, you are \{age} years old";` |
| 6 | **Structured Concurrency (Preview)** | Manage concurrent subtasks as a unit. ShutdownOnFailure/ShutdownOnSuccess scopes. | `try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { var user = scope.fork(() -> getUser()); var order = scope.fork(() -> getOrder()); scope.join().throwIfFailed(); return new Response(user.get(), order.get()); }` |
| 7 | **Scoped Values (Preview)** | Share immutable data across threads without ThreadLocal overhead. Perfect for virtual threads. | `static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance(); ScopedValue.where(CURRENT_USER, user).run(() -> handleRequest());` |
| 8 | **Unnamed Patterns & Variables** | Use `_` for unused variables/patterns. Reduces noise in code. | `try { ... } catch (Exception _) { log("failed"); }` `case Point(int x, _) -> "x=" + x;` |
| 9 | **Unnamed Classes (Preview)** | Run Java programs without class declaration. Simplifies learning and scripts. | `void main() { System.out.println("Hello!"); }` |

---

## Quick Comparison — Which LTS to Target?

| Feature | Java 8 | Java 11 | Java 17 | Java 21 |
|---------|--------|---------|---------|---------|
| Lambda/Streams | ✅ | ✅ | ✅ | ✅ |
| `var` | ❌ | ✅ | ✅ | ✅ |
| HTTP Client | ❌ | ✅ | ✅ | ✅ |
| Records | ❌ | ❌ | ✅ | ✅ |
| Sealed Classes | ❌ | ❌ | ✅ | ✅ |
| Text Blocks | ❌ | ❌ | ✅ | ✅ |
| Pattern Matching Switch | ❌ | ❌ | Preview | ✅ |
| Virtual Threads | ❌ | ❌ | ❌ | ✅ |
| Sequenced Collections | ❌ | ❌ | ❌ | ✅ |

---

## Top 10 Interview Questions on Java Versions

### 1. What's the migration path from Java 8 to 21?
```
Java 8 → 11: Module system (add-opens), remove javax.xml, HttpClient
Java 11 → 17: Sealed classes, records, pattern matching instanceof
Java 17 → 21: Virtual threads, switch patterns, sequenced collections
Key: Test with --illegal-access=deny, fix module access, update libraries
```

### 2. When would you use Records vs Lombok @Data?
```
Records: Immutable DTOs, value objects, API responses. Cannot extend, all fields final.
Lombok: Mutable POJOs, entities (JPA needs no-arg constructor), when you need setters/builders.
Rule: New code → Records. Legacy/JPA entities → Lombok.
```

### 3. Virtual Threads vs Platform Threads — when to use which?
```
Virtual Threads: I/O-bound tasks (HTTP calls, DB queries, file I/O). 1M+ threads possible.
Platform Threads: CPU-bound tasks (computation, number crunching). Limited to ~10K threads.
Rule: If the thread mostly WAITS → virtual. If it mostly COMPUTES → platform.
Don't use synchronized or ThreadLocal with virtual threads (pin the carrier thread).
```

### 4. Explain the new Switch Pattern Matching.
```java
// Old style (Java 8):
if (obj instanceof String) {
    String s = (String) obj;
    process(s);
}

// Java 21 style:
switch (obj) {
    case String s when s.length() > 5 -> processLong(s);
    case String s                      -> processShort(s);
    case Integer i                     -> processNum(i);
    case null                          -> handleNull();
    default                            -> handleOther();
}
// ✅ Type checking + casting + guards in one expression
// ✅ null handling (no more NPE on switch(nullObj))
// ✅ Exhaustive check with sealed types
```

### 5. What are Sequenced Collections?
```java
// Before Java 21: No common interface for "first/last" operations
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
list.get(0);                    // first — works
list.get(list.size() - 1);     // last — verbose!
// LinkedHashSet had no easy way to get first/last

// Java 21: SequencedCollection interface
SequencedCollection<String> seq = new ArrayList<>(List.of("a", "b", "c"));
seq.getFirst();     // "a"
seq.getLast();      // "c"
seq.addFirst("z");  // adds at beginning
seq.reversed();     // reversed view (not a copy)
// Also: SequencedSet (LinkedHashSet), SequencedMap (LinkedHashMap)
```

### 6. What are Sealed Classes? Why do they matter?
```java
// Only Circle, Rectangle, Triangle can implement Shape
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
final class Triangle implements Shape { /* ... */ }

// Compiler ensures exhaustive switch (no default needed!)
double area = switch (shape) {
    case Circle c    -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.w() * r.h();
    case Triangle t  -> calculateTriangle(t);
};
// If you add a new Shape subtype, compiler forces you to handle it
```

### 7. Text Blocks — practical uses?
```java
String sql = """
        SELECT e.name, d.name
        FROM employees e
        JOIN departments d ON e.dept_id = d.id
        WHERE e.status = 'ACTIVE'
        """;

String json = """
        {
            "name": "%s",
            "age": %d
        }
        """.formatted(name, age);
// ✅ Clean multi-line strings for SQL, JSON, HTML, GraphQL queries
```

### 8. Structured Concurrency — why is it better than CompletableFuture?
```java
// Problem with CompletableFuture: if getOrder() fails, getUser() keeps running
var userFuture = CompletableFuture.supplyAsync(() -> getUser(id));
var orderFuture = CompletableFuture.supplyAsync(() -> getOrder(id));

// Structured Concurrency (Java 21): treats both as a UNIT
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var user  = scope.fork(() -> getUser(id));
    var order = scope.fork(() -> getOrder(id));
    scope.join().throwIfFailed();  // if either fails, cancel the other
    return new Response(user.get(), order.get());
}
// ✅ Automatic cancellation, better error handling, clear ownership
```

### 9. What replaced `finalize()` in modern Java?
```
finalize() deprecated since Java 9, removed in 18.
Replacement: try-with-resources (AutoCloseable) + Cleaner API.
Rule: Never rely on finalize() for resource cleanup.
```

### 10. Key deprecations/removals across versions?
```
Java 9:  Applet API deprecated, Observer/Observable deprecated
Java 11: Java EE modules removed (javax.xml.bind), Nashorn deprecated
Java 15: Nashorn removed, RMI Activation removed
Java 16: Wrapper class constructors deprecated (new Integer(5))
Java 17: Applet API removed, Security Manager deprecated
Java 18: finalize() deprecated for removal
Java 21: String Templates (preview), more Security Manager removed
```
