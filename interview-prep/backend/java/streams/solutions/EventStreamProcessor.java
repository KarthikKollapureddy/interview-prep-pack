import java.time.*;
import java.util.*;
import java.util.stream.*;

/**
 * Challenge 2: Event Stream Processor
 * 
 * Given: ScanEvent(String trackingNumber, String facility, Instant timestamp, String eventType)
 * 
 * Implement all three methods using Java Streams.
 */
public class EventStreamProcessor {

    public record ScanEvent(String trackingNumber, String facility, Instant timestamp, String eventType) {}

    /**
     * 1. Deduplicate, keeping only the latest event per tracking number.
     */
    public Map<String, ScanEvent> latestEventPerPackage(List<ScanEvent> events) {
        // YOUR CODE HERE
//        return null;
        return events.stream()
                .distinct()
                .collect(
                        Collectors.toMap(
                                ScanEvent::trackingNumber,
                                event->event,
                                (existingEvent, updatedEvent)->{
                                    int sorter = updatedEvent.timestamp().compareTo(existingEvent.timestamp());
                                    return sorter<0?existingEvent:updatedEvent; // keep latest check isAfter
                                }
                        )
                );

    }
    // MENTOR REVIEW: latestEventPerPackage
    // ✅ Merge function logic is correct — when sorter >= 0 (updatedEvent is later or equal),
    //    returns updatedEvent. When sorter < 0 (updatedEvent is earlier), keeps existingEvent. Good.
    //
    // ⚠️ UNNECESSARY: distinct() before toMap is redundant here.
    //    - distinct() on records uses equals() which compares ALL fields (trackingNumber + facility
    //      + timestamp + eventType). It only removes exact duplicate ScanEvent objects.
    //    - toMap's merge function already handles multiple events with the same trackingNumber.
    //    - Remove distinct() — it adds O(n) overhead (builds an internal HashSet) with no benefit.
    //
    // 💡 STYLE: The qa.md reference solution uses Instant::isAfter which reads more clearly:
    //      (existing, replacement) ->
    //          existing.timestamp().isAfter(replacement.timestamp()) ? existing : replacement
    //    This avoids the int variable and compareTo, making intent immediately obvious.
    //
    // ✅ Complexity: O(n) time, O(k) space where k = unique tracking numbers. Correct.

    /**
     * 2. For each tracking number, return the ordered list of facilities it passed through (sorted by timestamp).
     */
    public Map<String, List<String>> facilityRouteMap(List<ScanEvent> events) {
        // YOUR CODE HERE
//        return null;
        return events.stream()
                .sorted(Comparator.comparing(ScanEvent::timestamp))
                .collect(
                        Collectors.toMap(
                                ScanEvent::trackingNumber,
                                event->new ArrayList<>(List.of(event.facility())),
                                (existing, updated)->{
                                     existing.addAll(updated);
                                     return existing;
                                }

                        )
                );
    }
    // MENTOR REVIEW: facilityRouteMap (re-reviewed after fix)
    // ✅ Sort key fixed — now correctly sorts by ScanEvent::timestamp. Good.
    //
    // ✅ Merge function (addAll) correctly combines facility lists per tracking number.
    //
    // ⚠️ ORDERING CAVEAT with toMap: The stream is sorted, but toMap processes elements in
    //    encounter order only if the stream is sequential. This works because:
    //    - The stream is sequential (no .parallel())
    //    - sorted() establishes a total order before toMap sees the elements
    //    - The merge function appends (addAll), preserving that order
    //    In a parallel stream, toMap's merge could interleave — the ordering guarantee breaks.
    //    Worth noting in an interview if asked about parallelism.
    //
    // 💡 STYLE: groupingBy is more idiomatic for 1:many groupings and avoids manual ArrayList creation:
    //      return events.stream()
    //          .sorted(Comparator.comparing(ScanEvent::timestamp))
    //          .collect(Collectors.groupingBy(
    //              ScanEvent::trackingNumber,
    //              Collectors.mapping(ScanEvent::facility, Collectors.toList())
    //          ));
    //    However, groupingBy does NOT guarantee element ordering within groups from a sorted stream
    //    in all implementations. Your toMap approach actually has a stronger ordering guarantee
    //    for sequential streams, so it's a valid choice.
    //
    // ✅ Complexity: O(n log n) time (sort), O(n) space. Correct.

    /**
     * 3. Find tracking numbers where the latest event is older than the given threshold.
     */
    public List<String> findStuckPackages(List<ScanEvent> events, Duration threshold) {
        // YOUR CODE HERE
//        return null;
        return latestEventPerPackage(events).values().stream()
                .filter(scanEvent -> Duration.ofSeconds(scanEvent.timestamp().getEpochSecond()).compareTo(threshold) > 0)
                .map(ScanEvent::trackingNumber)
                .toList();


    }
    // MENTOR REVIEW: findStuckPackages
    // 🔴 BUG: Filter logic is semantically wrong.
    //    Duration.ofSeconds(scanEvent.timestamp().getEpochSecond()) converts the absolute epoch
    //    timestamp (e.g., ~1.7 billion seconds since 1970-01-01) into a Duration. This is NOT
    //    the "age" of the event — it's the total seconds since Unix epoch treated as a duration.
    //    Comparing that to threshold (e.g., 24 hours = 86400s) will almost ALWAYS be true,
    //    so every package looks "stuck".
    //
    //    FIX: Calculate the actual age of the event by comparing to Instant.now():
    //        .filter(scanEvent -> Duration.between(scanEvent.timestamp(), Instant.now()).compareTo(threshold) > 0)
    //    Or equivalently:
    //        .filter(scanEvent -> scanEvent.timestamp().isBefore(Instant.now().minus(threshold)))
    //    The second form is cleaner — "is the event's timestamp before (now - threshold)?"
    //
    // ✅ Good: Reusing latestEventPerPackage() to get the latest event per tracking number
    //    avoids duplicating logic. This is clean composition.
    //
    // ✅ Good: Using .toList() (Java 16+) instead of .collect(Collectors.toList()) — concise.

    public static void main(String[] args) {
        // Test your implementations here
    }
}

// ============================================================================
// CONCEPTUAL GUIDE: Instant & Duration (java.time)
// ============================================================================
//
// 📌 INSTANT — a point on the timeline
// ----------------------------------------------------------------------------
// - Represents a specific moment in UTC (like a timestamp).
// - Internally stored as: seconds since 1970-01-01T00:00:00Z (epoch) + nanoseconds.
// - Think of it as "WHEN something happened".
//
//   Instant now    = Instant.now();                          // current moment
//   Instant parsed = Instant.parse("2025-04-22T10:30:00Z"); // from ISO string
//   long epoch     = now.getEpochSecond();                   // seconds since epoch (~1.7 billion)
//
// Key methods:
//   now.isAfter(parsed)          → true/false (compare two points in time)
//   now.isBefore(parsed)         → true/false
//   now.plus(Duration.ofHours(2))  → new Instant 2 hours later
//   now.minus(Duration.ofDays(1))  → new Instant 1 day earlier
//
// ❌ COMMON MISTAKE (what happened in findStuckPackages):
//   Duration.ofSeconds(instant.getEpochSecond())
//   This takes ~1.7 BILLION seconds (the epoch value) and wraps it as a Duration.
//   That's ~54 YEARS as a duration — NOT the "age" of the event.
//   The epoch value is a POSITION on the timeline, not an ELAPSED amount.
//
//
// 📌 DURATION — an amount of time between two points
// ----------------------------------------------------------------------------
// - Represents a length of time (hours, minutes, seconds, nanos).
// - Think of it as "HOW LONG between two events" or "HOW LONG to wait".
//
//   Duration d1 = Duration.ofHours(24);                     // 24 hours
//   Duration d2 = Duration.ofMinutes(30);                   // 30 minutes
//   Duration d3 = Duration.between(startInstant, endInstant); // elapsed time
//
// Key methods:
//   d1.toHours()        → 24
//   d1.toMinutes()      → 1440
//   d1.compareTo(d2)    → positive (d1 > d2)
//   d1.plus(d2)         → new Duration (24h + 30m)
//   d1.isNegative()     → true if start is AFTER end in Duration.between()
//
//
// 📌 HOW TO COMPUTE "AGE" OF AN EVENT (the correct pattern)
// ----------------------------------------------------------------------------
//
//   Instant eventTime = scanEvent.timestamp();
//   Instant now       = Instant.now();
//
//   // Option 1: Duration.between — gives you the elapsed Duration
//   Duration age = Duration.between(eventTime, now);   // e.g., PT48H (48 hours)
//   boolean isStuck = age.compareTo(threshold) > 0;    // age > threshold?
//
//   // Option 2: isBefore — simpler boolean check
//   Instant cutoff = now.minus(threshold);              // e.g., now - 24h
//   boolean isStuck = eventTime.isBefore(cutoff);      // event older than cutoff?
//
//   // Option 2 reads as: "did this event happen before the cutoff?"
//   //   cutoff = April 21 10:00 AM (now minus 24h)
//   //   eventTime = April 20 08:00 AM → isBefore(cutoff) → true → stuck!
//
//
// 📌 MENTAL MODEL: Instant vs Duration
// ----------------------------------------------------------------------------
//   Instant  = a POINT     (April 22, 2026 at 10:30:00 UTC)  — WHERE on the timeline
//   Duration = a LENGTH    (48 hours, 30 minutes)             — HOW MUCH time
//
//   You CANNOT compare an Instant to a Duration directly — they're different units.
//   You CAN:
//     - Subtract two Instants to get a Duration:  Duration.between(a, b)
//     - Add/subtract a Duration from an Instant:  instant.plus(duration)
//     - Compare two Durations:                    duration1.compareTo(duration2)
//     - Compare two Instants:                     instant1.isAfter(instant2)
//
//
// 📌 QUICK REFERENCE TABLE
// ----------------------------------------------------------------------------
//   | Want to...                        | Use                                    |
//   |-----------------------------------|----------------------------------------|
//   | Get current time                  | Instant.now()                          |
//   | Parse a timestamp string          | Instant.parse("2025-01-01T00:00:00Z") |
//   | Time between two events           | Duration.between(start, end)           |
//   | Check if event is "too old"       | event.isBefore(Instant.now().minus(d)) |
//   | Create a fixed duration           | Duration.ofHours(24), ofMinutes(30)    |
//   | Shift a timestamp forward/back    | instant.plus(d) / instant.minus(d)     |
//   | Compare which event is newer      | a.isAfter(b) or a.compareTo(b) > 0    |
// ============================================================================


// ============================================================================
// CONCEPTUAL GUIDE: Collectors.toMap() vs Collectors.groupingBy()
// ============================================================================
//
// You used toMap() for both problems 1 and 2. It worked, but problem 2 was a
// case where groupingBy() is the natural fit. Knowing when to pick which is
// a common interview differentiator.
//
// 📌 Collectors.toMap() — ONE value per key
// ----------------------------------------------------------------------------
// Use when: each key maps to exactly ONE value (1:1 or deduplicated).
//
//   // Problem 1: one latest ScanEvent per trackingNumber → toMap is perfect
//   Collectors.toMap(
//       ScanEvent::trackingNumber,    // key
//       event -> event,               // value (single event)
//       (old, new) -> ...             // merge: resolve duplicates → keep one
//   )
//
// The merge function (3rd arg) is called ONLY when two entries have the same key.
// It receives the existing value and the new value, and you return the winner.
//
// Without a merge function, duplicate keys → IllegalStateException!
//   Map<String, ScanEvent> map = events.stream()
//       .collect(Collectors.toMap(ScanEvent::trackingNumber, e -> e));
//   // ❌ throws if two events share a trackingNumber
//
//
// 📌 Collectors.groupingBy() — MANY values per key
// ----------------------------------------------------------------------------
// Use when: each key maps to a COLLECTION of values (1:many).
//
//   // Problem 2: multiple facilities per trackingNumber → groupingBy is natural
//   Collectors.groupingBy(
//       ScanEvent::trackingNumber,                              // key
//       Collectors.mapping(ScanEvent::facility, Collectors.toList())  // downstream
//   )
//
// groupingBy automatically creates a List (or whatever the downstream collector produces)
// for each key. No manual ArrayList creation, no merge function needed.
//
//
// 📌 DECISION RULE
// ----------------------------------------------------------------------------
//   Ask: "For each key, do I want ONE value or MANY values?"
//
//   ONE value per key   → toMap()      + merge function for conflict resolution
//   MANY values per key → groupingBy() + downstream collector (mapping, counting, etc.)
//
//   Your problem 2 approach (toMap + new ArrayList + addAll merge) works, but it's
//   reinventing what groupingBy does internally. In an interview, using groupingBy
//   signals you know the Collectors API well.
//
//
// 📌 DOWNSTREAM COLLECTORS (groupingBy's power tool)
// ----------------------------------------------------------------------------
//   groupingBy takes an optional second argument — a "downstream collector" that
//   determines what to do with the grouped elements:
//
//   Collectors.groupingBy(keyFn, Collectors.toList())          → Map<K, List<V>>
//   Collectors.groupingBy(keyFn, Collectors.counting())        → Map<K, Long>
//   Collectors.groupingBy(keyFn, Collectors.summingDouble(fn)) → Map<K, Double>
//   Collectors.groupingBy(keyFn, Collectors.mapping(fn, toList())) → Map<K, List<R>>
//   Collectors.groupingBy(keyFn, Collectors.maxBy(comparator)) → Map<K, Optional<V>>
//
//   You can even NEST them:
//   Collectors.groupingBy(outerKey,
//       Collectors.groupingBy(innerKey, Collectors.counting()))
//   → Map<K1, Map<K2, Long>>  (like the FedEx facility→shift→scanType report in qa.md Q10)
//
// ============================================================================


// ============================================================================
// CONCEPTUAL GUIDE: distinct() on Records & When It's Redundant
// ============================================================================
//
// In problem 1, you used .distinct() before .collect(toMap(...)). This was unnecessary.
//
// 📌 WHAT distinct() DOES
// ----------------------------------------------------------------------------
// - Removes duplicate elements from the stream.
// - Uses .equals() and .hashCode() to determine duplicates.
// - Internally maintains a HashSet of seen elements → O(n) extra memory.
//
// 📌 HOW distinct() WORKS ON RECORDS
// ----------------------------------------------------------------------------
// Java records auto-generate equals() and hashCode() based on ALL fields.
//
//   record ScanEvent(String trackingNumber, String facility,
//                    Instant timestamp, String eventType) {}
//
//   // Two ScanEvents are equal ONLY if ALL 4 fields match:
//   new ScanEvent("PKG1", "FacA", t1, "PICKUP")
//       .equals(new ScanEvent("PKG1", "FacA", t1, "PICKUP"))  → true
//
//   new ScanEvent("PKG1", "FacA", t1, "PICKUP")
//       .equals(new ScanEvent("PKG1", "FacB", t2, "SORTED"))  → false
//       // same trackingNumber but different facility/timestamp/eventType → NOT equal
//
// 📌 WHY IT WAS REDUNDANT IN PROBLEM 1
// ----------------------------------------------------------------------------
// You wanted to keep one event per trackingNumber (same key = same trackingNumber).
// But distinct() only removes events where ALL fields match (exact duplicates).
// Two events with the same trackingNumber but different timestamps are NOT duplicates
// to distinct(). They pass through, and toMap's merge function handles them anyway.
//
// So distinct() only filters out exact clones — which rarely happen in real scan data.
// Meanwhile, toMap's merge function already handles the "multiple events per trackingNumber"
// case. Result: distinct() adds overhead (HashSet) with no practical effect.
//
// 📌 WHEN distinct() IS USEFUL
// ----------------------------------------------------------------------------
//   - Removing exact duplicates: List.of("a", "b", "a") → ["a", "b"]
//   - After flatMap that might produce repeated values
//   - When you DON'T have a downstream merge/grouping that already handles duplicates
//
// ============================================================================


// ============================================================================
// CONCEPTUAL GUIDE: Comparator.comparing() — Picking the Right Key
// ============================================================================
//
// In problem 2, you originally sorted by ScanEvent::eventType (alphabetical)
// instead of ScanEvent::timestamp (chronological). This is an easy mistake when
// you're writing fast, but it changes the entire result.
//
// 📌 HOW Comparator.comparing() WORKS
// ----------------------------------------------------------------------------
//   Comparator.comparing(ScanEvent::timestamp)
//   // Extracts the timestamp field, compares using its natural order (Instant implements Comparable)
//
//   Comparator.comparing(ScanEvent::eventType)
//   // Extracts the eventType field, compares using String's natural order (alphabetical)
//   // "DELIVERED" < "IN_TRANSIT" < "PICKUP" < "SORTED" — meaningless for chronology!
//
// 📌 THE KEY SELECTOR IS EVERYTHING
// ----------------------------------------------------------------------------
// Comparator.comparing(keyExtractor) sorts by whatever the key extractor returns.
// Always ask: "What am I actually sorting BY?"
//
//   .sorted(Comparator.comparing(ScanEvent::timestamp))     // by time ✅
//   .sorted(Comparator.comparing(ScanEvent::facility))      // by facility name alphabetically
//   .sorted(Comparator.comparing(ScanEvent::eventType))     // by event type alphabetically ❌ (not time!)
//
// 📌 COMMON COMPARATOR PATTERNS
// ----------------------------------------------------------------------------
//   // Natural order (ascending)
//   Comparator.comparing(Event::getTimestamp)
//
//   // Reverse order (descending)
//   Comparator.comparing(Event::getTimestamp).reversed()
//
//   // Multiple fields (tiebreaker)
//   Comparator.comparing(Event::getTimestamp)
//             .thenComparing(Event::getFacility)
//
//   // Null-safe
//   Comparator.comparing(Event::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
//
//   // Custom comparator for the extracted key
//   Comparator.comparing(Event::getName, String.CASE_INSENSITIVE_ORDER)
//
// 📌 INTERVIEW TIP
// ----------------------------------------------------------------------------
// When an interviewer says "sorted by X", double-check your Comparator extracts X.
// Read it back to yourself: "I'm sorting by [field] in [ascending/descending] order."
// ============================================================================
