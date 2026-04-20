# API Design Patterns — Interview Q&A

> 10 questions covering idempotency, pagination, versioning, rate limiting, webhooks  
> Priority: **P0** — Critical for FedEx/NPCI payment and logistics API discussions

---

### Q1. What is idempotency and why is it critical for payment APIs?

**Answer:**

**Idempotent operation:** Making the same request multiple times produces the same result as making it once.

```
Scenario: User clicks "Pay ₹5000" but network timeout occurs.
Client retries the request.

WITHOUT idempotency: ₹10,000 charged (double payment!)
WITH idempotency: ₹5,000 charged (duplicate detected, original result returned)
```

**Implementation with idempotency key:**
```java
@PostMapping("/payments")
public ResponseEntity<PaymentResponse> createPayment(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody PaymentRequest request) {

    // 1. Check if we've seen this key before:
    Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(toResponse(existing.get()));  // return cached result
    }

    // 2. Process payment:
    Payment payment = paymentService.process(request);
    payment.setIdempotencyKey(idempotencyKey);
    paymentRepository.save(payment);

    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(payment));
}
```

```sql
-- DB constraint ensures uniqueness:
ALTER TABLE payments ADD CONSTRAINT uk_idempotency UNIQUE (idempotency_key);
```

**Which HTTP methods are naturally idempotent?**
| Method | Idempotent? | Safe? | Explanation |
|--------|:-----------:|:-----:|-------------|
| GET | ✅ | ✅ | Read-only, no side effects |
| PUT | ✅ | ❌ | Replace entire resource — same result each time |
| DELETE | ✅ | ❌ | Deleting twice = same result (resource gone) |
| PATCH | ❌ | ❌ | Depends on implementation |
| POST | ❌ | ❌ | Creates new resource each time → needs idempotency key |

---

### Q2. Cursor-based vs Offset-based Pagination.

**Answer:**

**Offset-based (traditional):**
```
GET /api/orders?page=3&size=20
→ SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET 40;
```

**Cursor-based (recommended for large datasets):**
```
GET /api/orders?cursor=eyJpZCI6MTAwfQ==&size=20
→ SELECT * FROM orders WHERE id < 100 ORDER BY id DESC LIMIT 20;
```

| Aspect | Offset | Cursor |
|--------|--------|--------|
| Implementation | Simple `LIMIT/OFFSET` | Use last item's ID/timestamp |
| Performance | O(n) — DB scans & skips offset rows | O(1) — WHERE clause with index |
| Consistency | Items shift if data added/removed | Stable — always starts from known point |
| Jump to page | ✅ Easy (`page=50`) | ❌ Must iterate sequentially |
| Real-time feeds | ❌ Duplicates/missing items | ✅ Perfect for infinite scroll |

**Cursor implementation:**
```java
@GetMapping("/orders")
public CursorPage<OrderDto> getOrders(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size) {

    Long lastId = decodeCursor(cursor);  // Base64 decode
    List<Order> orders;

    if (lastId == null) {
        orders = orderRepo.findTopNByOrderByIdDesc(size + 1);
    } else {
        orders = orderRepo.findByIdLessThanOrderByIdDesc(lastId, PageRequest.of(0, size + 1));
    }

    boolean hasMore = orders.size() > size;
    if (hasMore) orders = orders.subList(0, size);

    String nextCursor = hasMore ? encodeCursor(orders.get(orders.size() - 1).getId()) : null;
    return new CursorPage<>(orders.stream().map(this::toDto).toList(), nextCursor, hasMore);
}

public record CursorPage<T>(List<T> data, String nextCursor, boolean hasMore) {}
```

**Response:**
```json
{
  "data": [...20 orders...],
  "nextCursor": "eyJpZCI6ODB9",
  "hasMore": true
}
```

---

### Q3. API Versioning Strategies — which to use and when?

**Answer:**

| Strategy | Example | Pros | Cons |
|----------|---------|------|------|
| **URI path** | `/api/v1/users` | Simple, visible, cacheable | URL pollution, violates REST (same resource, different URL) |
| **Query param** | `/api/users?version=1` | Simple, optional | Easy to forget, not standard |
| **Header** | `Accept: application/vnd.myapi.v1+json` | Clean URLs, proper REST | Hidden, harder to test in browser |
| **Content negotiation** | `Accept: application/json; version=1` | Standard HTTP | Complex |

**Most common in practice: URI path (`/api/v1/`)** — simple and explicit.

```java
// Spring Boot versioning with URI path:
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    @GetMapping("/{id}")
    public UserDtoV1 getUser(@PathVariable Long id) { ... }
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    @GetMapping("/{id}")
    public UserDtoV2 getUser(@PathVariable Long id) { ... }
}
```

**When to create a new version:**
- Breaking changes: removing fields, changing field types, renaming fields
- Non-breaking (DON'T version): adding optional fields, adding new endpoints

**Deprecation strategy:**
```
1. Release v2 alongside v1
2. Add Deprecation header to v1 responses: "Sunset: 2026-06-01"
3. Monitor v1 usage, notify consumers
4. After sunset date, return 410 Gone
```

---

### Q4. Implement Token Bucket Rate Limiting.

**Answer:**

**Concept:**
```
Bucket has capacity of N tokens. Tokens refill at rate R per second.
Each request consumes 1 token. If bucket empty → reject (429).

Example: capacity=10, refill=2/sec
  - Burst of 10 requests → all pass (bucket empties)
  - Next request in <0.5s → rejected
  - After 5 seconds → bucket full again (10 tokens refilled)
```

**Implementation:**
```java
public class TokenBucketRateLimiter {
    private final int capacity;
    private final double refillRate;  // tokens per second
    private double tokens;
    private long lastRefillTimestamp;

    public TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillTimestamp = System.nanoTime();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsed = (now - lastRefillTimestamp) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsed * refillRate);
        lastRefillTimestamp = now;
    }
}
```

**Spring Boot filter:**
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, TokenBucketRateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws Exception {
        String clientId = request.getRemoteAddr();  // or API key, user ID
        TokenBucketRateLimiter limiter = limiters.computeIfAbsent(
            clientId, k -> new TokenBucketRateLimiter(100, 10));  // 100 burst, 10/sec

        if (limiter.tryAcquire()) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "1");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
        }
    }
}
```

**Other algorithms:**
| Algorithm | Description | Best For |
|-----------|-------------|----------|
| Token Bucket | Tokens refill at steady rate, allows bursts | API rate limiting (most common) |
| Leaky Bucket | Requests queue, processed at fixed rate | Smoothing bursty traffic |
| Fixed Window | Count per time window (e.g., 100/min) | Simple, but boundary burst problem |
| Sliding Window | Rolling window counter | More accurate than fixed window |

**Distributed rate limiting (Redis):**
```lua
-- Redis Lua script for atomic token bucket:
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call('hmget', key, 'tokens', 'lastRefill')
-- ... refill logic ... atomic check and decrement
```

---

### Q5. How do you design a webhook system?

**Answer:**

```
Webhook = Server pushes events to client (vs. client polling the server)

Flow:
1. Client registers webhook URL: POST /webhooks { "url": "https://client.com/callback", "events": ["payment.success"] }
2. Event happens on server → Server POSTs payload to registered URL
3. Client receives, processes, returns 2xx
4. If no 2xx → Server retries with exponential backoff
```

**Server-side implementation:**
```java
@Entity
public class WebhookSubscription {
    @Id @GeneratedValue
    private Long id;
    private String callbackUrl;        // client's endpoint
    private String secret;             // for HMAC signature
    @ElementCollection
    private Set<String> events;        // ["payment.success", "order.shipped"]
    private boolean active;
}

@Service
public class WebhookDispatcher {

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void dispatch(WebhookSubscription sub, WebhookPayload payload) {
        String body = objectMapper.writeValueAsString(payload);
        String signature = computeHmacSha256(body, sub.getSecret());

        ResponseEntity<String> response = restTemplate.exchange(
            RequestEntity.post(URI.create(sub.getCallbackUrl()))
                .header("X-Webhook-Signature", "sha256=" + signature)
                .header("X-Webhook-Id", payload.getId())
                .header("Content-Type", "application/json")
                .body(body),
            String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new WebhookDeliveryException("Non-2xx: " + response.getStatusCode());
        }
    }
}
```

**Security considerations:**
1. **HMAC signature:** Client verifies payload wasn't tampered with
2. **Replay protection:** Include timestamp + event ID, client checks for duplicates
3. **URL validation:** Verify the callback URL isn't a private/internal IP (SSRF prevention)
4. **Timeout:** Set short timeout (5s) to avoid blocking on slow clients

---

### Q6. REST API Best Practices — Error Response Design.

**Answer:**

```json
// ✅ Consistent error format:
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      { "field": "email", "message": "must be a valid email address" },
      { "field": "amount", "message": "must be greater than 0" }
    ],
    "traceId": "abc-123-def",
    "timestamp": "2026-04-20T10:15:30Z"
  }
}

// ❌ Don't expose stack traces, internal paths, or DB error messages
```

**Standard HTTP status codes:**
| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation errors, malformed input |
| 401 | Unauthorized | Missing/invalid authentication |
| 403 | Forbidden | Authenticated but no permission |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource, version conflict |
| 422 | Unprocessable Entity | Semantic validation (business rules) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unhandled server error |
| 503 | Service Unavailable | Maintenance, overload |

---

### Q7. How do you design APIs for bulk operations?

**Answer:**

```json
// Option 1: Batch endpoint
POST /api/orders/batch
{
  "orders": [
    { "product_id": 1, "quantity": 2 },
    { "product_id": 2, "quantity": 1 }
  ]
}

// Response with per-item status:
{
  "results": [
    { "index": 0, "status": "success", "orderId": 101 },
    { "index": 1, "status": "error", "error": "Product out of stock" }
  ],
  "summary": { "total": 2, "success": 1, "failed": 1 }
}
```

```json
// Option 2: Async processing (large batches)
POST /api/imports
{ "file_url": "s3://bucket/orders.csv" }

Response:
201 Created
{ "jobId": "job-abc-123", "status": "PROCESSING" }

// Poll for result:
GET /api/imports/job-abc-123
{ "jobId": "job-abc-123", "status": "COMPLETED", "processed": 5000, "failed": 3 }
```

---

### Q8. HATEOAS — do you need it? When does it help?

**Answer:**

**HATEOAS** = Hypermedia As The Engine Of Application State

```json
// Response WITH HATEOAS links:
{
  "orderId": 101,
  "status": "SHIPPED",
  "total": 5000,
  "_links": {
    "self": { "href": "/api/orders/101" },
    "cancel": { "href": "/api/orders/101/cancel", "method": "POST" },
    "track": { "href": "/api/shipments/SHP-456" },
    "invoice": { "href": "/api/orders/101/invoice" }
  }
}
```

| Aspect | HATEOAS | No HATEOAS |
|--------|---------|------------|
| Client coupling | Low — follows links | High — hardcodes URLs |
| Discoverability | Self-documenting | Needs external docs |
| Complexity | More server-side work | Simpler |
| In practice | Government/enterprise APIs, Spring Data REST | Most startup APIs, SPAs with fixed routes |

**Real answer for interviews:** "We use basic REST without HATEOAS for our internal APIs. HATEOAS is valuable for public APIs consumed by many clients, where URL changes shouldn't break consumers."

---

### Q9. How do you handle long-running operations in REST APIs?

**Answer:**

```
Don't make clients wait for a 30-second operation. Use async pattern:

POST /api/reports/generate
Response: 202 Accepted
{
  "jobId": "rpt-abc-123",
  "status": "PROCESSING",
  "statusUrl": "/api/reports/jobs/rpt-abc-123"
}

GET /api/reports/jobs/rpt-abc-123
{
  "jobId": "rpt-abc-123",
  "status": "COMPLETED",       // PROCESSING → COMPLETED | FAILED
  "progress": 100,
  "resultUrl": "/api/reports/rpt-abc-123/download",
  "completedAt": "2026-04-20T10:20:00Z"
}
```

**Implementation:**
```java
@PostMapping("/reports/generate")
public ResponseEntity<JobResponse> generateReport(@RequestBody ReportRequest request) {
    String jobId = UUID.randomUUID().toString();
    reportService.generateAsync(jobId, request);  // @Async method

    return ResponseEntity.accepted()
        .header("Location", "/api/reports/jobs/" + jobId)
        .body(new JobResponse(jobId, "PROCESSING"));
}
```

---

### Q10. API Security Best Practices Checklist.

**Answer:**

| Category | Practice |
|----------|----------|
| **Authentication** | JWT / OAuth 2.0, never API keys in URL params |
| **Authorization** | Check permissions per resource, not just role |
| **Input validation** | Validate ALL inputs, use allowlists not blocklists |
| **Rate limiting** | Per-user and per-IP, return 429 with Retry-After |
| **HTTPS** | Always TLS, HSTS header, no HTTP fallback |
| **CORS** | Whitelist specific origins, not `*` in production |
| **Headers** | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` |
| **Secrets** | Never in URLs/logs, use request body or headers |
| **Pagination** | Enforce max page size (prevent data dump) |
| **Error responses** | Never expose stack traces, internal IPs, DB errors |
| **Idempotency** | For POST operations (especially payments) |
| **SQL Injection** | Parameterized queries, never string concatenation |
| **IDOR** | Verify user owns the resource they're accessing |
| **Audit logging** | Log who did what, when, with correlation ID |
