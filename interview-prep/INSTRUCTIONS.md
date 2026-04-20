# Interview Prep Workspace — INSTRUCTIONS

> **Owner:** Sai Karthik Kollapureddy  
> **Created:** April 20, 2026  
> **Purpose:** Structured preparation for product-based company interviews  

---

## Candidate Background

- **Name:** Sai Karthik Kollapureddy  
- **Experience:** 3.6+ years as Java Full Stack Developer at Wipro Technologies Ltd.  
- **Current Role:** Java Full Stack Developer — Client: UnitedHealth Group (Feb 2025 – Present)  
- **Previous Role:** Backend Developer — Client: Federal Express (Sep 2022 – Jan 2025)  
- **Education:** B.Tech, Pragati Engineering College (CGPA: 8.97/10)  
- **Location:** Hyderabad, India  

### Key Projects
1. **UHG — My Practice Profile:** Provider portal for healthcare demographic data sync. Stack: Java 17, Spring Boot, Kafka, React.js, GraphQL, AWS (S3, CloudFront, Route 53), Kubernetes.  
2. **FedEx — SEFS-PDDV:** Real-time pickup/delivery data processing microservices ecosystem handling STARV scanner events.  
3. **FedEx — On Road Event Gateway:** End-to-end data pipelines consuming STARV/FORGE scanner events; picture proof of delivery for fedex.com.  
4. **FedEx — Sort Package Scan Gateway:** Hub integration for package movement, routing, and sort facility data to tracking/rating/linehaul systems.  

### Current Technical Stack
| Layer | Technologies |
|-------|-------------|
| Backend | Java 17, Spring Boot, Microservices, JMS, Kafka, RESTful APIs, GraphQL (Queries) |
| Frontend | React.js, Angular 12, HTML5, CSS3, TypeScript |
| Cloud/DevOps | AWS (S3, CloudFront, Route 53, CloudWatch, Lambda), Docker, Kubernetes |
| Monitoring | Grafana, AppDynamics, Splunk, Ready API (Perf Testing) |
| Database | MySQL, MongoDB (Spring Data) |
| Security | OAuth2 Authentication & Authorization |
| Testing | BDD Cucumber Test Suites |

### Certifications
- AWS Certified Cloud Practitioner Fundamentals (2023)
- AZ-900: Microsoft Azure Fundamentals (2022)
- GitHub Foundations Certification (2024)
- Java Full Stack — StackRoute (2022)
- Core Java, Spring Boot & Microservices L1 — Wipro (2025)
- Cloud Migration Appreciation — Wipro + UHG (April 2026)

---

## Target Companies

| Company | Role | Status | Difficulty Baseline |
|---------|------|--------|---------------------|
| **Hatio/BillDesk** | SDE-2 (Fullstack) | ✅ CLEARED | Baseline (use as floor) |
| **NPCI** | Fullstack Developer | 🔄 In Progress (form submitted) | Harder than Hatio |
| **FedEx** | Full Stack Developer II | 📋 Applied | Harder than Hatio |

### Company Profiles
- **Hatio (BillDesk subsidiary):** Engineering-first, payment processor. Real-time financial transaction systems. Expects: E2E feature delivery, module ownership, React + Java + SpringBoot + Kafka + RDBMS.
- **NPCI:** India's payment infrastructure (UPI, IMPS, RuPay, NACH). Mission-critical, ultra-high-throughput, zero-downtime systems. Expects: Deep Java, concurrency, transaction safety, system design, security.
- **FedEx:** Global logistics tech. Real-time tracking, event-driven architecture, cloud-native microservices. Expects: Java 8+, Spring Cloud Gateway, JUnit5, Jenkins CI/CD, PCF/PAAS, AppDynamics, Splunk.

---

## Skill Gap Analysis

> Cross-referenced from Resume vs. JD keywords. Gaps sorted by interview impact.

### Gap Matrix

| Skill | In Resume? | FedEx JD | Hatio JD | NPCI (Inferred) | Gap Level | Priority |
|-------|-----------|----------|----------|-----------------|-----------|----------|
| Spring Cloud Gateway | ❌ No | **Mandatory** | — | Likely | 🔴 HIGH | P0 |
| JUnit5 / Unit Testing depth | ❌ Has Cucumber only | **Mandatory** | Expected | Expected | 🔴 HIGH | P0 |
| Multithreading / Concurrency | Not explicit in resume | Expected | Expected | **Critical** | 🔴 HIGH | P0 |
| System Design (HLD/LLD) | Not demonstrated | Expected | Expected | Expected | 🔴 HIGH | P0 |
| Design Patterns (GoF + Microservice) | Not explicit | Expected | Expected | Expected | 🔴 HIGH | P0 |
| Jenkins CI/CD pipelines | ❌ Not mentioned | **Mandatory** | — | Likely | 🟡 MEDIUM | P1 |
| PCF (PAAS) / Cloud Foundry | ❌ No | **Mandatory** | — | — | 🟡 MEDIUM | P1 |
| PostgreSQL / Oracle RDBMS | ❌ Has MySQL only | — | **Required** | Likely | 🟡 MEDIUM | P1 |
| Kubernetes (deep — scaling, helm, probes) | ⚠️ Basic mention | — | — | Likely | 🟡 MEDIUM | P1 |
| Docker (Dockerfile, compose, networking) | ⚠️ Basic mention | — | — | Likely | 🟡 MEDIUM | P1 |
| Java Streams (advanced patterns) | Not demonstrated | Expected | Expected | Expected | 🟡 MEDIUM | P1 |
| Collections internals | Not demonstrated | Expected | Expected | Expected | 🟡 MEDIUM | P1 |
| Transaction management (distributed) | Not explicit | Expected | Expected | **Critical** | 🟡 MEDIUM | P1 |
| Resilience patterns (Circuit Breaker, Retry) | Not explicit | Expected | — | Expected | 🟡 MEDIUM | P1 |
| Maven / Gradle (build lifecycle) | Implicit only | **Mandatory** | — | Likely | 🟢 LOW | P2 |
| Dynatrace | ❌ No | Listed | — | — | 🟢 LOW | P2 |
| AeroSpike NoSQL | ❌ No | — | Mentioned | — | 🟢 LOW | P2 |
| Postman / Insomnia (API testing) | Not mentioned | Listed | — | — | 🟢 LOW | P2 |

### Strengths (Already Strong — Maintain)
- Java 17, Spring Boot, Microservices architecture
- Kafka event-driven architecture
- React.js frontend development
- AWS services (S3, CloudFront, Route 53, Lambda)
- REST API design and GraphQL queries
- OAuth2 security implementation
- AppDynamics + Splunk monitoring
- Real-world FedEx logistics + UHG healthcare domain experience
- Agile/Scrum, BDD testing, code reviews

### Strategy
1. **P0 gaps** are embedded into qa.md questions with higher weight (more questions, deeper scenarios)
2. **P1 gaps** get dedicated scenario questions in each relevant qa.md
3. **P2 gaps** get 1-2 awareness questions — enough to not blank in an interview
4. **Strengths** still get reviewed — interviewers probe depth on claimed skills
5. Questions use **JD keywords verbatim**: "Spring Cloud Gateway service", "STARV scanner events", "provider portal", "payment processor"

---

## Folder Structure

```
interview-prep/
├── INSTRUCTIONS.md              # THIS FILE — goals, rules, context
├── README.md                    # Master index, progress tracker, quick-start
│
├── backend/
│   ├── README.md                # Backend section overview
│   ├── java/
│   │   ├── README.md            # Java core topics index
│   │   ├── streams/
│   │   │   ├── README.md        # Topic intro + learning resources
│   │   │   ├── qa.md            # 15-20 scenario Q&A + coding challenges
│   │   │   └── solutions/       # Stub .java files for coding challenges
│   │   ├── oops/                # OOP concepts, SOLID, design principles
│   │   ├── collections/         # Collections internals, HashMap, ConcurrentMap
│   │   ├── multithreading/      # Threads, executors, CompletableFuture, locks
│   │   ├── java8_features/      # Lambdas, Optional, functional interfaces
│   │   └── exceptions/          # Exception hierarchy, custom exceptions, best practices
│   │
│   ├── spring_boot/
│   │   ├── core_concepts/       # Auto-config, starters, actuator, profiles
│   │   ├── rest_apis/           # Controller design, validation, error handling
│   │   ├── security_oauth2/     # Spring Security, OAuth2, JWT, CORS
│   │   └── testing/             # JUnit5, Mockito, @SpringBootTest, TestContainers
│   │
│   ├── microservices/
│   │   ├── design_patterns/     # Saga, CQRS, API Gateway, Service Discovery
│   │   ├── kafka/               # Producers, consumers, partitions, exactly-once
│   │   ├── resilience/          # Circuit breaker, retry, bulkhead, rate limiting
│   │   └── service_mesh/        # Spring Cloud Gateway, Istio basics, load balancing
│   │
│   └── graphql/                 # Queries, mutations, schema design, N+1 problem
│
├── frontend/
│   ├── react/
│   │   ├── hooks/               # useState, useEffect, useCallback, useMemo, custom hooks
│   │   ├── state_management/    # Context, Redux, Zustand, state lifting
│   │   ├── performance/         # React.memo, lazy loading, code splitting, profiler
│   │   └── testing/             # Jest, React Testing Library, integration tests
│   └── angular/
│       ├── components/          # Component lifecycle, directives, pipes, modules
│       └── rxjs/                # Observables, operators, subjects, error handling
│
├── database/
│   ├── mysql/
│   │   ├── queries/             # Complex joins, subqueries, window functions
│   │   ├── indexing/            # B-tree, composite, covering, EXPLAIN analysis
│   │   └── transactions/        # ACID, isolation levels, deadlocks, distributed txn
│   └── mongodb/                 # Document design, aggregation pipeline, indexing
│
├── devops/
│   ├── aws/
│   │   ├── s3_cloudfront/       # Static hosting, CDN, cache invalidation
│   │   └── lambda/              # Serverless, cold start, event sources, SAM
│   ├── docker_kubernetes/       # Dockerfile, compose, K8s pods, deployments, services
│   └── ci_cd/                   # Jenkins pipelines, GitLab CI, build strategies
│
└── system_design/
    ├── hld/                     # High-level: load balancers, caching, DB sharding
    └── lld/                     # Low-level: class diagrams, API contracts, schemas
```

**Each leaf folder contains:**
- `README.md` — Topic intro, key concepts checklist, resource links
- `qa.md` — 15-20 scenario-based Q&A + coding challenges
- `solutions/` — Stub files for coding challenges

---

## Rules (Verbatim)

### QA File Rules
1. 15–20 scenario-based Q&A (product company style — "At FedEx/Hatio/NPCI, how would you...")
2. Questions grouped: **Conceptual → Scenario → Code Challenge → Gotchas**
3. Answers: concise, no fluff, include complexity/tradeoffs where relevant
4. Difficulty weighting:
   - Hatio-cleared level = baseline floor
   - NPCI and FedEx = harder (deeper system design, concurrency, scale)

### Coding Challenge Rule (CRITICAL)
- In each qa.md under a coding section, add a `## Coding Challenges` block
- List 3–5 problems with problem statement only
- Create a corresponding `solutions/` subfolder with empty `.java` or `.jsx` stub files
- **When candidate submits a solution: ADD REVIEW AS COMMENTS ONLY inside the file**
- **NEVER delete or replace candidate code — append `// MENTOR REVIEW:` blocks below each method**

### JD Keyword Integration
- FedEx questions must reference: Spring Cloud Gateway, STARV scanners, PCF/PAAS, AppDynamics, Jenkins, JUnit5
- Hatio questions must reference: payment processing, financial transactions, module ownership, sprint delivery, AeroSpike
- NPCI questions must reference: UPI, high-throughput transaction processing, zero-downtime, payment security

---

## Progress Tracking Format

Use these markers in README.md and per-topic README.md files:

```
[ ] Not started
[~] In progress
[x] Done
```

Each topic tracks: **Status | Self-Score (1-10) | Last Reviewed Date**

---

## How a New AI Model Should Pick Up Where We Left Off

1. **Read this file first** — it contains all context, rules, and the gap matrix.
2. **Read `README.md`** — check the progress tracker to see what's done vs. pending.
3. **Check `solutions/` folders** — if files have code, the candidate has attempted them. Review but NEVER overwrite.
4. **Continue populating `qa.md` files** in the order shown in the progress tracker.
5. **Maintain difficulty calibration:** Hatio = baseline, NPCI/FedEx = harder.
6. **Use JD keywords verbatim** in scenario questions.
7. **After each qa.md is populated**, pause and ask the candidate to solve the first coding challenge before moving to the next topic.
8. **Review protocol:** When reviewing candidate solutions, append `// MENTOR REVIEW:` comment blocks. Never delete or modify candidate code.
9. **Gap matrix drives priority:** P0 gaps get more questions and deeper scenarios.

---

## Session Log

| Date | Activity | Notes |
|------|----------|-------|
| 2026-04-20 | Workspace created | PDFs parsed, gap matrix built, streams qa.md populated |
