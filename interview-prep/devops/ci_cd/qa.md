# CI/CD Pipelines — Interview Q&A

> 12 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. What is CI/CD? Explain the difference between CI, CD (Delivery), and CD (Deployment).

```
Code Commit → CI → CD (Delivery) → CD (Deployment)

CI (Continuous Integration):
  - Developers merge code frequently (multiple times/day)
  - Automated build + unit tests on every push
  - Catch integration issues early
  
CD (Continuous Delivery):
  - Code is ALWAYS in a deployable state
  - Automated testing (integration, E2E)
  - Manual approval before production deploy
  
CD (Continuous Deployment):
  - Every change that passes tests → automatically deployed to production
  - No manual intervention
  - Requires high confidence in test suite
```

**At FedEx:** CI + Continuous Delivery (manual approval for prod).  
**At NPCI:** CI + Continuous Delivery with strict change management.

---

### Q2. Typical CI/CD pipeline stages.

```
┌─────────┐  ┌─────────┐  ┌──────────┐  ┌───────────┐  ┌──────────┐  ┌────────┐
│  Code   │→│  Build  │→│  Test    │→│  Security │→│  Deploy   │→│ Monitor│
│  Commit │  │ Compile │  │ Unit/Int │  │  Scan    │  │  Staging  │  │ Health │
└─────────┘  └─────────┘  └──────────┘  └───────────┘  └──────────┘  └────────┘
                                                             │
                                                       ┌─────┴──────┐
                                                       │  Approval  │
                                                       └─────┬──────┘
                                                             │
                                                       ┌─────┴──────┐
                                                       │ Deploy Prod│
                                                       └────────────┘
```

**Stages:**
1. **Source** — Git push/PR triggers pipeline
2. **Build** — Compile, resolve dependencies, create artifact
3. **Unit Test** — Run unit tests (fast, < 5 min)
4. **Code Quality** — SonarQube scan, lint
5. **Security Scan** — SAST (Snyk, Checkmarx), dependency CVE check
6. **Integration Test** — TestContainers, API tests
7. **Build Image** — Docker build + push to registry
8. **Deploy Staging** — Deploy to staging environment
9. **E2E Test** — Selenium/Playwright against staging
10. **Approval** — Manual gate for production
11. **Deploy Production** — Rolling/canary deployment
12. **Monitor** — Health checks, error rate alerts

---

### Q3. GitHub Actions — pipeline as code.

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Build & Test
        run: mvn verify
      
      - name: SonarQube Scan
        run: mvn sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}
      
      - name: Build Docker Image
        run: docker build -t ${{ env.REGISTRY }}/shipment-service:${{ github.sha }} .
      
      - name: Push to ECR
        run: docker push ${{ env.REGISTRY }}/shipment-service:${{ github.sha }}

  deploy-staging:
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to EKS
        run: |
          kubectl set image deployment/shipment-service \
            app=${{ env.REGISTRY }}/shipment-service:${{ github.sha }}

  deploy-production:
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production  # Requires approval
    steps:
      - name: Deploy to Production
        run: |
          kubectl set image deployment/shipment-service \
            app=${{ env.REGISTRY }}/shipment-service:${{ github.sha }}
```

---

## Scenario-Based Questions

### Q4. At FedEx, design a CI/CD pipeline for the SEFS-PDDV microservice deployed on PCF.

```yaml
Pipeline:
  1. PR Created:
     - Build + unit tests
     - SonarQube code quality gate
     - Snyk dependency scan
     - PR status check → approve/reject

  2. Merge to develop:
     - Full build
     - Unit + integration tests (TestContainers)
     - Docker image build + tag (develop-{sha})
     - Push to Artifactory
     - Auto-deploy to DEV environment
     - Smoke tests against DEV
  
  3. Merge to main (release):
     - Full build + all tests
     - Docker image tag (release-{version})
     - Deploy to STAGING
     - E2E tests + performance tests
     - AppDynamics baseline comparison
     - Manual approval (Release Manager)
     - Deploy to PRODUCTION (rolling update)
     - Post-deploy health checks
     - Notify Slack channel
```

**Rollback:** If error rate > threshold after deploy, auto-rollback to previous version.

---

### Q5. At NPCI, how do you ensure zero-downtime deployments in CI/CD?

1. **Database migrations first** — run backward-compatible migrations before code deploy
2. **Rolling update** — replace pods one at a time
3. **Readiness probes** — new pods only receive traffic when ready
4. **Feature flags** — deploy code dark, enable feature via flag
5. **Canary** — route 5% traffic to new version, monitor, gradually increase

**Database migration strategy:**
```
v1 code + v1 schema (current)
  ↓ Deploy v2 schema (backward compatible — add columns, not remove)
v1 code + v2 schema (both work)
  ↓ Deploy v2 code
v2 code + v2 schema (target)
  ↓ Cleanup migration (remove unused columns)
```

---

### Q6. At Hatio, how do you manage environment-specific configuration across dev/staging/prod?

```yaml
# Kubernetes ConfigMaps per environment
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: production
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_HOST: "prod-mysql-cluster.internal"
  KAFKA_BROKERS: "prod-kafka-1:9092,prod-kafka-2:9092"
  LOG_LEVEL: "WARN"

# Secrets from External Secrets Operator (never in Git)
# Environment-specific values injected at deploy time
```

**Pattern:** Same Docker image across all environments. Only configuration changes.

---

## Gotchas & Edge Cases

### Q7. "It works on my machine" — how does CI/CD solve this?

**Reproducible builds:**
1. CI runs in a clean, standardized environment (Ubuntu container)
2. Dependencies locked (pom.xml, package-lock.json)
3. Same Docker image runs everywhere (dev, staging, prod)
4. No local machine quirks affect the build

---

### Q8. Pipeline security — what to watch for?

1. **Never echo secrets in logs** — mask in CI config
2. **Least privilege** — CI service account has minimal permissions
3. **Signed commits** — verify code author
4. **Image scanning** — scan Docker images for CVEs before pushing
5. **Artifact integrity** — sign and verify Docker images (cosign)
6. **Branch protection** — require PR reviews, status checks before merge

```yaml
# GitHub Actions: secrets are automatically masked in logs
- name: Deploy
  env:
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}  # Masked as ***
  run: deploy.sh
```

---

### Q9. How do you handle database migrations in CI/CD?

**Tools:** Flyway, Liquibase

```sql
-- V1__create_shipments_table.sql
CREATE TABLE shipments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tracking_number VARCHAR(22) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL
);

-- V2__add_weight_column.sql
ALTER TABLE shipments ADD COLUMN weight DECIMAL(10,2);
```

```yaml
# CI pipeline step: run migrations before deploy
- name: Run Flyway Migrations
  run: mvn flyway:migrate -Dflyway.url=${{ secrets.DB_URL }}
```

**Rule:** Migrations must be backward-compatible. Never rename/drop columns in the same release as code changes.
