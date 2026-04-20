# Networking & Web Fundamentals — Interview Q&A

> 12 questions covering HTTP, DNS, TCP/UDP, SSL/TLS, browser rendering, and web protocols  
> Priority: **P1** — FedEx explicitly tests network protocols; product companies ask 2-3 questions

---

## Conceptual Questions

### Q1. What happens when you type a URL in the browser and press Enter?

**Answer (step-by-step — the #1 most asked question):**

```
1. URL Parsing
   Browser parses: protocol (https), domain (www.example.com), path (/api), query (?q=test)

2. DNS Resolution
   Browser cache → OS cache → Router cache → ISP DNS → Root → TLD → Authoritative
   Result: IP address (e.g., 93.184.216.34)

3. TCP Connection (3-Way Handshake)
   Client → SYN → Server
   Client ← SYN-ACK ← Server
   Client → ACK → Server
   Connection established.

4. TLS/SSL Handshake (for HTTPS)
   Client Hello (supported ciphers) → Server Hello (chosen cipher + certificate)
   → Certificate verification → Key exchange → Symmetric session key created

5. HTTP Request
   GET /api?q=test HTTP/2
   Host: www.example.com
   Accept: text/html

6. Server Processing
   Load balancer → Web server → Application → Database → Response

7. HTTP Response
   HTTP/2 200 OK
   Content-Type: text/html
   <html>...</html>

8. Browser Rendering (Critical Rendering Path)
   HTML → DOM Tree
   CSS  → CSSOM Tree
   DOM + CSSOM → Render Tree → Layout → Paint → Composite

9. Connection Close (or kept alive via Keep-Alive / HTTP/2 multiplexing)
```

---

### Q2. HTTP vs HTTPS — What is the difference?

**Answer:**

| Feature | HTTP | HTTPS |
|---------|------|-------|
| Port | 80 | 443 |
| Encryption | None (plaintext) | TLS/SSL (encrypted) |
| Certificate | Not required | SSL/TLS certificate required |
| Performance | Slightly faster (no handshake) | Minor overhead for TLS handshake |
| SEO | Lower ranking | Google boosts HTTPS sites |
| Use case | Never for production | Always for production |

**How HTTPS works:**
1. **Certificate Authority (CA)** issues an SSL certificate to the server
2. Browser verifies the certificate against trusted CAs
3. **Asymmetric encryption** (RSA/ECDSA) used for key exchange
4. **Symmetric encryption** (AES-256) used for actual data transfer (faster)

**Why symmetric for data?** Asymmetric is 1000x slower — only used to securely exchange the symmetric session key.

---

### Q3. TCP vs UDP — When do you use each?

**Answer:**

| Feature | TCP | UDP |
|---------|-----|-----|
| Connection | Connection-oriented (handshake) | Connectionless |
| Reliability | Guaranteed delivery (ACK, retransmit) | Best-effort (no guarantee) |
| Ordering | In-order delivery | No ordering |
| Speed | Slower (overhead) | Faster (minimal overhead) |
| Header size | 20 bytes | 8 bytes |
| Flow control | Yes (windowing) | No |

**Use cases:**

| Protocol | TCP | UDP |
|----------|-----|-----|
| HTTP/HTTPS | ✅ | (HTTP/3 uses QUIC over UDP) |
| Email (SMTP) | ✅ | |
| File transfer (FTP) | ✅ | |
| Video streaming | | ✅ (acceptable packet loss) |
| Online gaming | | ✅ (low latency) |
| DNS queries | | ✅ (small, fast) |
| VoIP | | ✅ |

**HTTP/3 (QUIC)** — uses UDP with built-in reliability, solving TCP's head-of-line blocking. Faster connection setup (0-RTT).

---

### Q4. Explain DNS Resolution process.

**Answer:**

```
Browser: "What is the IP for www.example.com?"

Step 1: Browser DNS cache (check local cache)
Step 2: OS DNS cache (check /etc/hosts, OS resolver)
Step 3: Router DNS cache
Step 4: ISP DNS Resolver (Recursive resolver)
Step 5: Root DNS Server → "Ask .com TLD server"
Step 6: TLD Server (.com) → "Ask authoritative server for example.com"
Step 7: Authoritative DNS → "93.184.216.34" (with TTL)
Step 8: Result cached at each level for TTL duration
```

**DNS Record Types:**
| Type | Purpose | Example |
|------|---------|---------|
| A | Domain → IPv4 | example.com → 93.184.216.34 |
| AAAA | Domain → IPv6 | example.com → 2606:2800:220:1:... |
| CNAME | Alias to another domain | www.example.com → example.com |
| MX | Mail server | example.com → mail.example.com |
| NS | Nameserver for domain | example.com → ns1.dns.com |
| TXT | Arbitrary text (SPF, DKIM) | Used for email verification |

---

### Q5. Explain HTTP methods, status codes, and headers.

**Answer:**

**HTTP Methods (REST):**
| Method | Safe? | Idempotent? | Purpose |
|--------|-------|------------|---------|
| GET | Yes | Yes | Retrieve resource |
| POST | No | No | Create resource |
| PUT | No | Yes | Replace resource entirely |
| PATCH | No | No* | Partial update |
| DELETE | No | Yes | Delete resource |
| HEAD | Yes | Yes | Like GET but no body |
| OPTIONS | Yes | Yes | CORS preflight, discover methods |

**Status Codes:**
| Range | Category | Common Codes |
|-------|----------|-------------|
| 1xx | Informational | 101 Switching Protocols (WebSocket) |
| 2xx | Success | 200 OK, 201 Created, 204 No Content |
| 3xx | Redirection | 301 Moved Permanently, 302 Found, 304 Not Modified |
| 4xx | Client Error | 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 429 Too Many Requests |
| 5xx | Server Error | 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable, 504 Gateway Timeout |

**Important Headers:**
| Header | Purpose |
|--------|---------|
| `Authorization` | Bearer token / Basic auth |
| `Content-Type` | application/json, multipart/form-data |
| `Cache-Control` | no-cache, max-age=3600 |
| `ETag` | Conditional caching (304 Not Modified) |
| `X-Request-ID` | Distributed tracing |
| `CORS headers` | Access-Control-Allow-Origin, etc. |

---

### Q6. HTTP/1.1 vs HTTP/2 vs HTTP/3 — Key differences?

**Answer:**

| Feature | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---------|----------|--------|--------|
| Transport | TCP | TCP | QUIC (over UDP) |
| Multiplexing | No (1 req per connection) | Yes (multiple streams) | Yes |
| Header compression | No | HPACK | QPACK |
| Server Push | No | Yes | Yes |
| Head-of-line blocking | Yes (TCP) | Partially (TCP level) | No (per-stream) |
| Connection setup | 1-3 RTT | 1-3 RTT | 0-1 RTT |

**HTTP/2 key improvement:** Multiple requests/responses over a single TCP connection (multiplexing), eliminating the need for domain sharding and sprite sheets.

**HTTP/3 key improvement:** QUIC over UDP eliminates TCP head-of-line blocking. If one stream's packet is lost, other streams are unaffected.

---

### Q7. What is CORS and why does it exist?

**Answer:**
CORS (Cross-Origin Resource Sharing) is a security mechanism that restricts web pages from making requests to a different domain than the one serving the page.

**Same-Origin Policy:** Browser blocks requests where protocol + domain + port differ.
```
https://app.com → https://api.com   ❌ Different domain
https://app.com → https://app.com:8080  ❌ Different port
https://app.com → http://app.com    ❌ Different protocol
```

**How CORS works (preflight):**
```
1. Browser sends OPTIONS request (preflight):
   Origin: https://app.com
   Access-Control-Request-Method: POST
   Access-Control-Request-Headers: Content-Type

2. Server responds:
   Access-Control-Allow-Origin: https://app.com
   Access-Control-Allow-Methods: GET, POST
   Access-Control-Allow-Headers: Content-Type
   Access-Control-Max-Age: 86400

3. If allowed, browser sends the actual request
```

**Spring Boot CORS config:**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://app.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## Scenario / Interview Deep-Dive Questions

### Q8. REST vs GraphQL vs gRPC — When to use which?

**Answer:**

| Aspect | REST | GraphQL | gRPC |
|--------|------|---------|------|
| Protocol | HTTP/JSON | HTTP/JSON | HTTP/2 + Protobuf |
| Data fetching | Fixed endpoints | Client specifies fields | Schema-based |
| Over/under fetching | Common problem | Solved | Solved |
| Caching | Easy (HTTP caching) | Complex | Complex |
| File upload | Easy | Complex | Supported |
| Real-time | Polling/WebSocket | Subscriptions | Bidirectional streaming |
| Best for | Public APIs, CRUD | Mobile apps, complex UIs | Internal microservices |

**Decision guide:**
- **REST:** Default choice for external APIs, simple CRUD
- **GraphQL:** When frontend needs flexible queries, mobile apps with bandwidth concerns
- **gRPC:** Service-to-service communication where performance matters

---

### Q9. What are WebSockets? How do they differ from HTTP?

**Answer:**

| Feature | HTTP | WebSocket |
|---------|------|-----------|
| Connection | Request-Response | Full-duplex persistent |
| Direction | Client → Server | Both directions simultaneously |
| Overhead | Headers per request | Single handshake, then lightweight frames |
| Protocol | http:// / https:// | ws:// / wss:// |

**WebSocket handshake:**
```
Client → HTTP Upgrade Request:
  GET /chat HTTP/1.1
  Connection: Upgrade
  Upgrade: websocket

Server → 101 Switching Protocols:
  Connection: Upgrade
  Upgrade: websocket

→ Now both can send messages at any time
```

**Use cases:** Chat, live notifications, real-time dashboards, online gaming, stock tickers.

**Alternatives:**
- **SSE (Server-Sent Events)** — one-way server → client (simpler, auto-reconnect)
- **Long Polling** — client holds connection until server has data
- **HTTP/2 Server Push** — server pushes resources proactively

---

### Q10. Explain the Browser Rendering Pipeline (Critical Rendering Path).

**Answer:**

```
HTML ──parse──► DOM Tree
                         ├──► Render Tree ──► Layout ──► Paint ──► Composite
CSS  ──parse──► CSSOM Tree     (visible       (positions   (pixels)   (layers)
                                elements)      & sizes)

JS can modify DOM/CSSOM at any step → triggers reflow/repaint
```

**Steps:**
1. **DOM Construction** — Parse HTML into DOM tree
2. **CSSOM Construction** — Parse CSS into CSSOM tree
3. **Render Tree** — Combine DOM + CSSOM (only visible elements)
4. **Layout (Reflow)** — Calculate positions and sizes
5. **Paint** — Fill pixels (colors, borders, shadows)
6. **Composite** — Combine layers for final display

**Performance tips:**
- CSS in `<head>` (render-blocking) — load early
- JS at end of `<body>` or `defer`/`async` attribute
- Avoid layout thrashing (reading + writing DOM in loops)
- Use `transform` and `opacity` for animations (compositing only, no reflow)

---

### Q11. What is a CDN and how does it work?

**Answer:**
A CDN (Content Delivery Network) is a distributed network of servers that serves content from the server **geographically closest** to the user.

```
Without CDN:
  User (India) ──────────────────► Origin Server (US)
                    300ms RTT

With CDN:
  User (India) ──► CDN Edge (Mumbai) ──► Origin Server (US)
                    20ms RTT              (only if cache miss)
```

**How it works:**
1. User requests `https://cdn.example.com/image.jpg`
2. DNS resolves to nearest CDN edge server
3. **Cache hit** → serve directly (fast)
4. **Cache miss** → fetch from origin, cache it, serve to user

**What to cache on CDN:** Static assets (images, CSS, JS, fonts), API responses (with proper Cache-Control), video chunks.

**Popular CDNs:** CloudFront (AWS), Cloudflare, Akamai, Fastly.

---

### Q12. Explain Load Balancing strategies.

**Answer:**

**Layer 4 (Transport)** vs **Layer 7 (Application):**
- L4: Routes based on IP/port (faster, less intelligent)
- L7: Routes based on HTTP headers, URL path, cookies (smarter)

**Algorithms:**
| Algorithm | How It Works | Best For |
|-----------|-------------|----------|
| **Round Robin** | Rotate sequentially | Equal-capacity servers |
| **Weighted Round Robin** | More traffic to higher-weight servers | Mixed-capacity servers |
| **Least Connections** | Route to server with fewest active connections | Variable request duration |
| **IP Hash** | Hash client IP to consistent server | Session stickiness |
| **Random** | Random server selection | Simple, stateless |

**Health Checks:** Load balancer periodically pings servers. Unhealthy servers are removed from rotation.

**Popular tools:** Nginx, HAProxy, AWS ALB/NLB, Envoy.

---

## Quick Reference

### Networking Interview Cheat Sheet

| Topic | Key Point |
|-------|-----------|
| URL → Page | DNS → TCP 3-way → TLS → HTTP → Render |
| HTTPS | Asymmetric for key exchange, Symmetric (AES) for data |
| TCP vs UDP | TCP = reliable/ordered, UDP = fast/best-effort |
| HTTP/2 | Multiplexing, header compression, server push |
| HTTP/3 | QUIC over UDP, no head-of-line blocking |
| CORS | Browser security — preflight OPTIONS request |
| REST vs gRPC | REST for external APIs, gRPC for internal microservices |
| WebSocket | Full-duplex, persistent connection for real-time |
| CDN | Edge caching, reduces latency globally |
| DNS | Browser → OS → Router → ISP → Root → TLD → Authoritative |
| Status codes | 2xx success, 3xx redirect, 4xx client error, 5xx server error |
