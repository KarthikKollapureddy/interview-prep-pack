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
//        return null;
        return txns.stream()
                .filter(txn -> Objects.nonNull(txn) && "SUCCESS".equals(txn.status()))
                .collect(
                        Collectors.groupingBy(
                                Transaction::merchantId,
                                Collectors.summarizingDouble(Transaction::amount)
                        )
                );
    }

    // MENTOR REVIEW — summaryByMerchant():
    // ✅ CORRECT: "SUCCESS".equals(txn.status()) — safe against null status (avoids NPE).
    // ✅ CORRECT: Objects.nonNull(txn) null guard is solid defensive coding.
    // ✅ CORRECT: groupingBy + summarizingDouble is the idiomatic collector combo here.
    // 💡 STYLE: In an interview, mention you chose Collectors.summarizingDouble over
    //    Collectors.averagingDouble because it gives count, sum, min, max, avg — more info.
    // 💡 EDGE CASE: If a merchant has only FAILED txns, they won't appear in the result map.
    //    If the interviewer asks "what if I want all merchants?", you'd need a pre-populated map
    //    or a separate stream pass. Good to mention proactively.
    // 🏆 VERDICT: Production-ready. Clean and correct.

    /**
     * 2. Return top N transactions by amount, across all statuses.
     */
    public List<Transaction> topNByAmount(List<Transaction> txns, int n) {
        // YOUR CODE HERE
//        return null;
        return txns.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Transaction::amount).reversed())
                .limit(n)
                .toList();
    }

    // MENTOR REVIEW — topNByAmount():
    // ✅ CORRECT: Comparator.comparingDouble(Transaction::amount).reversed() — perfect idiomatic approach.
    // ✅ CORRECT: .toList() returns an unmodifiable list (Java 16+). Good for immutability.
    // 💡 NOTE: .toList() vs .collect(Collectors.toList()) — .toList() is unmodifiable,
    //    Collectors.toList() is modifiable. If interviewer asks the difference, this is a strong signal.
    // 💡 EDGE CASE: If n > txns.size(), limit() simply returns all elements — no error. Good to mention.
    // 💡 ALTERNATIVE: For very large lists, a PriorityQueue (min-heap of size n) would be O(N log K)
    //    vs O(N log N) for full sort. Worth mentioning for scale discussions at NPCI/FedEx.
    // 🏆 VERDICT: Clean and correct.


    /**
     * 3. Return a map where key = "merchantId:status", value = count.
     */
    public Map<String, Long> countByStatusPerMerchant(List<Transaction> txns) {
        // YOUR CODE HERE
//        return null;
        return txns.stream()
                .filter(Objects::nonNull)
                .collect(
                        Collectors.groupingBy(
                                txn -> txn.merchantId()+":"+txn.status(),
                                Collectors.counting()
                        )
                );
    }

    // MENTOR REVIEW — countByStatusPerMerchant():
    // ✅ CORRECT: groupingBy with a composite key string + Collectors.counting() is the right approach.
    // ✅ CORRECT: Null guard with Objects::nonNull.
    // 💡 STYLE: The composite key "merchantId:status" works, but in production you'd typically use
    //    a record/class as the key or a nested Map<String, Map<String, Long>> via:
    //    Collectors.groupingBy(Transaction::merchantId, Collectors.groupingBy(Transaction::status, Collectors.counting()))
    //    Mention the nested groupingBy alternative in an interview — shows depth.
    // 💡 EDGE CASE: If merchantId or status contains ":", the key becomes ambiguous. Minor point
    //    but shows attention to detail if you mention it.
    // 🏆 VERDICT: Correct and clean. The string key approach is acceptable for interview scope.

    // MENTOR REVIEW — Overall TransactionAnalyzer:
    // ✅ All 3 methods are correct and idiomatic.
    // ✅ Good use of Java 17 record for Transaction — shows modern Java awareness.
    // ✅ Consistent null-safety pattern across all methods.
    // ✅ Interview-ready: would pass FedEx/Hatio/NPCI coding rounds.
    // 📊 SCORE: 9/10 — Deducted 1 for not mentioning edge cases inline (empty list, all-null list).


    public static void main(String[] args) {
        // Test your implementations here
    }
}
