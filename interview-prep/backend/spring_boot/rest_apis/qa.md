# REST APIs with Spring Boot — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What are REST maturity levels (Richardson Maturity Model)?

| Level | Description | Example |
|-------|-------------|---------|
| Level 0 | Single URI, single verb (SOAP-like) | `POST /api` with action in body |
| Level 1 | Multiple URIs, single verb | `POST /orders`, `POST /orders/123/cancel` |
| Level 2 | Multiple URIs + HTTP verbs properly | `GET /orders`, `POST /orders`, `DELETE /orders/123` |
| Level 3 | HATEOAS — links in responses | Response includes `_links: { self, next, cancel }` |

**FedEx/Hatio services target Level 2 minimum.** HATEOAS is nice but rarely fully adopted.

---

### Q2. How do you design proper REST endpoints? Show naming conventions.

```
# Resource-oriented, nouns not verbs, plural
GET    /api/v1/shipments              — list all shipments
GET    /api/v1/shipments/{id}         — get one
POST   /api/v1/shipments              — create
PUT    /api/v1/shipments/{id}         — full update
PATCH  /api/v1/shipments/{id}         — partial update
DELETE /api/v1/shipments/{id}         — delete

# Nested resources
GET    /api/v1/shipments/{id}/tracking-events
POST   /api/v1/shipments/{id}/tracking-events

# Filtering, sorting, pagination via query params
GET    /api/v1/shipments?status=IN_TRANSIT&sort=createdAt,desc&page=0&size=20

# Actions that don't map to CRUD — use verbs as sub-resource
POST   /api/v1/shipments/{id}/cancel
POST   /api/v1/shipments/{id}/reroute
```

**Anti-patterns:**
- ❌ `GET /getShipment`, `POST /createShipment` (verbs in URL)
- ❌ `GET /shipments/delete/123` (using GET for mutations)
- ❌ `POST /shipments` for everything (RPC-style)

---

### Q3. How does Spring Boot handle request/response serialization?

```java
@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable Long id) {
        return service.findById(id); // Jackson auto-converts to JSON
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse create(@Valid @RequestBody CreateShipmentRequest req) {
        return service.create(req);
    }
}
```

**Flow:** HTTP Request → `DispatcherServlet` → `HandlerMapping` → `Controller` → `HttpMessageConverter` (Jackson) → HTTP Response

**Jackson customization:**
```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ShipmentResponse(
    Long id,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
    @JsonIgnore String internalField
) {}
```

---

### Q4. How do you implement global exception handling for REST APIs?

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException e) {
        return new ErrorResponse("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a + "; " + b
            ));
        return new ErrorResponse("VALIDATION_ERROR", "Invalid request", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception e) {
        log.error("Unexpected error", e);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        // Never expose stack traces or internal details to clients
    }
}

public record ErrorResponse(String code, String message, Object details) {
    public ErrorResponse(String code, String message) { this(code, message, null); }
}
```

---

## Scenario-Based Questions

### Q5. At FedEx, you're building a shipment tracking API. Design the complete controller layer with validation, pagination, and error handling.

```java
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService service;

    @GetMapping
    public Page<ShipmentSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ShipmentStatus status) {
        return service.findAll(status, PageRequest.of(page, size));
    }

    @GetMapping("/{trackingNumber}")
    public ShipmentDetail get(@PathVariable @Pattern(regexp = "^[0-9]{12,22}$") String trackingNumber) {
        return service.findByTracking(trackingNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment", trackingNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentDetail create(@Valid @RequestBody CreateShipmentRequest req) {
        return service.create(req);
    }

    @GetMapping("/{trackingNumber}/events")
    public List<TrackingEvent> getEvents(@PathVariable String trackingNumber) {
        return service.getTrackingEvents(trackingNumber);
    }
}

// Request DTO with validation
public record CreateShipmentRequest(
    @NotBlank String senderName,
    @Valid @NotNull Address origin,
    @Valid @NotNull Address destination,
    @NotNull @Positive Double weight,
    @NotNull ServiceType serviceType
) {}
```

---

### Q6. At NPCI, your UPI API needs rate limiting. How do you implement it in Spring Boot?

```java
// Using Bucket4j + Spring Boot
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return new RateLimitInterceptor(Bucket.builder().addLimit(limit).build());
    }
}

// Per-client rate limiting
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String clientId = req.getHeader("X-Client-Id");
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            return true;
        }
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", "60");
        return false;
    }
}
```

**Production considerations:**
- Use Redis-backed buckets for distributed rate limiting across pods
- Different limits per API key tier
- Return `429 Too Many Requests` with `Retry-After` header

---

### Q7. At Hatio, you need API versioning for your payment gateway. What strategies exist?

| Strategy | Example | Pros | Cons |
|----------|---------|------|------|
| URI path | `/api/v1/payments` | Simple, visible | URL pollution |
| Header | `X-API-Version: 2` | Clean URLs | Hidden, harder to test |
| Media type | `Accept: application/vnd.hatio.v2+json` | RESTful, per-resource | Complex |
| Query param | `/api/payments?version=2` | Easy to switch | Not RESTful |

**Recommendation for Hatio:** URI path versioning (`/api/v1/`, `/api/v2/`). It's the most explicit and testable.

```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentControllerV1 { /* original */ }

@RestController
@RequestMapping("/api/v2/payments")
public class PaymentControllerV2 { /* enhanced with new fields */ }
```

---

### Q8. How do you implement content negotiation (JSON + XML)?

```java
// Accept: application/json → Jackson
// Accept: application/xml → JAXB (add dependency)

// application.yml
spring:
  mvc:
    contentnegotiation:
      favor-parameter: true
      parameter-name: format
      media-types:
        json: application/json
        xml: application/xml
```

Add `jackson-dataformat-xml` dependency, and Spring will automatically negotiate based on the `Accept` header.

---

## Coding Challenges

### Challenge 1: Complete CRUD REST API
**File:** `solutions/CrudApiDesign.java`  
Design a complete REST API for an order management system:
1. Full CRUD with proper HTTP methods and status codes
2. Request validation with custom validators
3. Pagination and sorting
4. Global exception handler with standard error format
5. Response DTO mapping (entity ≠ response)

### Challenge 2: API Rate Limiter
**File:** `solutions/ApiRateLimiter.java`  
Implement a token-bucket rate limiter:
1. Per-client rate limiting using client ID header
2. Configurable rates (requests per second)
3. Thread-safe implementation
4. Return proper 429 response with Retry-After header
5. Test concurrent access from multiple clients

---

## Gotchas & Edge Cases

### Q9. What's the difference between `@RequestParam` and `@PathVariable`?

```java
// @PathVariable — part of the URL path
@GetMapping("/users/{id}")          // /users/123
public User get(@PathVariable Long id) { }

// @RequestParam — query parameter
@GetMapping("/users")               // /users?name=Karthik&page=0
public List<User> search(
    @RequestParam String name,
    @RequestParam(defaultValue = "0") int page
) { }
```

**Gotcha:** `@PathVariable` is required by default. If the path variable is missing, you get 404 (not 400). `@RequestParam` without `required=false` returns 400 if missing.

---

### Q10. How do you handle file uploads in Spring Boot REST?

```java
@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) throw new BadRequestException("File is empty");
    if (file.getSize() > 10_000_000) throw new BadRequestException("File too large");
    
    // Validate content type (security!)
    String contentType = file.getContentType();
    if (!List.of("application/pdf", "image/png").contains(contentType)) {
        throw new BadRequestException("Unsupported file type");
    }
    
    storage.store(file);
    return ResponseEntity.ok("Uploaded: " + file.getOriginalFilename());
}
```

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```
