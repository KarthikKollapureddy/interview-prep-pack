# MySQL Indexing & Performance — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. How do indexes work in MySQL? Explain B+ Tree structure.

```
B+ Tree Index (clustered/secondary)

                    [50]
                  /       \
           [20, 35]     [70, 85]
          / |  \       / |   \
     [10,15] [20,25,30] [35,40,45] [50,60,65] [70,75,80] [85,90,95]
         ↓       ↓         ↓          ↓          ↓          ↓
       data    data      data       data       data       data
       
     Leaf nodes are linked → efficient range scans
```

**Key properties:**
- **Balanced** — all leaf nodes at same depth → O(log n) lookups
- **Sorted** — enables range queries (`BETWEEN`, `>`, `<`)
- **Leaf nodes linked** — efficient sequential scans

**Index types:**
| Type | Description |
|------|-------------|
| **Clustered (Primary Key)** | Data rows stored in PK order. One per table. |
| **Secondary** | Separate B+ tree, leaf contains PK reference |
| **Composite** | Multi-column index |
| **Covering** | Index contains all columns needed (no table lookup) |
| **Full-text** | For text search (`MATCH ... AGAINST`) |
| **Hash** | Memory engine only, exact lookups only (no range) |

---

### Q2. Composite indexes — what is the leftmost prefix rule?

```sql
CREATE INDEX idx_shipments ON shipments (status, customer_id, created_at);

-- ✅ Uses index (leftmost prefix):
WHERE status = 'IN_TRANSIT'
WHERE status = 'IN_TRANSIT' AND customer_id = 123
WHERE status = 'IN_TRANSIT' AND customer_id = 123 AND created_at > '2024-01-01'

-- ❌ Does NOT use index (skips leftmost column):
WHERE customer_id = 123                          -- skips status
WHERE created_at > '2024-01-01'                   -- skips status and customer_id
WHERE customer_id = 123 AND created_at > '2024-01-01' -- skips status
```

**Think of it like a phone book** sorted by (LastName, FirstName, City). You can look up by LastName, or (LastName, FirstName), but NOT by FirstName alone.

---

### Q3. What is a covering index?

```sql
-- Query
SELECT tracking_number, status FROM shipments WHERE customer_id = 123;

-- Covering index (includes all selected + filtered columns)
CREATE INDEX idx_covering ON shipments (customer_id, tracking_number, status);

-- EXPLAIN shows "Using index" — no table lookup needed!
-- The index itself contains all data the query needs.
```

**Performance gain:** Avoids the "random I/O" of looking up full rows from the clustered index. Huge win for read-heavy queries.

---

### Q4. EXPLAIN plan — how to read every column (with real output analysis)?

```sql
EXPLAIN SELECT * FROM shipments WHERE tracking_number = '123456';
```

**Every column explained:**

```
┌────────────────┬─────────────────────────────────────────────────────────────┐
│ Column         │ What It Tells You                                           │
├────────────────┼─────────────────────────────────────────────────────────────┤
│ id             │ Query sequence number. Same id = same SELECT. Higher = subq │
│ select_type    │ SIMPLE / PRIMARY / SUBQUERY / DERIVED / UNION               │
│ table          │ Which table this row refers to                              │
│ partitions     │ Which partitions are scanned (NULL = not partitioned)       │
│ type           │ Access method (BEST → WORST):                               │
│                │   system > const > eq_ref > ref > range > index > ALL       │
│ possible_keys  │ Indexes MySQL COULD use                                     │
│ key            │ Index MySQL ACTUALLY chose (NULL = no index!)               │
│ key_len        │ Bytes of index used — reveals how many columns of a         │
│                │   composite index are actually utilized                     │
│ ref            │ What value is compared to the index (const, column, NULL)   │
│ rows           │ Estimated rows MySQL must examine (NOT exact — from stats)  │
│ filtered       │ % of rows remaining after table condition applied           │
│ Extra          │ Crucial details (see breakdown below)                       │
└────────────────┴─────────────────────────────────────────────────────────────┘
```

---

### Q4a. EXPLAIN `type` column — access methods ranked (with examples).

```
BEST ──────────────────────────────────────────────────── WORST

system → const → eq_ref → ref → range → index → ALL

┌──────────┬──────────────────────────────┬──────────────────────────────────┐
│ type     │ Meaning                      │ Example Query                    │
├──────────┼──────────────────────────────┼──────────────────────────────────┤
│ system   │ Table has 0 or 1 row         │ SELECT * FROM single_row_table   │
│ const    │ PK/UNIQUE lookup, 1 row max  │ WHERE id = 42                    │
│ eq_ref   │ PK/UNIQUE JOIN, 1 row each   │ JOIN users u ON u.id = o.user_id │
│ ref      │ Non-unique index lookup       │ WHERE status = 'ACTIVE'          │
│ fulltext │ Fulltext index used           │ MATCH(body) AGAINST('search')    │
│ range    │ Index range scan              │ WHERE created_at > '2024-01-01'  │
│ index    │ Full index scan (all rows)    │ SELECT COUNT(*) — reads index    │
│ ALL      │ FULL TABLE SCAN ⚠️           │ No index matches the query       │
└──────────┴──────────────────────────────┴──────────────────────────────────┘

Rule of thumb:
  ✅ const, eq_ref, ref, range — acceptable
  ⚠️  index — reads entire index (okay for small tables)
  🔴 ALL — full table scan on large tables = performance disaster
```

---

### Q4b. EXPLAIN `Extra` column — what each value means.

```
┌────────────────────────────┬───────┬─────────────────────────────────────────┐
│ Extra Value                │ Good? │ What It Means                           │
├────────────────────────────┼───────┼─────────────────────────────────────────┤
│ Using index                │ ✅    │ Covering index — all data from index,   │
│                            │       │ no table row lookup needed              │
│ Using index condition      │ ✅    │ Index Condition Pushdown (ICP) — filter │
│                            │       │ applied at storage engine level         │
│ Using where                │ 🟡   │ MySQL server filters AFTER reading rows │
│                            │       │ (may mean index isn't filtering enough) │
│ Using temporary            │ 🔴   │ Temp table created — common with        │
│                            │       │ GROUP BY on non-indexed columns         │
│ Using filesort             │ 🔴   │ Extra sorting pass — ORDER BY without   │
│                            │       │ matching index                          │
│ Using join buffer          │ 🟡   │ No index on JOIN column — nested loop   │
│ Using MRR                  │ ✅    │ Multi-Range Read optimization active    │
│ Backward index scan        │ 🟡   │ Scanning index in reverse (ORDER BY    │
│                            │       │ DESC) — consider DESC index in 8.0+    │
│ Select tables optimized... │ ✅    │ Query resolved entirely from indexes    │
│ Impossible WHERE           │ 🟡   │ WHERE condition always false            │
│ Using index for group-by   │ ✅    │ GROUP BY resolved using loose index     │
└────────────────────────────┴───────┴─────────────────────────────────────────┘
```

---

### Q4c. Reading real EXPLAIN outputs — 5 sample scenarios.

**Scenario 1: Full table scan (the worst case)**
```sql
EXPLAIN SELECT * FROM shipments WHERE YEAR(created_at) = 2024;
```
```
+----+-------------+-----------+------+---------------+------+------+---------+
| id | select_type | table     | type | possible_keys | key  | rows | Extra   |
+----+-------------+-----------+------+---------------+------+------+---------+
|  1 | SIMPLE      | shipments | ALL  | NULL          | NULL | 5.2M | Using   |
|    |             |           |      |               |      |      | where   |
+----+-------------+-----------+------+---------------+------+------+---------+
```
```
Diagnosis:
  type = ALL         → Full table scan (5.2 million rows!)
  key = NULL         → No index used
  possible_keys=NULL → MySQL can't use ANY index
  
Root cause: YEAR(created_at) wraps column in a function → index unusable

Fix:
  WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'
  Now MySQL uses range scan on created_at index.
```

---

**Scenario 2: Good — PK lookup**
```sql
EXPLAIN SELECT * FROM shipments WHERE id = 12345;
```
```
+----+-------------+-----------+-------+---------------+---------+---------+-------+------+-------+
| id | select_type | table     | type  | possible_keys | key     | key_len | ref   | rows | Extra |
+----+-------------+-----------+-------+---------------+---------+---------+-------+------+-------+
|  1 | SIMPLE      | shipments | const | PRIMARY       | PRIMARY | 8       | const |    1 | NULL  |
+----+-------------+-----------+-------+---------------+---------+---------+-------+------+-------+
```
```
Diagnosis:
  type = const   → Best possible — exact PK match, 1 row
  key = PRIMARY  → Using primary key index
  rows = 1       → Only 1 row examined
  ref = const    → Comparing against a constant value
  
This is optimal. No further tuning needed.
```

---

**Scenario 3: JOIN with missing index**
```sql
EXPLAIN SELECT s.tracking_number, e.event_type 
FROM shipments s 
JOIN tracking_events e ON s.id = e.shipment_id
WHERE s.customer_id = 42;
```
```
+----+-------------+-------+------+------------------+------------------+---------+-----------+-------+-----------+
| id | select_type | table | type | possible_keys    | key              | key_len | ref       | rows  | Extra     |
+----+-------------+-------+------+------------------+------------------+---------+-----------+-------+-----------+
|  1 | SIMPLE      | s     | ref  | PRIMARY,idx_cust | idx_cust         | 8       | const     |    12 | NULL      |
|  1 | SIMPLE      | e     | ALL  | NULL             | NULL             | NULL    | NULL      | 890000| Using     |
|    |             |       |      |                  |                  |         |           |       | where;    |
|    |             |       |      |                  |                  |         |           |       | Using     |
|    |             |       |      |                  |                  |         |           |       | join buff |
+----+-------------+-------+------+------------------+------------------+---------+-----------+-------+-----------+
```
```
Diagnosis:
  Row 1 (shipments): type=ref, key=idx_cust → Good, using customer index, 12 rows
  Row 2 (events):    type=ALL, key=NULL     → 🔴 Full scan of 890K rows!
  Extra: "Using join buffer"                → No index on JOIN column

Root cause: tracking_events.shipment_id has no index

Fix:
  CREATE INDEX idx_events_shipment ON tracking_events (shipment_id);
  
After fix:
  Row 2 becomes: type=ref, key=idx_events_shipment, rows=~8
  Total: 12 × 8 = ~96 rows examined instead of 12 × 890,000
```

---

**Scenario 4: Covering index (optimal read query)**
```sql
EXPLAIN SELECT tracking_number, status 
FROM shipments 
WHERE customer_id = 42 AND status = 'IN_TRANSIT';
```
```
+----+-------------+-----------+------+------------------+------------------+---------+-------------+------+-------------+
| id | select_type | table     | type | possible_keys    | key              | key_len | ref         | rows | Extra       |
+----+-------------+-----------+------+------------------+------------------+---------+-------------+------+-------------+
|  1 | SIMPLE      | shipments | ref  | idx_cust_status  | idx_cust_status  | 12      | const,const |    3 | Using index |
+----+-------------+-----------+------+------------------+------------------+---------+-------------+------+-------------+
```
```
Diagnosis:
  type = ref         → Index lookup (good)
  Extra: Using index → 🟢 COVERING INDEX! No table lookup needed
  rows = 3           → Only 3 rows examined
  key_len = 12       → Both columns of composite index used (8+4 bytes)
  ref = const,const  → Both comparisons are against constant values
  
This is optimal. Index contains all needed columns.
Index: (customer_id, status, tracking_number) — covers SELECT + WHERE.
```

---

**Scenario 5: Filesort + temporary table (common GROUP BY problem)**
```sql
EXPLAIN SELECT customer_id, COUNT(*) as total, AVG(weight) as avg_weight
FROM shipments
WHERE created_at > '2024-01-01'
GROUP BY customer_id
ORDER BY total DESC
LIMIT 10;
```
```
+----+-------------+-----------+-------+---------------+-----------+---------+------+--------+----------------------------------------------+
| id | select_type | table     | type  | possible_keys | key       | key_len | ref  | rows   | Extra                                        |
+----+-------------+-----------+-------+---------------+-----------+---------+------+--------+----------------------------------------------+
|  1 | SIMPLE      | shipments | range | idx_created   | idx_created| 5      | NULL | 320000 | Using index condition; Using temporary;       |
|    |             |           |       |               |           |         |      |        | Using filesort                               |
+----+-------------+-----------+-------+---------------+-----------+---------+------+--------+----------------------------------------------+
```
```
Diagnosis:
  type = range             → Range scan on created_at (acceptable)
  Extra: Using temporary   → 🔴 Temp table for GROUP BY
  Extra: Using filesort    → 🔴 Sorting for ORDER BY total DESC
  rows = 320,000           → Scanning 320K rows

Why both flags:
  GROUP BY customer_id → temp table to accumulate per-customer counts
  ORDER BY total DESC  → can't sort by computed alias via index

Fix options:
  1. Add index: (created_at, customer_id) — avoids temp for grouping
  2. Materialized view / summary table for reporting queries
  3. If acceptable, remove ORDER BY and sort in application layer
```

---

### Q4d. `key_len` — how to decode it to find how much of a composite index is used.

```
key_len tells you how many bytes of an index are utilized.
Use this to verify if ALL columns of a composite index are being used.

Data type sizes:
  INT          → 4 bytes
  BIGINT       → 8 bytes
  VARCHAR(N)   → 3 × N + 2 (for utf8mb4, +2 for length prefix)
  DATE         → 3 bytes
  DATETIME     → 5 bytes (MySQL 5.6.4+)
  TIMESTAMP    → 4 bytes
  +1 byte if column is NULLable

Example:
  INDEX idx_composite (customer_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL)

  key_len = 8                → Only customer_id used
  key_len = 8 + 62 = 70     → customer_id + status used (VARCHAR(20) utf8mb4 = 3×20+2 = 62)
  key_len = 8 + 62 + 5 = 75 → All 3 columns used ✅
  
If key_len is shorter than expected → your WHERE clause doesn't utilize the full index.
Check: is a column missing from WHERE? Or is a range condition stopping further index use?
```

---

### Q4e. EXPLAIN ANALYZE (MySQL 8.0.18+) — actual execution stats.

```sql
EXPLAIN ANALYZE SELECT * FROM shipments WHERE customer_id = 42;
```
```
-> Index lookup on shipments using idx_customer (customer_id=42)
   (cost=4.30 rows=12) (actual time=0.045..0.062 rows=12 loops=1)
```
```
What EXPLAIN ANALYZE adds over EXPLAIN:
  ┌────────────────┬─────────────────────────────────────────┐
  │ Field          │ Meaning                                 │
  ├────────────────┼─────────────────────────────────────────┤
  │ cost=4.30      │ Optimizer's estimated cost              │
  │ rows=12        │ Estimated rows (from stats)             │
  │ actual time    │ Real time: first_row..last_row (ms)     │
  │ rows=12        │ ACTUAL rows returned                    │
  │ loops=1        │ How many times this step was executed   │
  └────────────────┴─────────────────────────────────────────┘

Key insight: Compare estimated rows vs actual rows.
  If they differ significantly → run ANALYZE TABLE to refresh stats.

  Estimated: rows=100  vs  Actual: rows=50000
  → Statistics are stale! Run: ANALYZE TABLE shipments;
```

---

### Q4f. Quick EXPLAIN diagnostic checklist (interview answer).

```
When asked "How do you diagnose a slow query?" — answer with this:

1. EXPLAIN the query — check type, key, rows, Extra
2. Red flags to look for:
   ┌─────────────────────────┬──────────────────────────────────────┐
   │ Red Flag                │ Fix                                  │
   ├─────────────────────────┼──────────────────────────────────────┤
   │ type: ALL               │ Add index on WHERE/JOIN columns      │
   │ key: NULL               │ No usable index — create one         │
   │ rows: very large        │ Index not selective enough / missing  │
   │ Extra: Using filesort   │ Add index matching ORDER BY columns  │
   │ Extra: Using temporary  │ Add index matching GROUP BY columns  │
   │ Extra: Using where      │ Index filters partially — check      │
   │                         │ composite index column order          │
   │ key_len too short       │ Composite index partially used —     │
   │                         │ check WHERE clause covers all cols   │
   │ filtered: < 20%         │ Many rows read but few returned —    │
   │                         │ consider more selective index         │
   └─────────────────────────┴──────────────────────────────────────┘

3. After adding/changing index → re-EXPLAIN and compare rows + type
4. For production: use EXPLAIN ANALYZE to see actual vs estimated
5. If stats are stale: ANALYZE TABLE to refresh index statistics
```

---

## Scenario-Based Questions

### Q5. At FedEx, the shipment search query is slow. It searches by status, date range, and customer. How do you optimize?

```sql
-- Slow query
SELECT * FROM shipments 
WHERE status = 'IN_TRANSIT' 
  AND created_at BETWEEN '2024-01-01' AND '2024-01-31'
  AND customer_id = 42;

-- Step 1: Analyze with EXPLAIN
EXPLAIN SELECT ...;  -- type: ALL, rows: 5000000 (full scan!)

-- Step 2: Create composite index (most selective column first)
CREATE INDEX idx_shipments_search ON shipments (customer_id, status, created_at);
-- customer_id is most selective (filters to ~100 rows out of 5M)

-- Step 3: Verify
EXPLAIN SELECT ...;  -- type: range, rows: 15, Extra: Using index condition ✅
```

**Column ordering in composite index:**
1. **Equality conditions first** (`customer_id = 42`, `status = 'IN_TRANSIT'`)
2. **Range condition last** (`created_at BETWEEN ...`)
3. Most selective column first among equals

---

### Q6. At NPCI, the transactions table has 100M rows. Queries are slow despite indexes. What are your options?

1. **Partitioning** — split table by date range
```sql
CREATE TABLE transactions (
  id BIGINT, user_id BIGINT, amount DECIMAL, created_at DATETIME
) PARTITION BY RANGE (YEAR(created_at)) (
  PARTITION p2023 VALUES LESS THAN (2024),
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
-- Query for 2024 data only scans p2024 partition
```

2. **Read replicas** — route read queries to replicas
3. **Archiving** — move old data to archive table
4. **Materialized views** — pre-compute aggregated reports
5. **Caching** — Redis for frequently accessed data

---

### Q7. At Hatio, your payment processing table has frequent writes AND reads. How do you balance index performance?

**Trade-off:** More indexes = faster reads, slower writes (every INSERT/UPDATE must update all indexes).

**Strategy:**
- Keep indexes minimal on write-heavy tables
- Use covering indexes for critical read paths
- Drop unused indexes: `SELECT * FROM sys.schema_unused_indexes;`
- Monitor index usage: `SELECT * FROM sys.schema_index_statistics;`

```sql
-- Essential indexes only for payment transactions:
CREATE UNIQUE INDEX idx_txn_id ON transactions (transaction_id);  -- Lookups
CREATE INDEX idx_merchant_date ON transactions (merchant_id, created_at);  -- Reports
-- Don't add indexes for every possible query — add them based on actual slow query log
```

---

## Gotchas & Edge Cases

### Q8. Index not used despite existing — why?

Common reasons MySQL ignores an index:
1. **Query returns > 20-30% of rows** — full scan is faster
2. **Function on indexed column** — `WHERE YEAR(created_at) = 2024` ❌ (use range instead)
3. **Type mismatch** — `WHERE phone = 1234567890` when phone is VARCHAR
4. **OR conditions** — `WHERE status = 'A' OR customer_id = 5` (use UNION instead)
5. **LIKE with leading wildcard** — `WHERE name LIKE '%john'` ❌

```sql
-- ❌ Function breaks index usage
WHERE DATE(created_at) = '2024-01-15'

-- ✅ Use range instead
WHERE created_at >= '2024-01-15' AND created_at < '2024-01-16'
```

---

### Q9. Difference between `DELETE`, `TRUNCATE`, and `DROP`?

| | DELETE | TRUNCATE | DROP |
|---|---|---|---|
| Removes | Specific rows | All rows | Table + data |
| WHERE clause | Yes | No | No |
| Rollback | Yes (logged) | No (DDL) | No |
| Auto-increment | Continues | Resets | N/A |
| Speed | Slowest | Fast | Fastest |
| Triggers | Fires | Doesn't fire | N/A |
