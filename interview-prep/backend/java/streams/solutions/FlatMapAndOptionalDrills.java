import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Targeted Practice: flatMap, Optional handling, and LinkedHashMap ordering
 * 
 * These drills target the exact weaknesses found in your WordFrequencyCounter review.
 * Each method focuses on ONE concept. Solve them in order.
 * 
 * After solving, run main() to validate against expected outputs.
 */
public class FlatMapAndOptionalDrills {

    // =====================================================================
    // DRILL 1: flatMap basics — splitting and flattening
    // =====================================================================

    /**
     * Given a list of CSV rows (e.g., "apple,banana,cherry"), return a flat list of ALL items.
     * Expected: ["apple,banana,cherry", "dog,cat"] → ["apple", "banana", "cherry", "dog", "cat"]
     */
    public List<String> flattenCSVRows(List<String> csvRows) {
        // YOUR CODE HERE
//        return null;
        return csvRows.stream()
                .flatMap(csv->Arrays.stream(csv.split(",")))
                .toList();
    }

    // MENTOR REVIEW — flattenCSVRows():
    // ✅ CORRECT: flatMap + split(",") is exactly right.
    // ✅ CORRECT: .toList() for clean output.
    // 💡 INTERVIEW TIP: If asked "what if CSV values have spaces around commas?",
    //    split on "\\s*,\\s*" to trim whitespace. Good edge case to mention proactively.
    // 🏆 VERDICT: Perfect. Shows you've internalized flatMap.
    // 📊 SCORE: 10/10

    /**
     * Given a list of sentences, return ALL unique words (case-insensitive, sorted alphabetically).
     * Strip punctuation. e.g., "Hello, world!" → "hello", "world"
     */
    public List<String> uniqueWordsSorted(List<String> sentences) {
        // YOUR CODE HERE
//        return null;
        return sentences.stream()
                .flatMap(words->Arrays.stream(words.split(" ")))
                .map(sentence-> sentence.replaceAll("[^a-zA-Z]","").toLowerCase())
                .filter(s->!s.isEmpty())
                .sorted()
                .distinct()
                .toList();

    }

    // MENTOR REVIEW — uniqueWordsSorted():
    // ✅ CORRECT: flatMap to split sentences into words — this was your main gap, now fixed.
    // ✅ CORRECT: replaceAll("[^a-zA-Z]","") strips all punctuation properly.
    // ✅ CORRECT: .filter(s->!s.isEmpty()) handles edge case of punctuation-only tokens.
    // ✅ CORRECT: toLowerCase + sorted + distinct produces correct output.
    // 💡 OPTIMIZATION: .sorted().distinct() works, but .distinct().sorted() is slightly better:
    //    distinct() first reduces the set size, then sorted() works on fewer elements.
    //    With sorted-first, the stream must see all elements before distinct can consume.
    //    In practice negligible, but shows awareness in interviews.
    // 💡 ALTERNATIVE: Use "\\s+" instead of " " for split — handles tabs, multiple spaces.
    // 🏆 VERDICT: Clean and correct. Big improvement from WordFrequencyCounter.
    // 📊 SCORE: 9/10

    /**
     * Given a Map<String, List<String>> of department → employees,
     * return a flat list of "department:employee" strings.
     * e.g., {"eng": ["alice","bob"]} → ["eng:alice", "eng:bob"]
     */
    public List<String> flattenDepartmentMap(Map<String, List<String>> deptMap) {
        // YOUR CODE HERE
//        return null;
        return deptMap.entrySet().stream()
                .map(entry ->
                    entry.getValue().stream()
                            .map(v->entry.getKey()+":"+v)
                            .toList()
                )
                .flatMap(List::stream)
                .toList();
    }

    // MENTOR REVIEW — flattenDepartmentMap():
    // ✅ CORRECT: The logic works — map entries to list of strings, then flatMap.
    // 💡 STYLE: You're doing map → toList() → flatMap(List::stream). This creates intermediate
    //    lists unnecessarily. Cleaner with a single flatMap:
    //    deptMap.entrySet().stream()
    //        .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + ":" + v))
    //        .toList();
    //    This avoids the intermediate List allocation — one pass, no temp objects.
    //    Both produce the same result, but the single-flatMap version is more idiomatic
    //    and is what interviewers expect to see.
    // 🏆 VERDICT: Correct but slightly verbose. Prefer single flatMap in interviews.
    // 📊 SCORE: 8/10

    // =====================================================================
    // DRILL 2: Optional safety — never call .get() without checking
    // =====================================================================

    /**
     * Find the longest string in the list. Return "" if list is empty.
     * Do NOT use .get() — use .orElse()
     */
    public String longestString(List<String> words) {
        // YOUR CODE HERE
        return words.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .findFirst().orElse("");
    }

    // MENTOR REVIEW — longestString():
    // ✅ CORRECT: .orElse("") — great, no more .get()! This is the pattern to always use.
    // 💡 PERFORMANCE: Same feedback as your palindrome review — sorted().findFirst() is
    //    O(N log N) when .max(Comparator.comparingInt(String::length)) is O(N):
    //    words.stream().max(Comparator.comparingInt(String::length)).orElse("");
    //    Use .max() or .min() when you only need the extreme value. Reserve sorted() for
    //    when you need multiple ordered results.
    // 🏆 VERDICT: Correct. Adopt the .max() pattern for interviews.
    // 📊 SCORE: 8/10

    /**
     * Find the first word starting with 'z' (case-insensitive).
     * Return "NOT_FOUND" if none exists. Use .orElse()
     */
    public String firstWordStartingWithZ(List<String> words) {
        // YOUR CODE HERE
        return words.stream()
                .filter(word-> "z".equalsIgnoreCase(String.valueOf(word.charAt(0))))
                .findFirst().orElse("NOT_FOUND");

    }

    // MENTOR REVIEW — firstWordStartingWithZ():
    // ✅ CORRECT: .orElse("NOT_FOUND") handles the empty case perfectly.
    // ✅ CORRECT: Case-insensitive check works.
    // 💡 STYLE: String.valueOf(word.charAt(0)) creates an intermediate String. Cleaner:
    //    word.toLowerCase().startsWith("z")
    //    or: Character.toLowerCase(word.charAt(0)) == 'z'
    // ⚠️ EDGE CASE: word.charAt(0) throws StringIndexOutOfBoundsException on empty strings.
    //    Add .filter(w -> !w.isEmpty()) before the charAt check for safety.
    // 📊 SCORE: 8/10

    /**
     * Find the maximum value in the list. If empty, throw IllegalArgumentException
     * with message "Empty list". Use .orElseThrow()
     */
    public int findMax(List<Integer> numbers) {
        // YOUR CODE HERE
        return numbers.stream()
                .sorted(Comparator.reverseOrder()).findFirst().orElseThrow(IllegalArgumentException::new);
    }

    // MENTOR REVIEW — findMax():
    // ✅ CORRECT: .orElseThrow(IllegalArgumentException::new) — correct exception pattern.
    // ⚠️ MINOR: The test expects message "Empty list" but your exception has null message.
    //    Use: .orElseThrow(() -> new IllegalArgumentException("Empty list"))
    //    The test still passed because it only checks exception type, not message.
    //    In production, always include meaningful messages.
    // 💡 PERFORMANCE: Again sorted().findFirst() is O(N log N) vs .max() at O(N):
    //    numbers.stream().max(Comparator.naturalOrder()).orElseThrow(...);
    //    This is the third time — make .max()/.min() your default reflex.
    // 📊 SCORE: 7/10 — missing exception message, O(N log N) when O(N) suffices.

    // =====================================================================
    // DRILL 3: Preserving order in Collectors.toMap() with LinkedHashMap
    // =====================================================================

    /**
     * Given a list of words, return a Map<String, Integer> of word → length,
     * sorted by length descending, preserving the sorted order.
     * Must use LinkedHashMap to preserve order.
     */
    public Map<String, Integer> wordLengthsSorted(List<String> words) {
        // YOUR CODE HERE
        return words.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                count->1,
                                Integer::sum,
                                LinkedHashMap::new
                        )
                );
    }

    // MENTOR REVIEW — wordLengthsSorted():
    // 🐛 BUG: The value is count->1, meaning every word gets value 1. The requirement says
    //    word → length (i.e., "java" → 4, "go" → 2). Should be:
    //    Collectors.toMap(Function.identity(), String::length, (a,b)->a, LinkedHashMap::new)
    //    The test passed by coincidence because it only checks key ORDER, not values.
    //    If the test had checked: wl.get("java") == 4, it would have failed (got 1).
    // ✅ CORRECT: LinkedHashMap::new in toMap — you learned the pattern. Good.
    // ✅ CORRECT: Sorted by length descending before collecting — order is preserved.
    // 💡 NOTE: Integer::sum as merge function means duplicate words get summed counts (1+1=2),
    //    but for word→length you want (a,b)->a to keep first occurrence.
    // 📊 SCORE: 5/10 — correct ordering technique, but wrong value mapping.

    /**
     * Given a list of integers, return a Map<Integer, Long> of number → frequency,
     * sorted by frequency descending (highest frequency first).
     * Must preserve sort order.
     */
    public Map<Integer, Long> frequencySorted(List<Integer> numbers) {
        // YOUR CODE HERE
        return numbers.stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                count-> 1L,
                                Long::sum,
                                LinkedHashMap::new
                        )
                ).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                .peek(System.out::println)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a,b)->a,
                                LinkedHashMap::new
                        )
                );
    }

    // MENTOR REVIEW — frequencySorted():
    // ✅ CORRECT: Two-pass approach — first collect frequencies, then sort by value.
    // ✅ CORRECT: LinkedHashMap preserves sorted order in the final collect.
    // ✅ CORRECT: Map.Entry.comparingByValue(Comparator.reverseOrder()) — clean sorting.
    // 💡 ALTERNATIVE: The first pass can use Collectors.groupingBy + counting() instead of
    //    manual toMap with Long::sum. More idiomatic:
    //    numbers.stream()
    //        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    //        .entrySet().stream()
    //        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
    //        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    //    groupingBy + counting is the standard idiom for frequency maps. Interviewers expect it.
    // 🏆 VERDICT: Correct and well-structured. The toMap approach works but groupingBy is more idiomatic.
    // 📊 SCORE: 8/10

    // =====================================================================
    // DRILL 4: Combined — putting it all together (interview-style)
    // =====================================================================

    /**
     * Given a list of sentences, find the top K words by frequency.
     * Rules:
     *   - Split sentences into words
     *   - Case-insensitive (return lowercase keys)
     *   - Strip ALL non-letter characters
     *   - Skip empty strings after stripping
     *   - Ordered by frequency descending (preserve order in map)
     *   - If no words exist, return empty map
     *
     * This is your topKWords() redo. Get it right this time.
     */
    public Map<String, Long> topKWordsFixed(List<String> sentences, int k) {
        // YOUR CODE HERE
        return sentences.stream()
                .flatMap(sentence->Arrays.stream(sentence.split(" ")))
                .map(word -> word.replaceAll("[^a-zA-Z0-9]","").toLowerCase())
                .filter(word->!word.isEmpty())
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                count-> 1L,
                                Long::sum,
                                LinkedHashMap::new
                        )
                ).entrySet().stream()
                .sorted(
                        Map.Entry.comparingByValue(Comparator.reverseOrder())
                )
                .limit(k)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a,b)->a,
                                LinkedHashMap::new
                        )
                );
    }

    // MENTOR REVIEW — topKWordsFixed():
    // ✅ CORRECT: flatMap + split — the critical fix from WordFrequencyCounter. Nailed it.
    // ✅ CORRECT: replaceAll("[^a-zA-Z0-9]","").toLowerCase() — proper punctuation stripping.
    // ✅ CORRECT: .filter(word->!word.isEmpty()) — handles edge cases.
    // ✅ CORRECT: LinkedHashMap in final collect — preserves sorted order.
    // ✅ CORRECT: Map.Entry.comparingByValue(Comparator.reverseOrder()) — clean sort.
    // 💡 SAME NOTE: First pass uses toMap + Long::sum. groupingBy + counting() is more
    //    idiomatic for frequency counting. Not wrong, just less conventional.
    // 💡 SPLIT: Use "\\s+" instead of " " for robustness against tabs/multiple spaces.
    // 🏆 VERDICT: Excellent. All 4 bugs from your original topKWords() are fixed.
    //    This is the version you should memorize for interviews.
    // 📊 SCORE: 9/10

    /**
     * Given a list of sentences, find the longest palindrome word.
     * Rules:
     *   - Split sentences into words
     *   - Case-insensitive palindrome check (convert to lowercase first)
     *   - Return the word in lowercase
     *   - Return "" if no palindrome exists (NOT an exception)
     *
     * This is your longestPalindromeWord() redo. Get it right this time.
     */
    public String longestPalindromeFixed(List<String> sentences) {
        // YOUR CODE HERE
        return sentences.stream()
                .flatMap(words->Arrays.stream(words.split(" ")))
                .map(String::toLowerCase)
                .filter(FlatMapAndOptionalDrills::isPalindrome)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .findFirst().orElse("");
    }

    // MENTOR REVIEW — longestPalindromeFixed():
    // ✅ CORRECT: flatMap + split into words — fixed.
    // ✅ CORRECT: .map(String::toLowerCase) before palindrome check — case-insensitive.
    // ✅ CORRECT: .orElse("") — no more .get() crash. Clean Optional handling.
    // 💡 PERFORMANCE: sorted().findFirst() again — use .max() instead:
    //    .max(Comparator.comparingInt(String::length)).orElse("");
    //    This is O(N) vs O(N log N). You've used sorted+findFirst in 4 methods now.
    //    Train yourself: "I only need one extreme value → .max() or .min()".
    // 🏆 VERDICT: Correct. Both bugs from original (missing flatMap, unsafe get) are fixed.
    // 📊 SCORE: 8/10

    private static boolean isPalindrome(String word){
        for (int i = 0; i<word.length()/2 ;i++){
            if (word.charAt(i) != word.charAt((word.length()-i)-1))
                return false;
        }
        return true;
    }

    // =====================================================================
    // Test harness — run to validate
    // =====================================================================
    public static void main(String[] args) {
        FlatMapAndOptionalDrills d = new FlatMapAndOptionalDrills();
        int passed = 0, failed = 0;

        // --- DRILL 1: flatMap ---
        // Test flattenCSVRows
        List<String> csv = d.flattenCSVRows(List.of("apple,banana,cherry", "dog,cat"));
        passed += check("flattenCSV", csv, List.of("apple", "banana", "cherry", "dog", "cat")) ? 1 : 0;

        // Test uniqueWordsSorted
        List<String> uniq = d.uniqueWordsSorted(List.of("Hello, world!", "hello Java world."));
        passed += check("uniqueWords", uniq, List.of("hello", "java", "world")) ? 1 : 0;

        // Test flattenDepartmentMap
        Map<String, List<String>> deptMap = new LinkedHashMap<>();
        deptMap.put("eng", List.of("alice", "bob"));
        deptMap.put("hr", List.of("carol"));
        List<String> flat = d.flattenDepartmentMap(deptMap);
        passed += check("flattenDept", flat, List.of("eng:alice", "eng:bob", "hr:carol")) ? 1 : 0;

        // --- DRILL 2: Optional ---
        // Test longestString
        passed += check("longestStr", d.longestString(List.of("hi", "hello", "hey")), "hello") ? 1 : 0;
        passed += check("longestStr empty", d.longestString(List.of()), "") ? 1 : 0;

        // Test firstWordStartingWithZ
        passed += check("firstZ found", d.firstWordStartingWithZ(List.of("apple", "Zebra", "zoo")), "Zebra") ? 1 : 0;
        passed += check("firstZ not found", d.firstWordStartingWithZ(List.of("apple", "banana")), "NOT_FOUND") ? 1 : 0;

        // Test findMax
        passed += check("findMax", d.findMax(List.of(3, 7, 1, 9, 4)), 9) ? 1 : 0;
        try {
            d.findMax(List.of());
            System.out.println("  ❌ findMax empty — expected exception, got none");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ findMax empty — correctly threw: " + e.getMessage());
            passed++;
        }

        // --- DRILL 3: LinkedHashMap ordering ---
        // Test wordLengthsSorted
        Map<String, Integer> wl = d.wordLengthsSorted(List.of("go", "java", "typescript", "py"));
        List<String> wlKeys = new ArrayList<>(wl.keySet());
        passed += check("wordLengths order", wlKeys, List.of("typescript", "java", "go", "py")) ? 1 : 0;

        // Test frequencySorted
        Map<Integer, Long> freq = d.frequencySorted(List.of(1, 2, 2, 3, 3, 3, 1, 1, 1));
        List<Integer> freqKeys = new ArrayList<>(freq.keySet());
        passed += check("freqSorted order", freqKeys, List.of(1, 3, 2)) ? 1 : 0;

        // --- DRILL 4: Combined redo ---
        // Test topKWordsFixed
        Map<String, Long> top = d.topKWordsFixed(List.of(
                "the cat sat on the mat",
                "the dog sat on the log",
                "the cat chased the dog"
        ), 3);
        // "the"=6, then "cat","sat","on","dog" each=2, "mat","log","chased" each=1
        List<String> topKeys = new ArrayList<>(top.keySet());
        passed += check("topK key[0]", topKeys.get(0), "the") ? 1 : 0;
        passed += check("topK value[0]", top.get("the"), 6L) ? 1 : 0;
        passed += check("topK size", top.size(), 3) ? 1 : 0;

        // Test topKWordsFixed with punctuation
        Map<String, Long> topPunc = d.topKWordsFixed(List.of("Hello, hello! HELLO."), 1);
        passed += check("topK punct", topPunc, Map.of("hello", 3L)) ? 1 : 0;

        // Test topKWordsFixed empty
        Map<String, Long> topEmpty = d.topKWordsFixed(List.of(), 5);
        passed += check("topK empty", topEmpty.isEmpty(), true) ? 1 : 0;

        // Test longestPalindromeFixed
        passed += check("palindrome", d.longestPalindromeFixed(
                List.of("racecar is a civic deed for kayak")), "racecar") ? 1 : 0;
        passed += check("palindrome none", d.longestPalindromeFixed(
                List.of("hello world java spring")), "") ? 1 : 0;
        passed += check("palindrome single", d.longestPalindromeFixed(
                List.of("a b c")), "a") ? 1 : 0;

        System.out.println("\n========================================");
        System.out.println("  PASSED: " + passed + " / " + (passed + failed));
        System.out.println("========================================");
    }

    private static <T> boolean check(String name, T actual, T expected) {
        boolean pass = Objects.equals(actual, expected);
        System.out.println((pass ? "  ✅ " : "  ❌ ") + name +
                (pass ? "" : " — expected: " + expected + ", got: " + actual));
        if (!pass) return false;
        return true;
    }

    // MENTOR REVIEW — Overall FlatMapAndOptionalDrills:
    // 📊 OVERALL SCORE: 8.2/10 (up from 6/10 on WordFrequencyCounter)
    //
    // ✅ MAJOR IMPROVEMENTS:
    //   - flatMap usage is now confident and correct across all methods
    //   - Optional handling (.orElse, .orElseThrow) — no more .get() anywhere
    //   - LinkedHashMap pattern for preserving order — fully understood
    //   - Punctuation stripping with regex — properly applied
    //   - All 19/19 tests passing
    //
    // 🔴 RECURRING PATTERN TO FIX:
    //   - sorted().findFirst() when .max()/.min() suffices — appeared 4 times.
    //     This is O(N log N) vs O(N). At NPCI/FedEx scale, this matters.
    //     RULE: "Need one extreme value? → .max() or .min(). Need multiple ordered? → sorted()."
    //
    // 🟡 MINOR FIXES:
    //   - wordLengthsSorted: value should be String::length, not count->1
    //   - findMax: include message in IllegalArgumentException
    //   - Use "\\s+" instead of " " for split (handles tabs, multiple spaces)
    //   - Prefer groupingBy+counting() over toMap+Long::sum for frequency maps
    //   - flattenDepartmentMap: use single flatMap instead of map+flatMap
    //
    // 🎯 NEXT STEPS:
    //   - Go back to WordFrequencyCounter.java and re-solve topKWords() + longestPalindromeWord()
    //     with these patterns. You now have the muscle memory.
    //   - Move on to EventStreamProcessor.java (Challenge 2) — it tests flatMap + toMap merge
    //     functions, which is the next level.
}
