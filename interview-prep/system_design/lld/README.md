# Low-Level Design (LLD)

## Status: [ ] Not started
## Score: —/10
## Last Reviewed: —

## Where to Start
1. **First:** Master SOLID principles (qa.md Q1) — this is the foundation
2. **Then:** Learn top 5 patterns: Strategy, Observer, Factory, State, Builder
3. **Practice:** Work through `end_to_end_designs.md` — 5+1 full class designs with working Java code
4. **Code it:** Don't just read — open IDE, implement from scratch, then compare

## Key Concepts
- SOLID principles (asked in every LLD round)
- Design Patterns (Strategy, State, Observer, Factory, Builder)
- Class relationships (composition over inheritance)
- Thread safety for shared resources
- State machines for status/lifecycle management
- Enums over strings for types and states

## Study Order
```
Start here (most asked):
  1. Parking Lot — end_to_end_designs.md (every company asks this)
  2. Vending Machine — end_to_end_designs.md (State pattern showcase)
  3. Snake & Ladder — end_to_end_designs.md (game loop design)

Then:
  4. Library Management — end_to_end_designs.md (Observer, reservation)
  5. Hotel Booking — end_to_end_designs.md (date overlap, cancellation)
  6. ATM Machine — end_to_end_designs.md (bonus, State pattern)

Also in qa.md:
  7. Rate Limiter (Q7) — Token Bucket algorithm
  8. LRU Cache (Q8) — HashMap + DLL
  9. Elevator System (Q11) — LOOK algorithm
  10. Splitwise (Q12) — debt simplification
  11. BookMyShow (Q13) — seat locking
```

## Files
- [qa.md](qa.md) — 13 LLD Q&A (SOLID, patterns, quick designs)
- [end_to_end_designs.md](end_to_end_designs.md) — ⭐ 5+1 full end-to-end designs (deep dive with Java code)
- [solutions/](solutions/) — Your implementations
