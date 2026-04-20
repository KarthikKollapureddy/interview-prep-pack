# Elasticsearch — Interview Q&A

> 10 questions covering inverted index, mappings, query DSL, aggregations, sharding, Spring Data integration  
> Priority: **P2** — "Design a search feature" questions + FedEx logistics search use cases

---

### Q1. What is Elasticsearch and when should you use it?

**Answer:**

```
Elasticsearch = Distributed search & analytics engine built on Apache Lucene

Use when:
  ✅ Full-text search (product search, log search, document search)
  ✅ Log analytics (ELK stack: Elasticsearch + Logstash + Kibana)
  ✅ Auto-complete / suggestions
  ✅ Fuzzy matching ("iphone" finds "iPhone", "ifone")
  ✅ Geospatial search (find nearby restaurants, track shipments)
  ✅ Real-time analytics on large datasets

DON'T use when:
  ❌ Primary database (no ACID transactions)
  ❌ Relational queries (JOINs are limited)
  ❌ Strict consistency needed (eventual consistency by default)

Typical architecture:
  User types "red shoes" → API → Elasticsearch (full-text search) → return results
  Source of truth: PostgreSQL
  Search index: Elasticsearch (synced via CDC/events)
```

---

### Q2. What is an inverted index? How does Elasticsearch search so fast?

**Answer:**

```
Regular index (like B-tree in SQL):
  Document → Words
  Doc1: "The quick brown fox"
  Doc2: "The quick blue car"

Inverted index (Elasticsearch):
  Word → Documents
  "the"   → [Doc1, Doc2]
  "quick" → [Doc1, Doc2]
  "brown" → [Doc1]
  "fox"   → [Doc1]
  "blue"  → [Doc2]
  "car"   → [Doc2]

Search for "quick fox":
  "quick" → [Doc1, Doc2]
  "fox"   → [Doc1]
  Intersection + scoring → Doc1 is the best match

Why it's fast:
  - Hash lookup for each term → O(1) per term
  - Bitmap intersection for combining terms
  - Like a book's index: look up "Java" → page 42, 87, 156
```

---

### Q3. Explain index, document, mapping, and field types.

**Answer:**

```
SQL analogy:
  Database  → Index (e.g., "products")
  Table     → Type (deprecated, one per index now)
  Row       → Document (JSON)
  Column    → Field
  Schema    → Mapping
```

**Mapping (schema):**
```json
PUT /products
{
  "mappings": {
    "properties": {
      "name":        { "type": "text" },           // full-text search (analyzed)
      "brand":       { "type": "keyword" },         // exact match (not analyzed)
      "price":       { "type": "float" },
      "description": { "type": "text", "analyzer": "english" },
      "category":    { "type": "keyword" },
      "created_at":  { "type": "date" },
      "location":    { "type": "geo_point" },
      "in_stock":    { "type": "boolean" },
      "tags":        { "type": "keyword" }          // arrays supported natively
    }
  }
}
```

**text vs keyword — the most important distinction:**
```
"text" field:
  - Analyzed (tokenized, lowercased, stemmed)
  - "Quick Brown Fox" → ["quick", "brown", "fox"]
  - Used for: full-text search, match queries
  - Cannot be used for: sorting, aggregations

"keyword" field:
  - NOT analyzed (stored as-is)
  - "Quick Brown Fox" → ["Quick Brown Fox"]
  - Used for: exact match, filtering, sorting, aggregations
  - Example: email, status, category, ID
```

---

### Q4. Explain analyzers — standard, custom, and how tokenization works.

**Answer:**

```
Analyzer = Character Filters → Tokenizer → Token Filters

Standard analyzer (default):
  Input:  "The Quick-Brown FOX jumped!"
  Step 1: Tokenizer → ["The", "Quick", "Brown", "FOX", "jumped"]
  Step 2: Lowercase  → ["the", "quick", "brown", "fox", "jumped"]

English analyzer:
  Input:  "The foxes were running quickly"
  Step 1: Tokenizer → ["The", "foxes", "were", "running", "quickly"]
  Step 2: Lowercase  → ["the", "foxes", "were", "running", "quickly"]
  Step 3: Stop words → ["foxes", "running", "quickly"]
  Step 4: Stemming   → ["fox", "run", "quick"]
  → Now "running" matches "run", "foxes" matches "fox"!

Custom analyzer example (e-commerce):
```

```json
PUT /products
{
  "settings": {
    "analysis": {
      "analyzer": {
        "product_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "synonym_filter", "english_stemmer"]
        }
      },
      "filter": {
        "synonym_filter": {
          "type": "synonym",
          "synonyms": ["laptop, notebook", "phone, mobile, cell"]
        }
      }
    }
  }
}
```

---

### Q5. Query DSL — match, term, bool, range queries.

**Answer:**

```json
// match — full-text search (uses analyzer):
GET /products/_search
{
  "query": {
    "match": {
      "name": "red running shoes"
    }
  }
}
// Analyzes "red running shoes" → ["red", "running", "shoes"]
// Finds docs containing ANY of these terms, scores by relevance

// term — exact match (NO analysis, use for keyword fields):
GET /products/_search
{
  "query": {
    "term": {
      "category": "Electronics"
    }
  }
}

// bool — combine queries (must = AND, should = OR, must_not = NOT):
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "laptop" } }
      ],
      "filter": [
        { "range": { "price": { "gte": 500, "lte": 2000 } } },
        { "term": { "brand": "Dell" } },
        { "term": { "in_stock": true } }
      ],
      "should": [
        { "match": { "description": "gaming" } }
      ],
      "must_not": [
        { "term": { "category": "Refurbished" } }
      ]
    }
  }
}

// filter vs must:
//   must    → contributes to relevance score
//   filter  → yes/no only, cached, faster (use for exact conditions)

// Multi-match (search across multiple fields):
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "wireless headphones",
      "fields": ["name^3", "description", "tags^2"],
      "type": "best_fields"
    }
  }
}
// name^3 means name field is 3x more important for scoring
```

---

### Q6. How does relevance scoring work (BM25)?

**Answer:**

```
Elasticsearch uses BM25 (Best Matching 25) algorithm:

Score depends on:
  1. Term Frequency (TF): How often the term appears in the document
     - "java" appears 5 times → higher TF → higher score
     
  2. Inverse Document Frequency (IDF): How rare the term is across all documents
     - "java" in 10/1000 docs → rare → high IDF → important
     - "the" in 990/1000 docs → common → low IDF → not important
     
  3. Field Length: Shorter fields get higher scores
     - "Java programming" (2 words) scores higher than 
       "Introduction to Java programming basics for beginners" (7 words)
     - for the query "Java"

Practical scoring tips:
  - Use field boosting: "name^3" gives name field 3x weight
  - Use function_score for custom scoring (e.g., boost recent products)
  - Use explain API to debug why a doc scored high/low
```

```json
// Explain scoring:
GET /products/_explain/1
{
  "query": { "match": { "name": "laptop" } }
}
```

---

### Q7. Aggregations — analytics in Elasticsearch.

**Answer:**

```json
// Terms aggregation (GROUP BY):
GET /products/_search
{
  "size": 0,
  "aggs": {
    "brands": {
      "terms": { "field": "brand", "size": 10 }
    }
  }
}
// Result: { "brands": { "buckets": [{"key": "Apple", "doc_count": 150}, ...] } }

// Date histogram (time-series):
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "orders_per_month": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_revenue": {
          "sum": { "field": "amount" }
        }
      }
    }
  }
}

// Faceted search (e-commerce filters):
GET /products/_search
{
  "query": { "match": { "name": "laptop" } },
  "aggs": {
    "brand_filter":   { "terms": { "field": "brand" } },
    "price_ranges":   { "range": { "field": "price", "ranges": [
      { "to": 500 }, { "from": 500, "to": 1000 }, { "from": 1000 }
    ]}},
    "avg_price":      { "avg": { "field": "price" } }
  }
}
// This gives search results + filter options (like Amazon sidebar)
```

---

### Q8. Sharding and replication — how Elasticsearch scales.

**Answer:**

```
Index → Shards (horizontal partitioning)

products index (5 shards, 1 replica):
  Node-1: [Shard-0 primary] [Shard-3 replica]
  Node-2: [Shard-1 primary] [Shard-4 replica]
  Node-3: [Shard-2 primary] [Shard-0 replica]
  Node-4: [Shard-3 primary] [Shard-1 replica]
  Node-5: [Shard-4 primary] [Shard-2 replica]

Primary shard: handles writes + reads
Replica shard: handles reads only, provides redundancy

Search flow:
  1. Query hits coordinator node
  2. Coordinator fans out to ALL shards (primary or replica)
  3. Each shard returns local top results
  4. Coordinator merges, re-ranks, returns final results
  
Write flow:
  1. Write goes to primary shard (determined by: hash(docId) % numShards)
  2. Primary writes, then replicates to replica shard(s)
  3. Returns success

Shard sizing guidelines:
  - 10-50 GB per shard (sweet spot)
  - Too many small shards → overhead
  - Too few large shards → can't distribute well
  - Number of shards CANNOT be changed after index creation
    (use _reindex to migrate)
```

---

### Q9. Spring Data Elasticsearch integration.

**Answer:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: changeme
```

```java
@Document(indexName = "products")
@Setting(shards = 3, replicas = 1)
public class Product {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "english")
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Float)
    private Float price;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> tags;
}

// Repository:
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByName(String name);
    List<Product> findByCategory(String category);
    List<Product> findByPriceBetween(Float min, Float max);
}

// Custom query with ElasticsearchOperations:
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ElasticsearchOperations elasticsearchOps;

    public SearchHits<Product> search(String query, String category,
                                       Float minPrice, Float maxPrice) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        boolQuery.must(m -> m.multiMatch(mm -> mm
            .query(query)
            .fields("name^3", "description", "tags^2")));

        if (category != null) {
            boolQuery.filter(f -> f.term(t -> t
                .field("category").value(category)));
        }

        if (minPrice != null && maxPrice != null) {
            boolQuery.filter(f -> f.range(r -> r
                .field("price")
                .from(minPrice.toString())
                .to(maxPrice.toString())));
        }

        NativeQuery searchQuery = NativeQuery.builder()
            .withQuery(q -> q.bool(boolQuery.build()))
            .withSort(Sort.by(Sort.Direction.DESC, "_score"))
            .withPageable(PageRequest.of(0, 20))
            .build();

        return elasticsearchOps.search(searchQuery, Product.class);
    }
}
```

---

### Q10. Design a search feature for an e-commerce app.

**Answer:**

```
Architecture:
  PostgreSQL (source of truth) → CDC / Events → Elasticsearch (search index)
                                                         ↑
  Product Service → Kafka "product-updated" → Search Indexer → Elasticsearch
  
  User → Search API → Elasticsearch query → Return ranked results

Sync strategy (keep ES in sync with DB):
  Option 1: Dual write (write to DB + ES) → risk of inconsistency
  Option 2: Event-driven (publish event → consumer indexes to ES) ✅ recommended
  Option 3: CDC (Debezium captures DB changes → Kafka → ES)

Search API features:
  - Full-text search: match query on name, description
  - Filters: category, brand, price range, in_stock (filter context, cached)
  - Facets: aggregations for sidebar filters
  - Autocomplete: completion suggester or edge_ngram analyzer
  - Fuzzy search: handle typos ("iphne" → "iphone")
  - Sorting: by relevance, price, rating, newest
  - Pagination: search_after (not from/size for deep pages)

Performance tips:
  - Use filter context (cached) for exact conditions
  - Keep mapping minimal (don't index fields you won't search)
  - Use routing for multi-tenant data (route by tenant_id to same shard)
  - Warm up frequently searched queries
  - Separate hot/warm/cold indices for time-series data
```

---

## Quick Reference

| Concept | Key Point |
|---------|-----------|
| **Inverted Index** | Word → Document list (O(1) lookup per term) |
| **text vs keyword** | text = analyzed (search), keyword = exact (filter/sort) |
| **match vs term** | match = full-text (analyzed), term = exact (not analyzed) |
| **bool query** | must=AND, should=OR, must_not=NOT, filter=exact+cached |
| **BM25** | TF × IDF × field length normalization |
| **Shards** | Horizontal partitioning, 10-50GB each, fixed at creation |
| **Replicas** | Redundancy + read throughput |
| **Aggregations** | Analytics: terms, date_histogram, avg, sum |
| **Sync strategy** | Event-driven (DB → Kafka → Elasticsearch) |
