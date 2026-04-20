# GraphQL — Interview Q&A

> 15 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What is GraphQL? How does it differ from REST?

| Aspect | REST | GraphQL |
|--------|------|---------|
| Endpoints | Multiple (`/users`, `/orders`) | Single (`/graphql`) |
| Data fetching | Fixed response shape | Client specifies exact fields |
| Over-fetching | Returns all fields | Returns only requested fields |
| Under-fetching | Multiple round trips needed | Single query for nested data |
| Versioning | `/api/v1/`, `/api/v2/` | Schema evolution, no versioning |
| Caching | HTTP caching (easy) | More complex (POST requests) |
| Type system | OpenAPI/Swagger (optional) | Built-in schema (mandatory) |

```graphql
# REST: GET /api/users/123  → returns ALL user fields
# REST: GET /api/users/123/orders  → second request for orders

# GraphQL: single request, exact fields
query {
  user(id: "123") {
    name
    email
    orders(last: 5) {
      id
      total
      status
    }
  }
}
```

---

### Q2. Explain GraphQL schema, types, queries, and mutations.

```graphql
# Schema definition
type Query {
  shipment(trackingNumber: String!): Shipment
  shipments(status: ShipmentStatus, page: Int): ShipmentConnection!
}

type Mutation {
  createShipment(input: CreateShipmentInput!): Shipment!
  updateShipmentStatus(id: ID!, status: ShipmentStatus!): Shipment!
}

type Subscription {
  shipmentStatusChanged(trackingNumber: String!): Shipment!
}

# Custom types
type Shipment {
  id: ID!
  trackingNumber: String!
  status: ShipmentStatus!
  origin: Address!
  destination: Address!
  events: [TrackingEvent!]!
  estimatedDelivery: String
}

enum ShipmentStatus {
  CREATED
  PICKED_UP
  IN_TRANSIT
  OUT_FOR_DELIVERY
  DELIVERED
}

input CreateShipmentInput {
  senderName: String!
  origin: AddressInput!
  destination: AddressInput!
  weight: Float!
  serviceType: ServiceType!
}
```

**Key types:**
- `Query` — read operations (like GET)
- `Mutation` — write operations (like POST/PUT/DELETE)
- `Subscription` — real-time updates (WebSocket)
- `!` means non-nullable

---

### Q3. How do you implement GraphQL with Spring Boot?

```java
// Spring for GraphQL (official Spring project, Boot 3.x)
@Controller
public class ShipmentController {

    @QueryMapping
    public Shipment shipment(@Argument String trackingNumber) {
        return shipmentService.findByTracking(trackingNumber);
    }

    @MutationMapping
    public Shipment createShipment(@Argument CreateShipmentInput input) {
        return shipmentService.create(input);
    }

    // Batch load related data (solves N+1)
    @BatchMapping
    public Map<Shipment, List<TrackingEvent>> events(List<Shipment> shipments) {
        return shipmentService.getEventsForShipments(shipments);
    }
}
```

Schema file at `src/main/resources/graphql/schema.graphqls` — auto-detected by Spring.

---

### Q4. What is the N+1 problem in GraphQL? How do you solve it?

**Problem:**
```graphql
query {
  shipments(first: 100) {
    trackingNumber
    events { description }  # ← triggers 100 separate DB queries!
  }
}
```

1 query for shipments + 100 queries for events = 101 queries (N+1).

**Solution: DataLoader (batching)**
```java
@BatchMapping
public Map<Shipment, List<TrackingEvent>> events(List<Shipment> shipments) {
    // Single query: SELECT * FROM events WHERE shipment_id IN (1, 2, ..., 100)
    List<Long> ids = shipments.stream().map(Shipment::getId).toList();
    Map<Long, List<TrackingEvent>> eventMap = eventRepo.findByShipmentIdIn(ids)
        .stream()
        .collect(Collectors.groupingBy(TrackingEvent::getShipmentId));
    return shipments.stream()
        .collect(Collectors.toMap(s -> s, s -> eventMap.getOrDefault(s.getId(), List.of())));
}
```

**2 queries total** regardless of how many shipments.

---

## Scenario-Based Questions

### Q5. At FedEx, you're designing a GraphQL API for the shipment tracking portal. React frontend needs different data on different pages. How would you structure it?

```graphql
# List page — minimal fields
query ShipmentList {
  shipments(status: IN_TRANSIT, page: 0, size: 20) {
    edges {
      node {
        trackingNumber
        status
        estimatedDelivery
      }
    }
    pageInfo {
      hasNextPage
      totalCount
    }
  }
}

# Detail page — full data in one call
query ShipmentDetail($tracking: String!) {
  shipment(trackingNumber: $tracking) {
    trackingNumber
    status
    origin { city, state, zipCode }
    destination { city, state, zipCode }
    events {
      timestamp
      location
      description
      scanType
    }
    estimatedDelivery
    weight
    serviceType
  }
}
```

**Benefit:** Same API serves both mobile (minimal data) and desktop (full data) without API changes.

---

### Q6. At Hatio, the React frontend makes 5 REST calls to load the dashboard. How would GraphQL help?

```
# Current REST:
GET /api/users/me              → user profile
GET /api/accounts/summary      → account balances
GET /api/transactions/recent   → last 10 txns
GET /api/notifications/unread  → notification count
GET /api/offers/active         → promotional offers

# GraphQL: ONE request
query Dashboard {
  me { name, avatar }
  accountSummary { balance, currency }
  recentTransactions(limit: 10) { id, amount, merchant, date }
  unreadNotifications { count }
  activeOffers { title, description, expiresAt }
}
```

**5 round trips → 1.** Massive latency improvement, especially on mobile.

---

### Q7. How do you secure a GraphQL API?

**1. Authentication:** Same as REST — JWT in Authorization header.

**2. Authorization:** Field-level with directives
```graphql
type User {
  name: String
  email: String @auth(requires: SELF)       # Only the user can see their email
  balance: Float @auth(requires: ADMIN)     # Only admins
}
```

**3. Query depth limiting** (prevent abuse):
```java
@Bean
public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    return builder -> builder.queryDepthLimit(5); // Max 5 levels deep
}
```

**4. Query complexity analysis:** Assign costs to fields, reject expensive queries.

**5. Disable introspection in production:**
```yaml
spring:
  graphql:
    schema:
      introspection:
        enabled: false  # Don't expose schema to attackers
```

---

## Coding Challenges

### Challenge 1: GraphQL Schema Design
**File:** `solutions/GraphQLSchemaDesign.md`  
Design a complete GraphQL schema for an e-commerce platform:
1. Types: Product, Order, User, Review, Cart
2. Queries: search, filter, pagination (cursor-based)
3. Mutations: addToCart, placeOrder, writeReview
4. Subscriptions: orderStatusChanged
5. Input types and enums

### Challenge 2: Resolver Implementation
**File:** `solutions/GraphQLResolver.java`  
Implement Spring GraphQL resolvers:
1. Query resolver with filtering and pagination
2. Mutation resolver with input validation
3. BatchMapping to solve N+1 (orders → items)
4. Error handling with GraphQL error types

---

## Gotchas & Edge Cases

### Q8. When should you NOT use GraphQL?

- **Simple CRUD APIs** — REST is simpler and more cacheable
- **File uploads** — GraphQL doesn't handle binary data well (use REST for uploads)
- **Real-time high throughput** — WebSocket subscriptions have overhead vs raw WebSocket
- **Strict API versioning required** — GraphQL schema evolution is flexible but can break clients
- **Caching** — REST uses HTTP caching natively; GraphQL POST requests bypass CDN caches

---

### Q9. REST and GraphQL can coexist — how?

**Common pattern:** REST for simple CRUD and external APIs, GraphQL for frontend-facing aggregation.

```
External Partners → REST API (/api/v1/...)
React Frontend   → GraphQL (/graphql)
                    ↓
              GraphQL resolvers call same service layer
                    ↓
              Shared business logic
```

**At FedEx:** External tracking API is REST (simple, cacheable). Internal portal uses GraphQL (flexible, fewer round trips).
