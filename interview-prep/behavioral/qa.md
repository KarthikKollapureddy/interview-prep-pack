# Behavioral & HR Interview — Q&A Bank

> 20+ questions with STAR framework answers  
> Tailored to your 3.6 years Java Fullstack experience at Wipro (FedEx + UHG projects)

> **⚠️ TODO:** Personalize ALL STAR stories below with YOUR real experiences, metrics, and outcomes.  
> Replace placeholder `[X]` values with actual numbers. Add specific technical details from your FedEx/UHG projects.  
> The STAR frameworks and question templates are ready — you just need to fill in YOUR stories.

---

## The STAR Method

Every answer should follow this structure:

```
S — Situation:  Set the context (project, team, timeline)
T — Task:       What was your specific responsibility?
A — Action:     What did YOU do? (Be specific, use "I" not "we")
R — Result:     Quantifiable outcome (%, time saved, impact)
```

**Rules:**
- Keep answers to 2-3 minutes
- Always end with the RESULT (interviewers remember the ending)
- Prepare 5-6 stories that can be adapted to multiple questions
- Never badmouth previous employers or teammates

---

## Your Story Bank (Prepare These)

Map your real experiences to these story templates:

| Story ID | Project | Theme | Can Answer |
|----------|---------|-------|-----------|
| S1 | FedEx Track API | Performance optimization | "Tell me about a challenge", "Optimization story" |
| S2 | FedEx Kafka integration | Learning new tech | "Learned something new", "Out of comfort zone" |
| S3 | UHG Claims Processing | Bug fix under pressure | "Tight deadline", "Production issue" |
| S4 | Any sprint conflict | Conflict resolution | "Disagreed with teammate", "Handled conflict" |
| S5 | Any code review | Gave/received feedback | "Mentoring", "Constructive criticism" |
| S6 | Any failed deployment | Failure story | "Biggest mistake", "What you learned" |

---

## Section 1: Self Introduction (2 minutes)

### Q1. Tell me about yourself.

**Structure:** Present → Past → Future

```
"Hi, I'm Sai Karthik, a Java Fullstack Developer with 3.6 years of experience
at Wipro, working on enterprise applications for FedEx and UnitedHealth Group.

On the backend, I work with Java 17, Spring Boot, Microservices, Kafka, and 
REST APIs. On the frontend, I've used React and Angular. For databases, I work
with MySQL and MongoDB, and I'm experienced with AWS services like S3, Lambda,
and CloudFront.

At FedEx, I built tracking APIs handling [X] requests/day and implemented event-driven 
architecture with Kafka. At UHG, I worked on claims processing with strict HIPAA compliance.

I'm now looking to join a product-based company where I can work on challenging
problems at scale, own features end-to-end, and grow as an engineer.

I'm excited about [COMPANY NAME] because [specific reason — product, tech, scale]."
```

---

## Section 2: Project Deep-Dive

### Q2. Walk me through a challenging project you worked on.

**Use Story S1: FedEx Track API**

```
S: At FedEx, I was part of the team building the package tracking microservice 
   that served real-time tracking data to the FedEx mobile app and website.

T: I was responsible for designing and implementing the REST API layer with 
   Spring Boot, integrating with Kafka for event-driven updates, and 
   ensuring the service met SLA requirements for response time.

A: I designed the API with pagination and caching using Redis to reduce 
   database load. When we noticed high latency during peak hours, I profiled 
   the service and found N+1 query issues in our JPA layer. I refactored 
   to use batch fetching and custom queries, reducing response time by [X]%.
   I also implemented circuit breakers with Resilience4j for downstream 
   service failures.

R: The service handled [X] requests/day with p99 latency under [X]ms. 
   The optimization reduced average response time by [X]% and eliminated 
   timeout errors during peak Black Friday traffic.
```

### Q3. What's the most technically complex thing you've built?

*Adapt Story S1 or S2. Focus on architecture decisions and trade-offs.*

---

## Section 3: Conflict & Collaboration

### Q4. Tell me about a time you disagreed with a teammate.

```
S: During a code review at [project], a senior developer and I disagreed 
   on whether to use synchronous REST calls or async Kafka messaging 
   for inter-service communication.

T: I believed Kafka was the better choice for our use case (decoupled, 
   resilient), but needed to convince the team.

A: Instead of arguing, I created a small proof-of-concept with both 
   approaches and documented the pros/cons:
   - REST: simpler but tightly coupled, cascading failures
   - Kafka: resilient, async, but adds complexity
   I presented data on failure rates and latency to the team.

R: The team agreed on the Kafka approach for write operations and kept 
   REST for read queries. The hybrid approach reduced cascading failures 
   and improved system resilience. I learned that data-driven arguments 
   are more persuasive than opinions.
```

### Q5. Tell me about a time you received critical feedback.

```
S: During a code review early in my career, a senior developer pointed 
   out that my code had poor error handling and no unit tests.

T: I needed to improve my code quality practices.

A: I took the feedback constructively. I:
   1. Asked the reviewer to pair-program with me on proper testing
   2. Started writing tests first (TDD approach for critical paths)
   3. Created a code review checklist for the team
   
R: My code review rejection rate dropped from [X]% to near zero.
   The checklist I created was adopted by the team and improved 
   overall code quality.
```

---

## Section 4: Problem Solving Under Pressure

### Q6. Tell me about a production issue you resolved.

**Use Story S3: UHG Claims Processing**

```
S: On a Friday evening, we received alerts that the claims processing 
   service was timing out, affecting [X] users. The on-call engineer 
   escalated to the team.

T: As the developer most familiar with the service, I needed to 
   diagnose and fix the issue quickly.

A: I checked the monitoring dashboards (CloudWatch/Grafana) and found:
   1. Database connection pool was exhausted
   2. Traced to a new query that wasn't using an index
   3. The query was part of a recent deployment
   
   I immediately added the missing index as a hotfix, verified 
   the connection pool recovered, and monitored for 30 minutes.
   Next week, I added a database migration script and load tests 
   to prevent recurrence.

R: Service recovered within 20 minutes. I then proposed adding 
   query performance tests to our CI pipeline, which caught 2 
   similar issues before they reached production.
```

### Q7. Tell me about a time you worked under a tight deadline.

```
S: We had a sprint goal to deliver [feature] in 2 weeks, but halfway 
   through, a critical production bug consumed 3 days of the sprint.

T: I needed to deliver both the bug fix and the feature on time.

A: I prioritized ruthlessly:
   1. Fixed the production bug first (P0)
   2. Simplified the feature scope — delivered MVP, deferred nice-to-haves
   3. Communicated the adjusted timeline to stakeholders daily
   4. Worked focused hours instead of burning out with overtime

R: Delivered the bug fix in 1 day and the feature MVP on time. 
   The remaining enhancements were completed in the next sprint. 
   Stakeholders appreciated the transparency.
```

---

## Section 5: Leadership & Ownership

### Q8. Tell me about a time you took ownership beyond your role.

```
S: I noticed our team spent significant time debugging issues because 
   our logging was inconsistent across microservices.

T: Nobody was assigned to fix this, but I saw an opportunity to 
   improve developer productivity.

A: I created a shared logging library with:
   - Structured JSON logging
   - Correlation IDs for tracing requests across services
   - Standard log levels and patterns
   I presented it in a team meeting and helped onboard services.

R: Debugging time for cross-service issues reduced by ~40%.
   The library was adopted by 3 other teams.
```

### Q9. Tell me about a time you mentored someone.

```
S: A new junior developer joined our team and was struggling with 
   Spring Boot microservices patterns.

T: As someone who had ramped up on the same codebase, I took 
   initiative to help them.

A: I:
   1. Created a step-by-step onboarding document
   2. Pair-programmed for 30 min daily for the first 2 weeks
   3. Assigned progressively complex tasks with detailed reviews
   4. Explained the "why" behind architectural decisions

R: The developer became productive within 3 weeks (vs typical 6 weeks).
   They later told me the pair-programming sessions were the most 
   valuable part of their onboarding.
```

---

## Section 6: Failure & Learning

### Q10. Tell me about your biggest failure.

```
S: Early in my career, I deployed a code change that caused data 
   inconsistency in [service]. The change wasn't adequately tested 
   for edge cases.

T: I needed to fix the issue and ensure it never happened again.

A: I:
   1. Immediately owned the mistake — didn't blame others
   2. Wrote a rollback script and restored data integrity
   3. Conducted a blameless post-mortem with the team
   4. Introduced mandatory integration tests for data-critical paths
   5. Added pre-deployment checklists

R: Zero similar incidents since. The post-mortem culture became a 
   team practice. I learned that "it works on my machine" isn't 
   sufficient — automated testing at every layer is essential.
```

### Q11. What's something you wish you had done differently?

*Pick a real scenario. Show self-awareness and growth. Don't pick something trivial.*

---

## Section 7: Culture Fit

### Q12. Why are you leaving your current company?

```
"I've had a great learning experience at Wipro, working with enterprise 
clients like FedEx and UHG. Now I want to transition to a product-based 
company where I can:
1. Own features end-to-end rather than work on client-driven specs
2. Work on products used by millions of users
3. Collaborate with strong engineering teams that prioritize quality
I see [COMPANY NAME] as the perfect next step for this."
```

**Rules:** 
- Never badmouth current employer
- Frame as "growing toward" not "running from"
- Be specific about what attracts you to the target company

### Q13. Why this company specifically?

**Research template (fill in before each interview):**
```
1. Product: What do they build? What excites you about it?
2. Scale: How many users? What tech challenges at that scale?
3. Engineering culture: Blog posts, open source, tech talks
4. Recent news: Funding, product launches, engineering blog posts
5. Personal connection: Do you use their product?
```

### Q14. Where do you see yourself in 5 years?

```
"In 5 years, I see myself as a Senior/Staff Engineer who:
1. Designs and architects systems end-to-end
2. Mentors junior developers
3. Contributes to technical strategy and decision-making
4. Has deep expertise in distributed systems and cloud-native architecture
I want to grow with a company that invests in engineering excellence."
```

---

## Section 8: Amazon Leadership Principles (If Interviewing at Amazon)

Amazon asks behavioral questions mapped to their 16 Leadership Principles. Prepare one story per principle:

| Principle | What They're Looking For | Your Story |
|-----------|------------------------|-----------|
| Customer Obsession | Put customer first | FedEx tracking UX improvement |
| Ownership | Think long-term, act on behalf of entire company | Logging library initiative (Q8) |
| Invent and Simplify | Find simpler solutions | Refactored N+1 query issue |
| Are Right, A Lot | Good judgment | Kafka vs REST decision (Q4) |
| Learn and Be Curious | Never stop learning | Kafka integration (S2) |
| Hire and Develop the Best | Raise the bar | Mentoring story (Q9) |
| Insist on Highest Standards | Relentlessly high standards | Code quality checklist (Q5) |
| Bias for Action | Speed matters | Production bug fix (Q6) |
| Dive Deep | Get into details | Performance profiling story |
| Deliver Results | Focus on outcomes | Tight deadline delivery (Q7) |
| Earn Trust | Self-critical, benchmark against best | Owning failures (Q10) |

---

## Section 9: HR Round Questions

### Q15. What are your salary expectations?

```
Strategy:
1. Research market rate (levels.fyi, Glassdoor, AmbitionBox)
2. Never give a number first if possible
3. If pressed: "Based on my research for this role in [city], 
   I'm looking at [range]. But I'm open to discussing based on 
   the complete package."
4. Always negotiate — first offer is rarely the best
```

### Q16. Do you have any questions for me?

**Always ask 3-4 questions. It shows interest.**

Good questions:
```
1. "What does a typical day look like for this role?"
2. "What's the team's biggest technical challenge right now?"
3. "How do you measure success for this position in the first 6 months?"
4. "What's the engineering culture like? Code reviews, testing practices?"
5. "What opportunities are there for learning and growth?"
```

Bad questions (avoid):
```
❌ "What does the company do?" (shows no research)
❌ "How soon can I get promoted?" (premature)
❌ "What's the work-life balance?" (ask carefully, rephrase as culture question)
```

---

## Section 10: Negotiation Playbook

### After receiving an offer:

```
1. Express excitement: "I'm really excited about this opportunity"
2. Ask for time: "Could I have 3-5 days to evaluate?"
3. Research: Compare with market data
4. Counter: "Based on my skills and market data, I was hoping for [X].
   Is there flexibility?"
5. Non-salary levers: Signing bonus, RSUs, WFH days, title, learning budget
6. Get it in writing
```

**Key rules:**
- Never accept on the spot
- Competing offers strengthen your position
- Be respectful but firm
- Total compensation = base + bonus + RSUs + benefits
