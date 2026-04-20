# Docker & Kubernetes — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Docker

### Q1. Docker image vs container. Explain the layer system.

```
Dockerfile:
  FROM openjdk:17-slim    ← Base layer (cached, shared)
  COPY app.jar /app/      ← Application layer
  ENTRYPOINT ["java","-jar","/app/app.jar"]

Image = read-only template (stack of layers)
Container = running instance (image + writable layer on top)
```

**Layer caching:** Docker caches each layer. If `COPY pom.xml` hasn't changed, it reuses the cached layer. Order Dockerfile instructions from least-changed to most-changed.

---

### Q2. Multi-stage builds — why and how?

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline          # Cache dependencies
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime (much smaller image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Result:** Build image (~500MB) never ships. Runtime image is ~150MB (JRE only, no Maven/sources).

---

### Q3. Docker networking — bridge, host, overlay.

| Mode | Description | Use case |
|------|-------------|----------|
| **bridge** (default) | Private network, containers talk via DNS | Local dev, docker-compose |
| **host** | Container shares host's network | Performance-critical, no isolation |
| **overlay** | Multi-host networking | Docker Swarm, Kubernetes |
| **none** | No networking | Security-sensitive containers |

```yaml
# docker-compose.yml: services communicate by service name
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [db, redis]
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
  redis:
    image: redis:7-alpine
# app connects to db via hostname "db:3306"
```

---

### Q4. Dockerfile best practices.

```dockerfile
# ✅ Use specific base image tags (not :latest)
FROM eclipse-temurin:17.0.9_9-jre-alpine

# ✅ Non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

# ✅ Copy dependency files first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# ✅ .dockerignore to exclude unnecessary files
# ✅ Health check
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/actuator/health || exit 1

# ❌ AVOID:
# Running as root
# Using :latest tag
# Copying entire project in one layer
# Storing secrets in image
# Installing unnecessary packages
```

---

## Kubernetes

### Q5. Kubernetes architecture — explain the components.

```
Control Plane:
├── API Server (kube-apiserver) — REST API, all communication goes through here
├── etcd — distributed key-value store (cluster state)
├── Scheduler — assigns pods to nodes
└── Controller Manager — ensures desired state = actual state

Worker Nodes:
├── kubelet — manages pods on this node
├── kube-proxy — network rules, service routing
└── Container Runtime (containerd)

Key Objects:
├── Pod — smallest deployable unit (1+ containers)
├── Deployment — manages ReplicaSets, rolling updates
├── Service — stable network endpoint for pods
├── ConfigMap / Secret — configuration
├── Ingress — HTTP routing rules
├── HPA — auto-scaling based on metrics
└── PVC — persistent storage
```

---

### Q6. Pod, Deployment, Service, Ingress — explain each.

```yaml
# Deployment: manages pods with desired replica count
apiVersion: apps/v1
kind: Deployment
metadata:
  name: shipment-service
spec:
  replicas: 3
  selector:
    matchLabels: { app: shipment-service }
  template:
    metadata:
      labels: { app: shipment-service }
    spec:
      containers:
        - name: app
          image: fedex/shipment-service:1.2.0
          ports: [{ containerPort: 8080 }]
          resources:
            requests: { cpu: "250m", memory: "512Mi" }
            limits: { cpu: "500m", memory: "1Gi" }
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }

---
# Service: stable internal DNS name + load balancing
apiVersion: v1
kind: Service
metadata:
  name: shipment-service
spec:
  selector: { app: shipment-service }
  ports: [{ port: 80, targetPort: 8080 }]
  type: ClusterIP  # Internal only

---
# Ingress: external HTTP routing
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
spec:
  rules:
    - host: api.fedex.com
      http:
        paths:
          - path: /api/shipments
            pathType: Prefix
            backend:
              service: { name: shipment-service, port: { number: 80 } }
```

---

## Scenario-Based Questions

### Q7. At FedEx, how would you containerize and deploy the SEFS-PDDV service?

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build target/sefs-pddv.jar .
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "sefs-pddv.jar"]
```

```yaml
# Kubernetes deployment with HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: sefs-pddv-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sefs-pddv
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 70 }
```

---

### Q8. At NPCI, how do you manage secrets in Kubernetes?

```yaml
# ❌ Never hardcode secrets in YAML or images

# Option 1: Kubernetes Secrets (base64, not encrypted!)
apiVersion: v1
kind: Secret
metadata: { name: db-credentials }
type: Opaque
data:
  username: YWRtaW4=  # base64 encoded
  password: cGFzc3dvcmQ=

# Option 2: External Secrets Operator (pulls from AWS Secrets Manager)
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata: { name: db-credentials }
spec:
  refreshInterval: 1h
  secretStoreRef: { name: aws-secrets, kind: ClusterSecretStore }
  target: { name: db-credentials }
  data:
    - secretKey: password
      remoteRef: { key: /npci/prod/db-password }
```

**Best practice:** Use External Secrets Operator + AWS Secrets Manager. Secrets are never stored in Git.

---

### Q9. Explain rolling update vs blue-green vs canary deployments in K8s.

```yaml
# Rolling Update (default)
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1         # Add 1 new pod before removing old
      maxUnavailable: 0    # Never go below desired count
# Gradual replacement: v1 v1 v1 → v1 v1 v2 → v1 v2 v2 → v2 v2 v2

# Blue-Green: two full deployments, switch traffic at once
# Deploy "green" (v2) alongside "blue" (v1)
# Test green, then update Service selector to point to green
# Instant rollback: switch Service back to blue

# Canary: route small % of traffic to new version
# Use Istio VirtualService (see service_mesh/qa.md) or NGINX annotations
```

| Strategy | Rollback speed | Resource usage | Risk |
|----------|---------------|---------------|------|
| Rolling | Medium (wait for rollback) | Normal | Medium |
| Blue-Green | Instant (switch selector) | 2x (both versions running) | Low |
| Canary | Fast (reduce traffic to 0%) | 1x + small | Lowest |

---

## Gotchas & Edge Cases

### Q10. Container resource limits — what happens if exceeded?

```yaml
resources:
  requests: { cpu: "250m", memory: "512Mi" }  # Scheduler uses for placement
  limits: { cpu: "500m", memory: "1Gi" }      # Hard ceiling
```

- **CPU limit exceeded:** Container is throttled (slowed down, not killed)
- **Memory limit exceeded:** Container is OOMKilled (out of memory) and restarted

**Java heap:** Set `-XX:MaxRAMPercentage=75.0` to let JVM use 75% of container memory, leaving 25% for non-heap (metaspace, threads, NIO buffers).

---

### Q11. Pod stuck in CrashLoopBackOff — how do you debug?

```bash
kubectl describe pod <pod-name>          # Check events, exit codes
kubectl logs <pod-name> --previous       # Logs from crashed container
kubectl logs <pod-name> -f               # Stream live logs
kubectl exec -it <pod-name> -- sh        # Shell into running container
kubectl get events --sort-by=.metadata.creationTimestamp  # Cluster events
```

**Common causes:** Missing config/secrets, DB connection refused, port conflict, health check failing, OOMKilled.
