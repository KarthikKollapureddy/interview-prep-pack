# LLD — End-to-End Low Level Designs (Interview Deep Dives)

> **5 complete class-level designs** — each structured as a 45-min interview walkthrough  
> For each: Requirements → Class Diagram → Code → Design Patterns → Edge Cases → Extensions

---

## How to Use This File

```
For each design below:
1. First, try designing the classes yourself (pen & paper, 30 min)
2. Then compare with the solution
3. Focus on: SOLID principles, right patterns, extensibility
4. Practice explaining your class choices out loud

Interview approach (45 min):
  ├── 5 min  → Clarify requirements, scope, entities
  ├── 5 min  → Identify core classes and relationships
  ├── 20 min → Design classes + key methods + interfaces
  ├── 10 min → Handle concurrency, edge cases
  └── 5 min  → Discuss extensibility, patterns used
```

---

---

# Design 1: Parking Lot System

## Why This Is Asked
Tests: OOP fundamentals, Strategy pattern, state management, enums, multi-threading. Asked at: Amazon, Google, Flipkart, every product company. Often the FIRST LLD question.

---

## Step 1: Requirements

**Functional:**
- Multi-floor parking lot with different spot sizes (compact, regular, large)
- Vehicle types: motorcycle, car, truck (motorcycle fits compact, car fits regular, truck needs large)
- Entry/exit panels with ticket dispensing
- Hourly-rate billing (different per vehicle type)
- Real-time spot availability display per floor

**Scope boundaries (ask interviewer):**
- Single parking lot? (Yes)
- Multiple entry/exit gates? (Yes, 2 each)
- Reserved/handicap spots? (Skip for now)
- Electric vehicle charging? (Extension)

---

## Step 2: Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       ParkingLot (Singleton)                 │
│  - id, name, address                                        │
│  - floors: List<ParkingFloor>                               │
│  - entryPanels: List<EntryPanel>                            │
│  - exitPanels: List<ExitPanel>                              │
│  - activeTickets: Map<String, ParkingTicket>                │
│  + getAvailableSpot(VehicleType): ParkingSpot               │
│  + issueTicket(Vehicle): ParkingTicket                      │
│  + processExit(ParkingTicket): Payment                      │
└─────────────────────────────────────────────────────────────┘
         │
         │ has many
         ▼
┌─────────────────────────────────────────────┐
│            ParkingFloor                      │
│  - floorNumber: int                         │
│  - spots: Map<SpotType, List<ParkingSpot>>  │
│  + getAvailableSpot(VehicleType): ParkingSpot│
│  + getAvailableCount(SpotType): int          │
└─────────────────────────────────────────────┘
         │
         │ has many
         ▼
┌─────────────────────────────────────┐
│          ParkingSpot                 │
│  - id: String                       │
│  - floor: int                       │
│  - type: SpotType                   │
│  - vehicle: Vehicle (nullable)      │
│  - isAvailable: boolean             │
│  + park(Vehicle): boolean           │
│  + unpark(): Vehicle                │
│  + canFit(VehicleType): boolean     │
└─────────────────────────────────────┘

┌──────────────────────────┐     ┌──────────────────────────┐
│    Vehicle (abstract)     │     │     SpotType (enum)      │
│  - licensePlate: String   │     │  COMPACT, REGULAR, LARGE │
│  - type: VehicleType      │     └──────────────────────────┘
├───────────────────────────┤
│    Car extends Vehicle    │     ┌──────────────────────────┐
│    Truck extends Vehicle  │     │   VehicleType (enum)     │
│    Motorcycle extends V.  │     │  MOTORCYCLE, CAR, TRUCK  │
└───────────────────────────┘     └──────────────────────────┘

┌────────────────────────────┐    ┌────────────────────────────┐
│      ParkingTicket          │    │    PricingStrategy          │
│  - ticketId: String         │    │    (interface)              │
│  - vehicle: Vehicle         │    │  + calculate(ticket): Money │
│  - spot: ParkingSpot        │    ├────────────────────────────┤
│  - entryTime: LocalDateTime │    │  HourlyPricing implements  │
│  - exitTime: LocalDateTime  │    │  FlatRatePricing implements│
│  - status: TicketStatus     │    │  WeekendPricing implements │
└────────────────────────────┘    └────────────────────────────┘
```

---

## Step 3: Full Implementation

```java
// ==================== ENUMS ====================

enum VehicleType { MOTORCYCLE, CAR, TRUCK }
enum SpotType { COMPACT, REGULAR, LARGE }
enum TicketStatus { ACTIVE, PAID, LOST }

// Mapping: which spot type can fit which vehicle
// Motorcycle → COMPACT, REGULAR, LARGE (can fit in any)
// Car → REGULAR, LARGE
// Truck → LARGE only

// ==================== VEHICLE ====================

abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;
    
    Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
    
    // getters...
}

class Car extends Vehicle {
    Car(String plate) { super(plate, VehicleType.CAR); }
}

class Truck extends Vehicle {
    Truck(String plate) { super(plate, VehicleType.TRUCK); }
}

class Motorcycle extends Vehicle {
    Motorcycle(String plate) { super(plate, VehicleType.MOTORCYCLE); }
}

// ==================== PARKING SPOT ====================

class ParkingSpot {
    private final String id;
    private final int floor;
    private final SpotType type;
    private Vehicle vehicle;
    private volatile boolean available = true;
    
    ParkingSpot(String id, int floor, SpotType type) {
        this.id = id;
        this.floor = floor;
        this.type = type;
    }
    
    boolean canFit(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE -> true;  // fits anywhere
            case CAR -> type == SpotType.REGULAR || type == SpotType.LARGE;
            case TRUCK -> type == SpotType.LARGE;
        };
    }
    
    // Thread-safe: synchronized to prevent two cars parking in same spot
    synchronized boolean park(Vehicle v) {
        if (!available || !canFit(v.getType())) return false;
        this.vehicle = v;
        this.available = false;
        return true;
    }
    
    synchronized Vehicle unpark() {
        Vehicle v = this.vehicle;
        this.vehicle = null;
        this.available = true;
        return v;
    }
    
    // getters...
}

// ==================== PARKING FLOOR ====================

class ParkingFloor {
    private final int floorNumber;
    private final Map<SpotType, List<ParkingSpot>> spotsByType;
    
    ParkingFloor(int floorNumber, int compactCount, int regularCount, int largeCount) {
        this.floorNumber = floorNumber;
        this.spotsByType = new EnumMap<>(SpotType.class);
        
        spotsByType.put(SpotType.COMPACT, createSpots(floorNumber, SpotType.COMPACT, compactCount));
        spotsByType.put(SpotType.REGULAR, createSpots(floorNumber, SpotType.REGULAR, regularCount));
        spotsByType.put(SpotType.LARGE, createSpots(floorNumber, SpotType.LARGE, largeCount));
    }
    
    private List<ParkingSpot> createSpots(int floor, SpotType type, int count) {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            spots.add(new ParkingSpot(floor + "-" + type.name().charAt(0) + i, floor, type));
        }
        return spots;
    }
    
    // Find first available spot that fits the vehicle
    Optional<ParkingSpot> getAvailableSpot(VehicleType vehicleType) {
        // Try spots in order: smallest fitting → largest (efficient use of space)
        for (SpotType spotType : SpotType.values()) {
            List<ParkingSpot> spots = spotsByType.getOrDefault(spotType, List.of());
            Optional<ParkingSpot> spot = spots.stream()
                .filter(s -> s.isAvailable() && s.canFit(vehicleType))
                .findFirst();
            if (spot.isPresent()) return spot;
        }
        return Optional.empty();
    }
    
    int getAvailableCount(SpotType type) {
        return (int) spotsByType.getOrDefault(type, List.of()).stream()
            .filter(ParkingSpot::isAvailable).count();
    }
}

// ==================== PRICING (Strategy Pattern) ====================

interface PricingStrategy {
    BigDecimal calculate(ParkingTicket ticket);
}

class HourlyPricing implements PricingStrategy {
    private static final Map<VehicleType, BigDecimal> RATES = Map.of(
        VehicleType.MOTORCYCLE, new BigDecimal("20"),   // ₹20/hr
        VehicleType.CAR, new BigDecimal("40"),           // ₹40/hr
        VehicleType.TRUCK, new BigDecimal("60")          // ₹60/hr
    );
    
    @Override
    public BigDecimal calculate(ParkingTicket ticket) {
        long hours = Math.max(1, 
            java.time.Duration.between(ticket.getEntryTime(), ticket.getExitTime()).toHours());
        BigDecimal rate = RATES.get(ticket.getVehicle().getType());
        return rate.multiply(BigDecimal.valueOf(hours));
    }
}

class WeekendPricing implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.5");
    
    WeekendPricing(PricingStrategy base) { this.baseStrategy = base; }
    
    @Override
    public BigDecimal calculate(ParkingTicket ticket) {
        BigDecimal base = baseStrategy.calculate(ticket);
        DayOfWeek day = ticket.getEntryTime().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return base.multiply(WEEKEND_MULTIPLIER);
        }
        return base;
    }
}

// ==================== PARKING TICKET ====================

class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status = TicketStatus.ACTIVE;
    
    ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = LocalDateTime.now();
    }
    
    void markExit() {
        this.exitTime = LocalDateTime.now();
        this.status = TicketStatus.PAID;
    }
    
    // getters...
}

// ==================== PARKING LOT (Singleton) ====================

class ParkingLot {
    private static ParkingLot instance;
    
    private final String name;
    private final List<ParkingFloor> floors;
    private final Map<String, ParkingTicket> activeTickets = new ConcurrentHashMap<>();
    private PricingStrategy pricingStrategy;
    
    private ParkingLot(String name, List<ParkingFloor> floors, PricingStrategy pricing) {
        this.name = name;
        this.floors = floors;
        this.pricingStrategy = pricing;
    }
    
    static synchronized ParkingLot getInstance(String name, List<ParkingFloor> floors, 
                                                PricingStrategy pricing) {
        if (instance == null) {
            instance = new ParkingLot(name, floors, pricing);
        }
        return instance;
    }
    
    // Issue ticket: find spot + park + create ticket
    ParkingTicket issueTicket(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            Optional<ParkingSpot> spot = floor.getAvailableSpot(vehicle.getType());
            if (spot.isPresent() && spot.get().park(vehicle)) {
                ParkingTicket ticket = new ParkingTicket(vehicle, spot.get());
                activeTickets.put(ticket.getTicketId(), ticket);
                return ticket;
            }
        }
        throw new ParkingFullException("No available spots for " + vehicle.getType());
    }
    
    // Process exit: calculate fee + unpark + close ticket
    BigDecimal processExit(String ticketId) {
        ParkingTicket ticket = activeTickets.remove(ticketId);
        if (ticket == null) throw new InvalidTicketException("Ticket not found: " + ticketId);
        
        ticket.markExit();
        ticket.getSpot().unpark();
        return pricingStrategy.calculate(ticket);
    }
    
    // Display board: available spots per floor per type
    void displayAvailability() {
        for (ParkingFloor floor : floors) {
            System.out.printf("Floor %d → Compact: %d | Regular: %d | Large: %d%n",
                floor.getFloorNumber(),
                floor.getAvailableCount(SpotType.COMPACT),
                floor.getAvailableCount(SpotType.REGULAR),
                floor.getAvailableCount(SpotType.LARGE));
        }
    }
}
```

---

## Patterns Used
| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | ParkingLot | One parking lot instance |
| **Strategy** | PricingStrategy | Swap pricing without changing lot logic |
| **Decorator** | WeekendPricing wraps HourlyPricing | Layer pricing rules |
| **Factory** | Vehicle creation | Decouple vehicle type instantiation |
| **Template Method** | ParkingSpot.canFit() | Each spot type has different fitting rules |

---

## Edge Cases & Extensions
- **Concurrent parking:** Two cars try same spot → `synchronized` on ParkingSpot.park()
- **Lost ticket:** Charge maximum daily rate
- **EV charging spots:** Extend SpotType enum, add ChargingParkingSpot subclass
- **Valet parking:** Priority queue for valet-assigned spots
- **Monthly pass:** Decorator on PricingStrategy that checks pass validity

---

---

# Design 2: Library Management System

## Why This Is Asked
Tests: entity relationships, state machines, Observer pattern, concurrency (hold/reservation race conditions). Asked at: Amazon, Microsoft, Infosys, TCS (product rounds).

---

## Step 1: Requirements

**Functional:**
- Add/remove books, search by title/author/ISBN/category
- Members can borrow (max 5 books), return, reserve books
- Fine calculation for late returns (₹10/day)
- Book can have multiple copies
- Librarian can manage catalog and members
- Notification when reserved book becomes available

**Scope:**
- No online reading (physical library only)
- No inter-library loan

---

## Step 2: Class Diagram

```
┌──────────────────────────────────────────────────────┐
│                   Library (Singleton)                  │
│  - books: Map<String, BookItem>                       │
│  - members: Map<String, Member>                       │
│  + searchByTitle(title): List<Book>                   │
│  + searchByAuthor(author): List<Book>                 │
│  + issueBook(memberId, bookItemId): BorrowRecord      │
│  + returnBook(memberId, bookItemId): ReturnResult     │
└──────────────────────────────────────────────────────┘

┌──────────────────────────┐     ┌──────────────────────────┐
│       Book               │     │      BookItem             │
│  - isbn: String          │     │  - barcode: String        │
│  - title: String         │     │  - book: Book (parent)    │
│  - author: String        │     │  - status: BookStatus     │
│  - category: Category    │     │  - rack: String           │
│  - items: List<BookItem> │     │  - dueDate: LocalDate     │
│  + getAvailableCopies()  │     │  - borrowedBy: Member     │
└──────────────────────────┘     └──────────────────────────┘
                                 BookStatus: AVAILABLE,
                                 BORROWED, RESERVED, LOST

┌──────────────────────────────────┐
│      Member                       │
│  - id, name, email, phone         │
│  - borrowedBooks: List<BookItem>  │
│  - fineAmount: BigDecimal         │
│  - maxBooks: int (default 5)      │
│  + canBorrow(): boolean           │
│  + addBook(BookItem): void        │
│  + removeBook(BookItem): void     │
└──────────────────────────────────┘

┌────────────────────────────────┐  ┌────────────────────────────────┐
│  BorrowRecord                   │  │  Reservation                   │
│  - member: Member               │  │  - member: Member              │
│  - bookItem: BookItem           │  │  - book: Book                  │
│  - borrowDate: LocalDate        │  │  - createdAt: LocalDateTime    │
│  - dueDate: LocalDate           │  │  - status: WAITING, FULFILLED, │
│  - returnDate: LocalDate        │  │           CANCELLED            │
└────────────────────────────────┘  └────────────────────────────────┘

┌────────────────────────────────────────┐
│  NotificationService (Observer)         │
│  + notifyBookAvailable(member, book)    │
│  + notifyDueSoon(member, bookItem)      │
│  + notifyFineCharged(member, amount)    │
└────────────────────────────────────────┘
```

---

## Step 3: Implementation

```java
// ==================== CORE ENTITIES ====================

enum BookStatus { AVAILABLE, BORROWED, RESERVED, LOST }
enum Category { FICTION, NON_FICTION, SCIENCE, HISTORY, TECHNOLOGY, BIOGRAPHY }

class Book {
    private final String isbn;
    private String title;
    private String author;
    private Category category;
    private final List<BookItem> items = new ArrayList<>();
    
    long getAvailableCopies() {
        return items.stream().filter(i -> i.getStatus() == BookStatus.AVAILABLE).count();
    }
}

class BookItem {
    private final String barcode;  // Unique per physical copy
    private final Book book;
    private BookStatus status = BookStatus.AVAILABLE;
    private String rackLocation;
    private LocalDate dueDate;
    private Member borrowedBy;
    
    synchronized boolean checkout(Member member) {
        if (status != BookStatus.AVAILABLE) return false;
        this.status = BookStatus.BORROWED;
        this.borrowedBy = member;
        this.dueDate = LocalDate.now().plusDays(14);  // 2-week loan period
        return true;
    }
    
    synchronized void checkin() {
        this.status = BookStatus.AVAILABLE;
        this.borrowedBy = null;
        this.dueDate = null;
    }
}

class Member {
    private final String id;
    private String name, email;
    private final List<BookItem> borrowedBooks = new ArrayList<>();
    private BigDecimal fineAmount = BigDecimal.ZERO;
    private static final int MAX_BOOKS = 5;
    
    boolean canBorrow() {
        return borrowedBooks.size() < MAX_BOOKS && fineAmount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    void addBorrowedBook(BookItem item) {
        if (!canBorrow()) throw new BorrowLimitException("Cannot borrow more books");
        borrowedBooks.add(item);
    }
    
    void removeBorrowedBook(BookItem item) { borrowedBooks.remove(item); }
    void addFine(BigDecimal amount) { fineAmount = fineAmount.add(amount); }
    void payFine(BigDecimal amount) { fineAmount = fineAmount.subtract(amount); }
}

// ==================== SERVICES ====================

class LibraryService {
    private final Map<String, Book> booksByIsbn = new ConcurrentHashMap<>();
    private final Map<String, BookItem> bookItemsByBarcode = new ConcurrentHashMap<>();
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final Map<String, Queue<Reservation>> reservations = new ConcurrentHashMap<>();
    private final NotificationService notifier;
    private final FineCalculator fineCalculator;
    
    // ---- SEARCH (multiple strategies) ----
    
    List<Book> searchByTitle(String title) {
        return booksByIsbn.values().stream()
            .filter(b -> b.getTitle().toLowerCase().contains(title.toLowerCase()))
            .toList();
    }
    
    List<Book> searchByAuthor(String author) {
        return booksByIsbn.values().stream()
            .filter(b -> b.getAuthor().toLowerCase().contains(author.toLowerCase()))
            .toList();
    }
    
    Book searchByIsbn(String isbn) { return booksByIsbn.get(isbn); }
    
    // ---- BORROW ----
    
    BorrowRecord issueBook(String memberId, String barcode) {
        Member member = members.get(memberId);
        if (member == null) throw new MemberNotFoundException(memberId);
        if (!member.canBorrow()) throw new BorrowLimitException("Cannot borrow");
        
        BookItem item = bookItemsByBarcode.get(barcode);
        if (item == null) throw new BookNotFoundException(barcode);
        
        if (!item.checkout(member)) {
            throw new BookNotAvailableException("Book already borrowed/reserved");
        }
        
        member.addBorrowedBook(item);
        return new BorrowRecord(member, item, LocalDate.now(), item.getDueDate());
    }
    
    // ---- RETURN ----
    
    ReturnResult returnBook(String memberId, String barcode) {
        Member member = members.get(memberId);
        BookItem item = bookItemsByBarcode.get(barcode);
        
        // Calculate fine if overdue
        BigDecimal fine = fineCalculator.calculate(item.getDueDate(), LocalDate.now());
        if (fine.compareTo(BigDecimal.ZERO) > 0) {
            member.addFine(fine);
            notifier.notifyFineCharged(member, fine);
        }
        
        item.checkin();
        member.removeBorrowedBook(item);
        
        // Check if anyone is waiting for this book (reservation)
        fulfillReservation(item.getBook());
        
        return new ReturnResult(fine, item);
    }
    
    // ---- RESERVATION (Observer-like) ----
    
    void reserveBook(String memberId, String isbn) {
        Member member = members.get(memberId);
        Book book = booksByIsbn.get(isbn);
        
        if (book.getAvailableCopies() > 0) {
            throw new IllegalStateException("Book is available — borrow directly");
        }
        
        reservations.computeIfAbsent(isbn, k -> new LinkedList<>())
            .add(new Reservation(member, book));
    }
    
    private void fulfillReservation(Book book) {
        Queue<Reservation> queue = reservations.get(book.getIsbn());
        if (queue != null && !queue.isEmpty()) {
            Reservation next = queue.poll();
            next.setStatus(ReservationStatus.FULFILLED);
            notifier.notifyBookAvailable(next.getMember(), book);
            // Book is held for 3 days for the member
        }
    }
}

// ==================== FINE CALCULATOR ====================

class FineCalculator {
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("10"); // ₹10/day
    
    BigDecimal calculate(LocalDate dueDate, LocalDate returnDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (overdueDays <= 0) return BigDecimal.ZERO;
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
    }
}

// ==================== NOTIFICATION (Observer Pattern) ====================

interface LibraryObserver {
    void onBookAvailable(Member member, Book book);
    void onDueDateApproaching(Member member, BookItem item);
}

class EmailNotifier implements LibraryObserver {
    @Override
    public void onBookAvailable(Member member, Book book) {
        System.out.printf("EMAIL to %s: '%s' is now available!%n", member.getEmail(), book.getTitle());
    }
    
    @Override
    public void onDueDateApproaching(Member member, BookItem item) {
        System.out.printf("EMAIL to %s: '%s' is due on %s%n", 
            member.getEmail(), item.getBook().getTitle(), item.getDueDate());
    }
}

class SMSNotifier implements LibraryObserver {
    // Similar implementation for SMS channel
}
```

---

## Patterns Used
| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | Library | One library system |
| **Observer** | Notifications on book available | Decouple notification logic from library logic |
| **Strategy** | FineCalculator (could have weekend/holiday rules) | Swap fine rules without changing core |
| **State** | BookStatus transitions | AVAILABLE → BORROWED → AVAILABLE |

---

---

# Design 3: Snake and Ladder Game

## Why This Is Asked
Tests: clean OOP modeling, state machines, randomness handling, game loop design. Asked at: Flipkart, Amazon, Goldman Sachs. Fun question that reveals design thinking.

---

## Step 1: Requirements

**Functional:**
- Board: 100 cells (10×10 grid)
- N players take turns rolling a dice
- Snakes: land on head → slide to tail (go backward)
- Ladders: land on bottom → climb to top (go forward)
- First player to reach exactly cell 100 wins
- If dice roll would go beyond 100, player stays in place

**Scope:**
- 2-4 players
- Single 6-sided dice
- Configurable snake/ladder positions

---

## Step 2: Class Diagram

```
┌──────────────────────────────┐
│        Game                   │
│  - board: Board              │
│  - players: Queue<Player>    │
│  - dice: Dice                │
│  - winner: Player            │
│  - status: GameStatus        │
│  + start(): void             │
│  + playTurn(): TurnResult    │
└──────────────────────────────┘
         │
    ┌────┴────┬──────────────┐
    ▼         ▼              ▼
┌────────┐ ┌──────────┐ ┌──────────┐
│ Board  │ │  Player  │ │  Dice    │
│ - size │ │ - name   │ │ - faces  │
│ - cells│ │ - position│ │ + roll() │
└───┬────┘ └──────────┘ └──────────┘
    │
    ▼
┌─────────────────────────┐
│   Cell                   │
│  - position: int         │
│  - jump: Jump (nullable) │
└─────────────────────────┘
    │
    ▼
┌─────────────────────────┐
│  Jump (Snake or Ladder)  │
│  - start: int            │
│  - end: int              │
│  + isSnake(): boolean    │
│  + isLadder(): boolean   │
└─────────────────────────┘
```

---

## Step 3: Implementation

```java
// ==================== DICE ====================

class Dice {
    private final int faces;
    private final Random random = new Random();
    
    Dice(int faces) { this.faces = faces; }
    Dice() { this(6); }  // default 6-sided
    
    int roll() { return random.nextInt(faces) + 1; }
}

// ==================== JUMP (Snake or Ladder) ====================

class Jump {
    private final int start;
    private final int end;
    
    Jump(int start, int end) {
        this.start = start;
        this.end = end;
    }
    
    boolean isSnake() { return end < start; }   // Goes down
    boolean isLadder() { return end > start; }  // Goes up
    
    @Override
    public String toString() {
        return (isSnake() ? "🐍 Snake" : "🪜 Ladder") + " " + start + " → " + end;
    }
    
    // getters...
}

// ==================== BOARD ====================

class Board {
    private final int size;
    private final Map<Integer, Jump> jumps;  // position → Jump
    
    Board(int size, List<Jump> snakesAndLadders) {
        this.size = size;
        this.jumps = new HashMap<>();
        for (Jump j : snakesAndLadders) {
            // Validate: no snake/ladder at start(1) or end(100)
            if (j.getStart() == 1 || j.getStart() == size) {
                throw new IllegalArgumentException("Cannot place jump at start/end");
            }
            // Validate: no overlap (two jumps at same position)
            if (jumps.containsKey(j.getStart())) {
                throw new IllegalArgumentException("Duplicate jump at position " + j.getStart());
            }
            jumps.put(j.getStart(), j);
        }
    }
    
    int getSize() { return size; }
    
    // Get final position after applying any snake/ladder
    int getFinalPosition(int position) {
        Jump jump = jumps.get(position);
        if (jump != null) {
            System.out.println("    " + jump);
            return jump.getEnd();
        }
        return position;
    }
}

// ==================== PLAYER ====================

class Player {
    private final String name;
    private int position = 0;  // 0 = not on board yet, 1 = start
    
    Player(String name) { this.name = name; }
    
    void moveTo(int position) { this.position = position; }
    String getName() { return name; }
    int getPosition() { return position; }
}

// ==================== GAME ====================

enum GameStatus { NOT_STARTED, IN_PROGRESS, FINISHED }

class Game {
    private final Board board;
    private final Dice dice;
    private final Queue<Player> players;
    private Player winner;
    private GameStatus status = GameStatus.NOT_STARTED;
    
    Game(Board board, Dice dice, List<Player> playerList) {
        this.board = board;
        this.dice = dice;
        this.players = new LinkedList<>(playerList);
    }
    
    // Main game loop
    void start() {
        status = GameStatus.IN_PROGRESS;
        System.out.println("🎮 Game started with " + players.size() + " players!\n");
        
        while (status != GameStatus.FINISHED) {
            playTurn();
        }
        
        System.out.println("\n🏆 " + winner.getName() + " WINS!");
    }
    
    void playTurn() {
        Player current = players.poll();  // Take current player from front
        
        int diceValue = dice.roll();
        int oldPosition = current.getPosition();
        int newPosition = oldPosition + diceValue;
        
        System.out.printf("%s rolls %d: %d → ", current.getName(), diceValue, oldPosition);
        
        // Rule: if roll goes beyond board size, stay in place
        if (newPosition > board.getSize()) {
            System.out.println(oldPosition + " (stays, would exceed " + board.getSize() + ")");
            players.offer(current);  // Back to queue
            return;
        }
        
        // Check for snake or ladder at new position
        int finalPosition = board.getFinalPosition(newPosition);
        current.moveTo(finalPosition);
        
        System.out.println(finalPosition);
        
        // Check for win
        if (finalPosition == board.getSize()) {
            winner = current;
            status = GameStatus.FINISHED;
            return;
        }
        
        players.offer(current);  // Back to queue for next turn
    }
}

// ==================== GAME SETUP & MAIN ====================

class SnakeAndLadderApp {
    public static void main(String[] args) {
        // Configure snakes and ladders
        List<Jump> jumps = List.of(
            // Ladders (go up)
            new Jump(2, 38),
            new Jump(7, 14),
            new Jump(8, 31),
            new Jump(15, 26),
            new Jump(28, 84),
            new Jump(36, 44),
            new Jump(51, 67),
            new Jump(71, 91),
            new Jump(78, 98),
            // Snakes (go down)
            new Jump(16, 6),
            new Jump(46, 25),
            new Jump(49, 11),
            new Jump(62, 19),
            new Jump(64, 60),
            new Jump(74, 53),
            new Jump(89, 68),
            new Jump(92, 88),
            new Jump(95, 75),
            new Jump(99, 80)
        );
        
        Board board = new Board(100, jumps);
        Dice dice = new Dice(6);
        List<Player> players = List.of(
            new Player("Alice"),
            new Player("Bob"),
            new Player("Charlie")
        );
        
        Game game = new Game(board, dice, players);
        game.start();
    }
}
```

---

## Patterns Used
| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | Dice (could have different dice types) | Swap 6-sided for 8-sided |
| **State** | GameStatus | Clean state transitions |
| **Composition** | Board has Cells, Cells have Jumps | Flexible configuration |

## Extensions
- **Multiple dice:** Roll 2 dice, special rules for doubles
- **Power-ups:** Land on special cell → skip opponent's turn
- **Undo:** Stack-based undo for last move
- **Multiplayer online:** WebSocket + turn manager service

---

---

# Design 4: Hotel Booking System (OYO / MakeMyTrip)

## Why This Is Asked
Tests: inventory management (rooms are like limited stock), booking conflicts, payment integration, search with filters. Asked at: OYO, MakeMyTrip, Booking.com, Amazon.

---

## Step 1: Requirements

**Functional:**
- Search hotels by city, date range, guests, room type
- View hotel details, room types, prices, photos
- Book room(s) with date range
- Cancel booking (with cancellation policy)
- Payment: online prepaid or pay-at-hotel
- Admin: add hotels, rooms, manage pricing

**Key constraint:** Room can't be double-booked for overlapping dates.

---

## Step 2: Class Diagram

```
┌────────────────────────────────┐
│           Hotel                 │
│  - id, name, address, city     │
│  - rating: double              │
│  - amenities: List<Amenity>    │
│  - rooms: List<Room>           │
│  + getAvailableRooms(checkin,  │
│    checkout, type): List<Room> │
└────────────────────────────────┘
         │ has many
         ▼
┌────────────────────────────────┐
│            Room                 │
│  - id: String                  │
│  - hotel: Hotel                │
│  - type: RoomType              │
│  - pricePerNight: BigDecimal   │
│  - maxGuests: int              │
│  - floor: int                  │
│  + isAvailable(checkin,        │
│    checkout): boolean          │
└────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│           Booking                       │
│  - id: String                          │
│  - guest: Guest                        │
│  - room: Room                          │
│  - checkIn, checkOut: LocalDate        │
│  - totalAmount: BigDecimal             │
│  - status: BookingStatus               │
│  - payment: Payment                    │
│  - cancellationPolicy: CancelPolicy   │
└────────────────────────────────────────┘

RoomType: SINGLE, DOUBLE, DELUXE, SUITE
BookingStatus: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
```

---

## Step 3: Implementation

```java
// ==================== ENUMS ====================

enum RoomType { SINGLE, DOUBLE, DELUXE, SUITE }
enum BookingStatus { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

// ==================== ENTITIES ====================

class Hotel {
    private final String id;
    private String name, city;
    private Address address;
    private double rating;
    private List<String> amenities;
    private final List<Room> rooms = new ArrayList<>();
    
    List<Room> getAvailableRooms(LocalDate checkIn, LocalDate checkOut, RoomType type) {
        return rooms.stream()
            .filter(r -> r.getType() == type)
            .filter(r -> r.isAvailable(checkIn, checkOut))
            .toList();
    }
}

class Room {
    private final String id;
    private final Hotel hotel;
    private final RoomType type;
    private BigDecimal pricePerNight;
    private int maxGuests;
    private final List<Booking> bookings = new ArrayList<>();  // All bookings for this room
    
    // Check if room is available for given date range
    // No overlapping confirmed bookings should exist
    synchronized boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        return bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED 
                      || b.getStatus() == BookingStatus.CHECKED_IN)
            .noneMatch(b -> datesOverlap(checkIn, checkOut, b.getCheckIn(), b.getCheckOut()));
    }
    
    // Two date ranges overlap if: start1 < end2 AND start2 < end1
    private boolean datesOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
    
    synchronized void addBooking(Booking booking) { bookings.add(booking); }
}

class Guest {
    private final String id;
    private String name, email, phone;
    private List<Booking> bookingHistory = new ArrayList<>();
}

// ==================== BOOKING ====================

class Booking {
    private final String id;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private final CancellationPolicy cancellationPolicy;
    
    Booking(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut,
            CancellationPolicy policy) {
        this.id = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.cancellationPolicy = policy;
        this.status = BookingStatus.PENDING;
        this.totalAmount = calculateTotal();
    }
    
    private BigDecimal calculateTotal() {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }
    
    void confirm() { this.status = BookingStatus.CONFIRMED; }
    void checkIn() { this.status = BookingStatus.CHECKED_IN; }
    void checkOut() { this.status = BookingStatus.CHECKED_OUT; }
    
    // Cancel with refund calculation
    BigDecimal cancel() {
        this.status = BookingStatus.CANCELLED;
        return cancellationPolicy.calculateRefund(this);
    }
}

// ==================== CANCELLATION POLICY (Strategy) ====================

interface CancellationPolicy {
    BigDecimal calculateRefund(Booking booking);
}

class FreeCancellation implements CancellationPolicy {
    // Full refund if cancelled 24+ hours before check-in
    @Override
    public BigDecimal calculateRefund(Booking booking) {
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(
            LocalDateTime.now(), booking.getCheckIn().atStartOfDay());
        if (hoursUntilCheckIn >= 24) {
            return booking.getTotalAmount();  // 100% refund
        }
        return booking.getTotalAmount().multiply(new BigDecimal("0.5"));  // 50% refund
    }
}

class NonRefundable implements CancellationPolicy {
    @Override
    public BigDecimal calculateRefund(Booking booking) {
        return BigDecimal.ZERO;  // No refund
    }
}

class FlexibleCancellation implements CancellationPolicy {
    // Tiered: 100% if > 7 days, 50% if > 1 day, 0% if < 1 day
    @Override
    public BigDecimal calculateRefund(Booking booking) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckIn());
        if (daysUntil > 7) return booking.getTotalAmount();
        if (daysUntil > 1) return booking.getTotalAmount().multiply(new BigDecimal("0.5"));
        return BigDecimal.ZERO;
    }
}

// ==================== BOOKING SERVICE ====================

class BookingService {
    private final Map<String, Hotel> hotels = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    
    // Search hotels
    List<Hotel> searchHotels(String city, LocalDate checkIn, LocalDate checkOut,
                             RoomType type, int guests) {
        return hotels.values().stream()
            .filter(h -> h.getCity().equalsIgnoreCase(city))
            .filter(h -> !h.getAvailableRooms(checkIn, checkOut, type).isEmpty())
            .sorted(Comparator.comparingDouble(Hotel::getRating).reversed())
            .toList();
    }
    
    // Book a room — must be atomic to prevent double booking
    synchronized Booking createBooking(Guest guest, String roomId,
                                        LocalDate checkIn, LocalDate checkOut,
                                        CancellationPolicy policy) {
        Room room = findRoomById(roomId);
        
        // Double-check availability (inside synchronized block)
        if (!room.isAvailable(checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room " + roomId + " not available for dates");
        }
        
        Booking booking = new Booking(guest, room, checkIn, checkOut, policy);
        booking.confirm();
        room.addBooking(booking);
        bookings.put(booking.getId(), booking);
        
        return booking;
    }
    
    // Cancel booking
    BigDecimal cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) throw new BookingNotFoundException(bookingId);
        return booking.cancel();
    }
}
```

---

## Key Design Decisions

| Decision | Why |
|----------|-----|
| `synchronized` on createBooking | Prevents two guests booking same room for overlapping dates |
| Date overlap check | `s1 < e2 AND s2 < e1` — classic interval overlap formula |
| CancellationPolicy as Strategy | Hotels offer different policies; easily add new ones |
| Room has List<Booking> | Need to check all existing bookings for overlap |

---

## Extensions
- **Dynamic pricing:** Weekend surcharge, seasonal rates, demand-based pricing
- **Multi-room booking:** Atomic booking of multiple rooms
- **Waitlist:** If room unavailable, notify when cancellation happens
- **Reviews:** Observer pattern — post-checkout triggers review prompt
- **Loyalty points:** Decorator on payment — apply points discount

---

---

# Design 5: Vending Machine

## Why This Is Asked
Tests: State pattern (the textbook example), inventory management, payment handling, concurrent access. Asked at: Amazon, Google, Goldman Sachs. Clean, contained problem.

---

## Step 1: Requirements

**Functional:**
- Machine displays available products with prices
- User inserts coins/notes (₹1, ₹2, ₹5, ₹10)
- User selects product
- Machine dispenses product and returns change
- Refund: user can cancel and get money back
- Admin: restock products, collect cash

**Key design challenge:** State management — machine behaves differently based on state.

---

## Step 2: State Machine

```
                    ┌──────────────┐
            ┌──────→│    IDLE      │←──────────┐
            │       │ "Insert coin"│            │
            │       └──────┬───────┘            │
            │              │ insertCoin()       │
            │              ▼                    │
            │       ┌──────────────┐            │
            │       │ HAS_MONEY    │            │
        cancel()    │ "Select item"│───┐        │
            │       └──────┬───────┘   │        │
            │              │ select()  │ insertMore()
            │              ▼           │        │
            │       ┌──────────────┐   │        │
            │       │  DISPENSING  │───┘        │
            │       │ "Dispensing" │             │
            │       └──────┬───────┘            │
            │              │ dispense()         │
            └──────────────┘                    │
                    returnChange()──────────────┘
```

---

## Step 3: Implementation (State Pattern)

```java
// ==================== PRODUCT & INVENTORY ====================

class Product {
    private final String code;  // e.g., "A1", "B2"
    private final String name;
    private final BigDecimal price;
    
    Product(String code, String name, BigDecimal price) {
        this.code = code;
        this.name = name;
        this.price = price;
    }
    // getters...
}

class Inventory {
    private final Map<String, Product> products = new LinkedHashMap<>();  // code → product
    private final Map<String, Integer> stock = new ConcurrentHashMap<>(); // code → quantity
    
    void addProduct(Product product, int quantity) {
        products.put(product.getCode(), product);
        stock.merge(product.getCode(), quantity, Integer::sum);
    }
    
    boolean isAvailable(String code) {
        return stock.getOrDefault(code, 0) > 0;
    }
    
    Product getProduct(String code) { return products.get(code); }
    
    void reduceStock(String code) {
        stock.computeIfPresent(code, (k, v) -> v > 0 ? v - 1 : 0);
    }
    
    void displayProducts() {
        products.forEach((code, product) -> {
            int qty = stock.getOrDefault(code, 0);
            System.out.printf("  [%s] %s — ₹%s %s%n", code, product.getName(), 
                product.getPrice(), qty > 0 ? "(In Stock: " + qty + ")" : "(SOLD OUT)");
        });
    }
}

// ==================== STATE INTERFACE ====================

interface VendingMachineState {
    void insertCoin(VendingMachine machine, BigDecimal amount);
    void selectProduct(VendingMachine machine, String productCode);
    void dispense(VendingMachine machine);
    void cancel(VendingMachine machine);
}

// ==================== CONCRETE STATES ====================

class IdleState implements VendingMachineState {
    @Override
    public void insertCoin(VendingMachine machine, BigDecimal amount) {
        machine.addBalance(amount);
        System.out.println("Inserted ₹" + amount + ". Balance: ₹" + machine.getBalance());
        machine.setState(new HasMoneyState());
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Please insert coins first.");
    }
    
    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("Please insert coins and select a product.");
    }
    
    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Nothing to cancel.");
    }
}

class HasMoneyState implements VendingMachineState {
    @Override
    public void insertCoin(VendingMachine machine, BigDecimal amount) {
        machine.addBalance(amount);
        System.out.println("Inserted ₹" + amount + ". Balance: ₹" + machine.getBalance());
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String code) {
        Product product = machine.getInventory().getProduct(code);
        
        if (product == null) {
            System.out.println("Invalid product code: " + code);
            return;
        }
        if (!machine.getInventory().isAvailable(code)) {
            System.out.println(product.getName() + " is SOLD OUT. Pick another.");
            return;
        }
        if (machine.getBalance().compareTo(product.getPrice()) < 0) {
            BigDecimal shortfall = product.getPrice().subtract(machine.getBalance());
            System.out.println("Insufficient balance. Insert ₹" + shortfall + " more.");
            return;
        }
        
        machine.setSelectedProduct(product);
        machine.setState(new DispensingState());
        machine.dispense();  // Auto-trigger dispense
    }
    
    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("Please select a product first.");
    }
    
    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Refunding ₹" + machine.getBalance());
        machine.resetBalance();
        machine.setState(new IdleState());
    }
}

class DispensingState implements VendingMachineState {
    @Override
    public void insertCoin(VendingMachine machine, BigDecimal amount) {
        System.out.println("Please wait, dispensing in progress...");
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Please wait, dispensing in progress...");
    }
    
    @Override
    public void dispense(VendingMachine machine) {
        Product product = machine.getSelectedProduct();
        
        // Dispense product
        machine.getInventory().reduceStock(product.getCode());
        System.out.println("\n🎉 Dispensed: " + product.getName());
        
        // Calculate and return change
        BigDecimal change = machine.getBalance().subtract(product.getPrice());
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("💰 Change returned: ₹" + change);
        }
        
        // Reset machine
        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(new IdleState());
    }
    
    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Cannot cancel during dispensing.");
    }
}

// ==================== VENDING MACHINE (Context) ====================

class VendingMachine {
    private VendingMachineState state;
    private final Inventory inventory;
    private BigDecimal balance = BigDecimal.ZERO;
    private Product selectedProduct;
    
    VendingMachine() {
        this.inventory = new Inventory();
        this.state = new IdleState();
    }
    
    // Delegate all actions to current state
    public void insertCoin(BigDecimal amount)    { state.insertCoin(this, amount); }
    public void selectProduct(String code)       { state.selectProduct(this, code); }
    public void dispense()                       { state.dispense(this); }
    public void cancel()                         { state.cancel(this); }
    
    // State management (package-private, used by states)
    void setState(VendingMachineState newState) { this.state = newState; }
    void addBalance(BigDecimal amount) { balance = balance.add(amount); }
    void resetBalance() { balance = BigDecimal.ZERO; }
    
    BigDecimal getBalance() { return balance; }
    Inventory getInventory() { return inventory; }
    Product getSelectedProduct() { return selectedProduct; }
    void setSelectedProduct(Product p) { this.selectedProduct = p; }
    
    void displayMenu() {
        System.out.println("\n═══════════════════════════════");
        System.out.println("     VENDING MACHINE MENU      ");
        System.out.println("═══════════════════════════════");
        inventory.displayProducts();
        System.out.println("Balance: ₹" + balance);
        System.out.println("═══════════════════════════════\n");
    }
}

// ==================== DEMO ====================

class VendingMachineApp {
    public static void main(String[] args) {
        VendingMachine vm = new VendingMachine();
        
        // Stock the machine
        vm.getInventory().addProduct(new Product("A1", "Coca Cola", new BigDecimal("40")), 5);
        vm.getInventory().addProduct(new Product("A2", "Pepsi", new BigDecimal("35")), 3);
        vm.getInventory().addProduct(new Product("B1", "Lays Chips", new BigDecimal("20")), 10);
        vm.getInventory().addProduct(new Product("B2", "KitKat", new BigDecimal("30")), 0);  // Sold out!
        
        // Scenario 1: Successful purchase
        vm.displayMenu();
        vm.insertCoin(new BigDecimal("10"));
        vm.insertCoin(new BigDecimal("10"));
        vm.insertCoin(new BigDecimal("10"));
        vm.insertCoin(new BigDecimal("10"));
        vm.insertCoin(new BigDecimal("10"));   // Balance: ₹50
        vm.selectProduct("A1");                // ₹40 → dispense + ₹10 change
        
        // Scenario 2: Insufficient balance
        vm.insertCoin(new BigDecimal("10"));
        vm.selectProduct("A1");                // Need ₹30 more
        vm.cancel();                           // Refund ₹10
        
        // Scenario 3: Sold out
        vm.insertCoin(new BigDecimal("50"));
        vm.selectProduct("B2");                // SOLD OUT
        vm.selectProduct("B1");                // ₹20 → dispense + ₹30 change
        
        vm.displayMenu();
    }
}
```

---

## Patterns Used
| Pattern | Where | Why |
|---------|-------|-----|
| **State** | VendingMachineState | Machine behavior changes based on state — THE classic State pattern example |
| **Strategy** | Could add PaymentStrategy for different payment methods | Extensibility |
| **Singleton** | Could make VendingMachine singleton per location | One machine per physical unit |

---

## Edge Cases
- **Exact change only:** Machine can't make change → reject or display "exact change only"
- **Concurrent access:** Two users at same machine → not applicable (physical machine, one user at a time)
- **Power failure during dispensing:** Persist state to DB → recover on restart
- **Coin jam:** Transition to OUT_OF_SERVICE state

---

---

# Design 6 (Bonus): ATM Machine

## Quick Design — Extends Vending Machine Concepts

```java
// States: IDLE → CARD_INSERTED → PIN_VERIFIED → TRANSACTION_SELECTED → DISPENSING → IDLE

interface ATMState {
    void insertCard(ATM atm, Card card);
    void enterPin(ATM atm, String pin);
    void selectTransaction(ATM atm, TransactionType type);
    void enterAmount(ATM atm, BigDecimal amount);
    void cancel(ATM atm);
}

enum TransactionType { WITHDRAW, DEPOSIT, BALANCE_CHECK, MINI_STATEMENT }

class ATM {
    private ATMState state;
    private Card currentCard;
    private Account currentAccount;
    private final CashDispenser cashDispenser;
    private final BankService bankService;  // External bank API
    
    // Withdraw flow:
    // 1. insertCard → verify card (not expired, not blocked)
    // 2. enterPin → verify with bank (3 attempts, then block)
    // 3. selectTransaction(WITHDRAW) → enter amount
    // 4. enterAmount → bank debit → dispense cash → eject card
    
    // Key considerations:
    // - Idempotent bank transactions (prevent double debit)
    // - Cash dispenser denomination selection (₹2000, ₹500, ₹200, ₹100)
    //   → Greedy: largest denomination first
    // - Daily withdrawal limit check
    // - Handle: card stuck, cash jam, out of cash
}

// Cash denomination algorithm (Greedy)
class CashDispenser {
    private final Map<Integer, Integer> denominations;  // denomination → count
    // {2000: 50, 500: 100, 200: 200, 100: 500}
    
    List<Integer> dispense(int amount) {
        List<Integer> notes = new ArrayList<>();
        int remaining = amount;
        
        for (int denom : List.of(2000, 500, 200, 100)) {
            int available = denominations.getOrDefault(denom, 0);
            int needed = remaining / denom;
            int used = Math.min(needed, available);
            
            for (int i = 0; i < used; i++) notes.add(denom);
            remaining -= used * denom;
            denominations.put(denom, available - used);
        }
        
        if (remaining > 0) throw new InsufficientCashException("ATM cannot dispense ₹" + amount);
        return notes;
    }
}
```

---

---

# Quick Reference: Common LLD Patterns

| Pattern | When to Use | Example |
|---------|------------|---------|
| **Strategy** | Multiple algorithms, swap at runtime | Pricing, sorting, payment methods |
| **State** | Object behavior changes based on state | Vending machine, ATM, order status |
| **Observer** | One event → notify multiple listeners | Notifications, real-time updates |
| **Factory** | Object creation without specifying class | Vehicle types, notification channels |
| **Singleton** | Only one instance needed | ParkingLot, Library, config |
| **Builder** | Complex object with many optional fields | Booking, Order, Query objects |
| **Decorator** | Add behavior dynamically | Weekend pricing, logging, caching |
| **Command** | Encapsulate request as object | Undo/redo, queued operations |
| **Template Method** | Algorithm skeleton, subclass fills steps | Payment processing, report generation |

---

# LLD Interview Cheat Sheet

```
1. CLARIFY: What entities? What operations? What constraints?
2. ENTITIES: Identify nouns → classes. Identify verbs → methods.
3. RELATIONSHIPS: has-a (composition), is-a (inheritance), uses (dependency)
4. ENUMS: States, types, categories → always use enums, never strings
5. PATTERNS: Don't force patterns. Use only when they solve a real problem.
6. THREAD SAFETY: If shared state → synchronized / concurrent collections
7. EXTENSIBILITY: "What if we add a new vehicle type?" → shouldn't require modifying existing code (OCP)
8. EDGE CASES: Null inputs, concurrent access, boundary values, error states

Common mistakes:
  ✗ No encapsulation (public fields everywhere)
  ✗ God class (one class does everything)
  ✗ String-typed enums (status = "active" instead of enum)
  ✗ Forgetting thread safety on shared mutable state
  ✗ Over-engineering (using 5 patterns for a simple problem)
```
