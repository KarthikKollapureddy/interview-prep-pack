# AWS Lambda & Serverless — Interview Q&A

> 12 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Gotchas

---

## Conceptual Questions

### Q1. What is AWS Lambda? How does it work?

**Serverless compute** — you provide code, AWS manages servers, scaling, and availability.

```
Event Source → Lambda Function → Response/Side Effect
(API Gateway)   (your code)      (DB write, S3 upload, SQS message)
```

**Key characteristics:**
- **Pay per invocation** (not per hour) — 1M free requests/month
- **Auto-scales** to thousands of concurrent executions
- **Stateless** — no persistent local storage between invocations
- **Max execution time:** 15 minutes
- **Memory:** 128 MB to 10 GB (CPU scales with memory)
- **Package size:** 50 MB zipped, 250 MB unzipped (or container image up to 10 GB)

---

### Q2. Lambda triggers — what can invoke a Lambda?

| Trigger | Use case |
|---------|----------|
| **API Gateway** | REST/HTTP API endpoints |
| **S3** | File upload processing |
| **SQS** | Queue message processing |
| **DynamoDB Streams** | React to DB changes |
| **CloudWatch Events** | Scheduled tasks (cron) |
| **Kinesis** | Stream processing |
| **SNS** | Notification handling |
| **Cognito** | Auth triggers (pre-signup, post-confirm) |

```java
// Lambda handler (Java)
public class ShipmentEventHandler implements RequestHandler<S3Event, String> {
    @Override
    public String handleRequest(S3Event event, Context context) {
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        
        // Process uploaded file
        processShipmentFile(bucket, key);
        return "Processed: " + key;
    }
}
```

---

### Q3. Cold start — what is it and how to mitigate?

**Cold start:** First invocation after idle period → Lambda initializes runtime, loads code, creates execution context. Can add 1-10 seconds latency (Java is worst, Python/Node fastest).

**Mitigation strategies:**
1. **Provisioned Concurrency** — pre-warm N instances (costs money)
2. **SnapStart (Java)** — snapshots initialized state, restores in ~200ms
3. **Smaller package** — less code to load
4. **Use lighter runtimes** — Node.js/Python for latency-sensitive functions
5. **Keep functions warm** — CloudWatch scheduled event every 5 min (hacky but works)

```yaml
# SAM template with SnapStart
Resources:
  ShipmentFunction:
    Type: AWS::Serverless::Function
    Properties:
      Runtime: java17
      SnapStart:
        ApplyOn: PublishedVersions
```

---

## Scenario-Based Questions

### Q4. At FedEx, how would you use Lambda for processing shipment label PDFs uploaded to S3?

```
Flow:
1. Customer uploads label PDF to S3 (via pre-signed URL)
2. S3 event triggers Lambda
3. Lambda: extracts text (Textract), validates data, writes to DynamoDB
4. Lambda: publishes event to SNS → triggers notification Lambda
```

```java
public class LabelProcessor implements RequestHandler<S3Event, Void> {
    private final TextractClient textract;
    private final DynamoDbClient dynamo;
    
    public Void handleRequest(S3Event event, Context ctx) {
        var record = event.getRecords().get(0);
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        
        // Extract text from PDF
        var result = textract.detectDocumentText(req -> req
            .document(doc -> doc.s3Object(s3 -> s3.bucket(bucket).name(key))));
        
        // Parse and store
        ShipmentLabel label = parser.parse(result);
        dynamoService.save(label);
        
        return null;
    }
}
```

---

### Q5. At NPCI, how would you use Lambda for scheduled batch reconciliation?

```yaml
# EventBridge rule: run every day at 2 AM IST
Resources:
  ReconciliationRule:
    Type: AWS::Events::Rule
    Properties:
      ScheduleExpression: "cron(0 20 * * ? *)"  # 2 AM IST = 8:30 PM UTC
      Targets:
        - Arn: !GetAtt ReconciliationFunction.Arn
```

**Pattern:** Lambda reads day's transactions from RDS, compares with partner bank's file in S3, generates discrepancy report.

**⚠️ 15-minute limit** — for long-running batch jobs, use Step Functions to orchestrate multiple Lambda invocations or use Fargate.

---

## Gotchas & Edge Cases

### Q6. Lambda + VPC — networking pitfall?

If Lambda needs to access RDS (in VPC) AND external APIs (internet):
- Lambda in VPC → loses internet access by default
- Need NAT Gateway in public subnet → Lambda in private subnet routes through NAT
- NAT Gateway costs ~$32/month + data transfer

**Alternative:** Use VPC endpoints for AWS services (S3, DynamoDB, SQS) — free, no NAT needed.

---

### Q7. Lambda idempotency — why is it critical?

Lambda may execute your function **more than once** (retry on failure, at-least-once delivery from SQS).

```java
// ❌ Non-idempotent: charges customer twice on retry
public void handlePayment(PaymentEvent event) {
    paymentGateway.charge(event.amount()); // Called twice on retry!
}

// ✅ Idempotent: check if already processed
public void handlePayment(PaymentEvent event) {
    if (processedEvents.contains(event.eventId())) return; // Skip duplicate
    paymentGateway.charge(event.amount());
    processedEvents.add(event.eventId());
}
```

**Use DynamoDB conditional writes** or a unique constraint to enforce idempotency.
