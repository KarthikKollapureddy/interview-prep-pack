# Spring Boot Advanced Concepts — Interview Q&A

> Concepts from [Snailclimb/JavaGuide](https://github.com/Snailclimb/JavaGuide), [iluwatar/java-design-patterns](https://github.com/iluwatar/java-design-patterns), Baeldung
> Covers: AOP Deep Dive, Design Patterns in Spring, Auto-Configuration Internals, GraalVM Native, Spring Cloud
> **Priority: P1** — These are the "how does Spring actually work?" questions

---

## Q1. Spring AOP Deep Dive — Proxy, Aspects, and Weaving.

```
AOP = Aspect-Oriented Programming
  Separate cross-cutting concerns (logging, security, tx) from business logic

Key Terminology:
  Aspect      — module containing cross-cutting logic (@Aspect)
  Join Point  — point in execution (method call, exception)
  Pointcut    — predicate to match join points
  Advice      — action at a join point (before, after, around)
  Weaving     — linking aspects with target objects

Five Types of Advice:
  @Before        — runs before method
  @After         — runs after method (always, even on exception)
  @AfterReturning— runs after successful return
  @AfterThrowing — runs after exception
  @Around        — wraps method (most powerful)

  @Aspect
  @Component
  public class LoggingAspect {

      // Match all methods in service package
      @Pointcut("execution(* com.app.service.*.*(..))")
      public void serviceMethods() {}

      @Around("serviceMethods()")
      public Object logExecutionTime(ProceedingJoinPoint joinPoint)
              throws Throwable {
          long start = System.currentTimeMillis();
          Object result = joinPoint.proceed();
          long duration = System.currentTimeMillis() - start;
          log.info("{} executed in {}ms",
              joinPoint.getSignature(), duration);
          return result;
      }
  }

Pointcut Expressions:
  execution(* com.app.service.*.*(..))   — all methods in service package
  @annotation(com.app.Loggable)          — methods with @Loggable
  within(com.app.controller.*)           — all methods in controller package
  bean(*Service)                         — all beans ending with "Service"

How Spring AOP Works Internally:
  1. Spring detects @Aspect beans
  2. For each target bean matching a pointcut:
     → Creates a proxy (CGLIB or JDK Dynamic Proxy)
     → Proxy intercepts method calls
     → Executes advice chain around actual method

  ┌──────────┐    ┌───────────┐    ┌──────────────┐
  │  Caller  │───→│ AOP Proxy │───→│ Target Bean  │
  └──────────┘    │ (advice)  │    └──────────────┘
                  └───────────┘

Common Pitfall — Self-Invocation:
  @Service
  public class OrderService {
      @Transactional
      public void createOrder() {
          validate();  // ⚠ NO proxy, AOP bypassed!
      }

      @Cacheable
      public void validate() { ... }
  }
  Fix: Inject self, use AopContext, or extract to another bean
```

---

## Q2. Design Patterns Used in Spring Framework.

```
From: iluwatar/java-design-patterns (93.9K ⭐) + Spring source

1. Singleton Pattern:
   - Spring beans are singleton-scoped by default
   - BeanFactory maintains singleton registry

2. Factory Pattern:
   - BeanFactory — core IoC container
   - FactoryBean<T> — custom bean creation logic

3. Proxy Pattern:
   - AOP proxies (CGLIB/JDK Dynamic Proxy)
   - @Transactional, @Cacheable, @Async — all use proxies

4. Template Method:
   - JdbcTemplate, RestTemplate, JmsTemplate
   - Define algorithm skeleton, subclasses fill in steps

5. Observer Pattern:
   - ApplicationEvent + @EventListener
   - publisher.publishEvent(new OrderCreatedEvent(order));
   - @EventListener void handle(OrderCreatedEvent e) { ... }

6. Strategy Pattern:
   - Resource interface (ClassPathResource, UrlResource, FileResource)
   - HandlerMapping strategies for request routing

7. Adapter Pattern:
   - HandlerAdapter in Spring MVC
   - Different handler types (Controller, HttpRequestHandler) unified

8. Decorator Pattern:
   - BeanPostProcessor — decorates beans after creation
   - ServerHttpRequestDecorator in WebFlux

9. Chain of Responsibility:
   - Filter chain in Spring Security
   - HandlerInterceptor chain in Spring MVC

10. Builder Pattern:
    - UriComponentsBuilder
    - WebClient.builder()
    - SecurityFilterChain configuration

Interview Answer: "Spring heavily uses Proxy (AOP), Template Method
  (JdbcTemplate), Observer (@EventListener), and Factory (BeanFactory).
  Understanding these patterns helps debug Spring internals."
```

---

## Q3. Spring Boot Auto-Configuration — How it really works.

```
Auto-Configuration = Spring Boot's "magic" that configures beans for you

The Flow:
  1. @SpringBootApplication includes @EnableAutoConfiguration
  2. Spring reads META-INF/spring/...AutoConfiguration.imports
     (Spring Boot 3.x) or META-INF/spring.factories (2.x)
  3. Each auto-configuration class has @Conditional annotations
  4. Spring evaluates conditions and creates beans if met

  @AutoConfiguration
  @ConditionalOnClass(DataSource.class)         // only if JDBC on classpath
  @ConditionalOnMissingBean(DataSource.class)   // only if user didn't define
  @EnableConfigurationProperties(DataSourceProperties.class)
  public class DataSourceAutoConfiguration {

      @Bean
      @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
      public DataSource dataSource(DataSourceProperties props) {
          return DataSourceBuilder.create()
              .url(props.getUrl())
              .username(props.getUsername())
              .build();
      }
  }

Key @Conditional Annotations:
  @ConditionalOnClass       — class exists on classpath
  @ConditionalOnMissingBean — user hasn't defined this bean
  @ConditionalOnProperty    — property is set
  @ConditionalOnWebApplication — it's a web app
  @ConditionalOnBean        — another bean exists

Debugging Auto-Configuration:
  # application.properties
  debug=true
  # Shows "Positive matches" and "Negative matches" in startup log

  Or: /actuator/conditions endpoint

Creating Custom Starter:
  1. Create auto-configuration module with @AutoConfiguration
  2. Register in META-INF/spring/...AutoConfiguration.imports
  3. Create starter module that depends on auto-config
  4. Users just add starter dependency — beans auto-created
```

---

## Q4. Spring Cloud — Microservices Patterns.

```
Spring Cloud = toolbox for distributed systems

Core Components:
  ┌────────────────────────────────────────────┐
  │            Spring Cloud Ecosystem          │
  ├────────────────────┬───────────────────────┤
  │ Service Discovery  │ Eureka, Consul        │
  │ Config Server      │ Spring Cloud Config   │
  │ API Gateway        │ Spring Cloud Gateway  │
  │ Load Balancer      │ Spring Cloud LoadBal. │
  │ Circuit Breaker    │ Resilience4j          │
  │ Distributed Tracing│ Micrometer + Zipkin   │
  │ Messaging          │ Spring Cloud Stream   │
  │ Security           │ Spring Cloud Security │
  └────────────────────┴───────────────────────┘

Spring Cloud Gateway (replacing Zuul):
  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
      return builder.routes()
          .route("user-service", r -> r
              .path("/api/users/**")
              .filters(f -> f
                  .stripPrefix(1)
                  .addRequestHeader("X-Source", "gateway")
                  .circuitBreaker(c -> c.setName("userCB")))
              .uri("lb://user-service"))  // load-balanced
          .build();
  }

Spring Cloud Config:
  - Centralized config in Git repo
  - /actuator/refresh or Spring Cloud Bus for live updates
  - Encryption/decryption of sensitive properties

Resilience4j Integration:
  @CircuitBreaker(name = "userService", fallbackMethod = "fallback")
  @Retry(name = "userService")
  @TimeLimiter(name = "userService")
  public CompletableFuture<User> getUser(String id) {
      return CompletableFuture.supplyAsync(
          () -> restClient.get("/users/" + id, User.class));
  }

  public CompletableFuture<User> fallback(String id, Throwable t) {
      return CompletableFuture.completedFuture(User.defaultUser());
  }
```

---

## Q5. GraalVM Native Image with Spring Boot 3.

```
GraalVM Native = compile Java to standalone binary (no JVM needed!)

Benefits:
  ✓ Startup: 50-100ms (vs 2-5s with JVM)
  ✓ Memory: 50-100MB (vs 200-500MB with JVM)
  ✓ No JVM needed at runtime
  ✓ Perfect for serverless (Lambda, Cloud Run)

How It Works:
  1. Ahead-of-Time (AOT) compilation
  2. Closed-world analysis — all code paths resolved at build time
  3. Dead code elimination — only used code included

Limitations:
  ✗ No runtime reflection (must be declared at build time)
  ✗ No dynamic class loading
  ✗ No dynamic proxies (JDK Proxy, CGLIB)
  ✗ Longer build time (5-15 minutes)
  ✗ Not all libraries compatible

Spring Boot 3 Native Support:
  // Just add the plugin
  // build.gradle
  plugins {
      id 'org.graalvm.buildtools.native'
  }

  // Build native image
  ./gradlew nativeCompile

  // Run
  ./build/native/nativeCompile/myapp  // starts in ~50ms!

  Spring AOT engine handles:
  - Generating reflection hints
  - Replacing proxies with generated code
  - Pre-computing bean definitions

Interview Tip: "Native images are ideal for microservices and serverless
  where cold start matters. But for long-running monoliths, JVM with
  JIT is still faster for peak throughput."
```

---

## Q6. Spring Security Filter Chain — How authentication flows.

```
Spring Security = servlet filter chain that intercepts every request

Request Flow:
  Client → [Filter Chain] → Controller
              │
              ├── SecurityContextPersistenceFilter
              ├── CorsFilter
              ├── CsrfFilter
              ├── LogoutFilter
              ├── UsernamePasswordAuthenticationFilter (form login)
              │   or BearerTokenAuthenticationFilter (JWT)
              ├── ExceptionTranslationFilter
              └── AuthorizationFilter (formerly FilterSecurityInterceptor)

JWT Authentication Flow:
  1. Client sends: Authorization: Bearer <token>
  2. BearerTokenAuthenticationFilter extracts token
  3. JwtAuthenticationProvider validates + decodes JWT
  4. Creates Authentication object with authorities
  5. Stores in SecurityContextHolder (ThreadLocal)
  6. AuthorizationFilter checks if user has required role

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
      return http
          .csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(s ->
              s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/public/**").permitAll()
              .requestMatchers("/api/admin/**").hasRole("ADMIN")
              .anyRequest().authenticated())
          .oauth2ResourceServer(oauth2 ->
              oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
          .build();
  }

Method-Level Security:
  @PreAuthorize("hasRole('ADMIN') and #userId == authentication.principal.id")
  public User updateUser(Long userId, UserDto dto) { ... }

  @PostAuthorize("returnObject.owner == authentication.name")
  public Document getDocument(Long id) { ... }
```

---

## Q7. Spring Data JPA — Repository Patterns and Query Methods.

```
Spring Data JPA eliminates boilerplate DAO code

Repository Hierarchy:
  Repository (marker)
    └── CrudRepository (CRUD ops)
        └── ListCrudRepository
            └── JpaRepository (flush, batch, paging)

Query Method Derivation:
  // Spring generates SQL from method names!
  List<User> findByNameAndAge(String name, int age);
  List<User> findByEmailContainingIgnoreCase(String email);
  Optional<User> findFirstByOrderByCreatedAtDesc();
  List<User> findByAgeGreaterThanEqual(int age);
  long countByStatus(Status status);
  boolean existsByEmail(String email);

  // Custom queries
  @Query("SELECT u FROM User u WHERE u.email LIKE %:domain")
  List<User> findByEmailDomain(@Param("domain") String domain);

  @Query(value = "SELECT * FROM users WHERE age > :age", nativeQuery = true)
  List<User> findOlderThan(@Param("age") int age);

Specifications (dynamic queries):
  public static Specification<User> hasName(String name) {
      return (root, query, cb) -> cb.equal(root.get("name"), name);
  }
  public static Specification<User> olderThan(int age) {
      return (root, query, cb) -> cb.greaterThan(root.get("age"), age);
  }

  // Combine dynamically
  userRepository.findAll(hasName("Karthik").and(olderThan(25)));

Projections (select specific fields):
  // Interface-based projection (generated at runtime)
  public interface UserSummary {
      String getName();
      String getEmail();
  }
  List<UserSummary> findByStatus(Status status);
```

---

## Q8. Spring WebClient & Reactive Patterns.

```
WebClient = non-blocking HTTP client (replaces RestTemplate)

  WebClient client = WebClient.builder()
      .baseUrl("https://api.example.com")
      .defaultHeader("Authorization", "Bearer " + token)
      .build();

  // Non-blocking GET
  Mono<User> user = client.get()
      .uri("/users/{id}", userId)
      .retrieve()
      .bodyToMono(User.class);

  // With error handling
  Mono<User> user = client.get()
      .uri("/users/{id}", userId)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError,
          response -> Mono.error(new UserNotFoundException()))
      .onStatus(HttpStatusCode::is5xxServerError,
          response -> Mono.error(new ServiceUnavailableException()))
      .bodyToMono(User.class)
      .timeout(Duration.ofSeconds(3))
      .retryWhen(Retry.backoff(3, Duration.ofMillis(500)));

  // Parallel calls (scatter-gather)
  Mono<OrderDetails> details = Mono.zip(
      userClient.getUser(userId),
      orderClient.getOrders(userId),
      paymentClient.getPayments(userId)
  ).map(tuple -> new OrderDetails(
      tuple.getT1(), tuple.getT2(), tuple.getT3()));

RestClient (Spring 6.1+ — synchronous but modern):
  RestClient client = RestClient.builder()
      .baseUrl("https://api.example.com")
      .build();

  User user = client.get()
      .uri("/users/{id}", userId)
      .retrieve()
      .body(User.class);

When to use which:
  RestTemplate → Legacy (maintenance mode)
  RestClient   → New sync code (Spring 6.1+)
  WebClient    → Reactive / async code
```

---

*Sources: [Snailclimb/JavaGuide](https://github.com/Snailclimb/JavaGuide), [iluwatar/java-design-patterns](https://github.com/iluwatar/java-design-patterns), Spring Documentation*
