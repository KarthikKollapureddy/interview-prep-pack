# Spring Security & OAuth2 — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. Explain the Spring Security filter chain. How does a request get authenticated?

```
HTTP Request
  → SecurityFilterChain
    → CorsFilter
    → CsrfFilter
    → UsernamePasswordAuthenticationFilter (or JwtAuthFilter)
    → ExceptionTranslationFilter
    → AuthorizationFilter (formerly FilterSecurityInterceptor)
  → Controller
```

**Authentication flow (JWT example):**
1. `JwtAuthenticationFilter` extracts token from `Authorization: Bearer <token>` header
2. Token is validated (signature, expiry, claims)
3. `UsernamePasswordAuthenticationToken` is created from claims
4. Set in `SecurityContextHolder.getContext().setAuthentication(auth)`
5. Request proceeds to controller with user identity available

**Key objects:**
- `Authentication` — represents the current user (principal + authorities)
- `SecurityContext` — holds the `Authentication`, stored in `ThreadLocal`
- `UserDetailsService` — loads user by username from DB
- `PasswordEncoder` — hashes/verifies passwords (use BCrypt)

---

### Q2. What is OAuth 2.0? Explain the authorization code flow.

```
1. User clicks "Login with Google"
2. App redirects to Google's /authorize endpoint
3. User authenticates with Google
4. Google redirects back to app with authorization CODE
5. App exchanges code for ACCESS TOKEN (server-to-server)
6. App uses access token to call Google APIs / extract user info
```

**Key roles:**
- **Resource Owner** — the user
- **Client** — your application
- **Authorization Server** — issues tokens (Google, Okta, Keycloak)
- **Resource Server** — API that validates tokens

**Token types:**
- **Access Token** — short-lived (15 min), sent with each API call
- **Refresh Token** — long-lived, used to get new access tokens
- **ID Token** (OpenID Connect) — contains user identity claims

---

### Q3. JWT structure and validation — explain each part.

```
Header.Payload.Signature

Header: { "alg": "RS256", "typ": "JWT" }
Payload: { "sub": "user123", "roles": ["ADMIN"], "exp": 1700000000, "iss": "auth-server" }
Signature: RS256(base64(header) + "." + base64(payload), privateKey)
```

**Validation steps (Resource Server):**
1. Split token into 3 parts
2. Decode header → get algorithm
3. Verify signature using public key (RS256) or shared secret (HS256)
4. Check `exp` (expiry), `iss` (issuer), `aud` (audience)
5. Extract claims → build `Authentication` object

**RS256 vs HS256:**
- **HS256** — symmetric, same key to sign and verify (simpler, less secure)
- **RS256** — asymmetric, private key signs, public key verifies (production-grade)

---

## Scenario-Based Questions

### Q4. At FedEx, you need to secure the SEFS-PDDV API with JWT. Only authenticated users with specific roles can access scan endpoints. Show the SecurityConfig.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Stateless API — no CSRF needed
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/shipments/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers("/api/v1/scans/**").hasAuthority("SCAN_READ")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.fedex.com/realms/sefs
          # or jwk-set-uri: https://auth.fedex.com/.well-known/jwks.json
```

---

### Q5. At NPCI, you need method-level security. How do you restrict access at the service layer?

```java
@Service
public class TransactionService {

    @PreAuthorize("hasRole('ADMIN') or #merchantId == authentication.principal.merchantId")
    public TransactionReport getReport(String merchantId) { /* ... */ }

    @PreAuthorize("hasAuthority('PAYMENT_WRITE')")
    public PaymentResult initiatePayment(PaymentRequest req) { /* ... */ }

    @PostAuthorize("returnObject.merchantId == authentication.principal.merchantId")
    public Transaction getTransaction(String txnId) { /* ... */ }

    @PreFilter("filterObject.amount < 100000") // filter input collection
    public List<PaymentResult> batchProcess(List<PaymentRequest> requests) { /* ... */ }
}
```

**`@PreAuthorize`** — checked BEFORE method execution  
**`@PostAuthorize`** — checked AFTER (has access to `returnObject`)  
**`@PreFilter`/`@PostFilter`** — filter collections  

SpEL expressions can access `authentication`, `principal`, method args by name.

---

### Q6. At Hatio, you're integrating with a third-party payment gateway that uses API keys. How do you secure API key storage?

**Never hardcode secrets.** Hierarchy of options:

```
1. Vault (HashiCorp / AWS Secrets Manager) — BEST for production
2. Environment variables (via Kubernetes Secrets / PCF CredHub)
3. Spring Cloud Config Server with encryption
4. application-{profile}.yml with encrypted values (jasypt)
```

```java
// Using Spring Cloud Vault
@Value("${payment-gateway.api-key}")
private String apiKey; // Fetched from Vault at startup

// Or using AWS Secrets Manager
@Bean
public PaymentGatewayClient paymentClient(
        @Value("${aws.secretsmanager.payment-api-key}") String apiKey) {
    return new PaymentGatewayClient(apiKey);
}
```

**Never log API keys.** Sanitize in actuator:
```yaml
management:
  endpoint:
    env:
      keys-to-sanitize: "password,secret,key,token,api-key"
```

---

### Q7. CORS configuration for a React frontend calling Spring Boot API?

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://fedex-portal.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

// In SecurityFilterChain:
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**Gotcha:** If you use Spring Security, CORS must be configured in the security filter chain — `@CrossOrigin` alone won't work because the security filter runs first and blocks the preflight OPTIONS request.

---

## Coding Challenges

### Challenge 1: JWT Authentication Filter
**File:** `solutions/JwtAuthFilter.java`  
Implement a custom JWT authentication filter:
1. Extract token from Authorization header
2. Validate signature and expiry
3. Extract user roles from claims
4. Set SecurityContext with authenticated user
5. Handle invalid/expired token gracefully

### Challenge 2: RBAC Access Control
**File:** `solutions/RbacDesign.java`  
Design a role-based access control system:
1. User → Roles → Permissions mapping
2. Custom `UserDetailsService` loading from in-memory store
3. `SecurityConfig` with URL-based and method-based authorization
4. Test method: verify access/denied for different users

---

## Gotchas & Edge Cases

### Q8. CSRF — when do you need it, when can you disable it?

**Disable CSRF for stateless APIs** (JWT, API keys) — the token itself is the CSRF protection.  
**Enable CSRF for session-based apps** — browser automatically sends cookies, so an attacker can forge requests.

```java
// Stateless API — safe to disable
http.csrf(csrf -> csrf.disable())

// Session-based app — keep CSRF enabled
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```

---

### Q9. What's the difference between `hasRole()` and `hasAuthority()`?

```java
// hasRole("ADMIN") actually checks for authority "ROLE_ADMIN"
// hasAuthority("ADMIN") checks for authority "ADMIN" exactly

.hasRole("ADMIN")       // matches GrantedAuthority("ROLE_ADMIN")
.hasAuthority("ADMIN")  // matches GrantedAuthority("ADMIN")
```

**Rule:** If your authorities have `ROLE_` prefix, use `hasRole()`. Otherwise, use `hasAuthority()`.
