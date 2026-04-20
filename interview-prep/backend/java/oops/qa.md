# OOP & SOLID Principles — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas  
> Difficulty: Hatio = baseline, FedEx/NPCI = harder

---

## Conceptual Questions

### Q1. Explain the four pillars of OOP with real examples from your projects.

| Pillar | Definition | Your Project Example |
|--------|-----------|---------------------|
| **Encapsulation** | Bundling data + methods, restricting direct access | At FedEx, `ScanEvent` class encapsulates tracking data with private fields and public getters. External services can't modify `eventTimestamp` directly. |
| **Abstraction** | Hiding complexity, exposing only what's needed | At UHG, `ProviderService` interface exposes `updateDemographics()` — callers don't know if it hits MySQL, Kafka, or a downstream API. |
| **Inheritance** | Child class inherits from parent | `BaseGatewayService` provides common logging/metrics. `OnRoadEventGateway` and `SortPackageScanGateway` extend it. |
| **Polymorphism** | Same interface, different behavior | `NotificationSender.send()` → `EmailSender`, `SmsSender`, `PushSender` each implement differently. Called via interface reference. |

**Key interview trap:** "Is inheritance always good?" — No. Favor **composition over inheritance** (GoF principle). Inheritance creates tight coupling. Use interfaces + delegation instead.

---

### Q2. Explain all 5 SOLID principles with code examples.

**S — Single Responsibility Principle (SRP)**
```java
// ❌ BAD: Class does two things
class UserService {
    void createUser(User u) { /* DB logic */ }
    void sendWelcomeEmail(User u) { /* Email logic */ }
}

// ✅ GOOD: Separated responsibilities
class UserService { void createUser(User u) { /* DB logic */ } }
class EmailService { void sendWelcomeEmail(User u) { /* Email logic */ } }
```

**O — Open/Closed Principle (OCP)**
```java
// ✅ Open for extension, closed for modification
interface PaymentProcessor { void process(Payment p); }
class UpiProcessor implements PaymentProcessor { /* UPI logic */ }
class CardProcessor implements PaymentProcessor { /* Card logic */ }
// Adding NACH → create NachProcessor, don't modify existing classes
```

**L — Liskov Substitution Principle (LSP)**
```java
// ❌ VIOLATES LSP: Square overrides setWidth/setHeight breaking Rectangle contract
class Rectangle { void setWidth(int w); void setHeight(int h); }
class Square extends Rectangle { /* sets both w and h — surprises callers */ }

// ✅ FIX: Use separate Shape interface with area()
```

**I — Interface Segregation Principle (ISP)**
```java
// ❌ FAT interface forces unnecessary implementations
interface Worker { void code(); void test(); void deploy(); void managePeople(); }

// ✅ Segregated
interface Coder { void code(); }
interface Tester { void test(); }
interface Deployer { void deploy(); }
// A dev implements Coder + Tester. A DevOps implements Deployer.
```

**D — Dependency Inversion Principle (DIP)**
```java
// ❌ High-level depends on low-level
class OrderService { private MySqlRepo repo = new MySqlRepo(); }

// ✅ Depend on abstraction
class OrderService {
    private final OrderRepository repo; // interface
    OrderService(OrderRepository repo) { this.repo = repo; } // injected
}
```

---

### Q3. What is the difference between abstract class and interface in Java 17?

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| Methods | Abstract + concrete | Abstract + default + static + private (Java 9+) |
| State | Can have instance fields | Only static final constants |
| Constructor | Yes | No |
| Inheritance | Single (extends one) | Multiple (implements many) |
| Access modifiers | Any | Methods are public by default |

**When to use abstract class:** Shared state + partial implementation (e.g., `BaseGatewayService` with common `log()` and `metrics()` methods).

**When to use interface:** Defining a contract across unrelated classes (e.g., `Serializable`, `PaymentProcessor`).

**Java 17 sealed classes** add a third option:
```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
// Only these 3 can implement Shape — exhaustive pattern matching in switch
```

---

### Q4. Explain method overloading vs overriding. What are the rules for each?

**Overloading (compile-time polymorphism):**
- Same method name, different parameter list (type, count, or order)
- Return type can differ
- Access modifier can differ
- Resolved at compile time based on reference type

**Overriding (runtime polymorphism):**
- Same method signature in parent and child
- Return type must be same or covariant (subtype)
- Access modifier must be same or wider (not narrower)
- Cannot override `final`, `static`, or `private` methods
- `@Override` annotation is optional but strongly recommended
- Resolved at runtime based on actual object type

```java
// Overloading
class Printer {
    void print(String s) { }
    void print(int i) { }        // different param type
    void print(String s, int copies) { } // different param count
}

// Overriding
class Animal { void speak() { System.out.println("..."); } }
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
}
```

---

### Q5. What is the diamond problem? How does Java solve it?

The diamond problem occurs when a class inherits from two classes that both inherit from a common ancestor, creating ambiguity about which method implementation to use.

**Java prevents it by not allowing multiple class inheritance.** But with interfaces having `default` methods, the problem resurfaces:

```java
interface A { default void hello() { System.out.println("A"); } }
interface B { default void hello() { System.out.println("B"); } }

class C implements A, B {
    // ❌ Compilation error if not overridden — ambiguous
    @Override
    public void hello() {
        A.super.hello(); // Explicitly choose which default to call
    }
}
```

**Rules:**
1. Class method wins over interface default method
2. More specific interface wins (sub-interface > super-interface)
3. If still ambiguous → must override and explicitly resolve

---

## Scenario-Based Questions

### Q6. At FedEx, you're designing the Sort Package Scan Gateway. Different scanner types (STARV, FORGE, manual barcode) produce events in different formats but need to be processed uniformly. How would you design this using OOP?

```java
// Strategy Pattern + Interface Polymorphism
interface ScanEventParser {
    ScanEvent parse(byte[] rawData);
    boolean supports(ScannerType type);
}

class StarvParser implements ScanEventParser {
    public ScanEvent parse(byte[] rawData) { /* STARV-specific parsing */ }
    public boolean supports(ScannerType type) { return type == ScannerType.STARV; }
}

class ForgeParser implements ScanEventParser {
    public ScanEvent parse(byte[] rawData) { /* FORGE-specific parsing */ }
    public boolean supports(ScannerType type) { return type == ScannerType.FORGE; }
}

// Factory/Registry
class ScanEventParserFactory {
    private final List<ScanEventParser> parsers;
    
    ScanEvent parse(ScannerType type, byte[] data) {
        return parsers.stream()
            .filter(p -> p.supports(type))
            .findFirst()
            .orElseThrow(() -> new UnsupportedScannerException(type))
            .parse(data);
    }
}
```

**OCP in action:** Adding a new scanner type = create a new parser class + register it. Zero changes to existing code.

---

### Q7. At NPCI, the UPI payment flow has multiple steps: validate → debit → credit → notify. Each step can fail. How would you model this using OOP principles to ensure each step is independently testable and replaceable?

```java
// Chain of Responsibility + SRP
interface PaymentStep {
    PaymentContext execute(PaymentContext ctx) throws PaymentStepException;
    String stepName();
}

class ValidateStep implements PaymentStep { /* validates amount, VPA, limits */ }
class DebitStep implements PaymentStep { /* debits sender bank via IMPS */ }
class CreditStep implements PaymentStep { /* credits receiver bank */ }
class NotifyStep implements PaymentStep { /* sends SMS/push notification */ }

class PaymentPipeline {
    private final List<PaymentStep> steps;
    
    PaymentResult execute(PaymentRequest request) {
        PaymentContext ctx = new PaymentContext(request);
        for (PaymentStep step : steps) {
            try {
                ctx = step.execute(ctx);
            } catch (PaymentStepException e) {
                return PaymentResult.failed(step.stepName(), e);
            }
        }
        return PaymentResult.success(ctx);
    }
}
```

**Benefits:** Each step has SRP. Steps are injectable (DIP). Pipeline is open for extension (add AuditStep without modifying existing steps). Each step is independently unit-testable with mocks.

---

### Q8. At Hatio/BillDesk, you're building a notification system for merchant settlements. Merchants can receive notifications via email, SMS, webhook, or in-app. A merchant can opt into multiple channels. How would you design this?

```java
// Observer Pattern + Strategy
interface NotificationChannel {
    void send(Merchant merchant, SettlementNotification notification);
    ChannelType getType();
}

class EmailChannel implements NotificationChannel { /* SMTP logic */ }
class SmsChannel implements NotificationChannel { /* SMS gateway */ }
class WebhookChannel implements NotificationChannel { /* HTTP POST to merchant URL */ }
class InAppChannel implements NotificationChannel { /* WebSocket push */ }

class NotificationService {
    private final Map<ChannelType, NotificationChannel> channels;
    
    void notifyMerchant(Merchant merchant, SettlementNotification notification) {
        merchant.getEnabledChannels().stream()
            .map(channels::get)
            .filter(Objects::nonNull)
            .forEach(channel -> channel.send(merchant, notification));
    }
}
```

**ISP applied:** Each channel only implements what it needs. `WebhookChannel` doesn't need SMS gateway dependencies.

---

### Q9. Explain composition vs inheritance with a real scenario. When would you refactor from inheritance to composition?

**Scenario:** At FedEx, initially `ExpressShipment extends Shipment` and `FreightShipment extends Shipment`. Then a requirement comes: "Express shipment with freight-like insurance." Inheritance can't handle this — Java doesn't support multiple class inheritance.

```java
// ❌ Inheritance: rigid hierarchy
class Shipment { }
class ExpressShipment extends Shipment { void expressTracking() {} }
class FreightShipment extends Shipment { void freightInsurance() {} }
// Can't create ExpressWithInsurance without code duplication

// ✅ Composition: flexible behavior mixing
class Shipment {
    private TrackingStrategy tracking;
    private InsurancePolicy insurance;
    private DeliverySpeed speed;
    
    Shipment(TrackingStrategy t, InsurancePolicy i, DeliverySpeed s) {
        this.tracking = t; this.insurance = i; this.speed = s;
    }
}
// Express with insurance = new Shipment(expressTracking, freightInsurance, express)
```

**Rule of thumb:** "Has-a" → composition. "Is-a" → inheritance (but still prefer composition).

---

### Q10. What are the different types of relationships in OOP? Explain with code.

| Relationship | Strength | Lifetime | Example |
|-------------|----------|----------|---------|
| **Association** | Weak | Independent | Teacher ↔ Student |
| **Aggregation** | Medium | Container doesn't own child | Department has Employees (employees exist without dept) |
| **Composition** | Strong | Container owns child | House has Rooms (rooms don't exist without house) |
| **Inheritance** | Strongest | Permanent | Dog is-a Animal |

```java
// Aggregation — Employee exists independently
class Department {
    private List<Employee> employees; // injected, not created here
}

// Composition — Engine created and destroyed with Car
class Car {
    private final Engine engine = new Engine(); // owned, created internally
}
```

---

### Q11. At UHG, your My Practice Profile service needs to handle different provider types: Individual, Organization, and Group. Each has different validation rules but shares common fields (NPI, name, address). Design this.

```java
// Template Method Pattern
abstract class ProviderValidator {
    // Template method — defines the algorithm skeleton
    public final ValidationResult validate(Provider provider) {
        ValidationResult result = new ValidationResult();
        validateCommonFields(provider, result);  // shared
        validateSpecificFields(provider, result); // subclass-specific
        validateBusinessRules(provider, result);   // subclass-specific
        return result;
    }
    
    private void validateCommonFields(Provider p, ValidationResult r) {
        if (p.getNpi() == null || !p.getNpi().matches("\\d{10}")) r.addError("Invalid NPI");
        if (p.getName() == null || p.getName().isBlank()) r.addError("Name required");
    }
    
    protected abstract void validateSpecificFields(Provider p, ValidationResult r);
    protected abstract void validateBusinessRules(Provider p, ValidationResult r);
}

class IndividualProviderValidator extends ProviderValidator {
    protected void validateSpecificFields(Provider p, ValidationResult r) {
        if (p.getSsn() == null) r.addError("SSN required for individual");
    }
    protected void validateBusinessRules(Provider p, ValidationResult r) {
        // Individual-specific: license check, specialty validation
    }
}
```

---

## Coding Challenges

### Challenge 1: Design a Payment Processor
**File:** `solutions/PaymentProcessor.java`  
Design a payment processing system using OOP principles:
1. Create an interface `PaymentMethod` with `processPayment(double amount)` and `validate()`
2. Implement `CreditCardPayment`, `UpiPayment`, `NetBankingPayment`
3. Create a `PaymentService` that accepts any `PaymentMethod` and processes payments with logging
4. Demonstrate polymorphism in a `main()` method

### Challenge 2: Builder Pattern for Shipment
**File:** `solutions/ShipmentBuilder.java`  
At FedEx, shipments have many optional fields (insurance, signature-required, special-handling, priority, customs-info). Implement:
1. A `Shipment` class with 8+ fields
2. A `Shipment.Builder` using the Builder pattern
3. Validation in `build()` — throw if required fields are missing
4. Make `Shipment` immutable (all fields final, no setters)

### Challenge 3: Notification System with Observer Pattern
**File:** `solutions/NotificationSystem.java`  
Implement a merchant notification system:
1. `NotificationChannel` interface with `notify(Event event)`
2. Three implementations: `EmailChannel`, `SmsChannel`, `WebhookChannel`
3. `EventBus` class that allows subscribe/unsubscribe by event type
4. Demonstrate: Merchant subscribes to "SETTLEMENT" events via email + webhook, "REFUND" via SMS only

---

## Gotchas & Edge Cases

### Q12. Can you override a static method?

No. Static methods belong to the class, not the instance. If you define a static method with the same signature in a child class, it **hides** (not overrides) the parent method.

```java
class Parent { static void greet() { System.out.println("Parent"); } }
class Child extends Parent { static void greet() { System.out.println("Child"); } }

Parent p = new Child();
p.greet(); // Prints "Parent" — resolved by reference type, not object type
```

This is **method hiding**, not polymorphism. `@Override` on a static method → compilation error.

---

### Q13. What is the `equals()` and `hashCode()` contract? What breaks if you violate it?

**Contract:**
1. If `a.equals(b)` is true → `a.hashCode() == b.hashCode()` must be true
2. If `a.hashCode() != b.hashCode()` → `a.equals(b)` must be false
3. `hashCode()` can return same value for non-equal objects (collision is OK)

**What breaks:** If you override `equals()` but not `hashCode()`, objects that are "equal" hash to different buckets in `HashMap`/`HashSet`:
```java
class Employee {
    String id;
    @Override public boolean equals(Object o) { /* compare by id */ }
    // ❌ hashCode() not overridden — uses Object.hashCode() (memory address)
}

Set<Employee> set = new HashSet<>();
set.add(new Employee("E1"));
set.contains(new Employee("E1")); // FALSE — different hashCode, different bucket
```

**At Hatio:** This exact bug caused duplicate transaction records in a settlement batch — two `Transaction` objects with same ID were treated as different because `hashCode()` wasn't overridden.

---

### Q14. What happens when you clone an object? Shallow vs deep copy.

```java
class Team implements Cloneable {
    String name;
    List<Member> members;
    
    @Override
    protected Team clone() throws CloneNotSupportedException {
        return (Team) super.clone(); // SHALLOW copy
    }
}

Team original = new Team("Dev", members);
Team copy = original.clone();
copy.members.add(new Member("New")); // ❌ Modifies original.members too!
```

**Shallow copy:** Copies references, not objects. Both original and copy point to the same `List<Member>`.

**Deep copy:** Creates new instances of all nested objects.
```java
@Override
protected Team clone() throws CloneNotSupportedException {
    Team copy = (Team) super.clone();
    copy.members = new ArrayList<>(this.members); // new list, same Member refs
    // For true deep copy: copy.members = this.members.stream().map(Member::clone).collect(toList());
    return copy;
}
```

**Better alternative:** Use copy constructors or serialization instead of `Cloneable` (Josh Bloch recommends avoiding `Cloneable`).

---

### Q15. What are `sealed` classes in Java 17? When would you use them?

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
record Triangle(double a, double b, double c) implements Shape {}
```

**Benefits:**
1. **Exhaustive pattern matching** in switch (compiler warns if a case is missing)
2. **Controlled inheritance** — only permitted classes can extend/implement
3. **Domain modeling** — "A transaction can ONLY be Pending, Completed, or Failed"

```java
// Java 17 pattern matching
double area(Shape s) {
    return switch (s) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.w() * r.h();
        case Triangle t -> /* Heron's formula */;
        // No default needed — compiler knows all cases are covered
    };
}
```

**At NPCI:** `sealed interface TransactionStatus permits Initiated, Processing, Completed, Failed, Reversed {}` — guarantees no rogue status types at compile time.
