# Hibernate / JPA — Interview Q&A

> 15 questions covering ORM fundamentals, entity lifecycle, caching, performance, and real-world scenarios  
> Priority: **P0** — Asked in 90%+ of Java fullstack interviews

---

## Conceptual Questions

### Q1. What is the difference between JPA and Hibernate?

**Answer:**
JPA (Java Persistence API) is a **specification** — it defines the standard API and annotations (`@Entity`, `@Table`, `@Id`, etc.) for ORM in Java. It does NOT provide an implementation.

Hibernate is the most popular **implementation** of JPA. It provides the actual engine that translates Java objects to SQL.

```
JPA = Interface / Contract
Hibernate = Concrete Implementation

Other JPA implementations: EclipseLink, OpenJPA
```

**Why this matters:** You can switch ORM providers without changing code if you code to JPA interfaces. In practice, most teams use Hibernate-specific features (like `@Formula`, `@NaturalId`, `Criteria API`).

**Interview tip:** Say "We use JPA annotations with Hibernate as the provider" — shows you understand the distinction.

---

### Q2. Explain the Hibernate Entity Lifecycle (Object States)

**Answer:**
Every entity in Hibernate exists in one of **4 states**:

```
┌──────────┐    persist()     ┌───────────┐
│ Transient │ ──────────────► │  Managed   │
│ (new)     │                 │ (Persistent│
└──────────┘                 └─────┬─────┘
                                   │
                          detach() / close()
                                   │
                                   ▼
                             ┌───────────┐    merge()    ┌───────────┐
                             │  Detached  │ ────────────► │  Managed   │
                             └───────────┘               └───────────┘
                                   │
                              remove()
                                   │
                                   ▼
                             ┌───────────┐
                             │  Removed   │
                             └───────────┘
```

| State | In Persistence Context? | In Database? | Description |
|-------|------------------------|-------------|-------------|
| **Transient** | No | No | Just created with `new`. Not tracked. |
| **Managed/Persistent** | Yes | Yes (or will be on flush) | Tracked by Session. Changes auto-synced. |
| **Detached** | No | Yes | Was managed, but Session closed. |
| **Removed** | Yes (marked for deletion) | Will be deleted on flush | Scheduled for removal. |

**Key concept — Dirty Checking:** When an entity is in **Managed** state, Hibernate automatically detects changes and generates UPDATE SQL at flush time — you don't need to call `save()` again.

```java
@Transactional
public void updateUser(Long id) {
    User user = entityManager.find(User.class, id);  // Managed state
    user.setName("New Name");  // Dirty checking will auto-UPDATE
    // No need to call save() or merge()!
}
```

---

### Q3. What is the N+1 Problem? How do you detect and fix it?

**Answer:**
The N+1 problem occurs when Hibernate executes **1 query** to fetch parent entities, then **N additional queries** to fetch associated child entities one by one.

**Example:**
```java
@Entity
public class Department {
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees;
}

// This triggers N+1:
List<Department> depts = repo.findAll();  // 1 query for departments
for (Department d : depts) {
    d.getEmployees().size();  // N queries — one per department!
}
```

**Detection:**
- Enable `spring.jpa.show-sql=true` or `hibernate.format_sql=true`
- Use **Hibernate Statistics** (`hibernate.generate_statistics=true`)
- Tools like **p6spy** or **datasource-proxy** for production

**Fixes (pick one):**

| Fix | Code | When to Use |
|-----|------|-------------|
| **JOIN FETCH (JPQL)** | `SELECT d FROM Department d JOIN FETCH d.employees` | When you always need children |
| **@EntityGraph** | `@EntityGraph(attributePaths = {"employees"})` on repo method | Cleaner alternative to JOIN FETCH |
| **@BatchSize** | `@BatchSize(size = 10)` on the collection | Fetch in batches of 10 instead of 1 |
| **@Fetch(FetchMode.SUBSELECT)** | On the collection mapping | Fetch all children in one subselect |
| **DTO Projection** | `SELECT new DeptDTO(d.name, e.name) FROM ...` | When you don't need full entities |

**Best practice:** Default to `FetchType.LAZY` everywhere, then use `JOIN FETCH` or `@EntityGraph` where needed.

---

### Q4. Lazy Loading vs Eager Loading — When to use which?

**Answer:**

| Aspect | Lazy (default for collections) | Eager (default for @ManyToOne) |
|--------|-------------------------------|-------------------------------|
| When loaded | On first access | Immediately with parent |
| Default for | `@OneToMany`, `@ManyToMany` | `@ManyToOne`, `@OneToOne` |
| Pros | Less initial data, faster queries | No LazyInitializationException |
| Cons | N+1 risk, proxy issues | Loads unnecessary data |

**Best practice for interviews:**
```java
// ALWAYS set explicit fetch types:
@ManyToOne(fetch = FetchType.LAZY)   // Override default EAGER
private Department department;

@OneToMany(fetch = FetchType.LAZY)   // Already default, be explicit
private List<Employee> employees;
```

**LazyInitializationException** — happens when you access a lazy field outside a transaction/session:
```java
User user = repo.findById(1L);   // Session open
// Session closes after method returns
user.getOrders().size();         // LazyInitializationException!
```

**Fixes:**
1. Use `@Transactional` to keep session open
2. Use `JOIN FETCH` in query
3. Use `@EntityGraph`
4. **DON'T** use `spring.jpa.open-in-view=true` (anti-pattern — holds DB connection for entire HTTP request)

---

### Q5. Explain Hibernate Caching (L1 and L2)

**Answer:**

```
┌──────────────────────────────────┐
│         Application              │
├──────────────────────────────────┤
│  Session 1    │    Session 2     │
│  ┌─────────┐  │   ┌─────────┐   │
│  │ L1 Cache│  │   │ L1 Cache│   │   ← Per-session, automatic
│  └─────────┘  │   └─────────┘   │
├───────────────┴─────────────────┤
│       Second Level (L2) Cache    │   ← Shared, optional (EhCache/Redis)
├──────────────────────────────────┤
│         Query Cache              │   ← Caches query results
├──────────────────────────────────┤
│           Database               │
└──────────────────────────────────┘
```

| Level | Scope | Enabled By Default? | Shared Across Sessions? |
|-------|-------|--------------------|-----------------------|
| **L1 (First Level)** | Session/EntityManager | Yes (always on) | No — per session |
| **L2 (Second Level)** | SessionFactory | No — must configure | Yes — all sessions share |
| **Query Cache** | SessionFactory | No — must configure | Yes |

**L1 Cache behavior:**
```java
// Within same session — only 1 SQL query:
User u1 = session.get(User.class, 1L);  // SQL fired
User u2 = session.get(User.class, 1L);  // Served from L1 cache
assert u1 == u2;  // true — same object reference!
```

**L2 Cache setup (Spring Boot):**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product { ... }
```

**When to use L2 cache:** Read-heavy data that rarely changes (product catalog, country list, config).

---

### Q6. What are the different relationship mappings? Explain with examples.

**Answer:**

| Mapping | Relationship | Example |
|---------|-------------|---------|
| `@OneToOne` | 1:1 | User ↔ UserProfile |
| `@OneToMany` / `@ManyToOne` | 1:N | Department → Employees |
| `@ManyToMany` | M:N | Students ↔ Courses |

**Bidirectional @OneToMany (most common):**
```java
@Entity
public class Department {
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();

    // Helper method to maintain both sides:
    public void addEmployee(Employee emp) {
        employees.add(emp);
        emp.setDepartment(this);
    }
}

@Entity
public class Employee {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;
}
```

**Key rules:**
- **`mappedBy`** goes on the non-owning side (the side that does NOT have the foreign key column)
- **Always define a helper method** to sync both sides of a bidirectional relationship
- **`orphanRemoval = true`** — deletes child when removed from collection
- **`cascade = CascadeType.ALL`** — propagates persist/remove to children

---

### Q7. What is `@Transactional` and how does it work internally?

**Answer:**
`@Transactional` is a Spring annotation that manages transaction boundaries declaratively.

**How it works internally — Proxy pattern:**
```
Client → Proxy (TransactionInterceptor) → Actual Bean
         ↓
    1. Open transaction (begin)
    2. Call actual method
    3. If success → commit
    4. If RuntimeException → rollback
```

**Key properties:**

| Property | Default | Description |
|----------|---------|-------------|
| `propagation` | `REQUIRED` | Join existing or create new transaction |
| `isolation` | Database default | Transaction isolation level |
| `readOnly` | `false` | Optimization hint — skips dirty checking |
| `rollbackFor` | `RuntimeException` | Which exceptions trigger rollback |
| `timeout` | -1 (no timeout) | Seconds before timeout |

**Common propagation levels:**
```java
REQUIRED     — Use existing tx, or create new (default)
REQUIRES_NEW — Always create new tx (suspends existing)
NESTED       — Create savepoint within existing tx
SUPPORTS     — Use tx if exists, else run without
NOT_SUPPORTED — Suspend existing tx, run without
```

**Gotcha — Self-invocation doesn't work:**
```java
@Service
public class OrderService {
    public void processOrder() {
        this.updateInventory();  // @Transactional on updateInventory is IGNORED!
        // Because Spring proxy is bypassed in self-calls
    }

    @Transactional
    public void updateInventory() { ... }
}
```
**Fix:** Extract to a separate service, or use `@Transactional` on the outer method.

---

## Scenario / Interview Deep-Dive Questions

### Q8. Your API is slow. You suspect Hibernate. How do you diagnose?

**Answer — Step-by-step:**

1. **Enable SQL logging** — `spring.jpa.show-sql=true` + `hibernate.format_sql=true`
2. **Check for N+1** — Count queries per request. If you see patterns like `SELECT ... FROM employee WHERE dept_id = ?` repeated N times → N+1 problem
3. **Enable Hibernate Statistics:**
   ```yaml
   hibernate.generate_statistics: true
   logging.level.org.hibernate.stat: DEBUG
   ```
   This shows: entity loads, query counts, cache hit ratios, flush count
4. **Check fetch types** — Look for `FetchType.EAGER` on any `@ManyToOne` or `@OneToOne`
5. **Review query plan** — Use `EXPLAIN ANALYZE` on the generated SQL
6. **Check connection pool** — HikariCP `maximumPoolSize`, `connectionTimeout`
7. **Check batch operations** — For bulk inserts, enable:
   ```yaml
   spring.jpa.properties.hibernate.jdbc.batch_size: 50
   spring.jpa.properties.hibernate.order_inserts: true
   spring.jpa.properties.hibernate.order_updates: true
   ```

---

### Q9. How do you handle bulk inserts efficiently with Hibernate?

**Answer:**
Naive approach (slow — 1 INSERT per entity):
```java
for (int i = 0; i < 10000; i++) {
    entityManager.persist(new Product("Product-" + i));
}
```

**Optimized approach:**
```java
@Transactional
public void bulkInsert(List<Product> products) {
    int batchSize = 50;
    for (int i = 0; i < products.size(); i++) {
        entityManager.persist(products.get(i));
        if (i > 0 && i % batchSize == 0) {
            entityManager.flush();   // Force SQL execution
            entityManager.clear();   // Clear L1 cache to avoid OOM
        }
    }
}
```

**Properties to enable:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true
        generate_statistics: true
```

**Important:** `IDENTITY` ID generation strategy **disables batching**. Use `SEQUENCE` or `TABLE` for batch inserts.

---

### Q10. What is the difference between `save()`, `persist()`, `merge()`, and `update()`?

**Answer:**

| Method | Returns | Detached Entity? | Transient Entity? | Notes |
|--------|---------|-----------------|-------------------|-------|
| `persist()` (JPA) | void | Throws exception | Saves & makes managed | Standard JPA |
| `save()` (Spring Data) | Entity | Calls merge() | Calls persist() | Convenience wrapper |
| `merge()` (JPA) | New managed copy | Creates managed copy | Creates managed copy | Returns NEW object |
| `update()` (Hibernate) | void | Reattaches | Throws if ID exists | Hibernate-specific |

**Critical gotcha with `merge()`:**
```java
Product detached = new Product(1L, "Updated");
Product managed = entityManager.merge(detached);
// detached is STILL detached!
// managed is the new managed copy
// Always use the returned object!
```

---

### Q11. Explain `CascadeType` options and when to use each.

**Answer:**

| CascadeType | Effect | Use Case |
|-------------|--------|----------|
| `PERSIST` | Save child when parent is saved | Always safe |
| `MERGE` | Update child when parent is merged | Common |
| `REMOVE` | Delete child when parent is deleted | Parent owns child lifecycle |
| `REFRESH` | Refresh child when parent is refreshed | Rare |
| `DETACH` | Detach child when parent is detached | Rare |
| `ALL` | All of the above | Only for strong parent-child (Order → OrderItems) |

**Rule of thumb:**
- Use `CascadeType.ALL` + `orphanRemoval = true` for **composition** (Order → OrderItems)
- Use `CascadeType.PERSIST` + `CascadeType.MERGE` for **aggregation** (Department → Employees)
- **NEVER cascade from child to parent** (Employee → Department)
- **NEVER cascade on `@ManyToMany`** with `REMOVE` — it will delete shared entities

---

### Q12. What is `@Version` and Optimistic Locking?

**Answer:**
Optimistic locking prevents lost updates without database locks.

```java
@Entity
public class Product {
    @Id
    private Long id;

    @Version
    private Integer version;  // Auto-incremented by Hibernate on each update

    private String name;
    private BigDecimal price;
}
```

**How it works:**
```sql
-- When updating, Hibernate adds version check:
UPDATE product SET name = ?, price = ?, version = 2
WHERE id = 1 AND version = 1;

-- If another transaction already updated (version is now 2):
-- 0 rows affected → Hibernate throws OptimisticLockException
```

**Handling the exception:**
```java
@Transactional
public void updateProduct(Long id, ProductDTO dto) {
    try {
        Product p = repo.findById(id).orElseThrow();
        p.setPrice(dto.getPrice());
        // Flush triggers version check
    } catch (OptimisticLockException e) {
        // Retry or inform user: "Data was modified by another user"
    }
}
```

**When to use:**
- High-read, low-write scenarios
- Web applications where users may edit the same record
- Preferred over pessimistic locking for better concurrency

---

### Q13. Spring Data JPA — Repository hierarchy and custom queries

**Answer:**

```
Repository (marker)
  └── CrudRepository (CRUD)
       └── PagingAndSortingRepository (pagination + sort)
            └── JpaRepository (JPA-specific: flush, batch delete)
```

**Query methods:**
```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. Derived query (method name → SQL)
    List<User> findByEmailAndStatus(String email, Status status);

    // 2. JPQL
    @Query("SELECT u FROM User u WHERE u.department.name = :deptName")
    List<User> findByDepartmentName(@Param("deptName") String name);

    // 3. Native SQL
    @Query(value = "SELECT * FROM users WHERE email LIKE %:domain", nativeQuery = true)
    List<User> findByEmailDomain(@Param("domain") String domain);

    // 4. Projection (DTO)
    @Query("SELECT new com.app.dto.UserSummary(u.id, u.name) FROM User u")
    List<UserSummary> findAllSummaries();

    // 5. Modifying queries
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.lastLogin < :date")
    int deactivateInactiveUsers(@Param("status") Status status, @Param("date") LocalDate date);
}
```

---

### Q14. How do you handle database migrations with Hibernate?

**Answer:**

| Approach | When | Tool |
|----------|------|------|
| `spring.jpa.hibernate.ddl-auto=create` | Dev only | Hibernate auto-DDL |
| **Flyway** | Production | Versioned SQL scripts (V1__create_user.sql) |
| **Liquibase** | Production | XML/YAML/JSON changesets |

**Never use `ddl-auto` in production.** Always use migration tools.

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__add_email_column.sql
└── V3__create_orders_table.sql
```

**Flyway + Spring Boot:**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

### Q15. What is the difference between `findById()`, `getById()`, and `getReferenceById()`?

**Answer:**

| Method | Returns | SQL Executed? | Use Case |
|--------|---------|-------------|----------|
| `findById(id)` | `Optional<T>` | Immediately | When you need the data |
| `getReferenceById(id)` | Proxy | No (lazy) | When you only need a reference (e.g., setting FK) |
| `getById(id)` (deprecated) | Proxy | No (lazy) | Replaced by getReferenceById |

```java
// Use findById when you need the actual data:
User user = repo.findById(1L).orElseThrow();

// Use getReferenceById when setting a foreign key:
Order order = new Order();
order.setUser(userRepo.getReferenceById(userId));  // No SELECT for user!
orderRepo.save(order);
```

---

## Quick Reference

### Hibernate Interview Cheat Sheet

| Topic | Key Point |
|-------|-----------|
| JPA vs Hibernate | JPA = spec, Hibernate = implementation |
| Entity states | Transient → Managed → Detached → Removed |
| N+1 fix | JOIN FETCH, @EntityGraph, @BatchSize |
| Caching | L1 = session (auto), L2 = shared (configure), Query cache |
| Lazy default | Collections = LAZY, @ManyToOne = EAGER (override to LAZY!) |
| @Transactional self-call | Doesn't work — proxy bypassed. Extract to separate service. |
| Bulk inserts | Batch size + flush/clear + SEQUENCE strategy |
| Optimistic locking | @Version field, prevents lost updates |
| DDL in production | Never use ddl-auto. Use Flyway or Liquibase. |
| save vs persist | save() returns entity, persist() returns void. merge() returns NEW managed copy. |

---

### Q16. JPQL vs Native SQL vs Criteria API — when to use which?

**Answer:**

| Feature | JPQL | Native SQL | Criteria API |
|---------|------|-----------|--------------|
| Syntax | `SELECT e FROM Employee e` | `SELECT * FROM employees` | Java method calls |
| Entity-aware | ✅ Yes | ❌ No (uses table/column names) | ✅ Yes |
| Database portable | ✅ Yes | ❌ No (DB-specific) | ✅ Yes |
| Dynamic queries | ❌ Hard | ❌ Hard | ✅ Built for this |
| Complex SQL | ⚠️ Limited | ✅ Full power | ⚠️ Verbose |
| Type-safe | ❌ No (strings) | ❌ No | ✅ With Metamodel |

```java
// JPQL — entity-based queries:
@Query("SELECT e FROM Employee e WHERE e.department.name = :dept AND e.salary > :min")
List<Employee> findByDeptAndMinSalary(@Param("dept") String dept, @Param("min") double min);

// Native SQL — when you need DB-specific features:
@Query(value = "SELECT * FROM employees WHERE MATCH(name) AGAINST (?1 IN BOOLEAN MODE)",
       nativeQuery = true)
List<Employee> fullTextSearch(String keyword);

// Criteria API — dynamic queries (filters come from UI):
public List<Employee> search(String name, String dept, Double minSalary) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
    Root<Employee> root = cq.from(Employee.class);

    List<Predicate> predicates = new ArrayList<>();
    if (name != null)      predicates.add(cb.like(root.get("name"), "%" + name + "%"));
    if (dept != null)      predicates.add(cb.equal(root.get("department"), dept));
    if (minSalary != null) predicates.add(cb.greaterThan(root.get("salary"), minSalary));

    cq.where(predicates.toArray(new Predicate[0]));
    return entityManager.createQuery(cq).getResultList();
}
```

**When to use what:**
- **JPQL:** Known queries with entity navigation (90% of cases)
- **Native SQL:** DB-specific features (full-text search, window functions, stored procs)
- **Criteria API:** Dynamic filters (search forms, admin dashboards)
- **Spring Data JPA Specifications:** Reusable Criteria predicates (even better):

```java
// Specification pattern (Spring Data):
public static Specification<Employee> hasSalaryAbove(double min) {
    return (root, query, cb) -> cb.greaterThan(root.get("salary"), min);
}
public static Specification<Employee> inDepartment(String dept) {
    return (root, query, cb) -> cb.equal(root.get("department"), dept);
}
// Combine:
employeeRepo.findAll(hasSalaryAbove(50000).and(inDepartment("Engineering")));
```

---

### Q17. Optimistic vs Pessimistic Locking — when to use which?

**Answer:**

**Optimistic Locking** — assumes conflicts are rare, checks at commit time:
```java
@Entity
public class Product {
    @Id
    private Long id;
    private String name;
    private int stock;

    @Version  // ← Hibernate auto-increments this on every update
    private int version;
}

// What happens:
// Thread A reads Product (version=1)
// Thread B reads Product (version=1)
// Thread A updates stock → version becomes 2 ✅
// Thread B updates stock → version check fails → OptimisticLockException ❌
// Thread B must RETRY (re-read and re-apply)
```

**Pessimistic Locking** — locks the row in DB, others wait:
```java
// SELECT ... FOR UPDATE — blocks other transactions
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdForUpdate(@Param("id") Long id);

// LockModeType options:
// PESSIMISTIC_READ   → shared lock (others can read, can't write)
// PESSIMISTIC_WRITE  → exclusive lock (others can't read OR write)
// PESSIMISTIC_FORCE_INCREMENT → exclusive lock + increment @Version
```

**When to use which:**
| Scenario | Use | Why |
|----------|-----|-----|
| Read-heavy, rare conflicts | **Optimistic** | No DB lock overhead |
| High contention (inventory, seats) | **Pessimistic** | Prevents wasted work |
| Short transactions | **Optimistic** | Retry is cheap |
| Long transactions | **Pessimistic** | Can't afford to redo |
| Payment processing (NPCI) | **Pessimistic** | Money can't be wrong |

**Interview follow-up — Lost Update Problem:**
```
Without locking:
  T1: READ balance = 100
  T2: READ balance = 100
  T1: UPDATE balance = 100 + 50 = 150 ✅
  T2: UPDATE balance = 100 - 30 = 70  ❌ (overwrites T1's change!)

With @Version (optimistic):
  T1: READ balance=100, version=1
  T2: READ balance=100, version=1
  T1: UPDATE SET balance=150, version=2 WHERE version=1 ✅
  T2: UPDATE SET balance=70, version=2 WHERE version=1 → 0 rows updated → EXCEPTION
```

---

### Q18. What is the Criteria API Metamodel? Why use it?

**Answer:**

The string-based Criteria API is error-prone — a typo in `"salary"` won't fail until runtime.

```java
// ❌ String-based (runtime error if field name wrong):
cb.greaterThan(root.get("salry"), 50000);  // typo — fails at runtime!

// ✅ Metamodel-based (compile-time safety):
cb.greaterThan(root.get(Employee_.salary), 50000);  // Employee_ is auto-generated
```

**Generate Metamodel:** Add `hibernate-jpamodelgen` dependency → generates `Employee_` class at compile time with static fields for every entity attribute.

```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-jpamodelgen</artifactId>
    <scope>provided</scope>
</dependency>
```
