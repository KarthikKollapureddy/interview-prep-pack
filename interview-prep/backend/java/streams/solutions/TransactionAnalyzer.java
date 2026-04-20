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
        return null;
    }

    /**
     * 2. Return top N transactions by amount, across all statuses.
     */
    public List<Transaction> topNByAmount(List<Transaction> txns, int n) {
        // YOUR CODE HERE
        return null;
    }


    /**
     * 3. Return a map where key = "merchantId:status", value = count.
     */
    public Map<String, Long> countByStatusPerMerchant(List<Transaction> txns) {
        // YOUR CODE HERE
        return null;
    }


    public static void main(String[] args) {
        // Test your implementations here
    }
}
