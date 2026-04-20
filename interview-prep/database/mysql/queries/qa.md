# MySQL Complex Queries — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. JOINs — explain INNER, LEFT, RIGHT, CROSS, SELF.

```sql
-- INNER JOIN: only matching rows from both tables
SELECT s.tracking_number, e.description
FROM shipments s
INNER JOIN tracking_events e ON s.id = e.shipment_id;

-- LEFT JOIN: all from left, matching from right (NULL if no match)
SELECT c.name, s.tracking_number
FROM customers c
LEFT JOIN shipments s ON c.id = s.customer_id;
-- Shows customers even if they have no shipments

-- RIGHT JOIN: all from right, matching from left
-- (rarely used — just swap tables and use LEFT JOIN)

-- CROSS JOIN: cartesian product (every row × every row)
SELECT p.name, c.color FROM products p CROSS JOIN colors c;

-- SELF JOIN: table joined with itself
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

---

### Q2. GROUP BY + aggregate functions + HAVING.

```sql
-- Total shipments by status
SELECT status, COUNT(*) as total
FROM shipments
GROUP BY status;

-- Average order value by customer, only those spending > $1000
SELECT customer_id, AVG(total_amount) as avg_order
FROM orders
GROUP BY customer_id
HAVING AVG(total_amount) > 1000;

-- WHERE vs HAVING:
-- WHERE filters ROWS before grouping
-- HAVING filters GROUPS after grouping
```

---

### Q3. Window functions — ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD.

```sql
-- ROW_NUMBER: unique sequential number per partition
SELECT tracking_number, status, created_at,
  ROW_NUMBER() OVER (PARTITION BY tracking_number ORDER BY created_at DESC) as rn
FROM tracking_events;
-- Get latest event per shipment: WHERE rn = 1

-- RANK vs DENSE_RANK:
-- RANK: 1, 2, 2, 4 (skips after tie)
-- DENSE_RANK: 1, 2, 2, 3 (no skip)

-- Running total
SELECT date, amount,
  SUM(amount) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as running_total
FROM transactions;

-- LAG/LEAD: access previous/next row
SELECT date, amount,
  amount - LAG(amount) OVER (ORDER BY date) as daily_change
FROM daily_revenue;
```

---

### Q4. Subqueries vs CTEs (Common Table Expressions).

```sql
-- Subquery (inline)
SELECT * FROM shipments
WHERE customer_id IN (SELECT id FROM customers WHERE city = 'Memphis');

-- CTE (WITH clause) — more readable, reusable within query
WITH high_value_customers AS (
  SELECT customer_id, SUM(total) as total_spent
  FROM orders
  GROUP BY customer_id
  HAVING SUM(total) > 10000
)
SELECT c.name, hvc.total_spent
FROM customers c
JOIN high_value_customers hvc ON c.id = hvc.customer_id;

-- Recursive CTE (for hierarchical data)
WITH RECURSIVE org_chart AS (
  SELECT id, name, manager_id, 0 AS level FROM employees WHERE manager_id IS NULL
  UNION ALL
  SELECT e.id, e.name, e.manager_id, oc.level + 1
  FROM employees e JOIN org_chart oc ON e.manager_id = oc.id
)
SELECT * FROM org_chart;
```

---

## Scenario-Based Questions

### Q5. At FedEx, write a query to find the top 5 customers by shipment count in the last 30 days, along with their most recent shipment status.

```sql
WITH customer_stats AS (
  SELECT 
    c.id,
    c.name,
    COUNT(s.id) as shipment_count,
    MAX(s.created_at) as last_shipment_date
  FROM customers c
  JOIN shipments s ON c.id = s.customer_id
  WHERE s.created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
  GROUP BY c.id, c.name
  ORDER BY shipment_count DESC
  LIMIT 5
),
latest_shipment AS (
  SELECT DISTINCT ON (s.customer_id)
    s.customer_id,
    s.tracking_number,
    s.status
  FROM shipments s
  WHERE s.customer_id IN (SELECT id FROM customer_stats)
  ORDER BY s.customer_id, s.created_at DESC
)
SELECT 
  cs.name,
  cs.shipment_count,
  ls.tracking_number as latest_tracking,
  ls.status as latest_status
FROM customer_stats cs
JOIN latest_shipment ls ON cs.id = ls.customer_id
ORDER BY cs.shipment_count DESC;
```

---

### Q6. At NPCI, write a query to detect potential fraud — transactions where a user made more than 5 transactions in 10 minutes.

```sql
SELECT 
  t1.user_id,
  t1.transaction_time,
  COUNT(*) as txn_count_in_window,
  SUM(t2.amount) as total_amount
FROM transactions t1
JOIN transactions t2 
  ON t1.user_id = t2.user_id
  AND t2.transaction_time BETWEEN t1.transaction_time AND DATE_ADD(t1.transaction_time, INTERVAL 10 MINUTE)
GROUP BY t1.user_id, t1.transaction_time
HAVING COUNT(*) > 5
ORDER BY txn_count_in_window DESC;
```

**Alternative with window functions:**
```sql
WITH numbered AS (
  SELECT *,
    COUNT(*) OVER (
      PARTITION BY user_id 
      ORDER BY transaction_time 
      RANGE BETWEEN INTERVAL 10 MINUTE PRECEDING AND CURRENT ROW
    ) as txn_count_10min
  FROM transactions
)
SELECT * FROM numbered WHERE txn_count_10min > 5;
```

---

### Q7. At Hatio, write a query to calculate daily revenue with running total and day-over-day change.

```sql
SELECT 
  DATE(created_at) as date,
  COUNT(*) as order_count,
  SUM(total_amount) as daily_revenue,
  SUM(SUM(total_amount)) OVER (ORDER BY DATE(created_at)) as running_total,
  SUM(total_amount) - LAG(SUM(total_amount)) OVER (ORDER BY DATE(created_at)) as day_over_day_change,
  ROUND(
    (SUM(total_amount) - LAG(SUM(total_amount)) OVER (ORDER BY DATE(created_at))) 
    / LAG(SUM(total_amount)) OVER (ORDER BY DATE(created_at)) * 100, 2
  ) as pct_change
FROM orders
WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(created_at)
ORDER BY date;
```

---

## Coding Challenges

### Challenge 1: Complex Reporting Query
**File:** `solutions/ReportingQuery.sql`  
Write a query for a monthly sales report:
1. Total revenue, order count, avg order value per month
2. Top 3 products per month by revenue (use RANK window function)
3. Month-over-month growth percentage
4. Customer retention (customers who ordered in both current and previous month)

### Challenge 2: Data Cleanup Queries
**File:** `solutions/DataCleanup.sql`  
1. Find and delete duplicate rows (keep latest)
2. Find orphaned records (foreign key violations)
3. Update with JOIN (batch status update)
4. Find gaps in sequential IDs

---

## Gotchas & Edge Cases

### Q8. NULL handling — common pitfalls?

```sql
-- NULL comparisons ALWAYS return NULL (not true or false)
SELECT * FROM users WHERE email = NULL;    -- ❌ Returns nothing!
SELECT * FROM users WHERE email IS NULL;   -- ✅ Correct

-- NULL in aggregates
SELECT AVG(score) FROM reviews;  -- Ignores NULLs automatically
SELECT COUNT(score) FROM reviews; -- Ignores NULLs
SELECT COUNT(*) FROM reviews;    -- Counts all rows including NULLs

-- COALESCE for NULL defaults
SELECT name, COALESCE(phone, email, 'N/A') as contact FROM users;
```

---

### Q9. EXPLAIN and query optimization — what do you look for?

```sql
EXPLAIN SELECT * FROM shipments WHERE tracking_number = '123456';

-- Key indicators:
-- type: const (best) > eq_ref > ref > range > index > ALL (worst = full scan)
-- rows: estimated rows scanned (lower is better)
-- Extra: "Using index" (good), "Using filesort" (bad), "Using temporary" (bad)
```

**Red flags:** `type: ALL` (full table scan), high `rows` count, `Using filesort` on large tables.

---

### Q10. CASE WHEN — conditional logic in SQL.

```sql
-- Categorize shipments by weight:
SELECT tracking_number, weight,
  CASE
    WHEN weight < 1    THEN 'Light'
    WHEN weight < 10   THEN 'Medium'
    WHEN weight < 50   THEN 'Heavy'
    ELSE 'Freight'
  END AS weight_category
FROM shipments;

-- Pivot with CASE (rows → columns):
SELECT employee_id,
  SUM(CASE WHEN month = 'Jan' THEN sales ELSE 0 END) AS jan_sales,
  SUM(CASE WHEN month = 'Feb' THEN sales ELSE 0 END) AS feb_sales,
  SUM(CASE WHEN month = 'Mar' THEN sales ELSE 0 END) AS mar_sales
FROM monthly_sales
GROUP BY employee_id;

-- Conditional UPDATE:
UPDATE employees
SET salary = CASE
    WHEN department = 'Engineering' THEN salary * 1.15
    WHEN department = 'Sales'       THEN salary * 1.10
    ELSE salary * 1.05
END;
```

---

### Q11. EXISTS vs IN — when to use which?

```sql
-- EXISTS: checks if subquery returns ANY rows (stops at first match)
SELECT c.name FROM customers c
WHERE EXISTS (
    SELECT 1 FROM orders o WHERE o.customer_id = c.id AND o.total > 1000
);
-- ✅ Faster when outer table is SMALL, inner table is LARGE
-- ✅ Can use correlated subqueries
-- ✅ Handles NULLs correctly

-- IN: checks against a list of values
SELECT c.name FROM customers c
WHERE c.id IN (SELECT customer_id FROM orders WHERE total > 1000);
-- ✅ Faster when inner table is SMALL
-- ❌ Returns no rows if subquery has NULL (NULL IN (1,2,NULL) → UNKNOWN)

-- NOT EXISTS vs NOT IN:
SELECT c.name FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
-- ✅ ALWAYS prefer NOT EXISTS over NOT IN
-- ❌ NOT IN breaks if subquery returns ANY NULL:
--    WHERE id NOT IN (1, 2, NULL) → returns NOTHING (all rows UNKNOWN)
```

**Rule:** Use `EXISTS` for correlated subqueries, `IN` for small value lists. Always use `NOT EXISTS` over `NOT IN`.
