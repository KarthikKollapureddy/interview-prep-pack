# Database Scaling Patterns — Interview Q&A

> Concepts from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
> Covers: Replication, Sharding, Federation, Denormalization, SQL Tuning, SQL vs NoSQL, ACID vs BASE
> **Priority: P0** — Every system design interview involves database scaling decisions

---

## Q1. RDBMS — ACID Properties Explained.

```
ACID = Properties of relational database transactions

A — Atomicity:   Each transaction is ALL or NOTHING
                 If any part fails, entire transaction rolls back
                 Example: Bank transfer — debit AND credit must both happen

C — Consistency:  Transaction brings DB from one VALID state to another
                 All constraints, triggers, cascades are satisfied
                 Example: Foreign keys remain valid after transaction

I — Isolation:    Concurrent transactions produce same result as serial
                 Prevents dirty reads, phantom reads
                 Isolation levels: Read Uncommitted → Read Committed →
                                   Repeatable Read → Serializable

D — Durability:   Once committed, data survives crashes
                 Written to disk / WAL (Write-Ahead Log)
                 Example: After "COMMIT", data persists even if server crashes
```

---

## Q2. Master-Slave Replication — How does it work?

```
┌──────────┐      Replicates       ┌──────────┐
│  MASTER  │ ────────────────────→ │  SLAVE 1 │ (Read Only)
│ (R + W)  │                       └──────────┘
│          │      Replicates       ┌──────────┐
│          │ ────────────────────→ │  SLAVE 2 │ (Read Only)
└──────────┘                       └──────────┘

How it works:
  1. Master handles ALL writes AND reads
  2. Writes are replicated to slaves (async or sync)
  3. Slaves handle ONLY reads
  4. Slaves can replicate to additional slaves (tree)
  5. If master goes down → system is read-only until
     a slave is promoted or new master is provisioned

Use Case: Read-heavy workloads (90% reads, 10% writes)
  - 1 master for writes
  - 5 slaves for reads
  - Read QPS effectively 5x'd

Disadvantages:
  ✗ Replication lag: slave may serve stale data
  ✗ Write bottleneck: single master limits write throughput
  ✗ Promotion logic needed: promoting slave to master is complex
  ✗ Potential data loss if master fails before replication
  ✗ More replicas = more replication lag
```

---

## Q3. Master-Master Replication — When to use it?

```
┌──────────┐       Sync/Async      ┌──────────┐
│ MASTER 1 │ ←───────────────────→ │ MASTER 2 │
│ (R + W)  │                       │ (R + W)  │
└──────────┘                       └──────────┘

How it works:
  1. BOTH masters handle reads AND writes
  2. They coordinate/sync writes with each other
  3. If either goes down, other handles all traffic
  4. Better availability than master-slave

Use Case: Multi-region deployments
  - Master in US, Master in EU
  - Users write to nearest master
  - Data syncs between regions

Disadvantages:
  ✗ Need load balancer to determine which master to write to
  ✗ Loosely consistent (may violate ACID) OR increased write latency
  ✗ CONFLICT RESOLUTION is the big challenge:
    - What if both masters update the same row simultaneously?
    - Solutions: Last-write-wins, vector clocks, CRDTs
  ✗ More complex to set up and maintain

General Replication Disadvantages:
  ✗ Data loss risk if master fails before replicating
  ✗ Heavy writes → slaves lag behind
  ✗ More replicas = more lag
  ✗ Some systems: master writes in parallel, replicas write serially
```

---

## Q4. Federation (Functional Partitioning) — How does it scale?

```
Instead of ONE monolithic database, split by FUNCTION:

Before:
  ┌──────────────────────────────┐
  │     Single Database          │
  │  Users + Products + Orders   │
  │  + Forums + Analytics        │
  └──────────────────────────────┘

After (Federation):
  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │  Users   │  │ Products │  │  Orders  │
  │   DB     │  │   DB     │  │   DB     │
  └──────────┘  └──────────┘  └──────────┘

Benefits:
  ✓ Less read/write traffic per DB
  ✓ Less replication lag
  ✓ Smaller DBs → more fits in memory → better cache hits
  ✓ No single master serializing writes → write in parallel
  ✓ Can scale each DB independently

Disadvantages:
  ✗ Not effective if schema needs huge cross-function queries
  ✗ Application logic must know which DB to query
  ✗ Cross-database JOINs are complex (need application-level joins)
  ✗ More hardware and operational complexity
```

---

## Q5. Sharding — Strategies and Trade-offs.

```
Sharding = distributing data across multiple databases
Each database holds a SUBSET of the data

  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │ Shard 0  │  │ Shard 1  │  │ Shard 2  │  │ Shard 3  │
  │ Users    │  │ Users    │  │ Users    │  │ Users    │
  │ A-F      │  │ G-L      │  │ M-R      │  │ S-Z      │
  └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

### Sharding Strategies
```
1. Range-Based Sharding:
   - Partition by value range (A-F, G-L, etc.)
   - Simple to implement
   - Risk: uneven distribution (hotspots)

2. Hash-Based Sharding:
   - hash(key) % num_shards = shard_id
   - More even distribution
   - Hard to add/remove shards (all data reshuffles)

3. Consistent Hashing:
   - Nodes arranged on a hash ring
   - Each key maps to nearest node clockwise
   - Adding/removing a node only affects neighbors
   - Used by: Cassandra, DynamoDB, Memcached

4. Directory-Based Sharding:
   - Lookup table maps key → shard
   - Most flexible
   - Lookup table becomes bottleneck & SPOF

5. Geographic Sharding:
   - Partition by user location
   - Reduces latency for users
   - Complex for users who travel
```

### Sharding Benefits
```
  ✓ Less read/write traffic per shard
  ✓ Less replication per shard
  ✓ More cache hits (smaller dataset per shard)
  ✓ Smaller indexes = faster queries
  ✓ If one shard fails, others still work
  ✓ Write in parallel across shards
```

### Sharding Disadvantages
```
  ✗ Application logic must know which shard to query
  ✗ Complex SQL queries (cross-shard JOINs are very hard)
  ✗ Data can become LOPSIDED (hot shards)
     - Power users on one shard → uneven load
     - Rebalancing is painful
  ✗ JOINing across shards = application-level joins
  ✗ More hardware + operational complexity
  ✗ Consistent hashing can help with rebalancing
```

---

## Q6. Denormalization — When to sacrifice normal forms?

```
Denormalization = add redundant data to avoid expensive JOINs
Trade-off: faster reads, slower/more complex writes

Before (Normalized):
  Users: {id, name, email}
  Orders: {id, user_id, product_id, amount}
  Products: {id, name, price}

  Query "User's orders with product names":
    SELECT u.name, p.name, o.amount
    FROM orders o
    JOIN users u ON o.user_id = u.id
    JOIN products p ON o.product_id = p.id
    -- 3-way JOIN, expensive at scale

After (Denormalized):
  Orders: {id, user_id, user_name, product_id, product_name, amount}

  Query: SELECT user_name, product_name, amount FROM orders
  -- No JOINs, much faster!

When to denormalize:
  ✓ Read-heavy workloads (100:1 or 1000:1 read:write ratio)
  ✓ After federation/sharding (cross-DB joins are impossible)
  ✓ Performance-critical queries
  ✓ Reporting/analytics tables

Tools:
  - Materialized Views (PostgreSQL, Oracle): auto-maintain denormalized copies
  - Change Data Capture: stream changes to denormalized tables

Disadvantages:
  ✗ Data duplication → more storage
  ✗ Must keep redundant copies in sync (constraints, triggers)
  ✗ Write performance may decrease
  ✗ More complex application logic
```

---

## Q7. SQL Tuning — Key techniques.

```
Benchmark first, then optimize:
  - Benchmark: simulate load with tools like ab, JMeter, wrk
  - Profile: enable slow query log, use EXPLAIN ANALYZE

1. TIGHTEN THE SCHEMA:
   ┌──────────────────────────────────────────────────────┐
   │ Use CHAR for fixed-length (fast random access)       │
   │ Use VARCHAR for variable-length                       │
   │ Use TEXT for large text (stores pointer on disk)      │
   │ Use INT for numbers up to 2^32 (4 billion)           │
   │ Use DECIMAL for currency (no float errors!)           │
   │ Don't store BLOBs, store their location instead       │
   │ Set NOT NULL where possible (improves search perf)    │
   │ VARCHAR(255) maximizes a byte (8-bit count)           │
   └──────────────────────────────────────────────────────┘

2. USE GOOD INDICES:
   - Index columns used in WHERE, JOIN, ORDER BY, GROUP BY
   - Indices are B-trees: O(log n) for search, insert, delete
   - Trade-off: faster reads, slower writes (index must update)
   - For bulk loads: disable indices → load → rebuild

3. AVOID EXPENSIVE JOINS:
   - Denormalize where performance demands it
   - Use materialized views for complex aggregations

4. PARTITION TABLES:
   - Move hot data to separate table (keep it in memory)
   - Range partitioning by date is common
   - Example: orders_2024, orders_2025

5. TUNE QUERY CACHE:
   - MySQL query cache can help OR hurt
   - Invalidated on any write to the table
   - Better to use application-level caching (Redis)
```

---

## Q8. NoSQL Database Types — When to use which?

### Key-Value Store
```
Abstraction: Hash Table
  - O(1) reads and writes
  - Backed by memory or SSD
  - Simple: key → value (blob)
  - Examples: Redis, Memcached, DynamoDB

Use when:
  ✓ Simple data model (cache, sessions, counters)
  ✓ High throughput, low latency required
  ✓ Rapidly changing data

Limitations:
  ✗ Limited query capabilities
  ✗ No complex relationships
  ✗ Application must handle all logic
```

### Document Store
```
Abstraction: Key-Value store with structured documents as values
  - Documents: JSON, XML, BSON
  - Query by internal document structure
  - Collections organize documents
  - Examples: MongoDB, CouchDB, DynamoDB

Use when:
  ✓ Semi-structured data
  ✓ Flexible schema (fields vary per document)
  ✓ Nested objects / embedded documents
  ✓ Rapidly evolving schema

Example Document:
  {
    "user_id": "u123",
    "name": "Karthik",
    "addresses": [
      {"type": "home", "city": "Hyderabad"},
      {"type": "work", "city": "Bangalore"}
    ],
    "preferences": {"theme": "dark", "lang": "en"}
  }
```

### Wide Column Store
```
Abstraction: Nested Map → ColumnFamily<RowKey, Columns<ColKey, Value, Timestamp>>
  - Data organized in column families (like SQL tables)
  - Each row can have different columns
  - Timestamps for versioning
  - Examples: Cassandra, HBase, BigTable

Use when:
  ✓ MASSIVE datasets (petabytes)
  ✓ High write throughput
  ✓ Time-series data
  ✓ IoT sensor data
  ✓ Need geographic distribution

Characteristics:
  - Lexicographic key ordering → efficient range queries
  - Designed for horizontal scaling
  - Eventual consistency by default
```

### Graph Database
```
Abstraction: Graph (nodes + edges)
  - Each node = record, each edge = relationship
  - Optimized for traversing relationships
  - Examples: Neo4j, Amazon Neptune, FlockDB

Use when:
  ✓ Complex relationships (many-to-many)
  ✓ Social networks (friends, followers, recommendations)
  ✓ Knowledge graphs
  ✓ Fraud detection (pattern matching)
  ✓ Recommendation engines

Limitations:
  ✗ Relatively new, less tooling
  ✗ Not great for simple CRUD operations
  ✗ Often accessed via REST APIs only
```

---

## Q9. SQL vs NoSQL — Decision Guide.

```
┌─────────────────────┬──────────────────────┬──────────────────────┐
│ Criteria            │ SQL (RDBMS)          │ NoSQL                │
├─────────────────────┼──────────────────────┼──────────────────────┤
│ Data Structure      │ Structured, tabular  │ Semi/unstructured    │
│ Schema              │ Fixed, strict        │ Dynamic, flexible    │
│ Relationships       │ Complex JOINs        │ Denormalized, nested │
│ Transactions        │ ACID                 │ BASE (eventual)      │
│ Scaling             │ Vertical (+ shards)  │ Horizontal (native)  │
│ Query Language      │ SQL (standardized)   │ Varies per DB        │
│ Throughput          │ Moderate             │ Very high (for IOPS) │
│ Data Size           │ GBs to low TBs      │ TBs to PBs          │
│ Maturity            │ 40+ years, proven    │ ~15 years, evolving  │
│ Examples            │ MySQL, PostgreSQL    │ MongoDB, Cassandra   │
│                     │ Oracle, SQL Server   │ Redis, DynamoDB      │
└─────────────────────┴──────────────────────┴──────────────────────┘
```

### ACID vs BASE
```
ACID (SQL):                         BASE (NoSQL):
  Atomicity                          Basically Available
  Consistency                        Soft state
  Isolation                          Eventual consistency
  Durability

ACID → Strong consistency, slower    BASE → High availability, faster
```

### When to use NoSQL
```
✓ Rapid ingest: clickstream, log data
✓ Leaderboards / scoring data
✓ Shopping carts (temporary, high velocity)
✓ Frequently accessed "hot" tables
✓ Metadata / lookup tables
✓ Time-series data (IoT, metrics)
✓ Content management (varying schemas)
✓ Real-time analytics
```

### When to use SQL
```
✓ Financial transactions (need ACID)
✓ Complex relationships (JOINs)
✓ Reporting with complex aggregations
✓ Well-defined schema that rarely changes
✓ Need for referential integrity
```

---

## Q10. Real-World Database Architecture Patterns.

```
Pattern 1: CQRS (Command Query Responsibility Segregation)
  Writes → Master (SQL) → Event → Read Store (NoSQL/Elasticsearch)
  Reads  → Read Store (optimized for queries)

Pattern 2: Polyglot Persistence
  User data    → PostgreSQL (structured, relational)
  Sessions     → Redis (key-value, fast)
  Product catalog → MongoDB (flexible schema)
  Search       → Elasticsearch (full-text)
  Analytics    → Cassandra (time-series, high write)
  Relationships → Neo4j (graph traversal)

Pattern 3: Write-Ahead Log (WAL)
  1. Write to WAL (append-only log file)
  2. Acknowledge write to client
  3. Asynchronously apply to actual data structures
  Used by: PostgreSQL, MySQL InnoDB, Cassandra

Pattern 4: LSM Tree (Log-Structured Merge Tree)
  1. Writes go to in-memory buffer (memtable)
  2. When full, flush to disk as sorted file (SSTable)
  3. Background merge of SSTables
  Good for: Write-heavy workloads
  Used by: Cassandra, RocksDB, LevelDB

Interview tip: "We'd use polyglot persistence — right tool for each job.
  User profiles in PostgreSQL for ACID, sessions in Redis for speed,
  and search indices in Elasticsearch."
```

---

*Source: Concepts synthesized from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer) and [karanpratapsingh/system-design](https://github.com/karanpratapsingh/system-design)*
