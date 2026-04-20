-- Challenge 1: Complex Monthly Sales Report
-- 
-- 1. Total revenue, order count, avg order value per month
-- 2. Top 3 products per month by revenue (RANK window function)
-- 3. Month-over-month growth percentage
-- 4. Customer retention (ordered in both current and previous month)

-- ========================================
-- Part 1: Monthly Summary
-- ========================================
SELECT 
    DATE_FORMAT(o.created_at, '%Y-%m') AS month,
    COUNT(DISTINCT o.id) AS order_count,
    SUM(oi.quantity * oi.unit_price) AS total_revenue,
    ROUND(AVG(o.total_amount), 2) AS avg_order_value
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE o.status = 'COMPLETED'
GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
ORDER BY month;

-- ========================================
-- Part 2: Top 3 Products Per Month (RANK)
-- ========================================
WITH monthly_product_revenue AS (
    SELECT 
        DATE_FORMAT(o.created_at, '%Y-%m') AS month,
        p.id AS product_id,
        p.name AS product_name,
        SUM(oi.quantity * oi.unit_price) AS revenue,
        DENSE_RANK() OVER (
            PARTITION BY DATE_FORMAT(o.created_at, '%Y-%m')
            ORDER BY SUM(oi.quantity * oi.unit_price) DESC
        ) AS rank_in_month
    FROM orders o
    JOIN order_items oi ON o.id = oi.order_id
    JOIN products p ON oi.product_id = p.id
    WHERE o.status = 'COMPLETED'
    GROUP BY DATE_FORMAT(o.created_at, '%Y-%m'), p.id, p.name
)
SELECT month, product_name, revenue, rank_in_month
FROM monthly_product_revenue
WHERE rank_in_month <= 3
ORDER BY month, rank_in_month;

-- ========================================
-- Part 3: Month-over-Month Growth %
-- ========================================
WITH monthly_totals AS (
    SELECT 
        DATE_FORMAT(created_at, '%Y-%m') AS month,
        SUM(total_amount) AS revenue
    FROM orders
    WHERE status = 'COMPLETED'
    GROUP BY DATE_FORMAT(created_at, '%Y-%m')
)
SELECT 
    month,
    revenue,
    LAG(revenue) OVER (ORDER BY month) AS prev_month_revenue,
    ROUND(
        (revenue - LAG(revenue) OVER (ORDER BY month)) 
        / LAG(revenue) OVER (ORDER BY month) * 100, 2
    ) AS growth_pct
FROM monthly_totals
ORDER BY month;

-- ========================================
-- Part 4: Customer Retention (ordered in both current and previous month)
-- ========================================
WITH monthly_customers AS (
    SELECT DISTINCT 
        customer_id,
        DATE_FORMAT(created_at, '%Y-%m') AS month
    FROM orders
    WHERE status = 'COMPLETED'
)
SELECT 
    curr.month,
    COUNT(DISTINCT curr.customer_id) AS total_customers,
    COUNT(DISTINCT prev.customer_id) AS retained_customers,
    ROUND(
        COUNT(DISTINCT prev.customer_id) * 100.0 / COUNT(DISTINCT curr.customer_id), 2
    ) AS retention_rate_pct
FROM monthly_customers curr
LEFT JOIN monthly_customers prev 
    ON curr.customer_id = prev.customer_id
    AND prev.month = DATE_FORMAT(DATE_SUB(STR_TO_DATE(CONCAT(curr.month, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH), '%Y-%m')
GROUP BY curr.month
ORDER BY curr.month;
