# Exception Handling — Interview Q&A

> 15 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. Explain the Java exception hierarchy.

```
Throwable
├── Error (unrecoverable — don't catch)
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── VirtualMachineError
└── Exception
    ├── Checked Exceptions (must handle or declare)
    │   ├── IOException
    │   ├── SQLException
    │   └── ClassNotFoundException
    └── RuntimeException (unchecked — optional to catch)
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── IndexOutOfBoundsException
        ├── ConcurrentModificationException
        └── ClassCastException
```

**Checked:** Compiler forces handling. Recoverable situations (file not found, network down).  
**Unchecked (Runtime):** Programming bugs. Fix the code, don't catch.  
**Error:** JVM-level. Almost never catch (`OutOfMemoryError` recovery is unreliable).

---

### Q2. What is try-with-resources? Why is it better than try-finally?

```java
// ❌ try-finally: verbose, error-prone, suppresses original exception
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("file.txt"));
    return reader.readLine();
} finally {
    if (reader != null) reader.close(); // If this throws, original exception is LOST
}

// ✅ try-with-resources: auto-close, suppressed exceptions preserved
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    return reader.readLine();
} // reader.close() called automatically, even on exception
  // If both readLine() and close() throw, close() exception is SUPPRESSED (accessible via getSuppressed())
```

Any class implementing `AutoCloseable` or `Closeable` works with try-with-resources. **Multiple resources** are closed in reverse declaration order.

---

### Q3. What is the difference between `throw` and `throws`?

```java
// throws — declares that a method CAN throw (in method signature)
public void readFile(String path) throws IOException {
    // ...
}

// throw — actually throws an exception (in method body)
if (path == null) {
    throw new IllegalArgumentException("Path cannot be null");
}
```

---

## Scenario-Based Questions

### Q4. At FedEx, your SEFS-PDDV service receives malformed scan events from STARV scanners. How would you design exception handling to: (a) not crash the service, (b) log the bad event, (c) continue processing?

```java
public void processEventBatch(List<RawScanEvent> events) {
    for (RawScanEvent raw : events) {
        try {
            ScanEvent event = parser.parse(raw);
            processor.process(event);
        } catch (MalformedEventException e) {
            log.warn("Skipping malformed event: {} | Error: {}", raw.getId(), e.getMessage());
            metrics.increment("events.malformed");
            deadLetterQueue.send(raw); // Preserve for later investigation
        } catch (ProcessingException e) {
            log.error("Processing failed for event: {}", raw.getId(), e);
            metrics.increment("events.processing_failed");
            retryQueue.send(raw); // Retry later
        }
        // No catch-all — let unexpected exceptions bubble up to circuit breaker
    }
}
```

**Key decisions:**
- **Granular catch blocks** — malformed (skip) vs processing failure (retry)
- **Dead letter queue** for unrecoverable events — never lose data
- **No `catch (Exception e)`** — unexpected errors should trigger alerts via global handler
- **Metrics** — AppDynamics/Splunk dashboards track error rates

---

### Q5. At NPCI, payment transaction failures need different handling based on the failure stage. How would you design a custom exception hierarchy?

```java
// Base exception with transaction context
public class PaymentException extends RuntimeException {
    private final String transactionId;
    private final PaymentStage stage;
    private final ErrorCode errorCode;
    
    public PaymentException(String txnId, PaymentStage stage, ErrorCode code, String msg) {
        super(msg);
        this.transactionId = txnId;
        this.stage = stage;
        this.errorCode = code;
    }
}

// Stage-specific exceptions
public class ValidationException extends PaymentException { /* field validation failures */ }
public class DebitFailureException extends PaymentException { /* sender bank declined */ }
public class CreditFailureException extends PaymentException { /* receiver bank issue — needs reversal! */ }
public class TimeoutException extends PaymentException { /* SLA breach */ }

// Handler
@ControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(CreditFailureException.class)
    ResponseEntity<?> handleCreditFailure(CreditFailureException e) {
        reversalService.initiateReversal(e.getTransactionId()); // CRITICAL: reverse the debit
        return ResponseEntity.status(502).body(ErrorResponse.of(e));
    }
}
```

**CreditFailureException is special** — if credit fails after debit succeeded, you MUST reverse the debit. The exception hierarchy encodes this business logic.

---

### Q6. At Hatio, a junior developer wrote `catch (Exception e) { e.printStackTrace(); }` everywhere. What's wrong and how do you fix it?

**Problems:**
1. **Catches everything** including `NullPointerException`, `ClassCastException` — hides bugs
2. **`printStackTrace()` goes to stderr** — not captured by Splunk/log aggregators
3. **Swallows the exception** — caller thinks operation succeeded
4. **No recovery action** — no retry, no fallback, no metrics

**Fix:**
```java
// ❌ BAD
try { processPayment(txn); }
catch (Exception e) { e.printStackTrace(); }

// ✅ GOOD
try {
    processPayment(txn);
} catch (InsufficientFundsException e) {
    log.warn("Insufficient funds for txn {}: {}", txn.id(), e.getMessage());
    return PaymentResult.declined(e.getErrorCode());
} catch (GatewayTimeoutException e) {
    log.error("Gateway timeout for txn {}", txn.id(), e);
    retryQueue.enqueue(txn);
    return PaymentResult.pending();
}
// Let unexpected RuntimeExceptions propagate — global handler catches them
```

---

## Coding Challenges

### Challenge 1: Custom Exception Hierarchy
**File:** `solutions/CustomExceptions.java`  
Design a complete exception hierarchy for an e-commerce order processing system:
1. `OrderException` (base) with orderId and errorCode
2. `PaymentDeclinedException`, `InventoryUnavailableException`, `ShippingException`
3. An `OrderProcessor` that throws appropriate exceptions at each stage
4. A handler that takes different actions per exception type
5. Demo in `main()` with various failure scenarios

### Challenge 2: Retry with Exception Handling
**File:** `solutions/RetryMechanism.java`  
Implement a generic retry mechanism:
1. `retry(Callable<T> task, int maxRetries, Duration delay)` — retries on exception
2. Only retry on specific exception types (configurable)
3. Exponential backoff between retries
4. Return the result or throw the last exception after all retries exhausted
5. Test with a task that fails 2 times then succeeds

---

## Gotchas & Edge Cases

### Q7. Can you catch multiple exceptions in a single catch block?

```java
// Java 7+ multi-catch
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    log.error("IO or SQL error", e);
    // e is effectively final — can't reassign
}
```

**Restriction:** The exceptions must not be in the same hierarchy (can't catch `Exception | IOException` — IOException is already a subtype of Exception).

---

### Q8. What happens if an exception is thrown in a `finally` block?

```java
try {
    throw new RuntimeException("Original");
} finally {
    throw new RuntimeException("From finally"); // ❌ ORIGINAL EXCEPTION IS LOST!
}
// Only "From finally" propagates — "Original" is silently discarded
```

**This is why try-with-resources is preferred** — it preserves the original exception and adds the close() exception as suppressed.

---

### Q9. What is exception chaining? Why is it important?

```java
try {
    jdbcTemplate.update(sql);
} catch (DataAccessException e) {
    throw new PaymentProcessingException("Failed to record payment", e); // Chain the cause
}
```

Without chaining (`throw new PaymentProcessingException("Failed")` without `e`), the original stack trace is lost. Debugging becomes impossible — you see where the wrapper was thrown but not the root cause.

**Always pass the original exception as the `cause` parameter.**ror
│   └── VirtualMachineError
└── Exception
    ├── Checked Exceptions (must handle or declare)
    │   ├── IOException
    │   ├── SQLException
    │   └── ClassNotFoundException
    └── RuntimeException (unchecked — optional to catch)
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── IndexOutOfBoundsException
        ├── ConcurrentModificationException
        └── ClassCastException
```

**Checked:** Compiler forces handling. Recoverable situations (file not found, network down).  
**Unchecked (Runtime):** Programming bugs. Fix the code, don't catch.  
**Error:** JVM-level. Almost never catch (`OutOfMemoryError` recovery is unreliable).

---

### Q2. What is try-with-resources? Why is it better than try-finally?

```java
// ❌ try-finally: verbose, error-prone, suppresses original exception
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("file.txt"));
    return reader.readLine();
} finally {
    if (reader != null) reader.close(); // If this throws, original exception is LOST
}

// ✅ try-with-resources: auto-close, suppressed exceptions preserved
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    return reader.readLine();
} // reader.close() called automatically, even on exception
  // If both readLine() and close() throw, close() exception is SUPPRESSED (accessible via getSuppressed())
```

Any class implementing `AutoCloseable` or `Closeable` works with try-with-resources. **Multiple resources** are closed in reverse declaration order.

---

### Q3. What is the difference between `throw` and `throws`?

```java
// throws — declares that a method CAN throw (in method signature)
public void readFile(String path) throws IOException {
    // ...
}

// throw — actually throws an exception (in method body)
if (path == null) {
    throw new IllegalArgumentException("Path cannot be null");
}
```

---

## Scenario-Based Questions

### Q4. At FedEx, your SEFS-PDDV service receives malformed scan events from STARV scanners. How would you design exception handling to: (a) not crash the service, (b) log the bad event, (c) continue processing?

```java
public void processEventBatch(List<RawScanEvent> events) {
    for (RawScanEvent raw : events) {
        try {
            ScanEvent event = parser.parse(raw);
            processor.process(event);
        } catch (MalformedEventException e) {
            log.warn("Skipping malformed event: {} | Error: {}", raw.getId(), e.getMessage());
            metrics.increment("events.malformed");
            deadLetterQueue.send(raw); // Preserve for later investigation
        } catch (ProcessingException e) {
            log.error("Processing failed for event: {}", raw.getId(), e);
            metrics.increment("events.processing_failed");
            retryQueue.send(raw); // Retry later
        }
        // No catch-all — let unexpected exceptions bubble up to circuit breaker
    }
}
```

**Key decisions:**
- **Granular catch blocks** — malformed (skip) vs processing failure (retry)
- **Dead letter queue** for unrecoverable events — never lose data
- **No `catch (Exception e)`** — unexpected errors should trigger alerts via global handler
- **Metrics** — AppDynamics/Splunk dashboards track error rates

---

### Q5. At NPCI, payment transaction failures need different handling based on the failure stage. How would you design a custom exception hierarchy?

```java
// Base exception with transaction context
public class PaymentException extends RuntimeException {
    private final String transactionId;
    private final PaymentStage stage;
    private final ErrorCode errorCode;
    
    public PaymentException(String txnId, PaymentStage stage, ErrorCode code, String msg) {
        super(msg);
        this.transactionId = txnId;
        this.stage = stage;
        this.errorCode = code;
    }
}

// Stage-specific exceptions
public class ValidationException extends PaymentException { /* field validation failures */ }
public class DebitFailureException extends PaymentException { /* sender bank declined */ }
public class CreditFailureException extends PaymentException { /* receiver bank issue — needs reversal! */ }
public class TimeoutException extends PaymentException { /* SLA breach */ }

// Handler
@ControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(CreditFailureException.class)
    ResponseEntity<?> handleCreditFailure(CreditFailureException e) {
        reversalService.initiateReversal(e.getTransactionId()); // CRITICAL: reverse the debit
        return ResponseEntity.status(502).body(ErrorResponse.of(e));
    }
}
```

**CreditFailureException is special** — if credit fails after debit succeeded, you MUST reverse the debit. The exception hierarchy encodes this business logic.

---

### Q6. At Hatio, a junior developer wrote `catch (Exception e) { e.printStackTrace(); }` everywhere. What's wrong and how do you fix it?

**Problems:**
1. **Catches everything** including `NullPointerException`, `ClassCastException` — hides bugs
2. **`printStackTrace()` goes to stderr** — not captured by Splunk/log aggregators
3. **Swallows the exception** — caller thinks operation succeeded
4. **No recovery action** — no retry, no fallback, no metrics

**Fix:**
```java
// ❌ BAD
try { processPayment(txn); }
catch (Exception e) { e.printStackTrace(); }

// ✅ GOOD
try {
    processPayment(txn);
} catch (InsufficientFundsException e) {
    log.warn("Insufficient funds for txn {}: {}", txn.id(), e.getMessage());
    return PaymentResult.declined(e.getErrorCode());
} catch (GatewayTimeoutException e) {
    log.error("Gateway timeout for txn {}", txn.id(), e);
    retryQueue.enqueue(txn);
    return PaymentResult.pending();
}
// Let unexpected RuntimeExceptions propagate — global handler catches them
```

---

## Coding Challenges

### Challenge 1: Custom Exception Hierarchy
**File:** `solutions/CustomExceptions.java`  
Design a complete exception hierarchy for an e-commerce order processing system:
1. `OrderException` (base) with orderId and errorCode
2. `PaymentDeclinedException`, `InventoryUnavailableException`, `ShippingException`
3. An `OrderProcessor` that throws appropriate exceptions at each stage
4. A handler that takes different actions per exception type
5. Demo in `main()` with various failure scenarios

### Challenge 2: Retry with Exception Handling
**File:** `solutions/RetryMechanism.java`  
Implement a generic retry mechanism:
1. `retry(Callable<T> task, int maxRetries, Duration delay)` — retries on exception
2. Only retry on specific exception types (configurable)
3. Exponential backoff between retries
4. Return the result or throw the last exception after all retries exhausted
5. Test with a task that fails 2 times then succeeds

---

## Gotchas & Edge Cases

### Q7. Can you catch multiple exceptions in a single catch block?

```java
// Java 7+ multi-catch
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    log.error("IO or SQL error", e);
    // e is effectively final — can't reassign
}
```

**Restriction:** The exceptions must not be in the same hierarchy (can't catch `Exception | IOException` — IOException is already a subtype of Exception).

---

### Q8. What happens if an exception is thrown in a `finally` block?

```java
try {
    throw new RuntimeException("Original");
} finally {
    throw new RuntimeException("From finally"); // ❌ ORIGINAL EXCEPTION IS LOST!
}
// Only "From finally" propagates — "Original" is silently discarded
```

**This is why try-with-resources is preferred** — it preserves the original exception and adds the close() exception as suppressed.

---

### Q9. What is exception chaining? Why is it important?

```java
try {
    jdbcTemplate.update(sql);
} catch (DataAccessException e) {
    throw new PaymentProcessingException("Failed to record payment", e); // Chain the cause
}
```

Without chaining (`throw new PaymentProcessingException("Failed")` without `e`), the original stack trace is lost. Debugging becomes impossible — you see where the wrapper was thrown but not the root cause.

**Always pass the original exception as the `cause` parameter.**
