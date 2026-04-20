# FedEx Full Stack Developer II — 9-Day Battle Plan

> **Interview Date:** April 30, 2026  
> **Today:** April 20, 2026 (Day 0 — planning day)  
> **Prep Days:** April 21–29 (9 full days)  
> **Role:** Full Stack Developer II, FedEx, Hyderabad  
> **Your Edge:** You already work on FedEx projects at Wipro — use this domain knowledge

---

## FedEx Interview Process (Based on Research)

```
┌─────────────────────────────────────────────────────────────┐
│ Round 1: Online Assessment / Aptitude + Technical MCQs      │
│   • 40 MCQs in 40 min (timed, auto-graded)                 │
│   • Java OOP, Multithreading, Error Handling                │
│   • HTML/CSS/JavaScript basics                              │
│   • SQL queries, Joins                                      │
│   • Aptitude: Ratios, Speed-Distance, Probability           │
│   • Critical thinking: Deductive reasoning, logic           │
├─────────────────────────────────────────────────────────────┤
│ Round 2: Technical Interview — Deep Dive (45-60 min)        │
│   • 2-3 min self-intro / pitch                              │
│   • Core Java: Streams, Collections internals, Threading    │
│   • Spring Boot: REST, Security, @Transactional             │
│   • Hibernate/JPA: N+1, caching, entity lifecycle           │
│   • SQL: Write complex queries, EXPLAIN analysis            │
│   • React/Angular: Component lifecycle, hooks, state mgmt   │
│   • Live coding exercise (form validation, string ops)      │
│   • Microservices: Kafka, Circuit Breaker, Saga             │
├─────────────────────────────────────────────────────────────┤
│ Round 3: System Design + Problem Solving (45-60 min)        │
│   • HLD: Design a shipment tracking system / URL shortener  │
│   • LLD: Parking lot / Notification system                  │
│   • OR: Live DSA problem (Medium level)                     │
├─────────────────────────────────────────────────────────────┤
│ Round 4: Behavioral / Hiring Manager (30-45 min)            │
│   • STAR stories from FedEx/UHG projects                    │
│   • "Why leave Wipro? Why FedEx directly?"                  │
│   • Conflict resolution, ownership, deadline pressure       │
│   • "What do you know about FedEx tech?"                    │
│   • Your questions to them                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Weight Distribution (Where to Spend Time)

| Area | Interview Weight | Your Current Level | Priority |
|------|------------------|--------------------|----------|
| Core Java (OOP, Streams, Threading, Collections) | 30% | 9/10 | Revise only |
| Spring Boot + Microservices | 25% | 9/10 | Revise only |
| SQL + Database (MySQL queries, EXPLAIN) | 15% | 8/10 | Quick refresh |
| Frontend (React, JS, TS) | 10% | 7/10 | Targeted study |
| System Design (HLD + LLD) | 10% | 7.5/10 | Practice 2-3 designs |
| Behavioral + Self-Intro | 5% | 5.5/10 | **NEEDS WORK** |
| Aptitude + Reasoning | 5% | ? | Light prep |

---

## The Plan

### Day 1 (Apr 21) — Java Core Blitz + Self-Intro Craft

**Morning (3h): Java Speed Revision**
- [ ] `backend/java/streams/qa.md` — Skim all 18 Qs, mark any you can't answer cold → re-read those
- [ ] `backend/java/collections/qa.md` — HashMap internals, ConcurrentHashMap, TreeMap
- [ ] `backend/java/oops/qa.md` — SOLID with code examples, inheritance tricky parts
- [ ] `GENERIC_PRODUCT_QUESTIONS.md` Part 1 — String internals, immutability, hashCode/equals

**Afternoon (2h): Multithreading Deep Dive**
- [ ] `backend/java/multithreading/qa.md` — Focus on: ExecutorService, CompletableFuture, Virtual Threads, synchronized vs Lock, volatile
- [ ] `backend/java/tricky_output/qa.md` — Do all "what's the output" questions (MCQ prep)

**Evening (1h): Craft Your Pitch**
- [ ] Write a 2-min self-intro (see template below)
- [ ] Write 3 STAR stories from your FedEx project at Wipro
- [ ] Fill `behavioral/qa.md` placeholders with real metrics

**Self-Intro Template:**
```
"Hi, I'm Sai Karthik, a Java Full Stack Developer with 3.6 years of experience 
at Wipro. I've worked on two major projects:

1. At FedEx — [what you built, scale, tech stack, your contribution]
2. At UnitedHealth Group — [what you built, impact]

My core stack is Java 17, Spring Boot, React, MySQL, and Kafka. 
I'm particularly strong in [microservices/performance optimization/etc].

I'm excited about this role because I already understand FedEx's tech ecosystem 
from the inside, and I want to contribute directly as part of the core team."
```

---

### Day 2 (Apr 22) — Spring Boot + Hibernate + Security

**Morning (3h): Spring Ecosystem**
- [ ] `backend/spring_boot/core_concepts/qa.md` — Bean lifecycle, @Async, @Cacheable, profiles
- [ ] `backend/spring_boot/rest_apis/qa.md` — Exception handling, pagination, HATEOAS
- [ ] `backend/spring_boot/spring_advanced.md` — AOP deep dive, Spring Cloud, GraalVM

**Afternoon (2h): Hibernate + Security**
- [ ] `backend/hibernate_jpa/qa.md` — N+1 problem (KNOW THIS COLD), @Transactional propagation, entity states, 1st/2nd level cache
- [ ] `backend/spring_boot/security_oauth2/qa.md` — JWT flow, OAuth2 grant types, Security Filter Chain

**Evening (1h): API Design + Quick Practice**
- [ ] `backend/api_design/qa.md` — Idempotency, rate limiting, versioning
- [ ] Write from memory: "Design a REST API for shipment tracking" (3 endpoints, request/response)

---

### Day 3 (Apr 23) — SQL Mastery + Database Day

**Morning (3h): MySQL Deep Dive**
- [ ] `database/mysql/queries/qa.md` — Practice writing complex JOINs, subqueries, window functions
- [ ] `database/mysql/indexing/qa.md` — **EXPLAIN output analysis** (Q4-Q4f), B+ tree, composite index rules
- [ ] `database/mysql/transactions/qa.md` — ACID, isolation levels, deadlocks, MVCC

**Afternoon (2h): MongoDB + Schema Design**
- [ ] `database/mongodb/qa.md` — Aggregation pipeline, sharding, replication
- [ ] `database/schema_design/qa.md` — Normalization, when to denormalize, SQL vs NoSQL decision

**Evening (1h): SQL Practice**
- [ ] Write 10 queries from scratch on paper/notepad:
  1. Find 2nd highest salary per department
  2. Running total of orders by date
  3. Employees who earn more than their manager
  4. Delete duplicate rows keeping one
  5. Pivot monthly sales by product
  6. Find gaps in sequential IDs
  7. Rolling 7-day average
  8. Self-join: find pairs
  9. Recursive CTE: org hierarchy
  10. EXPLAIN analysis of a slow query → add index → re-EXPLAIN

---

### Day 4 (Apr 24) — Frontend Day (React + JS + TS)

**Morning (3h): React Deep Dive**
- [ ] `frontend/react/hooks/qa.md` — useState, useEffect, useCallback, useMemo, useRef, custom hooks
- [ ] `frontend/react/state_management/qa.md` — Context vs Redux vs Zustand, prop drilling
- [ ] `frontend/react/performance/qa.md` — React.memo, lazy loading, virtualization

**Afternoon (2h): JavaScript Core + TypeScript**
- [ ] `frontend/javascript_core/qa.md` — Closures, event loop, promises, `this`, prototype chain
- [ ] `frontend/typescript/qa.md` — Generics, utility types, type guards, discriminated unions

**Evening (1h): Practice Live Coding**
- [ ] Code from scratch (no IDE help):
  - Form validation function (username 10 chars, password has letters+numbers) — *this was an actual FedEx interview question*
  - Debounce function implementation
  - Promise.all polyfill
  - Custom React hook: `useFetch`

---

### Day 5 (Apr 25) — Microservices + DevOps + Kafka

**Morning (3h): Microservices Patterns**
- [ ] `backend/microservices/design_patterns/qa.md` — Saga, CQRS, Event Sourcing, API Gateway
- [ ] `backend/microservices/resilience/qa.md` — Circuit Breaker, Bulkhead, Retry, Rate Limiter
- [ ] `backend/microservices/kafka/qa.md` — Consumer groups, partitions, exactly-once, dead letter topic

**Afternoon (2h): DevOps + Observability**
- [ ] `devops/docker_kubernetes/qa.md` — Dockerfile best practices, K8s pods/services/deployments
- [ ] `devops/ci_cd/qa.md` — Jenkins pipeline, blue-green, canary deployments
- [ ] `backend/observability/qa.md` — ELK stack, Prometheus/Grafana, distributed tracing

**Evening (1h): Design Patterns**
- [ ] `backend/design_patterns/qa.md` — Focus on: Singleton, Factory, Strategy, Observer, Builder, Decorator
- [ ] For each: when to use + Spring Boot real-world example

---

### Day 6 (Apr 26) — System Design HLD Day

**Morning (3h): HLD Foundations**
- [ ] `system_design/hld/qa.md` — All 12 questions
- [ ] `system_design/hld/distributed_systems_fundamentals.md` — CAP, consistent hashing, leader election
- [ ] `system_design/hld/estimation_cheatsheet.md` — Back-of-envelope calculations

**Afternoon (3h): Practice 2 Full HLD Designs**
- [ ] `system_design/hld/end_to_end_designs.md` — Study URL Shortener + Food Delivery designs
- [ ] **Then design from scratch on paper:** "Design FedEx Package Tracking System"
  - Requirements: Real-time tracking, 10M packages/day, scan events, customer notifications
  - Components: API Gateway, Event Bus (Kafka), Tracking Service, Notification Service, Search (ES)
  - Database: Event store (Cassandra) + relational (MySQL for customer data)
  - Scale: CDN for tracking page, caching for hot packages

**Evening (1h): More HLD Topics**
- [ ] `system_design/hld/caching_deep_dive.md` — Cache aside, write-through, eviction
- [ ] `system_design/hld/database_scaling.md` — Sharding, replication, read replicas

---

### Day 7 (Apr 27) — System Design LLD + Design Patterns

**Morning (3h): LLD Deep Dive**
- [ ] `system_design/lld/qa.md` — All 13 questions (SOLID, patterns, design exercises)
- [ ] `system_design/lld/end_to_end_designs.md` — Study Parking Lot + Library Management designs

**Afternoon (2h): Practice LLD on Paper**
- [ ] Design from scratch: **Notification Service** (FedEx-relevant)
  - Classes: Notification, NotificationTemplate, Channel (Email/SMS/Push), NotificationService
  - Patterns: Strategy (channel selection), Observer (event listeners), Factory (template creation)
  - Interface segregation: separate send() by channel type
  
- [ ] Design from scratch: **Shipment State Machine**
  - States: CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED / EXCEPTION
  - State pattern, event-driven transitions

**Evening (1h): Quick Design Patterns Drill**
- [ ] For each of these 8 patterns, write: (a) 1-line definition (b) Java example (c) Spring Boot usage
  - Singleton, Factory, Builder, Strategy, Observer, Decorator, Adapter, Template Method

---

### Day 8 (Apr 28) — Behavioral + Mock Interview Day

**Morning (2h): Behavioral Prep**
- [ ] `behavioral/qa.md` — Read all 16+ questions, finalize your STAR answers
- [ ] Prepare these specific stories WITH REAL METRICS:

| Story | Situation | What you did | Result (numbers!) |
|-------|-----------|--------------|-------------------|
| Tight deadline | Sprint crunch on FedEx project | [your action] | Delivered on time, X% coverage |
| Production bug | Critical issue in FedEx/UHG | [your debugging process] | Resolved in X hours, impact Y |
| Conflict/disagreement | Team disagreement on approach | [how you resolved] | Adopted solution that improved Z |
| Ownership | Feature you owned end-to-end | [full lifecycle] | Reduced latency by X ms |
| Learning new tech | Picked up a new framework/tool | [self-learning approach] | Delivered feature in Y weeks |

- [ ] **FedEx-specific behavioral questions:**
  - "Why leave Wipro for FedEx directly?"
    → "I've worked on FedEx systems for X years through Wipro. I understand the domain deeply — package tracking, logistics APIs, event-driven architecture. Joining directly means I can have greater ownership, faster decision-making, and direct impact on the platform I already know."
  - "What do you know about FedEx technology?"
    → Mention: FedEx uses Java/Spring Boot, microservices, Kafka for event streaming, AWS cloud, React for frontend, real-time tracking with GPS + scan events
  - "Describe FedEx's competitive advantage in tech"
    → SenseAware (IoT), FedEx Surround (predictive intelligence), real-time tracking, supply chain visibility platform
  - "How would you improve the system you worked on?"
    → [Prepare a genuine answer about a bottleneck you identified]

**Afternoon (3h): Self-Mock Interview**
- [ ] Set a timer. Simulate each round:
  - **15 min:** Answer 10 rapid-fire Java MCQs from `tricky_output/qa.md`
  - **20 min:** Solve 1 SQL problem cold (write on paper, no IDE)
  - **30 min:** Design "Package Tracking System" HLD on paper
  - **15 min:** Answer 3 behavioral questions out loud (record yourself)
  - **10 min:** Code a form validation function in JS (no IDE, paper/whiteboard)

**Evening (1h): Gap Review**
- [ ] Identify 3 weakest areas from mock → note them for Day 9

---

### Day 9 (Apr 29) — Final Revision Day (Day Before Interview)

**Morning (2h): Speed Revision — Java + Spring (Your Strongest)**
- [ ] Re-read these specific hot topics only (skip what you know cold):
  - HashMap internals, ConcurrentHashMap
  - CompletableFuture chaining
  - Stream collectors, groupingBy, partitioningBy
  - @Transactional propagation levels
  - N+1 problem fix (JOIN FETCH vs EntityGraph)
  - JWT token flow
  - Circuit Breaker states

**Afternoon (2h): Speed Revision — Database + Frontend**
- [ ] MySQL: EXPLAIN output columns, index types, isolation levels
- [ ] Write 3 SQL queries from scratch (JOIN + GROUP BY + HAVING)
- [ ] React: useEffect cleanup, useCallback vs useMemo, React.memo
- [ ] JS: Event loop order (microtask vs macrotask), closure gotchas

**Late Afternoon (1h): System Design Quick Refresh**
- [ ] Re-read `estimation_cheatsheet.md` (numbers you should know)
- [ ] Mentally walk through 1 HLD design (Package Tracking)
- [ ] Review SOLID principles + 3 key design patterns

**Evening (1h): Pre-Interview Prep**
- [ ] Review your self-intro (say it out loud 3 times)
- [ ] Review your 5 STAR stories (say key metrics out loud)
- [ ] Review `INTERVIEW_PLAYBOOK.md` — day-of checklist
- [ ] Prepare 3-4 questions to ask the interviewer:
  1. "What does the tech stack look like for the team I'd join?"
  2. "How does FedEx handle deployments — blue-green, canary?"
  3. "What's the team structure — how many engineers, what's the sprint cadence?"
  4. "What's the biggest technical challenge the team is facing right now?"
- [ ] **Sleep early. No late-night cramming.**

---

## Quick Reference: Top 30 Questions They WILL Ask

### Java (expect 5-8 questions)
1. HashMap internal working (hashing, collision, resize, treeification)
2. ConcurrentHashMap vs Collections.synchronizedMap
3. Stream vs parallel stream — when to use, pitfalls
4. CompletableFuture — thenApply vs thenCompose vs thenCombine
5. Virtual Threads vs Platform Threads (Java 21)
6. String pool + "how many objects created?"
7. equals() and hashCode() contract
8. Immutability — how to create immutable class
9. Garbage Collection — G1 vs ZGC, when does GC run
10. Exception handling — checked vs unchecked, try-with-resources

### Spring Boot (expect 4-6 questions)
11. Spring Bean lifecycle + scopes (singleton, prototype, request)
12. @Transactional — propagation, isolation, rollbackFor
13. How does Spring Security filter chain work?
14. @Async — how it works, thread pool config
15. Spring Boot auto-configuration — how does it work?
16. Actuator endpoints — health, metrics, custom
17. How to handle N+1 in JPA? (JOIN FETCH, EntityGraph, @BatchSize)

### SQL (expect 3-5 questions)
18. Write a query: 2nd highest salary per department
19. EXPLAIN output — what does type=ALL vs ref vs range mean?
20. Index: composite index column order matters — why?
21. Difference between WHERE and HAVING
22. Deadlock — how to detect and prevent
23. ACID properties with real examples

### React/Frontend (expect 2-4 questions)
24. useEffect dependency array — empty vs specific vs none
25. Virtual DOM — how reconciliation works
26. Controlled vs Uncontrolled components
27. Event loop — Promise vs setTimeout execution order
28. Closures — practical use cases

### System Design (expect 1-2 questions)
29. Design a package tracking system (HLD)
30. Design a notification service (LLD)

---

## Aptitude Quick Prep (For MCQ Round)

FedEx OA includes aptitude questions. Spend 30 min on Day 4 or 5 evening:

**Common types:**
- Speed/Distance/Time: `speed = distance/time`, relative speed, trains
- Pipes & Cisterns: `rate = 1/time`, combined rate = sum of individual rates
- Probability: Basic counting, independent events
- Ratios & Proportions: Direct/inverse proportion
- Number Series: Find the pattern
- Logical Reasoning: Syllogisms, blood relations, direction sense

**Quick formulas to memorize:**
```
Speed-Distance: D = S × T
Relative speed (same direction): S1 - S2
Relative speed (opposite): S1 + S2
Pipe filling: 1/A + 1/B (together) or 1/A - 1/B (with leak)
Probability: P(A∪B) = P(A) + P(B) - P(A∩B)
Permutation: nPr = n!/(n-r)!
Combination: nCr = n!/[r!(n-r)!]
```

---

## File Quick-Access Map

| What you need | File path |
|---------------|-----------|
| Java Streams | `backend/java/streams/qa.md` |
| Multithreading | `backend/java/multithreading/qa.md` |
| Collections | `backend/java/collections/qa.md` |
| OOP + SOLID | `backend/java/oops/qa.md` |
| Tricky Output MCQs | `backend/java/tricky_output/qa.md` |
| Java 8-21 features | `backend/java/java_versions/java_8_to_21_features.md` |
| Spring Core | `backend/spring_boot/core_concepts/qa.md` |
| REST APIs | `backend/spring_boot/rest_apis/qa.md` |
| Security/OAuth2 | `backend/spring_boot/security_oauth2/qa.md` |
| Hibernate/JPA | `backend/hibernate_jpa/qa.md` |
| API Design | `backend/api_design/qa.md` |
| Microservices | `backend/microservices/design_patterns/qa.md` |
| Kafka | `backend/microservices/kafka/qa.md` |
| Resilience | `backend/microservices/resilience/qa.md` |
| Design Patterns | `backend/design_patterns/qa.md` |
| MySQL Queries | `database/mysql/queries/qa.md` |
| MySQL Indexing + EXPLAIN | `database/mysql/indexing/qa.md` |
| MySQL Transactions | `database/mysql/transactions/qa.md` |
| MongoDB | `database/mongodb/qa.md` |
| Schema Design | `database/schema_design/qa.md` |
| React Hooks | `frontend/react/hooks/qa.md` |
| JS Core | `frontend/javascript_core/qa.md` |
| TypeScript | `frontend/typescript/qa.md` |
| HLD Q&A | `system_design/hld/qa.md` |
| HLD E2E Designs | `system_design/hld/end_to_end_designs.md` |
| Estimation Cheatsheet | `system_design/hld/estimation_cheatsheet.md` |
| LLD Q&A | `system_design/lld/qa.md` |
| LLD E2E Designs | `system_design/lld/end_to_end_designs.md` |
| Docker/K8s | `devops/docker_kubernetes/qa.md` |
| CI/CD | `devops/ci_cd/qa.md` |
| Behavioral | `behavioral/qa.md` |
| Interview Playbook | `INTERVIEW_PLAYBOOK.md` |
| Generic Questions | `GENERIC_PRODUCT_QUESTIONS.md` |

---

## Daily Time Budget

```
Total: ~6 hours/day (adjust to your schedule)
Morning:  3 hours (deep study — core topics)
Afternoon: 2 hours (practice/secondary topics)
Evening:  1 hour (review/behavioral/light prep)
```

**Non-negotiable daily habits:**
1. Start each morning by reviewing yesterday's weak spots (15 min)
2. End each day by writing down 3 things you got wrong today
3. Say your self-intro out loud at least once per day starting Day 5

---

## Progress Tracker

| Day | Date | Focus | Status |
|-----|------|-------|--------|
| 1 | Apr 21 | Java Core + Self-Intro | ☐ |
| 2 | Apr 22 | Spring Boot + Hibernate + Security | ☐ |
| 3 | Apr 23 | SQL + Database | ☐ |
| 4 | Apr 24 | Frontend (React + JS + TS) | ☐ |
| 5 | Apr 25 | Microservices + DevOps + Kafka | ☐ |
| 6 | Apr 26 | System Design HLD | ☐ |
| 7 | Apr 27 | System Design LLD + Design Patterns | ☐ |
| 8 | Apr 28 | Behavioral + Mock Interview | ☐ |
| 9 | Apr 29 | Final Revision (NO NEW TOPICS) | ☐ |
| D-Day | Apr 30 | **INTERVIEW** | 🎯 |
