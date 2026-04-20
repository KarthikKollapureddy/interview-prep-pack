# Communication Protocols — Interview Q&A

> Concepts from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
> Covers: HTTP, TCP, UDP, RPC, REST, GraphQL, gRPC, WebSockets
> **Priority: P1** — Protocol choices are discussed in every HLD deep-dive

---

## Q1. TCP vs UDP — When to use which?

```
┌─────────────────────┬──────────────────────┬──────────────────────┐
│ Feature             │ TCP                  │ UDP                  │
├─────────────────────┼──────────────────────┼──────────────────────┤
│ Connection          │ Connection-oriented  │ Connectionless       │
│                     │ (3-way handshake)    │ (fire and forget)    │
│                     │                      │                      │
│ Reliability         │ Guaranteed delivery  │ No guarantee         │
│                     │ (ACK, retransmit)    │ (may lose packets)   │
│                     │                      │                      │
│ Order               │ Ordered delivery     │ No ordering          │
│                     │ (sequence numbers)   │                      │
│                     │                      │                      │
│ Flow Control        │ Yes (sliding window) │ No                   │
│                     │                      │                      │
│ Congestion Control  │ Yes                  │ No                   │
│                     │                      │                      │
│ Speed               │ Slower (overhead)    │ Faster (minimal      │
│                     │                      │   overhead)          │
│                     │                      │                      │
│ Header Size         │ 20-60 bytes          │ 8 bytes              │
│                     │                      │                      │
│ Use Cases           │ Web (HTTP), email,   │ VoIP, video stream,  │
│                     │ file transfer, DB    │ gaming, DNS lookups  │
└─────────────────────┴──────────────────────┴──────────────────────┘

Use TCP when:
  ✓ Need all data to arrive intact (file transfer, web pages)
  ✓ Need ordered delivery
  ✓ Want automatic network throughput optimization

Use UDP when:
  ✓ Need lowest latency
  ✓ Late data is worse than lost data (live video, gaming)
  ✓ Want to implement your own error correction
  ✓ Broadcasting (DHCP, service discovery)

TCP Connection Establishment (3-Way Handshake):
  Client → SYN → Server
  Client ← SYN-ACK ← Server
  Client → ACK → Server
  Connection established, data flows

TCP High Memory Usage:
  - Web servers keep many TCP connections open
  - Each connection uses memory (buffers, state)
  - Connection pooling helps reduce overhead
```

---

## Q2. HTTP — Methods, Status Codes, and Versions.

```
HTTP = Application layer protocol for client-server communication
Built on top of TCP. Stateless, self-contained.

HTTP Methods:
┌────────┬────────────────────────────┬────────────┬──────────┐
│ Method │ Purpose                    │ Idempotent │ Cacheable│
├────────┼────────────────────────────┼────────────┼──────────┤
│ GET    │ Read a resource            │ Yes        │ Yes      │
│ POST   │ Create resource / trigger  │ No         │ No*      │
│ PUT    │ Create or REPLACE resource │ Yes        │ No       │
│ PATCH  │ PARTIALLY update resource  │ No         │ No*      │
│ DELETE │ Delete a resource          │ Yes        │ No       │
│ HEAD   │ GET without body (headers) │ Yes        │ Yes      │
│ OPTIONS│ Supported methods (CORS)   │ Yes        │ No       │
└────────┴────────────────────────────┴────────────┴──────────┘
* Cacheable if response includes freshness info

Status Codes:
  1xx — Informational (100 Continue)
  2xx — Success (200 OK, 201 Created, 204 No Content)
  3xx — Redirect (301 Permanent, 302 Temporary, 304 Not Modified)
  4xx — Client Error (400 Bad Request, 401 Unauthorized,
                      403 Forbidden, 404 Not Found, 429 Too Many)
  5xx — Server Error (500 Internal, 502 Bad Gateway,
                      503 Unavailable, 504 Gateway Timeout)

HTTP Versions:
  HTTP/1.1: one request per TCP connection (pipelining rarely used)
  HTTP/2: multiplexed streams over single connection, header compression
  HTTP/3: built on QUIC (UDP-based), faster connection, no head-of-line blocking
```

---

## Q3. REST API — Principles and Design.

```
REST = Representational State Transfer
  - Client/server architecture
  - Stateless (each request contains all needed info)
  - Cacheable
  - Uniform interface

4 Qualities of RESTful Interface:
  1. Identify resources via URIs: /users/123
  2. Manipulate via representations: JSON body
  3. Self-descriptive messages: proper status codes
  4. HATEOAS: hypermedia links in responses

REST API Design Best Practices:
  ┌────────────────────────────────────────────────────┐
  │ Nouns for resources: /users, /orders, /products    │
  │ Verbs from HTTP methods: GET, POST, PUT, DELETE    │
  │ Plural nouns: /users NOT /user                     │
  │ Nested resources: /users/123/orders                │
  │ Filtering: /users?status=active&role=admin         │
  │ Pagination: /users?page=2&size=20                  │
  │ Versioning: /api/v1/users or Accept header         │
  │ Use proper status codes (not always 200)           │
  └────────────────────────────────────────────────────┘

Example REST API:
  GET    /api/v1/users           → List all users
  GET    /api/v1/users/123       → Get user 123
  POST   /api/v1/users           → Create new user
  PUT    /api/v1/users/123       → Replace user 123
  PATCH  /api/v1/users/123       → Update user 123 partially
  DELETE /api/v1/users/123       → Delete user 123
  GET    /api/v1/users/123/orders → Get user 123's orders

Advantages:
  ✓ Simple, well-understood
  ✓ Stateless → great for horizontal scaling
  ✓ Cacheable (GET requests)
  ✓ Uniform interface
  ✓ Great for public APIs

Disadvantages:
  ✗ Over-fetching: GET /users returns ALL fields
  ✗ Under-fetching: need multiple requests for nested data
  ✗ Limited verbs: some operations don't map to CRUD
  ✗ Versioning can be messy
  ✗ Payload bloat for mobile clients
```

---

## Q4. RPC (Remote Procedure Call) — How it differs from REST.

```
RPC = call a function on a remote server as if it were local

  Client → stub → marshal args → network → server → execute → return

Popular RPC Frameworks:
  - gRPC (Google): Protocol Buffers, HTTP/2, streaming
  - Thrift (Facebook): multi-language, binary protocol
  - Avro (Apache): schema-based, Hadoop ecosystem

RPC Flow:
  1. Client calls stub function (looks local)
  2. Stub marshals (serializes) arguments
  3. Client sends message over network
  4. Server receives and unmarshals
  5. Server executes the actual function
  6. Response goes back the same way

Example RPC calls:
  getUser(userId=123)
  createOrder(userId=123, items=[...])
  processPayment(orderId=456, amount=99.99)

vs REST:
  GET /users/123
  POST /orders  {userId: 123, items: [...]}
  POST /payments {orderId: 456, amount: 99.99}
```

### RPC vs REST Comparison
```
┌─────────────────┬──────────────────────┬──────────────────────┐
│ Aspect          │ RPC                  │ REST                 │
├─────────────────┼──────────────────────┼──────────────────────┤
│ Focus           │ Exposing BEHAVIORS   │ Exposing DATA        │
│                 │ (actions/functions)  │ (resources/nouns)    │
│                 │                      │                      │
│ Coupling        │ Tighter coupling     │ Loose coupling       │
│                 │ (client knows API)   │ (uniform interface)  │
│                 │                      │                      │
│ Protocol        │ Binary (Protobuf)    │ Text (JSON/XML)      │
│                 │                      │                      │
│ Performance     │ Faster (binary,      │ Slower (text,        │
│                 │  less overhead)      │  more overhead)      │
│                 │                      │                      │
│ Caching         │ Hard to cache        │ Easy (HTTP caching)  │
│                 │                      │                      │
│ Use Case        │ Internal services    │ Public APIs          │
│                 │ (microservices)      │ (external clients)   │
│                 │                      │                      │
│ Discovery       │ Need service registry│ URL-based            │
│                 │                      │                      │
│ Streaming       │ Bidirectional (gRPC) │ Limited (SSE, WS)    │
└─────────────────┴──────────────────────┴──────────────────────┘

Interview Tip: "For internal microservice communication, I'd use gRPC
  for its performance and strong typing. For external/public APIs,
  REST with JSON for simplicity and broad client compatibility."
```

---

## Q5. gRPC — When and why to use it.

```
gRPC = Google's high-performance RPC framework

Built on:
  - HTTP/2: multiplexed streams, header compression
  - Protocol Buffers: binary serialization (10x smaller than JSON)
  - Strong typing: .proto schema files

// Example .proto definition:
service UserService {
  rpc GetUser (GetUserRequest) returns (User);
  rpc ListUsers (ListUsersRequest) returns (stream User);
}

message GetUserRequest {
  string user_id = 1;
}

message User {
  string user_id = 1;
  string name = 2;
  string email = 3;
}

Communication Patterns:
  1. Unary:           Client sends 1, Server returns 1
  2. Server Streaming: Client sends 1, Server returns stream
  3. Client Streaming: Client sends stream, Server returns 1
  4. Bidirectional:    Both send streams simultaneously

Advantages:
  ✓ 10x faster than REST+JSON (binary serialization)
  ✓ Strong typing (contract-first with .proto files)
  ✓ Code generation for 11+ languages
  ✓ Bidirectional streaming
  ✓ HTTP/2 multiplexing
  ✓ Built-in deadline/timeout propagation

Disadvantages:
  ✗ Not human-readable (binary format)
  ✗ Browser support limited (need grpc-web proxy)
  ✗ Debugging harder than REST
  ✗ Less tooling than REST (Postman, curl)
  ✗ Schema changes need coordination

Use Cases:
  ✓ Microservice-to-microservice communication
  ✓ Real-time streaming (live updates)
  ✓ Mobile client to backend (lower bandwidth)
  ✓ Polyglot environments (multi-language)
```

---

## Q6. GraphQL — When REST isn't enough.

```
GraphQL = query language for APIs (developed by Facebook)

Problem with REST:
  - Over-fetching: GET /users/123 returns ALL fields
  - Under-fetching: need GET /users/123 + GET /users/123/orders
  - Multiple round trips for complex views

GraphQL Solution:
  ONE endpoint, client specifies exactly what it needs

  // Request
  query {
    user(id: "123") {
      name
      email
      orders(last: 5) {
        id
        total
        items {
          productName
          quantity
        }
      }
    }
  }

  // Response — exactly what was asked, nothing more
  {
    "data": {
      "user": {
        "name": "Karthik",
        "email": "k@email.com",
        "orders": [...]
      }
    }
  }

Advantages:
  ✓ No over-fetching or under-fetching
  ✓ Single request for complex data
  ✓ Strong typing with schema
  ✓ Self-documenting (introspection)
  ✓ Great for mobile (reduce bandwidth)

Disadvantages:
  ✗ Complex queries can be expensive (N+1 problem)
  ✗ Caching is harder (single endpoint)
  ✗ Rate limiting is harder (query complexity varies)
  ✗ Learning curve
  ✗ File uploads not native

When to use:
  ✓ Mobile apps (bandwidth-sensitive)
  ✓ Dashboards with varied data needs
  ✓ BFF (Backend-For-Frontend) layer
  ✗ Simple CRUD APIs (REST is simpler)
  ✗ File-heavy APIs
```

---

## Q7. WebSockets — Real-time bidirectional communication.

```
WebSocket = full-duplex communication over single TCP connection

HTTP (half-duplex):
  Client → Request → Server → Response
  Client must initiate every exchange

WebSocket (full-duplex):
  Client ←→ Server (both can send anytime)

  1. HTTP Upgrade handshake (once)
  2. Connection stays open
  3. Both sides send messages freely
  4. Connection closed when done

Use Cases:
  ✓ Chat applications (WhatsApp, Slack)
  ✓ Live notifications
  ✓ Real-time dashboards (stock prices, sports scores)
  ✓ Collaborative editing (Google Docs)
  ✓ Online gaming
  ✓ Live location tracking

Alternative: Server-Sent Events (SSE)
  - Server → Client only (one-way)
  - Built on HTTP (simpler, works through proxies)
  - Auto-reconnect built in
  - Good for: live feeds, notifications, stock tickers

Comparison:
  ┌───────────────┬──────────────┬──────────────┬──────────────┐
  │               │ WebSocket    │ SSE          │ Long Polling │
  ├───────────────┼──────────────┼──────────────┼──────────────┤
  │ Direction     │ Bidirectional│ Server→Client│ Client→Server│
  │ Protocol      │ WS           │ HTTP         │ HTTP         │
  │ Connection    │ Persistent   │ Persistent   │ Repeated     │
  │ Complexity    │ High         │ Low          │ Low          │
  │ Browser       │ Excellent    │ Good         │ Universal    │
  │ Scale         │ Harder       │ Easier       │ Easiest      │
  └───────────────┴──────────────┴──────────────┴──────────────┘
```

---

## Q8. API Gateway — The entry point for all clients.

```
API Gateway = single entry point for all API requests

  ┌───────────┐
  │  Mobile   │──┐
  └───────────┘  │    ┌─────────────┐    ┌──────────────┐
  ┌───────────┐  ├───→│ API Gateway │───→│ User Service │
  │   Web     │──┤    │             │    └──────────────┘
  └───────────┘  │    │ - Auth      │    ┌──────────────┐
  ┌───────────┐  │    │ - Rate Limit│───→│ Order Service│
  │  Partner  │──┘    │ - Routing   │    └──────────────┘
  └───────────┘       │ - Transform │    ┌──────────────┐
                      │ - Logging   │───→│Product Service│
                      └─────────────┘    └──────────────┘

Responsibilities:
  ✓ Authentication & Authorization (JWT validation)
  ✓ Rate Limiting (protect backend from abuse)
  ✓ Request Routing (path → service mapping)
  ✓ Load Balancing
  ✓ SSL Termination
  ✓ Request/Response Transformation
  ✓ Caching
  ✓ Logging & Monitoring
  ✓ Circuit Breaking

Technologies:
  - Kong, AWS API Gateway, Zuul (Netflix)
  - Spring Cloud Gateway, NGINX, Envoy

Patterns:
  - BFF (Backend for Frontend):
    Separate gateway per client type
    /mobile/* → Mobile BFF → lightweight responses
    /web/*    → Web BFF    → richer responses
```

---

## Q9. Communication Protocol Decision Matrix.

```
Choosing the right protocol for your system:

┌──────────────────────┬──────────────────────────────────────┐
│ Scenario             │ Best Protocol                        │
├──────────────────────┼──────────────────────────────────────┤
│ Public API           │ REST + JSON (universal support)      │
│ Internal services    │ gRPC (performance + strong typing)   │
│ Mobile client        │ GraphQL or gRPC (bandwidth savings)  │
│ Real-time chat       │ WebSocket (bidirectional)            │
│ Live notifications   │ SSE or WebSocket                     │
│ Event streaming      │ Kafka (async, high throughput)       │
│ File transfer        │ REST multipart or dedicated service  │
│ IoT / sensors        │ MQTT or UDP                          │
│ Browser ↔ Server     │ REST or GraphQL                      │
│ Batch processing     │ Message Queue (SQS, RabbitMQ)        │
└──────────────────────┴──────────────────────────────────────┘

In a typical microservices system:
  External → REST (API Gateway) → Internal (gRPC between services)
  Events → Kafka (async communication)
  Real-time → WebSocket (through API Gateway)
```

---

*Source: Concepts synthesized from [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer), gRPC docs, and [karanpratapsingh/system-design](https://github.com/karanpratapsingh/system-design)*
