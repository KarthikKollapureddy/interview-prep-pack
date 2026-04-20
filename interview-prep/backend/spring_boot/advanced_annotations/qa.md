# Spring Boot Advanced Annotations — Interview Q&A

> 10 questions covering @Async, @Scheduled, @EventListener, @Cacheable, @Retryable, @ConditionalOnProperty  
> Priority: **P1** — Asked in Spring Boot deep-dive rounds at product companies

---

### Q1. Explain @Async and @EnableAsync. How does async execution work in Spring?

**Answer:**

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async exception in {}: {}", method.getName(), ex.getMessage());
    }
}
```

```java
@Service
public class NotificationService {

    // Fire-and-forget (void return):
    @Async
    public void sendEmail(String to, String body) {
        // runs in separate thread, caller doesn't wait
        emailClient.send(to, body);
    }

    // With return value (CompletableFuture):
    @Async
    public CompletableFuture<EmailResult> sendEmailAsync(String to, String body) {
        EmailResult result = emailClient.send(to, body);
        return CompletableFuture.completedFuture(result);
    }
}
```

**⚠️ Common Pitfalls:**
```java
// ❌ DOESN'T WORK — self-invocation bypasses proxy:
@Service
public class OrderService {
    public void createOrder() {
        sendNotification();  // ← calls directly, NOT through proxy!
    }

    @Async
    public void sendNotification() { ... }
}

// ✅ FIX — inject self or use separate service:
@Service
public class OrderService {
    @Autowired private NotificationService notificationService;

    public void createOrder() {
        notificationService.sendEmail(email, body);  // ← through proxy ✅
    }
}
```

**How it works:** Spring creates a proxy around the bean. `@Async` methods are submitted to a `TaskExecutor`. Without `@EnableAsync`, the annotation is silently ignored.

---

### Q2. Explain @Scheduled and @EnableScheduling.

**Answer:**

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No additional config needed for basic usage
}

@Component
public class ScheduledTasks {

    // Fixed rate: runs every 5 seconds (may overlap if task takes > 5s)
    @Scheduled(fixedRate = 5000)
    public void checkPendingPayments() {
        log.info("Checking pending payments...");
    }

    // Fixed delay: 5 seconds AFTER previous execution finishes (no overlap)
    @Scheduled(fixedDelay = 5000)
    public void cleanupExpiredSessions() {
        log.info("Cleaning expired sessions...");
    }

    // Cron: every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        log.info("Generating daily report...");
    }

    // With initial delay (wait 10s after startup before first run):
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void healthCheck() { }

    // Externalized cron from properties:
    @Scheduled(cron = "${app.scheduler.report-cron}")
    public void configuredReport() { }
}
```

**Cron expression format:** `second minute hour day-of-month month day-of-week`
```
"0 0 2 * * ?"    → Every day at 2:00 AM
"0 */5 * * * ?"  → Every 5 minutes
"0 0 0 1 * ?"    → First day of every month at midnight
```

**⚠️ In a multi-instance deployment**, all instances run the scheduler. Use **ShedLock** to ensure only one instance executes:
```java
@SchedulerLock(name = "dailyReport", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
@Scheduled(cron = "0 0 2 * * ?")
public void generateDailyReport() { }
```

---

### Q3. Explain @EventListener and ApplicationEventPublisher.

**Answer:**

```java
// 1. Define event:
public class OrderCreatedEvent {
    private final Long orderId;
    private final String userEmail;
    private final BigDecimal amount;

    public OrderCreatedEvent(Long orderId, String userEmail, BigDecimal amount) {
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.amount = amount;
    }
    // getters...
}

// 2. Publish event:
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        eventPublisher.publishEvent(
            new OrderCreatedEvent(order.getId(), request.getEmail(), order.getTotal())
        );
        return order;
    }
}

// 3. Listen to event:
@Component
public class OrderEventHandler {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order {} created, sending email to {}", event.getOrderId(), event.getUserEmail());
        // send email, update analytics, etc.
    }

    // Async listener (non-blocking):
    @Async
    @EventListener
    public void handleOrderCreatedAsync(OrderCreatedEvent event) {
        // heavy processing in separate thread
    }

    // Conditional listener:
    @EventListener(condition = "#event.amount > 10000")
    public void handleHighValueOrder(OrderCreatedEvent event) {
        // alert for high-value orders
    }

    // Transactional event listener (after commit):
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterOrderCommitted(OrderCreatedEvent event) {
        // safe to send notification — order is definitely saved
    }
}
```

**Why use events?**
- **Decoupling:** OrderService doesn't need to know about email, analytics, audit
- **Open/Closed Principle:** Add new listeners without modifying OrderService
- `@TransactionalEventListener` ensures side effects only happen after successful commit

---

### Q4. Explain @Cacheable, @CacheEvict, @CachePut.

**Answer:**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("products", "users");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return manager;
    }
}

@Service
public class ProductService {

    // Cache the result. Key = productId. Subsequent calls return cached value.
    @Cacheable(value = "products", key = "#productId")
    public Product getProduct(Long productId) {
        log.info("DB call for product {}", productId);  // only logged on cache miss
        return productRepository.findById(productId).orElseThrow();
    }

    // Update cache entry (always executes method, puts result in cache):
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    // Remove from cache:
    @CacheEvict(value = "products", key = "#productId")
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    // Clear entire cache:
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() { }

    // Conditional caching:
    @Cacheable(value = "products", key = "#productId",
               condition = "#productId > 0",
               unless = "#result == null")
    public Product getProductConditional(Long productId) {
        return productRepository.findById(productId).orElse(null);
    }
}
```

**Cache providers:**
| Provider | Use Case |
|----------|----------|
| `ConcurrentHashMap` | Default, single instance only |
| `Caffeine` | In-process, high-performance (recommended local cache) |
| `Redis` | Distributed cache (multi-instance deployments) |
| `Hazelcast` | Distributed, embedded cache |

**Cache-aside pattern (most common):**
```
Read: Check cache → miss → query DB → put in cache → return
Write: Update DB → evict/update cache
```

---

### Q5. Explain @Retryable and @Recover (Spring Retry).

**Answer:**

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

```java
@Configuration
@EnableRetry
public class RetryConfig { }

@Service
public class PaymentService {

    @Retryable(
        retryFor = {PaymentGatewayException.class, TimeoutException.class},
        noRetryFor = {InvalidAmountException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s, 4s
    )
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Attempting payment, attempt #{}", RetrySynchronizationManager.getContext().getRetryCount() + 1);
        return paymentGateway.charge(request);  // may throw
    }

    // Called after ALL retries fail:
    @Recover
    public PaymentResponse recoverPayment(PaymentGatewayException ex, PaymentRequest request) {
        log.error("All retries failed for payment {}: {}", request.getOrderId(), ex.getMessage());
        return PaymentResponse.failed("Payment gateway unavailable. Please try again later.");
    }
}
```

**Backoff strategies:**
```
Fixed:       1s, 1s, 1s
Exponential: 1s, 2s, 4s  (multiplier=2)
Random:      1s ± jitter  (avoid thundering herd)
```

**When to retry:**
- ✅ Transient failures: network timeout, 503, connection reset, deadlock
- ❌ Don't retry: 400 Bad Request, 401 Unauthorized, validation errors, business rule violations

---

### Q6. Explain @ConditionalOnProperty and other conditional annotations.

**Answer:**

```java
// Only create this bean if property is set:
@Configuration
@ConditionalOnProperty(name = "app.feature.notifications.enabled", havingValue = "true")
public class NotificationConfig {
    @Bean
    public NotificationService notificationService() {
        return new EmailNotificationService();
    }
}

// application.yml:
app:
  feature:
    notifications:
      enabled: true   # set to false to disable entirely
```

**All conditional annotations:**
```java
@ConditionalOnProperty(name = "feature.x", havingValue = "true")  // property check
@ConditionalOnBean(DataSource.class)          // only if this bean exists
@ConditionalOnMissingBean(CacheManager.class) // only if NO bean of this type
@ConditionalOnClass(name = "com.redis.Redis") // only if class on classpath
@ConditionalOnMissingClass("...")             // only if class NOT on classpath
@ConditionalOnExpression("${feature.a} && !${feature.b}")  // SpEL expression

// Profile-based (not @Conditional but similar):
@Profile("prod")          // only in prod profile
@Profile("!test")         // everything except test
```

**Use case — Feature flags:**
```yaml
# application.yml
app:
  feature:
    new-checkout: true
    dark-mode: false
```
```java
@ConditionalOnProperty(name = "app.feature.new-checkout", havingValue = "true")
@Service
public class NewCheckoutService implements CheckoutService { }

@ConditionalOnProperty(name = "app.feature.new-checkout", havingValue = "false", matchIfMissing = true)
@Service
public class LegacyCheckoutService implements CheckoutService { }
```

---

### Q7. Explain @Transactional in depth — propagation, isolation, rollback.

**Answer:**

```java
@Transactional(
    propagation = Propagation.REQUIRED,      // default
    isolation = Isolation.READ_COMMITTED,     // default
    timeout = 30,                             // seconds
    readOnly = false,
    rollbackFor = Exception.class,            // rollback on checked exceptions too
    noRollbackFor = BusinessWarningException.class
)
public Order createOrder(OrderRequest request) { ... }
```

**Propagation types (most asked):**

| Type | Behavior |
|------|----------|
| `REQUIRED` (default) | Join existing txn, or create new one |
| `REQUIRES_NEW` | Always create new txn (suspend existing) |
| `MANDATORY` | Must run inside existing txn (throw if none) |
| `SUPPORTS` | Use txn if exists, else run without |
| `NOT_SUPPORTED` | Run without txn (suspend if exists) |
| `NEVER` | Must NOT be in a txn (throw if exists) |
| `NESTED` | Savepoint within existing txn |

**Common interview scenario:**
```java
@Service
public class OrderService {
    @Autowired private PaymentService paymentService;

    @Transactional  // REQUIRED
    public void createOrder(OrderRequest req) {
        orderRepo.save(order);
        paymentService.processPayment(order);  // REQUIRES_NEW
        // if this line throws → order rolled back, but payment already committed!
    }
}

@Service
public class PaymentService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPayment(Order order) {
        // runs in its OWN transaction — commits independently
    }
}
```

**⚠️ @Transactional pitfalls:**
1. **Self-invocation:** Same as @Async — calling within same class bypasses proxy
2. **Checked exceptions:** By default, Spring only rolls back on `RuntimeException`. Use `rollbackFor = Exception.class`
3. **readOnly = true:** Hint to DB/ORM for optimization (no dirty checking in Hibernate)
4. **Private methods:** `@Transactional` on private methods is silently ignored

---

### Q8. Explain @Value, @ConfigurationProperties, and externalized configuration.

**Answer:**

```java
// Simple value injection:
@Value("${app.payment.timeout:5000}")  // default 5000 if not set
private int paymentTimeout;

@Value("${app.feature.enabled:false}")
private boolean featureEnabled;

@Value("${APP_SECRET_KEY}")  // from environment variable
private String secretKey;
```

```java
// Better: Type-safe @ConfigurationProperties:
@ConfigurationProperties(prefix = "app.payment")
@Validated
public class PaymentProperties {
    @NotBlank private String gatewayUrl;
    @Min(1000) private int timeout = 5000;
    @NotNull private RetryConfig retry = new RetryConfig();

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long delay = 1000;
        // getters, setters
    }
    // getters, setters
}
```

```yaml
# application.yml:
app:
  payment:
    gateway-url: https://api.payment.com
    timeout: 10000
    retry:
      max-attempts: 5
      delay: 2000
```

**Property source priority (highest → lowest):**
```
1. Command line args (--server.port=9090)
2. JNDI attributes
3. System properties (-Dserver.port=9090)
4. Environment variables (SERVER_PORT=9090)
5. application-{profile}.yml
6. application.yml
7. @PropertySource annotations
8. Default properties
```

---

### Q9. Explain @ControllerAdvice and global exception handling.

**Answer:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));
        return new ErrorResponse("VALIDATION_FAILED", "Validation error", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DataIntegrityViolationException ex) {
        return new ErrorResponse("DUPLICATE", "Resource already exists");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        // Don't expose stack trace or internal details to client!
    }
}

// Standard error response DTO:
public record ErrorResponse(
    String code,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fieldErrors
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
```

---

### Q10. Explain Spring Boot Actuator endpoints and production readiness.

**Answer:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, env, loggers
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true  # /health/liveness, /health/readiness for K8s
  info:
    env:
      enabled: true
```

**Key actuator endpoints:**
| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health (UP/DOWN) — K8s liveness/readiness |
| `/actuator/metrics` | JVM, Tomcat, HikariCP, custom metrics |
| `/actuator/prometheus` | Metrics in Prometheus format |
| `/actuator/loggers` | View/change log levels at runtime! |
| `/actuator/env` | View resolved configuration properties |
| `/actuator/threaddump` | Thread dump (debugging deadlocks) |
| `/actuator/heapdump` | Download heap dump |
| `/actuator/info` | Build info, git commit, custom info |

**Custom health indicator:**
```java
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        boolean gatewayReachable = checkGateway();
        if (gatewayReachable) {
            return Health.up().withDetail("gateway", "reachable").build();
        }
        return Health.down().withDetail("gateway", "unreachable").build();
    }
}
```

**Security:** NEVER expose all actuator endpoints publicly. Use Spring Security to restrict:
```java
@Bean
public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/**").hasRole("ADMIN"))
        .build();
}
```
