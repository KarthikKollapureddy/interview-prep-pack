# Spring WebFlux & Reactive Programming — Interview Q&A

> 12 questions covering Reactive Streams, Mono/Flux, WebFlux vs MVC, backpressure, WebClient  
> Priority: **P1** — Asked in senior Java interviews: "When would you use reactive?"

---

### Q1. What is Reactive Programming? Why does it exist?

**Answer:**

**Problem:** Traditional thread-per-request model (Spring MVC + Tomcat) wastes threads waiting for I/O:
```
Thread-1: [receive request]──[call DB]──────WAITING──────[got result]──[respond]
Thread-2: [receive request]──[call API]─────WAITING──────[got result]──[respond]
→ 200 threads max (Tomcat default) = 200 concurrent requests max
→ Thread sitting idle during I/O = wasted resource
```

**Reactive solution:** Non-blocking I/O. One thread handles many requests:
```
Thread-1: [req-A start]──[req-A DB call, don't wait]──[req-B start]──[req-B API call]──[req-A DB ready, resume]──[req-A respond]──[req-B API ready]──[req-B respond]
→ Same thread handles multiple requests by not blocking on I/O
→ Far fewer threads needed for same throughput
```

**Reactive Streams specification (java.util.concurrent.Flow in Java 9+):**
```java
Publisher<T>    // produces data
Subscriber<T>  // consumes data
Subscription   // controls flow (backpressure)
Processor<T,R> // both publisher and subscriber (transform)
```

**Key principle:** Data flows as a stream. Consumer controls the pace (backpressure).

---

### Q2. Spring MVC vs Spring WebFlux — when to use which?

**Answer:**

| Aspect | Spring MVC | Spring WebFlux |
|--------|-----------|----------------|
| **Programming model** | Imperative (blocking) | Reactive (non-blocking) |
| **Server** | Tomcat (Servlet-based) | Netty (event-loop, non-blocking) |
| **Thread model** | Thread-per-request (200 default) | Event loop (few threads, many requests) |
| **Return types** | `Object`, `ResponseEntity<T>` | `Mono<T>`, `Flux<T>` |
| **Blocking I/O** | ✅ Fine (JDBC, JPA, RestTemplate) | ❌ Must NOT block (kills event loop) |
| **DB support** | JPA/Hibernate (blocking) | R2DBC, reactive MongoDB, reactive Redis |
| **Learning curve** | Low | High (reactive thinking, debugging harder) |
| **Debugging** | Easy (stack traces) | Hard (async stack traces, scattered flow) |
| **Best for** | CRUD apps, most business apps | High-concurrency I/O-bound (proxy, gateway, streaming) |

**Decision framework:**
```
Use Spring MVC when:
  ✅ Team knows imperative programming
  ✅ Using JPA/Hibernate (blocking JDBC)
  ✅ CRUD applications (most business apps)
  ✅ Request-response pattern dominant
  ✅ 90% of Spring Boot applications

Use Spring WebFlux when:
  ✅ Very high concurrency (10K+ concurrent connections)
  ✅ Streaming data (SSE, WebSocket, real-time feeds)
  ✅ API Gateway / Proxy (Spring Cloud Gateway uses WebFlux)
  ✅ All I/O is non-blocking (R2DBC, reactive Mongo, WebClient)
  ✅ Microservice doing mostly I/O aggregation (call 5 APIs, combine)

NEVER use WebFlux when:
  ❌ Team is unfamiliar with reactive (productivity drops 50%)
  ❌ Using blocking libraries (JPA, JDBC, RestTemplate)
  ❌ CPU-bound processing (reactive adds overhead, no benefit)
```

**Interview answer:** "At Wipro/FedEx we use Spring MVC because our stack includes JPA/Hibernate (blocking). WebFlux makes sense for our API Gateway (Spring Cloud Gateway runs on WebFlux internally) and for services that aggregate multiple downstream API calls."

---

### Q3. Explain Mono and Flux with examples.

**Answer:**

```java
// Mono<T> — 0 or 1 element (like Optional but async):
Mono<User> userMono = userRepository.findById(1L);     // reactive repo
Mono<String> hello = Mono.just("Hello");                // immediate value
Mono<Void> empty = Mono.empty();                        // no value
Mono<String> error = Mono.error(new RuntimeException()); // error signal

// Flux<T> — 0 to N elements (like Stream but async):
Flux<User> allUsers = userRepository.findAll();
Flux<Integer> numbers = Flux.range(1, 10);              // 1, 2, ..., 10
Flux<String> stream = Flux.interval(Duration.ofSeconds(1))
                          .map(i -> "tick-" + i);       // infinite stream
```

**CRITICAL: Nothing happens until you subscribe!**
```java
// ❌ This does NOTHING (no subscriber):
userRepository.findById(1L);

// ✅ In a controller, Spring subscribes for you:
@GetMapping("/users/{id}")
public Mono<User> getUser(@PathVariable Long id) {
    return userRepository.findById(id);  // Spring subscribes internally
}

// ✅ Manual subscription (for testing):
userMono.subscribe(
    user -> System.out.println("Got: " + user),   // onNext
    error -> System.err.println("Error: " + error), // onError
    () -> System.out.println("Done")                // onComplete
);
```

---

### Q4. Show common Mono/Flux operators.

**Answer:**

```java
// Transform:
Mono<String> name = userMono.map(user -> user.getName());
Flux<Order> orders = userMono.flatMapMany(user -> orderRepo.findByUserId(user.getId()));

// Chain (flatMap = async map):
Mono<OrderResponse> response = userRepo.findById(userId)          // Mono<User>
    .flatMap(user -> orderService.createOrder(user, request))     // Mono<Order>
    .flatMap(order -> paymentService.charge(order))               // Mono<Payment>
    .map(payment -> new OrderResponse(payment));                  // Mono<OrderResponse>

// Error handling:
Mono<User> user = userRepo.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException("User not found")))
    .onErrorResume(e -> Mono.just(User.defaultUser()))    // fallback
    .doOnError(e -> log.error("Failed: {}", e.getMessage()));

// Combine multiple calls (parallel):
Mono<UserProfile> profile = Mono.zip(
    userService.getUser(id),          // parallel call 1
    orderService.getOrders(id),       // parallel call 2
    reviewService.getReviews(id)      // parallel call 3
).map(tuple -> new UserProfile(tuple.getT1(), tuple.getT2(), tuple.getT3()));

// Filter:
Flux<Product> expensive = productFlux.filter(p -> p.getPrice() > 1000);

// Collect to list (convert Flux → Mono<List>):
Mono<List<User>> userList = userFlux.collectList();

// Timeout:
Mono<Response> resp = webClient.get().retrieve()
    .bodyToMono(Response.class)
    .timeout(Duration.ofSeconds(5))
    .onErrorResume(TimeoutException.class, e -> Mono.just(Response.fallback()));
```

---

### Q5. What is backpressure and how does it work?

**Answer:**

**Problem:** Publisher produces data faster than subscriber can consume:
```
Publisher: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10  →  (100 items/sec)
Subscriber: processes 1... 2... 3...          →  (3 items/sec)
→ Without backpressure: OutOfMemoryError (buffer overflow)
```

**Backpressure = subscriber tells publisher how much it can handle:**
```java
// Subscriber requests N items at a time:
flux.subscribe(new BaseSubscriber<Integer>() {
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(3);  // "I can handle 3 items"
    }

    @Override
    protected void hookOnNext(Integer value) {
        process(value);
        request(1);  // "OK, send me 1 more"
    }
});
```

**Backpressure strategies in Project Reactor:**
```java
flux.onBackpressureBuffer(100)   // buffer up to 100 items, error if full
flux.onBackpressureDrop()        // drop items subscriber can't handle
flux.onBackpressureLatest()      // keep only the latest item
flux.onBackpressureError()       // error immediately if overwhelmed
```

**Real-world:** HTTP streaming, WebSocket feeds, Kafka consumer processing.

---

### Q6. Write a reactive REST controller with WebFlux.

**Answer:**

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;  // ReactiveCrudRepository

    @GetMapping
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUser(@PathVariable String id) {
        return userRepository.findById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user) {
        return userRepository.save(user);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteUser(@PathVariable String id) {
        return userRepository.deleteById(id);
    }

    // Server-Sent Events (streaming):
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> streamUsers() {
        return userRepository.findAll()
            .delayElements(Duration.ofSeconds(1));  // simulate real-time feed
    }
}
```

```java
// Reactive Repository (R2DBC or Reactive MongoDB):
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    Flux<User> findByRole(String role);
    Mono<User> findByEmail(String email);
}
```

---

### Q7. Explain WebClient — the reactive HTTP client.

**Answer:**

```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
            .baseUrl("https://api.payment.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(ExchangeFilterFunctions.basicAuthentication("user", "pass"))
            .build();
    }
}

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final WebClient webClient;

    // GET request:
    public Mono<PaymentStatus> getStatus(String paymentId) {
        return webClient.get()
            .uri("/payments/{id}/status", paymentId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                resp -> Mono.error(new PaymentNotFoundException(paymentId)))
            .onStatus(HttpStatusCode::is5xxServerError,
                resp -> Mono.error(new PaymentGatewayException("Gateway error")))
            .bodyToMono(PaymentStatus.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }

    // POST request:
    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return webClient.post()
            .uri("/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class);
    }

    // Parallel calls with Mono.zip:
    public Mono<OrderDetails> getOrderDetails(String orderId) {
        Mono<Order> order = getOrder(orderId);
        Mono<PaymentStatus> payment = getPaymentByOrder(orderId);
        Mono<ShippingStatus> shipping = getShippingByOrder(orderId);

        return Mono.zip(order, payment, shipping)
            .map(tuple -> new OrderDetails(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }
}
```

**WebClient is the ONLY non-deprecated HTTP client in Spring. See Q8 for full comparison.**

---

### Q8. Compare WebClient vs RestTemplate vs RestClient vs Feign.

**Answer:**

| Feature | RestTemplate | WebClient | RestClient (Spring 6.1+) | OpenFeign |
|---------|:---:|:---:|:---:|:---:|
| **Blocking** | ✅ Blocking | ✅ Non-blocking + blocking | ✅ Blocking | ✅ Blocking |
| **Reactive support** | ❌ | ✅ Native (Mono/Flux) | ❌ | ❌ |
| **Status** | ⚠️ Maintenance mode | ✅ Recommended | ✅ New (Spring 6.1) | ✅ Popular |
| **Fluent API** | ❌ | ✅ | ✅ | Interface-based |
| **Spring Boot** | 2.x legacy | 2.x+ (WebFlux) | 3.2+ | Spring Cloud |
| **Error handling** | Try-catch | `.onStatus()` declarative | `.onStatus()` declarative | ErrorDecoder |
| **Use case** | Legacy apps | Reactive apps, gateway | New MVC apps (Spring 3.2+) | Declarative microservice calls |

**RestTemplate (legacy — maintenance mode since Spring 5):**
```java
// Simple but verbose, no fluent API:
RestTemplate restTemplate = new RestTemplate();
User user = restTemplate.getForObject("/api/users/{id}", User.class, 1);

ResponseEntity<User> response = restTemplate.exchange(
    "/api/users/{id}", HttpMethod.GET, null,
    new ParameterizedTypeReference<User>() {}, 1);

// POST:
User newUser = restTemplate.postForObject("/api/users", request, User.class);

// With headers:
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(token);
HttpEntity<UserRequest> entity = new HttpEntity<>(request, headers);
ResponseEntity<User> resp = restTemplate.exchange(
    "/api/users", HttpMethod.POST, entity, User.class);
```

**RestClient (Spring 6.1+ / Boot 3.2+ — the modern replacement for RestTemplate):**
```java
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    }
}

// Fluent, readable, blocking (perfect for Spring MVC apps):
User user = restClient.get()
    .uri("/users/{id}", userId)
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError,
        (request, response) -> { throw new UserNotFoundException(userId); })
    .body(User.class);

// POST:
User created = restClient.post()
    .uri("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(newUser)
    .retrieve()
    .body(User.class);

// Exchange for full control:
ResponseEntity<User> response = restClient.get()
    .uri("/users/{id}", userId)
    .retrieve()
    .toEntity(User.class);
```

**OpenFeign (declarative — Spring Cloud):**
```java
@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentClient {

    @GetMapping("/payments/{id}")
    PaymentResponse getPayment(@PathVariable String id);

    @PostMapping("/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
}

// Usage — just inject and call:
@Service
@RequiredArgsConstructor
public class OrderService {
    private final PaymentClient paymentClient;  // auto-implemented by Spring Cloud

    public void processOrder(Order order) {
        PaymentResponse payment = paymentClient.createPayment(
            new PaymentRequest(order.getTotal()));
    }
}
```

**Which to use (2024+ recommendation):**
```
New Spring MVC app (Boot 3.2+) → RestClient ✅
Reactive / WebFlux app         → WebClient ✅
Microservice-to-microservice   → OpenFeign ✅ (with Spring Cloud)
Legacy app (Boot 2.x)          → RestTemplate (don't migrate unless needed)
Need non-blocking in MVC       → WebClient (works in MVC too, just .block() to get result)
```

---

### Q9. What are the common mistakes with WebFlux?

**Answer:**

```java
// ❌ Mistake 1: Blocking call inside reactive chain (KILLS event loop):
Mono<User> user = Mono.fromCallable(() -> {
    return jdbcTemplate.queryForObject("SELECT ...", User.class);  // BLOCKS!
});
// ✅ Fix: Use R2DBC, or wrap blocking call on boundedElastic scheduler:
Mono<User> user = Mono.fromCallable(() -> jdbcTemplate.queryForObject(...))
    .subscribeOn(Schedulers.boundedElastic());  // runs on dedicated thread pool

// ❌ Mistake 2: Calling .block() inside a reactive chain:
@GetMapping("/user")
public Mono<User> getUser() {
    User user = userRepo.findById(1L).block();  // DEADLOCK in Netty!
    return Mono.just(user);
}
// ✅ Fix: Chain operations, never block:
@GetMapping("/user")
public Mono<User> getUser() {
    return userRepo.findById(1L);
}

// ❌ Mistake 3: Ignoring return value (nothing subscribes):
@PostMapping("/notify")
public Mono<Void> notify() {
    notificationService.sendEmail(email);  // returns Mono but not chained!
    return Mono.empty();
}
// ✅ Fix: Chain it:
@PostMapping("/notify")
public Mono<Void> notify() {
    return notificationService.sendEmail(email);
}

// ❌ Mistake 4: Using ThreadLocal (MDC, SecurityContext) — breaks in reactive:
// ThreadLocal doesn't propagate across reactive schedulers
// ✅ Fix: Use Reactor Context or Micrometer's context propagation
```

---

### Q10. How does Spring Cloud Gateway use WebFlux?

**Answer:**

Spring Cloud Gateway is built on **WebFlux + Netty** (not Servlet/Tomcat):
```
Why? An API Gateway is the PERFECT use case for reactive:
  - High concurrency (all traffic goes through it)
  - Mostly I/O (proxy requests to downstream services)
  - No heavy computation
  - Needs to handle thousands of concurrent connections

Gateway handles:
  Client → [Route matching] → [Filters: auth, rate limit, log] → [Proxy to downstream] → [Response filters] → Client
  
  All non-blocking — one event loop thread handles hundreds of connections
```

```java
// Custom reactive filter:
@Component
public class AuthFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return authService.validate(token)  // reactive auth check
            .flatMap(valid -> chain.filter(exchange));
    }
}
```

---

### Q11. R2DBC — Reactive database access.

**Answer:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
```

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
```

```java
// Reactive repository:
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Flux<User> findByRole(String role);
    Mono<User> findByEmail(String email);

    @Query("SELECT * FROM users WHERE created_at > :date")
    Flux<User> findRecentUsers(@Param("date") LocalDateTime date);
}
```

**R2DBC limitations vs JPA:**
| Feature | JPA/Hibernate | R2DBC |
|---------|:---:|:---:|
| Lazy loading | ✅ | ❌ |
| L1/L2 cache | ✅ | ❌ |
| Schema generation | ✅ | ❌ (use Flyway/Liquibase) |
| Relationships (@OneToMany) | ✅ | ❌ (manual joins) |
| Maturity | Very mature | Still evolving |
| Blocking | ✅ Yes | ❌ Non-blocking |

**When to use R2DBC:** Only when you're fully committed to WebFlux AND need reactive DB access. For most apps, JPA + Spring MVC is simpler and sufficient.

---

### Q12. Testing reactive code.

**Answer:**

```java
// StepVerifier — the standard way to test Mono/Flux:
@Test
void testGetUser() {
    Mono<User> userMono = userService.findById(1L);

    StepVerifier.create(userMono)
        .assertNext(user -> {
            assertEquals("Alice", user.getName());
            assertEquals("alice@example.com", user.getEmail());
        })
        .verifyComplete();
}

@Test
void testGetAllUsers() {
    Flux<User> usersFlux = userService.findAll();

    StepVerifier.create(usersFlux)
        .expectNextCount(3)
        .verifyComplete();
}

@Test
void testError() {
    Mono<User> userMono = userService.findById(999L);

    StepVerifier.create(userMono)
        .expectError(NotFoundException.class)
        .verify();
}

// WebTestClient (reactive equivalent of MockMvc):
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testGetUser() {
        webTestClient.get().uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(User.class)
            .value(user -> assertEquals("Alice", user.getName()));
    }
}
```

---

## Quick Reference

| Concept | Key Point |
|---------|-----------|
| **Mono** | 0 or 1 element (like async Optional) |
| **Flux** | 0 to N elements (like async Stream) |
| **Backpressure** | Consumer controls data flow rate |
| **Nothing happens until subscribe** | Mono/Flux are lazy — must subscribe |
| **Don't block in reactive** | Use `.subscribeOn(Schedulers.boundedElastic())` for blocking calls |
| **WebClient** | Only non-deprecated HTTP client in Spring |
| **RestClient** | Modern blocking replacement for RestTemplate (Spring 6.1+) |
| **R2DBC** | Reactive DB access (no lazy loading, no L2 cache) |
| **When to use WebFlux** | High concurrency I/O, streaming, API Gateway |
| **When NOT to** | CRUD apps, JPA/Hibernate, CPU-bound, small team |
