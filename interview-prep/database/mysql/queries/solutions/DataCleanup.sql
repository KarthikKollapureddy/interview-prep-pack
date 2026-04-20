-- Challenge 2: Data Cleanup Queries
-- 
-- 1. Find and delete duplicate rows (keep latest)
-- 2. Find orphaned records (foreign key violations)
-- 3. Update with JOIN (batch status update)
-- 4. Find gaps in sequential IDs

-- ========================================
-- Part 1: Find and Delete Duplicates (keep latest)
-- ========================================

-- Step 1: Identify duplicates
SELECT email, COUNT(*) as cnt
FROM users
GROUP BY email
HAVING COUNT(*) > 1;

-- Step 2: Delete duplicates, keeping the row with MAX id (latest)
DELETE u1 FROM users u1
INNER JOIN users u2 
    ON u1.email = u2.email 
    AND u1.id < u2.id;
-- This deletes the older duplicate (smaller id)

-- Alternative using CTE (MySQL 8+):
WITH duplicates AS (
    SELECT id, email,
        ROW_NUMBER() OVER (PARTITION BY email ORDER BY id DESC) AS rn
    FROM users
)
DELETE FROM users WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);

-- ========================================
-- Part 2: Find Orphaned Records
-- ========================================

-- Orders referencing non-existent customers
SELECT o.id, o.customer_id
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Order items referencing non-existent orders
SELECT oi.id, oi.order_id
FROM order_items oi
LEFT JOIN orders o ON oi.order_id = o.id
WHERE o.id IS NULL;

-- Clean up orphans
DELETE oi FROM order_items oi
LEFT JOIN orders o ON oi.order_id = o.id
WHERE o.id IS NULL;

-- ========================================
-- Part 3: Update with JOIN (batch status update)
-- ========================================

-- Mark orders as 'EXPIRED' if pending > 24 hours and payment not received
UPDATE orders o
JOIN payments p ON o.id = p.order_id
SET o.status = 'EXPIRED'
WHERE o.status = 'PENDING'
  AND o.created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)
  AND p.status != 'SUCCESS';

-- Update product stock from a batch import table
UPDATE products p
JOIN inventory_updates iu ON p.id = iu.product_id
SET p.stock_quantity = p.stock_quantity + iu.quantity_change,
    p.updated_at = NOW()
WHERE iu.batch_id = 'BATCH-2024-001'
  AND iu.processed = FALSE;

-- ========================================
-- Part 4: Find Gaps in Sequential IDs
-- ========================================

-- Method 1: Self-join to find gaps
SELECT (t1.id + 1) AS gap_start, 
       (MIN(t2.id) - 1) AS gap_end
FROM orders t1
JOIN orders t2 ON t2.id > t1.id
GROUP BY t1.id
HAVING gap_start < MIN(t2.id);

-- Method 2: Using LEAD (MySQL 8+, more elegant)
WITH id_gaps AS (
    SELECT id,
        LEAD(id) OVER (ORDER BY id) AS next_id
    FROM orders
)
SELECT id + 1 AS gap_start,
       next_id - 1 AS gap_end,
       next_id - id - 1 AS gap_size
FROM id_gaps
WHERE next_id - id > 1
ORDER BY gap_start;

-- Method 3: Generate full sequence and find missing (small tables only)
WITH RECURSIVE seq AS (
    SELECT MIN(id) AS n FROM orders
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < (SELECT MAX(id) FROM orders)
)
SELECT s.n AS missing_id
FROM seq s
LEFT JOIN orders o ON s.n = o.id
WHERE o.id IS NULL;
