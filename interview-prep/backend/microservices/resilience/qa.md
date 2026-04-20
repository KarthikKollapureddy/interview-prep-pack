# Resilience Patterns — Interview Q&A

> 15 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What is the Circuit Breaker pattern? Explain the state machine.

```
        ┌─────────┐   failure threshold    ┌──────┐
  ──────│ CLOSED   │──────────────────────→│ OPEN  │
        │(normal)  │                       │(fail  │
        └─────────┘                       │ fast) │
             ↑                             └──┬───┘
             │                                │
             │   success in half-open          │ wait duration
             │                                ↓
             │                          ┌───────────┐
             └──────────────────────────│ HALF-OPEN  │
                                        │(test)      │
                                        └───────────┘
```

**States:**
- **CLOSED** — normal operation, requests pass through. Track failure rate.
- **OPEN** — failure rate exceeded threshold. All calls fail immediately (no network call). Wait for timeout.
- **HALF-OPEN** — allow a limited number of test calls. If they succeed → CLOSED. If they fail → back to OPEN.

```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
public PaymentResponse processPayment(PaymentRequest req) {
    return gatewayClient.charge(req);
}

private PaymentResponse paymentFallback(PaymentRequest req, Exception e) {
    log.warn("Circuit breaker open for payment gateway: {}", e.getMessage());
    return PaymentResponse.pending("Payment queued for retry");
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        failure-rate-threshold: 50       # Open at 50% failure rate
        minimum-number-of-calls: 10      # Need at least 10 calls to evaluate
        wait-duration-in-open-state: 30s # Wait 30s before half-open
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 20
```

---

### Q2. What is the Bulkhead pattern?

**Problem:** One slow dependency consumes all threads → entire application hangs.

**Solution:** Isolate thread pools per dependency. If payment gateway is slow, only its threads are consumed — shipment service threads remain available.

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10    # Max 10 concurrent calls
        max-wait-duration: 100ms    # Wait max 100ms for a slot
      shipmentService:
        max-concurrent-calls: 25
```

```java
@Bulkhead(name = "paymentService", fallbackMethod = "paymentFallback")
public PaymentResponse charge(PaymentRequest req) {
    return paymentClient.charge(req);
}
```

**Thread pool bulkhead vs Semaphore bulkhead:**
- **Semaphore** — limits concurrent calls, same thread. Lighter.
- **Thread pool** — separate thread pool, provides timeout control. Heavier.

---

### Q3. What is the Retry pattern? How do you configure it with Resilience4j?

```yaml
resilience4j:
  retry:
    instances:
      externalApi:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
        ignore-exceptions:
          - com.example.BusinessException  # Don't retry business logic errors
        exponential-backoff-multiplier: 2  # 500ms, 1s, 2s
```

```java
@Retry(name = "externalApi", fallbackMethod = "apiFallback")
public ApiResponse callExternalApi(ApiRequest req) {
    return externalClient.call(req);
}
```

**Key decision:** Only retry on transient errors (network, timeout). Never retry on validation errors or business rule violations — they'll fail every time.

---

### Q4. Rate Limiter pattern — when and how?

```yaml
resilience4j:
  ratelimiter:
    instances:
      upiApi:
        limit-for-period: 1000        # 1000 requests
        limit-refresh-period: 1s       # per second
        timeout-duration: 500ms        # wait 500ms for permission
```

**At NPCI:** Rate limit UPI transactions to prevent system overload during peak times (salary day, festival sales).

---

## Scenario-Based Questions

### Q5. At FedEx, the STARV scanner API occasionally goes down. Design a resilient integration.

```java
@Service
public class ScannerService {

    @CircuitBreaker(name = "starvScanner", fallbackMethod = "scannerFallback")
    @Retry(name = "starvScanner")
    @Bulkhead(name = "starvScanner")
    public ScanResult getScanResult(String scanId) {
        return starvClient.getScan(scanId);
    }

    private ScanResult scannerFallback(String scanId, Exception e) {
        log.warn("STARV scanner unavailable, queuing for retry: {}", scanId);
        retryQueue.enqueue(scanId);
        return ScanResult.pending(scanId);
    }
}
```

**Order of decorators matters:** Retry → CircuitBreaker → Bulkhead (outer to inner).  
Retry wraps the circuit breaker — each retry attempt counts toward the circuit breaker's failure count.

**Monitoring with AppDynamics:**
- Circuit breaker state transitions → alert when OPEN
- Retry count metrics → alert when retries spike
- Bulkhead rejection count → alert when threads exhausted

---

### Q6. At NPCI, how do you prevent cascading failures when the bank API is slow?

```
UPI Service → Account Service → Bank API (slow!)
                                    ↓ timeout
                              Circuit breaker opens
                                    ↓
                              Fallback: "Bank temporarily unavailable, try in 30s"
```

**Defense layers:**
1. **Timeout** — `WebClient.timeout(Duration.ofSeconds(2))` — don't wait forever
2. **Circuit Breaker** — fail fast after threshold
3. **Bulkhead** — limit concurrent calls to bank API
4. **Fallback** — return cached data or pending status
5. **Async decoupling** — put request on Kafka, process when bank recovers

---

### Q7. At Hatio, how do you implement graceful degradation for the payment processing system?

```java
@Service
public class PaymentService {

    // Primary: real-time processing
    @CircuitBreaker(name = "primaryPayment", fallbackMethod = "fallbackPayment")
    public PaymentResult process(PaymentRequest req) {
        return primaryGateway.charge(req);
    }

    // Fallback 1: secondary payment gateway
    private PaymentResult fallbackPayment(PaymentRequest req, Exception e) {
        log.warn("Primary gateway down, trying secondary");
        try {
            return secondaryGateway.charge(req);
        } catch (Exception e2) {
            return queueForLater(req); // Fallback 2
        }
    }

    // Fallback 2: queue for async processing
    private PaymentResult queueForLater(PaymentRequest req) {
        kafkaTemplate.send("payment-retry", req);
        return PaymentResult.pending("Payment queued, will be processed shortly");
    }
}
```

**Degradation hierarchy:** Primary → Secondary → Queue → Reject with clear error

---

## Coding Challenges

### Challenge 1: Circuit Breaker Implementation
**File:** `solutions/CircuitBreakerImpl.java`  
Implement a circuit breaker from scratch:
1. Three states: CLOSED, OPEN, HALF_OPEN
2. Configurable failure threshold and timeout
3. Sliding window for failure counting
4. Thread-safe state transitions
5. Test with a service that fails intermittently

### Challenge 2: Retry with Backoff
**File:** `solutions/RetryWithBackoff.java`  
Implement a configurable retry mechanism:
1. Fixed delay, exponential backoff, and jitter strategies
2. Configurable max attempts
3. Exception type filtering (retry only specific exceptions)
4. Callback for each retry attempt (logging)
5. Demo with simulated flaky service

---

## Gotchas & Edge Cases

### Q8. What's the "thundering herd" problem with circuit breakers?

When a circuit breaker transitions from OPEN to HALF-OPEN, all waiting requests try to go through simultaneously → overwhelms the recovering service.

**Fix:** Allow only a limited number of calls in HALF-OPEN state. Add jitter to retry delays so requests spread out.

---

### Q9. Should you retry on timeout exceptions?

**It depends.** If the operation is idempotent (GET, idempotent PUT), yes. If it's non-idempotent (POST payment), **NO** — the first request may have succeeded but timed out. You'd charge the customer twice.

**Always make consumers idempotent before enabling retries for non-idempotent operations.**
