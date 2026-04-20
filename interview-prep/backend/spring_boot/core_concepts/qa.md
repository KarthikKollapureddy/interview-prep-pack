# Spring Boot Core Concepts — Interview Q&A

> 18 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What is Spring Boot auto-configuration? How does it work internally?

Spring Boot auto-configuration automatically configures beans based on classpath dependencies and properties.

**Mechanism:**
1. `@SpringBootApplication` includes `@EnableAutoConfiguration`
2. Spring reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Boot 3.x) or `spring.factories` (Boot 2.x)
3. Each auto-config class has `@Conditional` annotations:
   - `@ConditionalOnClass` — only if class is on classpath
   - `@ConditionalOnMissingBean` — only if user hasn't defined this bean
   - `@ConditionalOnProperty` — only if property is set

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.url")
public class DataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }
}
```

**Key principle:** User-defined beans always win (`@ConditionalOnMissingBean`). You can override any auto-configured bean by defining your own.

**Debug:** `--debug` flag or `spring.autoconfigure.exclude` to see/disable auto-configs.

---

### Q2. Explain the difference between `@Component`, `@Service`, `@Repository`, `@Controller`.

All are specializations of `@Component` — they all register the class as a Spring bean via component scanning.

| Annotation | Layer | Special behavior |
|-----------|-------|-----------------|
| `@Component` | Generic | Just registers as bean |
| `@Service` | Business logic | No extra behavior (semantic marker) |
| `@Repository` | Data access | Translates persistence exceptions to Spring's `DataAccessException` |
| `@Controller` | Web layer | Enables `@RequestMapping`, returns view names |
| `@RestController` | REST API | `@Controller` + `@ResponseBody` on all methods |

**At FedEx:** Services annotated `@Service`, repositories `@Repository` (enables exception translation for JPA/JDBC errors).

---

### Q3. What is the Spring Bean lifecycle? Describe the complete sequence.

```
1. Bean Definition Loading (from @Component scan, @Bean, XML)
2. Bean Instantiation (constructor)
3. Dependency Injection (setter/@Autowired)
4. BeanNameAware.setBeanName()
5. BeanFactoryAware.setBeanFactory()
6. ApplicationContextAware.setApplicationContext()
7. BeanPostProcessor.postProcessBeforeInitialization()
8. @PostConstruct
9. InitializingBean.afterPropertiesSet()
10. Custom init-method
11. BeanPostProcessor.postProcessAfterInitialization()
── Bean is ready ──
12. @PreDestroy
13. DisposableBean.destroy()
14. Custom destroy-method
```

**Most commonly used:** `@PostConstruct` (initialize resources after injection) and `@PreDestroy` (cleanup before shutdown).

```java
@Service
public class CacheService {
    @PostConstruct
    void loadCache() { /* pre-warm cache from DB */ }
    
    @PreDestroy
    void flushCache() { /* write dirty entries back to DB */ }
}
```

---

### Q4. Explain Spring Bean scopes.

| Scope | Description | Use case |
|-------|-------------|----------|
| **singleton** (default) | One instance per ApplicationContext | Stateless services, repos |
| **prototype** | New instance per injection/request | Stateful beans, builders |
| **request** | One per HTTP request | Request-scoped data |
| **session** | One per HTTP session | User session data |
| **application** | One per ServletContext | App-wide config |

**Gotcha:** Injecting a prototype bean into a singleton → you get ONE instance (not a new one each time). Fix: use `ObjectProvider<T>` or `@Lookup` annotation.

```java
@Service
class OrderService { // singleton
    private final ObjectProvider<OrderProcessor> processorProvider;
    
    void process(Order o) {
        OrderProcessor proc = processorProvider.getObject(); // new instance each time
        proc.execute(o);
    }
}
```

---

### Q5. What is Spring Actuator? How do you use it for production monitoring?

Spring Actuator exposes operational endpoints for health checks, metrics, and environment info.

**Key endpoints:**
| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | App health status (DB, disk, custom checks) |
| `/actuator/metrics` | JVM memory, thread counts, HTTP request stats |
| `/actuator/info` | App version, git info |
| `/actuator/env` | Configuration properties (⚠️ sanitize secrets) |
| `/actuator/loggers` | Change log levels at runtime |

**At FedEx with AppDynamics:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info, prometheus
  endpoint:
    health:
      show-details: when_authorized
  health:
    kafka:
      enabled: true  # custom Kafka health indicator
```

Custom health check:
```java
@Component
public class StarVScannerHealth implements HealthIndicator {
    public Health health() {
        if (scannerClient.isReachable()) return Health.up().build();
        return Health.down().withDetail("error", "STARV scanner unreachable").build();
    }
}
```

---

## Scenario-Based Questions

### Q6. At FedEx, you need different configurations for dev, staging, and production. How do you manage Spring profiles?

```yaml
# application.yml (common)
spring:
  application:
    name: sefs-pddv-service

# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fedex_dev
logging:
  level:
    root: DEBUG

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db-cluster:3306/fedex
    hikari:
      maximum-pool-size: 20
logging:
  level:
    root: WARN
```

**Activation:** `SPRING_PROFILES_ACTIVE=prod` (env var) or `--spring.profiles.active=prod` (CLI).

**Profile-specific beans:**
```java
@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() { /* HikariCP with prod settings */ }
}
```

**At FedEx PCF:** Profile is set via Cloud Foundry environment. Never hardcode profile in code.

---

### Q7. At Hatio, you need to build a custom Spring Boot starter for audit logging that other teams can plug into their services. How?

```
audit-spring-boot-starter/
├── src/main/java/
│   ├── AuditAutoConfiguration.java
│   ├── AuditProperties.java
│   └── AuditLogInterceptor.java
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

```java
@ConfigurationProperties(prefix = "hatio.audit")
public record AuditProperties(boolean enabled, String topic) {}

@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "hatio.audit.enabled", havingValue = "true")
public class AuditAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuditLogInterceptor auditInterceptor(AuditProperties props) {
        return new AuditLogInterceptor(props.topic());
    }
}
```

**Usage in any service:**
```yaml
hatio:
  audit:
    enabled: true
    topic: audit-events
```

---

### Q8. At NPCI, your service starts slowly because it initializes large caches and database connections eagerly. How do you optimize startup time?

1. **Lazy initialization:** `spring.main.lazy-initialization=true` (only create beans when first accessed)
2. **Async initialization:** Use `@Async` + `@PostConstruct` for cache warming
3. **GraalVM Native Image:** Compile to native binary — sub-second startup
4. **Reduce classpath scanning:** Narrow `@ComponentScan(basePackages = "com.npci.upi")`
5. **Connection pool lazy init:** `spring.datasource.hikari.initialization-fail-timeout=-1`

```java
@Bean
@Lazy
public ExpensiveCache transactionCache() {
    return new ExpensiveCache(); // Only created on first use
}
```

---

## Coding Challenges

### Challenge 1: Custom Spring Boot Starter
**File:** `solutions/CustomStarterDesign.java`  
Design (pseudo-code) a Spring Boot starter for rate limiting:
1. `@ConfigurationProperties` for rate limit config (requests/sec, burst size)
2. Auto-configuration with `@ConditionalOnClass`, `@ConditionalOnMissingBean`
3. A `RateLimitInterceptor` that plugs into Spring MVC
4. Show how a consuming app would use it

### Challenge 2: Bean Lifecycle Demo
**File:** `solutions/BeanLifecycleDemo.java`  
Create a class that demonstrates the full bean lifecycle:
1. Implement `BeanNameAware`, `ApplicationContextAware`, `InitializingBean`, `DisposableBean`
2. Add `@PostConstruct` and `@PreDestroy`
3. Print a message at each stage
4. Predict and verify the order of execution

---

## Gotchas & Edge Cases

### Q9. What is circular dependency? How does Spring handle it?

```java
@Service class A { @Autowired B b; }
@Service class B { @Autowired A a; }
```

**Spring Boot 2.6+:** Circular dependencies are **prohibited by default**. Throws `BeanCurrentlyInCreationException`.

**Fix approaches (in order of preference):**
1. **Redesign** — extract shared logic into a third service (best)
2. **Use `@Lazy`** — `@Autowired @Lazy B b;` (injects a proxy, resolves lazily)
3. **Use setter injection** instead of constructor injection (Spring can inject partially created beans)
4. **`spring.main.allow-circular-references=true`** (last resort, not recommended)

---

### Q10. `@Value` vs `@ConfigurationProperties` — when to use each?

```java
// @Value — simple, one-off values
@Value("${app.timeout:5000}")
private int timeout;

// @ConfigurationProperties — structured, type-safe, validated
@ConfigurationProperties(prefix = "app")
@Validated
public record AppConfig(
    @NotNull int timeout,
    @NotEmpty String name,
    DatabaseConfig database
) {}
```

**Use `@ConfigurationProperties`** when you have related properties (group under a prefix). Use `@Value` for isolated values. Never use `@Value` for more than 3-4 properties — it becomes unmaintainable.
