# Java Internals & Memory Management

> Deep-dive into JVM, GC, Memory Model, ClassLoaders, String Pool  
> **Critical for product-based companies** — nearly every interview includes 3-5 JVM questions

## Topics Covered

| # | Topic | Difficulty | Priority |
|---|-------|-----------|----------|
| 1 | JVM Architecture | Medium | 🔴 Must Know |
| 2 | Stack vs Heap | Easy | 🔴 Must Know |
| 3 | Memory Areas (Method Area, Heap, Stack, PC, Native) | Medium | 🔴 Must Know |
| 4 | Generational GC (Young, Old, Metaspace) | Hard | 🔴 Must Know |
| 5 | GC Algorithms (Serial, Parallel, G1, ZGC) | Hard | 🟡 Good to Know |
| 6 | GC Roots & Reachability | Medium | 🔴 Must Know |
| 7 | Memory Leaks in Java | Medium | 🔴 Must Know |
| 8 | String Pool & Immutability | Easy | 🔴 Must Know |
| 9 | String vs StringBuilder vs StringBuffer | Easy | 🔴 Must Know |
| 10 | ClassLoader Hierarchy | Medium | 🟡 Good to Know |
| 11 | Tricky Output Questions | Medium | 🔴 Must Know |
| 12 | JVM Tuning Flags | Hard | 🟡 Good to Know |
| 13 | OOM Diagnosis | Hard | 🟡 Good to Know |
| 14 | equals/hashCode Contract | Medium | 🔴 Must Know |
| 15 | Serialization & serialVersionUID | Medium | 🟡 Good to Know |
| 16 | Generics & Type Erasure | Hard | 🔴 Must Know |
| 17 | Java Memory Model (JMM) | Hard | 🔴 Must Know |
| 18 | final keyword | Easy | 🔴 Must Know |
| 19 | Immutable Classes | Medium | 🔴 Must Know |

## Quick Reference

See [qa.md](qa.md) for all 25 questions with detailed answers and diagrams.

## Study Strategy

1. **Day 1-2:** JVM Architecture + Memory Areas + Stack vs Heap (Q1-Q3)
2. **Day 3-4:** GC deep dive — generational model, algorithms, GC roots (Q4-Q6)
3. **Day 5:** String Pool + Immutability + String/SB/SBuffer (Q8-Q9)
4. **Day 6:** Memory leaks + JVM tuning + OOM diagnosis (Q7, Q16-Q17)
5. **Day 7:** Tricky output questions + equals/hashCode (Q11-Q14, Q18)
6. **Day 8:** Generics, JMM, Concurrency internals (Q20-Q21)
7. **Day 9:** Practice — explain each topic out loud in 2 minutes (mock interview)
