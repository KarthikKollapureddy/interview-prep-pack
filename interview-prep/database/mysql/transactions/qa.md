# MySQL Transactions & Locking — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. ACID properties — explain each with examples.

| Property | Meaning | Example |
|----------|---------|---------|
| **Atomicity** | All or nothing | Transfer ₹500: debit + credit either both happen or neither |
| **Consistency** | Data goes from one valid state to another | Balance can't go negative (constraints enforced) |
| **Isolation** | Concurrent transactions don't interfere | Two people can't book the last seat simultaneously |
| **Durability** | Committed data survives crashes | Even if server crashes after COMMIT, data is safe (WAL) |

```sql
START TRANSACTION;
UPDATE accounts SET balance = balance - 500 WHERE id = 1;  -- Debit
UPDATE accounts SET balance = balance + 500 WHERE id = 2;  -- Credit
-- If any UPDATE fails → ROLLBACK (Atomicity)
COMMIT;  -- Only now is the transfer permanent (Durability)
```

---

### Q2. Isolation levels — what are they and what anomalies do they prevent?

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance |
|----------------|------------|--------------------|--------------|----|
| READ UNCOMMITTED | ✅ possible | ✅ possible | ✅ possible | Fastest |
| READ COMMITTED | ❌ prevented | ✅ possible | ✅ possible | Fast |
| REPEATABLE READ (MySQL default) | ❌ | ❌ | ❌* | Medium |
| SERIALIZABLE | ❌ | ❌ | ❌ | Slowest |

*MySQL's REPEATABLE READ uses gap locks to prevent phantoms too (InnoDB-specific).

**Anomalies explained:**
- **Dirty Read:** Reading uncommitted data from another transaction (may be rolled back)
- **Non-Repeatable Read:** Same query returns different data within one transaction (someone updated between reads)
- **Phantom Read:** Same range query returns different ROWS (someone inserted between reads)

```sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT balance FROM accounts WHERE id = 1; -- Returns 1000
-- Another transaction updates balance to 500 and commits
SELECT balance FROM accounts WHERE id = 1; -- Still returns 1000 (REPEATABLE READ!)
COMMIT;
```

---

### Q3. Locking mechanisms — row locks, table locks, gap locks.

```sql
-- Row lock (InnoDB default for UPDATE/DELETE)
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- Only locks row with id=1, other rows accessible

-- Table lock (explicit)
LOCK TABLES accounts WRITE;
-- No other session can read or write until UNLOCK TABLES

-- Gap lock (prevents phantom reads in REPEATABLE READ)
SELECT * FROM orders WHERE amount BETWEEN 100 AND 200 FOR UPDATE;
-- Locks the "gap" — prevents inserts of rows with amount 100-200

-- SELECT ... FOR UPDATE (pessimistic lock)
SELECT * FROM inventory WHERE product_id = 42 FOR UPDATE;
-- Other transactions BLOCK until this transaction commits/rolls back

-- SELECT ... FOR SHARE (shared lock)
SELECT * FROM accounts WHERE id = 1 FOR SHARE;
-- Others can read but not write until released
```

---

### Q4. Optimistic vs Pessimistic locking.

```java
// Pessimistic: Lock the row in DB — blocks other transactions
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Account findByIdForUpdate(@Param("id") Long id);

// Optimistic: Version check — no DB lock, detect conflicts at commit
@Entity
public class Account {
    @Version
    private Long version; // Auto-incremented by JPA on each update
}
// If two transactions read version=5, first commits (version→6), second gets OptimisticLockException
```

| | Pessimistic | Optimistic |
|---|---|---|
| Locking | DB row lock (blocks others) | Version column (no block) |
| Conflict detection | Prevention (locks upfront) | Detection (at commit time) |
| Use when | High contention (same row updated often) | Low contention (conflicts rare) |
| Risk | Deadlocks, reduced concurrency | Retries needed on conflict |

**At NPCI (payment processing):** Pessimistic for balance deductions (must prevent double-spend).  
**At Hatio (product catalog):** Optimistic for updating product details (low contention).

---

## Scenario-Based Questions

### Q5. At NPCI, how do you prevent double-spending in a payment system?

```sql
-- Pessimistic lock: SELECT ... FOR UPDATE
START TRANSACTION;

SELECT balance FROM accounts WHERE user_id = 'U001' FOR UPDATE;
-- Row is LOCKED — other transactions wait here

-- Check balance
-- If balance >= amount:
UPDATE accounts SET balance = balance - 500 WHERE user_id = 'U001';
INSERT INTO transactions (user_id, amount, type) VALUES ('U001', 500, 'DEBIT');

COMMIT;  -- Lock released
```

**Spring Data JPA:**
```java
@Transactional
public PaymentResult processPayment(String userId, BigDecimal amount) {
    Account account = accountRepo.findByUserIdForUpdate(userId); // FOR UPDATE
    
    if (account.getBalance().compareTo(amount) < 0) {
        throw new InsufficientFundsException();
    }
    
    account.setBalance(account.getBalance().subtract(amount));
    accountRepo.save(account);
    
    return PaymentResult.success();
}
```

---

### Q6. At Hatio, two users try to buy the last item in stock. How do you handle it?

```java
@Transactional
public OrderResult placeOrder(Long productId, int quantity) {
    // Pessimistic lock on inventory row
    Inventory inv = inventoryRepo.findByProductIdForUpdate(productId);
    
    if (inv.getQuantity() < quantity) {
        throw new OutOfStockException("Only " + inv.getQuantity() + " left");
    }
    
    inv.setQuantity(inv.getQuantity() - quantity);
    inventoryRepo.save(inv);
    
    Order order = new Order(productId, quantity);
    return OrderResult.success(orderRepo.save(order));
}
```

**Alternative (optimistic with retry):**
```java
@Retryable(value = OptimisticLockException.class, maxAttempts = 3)
@Transactional
public OrderResult placeOrder(Long productId, int quantity) {
    Inventory inv = inventoryRepo.findByProductId(productId); // No lock
    if (inv.getQuantity() < quantity) throw new OutOfStockException();
    inv.setQuantity(inv.getQuantity() - quantity);
    inventoryRepo.save(inv); // @Version check — throws if concurrent update
}
```

---

### Q7. Explain deadlocks — how do they happen and how to prevent them?

```
Transaction A:
  LOCK row 1 → waiting for row 2

Transaction B:
  LOCK row 2 → waiting for row 1

→ DEADLOCK! Neither can proceed.
```

**MySQL detects deadlocks** and automatically rolls back one transaction (the victim).

**Prevention:**
1. **Always lock in consistent order** — if you need rows 1 and 2, always lock 1 first
2. **Keep transactions short** — less time holding locks
3. **Use index** — without index, UPDATE locks entire table
4. **Use READ COMMITTED** — releases non-matching row locks early

```java
// ✅ Consistent ordering: always lock lower ID first
ids.sort();
for (Long id : ids) {
    accountRepo.findByIdForUpdate(id);
}
```

---

## Gotchas & Edge Cases

### Q8. @Transactional pitfalls in Spring.

```java
// ❌ Self-invocation: @Transactional ignored!
@Service
public class OrderService {
    public void process() {
        this.createOrder(); // Direct call bypasses proxy → no transaction!
    }
    
    @Transactional
    public void createOrder() { /* ... */ }
}

// ✅ Fix: inject self or extract to another service
@Autowired private OrderService self;
public void process() { self.createOrder(); } // Goes through proxy

// ❌ Catching exceptions silently
@Transactional
public void save() {
    try {
        repo.save(entity);
    } catch (Exception e) {
        log.error("Failed", e); // Transaction still commits! Spring didn't see the exception
    }
}

// ❌ Wrong exception type for rollback
@Transactional // Only rolls back on unchecked exceptions (RuntimeException) by default!
public void save() throws IOException { // Checked exception → NO rollback
}
// Fix: @Transactional(rollbackFor = Exception.class)
```

---

### Q9. Connection pool tuning — HikariCP.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20     # Max connections (start with CPU cores * 2)
      minimum-idle: 5           # Min idle connections
      connection-timeout: 30000 # 30s to get connection before error
      idle-timeout: 600000      # 10 min idle before eviction
      max-lifetime: 1800000     # 30 min max connection lifetime
      leak-detection-threshold: 60000  # Log warning if connection held > 60s
```

**Pool size formula:** `connections = (core_count * 2) + effective_spindle_count`  
For SSD: ~10-20 is usually optimal. **Too many connections** = context switching overhead.
