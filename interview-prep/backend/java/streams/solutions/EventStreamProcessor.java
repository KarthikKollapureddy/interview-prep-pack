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

    record ScanEvent(String trackingNumber, String facility, Instant timestamp, String eventType) {}

    /**
     * 1. Deduplicate, keeping only the latest event per tracking number.
     */
    public Map<String, ScanEvent> latestEventPerPackage(List<ScanEvent> events) {
        // YOUR CODE HERE
        return null;
    }

    /**
     * 2. For each tracking number, return the ordered list of facilities it passed through (sorted by timestamp).
     */
    public Map<String, List<String>> facilityRouteMap(List<ScanEvent> events) {
        // YOUR CODE HERE
        return null;
    }

    /**
     * 3. Find tracking numbers where the latest event is older than the given threshold.
     */
    public List<String> findStuckPackages(List<ScanEvent> events, Duration threshold) {
        // YOUR CODE HERE
        return null;
    }

    public static void main(String[] args) {
        // Test your implementations here
    }
}
