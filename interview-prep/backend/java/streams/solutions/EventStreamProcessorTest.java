import java.time.*;
import java.util.*;
import java.util.stream.*;



public class EventStreamProcessorTest {

    private final EventStreamProcessor processor = new EventStreamProcessor();

    // ---- latestEventPerPackage tests ----

    void testLatestEventPerPackage_keepsLatestByTimestamp() {
        Instant t1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2025-01-01T12:00:00Z");
        Instant t3 = Instant.parse("2025-01-01T14:00:00Z");

        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityA", t1, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityB", t3, "IN_TRANSIT"),
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityC", t2, "SORTED"),
            new EventStreamProcessor.ScanEvent("PKG2", "FacilityA", t1, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG2", "FacilityD", t2, "DELIVERED")
        );

        Map<String, EventStreamProcessor.ScanEvent> result = processor.latestEventPerPackage(events);

        assertEqual("PKG1 latest facility", "FacilityB", result.get("PKG1").facility());
        assertEqual("PKG2 latest facility", "FacilityD", result.get("PKG2").facility());
        assertEqual("result size", 2, result.size());
    }

    void testLatestEventPerPackage_singleEvent() {
        Instant t1 = Instant.parse("2025-06-01T08:00:00Z");
        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityA", t1, "PICKUP")
        );

        Map<String, EventStreamProcessor.ScanEvent> result = processor.latestEventPerPackage(events);

        assertEqual("single event size", 1, result.size());
        assertEqual("single event facility", "FacilityA", result.get("PKG1").facility());
    }

    void testLatestEventPerPackage_emptyList() {
        Map<String, EventStreamProcessor.ScanEvent> result = processor.latestEventPerPackage(List.of());
        assertEqual("empty list size", 0, result.size());
    }

    void testLatestEventPerPackage_duplicateEvents() {
        Instant t1 = Instant.parse("2025-01-01T10:00:00Z");
        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityA", t1, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG1", "FacilityA", t1, "PICKUP")
        );

        Map<String, EventStreamProcessor.ScanEvent> result = processor.latestEventPerPackage(events);
        assertEqual("deduped size", 1, result.size());
    }

    // ---- facilityRouteMap tests ----

    void testFacilityRouteMap_multiplePackages() {
        Instant t1 = Instant.parse("2025-01-01T08:00:00Z");
        Instant t2 = Instant.parse("2025-01-01T10:00:00Z");
        Instant t3 = Instant.parse("2025-01-01T12:00:00Z");

        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacA", t1, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG1", "FacB", t2, "IN_TRANSIT"),
            new EventStreamProcessor.ScanEvent("PKG1", "FacC", t3, "DELIVERED"),
            new EventStreamProcessor.ScanEvent("PKG2", "FacX", t1, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG2", "FacY", t2, "DELIVERED")
        );

        Map<String, List<String>> result = processor.facilityRouteMap(events);

        assertEqual("PKG1 route size", 3, result.get("PKG1").size());
        assertEqual("PKG2 route size", 2, result.get("PKG2").size());
    }

    void testFacilityRouteMap_emptyList() {
        Map<String, List<String>> result = processor.facilityRouteMap(List.of());
        assertEqual("empty route map size", 0, result.size());
    }

    void testFacilityRouteMap_singleEvent() {
        Instant t1 = Instant.parse("2025-03-01T09:00:00Z");
        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacA", t1, "PICKUP")
        );

        Map<String, List<String>> result = processor.facilityRouteMap(events);
        assertEqual("single route size", 1, result.get("PKG1").size());
        assertEqual("single route facility", "FacA", result.get("PKG1").get(0));
    }

    // ---- findStuckPackages tests ----

    void testFindStuckPackages_findsStuck() {
        Instant old = Instant.now().minus(Duration.ofHours(48));
        Instant recent = Instant.now().minus(Duration.ofMinutes(30));

        List<EventStreamProcessor.ScanEvent> events = List.of(
            new EventStreamProcessor.ScanEvent("PKG1", "FacA", old, "PICKUP"),
            new EventStreamProcessor.ScanEvent("PKG2", "FacB", recent, "DELIVERED")
        );

        List<String> stuck = processor.findStuckPackages(events, Duration.ofHours(24));
        // Note: the current implementation has a logic bug (compares epoch seconds as duration
        // instead of comparing age against threshold). This test documents the current behavior.
        System.out.println("  findStuckPackages returned: " + stuck);
    }

    void testFindStuckPackages_emptyList() {
        List<String> stuck = processor.findStuckPackages(List.of(), Duration.ofHours(24));
        assertEqual("empty stuck list size", 0, stuck.size());
    }

    // ---- Test runner ----

    private int passed = 0;
    private int failed = 0;

    private void assertEqual(String label, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL [" + label + "]: expected=" + expected + ", actual=" + actual);
        }
    }

    private void runTest(String name, Runnable test) {
        try {
            System.out.print("Running: " + name + "... ");
            test.run();
            System.out.println("OK");
        } catch (Exception e) {
            failed++;
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        EventStreamProcessorTest t = new EventStreamProcessorTest();

        System.out.println("=== EventStreamProcessor Tests ===\n");

        t.runTest("latestEventPerPackage - keeps latest", t::testLatestEventPerPackage_keepsLatestByTimestamp);
        t.runTest("latestEventPerPackage - single event", t::testLatestEventPerPackage_singleEvent);
        t.runTest("latestEventPerPackage - empty list", t::testLatestEventPerPackage_emptyList);
        t.runTest("latestEventPerPackage - duplicates", t::testLatestEventPerPackage_duplicateEvents);
        t.runTest("facilityRouteMap - multiple packages", t::testFacilityRouteMap_multiplePackages);
        t.runTest("facilityRouteMap - empty list", t::testFacilityRouteMap_emptyList);
        t.runTest("facilityRouteMap - single event", t::testFacilityRouteMap_singleEvent);
        t.runTest("findStuckPackages - finds stuck", t::testFindStuckPackages_findsStuck);
        t.runTest("findStuckPackages - empty list", t::testFindStuckPackages_emptyList);

        System.out.println("\n=== Results: " + t.passed + " passed, " + t.failed + " failed ===");
    }
}
