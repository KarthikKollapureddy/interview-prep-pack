# Java 8+ Features — Interview Q&A

> 17 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What is a functional interface? Name the core functional interfaces in `java.util.function`.

A functional interface has **exactly one abstract method** (SAM). It can have default/static methods.

| Interface | Method | Signature | Use |
|-----------|--------|-----------|-----|
| `Function<T,R>` | `apply(T)` | `T → R` | Transformation |
| `Predicate<T>` | `test(T)` | `T → boolean` | Filtering |
| `Consumer<T>` | `accept(T)` | `T → void` | Side effects |
| `Supplier<T>` | `get()` | `() → T` | Lazy creation |
| `UnaryOperator<T>` | `apply(T)` | `T → T` | Same-type transform |
| `BiFunction<T,U,R>` | `apply(T,U)` | `(T,U) → R` | Two-arg transform |
| `BinaryOperator<T>` | `apply(T,T)` | `(T,T) → T` | Reduce |

```java
@FunctionalInterface
interface PaymentValidator {
    boolean validate(Transaction txn);
    // Can have default methods
    default PaymentValidator and(PaymentValidator other) {
        return txn -> this.validate(txn) && other.validate(txn);
    }
}
```

---

### Q2. What is the difference between lambda expressions and method references? When to use each?

```java
// Lambda
list.forEach(s -> System.out.println(s));

// Method reference (equivalent, more concise)
list.forEach(System.out::println);
```

**4 types of method references:**
| Type | Syntax | Lambda equivalent |
|------|--------|-------------------|
| Static method | `Math::abs` | `x -> Math.abs(x)` |
| Instance method (bound) | `str::length` | `() -> str.length()` |
| Instance method (unbound) | `String::length` | `s -> s.length()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

**Use method references** when the lambda body just calls an existing method with the same parameters. If transformation is needed, use lambda.

---

### Q3. Explain `Optional` in Java. How does it prevent NullPointerException?

```java
// ❌ Without Optional
String city = user.getAddress().getCity(); // NPE if address is null

// ✅ With Optional
Optional<String> city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity);

// Usage
String result = city.orElse("Unknown");
String result = city.orElseThrow(() -> new NotFoundException("City not found"));
city.ifPresent(c -> log.info("City: {}", c));
```

**Anti-patterns to avoid:**
```java
// ❌ Using Optional as field type (not serializable, adds overhead)
class User { Optional<Address> address; } // BAD

// ❌ Calling get() without checking
optional.get(); // NoSuchElementException if empty — defeats the purpose

// ❌ Using Optional for method parameters
void process(Optional<String> name) { } // BAD — use overloading or @Nullable
```

**Correct usage:** Return type of methods that may return no value. Never as fields, parameters, or collections.

---

### Q4. What are `default` and `static` methods in interfaces? Why were they added in Java 8?

**Default methods:** Allow adding new methods to interfaces without breaking existing implementations.
```java
interface PaymentGateway {
    void processPayment(Payment p); // abstract
    
    default void logPayment(Payment p) { // default implementation
        System.out.println("Processing: " + p.getId());
    }
}
// Existing implementations don't need to change when logPayment is added
```

**Static methods:** Utility methods that belong to the interface, not the instance.
```java
interface Validator {
    boolean validate(String input);
    
    static Validator nonEmpty() {
        return input -> input != null && !input.isEmpty();
    }
    static Validator maxLength(int max) {
        return input -> input.length() <= max;
    }
}
// Usage: Validator v = Validator.nonEmpty();
```

**Why added:** To evolve the Collections API (adding `stream()`, `forEach()`) without breaking millions of existing implementations.

---

### Q5. What are Records in Java 17? How are they different from regular classes?

```java
// Record — auto-generates constructor, getters, equals, hashCode, toString
record Transaction(String id, double amount, String status) {}

// Equivalent to a class with:
// - private final fields
// - canonical constructor
// - accessor methods (id(), amount(), status())
// - equals() based on all fields
// - hashCode() based on all fields
// - toString() with all fields
```

**Restrictions:**
- Cannot extend other classes (implicitly extends `Record`)
- All fields are `final` — immutable
- No instance field declarations outside constructor
- Can implement interfaces, have static fields/methods, custom constructors

**At FedEx:** Scan event DTOs are perfect for records:
```java
record ScanEvent(String trackingNumber, String facility, Instant timestamp, String eventType) {
    // Compact constructor for validation
    ScanEvent {
        Objects.requireNonNull(trackingNumber, "Tracking number required");
        if (trackingNumber.isBlank()) throw new IllegalArgumentException("Empty tracking number");
    }
}
```

---

## Scenario-Based Questions

### Q6. At Hatio/BillDesk, you're building a chain of payment validators. Each validator checks one rule (amount limit, merchant status, fraud score). How would you compose them using functional interfaces?

```java
@FunctionalInterface
interface PaymentValidator {
    ValidationResult validate(Transaction txn);
    
    default PaymentValidator and(PaymentValidator next) {
        return txn -> {
            ValidationResult result = this.validate(txn);
            return result.isValid() ? next.validate(txn) : result;
        };
    }
}

// Individual validators
PaymentValidator amountCheck = txn -> txn.amount() > 0 && txn.amount() < 1_000_000
    ? ValidationResult.valid()
    : ValidationResult.invalid("Amount out of range");

PaymentValidator merchantCheck = txn -> merchantService.isActive(txn.merchantId())
    ? ValidationResult.valid()
    : ValidationResult.invalid("Merchant inactive");

PaymentValidator fraudCheck = txn -> fraudScore(txn) < 0.8
    ? ValidationResult.valid()
    : ValidationResult.invalid("Fraud score too high");

// Compose: all validators must pass
PaymentValidator fullValidation = amountCheck.and(merchantCheck).and(fraudCheck);
ValidationResult result = fullValidation.validate(transaction);
```

**Benefits:** Each validator has SRP. New rules = new lambda. No modification to existing validators (OCP). Short-circuit on first failure.

---

### Q7. At FedEx, you fetch shipment data from 3 APIs: tracking, customs, and billing. Using `Optional`, write null-safe code that extracts the delivery city from a deeply nested response.

```java
record ShipmentResponse(TrackingInfo tracking) {}
record TrackingInfo(DeliveryDetails delivery) {}
record DeliveryDetails(Address address) {}
record Address(String city, String state, String zip) {}

String deliveryCity = Optional.ofNullable(response)
    .map(ShipmentResponse::tracking)
    .map(TrackingInfo::delivery)
    .map(DeliveryDetails::address)
    .map(Address::city)
    .filter(city -> !city.isBlank())
    .orElse("UNKNOWN");
```

**Without Optional (the NPE nightmare):**
```java
String city = "UNKNOWN";
if (response != null && response.tracking() != null 
    && response.tracking().delivery() != null
    && response.tracking().delivery().address() != null
    && response.tracking().delivery().address().city() != null) {
    city = response.tracking().delivery().address().city();
}
```

---

### Q8. At NPCI, you need to process a list of transactions and return a Map of status → list of transaction IDs, but only for transactions in the last 24 hours. Use Java 8+ features end-to-end.

```java
Map<String, List<String>> statusMap = transactions.stream()
    .filter(txn -> txn.timestamp().isAfter(LocalDateTime.now().minusHours(24)))
    .collect(Collectors.groupingBy(
        Transaction::status,
        Collectors.mapping(Transaction::id, Collectors.toList())
    ));
```

**`Collectors.mapping()`** — applies a transformation before collecting. Here it extracts IDs instead of collecting full Transaction objects.

---

## Coding Challenges

### Challenge 1: Functional Pipeline Builder
**File:** `solutions/FunctionalPipeline.java`  
Build a generic data processing pipeline using `Function`:
1. Create a `Pipeline<T>` class that chains `Function` transformations
2. `addStep(Function<T, T> step)` — adds a transformation
3. `execute(T input)` — runs all steps in order
4. Demo: String pipeline → trim → lowercase → replace spaces with hyphens

### Challenge 2: Custom Optional
**File:** `solutions/CustomOptional.java`  
Implement a simplified version of `Optional<T>`:
1. `of(T value)`, `ofNullable(T value)`, `empty()`
2. `map(Function)`, `flatMap(Function)`, `filter(Predicate)`
3. `orElse(T)`, `orElseThrow(Supplier)`, `ifPresent(Consumer)`
4. Test with null-safe chaining scenario

### Challenge 3: Validator Combinator
**File:** `solutions/ValidatorCombinator.java`  
Using functional interfaces, build composable validators:
1. `Validator<T>` functional interface with `validate(T) → ValidationResult`
2. Combinator methods: `and()`, `or()`, `negate()`
3. Create 5 validators for a `User` object (name, email, age, phone, password)
4. Compose them and test with valid/invalid users

---

## Gotchas & Edge Cases

### Q9. What is the difference between `Predicate.and()` and `&&`?

`Predicate.and()` creates a **new composed Predicate** that can be stored, reused, and passed as an argument. `&&` is evaluated inline.

```java
Predicate<String> notEmpty = s -> !s.isEmpty();
Predicate<String> notNull = Objects::nonNull;
Predicate<String> valid = notNull.and(notEmpty); // Reusable composed predicate

// vs
boolean isValid = s != null && !s.isEmpty(); // Inline, not reusable
```

---

### Q10. What is effectively final? Why do lambdas require it?

A variable is effectively final if it's never modified after initialization (even without the `final` keyword).

```java
int x = 10;
// x = 20; // If uncommented, lambda below won't compile
Runnable r = () -> System.out.println(x); // x must be effectively final
```

**Why?** Lambdas capture a **copy** of the variable, not a reference. If the variable changed after capture, the lambda would have a stale value — confusing and error-prone.

---

### Q11. `orElse()` vs `orElseGet()` — when does it matter?

```java
// orElse — default is ALWAYS evaluated (even if Optional has value)
optional.orElse(expensiveComputation()); // expensiveComputation() runs regardless!

// orElseGet — default is LAZILY evaluated (only if Optional is empty)
optional.orElseGet(() -> expensiveComputation()); // runs only when needed
```

**Rule:** Use `orElseGet()` when the default involves a method call, DB query, or object creation. Use `orElse()` only for simple constants.

---

### Q11. Explain the Java 8 Date/Time API (`java.time`). Why was it introduced?

**Answer:**

**Problems with old `Date`/`Calendar`:**
- Mutable (thread-unsafe)
- Month is 0-indexed (`Calendar.JANUARY == 0`)
- No time zones done right
- `java.util.Date` is actually date+time (confusing)

**New `java.time` package — all classes are IMMUTABLE & THREAD-SAFE:**

| Class | Represents | Example |
|-------|-----------|---------|
| `LocalDate` | Date without time | `LocalDate.of(2026, 4, 20)` |
| `LocalTime` | Time without date | `LocalTime.of(14, 30, 0)` |
| `LocalDateTime` | Date + time, no timezone | `LocalDateTime.now()` |
| `ZonedDateTime` | Date + time + timezone | `ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))` |
| `Instant` | Machine timestamp (epoch) | `Instant.now()` — nanosecond precision |
| `Duration` | Time-based amount | `Duration.between(start, end).toHours()` |
| `Period` | Date-based amount | `Period.between(birthDate, today).getYears()` |

```java
// Common operations:
LocalDate today = LocalDate.now();
LocalDate nextWeek = today.plusDays(7);
LocalDate firstDay = today.withDayOfMonth(1);
boolean isLeap = today.isLeapYear();

// Parsing & formatting:
LocalDate parsed = LocalDate.parse("2026-04-20");
String formatted = today.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")); // 20-Apr-2026

// Duration vs Period:
Duration d = Duration.ofHours(2).plusMinutes(30);  // 2h 30m (for time)
Period p = Period.ofMonths(3);                      // 3 months (for dates)

// Convert from old Date:
Instant instant = oldDate.toInstant();
LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
```

**Interview trick question:**
```java
LocalDate d1 = LocalDate.of(2026, 3, 31);
LocalDate d2 = d1.plusMonths(1);  // April 30 (NOT April 31 — auto-adjusted!)
```

---

### Q12. What are Sealed Classes (Java 17) and Records (Java 16)?

**Answer:**

**Records** — immutable data carriers:
```java
// Replaces ~50 lines of boilerplate (constructor, getters, equals, hashCode, toString)
record Employee(String name, int age, String dept) {
    // Compact constructor for validation:
    Employee {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
        name = name.trim();  // normalize
    }

    // Custom methods are allowed:
    String displayName() { return name + " (" + dept + ")"; }
}

var emp = new Employee("Karthik", 25, "Engineering");
emp.name();    // "Karthik" (accessor, NOT getName())
emp.age();     // 25
// Records are: final, cannot extend classes, all fields private final
// Records CAN: implement interfaces, have static fields/methods
```

**Sealed Classes** — restrict inheritance hierarchy:
```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
non-sealed class Triangle implements Shape { /* open for further extension */ }

// Compiler enforces exhaustive switch (no default needed!):
double area = switch (shape) {
    case Circle c    -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.w() * r.h();
    case Triangle t  -> calculateTriangle(t);
};
```

| Feature | Records | Sealed Classes |
|---------|---------|---------------|
| Purpose | Immutable data carrier | Restrict inheritance |
| Since | Java 16 | Java 17 |
| Modifiers | Always `final` | `sealed`, `non-sealed`, `final` |
| Use case | DTOs, value objects | Domain models, algebraic types |

---

### Q13. New Switch Expressions & Pattern Matching (Java 14-21)

**Answer:**

```java
// Old switch (Java 7):
String result;
switch (status) {
    case "ACTIVE":
        result = "Running";
        break;            // easy to forget → fall-through bug
    case "INACTIVE":
        result = "Stopped";
        break;
    default:
        result = "Unknown";
}

// New switch EXPRESSION (Java 14+):
String result = switch (status) {
    case "ACTIVE"   -> "Running";     // arrow syntax, no break needed
    case "INACTIVE" -> "Stopped";
    default         -> "Unknown";
};  // ← note the semicolon (it's an expression)

// Multi-line case with yield:
String result = switch (status) {
    case "ACTIVE" -> {
        log("Processing active status");
        yield "Running";  // 'yield' returns value from block
    }
    default -> "Unknown";
};

// Pattern Matching for Switch (Java 21):
String describe(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> "positive int: " + i;
        case Integer i            -> "non-positive int: " + i;
        case String s             -> "string: " + s;
        case null                 -> "null value";
        default                   -> "other: " + obj;
    };
}
// ✅ Type check + cast + guard in one expression
// ✅ null handling (no NPE on switch(null))
// ✅ Exhaustive with sealed types (compiler-enforced)
```
