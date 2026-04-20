# MongoDB — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. MongoDB vs MySQL — when to use which?

| Aspect | MySQL (Relational) | MongoDB (Document) |
|--------|-------------------|-------------------|
| Schema | Fixed (ALTER TABLE) | Flexible (schema-less) |
| Relationships | JOINs (normalized) | Embedded documents (denormalized) |
| ACID | Full ACID | ACID per document (multi-doc since 4.0) |
| Scale | Vertical (read replicas) | Horizontal (sharding) |
| Query | SQL | MQL (MongoDB Query Language) |
| Best for | Structured data, complex JOINs, transactions | Semi-structured, rapid iteration, high write throughput |

**Use MongoDB when:**
- Schema evolves frequently (startup, MVP)
- Hierarchical/nested data (product catalog with varying attributes)
- High write throughput (logs, events, IoT)
- Geo-spatial queries

**Use MySQL when:**
- Strict data integrity required (financial transactions)
- Complex JOINs across many tables
- ACID transactions across multiple tables
- Mature, stable schema

---

### Q2. MongoDB data modeling — embedding vs referencing.

```javascript
// Embedding (denormalized) — data lives together
{
  _id: "S001",
  trackingNumber: "123456789012",
  status: "IN_TRANSIT",
  events: [  // Embedded array
    { timestamp: "2024-01-15T10:00:00Z", location: "Memphis", action: "Picked up" },
    { timestamp: "2024-01-16T14:00:00Z", location: "Nashville", action: "In transit" }
  ],
  customer: {  // Embedded document
    name: "Karthik",
    address: { city: "Hyderabad", zip: "500001" }
  }
}

// Referencing (normalized) — separate collections with IDs
// shipments collection
{ _id: "S001", trackingNumber: "123", customerId: "C001" }
// customers collection
{ _id: "C001", name: "Karthik", address: { ... } }
```

| | Embedding | Referencing |
|---|---|---|
| Reads | Fast (one query) | Slower (multiple queries or $lookup) |
| Writes | Slower (update whole document) | Faster (update specific doc) |
| Data size | Duplicated | No duplication |
| Use when | 1:1, 1:few, always read together | 1:many, many:many, large sub-docs |

**Rule:** Embed data accessed together. Reference data that grows unboundedly or is shared.

---

### Q3. MongoDB indexes — types and usage.

```javascript
// Single field index
db.shipments.createIndex({ trackingNumber: 1 });  // 1=ascending

// Compound index
db.shipments.createIndex({ status: 1, createdAt: -1 });

// Unique index
db.users.createIndex({ email: 1 }, { unique: true });

// TTL index (auto-delete old documents)
db.sessions.createIndex({ createdAt: 1 }, { expireAfterSeconds: 3600 }); // Delete after 1 hour

// Text index (full-text search)
db.products.createIndex({ name: "text", description: "text" });
db.products.find({ $text: { $search: "wireless headphones" } });

// Geo-spatial index
db.stores.createIndex({ location: "2dsphere" });
db.stores.find({ location: { $near: { $geometry: { type: "Point", coordinates: [78.4, 17.3] } } } });
```

---

### Q4. Aggregation Pipeline — explain with example.

```javascript
// At FedEx: shipment analytics
db.shipments.aggregate([
  // Stage 1: Filter to last 30 days
  { $match: { createdAt: { $gte: new Date(Date.now() - 30*24*60*60*1000) } } },
  
  // Stage 2: Group by status
  { $group: {
      _id: "$status",
      count: { $sum: 1 },
      avgWeight: { $avg: "$weight" }
  }},
  
  // Stage 3: Sort by count
  { $sort: { count: -1 } },
  
  // Stage 4: Reshape output
  { $project: {
      status: "$_id",
      count: 1,
      avgWeight: { $round: ["$avgWeight", 2] },
      _id: 0
  }}
]);
// Output: [{ status: "DELIVERED", count: 15000, avgWeight: 2.34 }, ...]
```

**Key stages:** `$match` → `$group` → `$sort` → `$project` → `$lookup` (JOIN) → `$unwind` (flatten arrays)

---

## Scenario-Based Questions

### Q5. At FedEx, design the MongoDB schema for a shipment tracking system.

```javascript
// shipments collection
{
  _id: ObjectId("..."),
  trackingNumber: "794644790138",
  status: "IN_TRANSIT",
  serviceType: "GROUND",
  weight: 2.5,
  origin: { city: "Memphis", state: "TN", zip: "38101", country: "US" },
  destination: { city: "Nashville", state: "TN", zip: "37201", country: "US" },
  customer: {
    id: "C001",
    name: "Acme Corp"
  },
  events: [  // Embedded — always fetched with shipment, bounded size
    {
      timestamp: ISODate("2024-01-15T10:00:00Z"),
      location: { city: "Memphis", facility: "MEMH" },
      scanType: "PICKUP",
      description: "Package picked up"
    },
    {
      timestamp: ISODate("2024-01-16T06:00:00Z"),
      location: { city: "Nashville", facility: "BNAS" },
      scanType: "ARRIVAL_SCAN",
      description: "Arrived at FedEx facility"
    }
  ],
  estimatedDelivery: ISODate("2024-01-17T18:00:00Z"),
  createdAt: ISODate("2024-01-15T09:00:00Z"),
  updatedAt: ISODate("2024-01-16T06:00:00Z")
}

// Indexes
db.shipments.createIndex({ trackingNumber: 1 }, { unique: true });
db.shipments.createIndex({ "customer.id": 1, createdAt: -1 });
db.shipments.createIndex({ status: 1, estimatedDelivery: 1 });
```

**Why embed events?** Each shipment has < 50 events. Always displayed together. No need for separate collection.

---

### Q6. At NPCI, how do you use MongoDB for transaction audit logs?

```javascript
// Audit log collection — append-only, high write throughput
{
  _id: ObjectId("..."),
  transactionId: "TXN-2024-001",
  userId: "U001",
  action: "PAYMENT_INITIATED",
  amount: 5000,
  metadata: {
    sourceBank: "HDFC",
    destBank: "SBI",
    upiId: "user@upi"
  },
  timestamp: ISODate("2024-01-15T10:30:00Z"),
  ttl: ISODate("2024-07-15T10:30:00Z") // Auto-delete after 6 months
}

// TTL index for auto-cleanup
db.auditLogs.createIndex({ ttl: 1 }, { expireAfterSeconds: 0 });

// Capped collection for recent logs (fixed size, FIFO)
db.createCollection("recentLogs", { capped: true, size: 104857600, max: 100000 });
```

---

### Q7. Spring Data MongoDB — how do you use it?

```java
@Document(collection = "shipments")
public class Shipment {
    @Id private String id;
    @Indexed(unique = true) private String trackingNumber;
    private ShipmentStatus status;
    @Indexed private String customerId;
    private List<TrackingEvent> events; // Embedded
    private Address origin;
    private Address destination;
}

public interface ShipmentRepository extends MongoRepository<Shipment, String> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByStatusAndCustomerId(ShipmentStatus status, String customerId);
    
    @Query("{ 'events.scanType': ?0, 'createdAt': { $gte: ?1 } }")
    List<Shipment> findByScanTypeAfterDate(String scanType, LocalDateTime date);
}

// Custom aggregation
@Autowired MongoTemplate mongoTemplate;

public List<StatusCount> getStatusCounts() {
    Aggregation agg = Aggregation.newAggregation(
        Aggregation.group("status").count().as("count"),
        Aggregation.sort(Sort.Direction.DESC, "count")
    );
    return mongoTemplate.aggregate(agg, "shipments", StatusCount.class).getMappedResults();
}
```

---

## Gotchas & Edge Cases

### Q8. MongoDB document size limit and array growth.

**Max document size: 16 MB.** If embedded arrays grow unbounded, you'll hit this limit.

```javascript
// ❌ BAD: unbounded array — could exceed 16MB
{
  userId: "U001",
  allTransactions: [ /* thousands of transactions */ ]
}

// ✅ GOOD: separate collection for large arrays
// transactions collection (referenced by userId)
{ userId: "U001", amount: 500, date: "..." }
```

**The Bucket Pattern** for time-series data:
```javascript
// Instead of one doc per measurement, bucket by hour
{
  sensorId: "S001",
  date: ISODate("2024-01-15T10:00:00Z"),
  measurements: [
    { time: "10:01", temp: 22.5 },
    { time: "10:02", temp: 22.6 },
    // ... up to 60 measurements per bucket
  ],
  count: 60
}
```

---

### Q9. MongoDB transactions — when do you need them?

```javascript
const session = client.startSession();
try {
  session.startTransaction();
  
  await accounts.updateOne({ _id: "A1" }, { $inc: { balance: -500 } }, { session });
  await accounts.updateOne({ _id: "A2" }, { $inc: { balance: 500 } }, { session });
  
  await session.commitTransaction();
} catch (e) {
  await session.abortTransaction();
} finally {
  session.endSession();
}
```

**Multi-document transactions** (since MongoDB 4.0) are supported but slower than single-document operations. **Design your schema so most operations are single-document** (embed related data).
