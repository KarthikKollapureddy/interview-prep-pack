# Application Security — Interview Q&A

> 12 questions covering OWASP Top 10, JWT, XSS, CSRF, SQL Injection, and secure coding  
> Priority: **P1** — Critical for NPCI (payments) and FedEx; asked at all product companies

---

## Conceptual Questions

### Q1. What is the OWASP Top 10? Explain the most critical ones.

**Answer:**
OWASP Top 10 is a standard awareness document for web application security risks.

| # | Risk | One-liner |
|---|------|-----------|
| A01 | **Broken Access Control** | Users access resources beyond their permissions |
| A02 | **Cryptographic Failures** | Sensitive data exposed (plaintext passwords, weak encryption) |
| A03 | **Injection** | SQL, NoSQL, OS, LDAP injection via untrusted input |
| A04 | **Insecure Design** | Missing security controls in architecture |
| A05 | **Security Misconfiguration** | Default configs, open cloud storage, verbose errors |
| A06 | **Vulnerable Components** | Using libraries with known CVEs |
| A07 | **Auth Failures** | Weak passwords, missing MFA, session fixation |
| A08 | **Software & Data Integrity** | Untrusted CI/CD pipelines, unsigned updates |
| A09 | **Logging & Monitoring Failures** | Breaches go undetected |
| A10 | **SSRF** | Server makes requests to unintended internal resources |

**Top 3 for interview:**
1. **Injection** (SQL Injection) — always demonstrate parameterized queries
2. **Broken Access Control** — IDOR, missing function-level auth
3. **XSS** — stored, reflected, DOM-based

---

### Q2. What is SQL Injection? How do you prevent it?

**Answer:**

**Attack:**
```java
// VULNERABLE — string concatenation
String query = "SELECT * FROM users WHERE username = '" + userInput + "'";
// If userInput = "' OR '1'='1" → returns ALL users
// If userInput = "'; DROP TABLE users;--" → deletes table
```

**Prevention (defense in depth):**

| Layer | Technique |
|-------|-----------|
| **Code** | Parameterized queries / Prepared Statements |
| **ORM** | JPA/Hibernate (automatically parameterized) |
| **Validation** | Whitelist input validation |
| **Database** | Least-privilege DB accounts |
| **WAF** | Web Application Firewall rules |

```java
// SAFE — Parameterized query (JDBC)
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE username = ? AND password = ?");
ps.setString(1, username);
ps.setString(2, password);

// SAFE — JPA/Spring Data
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);

// SAFE — Spring Data derived query
List<User> findByUsername(String username);
```

---

### Q3. What is XSS (Cross-Site Scripting)? Types and prevention.

**Answer:**
XSS injects malicious JavaScript into a web page viewed by other users.

| Type | Stored in | Example |
|------|-----------|---------|
| **Stored XSS** | Database | Attacker posts `<script>steal()</script>` in a comment → every visitor executes it |
| **Reflected XSS** | URL parameter | `example.com/search?q=<script>alert(1)</script>` → reflected in response |
| **DOM-based XSS** | Client JS | JS reads `location.hash` and inserts into DOM without sanitization |

**Prevention:**
```
1. Output encoding — encode special characters (<, >, ", &)
   Java: OWASP Java Encoder — Encode.forHtml(userInput)
   React: JSX auto-escapes by default ✅
   Angular: auto-sanitizes by default ✅

2. Content Security Policy (CSP) header:
   Content-Security-Policy: default-src 'self'; script-src 'self'
   → Blocks inline scripts and external script sources

3. HttpOnly cookies — prevent JS access to session cookies:
   Set-Cookie: session=abc123; HttpOnly; Secure; SameSite=Strict

4. Input validation — whitelist allowed characters

5. Avoid innerHTML / dangerouslySetInnerHTML
```

---

### Q4. What is CSRF (Cross-Site Request Forgery)? Prevention methods.

**Answer:**
CSRF tricks a logged-in user into making unintended requests. The browser automatically attaches cookies to requests.

```
1. User logs into bank.com (session cookie set)
2. User visits evil.com
3. evil.com has: <img src="https://bank.com/transfer?to=attacker&amount=10000">
4. Browser sends request WITH the bank.com cookie → Transfer happens!
```

**Prevention:**

| Method | How |
|--------|-----|
| **CSRF Token** | Server generates unique token per session/form. Submitted with every state-changing request. Attacker can't guess it. |
| **SameSite Cookie** | `Set-Cookie: session=abc; SameSite=Strict` — cookie NOT sent on cross-site requests |
| **Double Submit Cookie** | Send CSRF token both in cookie and request body — server compares both |
| **Origin/Referer Check** | Verify `Origin` or `Referer` header matches expected domain |

**Spring Security CSRF (enabled by default):**
```java
// For traditional web apps — CSRF token in forms:
<input type="hidden" name="_csrf" value="${_csrf.token}"/>

// For REST APIs with JWT — CSRF is NOT needed:
// JWT is not auto-attached by browser (unlike cookies)
http.csrf().disable();  // Safe when using Bearer token auth
```

---

### Q5. Explain JWT (JSON Web Token) in detail. How does authentication work?

**Answer:**

**JWT Structure:**
```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiJ9.    ← Header (base64): {"alg":"HS256","typ":"JWT"}
eyJzdWIiOiIxMjM0NTY3ODkwIn0.  ← Payload (base64): {"sub":"user@example.com","role":"ADMIN","exp":1714000000}
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  ← Signature: HMAC-SHA256(header+payload, secret)
```

**Authentication Flow:**
```
1. Login: POST /auth/login {username, password}
2. Server validates credentials
3. Server creates JWT with user claims (id, role, expiry)
4. Server returns JWT to client
5. Client stores JWT (localStorage or httpOnly cookie)
6. Every request: Authorization: Bearer <JWT>
7. Server validates signature and expiry on each request
```

**JWT vs Session:**
| Feature | JWT | Session |
|---------|-----|---------|
| Storage | Client (token) | Server (session store) |
| Stateless | Yes | No (server must store) |
| Scalability | Excellent (no shared state) | Requires sticky sessions or shared store |
| Revocation | Hard (must wait for expiry) | Easy (delete from store) |
| Size | Larger (contains claims) | Small (just session ID) |

**Security best practices:**
- Use short expiry (15 min access token + refresh token)
- Store in **httpOnly cookie** (not localStorage — vulnerable to XSS)
- Use **RS256** (asymmetric) over HS256 for microservices
- Never store sensitive data in payload (it's base64, not encrypted)
- Implement token rotation and blacklisting for logout

---

### Q6. What is the difference between Authentication and Authorization?

**Answer:**

| Aspect | Authentication (AuthN) | Authorization (AuthZ) |
|--------|----------------------|---------------------|
| Question | "Who are you?" | "What can you do?" |
| Purpose | Verify identity | Verify permissions |
| Example | Login with credentials | Access admin dashboard |
| When | Before authorization | After authentication |
| HTTP code on failure | 401 Unauthorized | 403 Forbidden |

**Spring Security flow:**
```
Request → Authentication Filter → AuthenticationManager → UserDetailsService
         (extract credentials)     (validate)              (load user + roles)
         
       → Authorization Filter → AccessDecisionManager
         (check @PreAuthorize)   (vote: GRANTED/DENIED)
```

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<User> getAllUsers() { ... }

@PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
@GetMapping("/users/{userId}/profile")
public UserProfile getProfile(@PathVariable Long userId) { ... }
```

---

## Scenario / Interview Deep-Dive Questions

### Q7. How do you secure a Spring Boot REST API? (End-to-end)

**Answer — Security layers:**

```
Layer 1: Transport Security
  ├── HTTPS everywhere (TLS 1.2+)
  └── HSTS header (Strict-Transport-Security)

Layer 2: Authentication
  ├── JWT-based stateless auth
  ├── OAuth2/OIDC for third-party login
  └── MFA for sensitive operations

Layer 3: Authorization
  ├── Role-based access control (RBAC)
  ├── Method-level security (@PreAuthorize)
  └── Row-level security (data filtering)

Layer 4: Input Security
  ├── Input validation (@Valid, @Size, @Pattern)
  ├── Parameterized queries (prevent SQL injection)
  └── Output encoding (prevent XSS)

Layer 5: API Security
  ├── Rate limiting (429 Too Many Requests)
  ├── CORS configuration
  ├── Request size limits
  └── API versioning

Layer 6: Data Security
  ├── Encrypt at rest (AES-256)
  ├── Encrypt in transit (TLS)
  ├── Hash passwords (BCrypt, Argon2)
  └── Mask PII in logs

Layer 7: Infrastructure
  ├── WAF (Web Application Firewall)
  ├── DDoS protection
  ├── Secret management (Vault, AWS Secrets Manager)
  └── Security headers (CSP, X-Frame-Options, X-Content-Type-Options)
```

---

### Q8. How do you store passwords securely?

**Answer:**

**NEVER:** Plaintext, MD5, SHA-1, SHA-256 (without salt)

**ALWAYS use adaptive hashing:**
```java
// Spring Security BCrypt (recommended):
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 12 rounds = ~250ms per hash
}

// Registration:
String hashed = passwordEncoder.encode(rawPassword);
// hashed = "$2a$12$LJ3m4ys4...randomsalt...hashvalue"

// Login verification:
boolean matches = passwordEncoder.matches(rawPassword, storedHash);
```

**Why BCrypt?**
- Includes random salt (prevents rainbow table attacks)
- Configurable work factor (rounds) — increase as hardware gets faster
- Purpose-built for password hashing (unlike SHA which is fast)

**Alternatives:** Argon2 (newer, memory-hard), scrypt, PBKDF2.

---

### Q9. What is IDOR (Insecure Direct Object Reference)?

**Answer:**
IDOR happens when an API exposes internal object IDs and doesn't verify the user owns the resource.

```
// VULNERABLE:
GET /api/orders/12345      ← User changes 12345 to 12346 → sees another user's order!

// FIXED — Authorization check:
@GetMapping("/orders/{orderId}")
public Order getOrder(@PathVariable Long orderId) {
    Order order = orderRepo.findById(orderId).orElseThrow();
    if (!order.getUserId().equals(currentUser.getId())) {
        throw new AccessDeniedException("Not your order");
    }
    return order;
}
```

**Prevention:**
1. Always verify the resource belongs to the authenticated user
2. Use UUIDs instead of sequential IDs (harder to guess)
3. Implement row-level security at the repository layer
4. Use `@PreAuthorize` with SpEL expressions

---

### Q10. Explain OAuth 2.0 Authorization Code Flow.

**Answer:**

```
┌──────┐                              ┌──────────────┐
│ User │                              │ Auth Server   │
│(Browser)                            │ (Google/Okta) │
└──┬───┘                              └──────┬───────┘
   │                                         │
   │  1. Click "Login with Google"           │
   │──────────────────────────────────────► │
   │                                         │
   │  2. Auth Server shows consent screen    │
   │◄──────────────────────────────────────  │
   │                                         │
   │  3. User approves, gets Authorization   │
   │     Code (redirected to callback URL)   │
   │──────► App Server                       │
   │         │                               │
   │         │ 4. Exchange code for tokens   │
   │         │   (server-to-server, secure)  │
   │         │──────────────────────────────►│
   │         │                               │
   │         │ 5. Get Access Token +         │
   │         │    Refresh Token              │
   │         │◄──────────────────────────────│
   │         │                               │
   │         │ 6. Use Access Token to call   │
   │         │    Resource Server (API)      │
   │         │──────────────────────────────►│ Resource Server
   │                                         │
```

**Key tokens:**
| Token | Purpose | Lifetime | Storage |
|-------|---------|----------|---------|
| Access Token | Access APIs | Short (15 min) | Memory / httpOnly cookie |
| Refresh Token | Get new access token | Long (7 days) | Secure, httpOnly cookie |
| ID Token (OIDC) | User identity | Short | Client-side |

---

### Q11. How do you handle secrets and API keys in production?

**Answer:**

| Approach | Use Case | Example |
|----------|----------|---------|
| **Environment variables** | Simple deployments | `DB_PASSWORD=xxx` in .env |
| **Secrets Manager** | Cloud production | AWS Secrets Manager, Azure Key Vault |
| **HashiCorp Vault** | Enterprise | Dynamic secrets, rotation |
| **Spring Cloud Config** | Microservices | Encrypted properties in Git |

**Rules:**
- NEVER commit secrets to Git (use `.gitignore`, git-secrets hook)
- NEVER log secrets or tokens
- Rotate secrets regularly
- Use least-privilege access for service accounts

```yaml
# application.yml with encrypted value (Spring Cloud Config + Jasypt):
spring:
  datasource:
    password: ENC(dGVzdHBhc3N3b3Jk)  # Encrypted at rest
```

---

### Q12. What are Security Headers? List the important ones.

**Answer:**

| Header | Purpose | Example |
|--------|---------|---------|
| `Strict-Transport-Security` | Force HTTPS | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | Prevent XSS, clickjacking | `default-src 'self'; script-src 'self'` |
| `X-Content-Type-Options` | Prevent MIME sniffing | `nosniff` |
| `X-Frame-Options` | Prevent clickjacking | `DENY` or `SAMEORIGIN` |
| `X-XSS-Protection` | Browser XSS filter | `1; mode=block` |
| `Referrer-Policy` | Control referrer info | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | Restrict browser features | `camera=(), microphone=()` |

**Spring Security auto-configures most of these.** Additional config:
```java
http.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'")
    .frameOptions().deny()
    .httpStrictTransportSecurity()
);
```

---

## Quick Reference

### Security Interview Cheat Sheet

| Topic | Key Point |
|-------|-----------|
| OWASP Top 3 | Broken Access Control, Cryptographic Failures, Injection |
| SQL Injection | Use PreparedStatement / JPA — NEVER concatenate user input |
| XSS | Encode output, CSP header, HttpOnly cookies |
| CSRF | SameSite cookies + CSRF tokens (not needed with JWT Bearer) |
| JWT | Short-lived access + refresh token, store in httpOnly cookie |
| Passwords | BCrypt (12+ rounds), NEVER MD5/SHA-256 |
| IDOR | Always verify resource ownership server-side |
| OAuth 2.0 | Authorization Code flow for web apps |
| Secrets | Never in Git. Use Vault/Secrets Manager. |
| 401 vs 403 | 401 = who are you? 403 = you can't do that |
