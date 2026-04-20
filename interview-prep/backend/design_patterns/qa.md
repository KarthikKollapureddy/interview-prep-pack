# GoF Design Patterns — Interview Q&A

> 15 must-know patterns with Java examples  
> Product companies (Amazon, Google, Flipkart, Razorpay) dedicate entire rounds to this

---

## Why Design Patterns Matter in Interviews

Interviewers test:
1. **Can you recognize** when a pattern applies?
2. **Can you implement** it with clean code?
3. **Can you explain trade-offs** (when NOT to use)?

---

## Creational Patterns

### Q1. Singleton — Thread-safe implementations (THE most asked pattern)

**When:** Exactly one instance needed — DB connection pool, config, logger.

```java
// Method 1: Eager initialization (simplest, thread-safe)
public class Singleton {
    private static final Singleton INSTANCE = new Singleton();
    private Singleton() {}
    public static Singleton getInstance() { return INSTANCE; }
}
// Pro: Thread-safe. Con: Instance created even if never used.

// Method 2: Double-checked locking (lazy, thread-safe)
public class Singleton {
    private static volatile Singleton instance;
    private Singleton() {}
    public static Singleton getInstance() {
        if (instance == null) {                   // 1st check (no lock)
            synchronized (Singleton.class) {
                if (instance == null) {            // 2nd check (with lock)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
// volatile prevents instruction reordering — partially constructed object issue

// Method 3: Bill Pugh (best — lazy + thread-safe, no synchronized)
public class Singleton {
    private Singleton() {}
    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }
    public static Singleton getInstance() { return Holder.INSTANCE; }
}
// Inner class loaded only when getInstance() called → lazy
// Class loading is thread-safe by JVM spec → no synchronization needed

// Method 4: Enum Singleton (Joshua Bloch recommended)
public enum Singleton {
    INSTANCE;
    public void doStuff() { }
}
// Handles serialization + reflection attacks automatically

// ⚠️ In Spring: Don't implement Singleton yourself — use @Scope("singleton") (default)
```

**Interview follow-up:** "How to break Singleton?"
```java
// 1. Reflection: Constructor c = Singleton.class.getDeclaredConstructor();
//    c.setAccessible(true); c.newInstance(); → Fix: throw exception in constructor if exists
// 2. Serialization: readResolve() → Fix: implement readResolve() { return INSTANCE; }
// 3. Cloning → Fix: throw CloneNotSupportedException
// Enum singleton prevents ALL three attacks.
```

---

### Q2. Factory Method — Decouple object creation from usage.

**When:** Don't know exact type at compile time; creation logic is complex.

```java
// ❌ Without Factory: switch/if-else scattered everywhere
Notification n;
if (type.equals("email")) n = new EmailNotification();
else if (type.equals("sms")) n = new SMSNotification();
// Adding new type → modify every place

// ✅ With Factory: centralized creation
public interface Notification {
    void send(String message);
}
public class EmailNotification implements Notification {
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
public class SMSNotification implements Notification {
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
public class PushNotification implements Notification {
    public void send(String message) {
        System.out.println("Push: " + message);
    }
}

public class NotificationFactory {
    public static Notification create(String channel) {
        return switch (channel.toLowerCase()) {
            case "email" -> new EmailNotification();
            case "sms"   -> new SMSNotification();
            case "push"  -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }
}

// Usage
Notification n = NotificationFactory.create("email");
n.send("Your order shipped!");
// Adding new type → only change Factory class (Open/Closed principle)
```

**In Spring:** `@Component` + `Map<String, Notification>` auto-wired by Spring.
```java
@Service
public class NotificationService {
    private final Map<String, Notification> strategies;  // Spring auto-collects all implementations
    
    public void send(String channel, String msg) {
        strategies.get(channel).send(msg);
    }
}
```

---

### Q3. Abstract Factory — Family of related objects.

**When:** Multiple families of related products that must be used together.

```java
// Payment gateway families — each gateway has its own processor, fraud checker, settlement
public interface PaymentGatewayFactory {
    PaymentProcessor createProcessor();
    FraudChecker createFraudChecker();
    RefundService createRefundService();
}

public class StripeFactory implements PaymentGatewayFactory {
    public PaymentProcessor createProcessor() { return new StripeProcessor(); }
    public FraudChecker createFraudChecker() { return new StripeFraudChecker(); }
    public RefundService createRefundService() { return new StripeRefundService(); }
}

public class RazorpayFactory implements PaymentGatewayFactory {
    public PaymentProcessor createProcessor() { return new RazorpayProcessor(); }
    public FraudChecker createFraudChecker() { return new RazorpayFraudChecker(); }
    public RefundService createRefundService() { return new RazorpayRefundService(); }
}

// Client code doesn't know which gateway — depends on abstraction
public class PaymentService {
    private final PaymentGatewayFactory factory;
    
    public PaymentService(PaymentGatewayFactory factory) {
        this.factory = factory;
    }
    
    public void processPayment(Order order) {
        factory.createFraudChecker().check(order);
        factory.createProcessor().charge(order.getAmount());
    }
}
```

**Factory Method vs Abstract Factory:**
| Factory Method | Abstract Factory |
|---------------|-----------------|
| Creates ONE product | Creates FAMILY of related products |
| Single method | Multiple factory methods |
| Use when one type varies | Use when entire product family varies |

---

### Q4. Builder — Complex object construction.

**When:** Object has many parameters (especially optional ones), construction has steps.

```java
// Without Builder: telescoping constructor nightmare
User u1 = new User("Karthik", "karthik@email.com", null, null, true, false, 0);

// With Builder: readable, self-documenting
User user = User.builder()
    .name("Karthik")
    .email("karthik@email.com")
    .phone("+91-9999999999")     // optional
    .address("Hyderabad")        // optional
    .active(true)
    .build();

// Implementation
public class User {
    private final String name;       // required
    private final String email;      // required
    private final String phone;      // optional
    private final String address;    // optional
    private final boolean active;

    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
        this.active = builder.active;
    }

    public static class Builder {
        private final String name;   // required in constructor
        private final String email;
        private String phone;
        private String address;
        private boolean active = true;

        public Builder(String name, String email) {
            this.name = name;
            this.email = email;
        }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder address(String addr) { this.address = addr; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        
        public User build() {
            // Validation here
            if (name == null || name.isBlank()) throw new IllegalStateException("Name required");
            return new User(this);
        }
    }
    
    public static Builder builder() { return new Builder(); }
}

// Shortcut: Use Lombok @Builder annotation in production
@Builder @Value
public class User { String name; String email; String phone; }
```

---

### Q5. Prototype — Clone existing objects.

**When:** Object creation is expensive (DB fetch, API call), need copies with slight modifications.

```java
public abstract class Shape implements Cloneable {
    protected String type;
    protected int x, y;
    
    @Override
    public Shape clone() {
        try {
            return (Shape) super.clone();  // Shallow copy
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// Registry of prototypes
public class ShapeRegistry {
    private Map<String, Shape> prototypes = new HashMap<>();
    
    public void register(String key, Shape shape) { prototypes.put(key, shape); }
    public Shape get(String key) { return prototypes.get(key).clone(); }
}
```

**Shallow vs Deep copy:**
```java
// Shallow: clone copies references (shared mutable objects = danger!)
// Deep: clone copies everything recursively
public class Order implements Cloneable {
    private List<Item> items;
    
    @Override
    public Order clone() {
        Order copy = (Order) super.clone();
        copy.items = new ArrayList<>(this.items);  // Deep copy the list
        return copy;
    }
}
```

---

## Structural Patterns

### Q6. Adapter — Make incompatible interfaces work together.

**When:** Integrating third-party libraries, legacy code with new interfaces.

```java
// Legacy payment system returns XML
public class LegacyPaymentGateway {
    public String processXml(String xmlPayload) { /* returns XML */ }
}

// New system expects JSON interface
public interface ModernPaymentProcessor {
    PaymentResult process(PaymentRequest request);
}

// Adapter bridges the gap
public class PaymentAdapter implements ModernPaymentProcessor {
    private final LegacyPaymentGateway legacy;
    
    public PaymentAdapter(LegacyPaymentGateway legacy) {
        this.legacy = legacy;
    }
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        String xml = convertToXml(request);         // Convert new format → old
        String xmlResponse = legacy.processXml(xml); // Call legacy
        return parseFromXml(xmlResponse);            // Convert old format → new
    }
}

// Real-world Spring example:
// HandlerAdapter in Spring MVC — adapts different handler types to common interface
```

---

### Q7. Decorator — Add behavior dynamically without modifying existing code.

**When:** Adding features (logging, caching, auth) to existing classes without inheritance explosion.

```java
// Base interface
public interface DataSource {
    void writeData(String data);
    String readData();
}

// Concrete implementation
public class FileDataSource implements DataSource {
    public void writeData(String data) { /* write to file */ }
    public String readData() { /* read from file */ }
}

// Decorator base
public abstract class DataSourceDecorator implements DataSource {
    protected final DataSource wrapped;
    public DataSourceDecorator(DataSource source) { this.wrapped = source; }
}

// Encryption decorator
public class EncryptionDecorator extends DataSourceDecorator {
    public EncryptionDecorator(DataSource source) { super(source); }
    
    public void writeData(String data) {
        wrapped.writeData(encrypt(data));  // Add encryption
    }
    public String readData() {
        return decrypt(wrapped.readData()); // Add decryption
    }
}

// Compression decorator
public class CompressionDecorator extends DataSourceDecorator {
    public void writeData(String data) {
        wrapped.writeData(compress(data));
    }
    public String readData() {
        return decompress(wrapped.readData());
    }
}

// Stack decorators — encryption + compression
DataSource source = new CompressionDecorator(
                      new EncryptionDecorator(
                        new FileDataSource("data.txt")));
source.writeData("sensitive data"); // compressed → encrypted → written

// Real-world Java: BufferedInputStream(new FileInputStream("file"))
// Real-world Spring: @Transactional, @Cacheable, @Async — all decorators (AOP proxies)
```

---

### Q8. Proxy — Control access to an object.

**When:** Lazy loading, access control, logging, caching, remote calls.

```java
// Virtual Proxy — lazy loading expensive objects
public interface Image {
    void display();
}

public class RealImage implements Image {
    private byte[] data;
    public RealImage(String path) {
        this.data = loadFromDisk(path);  // Expensive!
    }
    public void display() { /* render data */ }
}

public class LazyImageProxy implements Image {
    private RealImage realImage;
    private final String path;
    
    public LazyImageProxy(String path) { this.path = path; }
    
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(path);  // Load only when needed
        }
        realImage.display();
    }
}

// Protection Proxy — access control
public class SecureServiceProxy implements UserService {
    private final UserService realService;
    private final SecurityContext context;
    
    public User getUser(Long id) {
        if (!context.hasRole("ADMIN")) {
            throw new AccessDeniedException("Admin only");
        }
        return realService.getUser(id);
    }
}

// Real-world Spring: @Transactional, @Cacheable → Spring creates proxy objects
// JPA: Lazy loading → Hibernate creates proxies for related entities
```

---

### Q9. Facade — Simplify complex subsystems.

**When:** Complex system with many classes; provide simple interface for common use cases.

```java
// Without Facade: client must know about 5 subsystems
orderValidator.validate(order);
inventoryService.reserve(order.getItems());
paymentService.charge(order.getPayment());
shippingService.createLabel(order);
notificationService.sendConfirmation(order);

// With Facade: single entry point
public class OrderFacade {
    private final OrderValidator validator;
    private final InventoryService inventory;
    private final PaymentService payment;
    private final ShippingService shipping;
    private final NotificationService notification;
    
    public OrderResult placeOrder(Order order) {
        validator.validate(order);
        inventory.reserve(order.getItems());
        PaymentResult pr = payment.charge(order.getPayment());
        ShippingLabel label = shipping.createLabel(order);
        notification.sendConfirmation(order);
        return new OrderResult(pr, label);
    }
}

// Client: orderFacade.placeOrder(order);  // Simple!

// Real-world: Spring's JdbcTemplate (facade over raw JDBC)
// Real-world: SLF4J (facade over Log4j, Logback, JUL)
```

---

## Behavioral Patterns

### Q10. Strategy — Swap algorithms at runtime. (Covered briefly in LLD, expanded here)

```java
// Shipping cost calculation — different strategies per carrier
public interface ShippingCostStrategy {
    BigDecimal calculate(Package pkg, Address destination);
}

public class FedExGround implements ShippingCostStrategy {
    public BigDecimal calculate(Package pkg, Address dest) {
        return pkg.getWeight().multiply(new BigDecimal("2.50"));
    }
}
public class FedExOvernight implements ShippingCostStrategy {
    public BigDecimal calculate(Package pkg, Address dest) {
        return pkg.getWeight().multiply(new BigDecimal("8.75"));
    }
}
public class USPSStandard implements ShippingCostStrategy {
    public BigDecimal calculate(Package pkg, Address dest) {
        return pkg.getWeight().multiply(new BigDecimal("1.25"));
    }
}

// Context
public class ShippingCalculator {
    private ShippingCostStrategy strategy;
    public void setStrategy(ShippingCostStrategy s) { this.strategy = s; }
    public BigDecimal calculate(Package pkg, Address dest) {
        return strategy.calculate(pkg, dest);
    }
}

// In Spring: use Map injection for cleaner strategy selection
@Service
public class ShippingService {
    private final Map<String, ShippingCostStrategy> strategies;
    
    public BigDecimal calculate(String carrier, Package pkg, Address dest) {
        return strategies.get(carrier).calculate(pkg, dest);
    }
}
```

---

### Q11. Observer — Event-driven communication.

```java
// Without Observer: tight coupling
orderService.updateStatus(order, SHIPPED);
emailService.send(order);   // OrderService knows about EmailService
smsService.send(order);     // OrderService knows about SmsService
analyticsService.track(order); // ...and every other service

// With Observer: loosely coupled
public interface OrderEventListener {
    void onOrderStatusChanged(OrderEvent event);
}

@Component
public class EmailListener implements OrderEventListener {
    public void onOrderStatusChanged(OrderEvent event) {
        if (event.getNewStatus() == SHIPPED) {
            emailService.sendShippedEmail(event.getOrder());
        }
    }
}

@Component
public class AnalyticsListener implements OrderEventListener {
    public void onOrderStatusChanged(OrderEvent event) {
        analyticsService.track("order_status_change", event);
    }
}

// Spring's built-in event system (recommended):
@Component
public class OrderService {
    @Autowired private ApplicationEventPublisher publisher;
    
    public void updateStatus(Order order, Status status) {
        order.setStatus(status);
        publisher.publishEvent(new OrderStatusEvent(order, status));
    }
}

@Component
public class EmailListener {
    @EventListener
    public void handleOrderStatus(OrderStatusEvent event) {
        // send email
    }
}
```

---

### Q12. Template Method — Define algorithm skeleton, let subclasses fill in steps.

```java
// Data export — same flow, different formats
public abstract class DataExporter {
    
    // Template method — final to prevent override of flow
    public final void export(List<Record> records) {
        openFile();
        writeHeader();
        for (Record r : records) {
            writeRecord(r);        // Subclass implements
        }
        writeFooter();
        closeFile();
    }
    
    protected abstract void writeHeader();
    protected abstract void writeRecord(Record r);
    protected abstract void writeFooter();
    
    private void openFile() { /* common logic */ }
    private void closeFile() { /* common logic */ }
}

public class CSVExporter extends DataExporter {
    protected void writeHeader() { write("id,name,amount\n"); }
    protected void writeRecord(Record r) { write(r.toCsv() + "\n"); }
    protected void writeFooter() { /* nothing for CSV */ }
}

public class PDFExporter extends DataExporter {
    protected void writeHeader() { /* PDF header with logo */ }
    protected void writeRecord(Record r) { /* add table row */ }
    protected void writeFooter() { /* page numbers, totals */ }
}

// Real-world: Spring's JdbcTemplate, RestTemplate, AbstractController
```

---

### Q13. Chain of Responsibility — Pass request through chain of handlers.

```java
// Request validation chain
public abstract class ValidationHandler {
    private ValidationHandler next;
    
    public ValidationHandler setNext(ValidationHandler next) {
        this.next = next;
        return next;
    }
    
    public void validate(Request request) {
        doValidate(request);
        if (next != null) next.validate(request);
    }
    
    protected abstract void doValidate(Request request);
}

public class AuthenticationHandler extends ValidationHandler {
    protected void doValidate(Request request) {
        if (request.getToken() == null) throw new UnauthorizedException();
    }
}

public class RateLimitHandler extends ValidationHandler {
    protected void doValidate(Request request) {
        if (rateLimiter.isExceeded(request.getUserId())) throw new TooManyRequestsException();
    }
}

public class InputValidationHandler extends ValidationHandler {
    protected void doValidate(Request request) {
        if (request.getBody() == null) throw new BadRequestException();
    }
}

// Build chain
ValidationHandler chain = new AuthenticationHandler();
chain.setNext(new RateLimitHandler())
     .setNext(new InputValidationHandler());

chain.validate(incomingRequest);

// Real-world: Spring Security filter chain, Servlet filters, Interceptors
```

---

### Q14. Command — Encapsulate request as an object.

**When:** Undo/redo, task queues, macro recording.

```java
public interface Command {
    void execute();
    void undo();
}

public class TransferCommand implements Command {
    private final Account from, to;
    private final BigDecimal amount;
    
    public void execute() {
        from.debit(amount);
        to.credit(amount);
    }
    
    public void undo() {
        to.debit(amount);
        from.credit(amount);
    }
}

// Command history for undo
public class CommandHistory {
    private final Deque<Command> history = new ArrayDeque<>();
    
    public void execute(Command cmd) {
        cmd.execute();
        history.push(cmd);
    }
    
    public void undo() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}
```

---

### Q15. State — Object behavior changes based on internal state.

```java
// Order state machine
public interface OrderState {
    void next(OrderContext context);
    void cancel(OrderContext context);
    String getStatus();
}

public class PendingState implements OrderState {
    public void next(OrderContext ctx) { ctx.setState(new ConfirmedState()); }
    public void cancel(OrderContext ctx) { ctx.setState(new CancelledState()); }
    public String getStatus() { return "PENDING"; }
}

public class ConfirmedState implements OrderState {
    public void next(OrderContext ctx) { ctx.setState(new ShippedState()); }
    public void cancel(OrderContext ctx) { ctx.setState(new CancelledState()); }
    public String getStatus() { return "CONFIRMED"; }
}

public class ShippedState implements OrderState {
    public void next(OrderContext ctx) { ctx.setState(new DeliveredState()); }
    public void cancel(OrderContext ctx) { throw new IllegalStateException("Cannot cancel shipped order"); }
    public String getStatus() { return "SHIPPED"; }
}

public class DeliveredState implements OrderState {
    public void next(OrderContext ctx) { throw new IllegalStateException("Already delivered"); }
    public void cancel(OrderContext ctx) { throw new IllegalStateException("Cannot cancel delivered order"); }
    public String getStatus() { return "DELIVERED"; }
}

public class OrderContext {
    private OrderState state = new PendingState();
    
    public void setState(OrderState state) { this.state = state; }
    public void nextStep() { state.next(this); }
    public void cancel() { state.cancel(this); }
    public String getStatus() { return state.getStatus(); }
}

// Usage
OrderContext order = new OrderContext();
order.nextStep();  // PENDING → CONFIRMED
order.nextStep();  // CONFIRMED → SHIPPED
order.cancel();    // throws IllegalStateException
```

---

## Pattern Selection Cheat Sheet

| Problem | Pattern | Key Indicator |
|---------|---------|---------------|
| One instance only | **Singleton** | "only one", "shared resource" |
| Create objects without knowing type | **Factory** | "based on type/config" |
| Family of related objects | **Abstract Factory** | "multiple providers" |
| Complex object with many params | **Builder** | "optional fields", "step-by-step" |
| Clone expensive objects | **Prototype** | "copy", "template" |
| Incompatible interfaces | **Adapter** | "legacy", "third-party" |
| Add behavior dynamically | **Decorator** | "add feature without changing" |
| Control access | **Proxy** | "lazy load", "access control", "logging" |
| Simplify complex system | **Facade** | "single entry point" |
| Swap algorithm at runtime | **Strategy** | "multiple ways to do same thing" |
| Notify on change | **Observer** | "event", "publish/subscribe" |
| Define skeleton, vary steps | **Template Method** | "same flow, different details" |
| Pass through handlers | **Chain of Responsibility** | "filters", "middleware" |
| Encapsulate action as object | **Command** | "undo", "queue", "macro" |
| Behavior depends on state | **State** | "state machine", "transitions" |

---

## What Interviewers Want to Hear

1. **"I used [Pattern] in my project because..."** — real-world application
2. **"The alternative was... but [Pattern] was better because..."** — trade-off analysis
3. **"In Spring, this maps to..."** — framework awareness
4. **"The downside is..."** — shows maturity (no pattern is perfect)

---

### Q15. State Pattern

**Problem:** Object behavior changes based on internal state. Avoid massive if/else or switch blocks.

**Solution:** Encapsulate state-specific behavior in separate classes. Object delegates to current state.

```java
// State interface:
interface OrderState {
    void next(Order order);
    void cancel(Order order);
    String getStatus();
}

// Concrete states:
class PlacedState implements OrderState {
    public void next(Order o)  { o.setState(new ShippedState()); }
    public void cancel(Order o){ o.setState(new CancelledState()); }
    public String getStatus()  { return "PLACED"; }
}

class ShippedState implements OrderState {
    public void next(Order o)  { o.setState(new DeliveredState()); }
    public void cancel(Order o){ throw new IllegalStateException("Cannot cancel shipped order"); }
    public String getStatus()  { return "SHIPPED"; }
}

class DeliveredState implements OrderState {
    public void next(Order o)  { throw new IllegalStateException("Already delivered"); }
    public void cancel(Order o){ throw new IllegalStateException("Cannot cancel delivered order"); }
    public String getStatus()  { return "DELIVERED"; }
}

class CancelledState implements OrderState {
    public void next(Order o)  { throw new IllegalStateException("Order cancelled"); }
    public void cancel(Order o){ throw new IllegalStateException("Already cancelled"); }
    public String getStatus()  { return "CANCELLED"; }
}

// Context:
class Order {
    private OrderState state = new PlacedState();
    void setState(OrderState s) { this.state = s; }
    void next()   { state.next(this); }
    void cancel() { state.cancel(this); }
    String getStatus() { return state.getStatus(); }
}

// Usage:
Order order = new Order();
order.getStatus();  // PLACED
order.next();       // → SHIPPED
order.next();       // → DELIVERED
order.cancel();     // throws IllegalStateException
```

**When to use:** Vending machines, ATMs, order workflows, connection states, game states.
**Spring equivalent:** Spring State Machine (`spring-statemachine`) for complex state transitions.

**State vs Strategy:**
| Feature | State | Strategy |
|---------|-------|----------|
| Object awareness | States know about transitions (call `context.setState()`) | Strategies are independent |
| Transitions | State changes itself | Client changes strategy |
| Use case | Object whose behavior changes over time | Swappable algorithm |
