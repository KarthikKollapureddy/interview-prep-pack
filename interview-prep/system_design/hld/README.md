# High-Level Design (HLD)

## Status: [ ] Not started
## Score: —/10
## Last Reviewed: —

## Where to Start
1. **First:** Read the 5-step HLD framework at the top of `qa.md`
2. **Then:** Study `end_to_end_designs.md` — 5 full system designs with architecture, trade-offs, and bottleneck analysis
3. **Practice:** Pick one design, try it yourself on paper (30 min), then compare
4. **Key:** Focus on TRADE-OFFS — that's what separates good from great answers

## Key Concepts
- Back-of-envelope estimation (QPS, storage, bandwidth)
- Database selection (SQL vs NoSQL vs Cache vs Search)
- Caching strategies (CDN, Redis, cache-aside)
- Message queues (Kafka) for async processing
- Sharding, replication, consistent hashing
- CAP theorem and real-world trade-offs

## Study Order
```
Start here (most asked):
  1. URL Shortener — end_to_end_designs.md
  2. E-Commerce (Amazon) — end_to_end_designs.md
  3. Food Delivery (Swiggy) — end_to_end_designs.md

Then (senior-level):
  4. Ride-sharing (Uber) — end_to_end_designs.md
  5. Video Streaming (Netflix) — end_to_end_designs.md

Company-specific (from qa.md):
  6. Package Tracking (FedEx) — qa.md Q1
  7. UPI Payment (NPCI) — qa.md Q2
  8. Chat System — qa.md Q9
```

## Files
- [qa.md](qa.md) — 12 system design Q&A (quick format)
- [end_to_end_designs.md](end_to_end_designs.md) — ⭐ 5 full end-to-end designs (deep dive)
- [solutions/](solutions/) — Your design notes and diagrams
