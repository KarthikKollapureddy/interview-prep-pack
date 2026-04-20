import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

/**
 * Challenge 1: Transaction Analyzer
 * 
 * Given: Transaction(String id, String merchantId, double amount, String status, LocalDateTime timestamp)
 * 
 * Implement all three methods using Java Streams.
 */
public class TransactionAnalyzer {

    // You can use this record (Java 17) or create a class
    record Transaction(String id, String merchantId, double amount, String status, LocalDateTime timestamp) {}

    /**
     * 1. Group by merchant, return DoubleSummaryStatistics for SUCCESS transactions only.
     */
    public Map<String, DoubleSummaryStatistics> summaryByMerchant(List<Transaction> txns) {
        // YOUR CODE HERE
        return txns.stream()
                .filter(txn->"SUCCESS".equals(txn.status))
                .collect(
                        Collectors.groupingBy(
                                Transaction::merchantId,
                                Collectors.summarizingDouble(Transaction::amount)
                        )
                );
    }
    // MENTOR REVIEW: summaryByMerchant()
    // ✅ CORRECT — Logic, collector choice, and filter are all spot-on.
    // ✅ GOOD: "SUCCESS".equals(txn.status) — null-safe comparison (avoids NPE if status is null). Sharp habit.
    // ✅ GOOD: groupingBy + summarizingDouble is the idiomatic one-pass approach.
    //
    // 🟡 MINOR: Consider using the record accessor txn.status() (method) instead of txn.status (field access).
    //    Both work on records, but txn.status() is the canonical accessor and more consistent with
    //    Transaction::merchantId method references you already use. Mixing styles can trip up reviewers.
    //
    // 🟡 STYLE: Add a space after txn-> for readability: txn -> "SUCCESS".equals(txn.status())
    //    FedEx/Hatio code reviews flag inconsistent lambda formatting.
    //
    // 💡 INTERVIEW EDGE: "What if txns is null?" — In production, guard with:
    //    if (txns == null || txns.isEmpty()) return Collections.emptyMap();
    //    Interviewers at product companies test for defensive coding at API boundaries.
    //
    // SCORE: 9/10 — Clean, correct, idiomatic.

    /**
     * 2. Return top N transactions by amount, across all statuses.
     */
    public List<Transaction> topNByAmount(List<Transaction> txns, int n) {
        // YOUR CODE HERE
        return txns.stream()
                .sorted(Comparator.comparingDouble(Transaction::amount).reversed())
                .limit(n)
                .toList();
    }
    // MENTOR REVIEW: topNByAmount()
    // ✅ CORRECT — sorted().limit(n).toList() is the textbook Streams approach.
    // ✅ GOOD: .reversed() on the comparator — correct for descending order.
    // ✅ GOOD: .toList() (Java 16+) — returns an unmodifiable list. Clean.
    //
    // ⚠️ COMPLEXITY: sorted() is O(n log n) for the full stream, then limit(n) takes top n.
    //    For interview follow-up: "Can you do this in O(n log k)?"
    //    Answer: Use a min-heap (PriorityQueue) of size k — but Streams API doesn't have a built-in
    //    top-K collector. You'd need a custom collector or imperative approach. Your answer is
    //    perfectly acceptable for interviews — just be ready to discuss the tradeoff.
    //
    // 🟡 EDGE CASE: What if n > txns.size()? limit(n) handles it gracefully (returns all elements).
    //    What if n <= 0? limit(0) returns empty list. Both are fine — but mention it if asked.
    //
    // 💡 INTERVIEW BONUS: If asked "what if amounts are equal?", your current impl returns
    //    arbitrary order among ties. To stabilize: chain a secondary sort:
    //    Comparator.comparingDouble(Transaction::amount).reversed()
    //        .thenComparing(Transaction::timestamp)
    //
    // SCORE: 9/10 — Correct and clean. Know the O(n log k) alternative for follow-up.

    /**
     * 3. Return a map where key = "merchantId:status", value = count.
     */
    public Map<String, Long> countByStatusPerMerchant(List<Transaction> txns) {
        // YOUR CODE HERE
        return txns.stream()
                .collect(
                        Collectors.toMap(
                                txn->txn.merchantId.concat(":").concat(txn.status),
                                txn->1L,
                                Long::sum,
                                HashMap::new


                        )
                );
    }
    // MENTOR REVIEW: countByStatusPerMerchant()
    // ✅ CORRECT — toMap with merge function Long::sum is a valid counting approach.
    // ✅ GOOD: You explicitly supplied HashMap::new as the map factory — shows you understand
    //    the 4-arg toMap signature. Most candidates forget this exists.
    //
    // 🔶 ALTERNATIVE (more idiomatic for counting):
    //    return txns.stream().collect(
    //        Collectors.groupingBy(
    //            txn -> txn.merchantId() + ":" + txn.status(),
    //            Collectors.counting()
    //        )
    //    );
    //    groupingBy + counting() is the canonical "count by key" pattern. Interviewers may ask
    //    "why not groupingBy?" — your toMap approach works, but groupingBy is more readable
    //    and signals intent ("I'm grouping and counting") vs toMap ("I'm building a map with merging").
    //
    // 🟡 MINOR: .concat(":").concat() works but string concatenation with + is more readable:
    //    txn.merchantId() + ":" + txn.status()
    //    The JVM optimizes both to StringBuilder in Java 9+. No performance difference.
    //
    // 🟡 MINOR: HashMap::new is the default for toMap anyway (implementation returns HashMap).
    //    Explicit is fine for clarity, but not strictly needed here.
    //
    // ⚠️ EDGE CASE: If merchantId or status is null, .concat() throws NPE.
    //    The + operator handles null ("null:SUCCESS"), but concat does not.
    //    In payment systems (Hatio/NPCI), null statuses can appear in error paths.
    //
    // SCORE: 8/10 — Correct but slightly non-idiomatic. Switch to groupingBy+counting() for interviews.

    public static void main(String[] args) {
        // Test your implementations here
    }
}
