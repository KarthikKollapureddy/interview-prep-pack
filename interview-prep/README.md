# Interview Prep Pack — Master Index

> **Sai Karthik Kollapureddy** | Java Full Stack Developer | 3.6+ YoE  
> **Targets:** FedEx (Full Stack Dev II) · NPCI (Fullstack Developer) · Hatio/BillDesk (SDE-2 ✅ Cleared)

---

## Quick Start

1. **NEW → Start here:** Read [STUDY_GUIDE.md](STUDY_GUIDE.md) for section-by-section navigation & study plan
2. Read [INSTRUCTIONS.md](INSTRUCTIONS.md) for full context, rules, and skill gap analysis
3. Check the **Progress Tracker** below to find your next topic
4. Open the topic's `qa.md` → study Conceptual → attempt Scenario → solve Coding Challenges
5. For System Design: use `end_to_end_designs.md` files for full deep-dive practice
6. Write solutions in the `solutions/` folder → request mentor review
7. Update your score and mark status below after each session

---

## Workspace Tree

```
interview-prep/
├── INSTRUCTIONS.md
├── README.md                          ← YOU ARE HERE
├── STUDY_GUIDE.md                     🎯 NEW — Where to start & how to proceed
├── INTERVIEW_PLAYBOOK.md              🎯 Master strategy guide
├── GENERIC_PRODUCT_QUESTIONS.md       🎯 Cross-cutting generic Q&A bank
│
├── backend/
│   ├── java/
│   │   ├── streams/                   ← FIRST TOPIC (start here)
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── oops/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── collections/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── multithreading/            🔴 P0 GAP
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── java8_features/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── exceptions/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── java_versions/             🎯 NEW — Java 8→21 features (tabular + examples)
│   │       └── java_8_to_21_features.md
│   │   └── tricky_output/             🆕 "What's the output?" (String pool, autoboxing, JS coercion)
│   │       └── qa.md
│   │
│   ├── spring_boot/
│   │   ├── core_concepts/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── rest_apis/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── security_oauth2/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── testing/                   🔴 P0 GAP (JUnit5)
│   │       ├── qa.md
│   │       └── solutions/
│   │   └── advanced_annotations/      🆕 @Async, @Scheduled, @Cacheable, @Retryable
│   │       └── qa.md
│   │   └── spring_advanced.md         🆕 AOP, Spring Cloud, GraalVM, Security Chain (from iluwatar/java-design-patterns 93.9K⭐)
│   │
│   ├── microservices/
│   │   ├── design_patterns/           🔴 P0 GAP
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── kafka/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── resilience/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── service_mesh/              🔴 P0 GAP (Spring Cloud Gateway)
│   │       ├── qa.md
│   │       └── solutions/
│   │
│   └── graphql/
│       ├── qa.md
│       └── solutions/
│
│   ├── design_patterns/               🎯 NEW — GoF Patterns (15 patterns + Java code)
│   │   └── qa.md
│   │
│   └── caching_redis/                 🎯 NEW — Caching Strategies, Redis Deep Dive
│       └── qa.md
│
│   └── hibernate_jpa/                 🎯 NEW — Hibernate/JPA Deep Dive (N+1, Caching, Transactions)
│       └── qa.md
│
│   ├── observability/                 🆕 Observability & Production Debugging
│   │   └── qa.md
│   ├── api_design/                    🆕 API Design Patterns (Idempotency, Rate Limiting, Webhooks)
│   │   └── qa.md
│   └── performance_tuning/            🆕 JVM Tuning, Profiling, Connection Pools
│       └── qa.md
│
│   ├── spring_boot/
│   │   └── webflux_reactive/          🆕 WebFlux, Mono/Flux, WebClient vs RestTemplate vs RestClient
│   │       └── qa.md
│   ├── elasticsearch/                 🆕 Inverted Index, Query DSL, Aggregations, Sharding
│   │   └── qa.md
│   └── spring_batch/                  🆕 Job/Step, Reader/Writer, Partitioning, Skip/Retry
│       └── qa.md
│
├── frontend/
│   ├── react/
│   │   ├── hooks/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── state_management/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── performance/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── testing/
│   │       ├── qa.md
│   │       └── solutions/
│   └── angular/
│       ├── components/
│       │   ├── qa.md
│       │   └── solutions/
│       └── rxjs/
│           ├── qa.md
│           └── solutions/
│   └── javascript_core/               🎯 NEW — JS Fundamentals (Closures, Event Loop, Promises)
│       └── qa.md
│   └── typescript/                    🆕 TypeScript Essentials (Types, Generics, React+TS)
│       └── qa.md
│   └── web_performance/               🆕 Core Web Vitals, SSR/SSG, a11y, Security (from front-end-interview-handbook 43.9K⭐)
│       └── qa.md
│
├── database/
│   ├── mysql/
│   │   ├── queries/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── indexing/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── transactions/              🟡 P1 GAP (distributed txn)
│   │       ├── qa.md
│   │       └── solutions/
│   └── mongodb/
│       ├── qa.md
│       └── solutions/
│   └── schema_design/                 🆕 ER Diagrams, Normalization, SQL vs NoSQL Decisions
│       └── qa.md
│
├── fundamentals/                      🎯 NEW — Cross-cutting fundamentals
│   ├── networking/                    ← HTTP, TCP/UDP, DNS, CORS, WebSockets
│   │   └── qa.md
│   └── security/                      ← OWASP, JWT, XSS, CSRF, SQL Injection
│       └── qa.md
│
├── devops/
│   ├── aws/
│   │   ├── s3_cloudfront/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── lambda/
│   │       ├── qa.md
│   │       └── solutions/
│   ├── docker_kubernetes/             🟡 P1 GAP
│   │   ├── qa.md
│   │   └── solutions/
│   └── ci_cd/                         🟡 P1 GAP (Jenkins)
│       ├── qa.md
│       └── solutions/
│
│   └── git/                           🎯 NEW — Git Commands, Branching, Workflows
│       └── qa.md
│   └── aws/
│       └── services_overview/         🆕 SQS/SNS, ECS/EKS, RDS/DynamoDB, IAM, CloudWatch
│           └── qa.md
│
└── system_design/                     🔴 P0 GAP
    ├── hld/
    │   ├── qa.md                      12 HLD problems (Q&A format)
    │   ├── end_to_end_designs.md      ⭐ 5 full E2E designs (deep dive)    │   ├── estimation_cheatsheet.md   🆕 Back-of-envelope estimation (QPS, storage, bandwidth + 6 examples)
│   ├── distributed_systems_fundamentals.md  🆕 CAP, consistency, DNS, CDN, load balancers (from system-design-primer 343K⭐)
│   ├── database_scaling.md                  🆕 Replication, sharding, federation, CQRS (from system-design-primer)
│   ├── caching_deep_dive.md                 🆕 Multi-level caching, Redis vs Memcached, strategies (from system-design-primer)
│   ├── async_and_messaging.md               🆕 Kafka, message queues, back pressure, DLQ (from system-design-primer)
│   ├── communication_protocols.md           🆕 TCP/UDP, HTTP, REST, gRPC, GraphQL, WebSocket (from system-design-primer)
│   └── solutions/
    └── lld/
        ├── qa.md                      13 LLD problems (SOLID, patterns)
        ├── end_to_end_designs.md      ⭐ 5+1 full E2E designs (with Java code)
        └── solutions/

├── dsa_patterns/                      🎯 NEW — Product Company Essential
│   ├── README.md
│   ├── qa.md                          50 curated problems by pattern
│   ├── dsa_approach_guide.md          🆕 Pattern templates, complexity cheat sheet (from kdn251/interviews)
│   └── solutions/
│
├── java_internals/                    🎯 NEW — JVM, GC, Memory, String Pool
│   ├── README.md
│   ├── qa.md                          25 deep-dive questions
│   ├── java_advanced_internals.md     🆕 Reflection, NIO, SPI, AQS, Proxy (from JavaGuide 155K⭐)
│   └── solutions/
│
└── behavioral/                        🎯 NEW — STAR method, HR, Negotiation
    ├── README.md
    ├── qa.md                          20+ behavioral questions with answers
    └── solutions/
```

---

## Progress Tracker

| # | Topic | Path | Gap | Status | Score | Last Reviewed |
|---|-------|------|-----|--------|-------|---------------|
| 1 | Java Streams | `backend/java/streams/` | P1 | [ ] Not started | —/10 | — |
| 2 | OOP & SOLID | `backend/java/oops/` | — | [ ] Not started | —/10 | — |
| 3 | Collections Internals | `backend/java/collections/` | P1 | [ ] Not started | —/10 | — |
| 4 | Multithreading | `backend/java/multithreading/` | **P0** | [ ] Not started | —/10 | — |
| 5 | Java 8+ Features | `backend/java/java8_features/` | — | [ ] Not started | —/10 | — |
| 6 | Exception Handling | `backend/java/exceptions/` | — | [ ] Not started | —/10 | — |
| 7 | Spring Boot Core | `backend/spring_boot/core_concepts/` | — | [ ] Not started | —/10 | — |
| 8 | REST API Design | `backend/spring_boot/rest_apis/` | — | [ ] Not started | —/10 | — |
| 9 | Security & OAuth2 | `backend/spring_boot/security_oauth2/` | — | [ ] Not started | —/10 | — |
| 10 | Testing (JUnit5) | `backend/spring_boot/testing/` | **P0** | [ ] Not started | —/10 | — |
| 11 | MS Design Patterns | `backend/microservices/design_patterns/` | **P0** | [ ] Not started | —/10 | — |
| 12 | Kafka Deep Dive | `backend/microservices/kafka/` | — | [ ] Not started | —/10 | — |
| 13 | Resilience Patterns | `backend/microservices/resilience/` | P1 | [ ] Not started | —/10 | — |
| 14 | Service Mesh / Gateway | `backend/microservices/service_mesh/` | **P0** | [ ] Not started | —/10 | — |
| 15 | GraphQL | `backend/graphql/` | — | [ ] Not started | —/10 | — |
| 16 | React Hooks | `frontend/react/hooks/` | — | [ ] Not started | —/10 | — |
| 17 | State Management | `frontend/react/state_management/` | — | [ ] Not started | —/10 | — |
| 18 | React Performance | `frontend/react/performance/` | — | [ ] Not started | —/10 | — |
| 19 | React Testing | `frontend/react/testing/` | — | [ ] Not started | —/10 | — |
| 20 | Angular Components | `frontend/angular/components/` | — | [ ] Not started | —/10 | — |
| 21 | RxJS | `frontend/angular/rxjs/` | — | [ ] Not started | —/10 | — |
| 22 | MySQL Queries | `database/mysql/queries/` | — | [ ] Not started | —/10 | — |
| 23 | MySQL Indexing | `database/mysql/indexing/` | — | [ ] Not started | —/10 | — |
| 24 | Transactions | `database/mysql/transactions/` | P1 | [ ] Not started | —/10 | — |
| 25 | MongoDB | `database/mongodb/` | — | [ ] Not started | —/10 | — |
| 26 | AWS S3/CloudFront | `devops/aws/s3_cloudfront/` | — | [ ] Not started | —/10 | — |
| 27 | AWS Lambda | `devops/aws/lambda/` | — | [ ] Not started | —/10 | — |
| 28 | Docker & Kubernetes | `devops/docker_kubernetes/` | P1 | [ ] Not started | —/10 | — |
| 29 | CI/CD (Jenkins) | `devops/ci_cd/` | P1 | [ ] Not started | —/10 | — |
| 30 | System Design — HLD | `system_design/hld/` | **P0** | [ ] Not started | —/10 | — |
| 31 | System Design — LLD | `system_design/lld/` | **P0** | [ ] Not started | —/10 | — |
| 32 | DSA & Coding Patterns | `dsa_patterns/` | **P0** | [ ] Not started | —/10 | — |
| 33 | Java Internals & Memory | `java_internals/` | **P0** | [ ] Not started | —/10 | — |
| 34 | Behavioral & HR | `behavioral/` | **P0** | [ ] Not started | —/10 | — |
| 35 | Generic Product Qs | `GENERIC_PRODUCT_QUESTIONS.md` | — | [ ] Not started | —/10 | — |
| 36 | GoF Design Patterns | `backend/design_patterns/` | **P0** | [ ] Not started | —/10 | — |
| 37 | Caching & Redis | `backend/caching_redis/` | **P0** | [ ] Not started | —/10 | — |
| 38 | Git & Version Control | `devops/git/` | P1 | [ ] Not started | —/10 | — |
| 39 | Hibernate & JPA | `backend/hibernate_jpa/` | **P0** | [ ] Not started | —/10 | — |
| 40 | JavaScript Core | `frontend/javascript_core/` | **P0** | [ ] Not started | —/10 | — |
| 41 | Networking & Web | `fundamentals/networking/` | P1 | [ ] Not started | —/10 | — |
| 42 | Application Security | `fundamentals/security/` | **P0** | [ ] Not started | —/10 | — |
| 43 | SQL Coding Practice | `database/mysql/queries/sql_coding_practice.md` | P1 | [ ] Not started | —/10 | — |
| 44 | Java 8→21 Features | `backend/java/java_versions/` | **P0** | [ ] Not started | —/10 | — |
| 45 | Observability & Debugging | `backend/observability/` | **P0** | [ ] Not started | —/10 | — |
| 46 | Schema Design & Data Modeling | `database/schema_design/` | **P0** | [ ] Not started | —/10 | — |
| 47 | Spring Boot Adv. Annotations | `backend/spring_boot/advanced_annotations/` | P1 | [ ] Not started | —/10 | — |
| 48 | Tricky Output Prediction | `backend/java/tricky_output/` | **P0** | [ ] Not started | —/10 | — |
| 49 | API Design Patterns | `backend/api_design/` | **P0** | [ ] Not started | —/10 | — |
| 50 | Performance Tuning & Profiling | `backend/performance_tuning/` | **P0** | [ ] Not started | —/10 | — |
| 51 | TypeScript Essentials | `frontend/typescript/` | P1 | [ ] Not started | —/10 | — |
| 52 | WebFlux & Reactive | `backend/spring_boot/webflux_reactive/` | P1 | [ ] Not started | —/10 | — |
| 53 | HTTP Clients Comparison | `backend/spring_boot/webflux_reactive/` (Q8) | P1 | [ ] Not started | —/10 | — |
| 54 | AWS Services Breadth | `devops/aws/services_overview/` | **P0** | [ ] Not started | —/10 | — |
| 55 | Elasticsearch | `backend/elasticsearch/` | P1 | [ ] Not started | —/10 | — |
| 56 | Spring Batch / ETL | `backend/spring_batch/` | P1 | [ ] Not started | —/10 | — |
| 57 | System Design Estimation | `system_design/hld/estimation_cheatsheet.md` | **P0** | [ ] Not started | —/10 | — |
| 58 | Distributed Systems Fundamentals | `system_design/hld/distributed_systems_fundamentals.md` | **P0** | [ ] Not started | —/10 | — |
| 59 | Database Scaling Patterns | `system_design/hld/database_scaling.md` | **P0** | [ ] Not started | —/10 | — |
| 60 | Caching Strategies Deep Dive | `system_design/hld/caching_deep_dive.md` | **P0** | [ ] Not started | —/10 | — |
| 61 | Async & Messaging Patterns | `system_design/hld/async_and_messaging.md` | **P0** | [ ] Not started | —/10 | — |
| 62 | Communication Protocols | `system_design/hld/communication_protocols.md` | P1 | [ ] Not started | —/10 | — |
| 63 | Java Advanced Internals | `java_internals/java_advanced_internals.md` | **P0** | [ ] Not started | —/10 | — |
| 64 | Spring Boot Advanced | `backend/spring_boot/spring_advanced.md` | **P0** | [ ] Not started | —/10 | — |
| 65 | DSA Approach Guide | `dsa_patterns/dsa_approach_guide.md` | **P0** | [ ] Not started | —/10 | — |
| 66 | Web Performance & Modern Frontend | `frontend/web_performance/qa.md` | **P0** | [ ] Not started | —/10 | — |

**Legend:** 🔴 P0 = Critical gap (interview blocker) · 🟡 P1 = Important gap · — = Strength/maintain

**Total Topics: 66** | **New additions (this round): Distributed Systems Fundamentals, Database Scaling, Caching Deep Dive, Async & Messaging, Communication Protocols, Java Advanced Internals, Spring Boot Advanced, DSA Approach Guide, Web Performance & Modern Frontend** — *sourced from system-design-primer (343K⭐), JavaGuide (155K⭐), java-design-patterns (93.9K⭐), front-end-interview-handbook (43.9K⭐), javascript-questions (65.3K⭐)*

---

## Recommended Study Order

**Phase 1 — Java Foundations (Week 1)**
> Streams → Collections → OOP → Multithreading → Java8 Features → Exceptions → **Java Internals (JVM, GC, String Pool)** → **Tricky Output Questions**

**Phase 2 — Spring & Microservices (Week 2)**
> Spring Core → REST APIs → **Advanced Annotations (@Async, @Cacheable, @Retryable)** → Security/OAuth2 → Testing → **Hibernate/JPA** → MS Patterns → Kafka → Resilience → Gateway → GraphQL → **GoF Design Patterns** → **Caching & Redis** → **API Design Patterns** → **Performance Tuning & Profiling** → **Observability & Production Debugging** → **WebFlux & Reactive (Mono/Flux/WebClient)** → **Spring Batch / ETL** → **Elasticsearch**

**Phase 3 — DSA & Coding (Week 3)** 🆕
> Arrays & Hashing → Two Pointers → Sliding Window → Stack → Binary Search → Trees → Graphs → DP → Backtracking  
> Follow the 4-week sprint plan in `dsa_patterns/qa.md`

**Phase 4 — Frontend + Database + DevOps (Week 4)**
> **JavaScript Core** → **TypeScript Essentials** → React Hooks → State Mgmt → Performance → Testing → Angular → MySQL → **SQL Coding Practice** → **Schema Design & Data Modeling** → MongoDB → **AWS Services Breadth (SQS/SNS/ECS/EKS/RDS/DynamoDB/IAM)** → Docker/K8s → CI/CD → **Git Workflows**

**Phase 4.5 — Fundamentals (Throughout)** 🆕
> **Networking & Web** → **Application Security (OWASP, JWT, XSS/CSRF)** — review these across all weeks

**Phase 5 — System Design (Ongoing)**
> **Estimation Cheatsheet (QPS, storage, bandwidth)** → HLD and LLD should be practiced throughout, 1 problem per day

**Phase 6 — Behavioral & Mock Interviews (Final Week)** 🆕
> STAR stories → Failure/conflict stories → Company-specific prep → Mock interviews  
> See `behavioral/qa.md` and `INTERVIEW_PLAYBOOK.md`

**Phase 7 — Final Revision** 🆕
> Top 50 Questions in `GENERIC_PRODUCT_QUESTIONS.md` → Tricky output questions → Weak areas revision

---

## New: Product Company Prep Resources

| Resource | What It Covers | File |
|----------|---------------|------|
| **Study Guide** | Where to start, how to proceed, 4-week plan | [STUDY_GUIDE.md](STUDY_GUIDE.md) |
| **Interview Playbook** | Day-of strategy, round types, traps, negotiation | [INTERVIEW_PLAYBOOK.md](INTERVIEW_PLAYBOOK.md) |
| **Generic Questions Bank** | 50 top questions + deep dives across ALL topics | [GENERIC_PRODUCT_QUESTIONS.md](GENERIC_PRODUCT_QUESTIONS.md) |
| **HLD Deep Dives** | 5 full E2E designs: URL Shortener, Swiggy, Uber, Netflix, Amazon | [end_to_end_designs.md](system_design/hld/end_to_end_designs.md) |
| **LLD Deep Dives** | 5+1 full class designs: Parking Lot, Library, Snake&Ladder, Hotel, Vending, ATM | [end_to_end_designs.md](system_design/lld/end_to_end_designs.md) |
| **DSA Patterns** | 50 curated LeetCode problems by pattern | [dsa_patterns/qa.md](dsa_patterns/qa.md) |
| **Java Internals** | JVM, GC, Memory, String Pool — 25 questions | [java_internals/qa.md](java_internals/qa.md) |
| **Behavioral Guide** | STAR method, 20+ Q&A, Amazon LPs, negotiation | [behavioral/qa.md](behavioral/qa.md) |
| **GoF Design Patterns** | 15 patterns with full Java code, Spring integration | [backend/design_patterns/qa.md](backend/design_patterns/qa.md) |
| **Caching & Redis** | Strategies, Redis data structures, Spring Boot integration | [backend/caching_redis/qa.md](backend/caching_redis/qa.md) |
| **Git & Version Control** | Commands, branching strategies, workflows, best practices | [devops/git/qa.md](devops/git/qa.md) |
| **Hibernate & JPA** | Entity lifecycle, N+1 problem, caching, @Transactional, Spring Data JPA | [backend/hibernate_jpa/qa.md](backend/hibernate_jpa/qa.md) |
| **JavaScript Core** | Closures, event loop, promises, hoisting, prototypes, this keyword | [frontend/javascript_core/qa.md](frontend/javascript_core/qa.md) |
| **Networking & Web** | HTTP, TCP/UDP, DNS, CORS, REST vs GraphQL vs gRPC, WebSockets | [fundamentals/networking/qa.md](fundamentals/networking/qa.md) |
| **Application Security** | OWASP Top 10, JWT, XSS, CSRF, SQL Injection, OAuth 2.0 | [fundamentals/security/qa.md](fundamentals/security/qa.md) |
| **SQL Coding Practice** | 15 classic SQL problems: Nth salary, top N per group, consecutive, pivot | [database/mysql/queries/sql_coding_practice.md](database/mysql/queries/sql_coding_practice.md) |
| **Java 8→21 Features** | Every feature from Java 8 to 21 in tabular format with examples | [backend/java/java_versions/java_8_to_21_features.md](backend/java/java_versions/java_8_to_21_features.md) |
| **WebFlux & Reactive** | Mono/Flux, backpressure, WebClient vs RestTemplate vs RestClient vs Feign | [backend/spring_boot/webflux_reactive/qa.md](backend/spring_boot/webflux_reactive/qa.md) |
| **AWS Services Breadth** | SQS/SNS, ECS/EKS/Lambda, RDS/DynamoDB, IAM, CloudWatch, architecture | [devops/aws/services_overview/qa.md](devops/aws/services_overview/qa.md) |
| **Elasticsearch** | Inverted index, mappings, Query DSL, aggregations, sharding, Spring Data | [backend/elasticsearch/qa.md](backend/elasticsearch/qa.md) |
| **Spring Batch / ETL** | Job/Step, Reader/Writer, chunk processing, partitioning, skip/retry | [backend/spring_batch/qa.md](backend/spring_batch/qa.md) |
| **Estimation Cheatsheet** | QPS, storage, bandwidth formulas + 6 worked examples (Twitter, YouTube, etc.) | [system_design/hld/estimation_cheatsheet.md](system_design/hld/estimation_cheatsheet.md) |
| **Distributed Systems** | CAP, consistency patterns, DNS, CDN, load balancers, reverse proxy | [system_design/hld/distributed_systems_fundamentals.md](system_design/hld/distributed_systems_fundamentals.md) |
| **Database Scaling** | Replication, sharding, federation, denormalization, SQL vs NoSQL, CQRS | [system_design/hld/database_scaling.md](system_design/hld/database_scaling.md) |
| **Caching Deep Dive** | Multi-level caching, Redis vs Memcached, cache-aside, write-through, eviction | [system_design/hld/caching_deep_dive.md](system_design/hld/caching_deep_dive.md) |
| **Async & Messaging** | Kafka internals, message queues, back pressure, DLQ, event-driven | [system_design/hld/async_and_messaging.md](system_design/hld/async_and_messaging.md) |
| **Communication Protocols** | TCP/UDP, HTTP/2/3, REST, gRPC, GraphQL, WebSocket, API Gateway | [system_design/hld/communication_protocols.md](system_design/hld/communication_protocols.md) |
| **Java Advanced Internals** | Reflection, NIO, SPI, AQS, Dynamic Proxy, Unsafe, JPMS modules | [java_internals/java_advanced_internals.md](java_internals/java_advanced_internals.md) |
| **Spring Boot Advanced** | AOP deep dive, Spring Cloud, GraalVM Native Image, Security Filter Chain | [backend/spring_boot/spring_advanced.md](backend/spring_boot/spring_advanced.md) |
| **DSA Approach Guide** | Pattern templates, complexity cheat sheet, approach for each of 10 patterns | [dsa_patterns/dsa_approach_guide.md](dsa_patterns/dsa_approach_guide.md) |
| **Web Performance** | Core Web Vitals, SSR/SSG/ISR, a11y, Web Security, JS advanced patterns | [frontend/web_performance/qa.md](frontend/web_performance/qa.md) |
