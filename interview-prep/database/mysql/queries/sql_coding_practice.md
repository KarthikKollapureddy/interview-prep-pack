# SQL Coding Practice — Top Interview Problems

> 15 most frequently asked SQL problems in coding rounds at product companies  
> Practice these on LeetCode Database section or HackerRank SQL track  
> Schema provided for each — write the query before checking the answer

---

## Problem 1: Second Highest Salary

**Schema:** `Employee(id, salary)`

Write a query to get the second highest salary. Return `null` if no second highest exists.

```sql
-- Solution 1: LIMIT + OFFSET
SELECT (
    SELECT DISTINCT salary FROM Employee
    ORDER BY salary DESC
    LIMIT 1 OFFSET 1
) AS SecondHighestSalary;

-- Solution 2: MAX with subquery
SELECT MAX(salary) AS SecondHighestSalary
FROM Employee
WHERE salary < (SELECT MAX(salary) FROM Employee);

-- Solution 3: DENSE_RANK (generalized for Nth)
SELECT salary AS SecondHighestSalary
FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM Employee
) ranked
WHERE rnk = 2
LIMIT 1;
```

---

## Problem 2: Nth Highest Salary (Generalized)

**Schema:** `Employee(id, salary)`  
Write a function to get the Nth highest salary.

```sql
-- Using DENSE_RANK:
SELECT DISTINCT salary AS NthHighestSalary
FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM Employee
) ranked
WHERE rnk = N;

-- Using LIMIT OFFSET:
SELECT DISTINCT salary AS NthHighestSalary
FROM Employee
ORDER BY salary DESC
LIMIT 1 OFFSET N-1;
```

---

## Problem 3: Department-wise Top 3 Earners

**Schema:** `Employee(id, name, salary, departmentId)`, `Department(id, name)`

Find employees who earn the top 3 salaries in each department.

```sql
SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
FROM (
    SELECT name, salary, departmentId,
           DENSE_RANK() OVER (PARTITION BY departmentId ORDER BY salary DESC) AS rnk
    FROM Employee
) e
JOIN Department d ON e.departmentId = d.id
WHERE e.rnk <= 3;
```

---

## Problem 4: Find Duplicate Emails

**Schema:** `Person(id, email)`

```sql
-- Solution 1: GROUP BY + HAVING
SELECT email FROM Person
GROUP BY email
HAVING COUNT(*) > 1;

-- Solution 2: Self JOIN
SELECT DISTINCT p1.email
FROM Person p1
JOIN Person p2 ON p1.email = p2.email AND p1.id != p2.id;
```

---

## Problem 5: Employees Earning More Than Their Manager

**Schema:** `Employee(id, name, salary, managerId)`

```sql
SELECT e.name AS Employee
FROM Employee e
JOIN Employee m ON e.managerId = m.id
WHERE e.salary > m.salary;
```

---

## Problem 6: Consecutive Numbers (3+ in a row)

**Schema:** `Logs(id, num)` — `id` is auto-increment

Find all numbers that appear at least 3 times consecutively.

```sql
-- Solution 1: Self JOIN
SELECT DISTINCT l1.num AS ConsecutiveNums
FROM Logs l1
JOIN Logs l2 ON l1.id = l2.id - 1
JOIN Logs l3 ON l2.id = l3.id - 1
WHERE l1.num = l2.num AND l2.num = l3.num;

-- Solution 2: LAG/LEAD
SELECT DISTINCT num AS ConsecutiveNums
FROM (
    SELECT num,
           LAG(num) OVER (ORDER BY id) AS prev_num,
           LEAD(num) OVER (ORDER BY id) AS next_num
    FROM Logs
) t
WHERE num = prev_num AND num = next_num;
```

---

## Problem 7: Running Total / Cumulative Sum

**Schema:** `Transactions(date, amount)`

```sql
SELECT date, amount,
       SUM(amount) OVER (ORDER BY date
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total
FROM Transactions;

-- Monthly running total:
SELECT date, amount,
       SUM(amount) OVER (PARTITION BY DATE_FORMAT(date, '%Y-%m')
           ORDER BY date) AS monthly_running_total
FROM Transactions;
```

---

## Problem 8: Customers Who Never Ordered

**Schema:** `Customers(id, name)`, `Orders(id, customerId)`

```sql
-- Solution 1: LEFT JOIN + NULL check
SELECT c.name AS Customers
FROM Customers c
LEFT JOIN Orders o ON c.id = o.customerId
WHERE o.id IS NULL;

-- Solution 2: NOT IN
SELECT name AS Customers
FROM Customers
WHERE id NOT IN (SELECT customerId FROM Orders);

-- Solution 3: NOT EXISTS (often fastest)
SELECT name AS Customers
FROM Customers c
WHERE NOT EXISTS (SELECT 1 FROM Orders o WHERE o.customerId = c.id);
```

---

## Problem 9: Rising Temperature (Compare with Previous Row)

**Schema:** `Weather(id, recordDate, temperature)`

Find all dates where the temperature was higher than the previous day.

```sql
-- Solution 1: Self JOIN with DATEDIFF
SELECT w1.id
FROM Weather w1
JOIN Weather w2 ON DATEDIFF(w1.recordDate, w2.recordDate) = 1
WHERE w1.temperature > w2.temperature;

-- Solution 2: LAG
SELECT id
FROM (
    SELECT id, temperature,
           LAG(temperature) OVER (ORDER BY recordDate) AS prev_temp
    FROM Weather
) t
WHERE temperature > prev_temp;
```

---

## Problem 10: Pivot / Conditional Aggregation

**Schema:** `Scores(student_id, subject, score)`

Show each student's scores as columns.

```sql
SELECT student_id,
       MAX(CASE WHEN subject = 'Math' THEN score END) AS Math,
       MAX(CASE WHEN subject = 'English' THEN score END) AS English,
       MAX(CASE WHEN subject = 'Science' THEN score END) AS Science
FROM Scores
GROUP BY student_id;
```

---

## Problem 11: Year-over-Year Growth

**Schema:** `Revenue(year, month, amount)`

Calculate month-over-month and year-over-year growth.

```sql
SELECT year, month, amount,
       -- Month-over-month growth
       ROUND((amount - LAG(amount) OVER (ORDER BY year, month))
             / LAG(amount) OVER (ORDER BY year, month) * 100, 2) AS mom_growth_pct,
       -- Year-over-year growth
       ROUND((amount - LAG(amount, 12) OVER (ORDER BY year, month))
             / LAG(amount, 12) OVER (ORDER BY year, month) * 100, 2) AS yoy_growth_pct
FROM Revenue;
```

---

## Problem 12: Delete Duplicate Rows (Keep Lowest ID)

**Schema:** `Person(id, email)`

```sql
-- Delete duplicates keeping the row with smallest id:
DELETE p1
FROM Person p1
JOIN Person p2
ON p1.email = p2.email AND p1.id > p2.id;
```

---

## Problem 13: Find Median Salary

**Schema:** `Employee(id, salary)`

```sql
-- Using ROW_NUMBER (works in MySQL 8+):
SELECT AVG(salary) AS MedianSalary
FROM (
    SELECT salary,
           ROW_NUMBER() OVER (ORDER BY salary) AS rn,
           COUNT(*) OVER () AS total
    FROM Employee
) t
WHERE rn IN (FLOOR((total + 1) / 2), CEIL((total + 1) / 2));
```

---

## Problem 14: Gaps in Sequential IDs

**Schema:** `Seat(id, student)`

Find missing IDs in a sequence.

```sql
-- Find gaps using generate_series equivalent:
SELECT t.id AS missing_id
FROM (
    SELECT @row := @row + 1 AS id
    FROM Seat, (SELECT @row := 0) r
    WHERE @row < (SELECT MAX(id) FROM Seat)
) t
LEFT JOIN Seat s ON t.id = s.id
WHERE s.id IS NULL;

-- MySQL 8+ with recursive CTE:
WITH RECURSIVE seq AS (
    SELECT 1 AS id
    UNION ALL
    SELECT id + 1 FROM seq WHERE id < (SELECT MAX(id) FROM Seat)
)
SELECT seq.id AS missing_id
FROM seq
LEFT JOIN Seat s ON seq.id = s.id
WHERE s.id IS NULL;
```

---

## Problem 15: Active Users (Retention) — Multi-day Login

**Schema:** `Logins(user_id, login_date)`

Find users who logged in for 3+ consecutive days.

```sql
SELECT DISTINCT user_id
FROM (
    SELECT user_id, login_date,
           login_date - INTERVAL ROW_NUMBER() OVER (
               PARTITION BY user_id ORDER BY login_date
           ) DAY AS grp
    FROM (SELECT DISTINCT user_id, login_date FROM Logins) t
) grouped
GROUP BY user_id, grp
HAVING COUNT(*) >= 3;
```

**Explanation:** The trick is: if dates are consecutive, `date - row_number` gives the same value. Group by that and count.

---

## Quick Reference — SQL Patterns for Interviews

| Pattern | Technique |
|---------|-----------|
| Nth highest | DENSE_RANK() or LIMIT OFFSET |
| Top N per group | DENSE_RANK() + PARTITION BY |
| Previous/Next row | LAG() / LEAD() |
| Running total | SUM() OVER (ORDER BY ... ROWS BETWEEN ...) |
| Consecutive | LAG/LEAD or self-join on id+1 |
| Find missing | LEFT JOIN + IS NULL or NOT EXISTS |
| Duplicates | GROUP BY + HAVING COUNT > 1 |
| Pivot | CASE WHEN + GROUP BY |
| Gaps in sequence | Recursive CTE or row number |
| Retention | Date - ROW_NUMBER trick for consecutive grouping |
