# Distributed Systems Fundamentals — Interview Q&A

> Concepts from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
> Covers: Scalability, CAP Theorem, Consistency & Availability Patterns, Trade-offs
> **Priority: P0** — Every system design interview starts with these fundamentals

---

## Q1. Performance vs Scalability — What's the difference?

```
Performance Problem:  System is slow for a SINGLE user
Scalability Problem:  System is fast for one user but SLOW under heavy load

A service is SCALABLE if increased resources yield proportional performance gains.

Example:
  - Adding 2x servers should roughly double throughput
  - If it doesn't, you have a scalability bottleneck

Key Insight: You can have good performance but poor scalability
  - A single beefy server handles 1 request in 10ms (great performance)
  - But at 10K concurrent users, response time jumps to 5s (poor scalability)
```

**Vertical vs Horizontal Scaling:**
```
Vertical Scaling (Scale UP):
  - Bigger machine: more CPU, RAM, SSD
  - Simpler architecture
  - Has a ceiling (hardware limits)
  - Single point of failure
  - Example: Upgrade from 16GB → 128GB RAM

Horizontal Scaling (Scale OUT):
  - More machines of same size
  - No theoretical ceiling
  - Requires load balancer
  - More complex (stateless services, distributed state)
  - Example: 3 servers → 10 servers behind a load balancer

Rule of Thumb: Start vertical, go horizontal when you hit limits
```

---

## Q2. Latency vs Throughput — Explain with examples.

```
Latency:    TIME to perform one action (measured in ms)
Throughput: NUMBER of actions per unit time (measured in req/sec, QPS)

They are NOT inversely proportional — you can optimize both.

Goal: Maximize throughput with ACCEPTABLE latency

Real-world analogy:
  Latency    = How long one car takes to travel a highway (travel time)
  Throughput = How many cars pass a point per hour (bandwidth)

  A wider highway (more lanes) increases throughput without reducing latency.
  A shorter highway reduces latency without affecting throughput.

System Design Example:
  Single DB:     Latency=5ms, Throughput=200 QPS
  Add caching:   Latency=1ms, Throughput=5000 QPS   ← Both improved
  Add replicas:  Latency=5ms, Throughput=800 QPS    ← Throughput improved, latency same
```

---

## Q3. CAP Theorem — Explain it. Which do real systems choose?

```
In a DISTRIBUTED system, you can only guarantee 2 of 3:

  C — Consistency:         Every read gets the most recent write (or error)
  A — Availability:        Every request gets a response (may not be latest)
  P — Partition Tolerance:  System works despite network partitions

┌─────────────────────────────────────────────┐
│                CAP Theorem                  │
│                                             │
│            Consistency (C)                  │
│               /       \                     │
│              /         \                    │
│            CP           CA ← Not practical  │
│            /               \  (networks     │
│           /                 \  always fail)  │
│  Partition (P) ───── AP ───── Availability  │
│        Tolerance              (A)           │
└─────────────────────────────────────────────┘

Networks ARE unreliable → P is non-negotiable
So the real choice is: CP or AP

CP Systems (Consistency + Partition Tolerance):
  - Waiting for partitioned node → may timeout (error)
  - Choose when: atomic reads/writes required
  - Examples: Banking systems, inventory systems
  - Tech: MongoDB (strong reads), HBase, Redis (single master)

AP Systems (Availability + Partition Tolerance):
  - Returns available data (may not be latest)
  - Choose when: eventual consistency is OK
  - Examples: Social media feeds, DNS, shopping cart
  - Tech: Cassandra, DynamoDB, CouchDB

Interview Tip: "Networks aren't reliable, so we always need partition
tolerance. The real trade-off is between consistency and availability."
```

**Real-World CAP Decisions:**
```
┌──────────────────┬──────────┬────────────────────────────┐
│ System           │ Choice   │ Reasoning                  │
├──────────────────┼──────────┼────────────────────────────┤
│ Bank transfers   │ CP       │ Money must be consistent   │
│ Shopping cart    │ AP       │ Better to show stale cart  │
│                  │          │   than error page          │
│ Social media     │ AP       │ Stale feed OK, must be up  │
│ Inventory count  │ CP       │ Overselling = real loss    │
│ DNS              │ AP       │ Eventual consistency fine  │
│ Leader election  │ CP       │ Must agree on one leader   │
└──────────────────┴──────────┴────────────────────────────┘
```

---

## Q4. Consistency Patterns — Weak, Eventual, and Strong.

```
When you have multiple copies of data, HOW do you keep them in sync?

┌─────────────────────────────────────────────────────────────┐
│              Consistency Spectrum                            │
│                                                             │
│  Weak          Eventual           Strong                    │
│  ←─────────────────────────────────────→                    │
│  Fastest         Balanced          Slowest                  │
│  Least safe      Good enough       Safest                   │
└─────────────────────────────────────────────────────────────┘
```

### Weak Consistency
```
After a write, reads MAY or MAY NOT see it. Best effort.

Use cases:
  - VoIP / video calls: if you lose connection, you don't replay missed audio
  - Real-time multiplayer games: stale position data is discarded
  - Memcached: cache may be inconsistent

Approach: "Fire and forget" — no guarantee reads reflect writes
```

### Eventual Consistency
```
After a write, reads will EVENTUALLY see it (typically milliseconds).
Data is replicated ASYNCHRONOUSLY.

Use cases:
  - DNS: domain changes propagate over hours
  - Email: delivery is not instant
  - Social media likes: count may be slightly off temporarily
  - DynamoDB, Cassandra: AP systems

How it works:
  1. Write goes to one node
  2. Node asynchronously replicates to others
  3. After propagation delay, all nodes agree

Tunable Consistency (Cassandra example):
  - Write to W nodes, Read from R nodes
  - If W + R > N (total nodes), you get strong-ish consistency
  - Example: N=3, W=2, R=2 → at least 1 overlap → consistent reads
```

### Strong Consistency
```
After a write, reads WILL see it. Data is replicated SYNCHRONOUSLY.

Use cases:
  - File systems (POSIX guarantees)
  - Relational databases (ACID transactions)
  - Bank account balances
  - Inventory counts

How it works:
  1. Write goes to master
  2. Master replicates to ALL replicas SYNCHRONOUSLY
  3. Write acknowledged only AFTER all replicas confirm
  4. Any read on any replica returns the latest value

Cost: Higher latency, lower throughput (waits for all nodes)
Tech: PostgreSQL with synchronous replication, Google Spanner
```

---

## Q5. Availability Patterns — How do you keep systems up?

### Fail-over Patterns
```
Active-Passive (Master-Slave Failover):
  ┌──────────┐    heartbeat    ┌──────────┐
  │  Active  │ ──────────────→ │ Passive  │
  │ (Master) │                 │ (Slave)  │
  └──────────┘                 └──────────┘
       │                            │
   Handles all              Standby (idle)
   traffic                  Takes over if
                            heartbeat stops

  - Passive server takes over Active's IP on failure
  - Hot standby: already running, fast failover
  - Cold standby: needs to start up, slower failover
  - Downside: passive server is idle (waste of resources)

Active-Active (Master-Master Failover):
  ┌──────────┐                 ┌──────────┐
  │ Active 1 │ ←─ sync ──────→│ Active 2 │
  │          │                 │          │
  └──────────┘                 └──────────┘
       │                            │
   Handles                    Handles
   traffic                    traffic

  - Both servers handle traffic (load spread)
  - DNS must know both IPs (public-facing)
  - If one fails, other handles all traffic
  - Better resource utilization
  - More complex: need conflict resolution for writes

Disadvantages of Failover:
  ✗ More hardware + complexity
  ✗ Potential data loss if active fails before replication
  ✗ Split-brain risk: both think they're active
```

### Availability in Numbers
```
Availability is measured in "nines":

┌────────────┬──────────────────┬──────────────┬─────────────┐
│ Level      │ Uptime %         │ Downtime/yr  │ Downtime/mo │
├────────────┼──────────────────┼──────────────┼─────────────┤
│ Two 9s     │ 99%              │ 3.65 days    │ 7.31 hours  │
│ Three 9s   │ 99.9%            │ 8h 45m 57s   │ 43m 49.7s   │
│ Four 9s    │ 99.99%           │ 52m 35.7s    │ 4m 23s      │
│ Five 9s    │ 99.999%          │ 5m 15.4s     │ 26.3s       │
└────────────┴──────────────────┴──────────────┴─────────────┘

Availability of Components in SEQUENCE (both must work):
  A(total) = A(Foo) × A(Bar)
  99.9% × 99.9% = 99.8%  ← Worse than individual

Availability of Components in PARALLEL (either can work):
  A(total) = 1 − (1 − A(Foo)) × (1 − A(Bar))
  1 − (0.001 × 0.001) = 99.9999%  ← Much better!

Key Insight: Redundancy (parallel components) dramatically improves availability.
This is why we use multiple load balancers, replicated databases, etc.
```

---

## Q6. Domain Name System (DNS) — How does it work at scale?

```
DNS translates domain names → IP addresses.

Resolution Hierarchy:
  Browser Cache → OS Cache → Router Cache → ISP DNS
    → Root DNS → TLD DNS (.com) → Authoritative DNS

Record Types:
  ┌────────┬───────────────────────────────────────────────┐
  │ Type   │ Purpose                                       │
  ├────────┼───────────────────────────────────────────────┤
  │ A      │ Maps name → IPv4 address                      │
  │ AAAA   │ Maps name → IPv6 address                      │
  │ CNAME  │ Maps name → another name (alias)              │
  │ NS     │ Specifies DNS servers for domain              │
  │ MX     │ Specifies mail servers                        │
  │ TXT    │ Arbitrary text (SPF, DKIM verification)       │
  └────────┴───────────────────────────────────────────────┘

Traffic Routing Methods (Route 53, CloudFlare):
  - Weighted Round Robin: distribute by weight (A/B testing)
  - Latency-based: route to lowest latency region
  - Geolocation-based: route by user's country/region
  - Failover: primary/secondary with health checks

TTL (Time to Live):
  - How long clients cache DNS results
  - Lower TTL = faster failover, more DNS queries
  - Higher TTL = fewer queries, slower updates
  - Typical: 300s (5 min) for dynamic, 86400s (1 day) for static

Disadvantages:
  ✗ Slight latency (mitigated by caching)
  ✗ Complex management (usually delegated to cloud providers)
  ✗ DDoS target (2016 Dyn attack took down Twitter, Reddit)
```

---

## Q7. Content Delivery Network (CDN) — Push vs Pull.

```
CDN = Globally distributed network of proxy servers
Serves content from locations CLOSER to users

Benefits:
  - Reduced latency (users hit nearby edge servers)
  - Reduced origin server load
  - DDoS protection (absorb traffic at edge)
  - SSL termination at edge

┌────────────────────────────────────────────────────┐
│                      CDN                           │
│   ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐       │
│   │ Edge│    │ Edge│    │ Edge│    │ Edge│        │
│   │ US  │    │ EU  │    │Asia │    │ AU  │        │
│   └──┬──┘    └──┬──┘    └──┬──┘    └──┬──┘        │
│      └──────────┼─────────┼──────────┘            │
│                 │ Origin  │                        │
│              ┌──┴─────────┴──┐                     │
│              │  Your Server  │                     │
│              └───────────────┘                     │
└────────────────────────────────────────────────────┘
```

### Push CDN
```
- YOU upload content to CDN whenever it changes
- Full control over what's cached and when
- Rewrite URLs to point to CDN

Best for:
  ✓ Low-traffic sites
  ✓ Content that changes infrequently
  ✓ When you want full control
  ✗ More work to manage uploads

Example: Upload static assets to CloudFront on each deploy
```

### Pull CDN
```
- CDN fetches content from your server on FIRST request
- Lazy loading — only caches what's requested
- TTL determines how long content is cached

Best for:
  ✓ High-traffic sites
  ✓ Content changes frequently
  ✓ Less management overhead
  ✗ First request is slower (cache miss)
  ✗ Redundant traffic if TTL expires before actual change

Example: CloudFlare sits in front, caches on first request
```

### CDN Disadvantages
```
✗ Cost: significant for high traffic (but usually worth it)
✗ Stale content: if updated before TTL expires
✗ URL changes needed: static content must point to CDN
✗ Cache invalidation: purging CDN cache is not instant
```

---

## Q8. Load Balancers — Types, Algorithms, and Architecture.

```
Load Balancers distribute requests across servers.

Benefits:
  ✓ Prevent requests to unhealthy servers (health checks)
  ✓ Prevent overloading any single resource
  ✓ Eliminate single points of failure
  ✓ SSL termination (offload encryption)
  ✓ Session persistence (sticky sessions via cookies)
```

### Layer 4 vs Layer 7 Load Balancing
```
Layer 4 (Transport Layer):
  - Looks at: source/dest IP, ports
  - Does NOT inspect packet content
  - Performs NAT (Network Address Translation)
  - Faster, less CPU
  - Use when: simple routing, TCP/UDP traffic
  - Example: AWS NLB

Layer 7 (Application Layer):
  - Looks at: HTTP headers, cookies, URL path, message body
  - Can make smart routing decisions
  - Terminates connection, creates new one to backend
  - Slower, more CPU, but very flexible
  - Use when: content-based routing, A/B testing
  - Example: AWS ALB, NGINX, HAProxy

Example L7 routing:
  /api/*        → API servers
  /static/*     → Static file servers
  /video/*      → Video streaming servers
  /payments/*   → Security-hardened servers
```

### Load Balancing Algorithms
```
┌────────────────────────┬───────────────────────────────────────┐
│ Algorithm              │ Description                           │
├────────────────────────┼───────────────────────────────────────┤
│ Round Robin            │ Each server gets a turn sequentially  │
│ Weighted Round Robin   │ Servers get turns proportional to     │
│                        │   weight (powerful servers get more)  │
│ Least Connections      │ Route to server with fewest active    │
│                        │   connections                         │
│ Least Response Time    │ Route to fastest responding server    │
│ IP Hash                │ Hash client IP → consistent server   │
│                        │   (session affinity without cookies)  │
│ Random                 │ Pick a server at random               │
│ Resource-Based         │ Route based on server resource        │
│                        │   utilization (CPU, memory)           │
└────────────────────────┴───────────────────────────────────────┘

High Availability for Load Balancers:
  - Deploy MULTIPLE load balancers
  - Active-passive or active-active mode
  - Single LB = single point of failure!
```

---

## Q9. Reverse Proxy — What is it and when to use one?

```
Reverse Proxy = web server that sits in FRONT of your backend servers

  Client → Reverse Proxy → Backend Server(s)

Clients only see the proxy's IP, never the backend servers.

Benefits:
  ✓ Security: hide backend server info, blacklist IPs, rate limit
  ✓ Scalability: add/remove backend servers transparently
  ✓ SSL Termination: handle HTTPS at proxy, HTTP internally
  ✓ Compression: gzip responses
  ✓ Caching: serve cached responses directly
  ✓ Static content: serve HTML/CSS/JS/images directly

Load Balancer vs Reverse Proxy:
  ┌───────────────────┬──────────────────────────────────┐
  │ Load Balancer     │ Useful with MULTIPLE servers      │
  │                   │ Routes to servers with same role   │
  ├───────────────────┼──────────────────────────────────┤
  │ Reverse Proxy     │ Useful even with ONE server        │
  │                   │ Adds security, caching, SSL, etc.  │
  └───────────────────┴──────────────────────────────────┘

  NGINX and HAProxy can do BOTH.

Disadvantages:
  ✗ Increased complexity
  ✗ Single reverse proxy = single point of failure
  ✗ Need to configure multiple proxies for HA
```

---

## Q10. Application Layer — Microservices & Service Discovery.

```
Separate web layer from application layer for independent scaling.

  ┌──────────┐     ┌──────────────┐     ┌───────────────┐
  │  Web     │────→│ Application  │────→│   Database    │
  │  Layer   │     │ Layer        │     │   Layer       │
  │ (NGINX)  │     │ (Services)   │     │ (MySQL, Redis)│
  └──────────┘     └──────────────┘     └───────────────┘

Benefits:
  - Scale web and application layers independently
  - Add new APIs without adding web servers
  - Workers in app layer enable async processing
```

### Microservices Architecture
```
Each service:
  ✓ Independently deployable
  ✓ Small, modular
  ✓ Runs unique process
  ✓ Communicates via well-defined API (REST, gRPC)

Example (Pinterest):
  - User Profile Service
  - Follower Service
  - Feed Service
  - Search Service
  - Photo Upload Service

Advantages:
  + Independent deployment & scaling
  + Team autonomy (each team owns a service)
  + Technology flexibility per service
  + Fault isolation

Disadvantages:
  - Network latency between services
  - Distributed transactions are hard
  - Operational complexity (monitoring, debugging)
  - Data consistency challenges
```

### Service Discovery
```
How do services FIND each other in a dynamic environment?

Tools: Consul, Etcd, Zookeeper, Eureka (Spring Cloud)

How it works:
  1. Service registers itself: name + address + port
  2. Other services query registry to find it
  3. Health checks verify service is alive
  4. Registry removes unhealthy services

  ┌─────────┐  register  ┌──────────────┐
  │Service A│───────────→│  Service     │
  └─────────┘            │  Registry    │
  ┌─────────┐  discover  │ (Consul/Etcd)│
  │Service B│←───────────│              │
  └─────────┘            └──────────────┘
```

---

## Q11. Security Fundamentals for System Design.

```
Key principles to mention in every design:

1. Encrypt in transit AND at rest
   - TLS for all communication
   - AES-256 for data at rest
   - Encrypt sensitive fields in database

2. Sanitize ALL user inputs
   - Prevent XSS (Cross-Site Scripting)
   - Prevent SQL injection
   - Use parameterized queries

3. Principle of Least Privilege
   - Services only get permissions they NEED
   - DB users have minimal required grants
   - IAM roles scoped to specific resources

4. Authentication & Authorization
   - OAuth 2.0 / JWT for API auth
   - RBAC (Role-Based Access Control)
   - Rate limiting per user/IP

5. Defense in Depth
   - WAF (Web Application Firewall)
   - Network segmentation (VPC, subnets)
   - API Gateway with rate limiting
   - Input validation at every layer

Interview Tip: Always mention security in your design wrap-up:
  "For security, I'd add TLS everywhere, WAF at the edge,
   rate limiting in API Gateway, and encrypt PII at rest."
```

---

## Q12. Latency Numbers Every Programmer Should Know.

```
Latency Comparison Numbers (approximate):
─────────────────────────────────────────────────────────────
L1 cache reference                           0.5 ns
Branch mispredict                            5   ns
L2 cache reference                           7   ns      14x L1
Mutex lock/unlock                           25   ns
Main memory reference                      100   ns      20x L2
Compress 1K bytes with Snappy           10,000   ns      10 μs
Send 1 KB over 1 Gbps network          10,000   ns      10 μs
Read 4 KB randomly from SSD           150,000   ns     150 μs
Read 1 MB sequentially from memory    250,000   ns     250 μs
Round trip within same datacenter     500,000   ns     500 μs
Read 1 MB sequentially from SSD    1,000,000   ns       1 ms
HDD seek                          10,000,000   ns      10 ms
Read 1 MB sequentially from HDD   30,000,000   ns      30 ms
Send packet CA → Netherlands → CA 150,000,000   ns     150 ms
─────────────────────────────────────────────────────────────

Key Takeaways for Design:
  - Memory is 100x faster than SSD, 1000x faster than HDD
  - SSD is 30x faster than HDD for sequential reads
  - Network within datacenter: 500μs round trip
  - Cross-continent: 150ms round trip
  - Cache everything you can in memory (Redis, Memcached)
  - Avoid cross-region calls in hot paths
  - Use SSD over HDD for databases
```

### Powers of Two Reference
```
Power   Value              Approx          Bytes
─────────────────────────────────────────────────
10      1,024              1 Thousand      1 KB
20      1,048,576          1 Million       1 MB
30      1,073,741,824      1 Billion       1 GB
32      4,294,967,296      4 Billion       4 GB
40      1,099,511,627,776  1 Trillion      1 TB
50      ~1 Quadrillion                     1 PB
```

---

## Q13. How to approach any System Design question — Framework.

```
4-Step Framework (30-45 minutes):

STEP 1: REQUIREMENTS & SCOPE (5 min)
  Ask clarifying questions:
  - Who uses it? How many users?
  - What are the key features? (scope down!)
  - Read/write ratio?
  - Expected QPS? Data volume?
  - Latency requirements?
  - Consistency vs availability preference?

STEP 2: HIGH-LEVEL DESIGN (10 min)
  Draw the big picture:
  - Clients → LB → API Servers → Services → DB/Cache
  - Identify 3-4 core components
  - Sketch data flow for key use cases

STEP 3: DEEP DIVE (15 min)
  Interviewer picks 2-3 areas to dive into:
  - Database schema design
  - API design
  - Specific algorithm (news feed ranking, URL hash)
  - Scaling a bottleneck

STEP 4: WRAP UP & BOTTLENECKS (5 min)
  - Identify bottlenecks
  - Discuss monitoring & alerting
  - Future improvements
  - Error handling & edge cases

Pro Tips:
  ✓ LEAD the conversation — don't wait for interviewer
  ✓ Think out loud — show your reasoning
  ✓ Use numbers — back-of-envelope estimates
  ✓ Discuss trade-offs — "We could use X but Y is better because..."
  ✓ Draw diagrams — visual communication is key
```

---

*Source: Concepts synthesized from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer) — 343K stars*
