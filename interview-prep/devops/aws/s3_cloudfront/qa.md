# AWS S3 & CloudFront — Interview Q&A

> 12 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. S3 storage classes — when to use each?

| Class | Use case | Retrieval | Cost |
|-------|----------|-----------|------|
| **S3 Standard** | Frequently accessed data | Instant | Highest |
| **S3 Intelligent-Tiering** | Unknown access patterns | Instant | Auto-optimized |
| **S3 Standard-IA** | Infrequent access (>30 days) | Instant | Lower storage, per-retrieval fee |
| **S3 One Zone-IA** | Non-critical infrequent data | Instant | Cheapest IA |
| **S3 Glacier Instant** | Archive, rare access | Instant | Low |
| **S3 Glacier Flexible** | Archive, hours to retrieve | 1-12 hours | Very low |
| **S3 Glacier Deep Archive** | Long-term archive | 12-48 hours | Lowest |

**Lifecycle policy example:**
```json
{
  "Rules": [{
    "Transitions": [
      { "Days": 30, "StorageClass": "STANDARD_IA" },
      { "Days": 90, "StorageClass": "GLACIER" },
      { "Days": 365, "StorageClass": "DEEP_ARCHIVE" }
    ],
    "Expiration": { "Days": 730 }
  }]
}
```

---

### Q2. S3 security — how do you control access?

1. **Bucket policies** (resource-based, JSON)
2. **IAM policies** (identity-based, attached to users/roles)
3. **ACLs** (legacy, avoid for new configs)
4. **Pre-signed URLs** (temporary, time-limited access)
5. **VPC endpoints** (keep traffic within AWS network)

```json
// Bucket policy: allow CloudFront to read
{
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "cloudfront.amazonaws.com" },
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::my-bucket/*",
    "Condition": {
      "StringEquals": { "AWS:SourceArn": "arn:aws:cloudfront::ACCOUNT:distribution/DIST_ID" }
    }
  }]
}
```

```java
// Pre-signed URL (temporary upload/download)
GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, key)
    .withMethod(HttpMethod.PUT)
    .withExpiration(Date.from(Instant.now().plus(Duration.ofMinutes(15))));
URL url = s3Client.generatePresignedUrl(req);
```

---

### Q3. CloudFront — how does it work?

```
User (India) → CloudFront Edge (Mumbai) → [Cache HIT?]
                                           Yes → Return cached content
                                           No  → Origin (S3 / ALB) → Cache → Return
```

**Key concepts:**
- **Edge locations** — 450+ worldwide CDN points
- **Distribution** — configuration for your content delivery
- **Origin** — where content comes from (S3, ALB, custom HTTP)
- **TTL** — how long content is cached (Cache-Control headers)
- **Invalidation** — force cache refresh for specific paths

---

## Scenario-Based Questions

### Q4. At FedEx, how would you serve the React tracking portal via S3 + CloudFront?

```
React build → S3 Bucket (static hosting)
                   ↓
CloudFront Distribution
  - Default behavior: *.js, *.css → cache 1 year (immutable, hashed filenames)
  - /index.html → cache 5 min (or no-cache, must-revalidate)
  - /api/* → forward to ALB (backend)
  - Custom error page: 403/404 → /index.html (SPA routing)
  - HTTPS only (ACM certificate)
  - Origin Access Control (OAC) — only CloudFront can access S3
```

```yaml
# CloudFront behaviors:
- Path: /api/*           → ALB Origin (backend)
- Path: /static/*        → S3 Origin (max-age=31536000, immutable)
- Path: Default (*)      → S3 Origin (max-age=300)
  Custom Error Responses:
    - 403 → /index.html (200) # S3 returns 403 for missing paths → SPA handles routing
    - 404 → /index.html (200)
```

---

### Q5. At Hatio, how do you handle file uploads for payment receipts?

```java
// Backend generates pre-signed URL → frontend uploads directly to S3
@PostMapping("/upload-url")
public PresignedUrlResponse getUploadUrl(@RequestParam String filename) {
    String key = "receipts/" + UUID.randomUUID() + "/" + sanitize(filename);
    
    GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, key)
        .withMethod(HttpMethod.PUT)
        .withContentType("application/pdf")
        .withExpiration(Date.from(Instant.now().plus(Duration.ofMinutes(15))));
    
    return new PresignedUrlResponse(s3Client.generatePresignedUrl(req).toString(), key);
}

// Frontend uploads directly to S3 (no backend proxy = scalable)
// fetch(presignedUrl, { method: 'PUT', body: file, headers: { 'Content-Type': 'application/pdf' } })
```

**Security:**
- Pre-signed URLs expire after 15 minutes
- Bucket policy restricts content types (only PDF/images)
- S3 event → Lambda → scan for malware before processing

---

## Gotchas & Edge Cases

### Q6. S3 consistency model — what changed?

**Since December 2020:** S3 provides **strong read-after-write consistency** for all operations. Previously, PUT/DELETE had eventual consistency.

Now: If you PUT an object and immediately GET it, you always get the latest version.

---

### Q7. CloudFront caching issues — how do you handle deployments?

```
Problem: Deploy new React build, but CloudFront serves old cached version.

Solutions:
1. Use hashed filenames (main.abc123.js) — new filename = new cache entry ✅
2. index.html: Cache-Control: no-cache, must-revalidate
3. CloudFront invalidation: aws cloudfront create-invalidation --paths "/*"
   (costs $0.005 per path, use sparingly)
4. Versioned paths: /v2/app.js (for API responses)
```
