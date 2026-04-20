# Interview Playbook — Master Strategy Guide

> Your single-page game plan for cracking product-based company interviews  
> Print this. Read it the night before every interview.

---

## Interview Day Checklist

```
□ Laptop charged, IDE open, internet stable (for virtual)
□ Water bottle, notepad, pen ready
□ Quiet room, professional background (virtual)
□ Resume printed (2 copies for in-person)
□ Company research done (product, scale, tech stack, recent news)
□ 3-4 questions ready to ask the interviewer
□ STAR stories rehearsed (5-6 stories)
□ Arrived 10 min early / joined meeting 5 min early
```

---

## Interview Round Types & Strategy

### Round 1: Online Assessment (OA) / Coding Round
```
Duration: 60-90 min, 2-3 problems
Difficulty: Easy + Medium (occasionally Hard)

Strategy:
1. Read ALL problems first → start with the easiest
2. Write examples on paper before coding
3. Think of edge cases: empty input, single element, duplicates, overflow
4. Brute force first → optimize → code → test
5. Time allocation: Easy (15 min), Medium (25 min), Hard (35 min)
6. Use familiar patterns from dsa_patterns/qa.md
```

### Round 2: Technical Interview (DSA)
```
Duration: 45-60 min
Format: Live coding on shared editor / whiteboard

Strategy:
1. Clarify: "Can I assume the input is sorted?" "Are there duplicates?"
2. Think out loud — interviewer wants to hear your thought process
3. Start: "Brute force would be..." → "We can optimize with..."
4. Code cleanly — variable names matter
5. Dry run your code with the given example
6. Discuss time/space complexity WITHOUT being asked
7. If stuck: Don't panic. Say "Let me think about this differently"
```

### Round 3: Technical Interview (System Design / HLD)
```
Duration: 45-60 min
Format: Whiteboard / virtual whiteboard

Strategy (use this framework):
1. Requirements (5 min)
   - Functional: What should the system DO?
   - Non-functional: Scale, latency, availability, consistency
   - Estimations: Users, QPS, storage
   
2. High-Level Design (15 min)
   - Draw major components: Client → LB → API → Service → DB
   - Identify APIs: POST /create, GET /read
   
3. Deep Dive (20 min)
   - Database schema design
   - Caching strategy
   - Message queues for async
   - Scaling: sharding, replication, CDN
   
4. Trade-offs (5 min)
   - SQL vs NoSQL? Why?
   - CP vs AP? (CAP theorem)
   - Push vs Pull?
   - Discuss what you'd do with more time
```

### Round 4: Technical Interview (Core Java / Spring Boot / LLD)
```
Duration: 45-60 min
Format: Questions + live coding

Strategy:
1. Be ready for "explain like I'm a junior developer" questions
2. Draw diagrams (JVM memory model, Spring bean lifecycle)
3. For LLD: Start with classes, then relationships, then code
4. Know WHY, not just WHAT (Why is HashMap O(1)? Why is String immutable?)
5. If you don't know: "I haven't worked with that, but here's my understanding..."
```

### Round 5: Behavioral / Hiring Manager
```
Duration: 30-45 min
Format: STAR-based questions

Strategy:
1. Every answer must have a RESULT with numbers/impact
2. Use "I" not "we" — they want YOUR contribution
3. Have a failure story ready — shows self-awareness
4. Ask good questions at the end — shows genuine interest
5. Research the interviewer on LinkedIn before the round
```

---

## Common Traps & How to Avoid Them

### Trap 1: "Do you know X technology?"
```
If YES: "Yes, I used X in [project] for [purpose]. For example..."
If NO: "I haven't used X directly, but I've used [similar thing]. 
        I'd be happy to learn it — here's how I'd approach it..."
❌ Never say just "No."
```

### Trap 2: Silence during coding
```
❌ Coding silently for 10 minutes
✅ "I'm thinking of using a HashMap here because..."
✅ "Let me first handle the base case..."
✅ "I realize this approach won't work because... Let me try..."
```

### Trap 3: "What's your current CTC?"
```
✅ "I'd prefer to discuss the range for this role based on the 
    responsibilities and market rate."
✅ "My expectation is [range] based on my skills and the role."
❌ Don't lie about current CTC — it can be verified
```

### Trap 4: "Any questions?" → "No, I'm good"
```
❌ NEVER say you have no questions
✅ Always have 3-4 prepared (see behavioral/qa.md Q16)
```

### Trap 5: Not asking clarifying questions in coding round
```
❌ Jumping straight to code
✅ "Can the array have negative numbers?"
✅ "Should I handle null input?"
✅ "What's the expected input size?"
```

---

## How to Structure Coding Answers

```
Step 1: CLARIFY  (1-2 min)
  → Input/output format, constraints, edge cases

Step 2: APPROACH (3-5 min)
  → "Brute force: [explain]. Time: O(n²)"
  → "Optimized: [explain]. Time: O(n)"

Step 3: CODE     (15-20 min)
  → Clean code, meaningful names
  → Handle edge cases

Step 4: TEST     (3-5 min)
  → Walk through example manually
  → Test edge cases

Step 5: ANALYZE  (1-2 min)
  → Time complexity
  → Space complexity
  → "We could further optimize by..."
```

---

## How to Structure System Design Answers

```
Step 1: REQUIREMENTS    (5 min)
  → Functional + Non-functional
  → "Let me estimate: 100M users, 10% daily active..."

Step 2: API DESIGN      (5 min)
  → POST /api/v1/resource
  → Response format

Step 3: HIGH-LEVEL      (10 min)
  → Draw boxes and arrows
  → Client → CDN → LB → API Gateway → Services → DB

Step 4: DATABASE        (10 min)
  → Schema design
  → SQL vs NoSQL decision with reasoning
  → Indexing strategy

Step 5: DEEP DIVE       (15 min)
  → Caching (Redis, CDN)
  → Message queues (Kafka)
  → Scaling (horizontal, sharding)
  → Fault tolerance

Step 6: WRAP UP         (5 min)
  → Trade-offs discussed
  → "With more time, I'd add..."
```

---

## Salary Negotiation Cheat Sheet

```
1. Research: levels.fyi, Glassdoor, AmbitionBox, Blind
2. Calculate total comp: Base + Bonus + RSU + Benefits
3. Ask for range: "What's the range for this level?"
4. Counter: 15-20% above their first offer
5. Levers: Signing bonus, RSUs, title, WFH, joining date, learning budget
6. Never accept on the spot: "I need 3-5 days to evaluate"
7. Competing offers: "I have another offer at [range]. Can you match?"
8. Be respectful but firm: "I'm excited about this role, and I'd love 
   to make this work at [number]."
```

---

## Study Order: 30-Day Plan

### Week 1: Core Fundamentals
| Day | Topics | Study From |
|-----|--------|-----------|
| 1 | Java OOP, SOLID | backend/java/oops/qa.md |
| 2 | Collections, HashMap internals | backend/java/collections/qa.md |
| 3 | Multithreading, deadlocks | backend/java/multithreading/qa.md |
| 4 | Java 8 features, Streams | backend/java/streams/qa.md, java8_features/qa.md |
| 5 | Exceptions, JVM internals | backend/java/exceptions/qa.md, java_internals/qa.md |
| 6 | String pool, Memory, GC | java_internals/qa.md |
| 7 | Revision + Tricky output Qs | GENERIC_PRODUCT_QUESTIONS.md Part 8 |

### Week 2: Frameworks & Architecture
| Day | Topics | Study From |
|-----|--------|-----------|
| 8 | Spring Boot core, auto-config | backend/spring_boot/core_concepts/qa.md |
| 9 | REST APIs, Spring Security | backend/spring_boot/rest_apis/qa.md, security_oauth2/qa.md |
| 10 | Microservices patterns | backend/microservices/design_patterns/qa.md |
| 11 | Kafka, resilience patterns | backend/microservices/kafka/qa.md, resilience/qa.md |
| 12 | Database: SQL, indexing, transactions | database/ |
| 13 | React: hooks, state, performance | frontend/react/ |
| 14 | Revision + Generic Qs | GENERIC_PRODUCT_QUESTIONS.md Parts 2-6 |

### Week 3: DSA
| Day | Topics | Study From |
|-----|--------|-----------|
| 15-16 | Arrays, Hashing, Two Pointers | dsa_patterns/qa.md P1-P9 |
| 17-18 | Sliding Window, Stack, Binary Search | dsa_patterns/qa.md P10-P18 |
| 19-20 | LinkedList, Trees, Graphs | dsa_patterns/qa.md P19-P32 |
| 21 | DP, Backtracking, Heap | dsa_patterns/qa.md P33-P47 |

### Week 4: System Design + Mock
| Day | Topics | Study From |
|-----|--------|-----------|
| 22-23 | HLD: URL shortener, Rate limiter, Notification | system_design/hld/qa.md |
| 24-25 | LLD: Parking lot, LRU cache, Design patterns | system_design/lld/qa.md |
| 26-27 | DevOps: Docker, K8s, AWS, CI/CD | devops/ |
| 28 | Behavioral: STAR stories | behavioral/qa.md |
| 29 | Full mock interview (time yourself) | This playbook |
| 30 | Top 50 revision | GENERIC_PRODUCT_QUESTIONS.md (last section) |

---

## Final Tips

1. **Consistency > Intensity** — 3 hours daily beats 12-hour weekend cramming
2. **Explain to rubber duck** — if you can't explain it simply, you don't understand it
3. **Track what you don't know** — mark ❌ in progress tracker, revisit in 3 days
4. **Mock interviews** — practice with a friend or Pramp.com
5. **Sleep well** — your brain consolidates memory during sleep
6. **It's a numbers game** — rejection is normal, keep applying (aim for 5-10 companies)
7. **Every interview is practice** — even if you don't get the offer, you get better

---

> "The best time to prepare was 6 months ago. The second best time is now." — Start today.
