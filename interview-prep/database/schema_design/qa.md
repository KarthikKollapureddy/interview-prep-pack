# Database Schema Design & Data Modeling — Interview Q&A

> 10 questions covering ER diagrams, normalization, SQL vs NoSQL decisions, and real schema design problems  
> Priority: **P0** — "Design the schema for X" is asked in every DB-heavy interview round

---

## Conceptual Questions

### Q1. Walk me through how you design a database schema from requirements.

**Answer:**

```
Step 1: IDENTIFY ENTITIES
  → Read requirements, highlight nouns → those are your tables
  → Example: "Users place Orders for Products" → User, Order, Product

Step 2: IDENTIFY RELATIONSHIPS
  → One-to-Many: User → Orders (one user, many orders)
  → Many-to-Many: Order ↔ Product (needs junction table: order_items)
  → One-to-One: User → UserProfile (rare, usually merge into one table)

Step 3: DEFINE ATTRIBUTES
  → Each entity gets columns: data types, constraints (NOT NULL, UNIQUE)
  → Identify primary keys (usually auto-increment ID or UUID)
  → Identify foreign keys (relationships)

Step 4: NORMALIZE (3NF minimum)
  → Remove duplicate data, ensure each fact stored once

Step 5: DENORMALIZE WHERE NEEDED
  → Add redundancy for read performance (calculated fields, summary tables)
  → Decide based on read:write ratio

Step 6: ADD INDEXES
  → Foreign keys, frequently queried columns, composite indexes for common WHERE + ORDER BY
```

---

### Q2. Explain normalization (1NF → 2NF → 3NF) with examples.

**Answer:**

**1NF — No repeating groups, atomic values:**
```
❌ Violates 1NF:
| order_id | products          |
|----------|-------------------|
| 1        | Laptop, Mouse     |  ← multi-valued, not atomic

✅ 1NF:
| order_id | product   |
|----------|-----------|
| 1        | Laptop    |
| 1        | Mouse     |
```

**2NF — No partial dependencies (every non-key depends on WHOLE primary key):**
```
❌ Violates 2NF (composite key: order_id + product_id):
| order_id | product_id | product_name | quantity |
|----------|-----------|--------------|----------|
| 1        | 101       | Laptop       | 2        |
↑ product_name depends only on product_id, not on the full composite key

✅ 2NF: Split into two tables:
  orders_items(order_id, product_id, quantity)
  products(product_id, product_name)
```

**3NF — No transitive dependencies (non-key depends only on primary key):**
```
❌ Violates 3NF:
| employee_id | department_id | department_name |
|-------------|--------------|-----------------|
↑ department_name depends on department_id, not on employee_id (transitive)

✅ 3NF: Split:
  employees(employee_id, department_id)
  departments(department_id, department_name)
```

**Interview rule:** Normalize for OLTP (transactional systems). Denormalize for OLAP (analytics/reporting).

---

### Q3. When to denormalize? Give real examples.

**Answer:**

| Scenario | Normalization | Denormalization | Why Denormalize |
|----------|:---:|:---:|------|
| E-commerce order total | Calculate from order_items every time | Store `total_amount` in orders table | Avoid expensive JOIN + SUM on every order view |
| User's post count | COUNT(*) from posts table | Store `post_count` in users table | Avoid COUNT on millions of rows |
| Product with category name | JOIN products + categories | Store `category_name` in products | Avoid JOIN on every product listing |
| Audit log / Event store | N/A | Fully denormalized (append-only) | Write-optimized, never updated |

**Denormalization tradeoffs:**
```
✅ Faster reads (no JOINs)
✅ Simpler queries
❌ Data redundancy (more storage)
❌ Update anomalies (must update in multiple places)
❌ More complex write logic (must keep denormalized data in sync)
```

**How to keep denormalized data in sync:**
1. Application-level: update both in same transaction
2. Database triggers (avoid — hard to maintain)
3. Change Data Capture (CDC) — Debezium → Kafka → update materialized view
4. Scheduled batch jobs (eventual consistency OK)

---

### Q4. SQL vs NoSQL — decision framework.

**Answer:**

| Factor | Choose SQL (MySQL/Postgres) | Choose NoSQL (MongoDB/DynamoDB) |
|--------|:---:|:---:|
| Data structure | Well-defined, relational | Flexible, evolving, nested |
| Relationships | Complex (many JOINs) | Few relationships, embed instead |
| Consistency | Strong ACID required | Eventual consistency OK |
| Query patterns | Ad-hoc, complex queries | Known access patterns |
| Scale | Vertical + read replicas | Horizontal sharding native |
| Schema changes | Infrequent, planned | Frequent, agile |

**Real-world examples:**
```
SQL:
  ✅ Banking / Payments (NPCI) — ACID transactions mandatory
  ✅ E-commerce orders — complex relationships (user, order, items, payments, shipping)
  ✅ ERP / CRM — structured data, complex reporting

NoSQL (Document — MongoDB):
  ✅ Product catalog — varying attributes per category
  ✅ Content management — flexible schemas, nested content
  ✅ User profiles — semi-structured, frequently changing

NoSQL (Key-Value — Redis/DynamoDB):
  ✅ Session storage — fast lookup by key
  ✅ Caching — ephemeral data
  ✅ Leaderboards — sorted sets

NoSQL (Wide-Column — Cassandra):
  ✅ Time-series data (IoT sensor data, logs)
  ✅ High write throughput across regions

NoSQL (Graph — Neo4j):
  ✅ Social networks (friends-of-friends)
  ✅ Recommendation engines
  ✅ Fraud detection (transaction graphs)
```

---

## Schema Design Problems

### Q5. Design the schema for an E-Commerce system.

**Answer:**

```sql
-- Core entities:
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(255) UNIQUE NOT NULL,
    name        VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,  -- BCrypt hash
    phone       VARCHAR(15),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
);

CREATE TABLE addresses (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    line1       VARCHAR(255) NOT NULL,
    line2       VARCHAR(255),
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(50) NOT NULL,
    pincode     VARCHAR(10) NOT NULL,
    is_default  BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE categories (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT,  -- self-referencing for hierarchy
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE products (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       DECIMAL(10,2) NOT NULL,
    stock       INT NOT NULL DEFAULT 0,
    category_id BIGINT,
    seller_id   BIGINT,
    version     INT DEFAULT 0,  -- optimistic locking
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_category (category_id),
    INDEX idx_price (price)
);

CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    address_id      BIGINT NOT NULL,
    status          ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    total_amount    DECIMAL(10,2) NOT NULL,  -- denormalized for fast reads
    payment_method  VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
);

CREATE TABLE order_items (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT NOT NULL,
    unit_price  DECIMAL(10,2) NOT NULL,  -- snapshot at order time (not current price!)
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    status          ENUM('PENDING','SUCCESS','FAILED','REFUNDED') DEFAULT 'PENDING',
    payment_method  VARCHAR(50),
    transaction_id  VARCHAR(255) UNIQUE,  -- from payment gateway
    idempotency_key VARCHAR(255) UNIQUE,  -- prevent duplicate payments
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order (order_id)
);
```

**Key design decisions:**
- `order_items.unit_price` snapshots the price at order time (product price may change later)
- `orders.total_amount` is denormalized (avoid SUM on every order view)
- `payments.idempotency_key` prevents double charges
- `products.version` for optimistic locking on stock updates
- Composite index `(user_id, status)` for "my orders filtered by status"

---

### Q6. Design the schema for a UPI Payment System (NPCI-relevant).

**Answer:**

```sql
CREATE TABLE bank_accounts (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    ifsc_code       VARCHAR(11) NOT NULL,
    bank_name       VARCHAR(100) NOT NULL,
    account_type    ENUM('SAVINGS','CURRENT') NOT NULL,
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    version         INT DEFAULT 0  -- optimistic locking for balance
);

CREATE TABLE vpa (  -- Virtual Payment Address (UPI ID)
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    upi_id          VARCHAR(100) UNIQUE NOT NULL,  -- e.g., karthik@upi
    user_id         BIGINT NOT NULL,
    bank_account_id BIGINT NOT NULL,
    is_primary      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id),
    INDEX idx_upi (upi_id)
);

CREATE TABLE transactions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_ref     VARCHAR(36) UNIQUE NOT NULL,  -- UUID
    sender_vpa_id       BIGINT NOT NULL,
    receiver_vpa_id     BIGINT NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    status              ENUM('INITIATED','PENDING','SUCCESS','FAILED','REVERSED') NOT NULL,
    type                ENUM('PAY','COLLECT','REFUND') NOT NULL,
    description         VARCHAR(255),
    idempotency_key     VARCHAR(255) UNIQUE NOT NULL,
    initiated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP NULL,
    failure_reason      VARCHAR(255),
    FOREIGN KEY (sender_vpa_id) REFERENCES vpa(id),
    FOREIGN KEY (receiver_vpa_id) REFERENCES vpa(id),
    INDEX idx_sender_date (sender_vpa_id, initiated_at),
    INDEX idx_receiver_date (receiver_vpa_id, initiated_at),
    INDEX idx_status (status)
);

CREATE TABLE transaction_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id  BIGINT NOT NULL,
    old_status      VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    changed_by      VARCHAR(100),
    changed_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata        JSON,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    INDEX idx_txn (transaction_id)
);
```

**Critical for payment systems:**
- `idempotency_key` on transactions — retry-safe
- `version` on bank_accounts — optimistic locking for concurrent debits
- `audit_log` — full state change history (regulatory requirement)
- Separate read replicas for statement generation (never read from write master for reports)

---

### Q7. ER diagram notation — how to communicate in interviews?

**Answer:**

```
Use this shorthand when drawing on whiteboard:

One-to-Many:  User ──|──< Orders      (one user, many orders)
Many-to-Many: Order >──<  Product     (needs junction table)
One-to-One:   User ──|──| Profile     (rare)

PK = Primary Key (underlined)
FK = Foreign Key (arrow to parent)
UK = Unique Key (double underline)
NN = Not Null (asterisk *)

Example:
┌──────────────┐         ┌───────────────┐        ┌──────────────┐
│   User       │         │   Order       │        │  Product     │
├──────────────┤    1:N  ├───────────────┤   N:M  ├──────────────┤
│ *id (PK)     │────────►│ *id (PK)      │◄──────►│ *id (PK)     │
│ *email (UK)  │         │ *user_id (FK) │        │ *name        │
│ *name        │         │ *status       │        │ *price       │
│  phone       │         │ *total        │        │ *stock       │
│ *created_at  │         │ *created_at   │        │  category_id │
└──────────────┘         └───────────────┘        └──────────────┘
                              │                        │
                              │ 1:N                    │
                         ┌────▼────────────────────────▼──┐
                         │        Order_Items (junction)   │
                         ├─────────────────────────────────┤
                         │ *id (PK)                        │
                         │ *order_id (FK)                  │
                         │ *product_id (FK)                │
                         │ *quantity                       │
                         │ *unit_price                     │
                         └─────────────────────────────────┘
```

---

### Q8. When to use UUID vs Auto-Increment ID?

**Answer:**

| Feature | Auto-Increment | UUID |
|---------|:---:|:---:|
| Size | 4-8 bytes | 16 bytes (36 chars as string) |
| Sequential | Yes (good for B-Tree inserts) | No (random → page splits) |
| Guessable | Yes (`/users/1`, `/users/2`) | No (security benefit) |
| Distributed | Needs coordination | Globally unique, no coordination |
| Index performance | Better (sequential) | Worse (random) |

**Recommendation:**
```
Internal IDs (FK, joins): Auto-increment BIGINT — best performance
Public-facing IDs (APIs, URLs): UUID or ULID — not guessable (IDOR prevention)
Distributed systems: UUID v7 (time-ordered) or ULID — sortable + unique

// Java:
UUID.randomUUID();           // v4 — fully random
// UUID v7 (Java 17+, or use library): time-ordered, better index performance
```

---

### Q9. How do you handle soft deletes vs hard deletes?

**Answer:**

```sql
-- Soft delete: add a column
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

-- All queries must filter:
SELECT * FROM users WHERE is_deleted = FALSE;

-- Spring JPA: @Where annotation
@Entity
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class User { ... }
```

| Approach | Pros | Cons |
|----------|------|------|
| Soft delete | Audit trail, undo possible, FK integrity maintained | Queries slower (filter), unique constraints complex, data grows |
| Hard delete | Clean data, simpler queries, real space freed | No undo, FK cascade issues, audit lost |

**Best practice:** Soft delete for business data (users, orders). Hard delete for ephemeral data (sessions, temp files, logs after retention).

---

### Q10. Explain database partitioning strategies.

**Answer:**

| Strategy | How | Use Case |
|----------|-----|----------|
| **Range** | Partition by date range (each month = 1 partition) | Time-series data, logs, transactions by date |
| **Hash** | Hash of key distributes evenly across N partitions | Even distribution when no natural range |
| **List** | Partition by specific values (region, status) | Multi-tenant, geographic data |
| **Composite** | Range + Hash combined | Large-scale systems |

```sql
-- MySQL range partitioning by date:
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT,
    amount DECIMAL(10,2),
    created_at DATE,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION pmax  VALUES LESS THAN MAXVALUE
);
-- Query: WHERE created_at BETWEEN '2026-01-01' AND '2026-03-31'
-- MySQL prunes to only p2026 partition → much faster
```

**Partitioning vs Sharding:**
- Partitioning = single database, multiple partitions (managed by DB engine)
- Sharding = multiple database servers, data distributed across them (application-level or proxy)
