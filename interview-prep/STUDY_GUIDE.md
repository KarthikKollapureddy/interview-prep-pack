# Study Guide — Where to Start & How to Proceed

> **Master navigation guide** for the entire interview-prep repo.  
> Follow this section-by-section to cover all technical interview rounds systematically.

---

## Overview: Interview Round Types & What This Repo Covers

```
Product company interviews typically have 4-6 rounds:

Round 1: Online Assessment (OA)
  → DSA coding problems (2-3 medium/hard)
  → Covered by: dsa_patterns/

Round 2: Technical Screening (Phone/Video, 45-60 min)
  → Core Java, Spring Boot, SQL, quick system design
  → Covered by: backend/java/*, backend/spring_boot/*, database/*

Round 3: DSA + Problem Solving (45-60 min)
  → Live coding with interviewer
  → Covered by: dsa_patterns/, coding challenge solutions/

Round 4: System Design — HLD (45-60 min)
  → Design a large-scale system from scratch
  → Covered by: system_design/hld/

Round 5: System Design — LLD / Machine Coding (45-60 min)
  → Design classes, write code for a small system
  → Covered by: system_design/lld/

Round 6: Behavioral / Culture Fit (30-45 min)
  → STAR stories, leadership principles
  → Covered by: behavioral/, INTERVIEW_PLAYBOOK.md

Hiring Manager Round (30-45 min)
  → Mix of technical + behavioral + "why this company"
  → Covered by: GENERIC_PRODUCT_QUESTIONS.md, behavioral/
```

---

## Recommended Study Plan (4-Week Sprint)

### Week 1: Java + Spring Foundations
| Day | Topic | Path | What to Do |
|-----|-------|------|------------|
| 1 | Java Streams | `backend/java/streams/qa.md` | Read all Qs → code challenges |
| 2 | OOP & SOLID | `backend/java/oops/qa.md` | Focus on SOLID examples |
| 3 | Collections Internals | `backend/java/collections/qa.md` | HashMap internals, TreeMap, ConcurrentHashMap, SequencedCollections |
| 4 | Multithreading | `backend/java/multithreading/qa.md` | ExecutorService, CompletableFuture, Fork/Join, Virtual Threads |
| 5 | Java 8→21 Features | `backend/java/java_versions/java_8_to_21_features.md` | Records, sealed classes, switch patterns, virtual threads |
| 6 | Java Internals + Tricky Output | `java_internals/qa.md` + `java_advanced_internals.md` + `backend/java/tricky_output/qa.md` | JVM memory, GC, String pool, Reflection, NIO, SPI + "What's the output?" drills |
| 7 | **Revision + 1 HLD problem** | Review weak areas | Practice 1 system design problem |

### Week 2: Spring Boot + Microservices + Design Patterns
| Day | Topic | Path | What to Do |
|-----|-------|------|------------|
| 1 | Spring Core + Adv. Annotations | `backend/spring_boot/core_concepts/qa.md` + `advanced_annotations/qa.md` + `spring_advanced.md` | IoC, DI, @Async, @Cacheable, @Retryable, AOP deep dive, Spring Cloud |
| 2 | REST APIs + API Design | `backend/spring_boot/rest_apis/qa.md` + `backend/api_design/qa.md` | REST best practices, idempotency, pagination, rate limiting |
| 3 | Hibernate & JPA | `backend/hibernate_jpa/qa.md` | N+1, caching, entity lifecycle, @Transactional |
| 4 | Security & OAuth2 | `backend/spring_boot/security_oauth2/qa.md` | JWT, OAuth2 flow, Spring Security |
| 5 | App Security (OWASP) | `fundamentals/security/qa.md` | OWASP Top 10, XSS, CSRF, SQL Injection |
| 6 | Testing + Performance | `backend/spring_boot/testing/qa.md` + `backend/performance_tuning/qa.md` | JUnit5, Mockito + JVM tuning, profiling |
| 7 | MS Patterns + Kafka + Observability | `backend/microservices/design_patterns/qa.md` + `backend/observability/qa.md` | Saga, CQRS, Kafka internals + ELK, tracing |

### Week 2.5: Advanced Spring + AWS (if time permits)
| Day | Topic | Path | What to Do |
|-----|-------|------|------------|
| 1 | WebFlux & Reactive | `backend/spring_boot/webflux_reactive/qa.md` | Mono/Flux, WebClient, HTTP clients comparison |
| 2 | AWS Services Breadth | `devops/aws/services_overview/qa.md` | SQS/SNS, ECS/EKS, RDS/DynamoDB, IAM |
| 3 | Elasticsearch | `backend/elasticsearch/qa.md` | Inverted index, Query DSL, Spring Data ES |
| 4 | Spring Batch | `backend/spring_batch/qa.md` | Job/Step, Reader/Writer, partitioning |

### Week 3: DSA + Database + DevOps
| Day | Topic | Path | What to Do |
|-----|-------|------|------------|
| 1 | Arrays + Two Pointers | `dsa_patterns/dsa_approach_guide.md` + `qa.md` | Read approach guide templates FIRST → First 10 problems |
| 2 | Sliding Window + Stack | `dsa_patterns/qa.md` | Problems 11-20 |
| 3 | Binary Search + Trees | `dsa_patterns/qa.md` | Problems 21-30 |
| 4 | Graphs + DP | `dsa_patterns/qa.md` | Problems 31-40 |
| 5 | MySQL + SQL + Schema Design | `database/mysql/*/qa.md` + `sql_coding_practice.md` + `database/schema_design/qa.md` | Complex queries, window functions, 15 classic problems, ER design |
| 6 | MongoDB + Transactions | `database/mongodb/qa.md` | Aggregation, sharding |
| 7 | Docker + CI/CD + Git | `devops/*/qa.md` | Dockerfile, K8s, Jenkins, Git workflow |

### Week 4: System Design + Behavioral + Fundamentals + Mock
| Day | Topic | Path | What to Do |
|-----|-------|------|------------|
| 1 | HLD Deep Dive 1 | `system_design/hld/estimation_cheatsheet.md` + `distributed_systems_fundamentals.md` + `end_to_end_designs.md` | **Estimation formulas FIRST** → Distributed systems fundamentals → URL Shortener + Food Delivery |
| 2 | HLD Deep Dive 2 | `system_design/hld/database_scaling.md` + `caching_deep_dive.md` + `end_to_end_designs.md` | Database scaling + caching patterns → Uber + Netflix |
| 3 | HLD Deep Dive 3 | `system_design/hld/async_and_messaging.md` + `communication_protocols.md` + `end_to_end_designs.md` | Async patterns + protocols → E-Commerce + qa.md problems |
| 4 | LLD Deep Dive 1 | `system_design/lld/end_to_end_designs.md` | Parking Lot + Library |
| 5 | LLD Deep Dive 2 | `system_design/lld/end_to_end_designs.md` | Snake & Ladder + Hotel + Vending |
| 6 | JS Core + TypeScript + Networking | `frontend/javascript_core/qa.md` + `frontend/typescript/qa.md` + `frontend/web_performance/qa.md` + `fundamentals/networking/qa.md` | Closures, event loop, TS types + Core Web Vitals, SSR/SSG, a11y + HTTP, DNS |
| 7 | Behavioral + **Mock** | `behavioral/qa.md` | STAR stories, practice answers aloud |

---

## Section-by-Section Guide: Where to Start & How to Proceed

### 1. Backend — Java Core (`backend/java/`)

```
📁 backend/java/
├── streams/        ← START HERE (asked in every interview)
├── oops/           ← Then this (SOLID principles)
├── collections/    ← Then this (HashMap internals are #1 asked)
├── multithreading/ ← Then this (P0 gap — practice thoroughly)
├── java8_features/ ← Then this
└── exceptions/     ← Quick review (asked briefly)
```

**How to study each:**
1. Open `qa.md` → Read Q1 conceptual question, think about your answer
2. Read the provided answer → note anything you didn't know
3. Move to scenario questions → these simulate real interview follow-ups
4. Attempt coding challenges in `solutions/` → code yourself first
5. Compare with reference solutions (where provided)

**Key files with solutions already filled:**
- `collections/solutions/LRUCache.java` — must-know for every interview
- `collections/solutions/CustomHashMap.java` — Amazon/Google favorite
- `multithreading/solutions/ProducerConsumer.java` — classic concurrency

---

### 2. Backend — Spring Boot (`backend/spring_boot/`)

```
📁 backend/spring_boot/
├── core_concepts/    ← START (IoC, DI, Bean lifecycle)
├── rest_apis/        ← Then (REST best practices)
├── security_oauth2/  ← Then (JWT flow — always asked)
└── testing/          ← Then (JUnit5 — P0 gap for FedEx)
```

**Focus areas by company:**
- **FedEx:** JUnit5, Spring Cloud Gateway, REST API design
- **NPCI:** Security, transaction management, resilience
- **General:** Bean lifecycle, profiles, auto-configuration

---

### 3. Backend — Microservices (`backend/microservices/`)

```
📁 backend/microservices/
├── design_patterns/  ← START (Saga, CQRS, API Gateway)
├── kafka/            ← Then (Kafka internals, consumer groups)
├── resilience/       ← Then (Circuit Breaker — solution provided!)
└── service_mesh/     ← Then (Spring Cloud Gateway)
```

**Key solution:** `resilience/solutions/CircuitBreakerImpl.java` — full state machine implementation.

---

### 4. Backend — Design Patterns (`backend/design_patterns/`)

```
📁 backend/design_patterns/
└── qa.md  ← 15 GoF patterns with full Java code

Study order (most-asked first):
  1. Singleton (4 implementations — enum is best)
  2. Factory & Abstract Factory
  3. Builder (Lombok @Builder)
  4. Strategy (payment, pricing, sorting)
  5. Observer (Spring events)
  6. Decorator (I/O streams, pricing layers)
  7. Adapter, Proxy, Facade, Template Method
  8. Chain of Responsibility, Command, State
```

---

### 5. Backend — Caching & Redis (`backend/caching_redis/`)

```
📁 backend/caching_redis/
└── qa.md  ← 12 questions on caching strategies + Redis deep dive

Focus: Cache-aside vs Read-through, Redis data structures,
distributed locking, rate limiting with Redis.
```

---

### 6. Frontend (`frontend/`)

```
📁 frontend/
├── react/
│   ├── hooks/             ← START (useState, useEffect, custom hooks)
│   ├── state_management/  ← Then (Redux, Context API)
│   ├── performance/       ← Then (memo, useMemo, code splitting)
│   └── testing/           ← Then (React Testing Library)
└── angular/
    ├── components/        ← START (lifecycle, forms)
    └── rxjs/              ← Then (observables, operators)
```

---

### 7. Database (`database/`)

```
📁 database/
├── mysql/
│   ├── queries/       ← START (JOINs, subqueries, window functions)
│   ├── indexing/       ← Then (B-Tree, composite index, EXPLAIN)
│   └── transactions/  ← Then (ACID, isolation levels, deadlocks)
└── mongodb/           ← Then (aggregation, sharding, replication)
```

**Key solutions provided:**
- `mysql/queries/solutions/ReportingQuery.sql` — window functions, CTEs
- `mysql/queries/solutions/DataCleanup.sql` — dedup, orphans, gaps

---

### 8. DevOps (`devops/`)

```
📁 devops/
├── aws/
│   ├── s3_cloudfront/  ← START (S3, CloudFront, Route53)
│   └── lambda/         ← Then (serverless patterns)
├── docker_kubernetes/  ← Then (Dockerfile, K8s pods, services)
├── ci_cd/              ← Then (Jenkins pipelines)
└── git/                ← Then (branching strategies, workflows)
```

---

### 9. System Design — HLD (`system_design/hld/`)

```
📁 system_design/hld/
├── qa.md                  ← Quick Q&A format (12 problems)
├── end_to_end_designs.md  ← DEEP DIVES (5 full designs) ⭐
└── solutions/
```

**How to study HLD:**
1. First read the framework in `qa.md` (5-step approach)
2. Pick one design from `end_to_end_designs.md`
3. **Try it yourself first** — draw on paper for 30 min
4. Then read the solution and compare
5. Focus on TRADE-OFFS — that's what interviewers test
6. Practice explaining out loud (time yourself: 35-40 min)

**Study order for designs:**
```
Start with these (most commonly asked):
  1. URL Shortener (end_to_end_designs.md) — classic, tests fundamentals
  2. E-Commerce / Amazon (end_to_end_designs.md) — comprehensive
  3. Food Delivery / Swiggy (end_to_end_designs.md) — real-time + matching
  
Then these (senior-level):
  4. Uber / Ride-sharing (end_to_end_designs.md) — geospatial + surge
  5. Netflix / Video (end_to_end_designs.md) — CDN + transcoding
  
Also review (from qa.md):
  6. Package Tracking (FedEx-specific!) — qa.md Q1
  7. UPI Payment (NPCI-specific!) — qa.md Q2
  8. Chat System (WhatsApp) — qa.md Q9
  9. Notification Service — qa.md Q3
  10. Distributed Cache — qa.md Q12
```

---

### 10. System Design — LLD (`system_design/lld/`)

```
📁 system_design/lld/
├── qa.md                  ← Design patterns + quick LLD problems (13 problems)
├── end_to_end_designs.md  ← DEEP DIVES (5+1 full designs) ⭐
└── solutions/
```

**How to study LLD:**
1. First master SOLID principles (qa.md Q1)
2. Know top 5 patterns: Strategy, Observer, Factory, State, Builder
3. Pick one design from `end_to_end_designs.md`
4. **Code it yourself** — open IDE, write classes, test
5. Compare with reference → check pattern choices, thread safety
6. Time yourself: 45 min for full design + working code

**Study order for designs:**
```
Start with these (most commonly asked):
  1. Parking Lot (end_to_end_designs.md) — classic, every company asks
  2. Vending Machine (end_to_end_designs.md) — State pattern showcase
  3. Snake & Ladder (end_to_end_designs.md) — Game design, fun question
  
Then these:
  4. Library Management (end_to_end_designs.md) — Observer, state, reservation
  5. Hotel Booking (end_to_end_designs.md) — Date overlap, Strategy for cancellation
  6. ATM Machine (end_to_end_designs.md bonus) — State pattern, cash dispensing
  
Also review (from qa.md):
  7. Rate Limiter — qa.md Q7 (Token Bucket algorithm)
  8. LRU Cache — qa.md Q8 (HashMap + DLL)
  9. Elevator System — qa.md Q11
  10. Splitwise — qa.md Q12
  11. BookMyShow — qa.md Q13 (seat locking with Redis)
```

---

### 11. DSA & Coding (`dsa_patterns/`)

```
📁 dsa_patterns/
├── README.md
├── qa.md                    ← 50 curated problems by pattern
├── dsa_approach_guide.md    ← 🆕 Pattern templates + complexity cheat sheet (from kdn251/interviews)
└── solutions/
```

**Study by pattern (not by problem number):**
```
Week 1: Arrays & Hashing → Two Pointers → Sliding Window
Week 2: Stack → Binary Search → Trees (BFS/DFS)
Week 3: Graphs (BFS/DFS, Dijkstra) → Dynamic Programming (1D, 2D)
Week 4: Backtracking → Tries → Intervals → Revision

Per problem:
  1. Read dsa_approach_guide.md FIRST → learn the pattern template
  2. Read problem → think 5 min → identify which pattern applies
  3. Code solution using template as skeleton → test with examples
  4. If stuck > 20 min → read hint/solution → re-code from memory next day
```

**NEW: `dsa_approach_guide.md` covers:**
- Pattern recognition framework (data structure → algorithm mapping)
- Java templates for all 10 patterns (Two Pointers, Sliding Window, Binary Search, DP, Graph BFS/DFS, Trees, Heap, Backtracking, Intervals)
- Top 3-6 classic problems per pattern with approach & key insight
- Complexity cheat sheet (time + space for every algorithm)
- Common mistakes per pattern

---

### 12. Behavioral (`behavioral/`)

```
📁 behavioral/
├── README.md
└── qa.md      ← 20+ STAR stories, HR questions
```

**Prepare these stories (minimum 5):**
1. Biggest technical challenge you solved
2. Time you disagreed with a teammate
3. A project that failed and what you learned
4. How you handled a tight deadline
5. A time you mentored someone or took initiative

---

### 13. Cross-Cutting Resources

| File | When to Use |
|------|-------------|
| `INTERVIEW_PLAYBOOK.md` | Day before interview — strategy, traps, negotiation |
| `GENERIC_PRODUCT_QUESTIONS.md` | Final revision — 50 top questions across all topics |
| `INSTRUCTIONS.md` | Reference — your skill gaps, company profiles |

---

### 14. Hibernate & JPA (`backend/hibernate_jpa/`)

```
📁 backend/hibernate_jpa/
└── qa.md  ← 15 questions on entity lifecycle, N+1, caching, @Transactional

Study order:
  1. Entity lifecycle (Transient → Managed → Detached → Removed)
  2. N+1 problem + 5 fix strategies
  3. Lazy vs Eager loading
  4. L1/L2 caching architecture
  5. @Transactional internals (propagation, self-invocation gotcha)
  6. Spring Data JPA repository hierarchy
```

---

### 15. JavaScript Core (`frontend/javascript_core/`)

```
📁 frontend/javascript_core/
└── qa.md  ← 15 questions on closures, event loop, this, promises

Study order:
  1. Closures + loop trap
  2. Event loop (microtask vs macrotask + output prediction)
  3. `this` keyword (7 contexts)
  4. Promises / async-await (sequential vs parallel)
  5. Hoisting, var/let/const
  6. Prototypal inheritance
  7. Debounce vs Throttle (implementations)
```

---

### 16. Networking & Web Fundamentals (`fundamentals/networking/`)

```
📁 fundamentals/networking/
└── qa.md  ← 12 questions on HTTP, TCP/UDP, DNS, CORS, WebSockets

Study order:
  1. "What happens when you type a URL" (full walkthrough)
  2. HTTP vs HTTPS (TLS handshake)
  3. TCP vs UDP
  4. HTTP methods, status codes, headers
  5. REST vs GraphQL vs gRPC
  6. CORS (preflight + Spring Boot config)
  7. WebSockets, CDN, Load Balancing
```

---

### 17. Application Security (`fundamentals/security/`)

```
📁 fundamentals/security/
└── qa.md  ← 12 questions on OWASP, JWT, XSS, CSRF, OAuth 2.0

Study order (critical for NPCI payments role):
  1. OWASP Top 10 overview
  2. SQL Injection + prevention
  3. XSS types + prevention
  4. CSRF attack flow + prevention
  5. JWT structure + auth flow
  6. OAuth 2.0 Authorization Code flow
  7. Spring Boot API security layers (end-to-end)
  8. Password hashing (BCrypt)
  9. Security headers
```

---

### 18. SQL Coding Practice (`database/mysql/queries/sql_coding_practice.md`)

```
15 classic SQL interview problems:
  1. Second/Nth Highest Salary (DENSE_RANK)
  2. Department-wise Top N Earners
  3. Find Duplicates
  4. Employee earning more than manager (Self JOIN)
  5. Consecutive numbers (LAG/LEAD)
  6. Running totals (Window SUM)
  7. Customers who never ordered (NOT EXISTS)
  8. Rising temperature (compare with previous row)
  9. Pivot / conditional aggregation
  10. Year-over-year growth
  11-15. Delete duplicates, median, gaps, retention

Practice on: LeetCode Database section, HackerRank SQL track
```

---

### 19. Observability & Production Debugging (`backend/observability/`)

```
📁 backend/observability/
└── qa.md  ← 12 questions on ELK, Prometheus, distributed tracing, debugging workflows

Study order:
  1. "Debug a slow API" walkthrough (Step-by-step production debugging)
  2. Three pillars of observability (Logs, Metrics, Traces)
  3. MDC + Correlation IDs (code examples)
  4. ELK Stack architecture (Filebeat → Logstash → Elasticsearch → Kibana)
  5. Distributed tracing (Micrometer + Zipkin/Jaeger)
  6. Prometheus + Grafana (RED method, custom metrics)
  7. Thread dump + Heap dump analysis
  8. Alerting setup (Prometheus rules)
  9. OpenTelemetry (OTel) — vendor-neutral future
```

---

### 20. Schema Design & Data Modeling (`database/schema_design/`)

```
📁 database/schema_design/
└── qa.md  ← 10 questions on ER design, normalization, SQL vs NoSQL

Study order:
  1. Schema design process (entities → relationships → normalize → index)
  2. 1NF → 2NF → 3NF with examples
  3. When to denormalize (with real examples)
  4. SQL vs NoSQL decision framework
  5. E-Commerce schema design (full DDL)
  6. UPI Payment schema design (NPCI-relevant!)
  7. UUID vs Auto-Increment
  8. Soft delete vs Hard delete
  9. Partitioning strategies
```

---

### 21. Spring Boot Advanced Annotations (`backend/spring_boot/advanced_annotations/`)

```
📁 backend/spring_boot/advanced_annotations/
└── qa.md  ← 10 questions on @Async, @Scheduled, @EventListener, @Cacheable, @Retryable

Study order:
  1. @Async + @EnableAsync (thread pool config, CompletableFuture)
  2. @Scheduled (cron, fixedRate, ShedLock for multi-instance)
  3. @EventListener + ApplicationEventPublisher (decoupling services)
  4. @Cacheable / @CacheEvict / @CachePut (Caffeine, Redis integration)
  5. @Retryable / @Recover (exponential backoff, when to retry)
  6. @ConditionalOnProperty (feature flags)
  7. @Transactional deep dive (propagation, isolation, pitfalls)
  8. @Value vs @ConfigurationProperties
  9. @ControllerAdvice (global exception handling)
  10. Actuator endpoints (health, metrics, security)
```

---

### 22. Tricky Output Prediction (`backend/java/tricky_output/`)

```
📁 backend/java/tricky_output/
└── qa.md  ← 20 "What's the output?" problems (Java + JavaScript)

Java questions cover:
  - String pool & == vs equals()
  - Integer caching (autoboxing trap: -128 to 127)
  - Autoboxing null → NPE
  - equals/hashCode contract (HashSet broken without hashCode)
  - Mutable keys in HashMap (memory leak!)
  - finally block overriding return
  - Static vs instance initialization order
  - Field access is NOT polymorphic (methods are)
  - Stream laziness & short-circuiting
  - ConcurrentModificationException

JavaScript questions cover:
  - Type coercion ("5" + 3 - 2 = ?)
  - var hoisting vs let TDZ
  - Closures + setTimeout loop trap
  - == vs === with falsy values
  - Arrow function `this` binding

⚡ TIP: Practice these with a timer — they appear in OA rounds.
```

---

### 23. API Design Patterns (`backend/api_design/`)

```
📁 backend/api_design/
└── qa.md  ← 10 questions on idempotency, pagination, rate limiting, webhooks

Study order (critical for FedEx/NPCI):
  1. Idempotency keys (why critical for payments + implementation)
  2. Cursor-based vs Offset pagination (with code)
  3. API versioning strategies (URI path vs Header)
  4. Token Bucket rate limiting (full implementation)
  5. Webhook design (HMAC signature, retry, SSRF prevention)
  6. Error response design (consistent format, status codes)
  7. Bulk operations API patterns
  8. HATEOAS — when it helps
  9. Long-running operations (async job pattern)
  10. API security checklist
```

---

### 24. Performance Tuning & Profiling (`backend/performance_tuning/`)

```
📁 backend/performance_tuning/
└── qa.md  ← 10 questions on JVM flags, GC, profiling, connection pools

Study order:
  1. JVM flags cheat sheet (-Xms, -Xmx, GC flags, container settings)
  2. G1GC vs ZGC vs Shenandoah comparison
  3. Slow query identification (EXPLAIN, slow query log)
  4. HikariCP connection pool tuning (sizing formula)
  5. Production profiling (async-profiler, JFR, flame graphs)
  6. Memory leak patterns & detection (heap dump + MAT)
  7. JMH benchmarking
  8. Spring Boot startup optimization
  9. N+1 query detection & fixes
  10. Performance tuning checklist (ordered by impact)
```

---

### 25. TypeScript Essentials (`frontend/typescript/`)

```
📁 frontend/typescript/
└── qa.md  ← 10 questions on types, generics, utility types, React+TS

Study order:
  1. Types vs Interfaces (when to use which)
  2. Generics (preserving type info, constraints)
  3. Utility types (Partial, Pick, Omit, Record, ReturnType)
  4. Union types & discriminated unions
  5. Type narrowing & type guards
  6. React+TS component props typing
  7. React hooks with TypeScript (useState, useRef, useReducer)
  8. unknown vs any vs never
  9. API response typing
  10. Common mistakes to avoid
```

---

### 26. WebFlux & Reactive Programming (`backend/spring_boot/webflux_reactive/`)

```
📁 backend/spring_boot/webflux_reactive/
└── qa.md  ← 12 questions on Mono/Flux, backpressure, WebClient, R2DBC + full HTTP client comparison

Study order:
  1. What is reactive programming & why it exists
  2. WebFlux vs Spring MVC decision framework
  3. Mono and Flux with examples
  4. Common operators (map, flatMap, zip, filter)
  5. Backpressure strategies
  6. Reactive REST controller
  7. WebClient — the reactive HTTP client
  8. WebClient vs RestTemplate vs RestClient vs OpenFeign (Q8 — CRITICAL comparison)
  9. Common mistakes (blocking in reactive chain, .block() deadlock)
  10. R2DBC — reactive database access
  11. Testing reactive code (StepVerifier, WebTestClient)
```

---

### 27. AWS Services Breadth (`devops/aws/services_overview/`)

```
📁 devops/aws/services_overview/
└── qa.md  ← 12 questions on SQS/SNS, ECS/EKS, RDS/DynamoDB, IAM, CloudWatch

Study order (critical for FedEx AWS-heavy stack):
  1. AWS services cheat sheet (which service for which need)
  2. SQS vs SNS vs EventBridge (with SNS+SQS fan-out pattern)
  3. SQS deep dive (Standard vs FIFO, DLQ, visibility timeout)
  4. ECS vs EKS vs Lambda decision tree
  5. RDS vs Aurora vs DynamoDB
  6. IAM — roles, policies, least privilege
  7. ALB vs NLB
  8. Secrets Manager with Spring Boot
  9. CloudWatch — logs, metrics, alarms
  10. API Gateway patterns
  11. Well-Architected Framework (6 pillars)
  12. Full architecture diagram (Spring Boot + AWS)
```

---

### 28. Elasticsearch (`backend/elasticsearch/`)

```
📁 backend/elasticsearch/
└── qa.md  ← 10 questions on inverted index, mappings, Query DSL, aggregations

Study order:
  1. What is Elasticsearch & when to use it
  2. Inverted index (how search is O(1) per term)
  3. Index, document, mapping — text vs keyword fields
  4. Analyzers (standard, english, custom with synonyms)
  5. Query DSL — match, term, bool, multi_match
  6. Relevance scoring (BM25: TF × IDF × field length)
  7. Aggregations (terms, date histogram, faceted search)
  8. Sharding and replication
  9. Spring Data Elasticsearch integration (code examples)
  10. Design a search feature (architecture + sync strategy)
```

---

### 29. Spring Batch / ETL (`backend/spring_batch/`)

```
📁 backend/spring_batch/
└── qa.md  ← 10 questions on Job/Step, Reader/Writer, partitioning, skip/retry

Study order:
  1. What is Spring Batch & when to use it
  2. Architecture: Job → Step → Reader/Processor/Writer
  3. Built-in readers (CSV, JDBC, JPA) and writers
  4. ItemProcessor — transform and validate
  5. Chunk-oriented vs Tasklet
  6. Skip and Retry policies (fault tolerance)
  7. Partitioning — parallel processing
  8. JobRepository and metadata (restart/resume)
  9. Scheduling and triggering batch jobs
  10. Real-world design: monthly billing job
```

---

### 30. System Design Estimation (`system_design/hld/estimation_cheatsheet.md`)

```
📁 system_design/hld/
└── estimation_cheatsheet.md  ← Formulas + 6 worked examples

Study order (DO THIS BEFORE any HLD round):
  1. Core formulas: QPS, storage, bandwidth, server count, cache sizing
  2. Memory-friendly numbers (powers of 2, time constants, data sizes)
  3. Worked examples:
     - Twitter (read-heavy, timeline, CDN)
     - URL Shortener (write QPS, base62, cache)
     - YouTube (bandwidth, storage, transcoding)
     - WhatsApp (connections, message rate)
     - Payment System (TPS, idempotency, exactly-once)
     - Notification System (rate limiting, priority queues)
  4. Estimation template (use this in every HLD interview)
```

---

### 31. Distributed Systems Fundamentals (`system_design/hld/distributed_systems_fundamentals.md`) 🆕

```
From: donnemartin/system-design-primer (343K ⭐)

13 Q&As covering:
  1. CAP theorem — proof + real-world trade-offs
  2. Consistency patterns — weak, eventual, strong
  3. Availability patterns — failover (active-passive, active-active), replication
  4. DNS — record types, round robin, weighted routing
  5. CDN — push vs pull, cache invalidation
  6. Load balancers — L4 vs L7, algorithms, session persistence
  7. Reverse proxy — vs load balancer, use cases
  8. Microservices vs Monolith — when to migrate
  9. Service discovery — client-side vs server-side
  10. Security — defense in depth (6 layers)
  11. Latency numbers every programmer should know
```

---

### 32. Database Scaling Patterns (`system_design/hld/database_scaling.md`) 🆕

```
From: donnemartin/system-design-primer (343K ⭐)

10 Q&As covering:
  1. ACID properties + examples
  2. Replication — master-slave, master-master
  3. Federation — split databases by function
  4. Sharding — horizontal partitioning strategies
  5. Denormalization — when and how
  6. SQL tuning — EXPLAIN, indexes, query optimization
  7. NoSQL types — Key-Value, Document, Column, Graph
  8. SQL vs NoSQL — decision framework
  9. CQRS pattern — separate read/write models
```

---

### 33. Caching Strategies Deep Dive (`system_design/hld/caching_deep_dive.md`) 🆕

```
From: donnemartin/system-design-primer (343K ⭐)

11 Q&As covering:
  1. Multi-level caching architecture (Client → CDN → LB → App → DB)
  2. Redis vs Memcached comparison
  3. Cache-aside pattern
  4. Write-through, write-behind, refresh-ahead
  5. Eviction policies — LRU, LFU, TTL
  6. Cache invalidation strategies
  7. Distributed caching with Redis Cluster
```

---

### 34. Async & Messaging Patterns (`system_design/hld/async_and_messaging.md`) 🆕

```
From: donnemartin/system-design-primer (343K ⭐)

10 Q&As covering:
  1. Message queues — SQS, RabbitMQ patterns
  2. Kafka architecture — partitions, consumer groups, exactly-once
  3. Task queues — Celery, background processing
  4. Back pressure — strategies for handling overload
  5. Event-driven architecture
  6. Pub/Sub patterns
  7. Idempotency in messaging
  8. Dead Letter Queues
```

---

### 35. Communication Protocols (`system_design/hld/communication_protocols.md`) 🆕

```
From: donnemartin/system-design-primer (343K ⭐)

9 Q&As covering:
  1. TCP vs UDP — when to use which
  2. HTTP methods & versions (HTTP/1.1, /2, /3)
  3. REST principles (6 constraints)
  4. RPC vs REST — comparison
  5. gRPC — Protobuf, streaming, performance
  6. GraphQL — queries, mutations, N+1 problem
  7. WebSocket — real-time bidirectional
  8. API Gateway — routing, rate limiting, auth
  9. Protocol decision matrix
```

---

### 36. Java Advanced Internals (`java_internals/java_advanced_internals.md`) 🆕

```
From: Snailclimb/JavaGuide (155K ⭐), iluwatar/java-design-patterns (93.9K ⭐)

10 Q&As covering:
  1. Reflection API — Class, Method, Field introspection
  2. Dynamic Proxy — JDK vs CGLIB, Spring AOP internals
  3. Java IO models — BIO, NIO, AIO
  4. SPI mechanism — ServiceLoader, JDBC/Dubbo examples
  5. AbstractQueuedSynchronizer (AQS) — ReentrantLock internals
  6. Atomic classes — CAS, ABA problem, LongAdder
  7. Unsafe class — direct memory, CAS operations
  8. Concurrent collections — ConcurrentHashMap, CopyOnWriteArrayList
  9. Java Platform Module System (JPMS)
  10. Concurrent programming patterns
```

---

### 37. Spring Boot Advanced (`backend/spring_boot/spring_advanced.md`) 🆕

```
From: iluwatar/java-design-patterns (93.9K ⭐)

8 Q&As covering:
  1. AOP deep dive — pointcuts, advice types, self-invocation pitfall
  2. Design patterns in Spring (10 patterns — Singleton, Factory, Proxy, Observer, etc.)
  3. Auto-configuration internals — @Conditional, spring.factories
  4. Spring Cloud ecosystem — Config, Gateway, Resilience4j, Sleuth
  5. GraalVM native image — AOT compilation, limitations
  6. Security filter chain architecture
  7. Spring Data JPA — repository hierarchy, query derivation, Specifications
  8. WebClient & reactive patterns
```

---

### 38. DSA Approach Guide (`dsa_patterns/dsa_approach_guide.md`) 🆕

```
From: kdn251/interviews, williamfiset/Algorithms, NeetCode patterns

Covers ALL 10 patterns with Java templates:
  1. Two Pointers (opposite + same direction)
  2. Sliding Window (fixed + variable)
  3. Binary Search (standard + boundary + on answer)
  4. Dynamic Programming (1D, 2D, knapsack, string, tree DP)
  5. Graph BFS/DFS (shortest path, connected components, topological sort)
  6. Tree patterns (traversals, common techniques)
  7. Heap/Priority Queue (top K, merge K, two-heap median)
  8. Backtracking (subsets, permutations, combinations)
  9. Interval patterns (merge, insert, meeting rooms)
  10. Complexity cheat sheet (all algorithms)

💡 Study this BEFORE solving problems — learn the template, then apply it.
```

---

### 39. Web Performance & Modern Frontend (`frontend/web_performance/qa.md`) 🆕

```
From: yangshun/front-end-interview-handbook (43.9K ⭐), lydiahallie/javascript-questions (65.3K ⭐)

10 Q&As covering:
  1. Core Web Vitals (LCP, INP, CLS) — metrics, thresholds, how to improve
  2. SSR vs SSG vs CSR vs ISR — rendering strategies comparison
  3. JavaScript performance — event loop deep dive, Web Workers, memory leaks
  4. Web Accessibility (a11y) — WCAG, ARIA, keyboard navigation
  5. Frontend security — XSS, CSRF, CSP headers
  6. JavaScript tricky concepts — generators, Proxy, WeakRef, private fields
  7. Frontend system design — framework for answering (News Feed, Chat, E-commerce)
  8. Modern CSS — Flexbox vs Grid, container queries, specificity
  9. Testing frontend — RTL philosophy, MSW, snapshot testing
  10. Build tools — Vite vs Webpack, tree shaking, code splitting
```

---

## How to Track Progress

1. After studying each section, update the **Progress Tracker** in `README.md`
2. Rate yourself honestly (1-10):
   - < 5: Need more practice
   - 5-7: Can handle basic questions
   - 8+: Can handle deep follow-ups
3. Re-visit weak areas (< 7) before interviews

---

## Quick Start for Different Interview Types

**"I have a DSA round tomorrow"**
→ `dsa_patterns/qa.md` → focus on top 10 patterns → practice 5 problems

**"I have a Java technical screening tomorrow"**
→ `backend/java/streams/qa.md` + `collections/qa.md` + `multithreading/qa.md` + `java_internals/qa.md` + `backend/hibernate_jpa/qa.md` + `backend/java/tricky_output/qa.md`

**"I have an HLD round tomorrow"**
→ `system_design/hld/estimation_cheatsheet.md` (do this FIRST!) → `system_design/hld/end_to_end_designs.md` → practice URL Shortener + one more → review cheat sheet

**"I have an LLD round tomorrow"**
→ `system_design/lld/end_to_end_designs.md` → practice Parking Lot → review SOLID + top 5 patterns

**"I have a behavioral round tomorrow"**
→ `behavioral/qa.md` → prepare 5 STAR stories → read `INTERVIEW_PLAYBOOK.md`

**"I have a full-day onsite tomorrow"**
→ `INTERVIEW_PLAYBOOK.md` → `GENERIC_PRODUCT_QUESTIONS.md` → quick review of weak areas

**"I have a fullstack/frontend round tomorrow"**
→ `frontend/javascript_core/qa.md` + `frontend/typescript/qa.md` + `fundamentals/networking/qa.md` + React/Angular sections

**"I have a payment/fintech company interview (NPCI/BillDesk)"**
→ `fundamentals/security/qa.md` + `backend/spring_boot/security_oauth2/qa.md` + `backend/api_design/qa.md` + `database/schema_design/qa.md` + `database/mysql/queries/sql_coding_practice.md` + `backend/spring_batch/qa.md`

**"I have a senior/production-readiness round"**
→ `backend/observability/qa.md` + `backend/performance_tuning/qa.md` + `backend/spring_boot/advanced_annotations/qa.md` + `backend/api_design/qa.md` + `backend/spring_boot/webflux_reactive/qa.md`

**"I have a FedEx / AWS-heavy company interview"**
→ `devops/aws/services_overview/qa.md` + `backend/microservices/design_patterns/qa.md` + `backend/spring_boot/webflux_reactive/qa.md` (Q8: HTTP clients) + `backend/elasticsearch/qa.md` + `system_design/hld/estimation_cheatsheet.md`
