import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Challenge 3: Word Frequency Counter
 * 
 * Given: A list of strings (sentences)
 * 
 * Implement all three methods using Java Streams.
 */
public class WordFrequencyCounter {

    /**
     * 1. Return the top K most frequent words (case-insensitive, ignore punctuation),
     *    ordered by frequency descending.
     */
    public Map<String, Long> topKWords(List<String> sentences, int k) {
        // YOUR CODE HERE
//        return null;
        return sentences.stream()
                .map(word -> {
                    if (word.charAt(word.length()-1)=='.'){
                        return word.substring(0,word.length()-2);
                    }
                    return word;
                })
                .map(String::toUpperCase)
                .collect(
                        Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()
                        )
                ).entrySet()
                .stream()
                .sorted(
//                        Map.Entry.comparingByValue().reversed()
                        (e1,e2)-> Math.toIntExact(e2.getValue() - e1.getValue())
                )
                .limit(k)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )
                );

    }

    // MENTOR REVIEW — topKWords():
    // 🐛 BUG 1 (Critical): Input is "sentences" (e.g., "hello world hello") but you're treating
    //    each element as a single word. You need flatMap to split sentences into words first:
    //    .flatMap(sentence -> Arrays.stream(sentence.split("\\s+")))
    //    Without this, "hello world" is one entry, not two words.
    //
    // 🐛 BUG 2 (Off-by-one): word.substring(0, word.length()-2) removes TWO characters, not one.
    //    "hello." → "hell" instead of "hello". Fix: word.substring(0, word.length()-1)
    //
    // 🐛 BUG 3 (Incomplete punctuation handling): Only strips trailing '.'. What about commas,
    //    exclamation marks, quotes? Use: word.replaceAll("[^a-zA-Z0-9]", "") to strip ALL punctuation.
    //
    // 🐛 BUG 4 (Order not preserved): The final Collectors.toMap() uses a HashMap internally —
    //    the sorted order is LOST. Use the 4-arg toMap with LinkedHashMap supplier:
    //    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new)
    //
    // ⚠️ FRAGILE: Math.toIntExact(e2.getValue() - e1.getValue()) can overflow if counts are large.
    //    Use Long.compare(e2.getValue(), e1.getValue()) instead. Or use the commented-out
    //    Map.Entry.comparingByValue(Comparator.reverseOrder()) which is cleaner.
    //
    // 💡 IMPROVED VERSION would look like:
    //    sentences.stream()
    //        .flatMap(s -> Arrays.stream(s.split("\\s+")))
    //        .map(w -> w.replaceAll("[^a-zA-Z]", "").toUpperCase())
    //        .filter(w -> !w.isEmpty())
    //        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    //        .entrySet().stream()
    //        .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
    //        .limit(k)
    //        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    //
    // 📊 SCORE: 4/10 — Logic direction is right, but 4 bugs would cause wrong output.

    /**
     * 2. Character frequency across all sentences (ignore spaces).
     */
    public Map<Character, Long> charFrequency(List<String> sentences) {
        // YOUR CODE HERE
//        return null;
        return sentences.stream()
                .flatMap(word -> word.chars().mapToObj(number -> (char) number))
                .filter(character->character!=' ')
                .collect(
                        Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()
                        )
                );
    }

    // MENTOR REVIEW — charFrequency():
    // ✅ CORRECT: flatMap with word.chars().mapToObj(c -> (char) c) is the right approach.
    // ✅ CORRECT: Filtering spaces with character != ' ' meets the requirement.
    // ✅ CORRECT: groupingBy + counting for frequency is idiomatic.
    // 💡 NOTE: word.chars() returns an IntStream. The cast (char) number is correct but
    //    in an interview, mention that chars() returns int codepoints, not char — shows depth.
    // 💡 EDGE CASE: This counts punctuation characters too. If interviewer says "letters only",
    //    add .filter(Character::isLetterOrDigit) instead of just != ' '.
    // 🏆 VERDICT: Clean and correct. Best of your three solutions here.
    // 📊 SCORE: 9/10

    /**
     * 3. Find the longest palindrome word across all sentences.
     */
    public String longestPalindromeWord(List<String> sentences) {
        // YOUR CODE HERE
//        return null;
        return sentences.stream()
                .filter(WordFrequencyCounter::isPalindrome)
                .sorted((w1,w2)->w2.length()-w1.length())
                .findFirst().get();
    }

    // MENTOR REVIEW — longestPalindromeWord():
    // 🐛 BUG 1 (Same as topKWords): Input is "sentences" — you need to flatMap/split into words:
    //    .flatMap(s -> Arrays.stream(s.split("\\s+")))
    //
    // ⚠️ UNSAFE: .findFirst().get() throws NoSuchElementException if no palindrome exists.
    //    Use .orElse("") or .orElseThrow(() -> new NoSuchElementException("No palindrome found"))
    //    In an interview, always handle Optional properly — interviewers specifically look for this.
    //
    // 💡 PERFORMANCE: sorting entire stream O(N log N) just to get the max is wasteful.
    //    Use .max(Comparator.comparingInt(String::length)) instead — O(N) single pass:
    //    sentences.stream()
    //        .flatMap(s -> Arrays.stream(s.split("\\s+")))
    //        .filter(WordFrequencyCounter::isPalindrome)
    //        .max(Comparator.comparingInt(String::length))
    //        .orElse("");
    //
    // ✅ CORRECT: isPalindrome() helper is well-implemented — clean loop, correct index math.
    // 💡 ALTERNATIVE isPalindrome: new StringBuilder(word).reverse().toString().equals(word)
    //    — one-liner but O(N) space. Your loop is O(1) space. Mention both in interview.
    // 📊 SCORE: 6/10 — Missing flatMap and unsafe Optional handling.

    private static boolean isPalindrome(String word){
        for (int i = 0; i<word.length()/2 ;i++){
            if (word.charAt(i) != word.charAt((word.length()-i)-1))
                return false;
        }
        return true;
    }

    public static void main(String[] args) {
        WordFrequencyCounter wfc = new WordFrequencyCounter();

        // ==================== TEST INPUTS ====================

        // --- topKWords() Tests ---
        // TEST 1: Basic — multiple sentences, k=2
        List<String> sentences1 = List.of(
                "the cat sat on the mat",
                "the dog sat on the log",
                "the cat chased the dog"
        );
        System.out.println("topKWords Test 1 (k=2): " + wfc.topKWords(sentences1, 2));
        // EXPECTED: {THE=9, THE is top at 9 across all 3 sentences... wait, let's count:
        //   "the"=6, "cat"=2, "sat"=2, "on"=2, "mat"=1, "dog"=2, "log"=1, "chased"=1
        //   Top 2: {THE=6, CAT=2} or {THE=6, SAT=2} or {THE=6, DOG=2} (tie at 2)
        // EXPECTED (case-insensitive, uppercase keys): {THE=6, CAT=2} (or any word with count 2)

        // TEST 2: Punctuation handling
        List<String> sentences2 = List.of(
                "Hello, world! Hello world.",
                "hello... WORLD!! hello"
        );
        System.out.println("topKWords Test 2 (k=3): " + wfc.topKWords(sentences2, 3));
        // EXPECTED: {HELLO=4, WORLD=3} (only 2 unique words after cleanup)
        // 🔍 YOUR CODE WILL FAIL HERE: punctuation not stripped, sentences not split into words

        // TEST 3: Edge — k larger than unique words
        List<String> sentences3 = List.of("a a a b b c");
        System.out.println("topKWords Test 3 (k=10): " + wfc.topKWords(sentences3, 10));
        // EXPECTED: {A=3, B=2, C=1}

        // TEST 4: Single word repeated
        List<String> sentences4 = List.of("java", "java", "java");
        System.out.println("topKWords Test 4 (k=1): " + wfc.topKWords(sentences4, 1));
        // EXPECTED: {JAVA=3}
        // 🔍 NOTE: This test PASSES with your current code because inputs are already single words

        // --- charFrequency() Tests ---
        // TEST 5: Basic
        List<String> sentences5 = List.of("aab", "bcc");
        System.out.println("charFrequency Test 5: " + wfc.charFrequency(sentences5));
        // EXPECTED: {a=2, b=2, c=2}

        // TEST 6: Spaces should be ignored
        List<String> sentences6 = List.of("a b", "c d");
        System.out.println("charFrequency Test 6: " + wfc.charFrequency(sentences6));
        // EXPECTED: {a=1, b=1, c=1, d=1} — NO space key

        // TEST 7: Mixed case and punctuation
        List<String> sentences7 = List.of("Hello! World!");
        System.out.println("charFrequency Test 7: " + wfc.charFrequency(sentences7));
        // EXPECTED: {H=1, e=1, l=2, o=2, !=2, W=1, r=1, d=1} — note: ! is counted (case-sensitive)

        // --- longestPalindromeWord() Tests ---
        // TEST 8: Basic — mixed words
        List<String> sentences8 = List.of("racecar is a civic deed for kayak");
        System.out.println("longestPalindrome Test 8: " + wfc.longestPalindromeWord(sentences8));
        // EXPECTED: "racecar" (length 7, longest palindrome)
        // 🔍 YOUR CODE WILL FAIL: sentence not split into words — whole sentence isn't a palindrome

        // TEST 9: Single-character palindromes
        List<String> sentences9 = List.of("a b c d");
        System.out.println("longestPalindrome Test 9: " + wfc.longestPalindromeWord(sentences9));
        // EXPECTED: "a" (or any single char — all are palindromes)

        // TEST 10: No palindromes — should handle gracefully
        List<String> sentences10 = List.of("hello world java spring");
        System.out.println("longestPalindrome Test 10: " + wfc.longestPalindromeWord(sentences10));
        // EXPECTED: "" (empty string) or exception
        // 🔍 YOUR CODE WILL CRASH: .get() on empty Optional throws NoSuchElementException

        System.out.println("\n===== Run all tests and compare against EXPECTED comments =====");
    }

    // MENTOR REVIEW — Overall WordFrequencyCounter:
    // 📊 OVERALL SCORE: 6/10
    //
    // ✅ STRENGTHS:
    //   - Good grasp of groupingBy + counting pattern
    //   - charFrequency() is clean and correct
    //   - isPalindrome() helper is efficient (O(1) space)
    //   - Shows comfort with stream chaining
    //
    // 🔴 CRITICAL FIXES NEEDED:
    //   1. flatMap to split sentences into words — this is a fundamental requirement
    //      for topKWords() and longestPalindromeWord(). Practice flatMap patterns.
    //   2. Off-by-one in substring (length-2 vs length-1)
    //   3. Punctuation stripping — use replaceAll regex, not manual char check
    //   4. LinkedHashMap to preserve sorted order in toMap()
    //   5. Never call .get() on Optional without .isPresent() or use .orElse()
    //
    // 🎯 ACTION ITEMS:
    //   - Re-solve topKWords() with the fixes above
    //   - Practice flatMap scenarios (this is a P1 gap from the skill matrix)
    //   - Always use .orElse() / .orElseThrow() on Optional — NPCI/FedEx interviewers check this
}
