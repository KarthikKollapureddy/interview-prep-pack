# System Design — Low Level Design (LLD) — Interview Q&A

> 12 questions covering SOLID, Design Patterns, Class Design  
> Focus: Code-level design, UML-style thinking, interview whiteboard problems

> **⚠️ TODO:** Add coded LLD solutions in `solutions/` directory.  
> Implement at least: Parking Lot, BookMyShow, Elevator System, Splitwise in Java.  
> Each should include: class diagram, interfaces, SOLID principles applied.

---

## SOLID Principles

### Q1. Explain SOLID with Java examples.

**S — Single Responsibility:** One class, one reason to change.
```java
// ❌ God class
class OrderService {
    void createOrder() { }
    void sendEmail() { }      // Should be in NotificationService
    void generatePDF() { }    // Should be in ReportService
}

// ✅ Each class has one job
class OrderService { void createOrder() { } }
class NotificationService { void sendEmail() { } }
class ReportService { void generatePDF() { } }
```

**O — Open/Closed:** Open for extension, closed for modification.
```java
// ✅ Add new discount types without modifying existing code
interface DiscountStrategy { double apply(double price); }
class FlatDiscount implements DiscountStrategy { ... }
class PercentDiscount implements DiscountStrategy { ... }
// Add new: class BuyOneGetOneDiscount implements DiscountStrategy { ... }
```

**L — Liskov Substitution:** Subtypes must be substitutable for base types.
```java
// ❌ Violates LSP: Square overrides width/height in unexpected way
class Rectangle { void setWidth(int w); void setHeight(int h); }
class Square extends Rectangle { 
    void setWidth(int w) { super.setWidth(w); super.setHeight(w); } // Surprise!
}
```

**I — Interface Segregation:** Don't force clients to depend on methods they don't use.
```java
// ❌ Fat interface
interface Worker { void work(); void eat(); void sleep(); }

// ✅ Segregated
interface Workable { void work(); }
interface Feedable { void eat(); }
class Robot implements Workable { void work() { } }  // No forced eat()
```

**D — Dependency Inversion:** Depend on abstractions, not concretions.
```java
// ❌ Tightly coupled
class OrderService { private MySQLRepository repo = new MySQLRepository(); }

// ✅ Depends on abstraction
class OrderService {
    private final OrderRepository repo;  // Interface
    OrderService(OrderRepository repo) { this.repo = repo; }
}
```

---

## Design Patterns (Most Asked)

### Q2. Strategy Pattern — when and how?

**When:** Multiple algorithms for same task, selected at runtime.

```java
// Payment processing at Hatio/BillDesk
interface PaymentStrategy {
    PaymentResult pay(BigDecimal amount);
}

class UPIPayment implements PaymentStrategy {
    public PaymentResult pay(BigDecimal amount) { /* UPI flow */ }
}
class CardPayment implements PaymentStrategy {
    public PaymentResult pay(BigDecimal amount) { /* Card flow */ }
}
class WalletPayment implements PaymentStrategy {
    public PaymentResult pay(BigDecimal amount) { /* Wallet flow */ }
}

class PaymentService {
    public PaymentResult processPayment(PaymentStrategy strategy, BigDecimal amount) {
        return strategy.pay(amount);
    }
}
```

---

### Q3. Observer Pattern — event-driven design.

```java
// Order status change notifies multiple listeners
interface OrderEventListener {
    void onStatusChange(Order order, OrderStatus newStatus);
}

class EmailNotifier implements OrderEventListener { ... }
class SMSNotifier implements OrderEventListener { ... }
class InventoryUpdater implements OrderEventListener { ... }

class OrderService {
    private List<OrderEventListener> listeners = new ArrayList<>();
    
    public void updateStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        listeners.forEach(l -> l.onStatusChange(order, status));
    }
}
```

**In Spring:** Use `ApplicationEventPublisher` + `@EventListener`.

---

### Q4. Builder Pattern — when constructors get complex.

```java
// Shipment with many optional fields (FedEx)
Shipment shipment = Shipment.builder()
    .trackingNumber("FX123456789")
    .origin(Address.of("Memphis", "TN"))
    .destination(Address.of("Dallas", "TX"))
    .weight(2.5)
    .serviceType(ServiceType.OVERNIGHT)
    .signature(true)           // optional
    .insurance(500.00)         // optional
    .build();

// With Lombok: @Builder on class → generates builder automatically
@Builder
@Value
public class Shipment {
    String trackingNumber;
    Address origin;
    Address destination;
    double weight;
    ServiceType serviceType;
    boolean signature;
    double insurance;
}
```

---

### Q5. Factory Pattern vs Abstract Factory.

```java
// Factory Method: create objects without specifying exact class
class NotificationFactory {
    static Notification create(String channel) {
        return switch (channel) {
            case "email" -> new EmailNotification();
            case "sms"   -> new SMSNotification();
            case "push"  -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown: " + channel);
        };
    }
}

// Abstract Factory: family of related objects
interface PaymentGatewayFactory {
    PaymentProcessor createProcessor();
    FraudChecker createFraudChecker();
    SettlementService createSettlement();
}
class StripeFactory implements PaymentGatewayFactory { ... }
class RazorpayFactory implements PaymentGatewayFactory { ... }
```

---

## LLD Interview Problems

### Q6. Design a Parking Lot System.

```
Classes:
  ParkingLot
    - floors: List<ParkingFloor>
    - entryPanels: List<EntryPanel>
    - exitPanels: List<ExitPanel>
    + findAvailableSpot(VehicleType): ParkingSpot
    
  ParkingFloor
    - spots: Map<SpotType, List<ParkingSpot>>
    + getAvailableSpots(VehicleType): List<ParkingSpot>
    
  ParkingSpot
    - id, floor, type (COMPACT, REGULAR, LARGE)
    - vehicle: Vehicle (null if empty)
    + isAvailable(): boolean
    + park(Vehicle): void
    + unpark(): Vehicle
    
  Vehicle (abstract)
    - licensePlate, type
    → Car, Truck, Motorcycle
    
  Ticket
    - id, spot, vehicle, entryTime, exitTime
    
  PaymentService
    + calculateFee(Ticket): BigDecimal
    → HourlyRate strategy per vehicle type
```

**Key patterns:** Strategy (pricing), Factory (vehicle creation), Singleton (ParkingLot).

---

### Q7. Design a Rate Limiter.

```java
// Token Bucket algorithm
class TokenBucketRateLimiter {
    private final int maxTokens;
    private final int refillRate;   // tokens per second
    private double tokens;
    private long lastRefillTime;
    
    public synchronized boolean allowRequest() {
        refill();
        if (tokens >= 1) {
            tokens--;
            return true;
        }
        return false;
    }
    
    private void refill() {
        long now = System.nanoTime();
        double elapsed = (now - lastRefillTime) / 1_000_000_000.0;
        tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
        lastRefillTime = now;
    }
}

// Sliding Window (Redis-based, distributed)
// Key: rate_limit:{userId}:{windowStart}
// INCR + EXPIRE
```

**Algorithms:** Token Bucket (bursty OK), Leaky Bucket (smooth), Sliding Window (precise), Fixed Window (simple).

---

### Q8. Design an LRU Cache.

```java
class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null); // dummy
    private final Node<K, V> tail = new Node<>(null, null); // dummy
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }
    
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToHead(node);
        return node.value;
    }
    
    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            node = new Node<>(key, value);
            map.put(key, node);
            addToHead(node);
            if (map.size() > capacity) {
                Node<K, V> evicted = removeTail();
                map.remove(evicted.key);
            }
        }
    }
    
    // DoublyLinkedList operations: addToHead, removeNode, moveToHead, removeTail
}
```

**Time complexity:** O(1) get and put. HashMap + Doubly Linked List.

---

## Gotchas & Edge Cases

### Q9. When NOT to use design patterns?

- **Don't use Singleton** for everything — use DI instead (Spring manages scope)
- **Don't use Strategy** if there's only one implementation — YAGNI
- **Don't use Abstract Factory** if only one product family exists
- **Over-engineering** is the #1 LLD interview mistake — start simple, add complexity only when justified

### Q10. Common LLD interview mistakes.

1. Jumping to code without clarifying requirements
2. Missing edge cases (concurrent access, null inputs, capacity limits)
3. Not mentioning thread safety when asked about shared resources
4. Ignoring extensibility — "what if we add a new vehicle type?"
5. Not discussing trade-offs (memory vs speed, consistency vs availability)

---

## More LLD Problems (Product Company Favorites)

### Q11. Design an Elevator System.

```java
// Enums
enum Direction { UP, DOWN, IDLE }
enum DoorState { OPEN, CLOSED }

// Request
class ElevatorRequest {
    int floor;
    Direction direction;  // null for internal requests
}

// Elevator
class Elevator {
    int id;
    int currentFloor = 0;
    Direction direction = Direction.IDLE;
    DoorState doorState = DoorState.CLOSED;
    TreeSet<Integer> upStops = new TreeSet<>();     // sorted ascending
    TreeSet<Integer> downStops = new TreeSet<>(Collections.reverseOrder()); // sorted descending
    
    void addStop(int floor) {
        if (floor > currentFloor) upStops.add(floor);
        else if (floor < currentFloor) downStops.add(floor);
    }
    
    void move() {
        if (direction == Direction.UP && !upStops.isEmpty()) {
            currentFloor = upStops.pollFirst();
            openDoor();
        } else if (direction == Direction.DOWN && !downStops.isEmpty()) {
            currentFloor = downStops.pollFirst();
            openDoor();
        } else {
            // Switch direction or go idle
            if (!downStops.isEmpty()) direction = Direction.DOWN;
            else if (!upStops.isEmpty()) direction = Direction.UP;
            else direction = Direction.IDLE;
        }
    }
    
    void openDoor() { doorState = DoorState.OPEN; }
    void closeDoor() { doorState = DoorState.CLOSED; }
}

// Scheduling strategy (Strategy pattern)
interface ElevatorScheduler {
    Elevator selectElevator(List<Elevator> elevators, ElevatorRequest request);
}

// LOOK Algorithm: select nearest elevator moving in same direction
class LOOKScheduler implements ElevatorScheduler {
    public Elevator selectElevator(List<Elevator> elevators, ElevatorRequest req) {
        return elevators.stream()
            .filter(e -> e.direction == Direction.IDLE || e.direction == req.direction)
            .min(Comparator.comparingInt(e -> Math.abs(e.currentFloor - req.floor)))
            .orElse(elevators.get(0));  // fallback
    }
}

// Controller
class ElevatorController {
    List<Elevator> elevators;
    ElevatorScheduler scheduler;
    
    void handleRequest(ElevatorRequest request) {
        Elevator best = scheduler.selectElevator(elevators, request);
        best.addStop(request.floor);
    }
}
```

**Patterns used:** Strategy (scheduling), State (elevator states), Observer (floor sensors).

---

### Q12. Design a Splitwise / Expense Sharing System.

```java
class User {
    String id, name, email;
    Map<String, BigDecimal> balances;  // userId → amount owed (+) or owes (-)
}

class Expense {
    String id;
    String description;
    BigDecimal totalAmount;
    User paidBy;
    List<Split> splits;
    LocalDateTime createdAt;
}

// Split types (Strategy pattern)
interface Split {
    BigDecimal getAmount();
    User getUser();
}

class EqualSplit implements Split {
    User user;
    BigDecimal amount;  // totalAmount / numParticipants
}

class ExactSplit implements Split {
    User user;
    BigDecimal amount;  // exact amount specified
}

class PercentSplit implements Split {
    User user;
    double percent;     // % of total
    BigDecimal amount;  // calculated: total × percent / 100
}

// Service
class ExpenseService {
    
    void addExpense(Expense expense) {
        validate(expense);  // splits must sum to totalAmount
        User payer = expense.paidBy;
        
        for (Split split : expense.splits) {
            if (!split.getUser().equals(payer)) {
                // split.user OWES payer
                updateBalance(split.getUser(), payer, split.getAmount());
            }
        }
    }
    
    void updateBalance(User debtor, User creditor, BigDecimal amount) {
        // debtor owes creditor: debtor.balances[creditor] += amount
        debtor.balances.merge(creditor.id, amount, BigDecimal::add);
        creditor.balances.merge(debtor.id, amount.negate(), BigDecimal::add);
    }
    
    // Simplify debts: minimize number of transactions
    // A owes B $10, B owes C $10 → A pays C $10 directly
    List<Transaction> simplifyDebts(Group group) {
        // Net balance per user
        Map<String, BigDecimal> net = new HashMap<>();
        // ... calculate net balances
        
        // Greedy: match max creditor with max debtor
        PriorityQueue<Map.Entry<String, BigDecimal>> creditors = new PriorityQueue<>(/*max*/);
        PriorityQueue<Map.Entry<String, BigDecimal>> debtors = new PriorityQueue<>(/*min*/);
        
        List<Transaction> result = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var creditor = creditors.poll();
            var debtor = debtors.poll();
            BigDecimal settled = creditor.getValue().min(debtor.getValue().abs());
            result.add(new Transaction(debtor.getKey(), creditor.getKey(), settled));
            // Add remainders back to queues
        }
        return result;
    }
}
```

**Patterns:** Strategy (split types), Observer (balance update notifications).

---

### Q13. Design a BookMyShow / Movie Ticket Booking System.

```java
class Movie { String id, title; int duration; String genre; }
class Theater { String id, name; List<Screen> screens; Address address; }
class Screen { String id; int totalSeats; List<Seat> seats; }
class Seat { String id; SeatType type; int row, col; }  // REGULAR, PREMIUM, VIP

class Show {
    String id;
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    Map<String, SeatStatus> seatAvailability;  // seatId → AVAILABLE/LOCKED/BOOKED
    BigDecimal basePrice;
}

class Booking {
    String id;
    User user;
    Show show;
    List<Seat> seats;
    BigDecimal totalAmount;
    BookingStatus status;  // PENDING, CONFIRMED, CANCELLED
    LocalDateTime createdAt;
}

// Seat locking to prevent double booking (critical!)
class BookingService {
    private final RedisTemplate<String, String> redis;
    
    // Step 1: Lock seats temporarily (5 min timeout for payment)
    public BookingResult initiateBooking(User user, Show show, List<Seat> seats) {
        String lockKey = "lock:show:" + show.getId();
        
        // Atomic lock check + acquire using Redis
        for (Seat seat : seats) {
            Boolean locked = redis.opsForValue()
                .setIfAbsent(lockKey + ":" + seat.getId(), user.getId(), 
                             Duration.ofMinutes(5));
            if (!Boolean.TRUE.equals(locked)) {
                // Seat already locked by someone else — rollback
                releaseLocks(show, seats);
                throw new SeatUnavailableException("Seat " + seat.getId() + " taken");
            }
        }
        
        Booking booking = new Booking(user, show, seats, BookingStatus.PENDING);
        return new BookingResult(booking.getId(), "Complete payment in 5 minutes");
    }
    
    // Step 2: Confirm after payment
    public void confirmBooking(String bookingId, PaymentResult payment) {
        Booking booking = bookingRepo.findById(bookingId);
        if (payment.isSuccess()) {
            booking.setStatus(BookingStatus.CONFIRMED);
            // Update seat status to BOOKED in DB
            // Release Redis locks
            // Send confirmation email/SMS
        } else {
            releaseLocks(booking.getShow(), booking.getSeats());
            booking.setStatus(BookingStatus.CANCELLED);
        }
    }
}
```

**Key design decisions:**
- **Redis for seat locking** — distributed lock with TTL prevents double booking
- **5-minute payment window** — auto-release if payment not completed
- **Optimistic locking** in DB as backup — version column on seat status
- **Search:** ElasticSearch for movie/theater search (city, genre, language)
