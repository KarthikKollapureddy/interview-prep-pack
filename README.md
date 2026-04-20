# Interview Prep Pack

> A comprehensive, self-contained interview preparation repository for **Java Full Stack Developer** roles at product-based companies.

**Target roles:** SDE-2 / Full Stack Dev II at FedEx, NPCI, Hatio, BillDesk and similar.

---

## What's Inside

| Metric | Count |
|---|---|
| Topics covered | 66 |
| Interview Q&As | 721+ |
| Coded solutions (Java, JS, TS, SQL) | 52 |
| End-to-end HLD designs | 5 (URL Shortener, Food Delivery, Uber, Netflix, E-Commerce) |
| End-to-end LLD designs | 6 (Parking Lot, Library, Snake & Ladder, Hotel, Vending, ATM) |
| DSA patterns | 12 (with approach templates) |
| Total lines of content | 33,500+ |

---

## Repository Structure

```
Interview-prep-pack/
├── README.md                    ← You are here
├── JobDescriptions/             ← Target company JDs (FedEx, NPCI)
├── Resume/                      ← Current resume
└── interview-prep/              ← All study material
    ├── README.md                ← Master index + progress tracker
    ├── STUDY_GUIDE.md           ← 4-week study plan + section-by-section guide
    ├── INTERVIEW_PLAYBOOK.md    ← Day-of strategy, traps, negotiation
    ├── GENERIC_PRODUCT_QUESTIONS.md ← Top 50 cross-cutting questions
    │
    ├── backend/
    │   ├── java/                ← Streams, OOP, Collections, Multithreading,
    │   │                          Exceptions, Java 8→21, Tricky Output
    │   ├── spring_boot/         ← Core, REST, Security, Testing, WebFlux,
    │   │                          Advanced Annotations, Spring Advanced
    │   ├── microservices/       ← Design Patterns, Kafka, Resilience, Service Mesh
    │   ├── hibernate_jpa/       ← N+1, Caching, Entity Lifecycle, @Transactional
    │   ├── design_patterns/     ← 16 GoF patterns with Java code
    │   ├── caching_redis/       ← Strategies, Redis, Spring Boot integration
    │   ├── api_design/          ← Idempotency, Rate Limiting, Pagination, Webhooks
    │   ├── observability/       ← ELK, Prometheus, Distributed Tracing, Debugging
    │   ├── performance_tuning/  ← JVM Flags, GC, Profiling, Connection Pools
    │   ├── elasticsearch/       ← Inverted Index, Query DSL, Aggregations
    │   ├── spring_batch/        ← Job/Step, Reader/Writer, Partitioning
    │   └── graphql/             ← Schema Design, Resolvers, N+1
    │
    ├── frontend/
    │   ├── react/               ← Hooks, State Management, Performance, Testing
    │   ├── angular/             ← Components, RxJS
    │   ├── javascript_core/     ← Closures, Event Loop, Promises, this
    │   ├── typescript/          ← Types, Generics, Utility Types, React+TS
    │   └── web_performance/     ← Core Web Vitals, SSR/SSG, a11y, Security
    │
    ├── database/
    │   ├── mysql/               ← Queries, Indexing (EXPLAIN deep dive), Transactions
    │   ├── mongodb/             ← Aggregation, Sharding, Replication
    │   └── schema_design/       ← ER Diagrams, Normalization, SQL vs NoSQL
    │
    ├── system_design/
    │   ├── hld/                 ← 12 Q&A + 5 E2E designs + Estimation Cheatsheet
    │   │                          + Distributed Systems + DB Scaling + Caching
    │   │                          + Async/Messaging + Communication Protocols
    │   └── lld/                 ← 13 Q&A + 6 E2E designs with Java code
    │
    ├── dsa_patterns/            ← 12 patterns, 50+ problems, approach guide
    ├── java_internals/          ← JVM, GC, Memory, Reflection, NIO, AQS
    ├── fundamentals/            ← Networking (HTTP, DNS, CORS) + Security (OWASP)
    ├── devops/                  ← Docker/K8s, CI/CD, Git, AWS Services
    └── behavioral/              ← STAR method, 16+ questions, story templates
```

---

## How to Use

1. **Start with** [`interview-prep/STUDY_GUIDE.md`](interview-prep/STUDY_GUIDE.md) — 4-week sprint plan with day-by-day topics
2. **Track progress in** [`interview-prep/README.md`](interview-prep/README.md) — 66-topic progress tracker
3. **Before interview day** — read [`interview-prep/INTERVIEW_PLAYBOOK.md`](interview-prep/INTERVIEW_PLAYBOOK.md)
4. **Quick revision** — skim [`interview-prep/GENERIC_PRODUCT_QUESTIONS.md`](interview-prep/GENERIC_PRODUCT_QUESTIONS.md)

### Per-Topic Study Flow

```
Open qa.md → Read conceptual Q&A → Attempt scenario questions
→ Code challenges in solutions/ → Rate yourself in progress tracker
```

---

## Content Sources

Material is original + enhanced with concepts from top open-source repos:

| Source | Stars | Topics Integrated |
|---|---|---|
| [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer) | 343K | Distributed systems, DB scaling, caching, async, protocols |
| [Snailclimb/JavaGuide](https://github.com/Snailclimb/JavaGuide) | 155K | Java NIO, SPI, AQS, Reflection, Concurrent collections |
| [iluwatar/java-design-patterns](https://github.com/iluwatar/java-design-patterns) | 93.9K | Spring AOP, Cloud ecosystem, design patterns in Spring |
| [lydiahallie/javascript-questions](https://github.com/lydiahallie/javascript-questions) | 65.3K | JS tricky concepts, event loop, generators, Proxy |
| [yangshun/front-end-interview-handbook](https://github.com/yangshun/front-end-interview-handbook) | 43.9K | Core Web Vitals, SSR/SSG, frontend system design |
| [winterbe/java8-tutorial](https://github.com/winterbe/java8-tutorial) | 16.8K | Streams, lambdas, functional interfaces |

---

## Tech Stack Covered

**Backend:** Java 8–21, Spring Boot, Spring Cloud, Hibernate/JPA, Kafka, Redis, Elasticsearch, GraphQL  
**Frontend:** React, Angular, TypeScript, JavaScript (ES6+)  
**Database:** MySQL (with EXPLAIN analysis), MongoDB, Schema Design  
**DevOps:** Docker, Kubernetes, Jenkins, Git, AWS (S3, Lambda, ECS, SQS/SNS, IAM)  
**System Design:** HLD (scalability, CAP, estimation) + LLD (SOLID, GoF patterns, class design)

---

## Author

**Sai Karthik Kollapureddy** — Java Full Stack Developer, 3.6+ YoE
