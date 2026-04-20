# GitHub Copilot Agent — Interview Prep Workspace Prompt

> Paste this entire prompt into GitHub Copilot Agent mode to bootstrap your workspace.

---

## My Background

- **Name:** Sai Karthik Kollapureddy
- **Role:** Java Full Stack Developer, 3.6 years at Wipro
- **Clients:** FedEx (backend microservices), UnitedHealth Group (fullstack, cloud migration)
- **Target companies:** FedEx Hyderabad, NPCI (in progress), similar product-based companies
- **Baseline cleared:** Hatio/BillDesk SDE-2

**Core stack:** Java 17, Spring Boot, Microservices, Kafka, REST, GraphQL, React.js, Angular 12, AWS (S3, CloudFront, Route 53, Lambda, CloudWatch), Docker, Kubernetes, MySQL, MongoDB, OAuth2, AppDynamics, Splunk, Grafana, JUnit, BDD Cucumber, Maven/Gradle, Git

---

## TASK: Scaffold the full interview prep workspace

### Folder Structure (follow this pattern recursively — do not deviate):

```
interview-prep/
├── INSTRUCTIONS.md
├── README.md
├── backend/
│   ├── README.md
│   ├── java/
│   │   ├── README.md
│   │   ├── streams/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── oops/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── collections/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── multithreading/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── java8_features/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── exceptions/
│   │       ├── qa.md
│   │       └── solutions/
│   ├── spring_boot/
│   │   ├── README.md
│   │   ├── core_concepts/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── rest_apis/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── security_oauth2/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── testing/
│   │       ├── qa.md
│   │       └── solutions/
│   ├── microservices/
│   │   ├── README.md
│   │   ├── design_patterns/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── kafka/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── resilience/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   └── service_mesh/
│   │       └── qa.md
│   └── graphql/
│       ├── qa.md
│       └── solutions/
├── frontend/
│   ├── README.md
│   ├── react/
│   │   ├── README.md
│   │   ├── hooks/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── state_management/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── performance/
│   │   │   └── qa.md
│   │   └── testing/
│   │       ├── qa.md
│   │       └── solutions/
│   └── angular/
│       ├── components/
│       │   └── qa.md
│       └── rxjs/
│           ├── qa.md
│           └── solutions/
├── database/
│   ├── README.md
│   ├── mysql/
│   │   ├── queries/
│   │   │   ├── qa.md
│   │   │   └── solutions/
│   │   ├── indexing/
│   │   │   └── qa.md
│   │   └── transactions/
│   │       └── qa.md
│   └── mongodb/
│       ├── qa.md
│       └── solutions/
├── devops/
│   ├── README.md
│   ├── aws/
│   │   ├── s3_cloudfront/
│   │   │   └── qa.md
│   │   └── lambda/
│   │       └── qa.md
│   ├── docker_kubernetes/
│   │   ├── qa.md
│   │   └── solutions/
│   └── ci_cd/
│       └── qa.md
└── system_design/
    ├── README.md
    ├── hld/
    │   └── qa.md
    └── lld/
        └── qa.md
```

---

## Rules for every qa.md

1. **15–20 questions** per file, scenario-based and product-company focused
2. **Question groups** (in this order every time):
   - `## Conceptual` — definitions, internals, tradeoffs
   - `## Scenario-Based` — "At FedEx/NPCI/Hatio, you face X problem…"
   - `## Coding Challenges` — problem statements only, no solutions
   - `## Gotchas & Edge Cases` — common mistakes, interview traps
3. Answers: concise, include time/space complexity and tradeoffs where relevant
4. Tag HIGH PRIORITY topics (from gap matrix) with `> ⚠️ HIGH PRIORITY — appears in JD` at the top of the file

---

## Coding Challenge Rules (CRITICAL — never break these)

- Under `## Coding Challenges` in each qa.md: list 3–5 problems with **problem statement only**
- Create stub files in the `solutions/` subfolder: empty `.java` or `.jsx` files named after the problem
- **When I write my solution in a stub file:**
  - Add your review as `// MENTOR REVIEW:` comment blocks appended **below** each method
  - `// MENTOR: What's good:`, `// MENTOR: What to improve:`, `// MENTOR: Optimised approach:`
  - **NEVER delete, replace, or rewrite my code — comments only**
  - If I have multiple attempts in the same file, review each one separately

---

## Required file contents

### INSTRUCTIONS.md must contain:
- My background (as stated above)
- Target companies and cleared status
- Complete folder tree with one-line purpose per node
- All rules from this prompt verbatim (qa.md rules + coding challenge rules)
- `## Skill Gap Analysis` — table of: Skill | Appears In JD | Resume Strength | Priority
- Progress tracking format: `[ ]` Not started · `[~]` In progress · `[x]` Done
- Handoff instructions: "If you are a new AI model, read this file first. Do not re-scaffold. Continue from the progress tracker."

### README.md (root) must contain:
- Full folder tree (visual)
- Progress tracker table: `| Topic | Status | Confidence (1-5) | Last Reviewed |`
- Quick-start: "How to use this repo in 3 steps"

### Each subfolder README.md must contain:
- What this section covers
- Links to its qa.md and solutions/
- Progress status for each topic inside

---

## Execution Order (follow exactly, pause where indicated)

1. Parse all PDFs → build gap matrix
2. Generate `INSTRUCTIONS.md`
3. Generate root `README.md`
4. Scaffold the entire folder and file structure (empty qa.md stubs are fine at this stage)
5. Fully populate `backend/java/streams/qa.md` as the reference template for all others
6. Create stub files in `backend/java/streams/solutions/`
7. **PAUSE** — present me the first coding challenge from streams and ask me to solve it before continuing
8. After I submit, add mentor review comments to my solution file
9. Ask: "Ready to move to the next topic?" — proceed only on my confirmation

---

> **For future AI models:** Read `INSTRUCTIONS.md` first. The progress tracker shows where Karthik left off. Match the qa.md format from `backend/java/streams/qa.md` exactly. Never regenerate completed sections. Always pause after posting a coding challenge and wait for a solution before proceeding.
