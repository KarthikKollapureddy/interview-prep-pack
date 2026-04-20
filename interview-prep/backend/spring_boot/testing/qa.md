# Spring Boot Testing (JUnit 5 + Mockito) — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. Explain the testing pyramid for Spring Boot applications.

```
        /  E2E  \          ← Few (Selenium, Playwright)
       / Integr. \         ← Some (@SpringBootTest, @WebMvcTest, @DataJpaTest)
      /   Unit    \        ← Many (JUnit 5 + Mockito, no Spring context)
```

| Test Type | What | Spring Context | Speed |
|-----------|------|---------------|-------|
| Unit | Single class, mock dependencies | No | Milliseconds |
| Slice | One layer (web, data) | Partial | Seconds |
| Integration | Full app, real DB (TestContainers) | Full | 10+ seconds |
| E2E | Browser / API | Full + deployment | Minutes |

**Goal:** 70% unit, 20% integration/slice, 10% E2E.

---

### Q2. JUnit 5 vs JUnit 4 — key differences?

| Feature | JUnit 4 | JUnit 5 |
|---------|---------|---------|
| Import | `org.junit` | `org.junit.jupiter` |
| Test annotation | `@Test` | `@Test` (no `expected` attribute) |
| Before/After | `@Before/@After` | `@BeforeEach/@AfterEach` |
| Class-level | `@BeforeClass/@AfterClass` | `@BeforeAll/@AfterAll` |
| Assertions | `Assert.assertEquals` | `Assertions.assertEquals` |
| Exception testing | `@Test(expected=...)` | `assertThrows()` |
| Parameterized | `@RunWith(Parameterized.class)` | `@ParameterizedTest` + `@ValueSource` |
| Extensions | `@RunWith` + `@Rule` | `@ExtendWith` |
| Display name | ❌ | `@DisplayName` |
| Nested tests | ❌ | `@Nested` |

```java
@Test
@DisplayName("Should throw when balance insufficient")
void shouldThrowOnInsufficientBalance() {
    assertThrows(InsufficientFundsException.class, 
        () -> account.withdraw(BigDecimal.valueOf(1000)));
}
```

---

### Q3. What is `@MockBean` vs `@Mock`? When to use each?

```java
// @Mock (Mockito) — plain mock, no Spring context
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository repo;           // Mockito creates a mock
    @InjectMocks OrderService service;    // Injects mocks into service
}

// @MockBean (Spring) — replaces bean in Spring ApplicationContext
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @MockBean OrderService service;       // Replaces real OrderService in Spring context
    @Autowired MockMvc mockMvc;
}
```

**Rule:** Use `@Mock` for unit tests (fast, no context). Use `@MockBean` only for slice/integration tests where you need the Spring context but want to mock a dependency.

---

### Q4. Explain `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` — when to use each.

```java
// @WebMvcTest — loads only the web layer (controllers, filters, advices)
@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean ShipmentService service;
}

// @DataJpaTest — loads only JPA layer (entities, repositories, in-memory DB)
@DataJpaTest
class ShipmentRepositoryTest {
    @Autowired ShipmentRepository repo;
    @Autowired TestEntityManager em;
}

// @SpringBootTest — loads FULL application context
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ShipmentIntegrationTest {
    @Autowired TestRestTemplate restTemplate;
}
```

| Annotation | Context | Use for |
|-----------|---------|---------|
| `@WebMvcTest` | Web slice only | Controller logic, validation, serialization |
| `@DataJpaTest` | JPA slice only | Repository queries, schema |
| `@SpringBootTest` | Full | Integration tests, full flow |

---

## Scenario-Based Questions

### Q5. At FedEx, write a complete test for the shipment tracking controller.

```java
@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ShipmentService service;

    @Test
    @DisplayName("GET /shipments/{id} returns shipment when found")
    void getShipment_found() throws Exception {
        ShipmentDTO dto = new ShipmentDTO("123456789012", "IN_TRANSIT", "Memphis");
        when(service.findByTracking("123456789012")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/shipments/123456789012")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackingNumber").value("123456789012"))
            .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        verify(service).findByTracking("123456789012");
    }

    @Test
    @DisplayName("GET /shipments/{id} returns 404 when not found")
    void getShipment_notFound() throws Exception {
        when(service.findByTracking(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/shipments/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /shipments validates request body")
    void createShipment_validation() throws Exception {
        String invalidJson = """
            { "senderName": "", "weight": -1 }
            """;

        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

---

### Q6. At NPCI, how do you test a repository with custom queries using TestContainers?

```java
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE) // Don't use H2, use real MySQL
class TransactionRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("npci_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired TransactionRepository repo;

    @Test
    void findByMerchantIdAndDateRange_returnsCorrectTransactions() {
        // Given
        repo.save(new Transaction("M001", BigDecimal.valueOf(500), LocalDate.of(2024, 1, 15)));
        repo.save(new Transaction("M001", BigDecimal.valueOf(300), LocalDate.of(2024, 2, 15)));
        repo.save(new Transaction("M002", BigDecimal.valueOf(800), LocalDate.of(2024, 1, 20)));

        // When
        List<Transaction> result = repo.findByMerchantIdAndDateBetween(
            "M001", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("500");
    }
}
```

---

### Q7. At Hatio, how do you test a service that depends on an external payment gateway?

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentGatewayClient gatewayClient;
    @Mock TransactionRepository repo;
    @InjectMocks PaymentService service;

    @Test
    void processPayment_success() {
        PaymentRequest req = new PaymentRequest("M001", BigDecimal.valueOf(100));
        when(gatewayClient.charge(any())).thenReturn(new GatewayResponse("SUCCESS", "txn_123"));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResult result = service.processPayment(req);

        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(gatewayClient).charge(argThat(r -> r.amount().equals(BigDecimal.valueOf(100))));
        verify(repo).save(argThat(t -> t.getStatus().equals("COMPLETED")));
    }

    @Test
    void processPayment_gatewayTimeout_retriesAndFails() {
        when(gatewayClient.charge(any())).thenThrow(new GatewayTimeoutException("Timeout"));

        assertThrows(PaymentProcessingException.class, () -> service.processPayment(req));

        verify(gatewayClient, times(3)).charge(any()); // Verify 3 retry attempts
    }
}
```

**For integration testing against a real API:** Use WireMock to stub HTTP responses:
```java
@WireMockTest(httpPort = 8089)
class PaymentGatewayIntegrationTest {
    @Test
    void chargeReturnsSuccess(WireMockRuntimeInfo wm) {
        stubFor(post("/api/charge")
            .willReturn(okJson("{\"status\":\"SUCCESS\",\"txnId\":\"123\"}")));
        // test against localhost:8089
    }
}
```

---

## Coding Challenges

### Challenge 1: Service Layer Test Suite
**File:** `solutions/ServiceTestSuite.java`  
Write a complete test class for OrderService:
1. Test happy path for createOrder, cancelOrder, getOrderById
2. Test exception cases (not found, already cancelled, validation failure)
3. Use `@Mock`, `@InjectMocks`, `@Captor`
4. Verify correct repository calls with argument captors
5. Use `@ParameterizedTest` for testing multiple scenarios

### Challenge 2: Controller Integration Test
**File:** `solutions/ControllerIntegrationTest.java`  
Write `@WebMvcTest` tests for a REST controller:
1. Test all CRUD endpoints with proper status codes
2. Test request validation (bad input → 400)
3. Test security (unauthorized → 401, forbidden → 403)
4. Test content negotiation (JSON response format)
5. Use `@MockBean` for service layer

---

## Gotchas & Edge Cases

### Q8. Why does my `@Transactional` test not persist data to the database?

By default, `@DataJpaTest` and `@SpringBootTest` with `@Transactional` **roll back after each test**. This is intentional — keeps tests isolated.

If you need to verify data in another transaction:
```java
@Test
void testDataPersistence() {
    repo.save(entity);
    repo.flush();           // Force SQL execution
    em.clear();             // Clear persistence context (1st level cache)
    
    Entity found = repo.findById(entity.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("test"); // Fetched from DB, not cache
}
```

---

### Q9. How do you test async methods in Spring Boot?

```java
@Test
void testAsyncMethod() {
    CompletableFuture<Result> future = service.processAsync(data);
    
    Result result = future.get(5, TimeUnit.SECONDS); // Block with timeout
    assertThat(result.status()).isEqualTo("DONE");
}

// Or use Awaitility
@Test
void testAsyncWithAwaitility() {
    service.processAsync(data);
    
    await().atMost(5, SECONDS)
        .untilAsserted(() -> {
            assertThat(repo.findById(id).get().getStatus()).isEqualTo("COMPLETED");
        });
}
```
