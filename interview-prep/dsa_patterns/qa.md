# DSA & Coding Patterns — Curated Problem Set

> 50+ curated problems by pattern. These are the most frequently asked at product-based companies.  
> Source: LeetCode, InterviewBit, GeeksforGeeks discussion forums, Glassdoor

> **⚠️ TODO:** Add coded solutions (Java) for all 50 problems in `solutions/` directory.  
> For each problem, write: brute force → optimized solution → time/space analysis.  
> Target: Complete at least 2-3 problems per pattern.

---

## How to Use This File

1. Read the pattern explanation
2. Try the problem yourself (timer on)
3. If stuck for 15+ min, read the hint
4. Code the solution in `solutions/`
5. Analyze: Time complexity, Space complexity, Edge cases

---

## Pattern 1: Arrays & Hashing

### Concept
Use HashMaps/HashSets for O(1) lookups. Convert O(n²) brute force into O(n).

### Problems

#### P1. Two Sum (Easy) — Asked at: Amazon, Google, Goldman Sachs
```
Given an array of integers nums and a target, return indices of
two numbers that add up to target.

Input:  nums = [2, 7, 11, 15], target = 9
Output: [0, 1]

Hint: Use a HashMap to store {value → index} as you iterate.
Time: O(n), Space: O(n)
```

#### P2. Contains Duplicate (Easy) — Asked at: Apple, Microsoft
```
Given array nums, return true if any value appears at least twice.

Hint: HashSet. Add each element; if already exists, return true.
Time: O(n), Space: O(n)
```

#### P3. Group Anagrams (Medium) — Asked at: Amazon, Facebook, Bloomberg
```
Given array of strings, group anagrams together.

Input:  ["eat","tea","tan","ate","nat","bat"]
Output: [["bat"],["nat","tan"],["ate","eat","tea"]]

Hint: Sort each string as key → HashMap<String, List<String>>
Time: O(n * k log k) where k = max string length
```

#### P4. Top K Frequent Elements (Medium) — Asked at: Amazon, Facebook
```
Given nums and k, return k most frequent elements.

Input:  nums = [1,1,1,2,2,3], k = 2
Output: [1,2]

Hint: HashMap for frequency count + Bucket Sort or Min-Heap
Time: O(n) with bucket sort
```

#### P5. Longest Consecutive Sequence (Medium) — Asked at: Google, Amazon
```
Given unsorted array, find length of longest consecutive sequence.
Must run in O(n).

Input:  [100, 4, 200, 1, 3, 2]
Output: 4 (sequence: [1,2,3,4])

Hint: HashSet. For each num, check if num-1 exists (start of sequence).
      If not, count forward: num+1, num+2, ...
Time: O(n)
```

---

## Pattern 2: Two Pointers

### Concept
Use two pointers (start/end or slow/fast) to reduce O(n²) to O(n). Works best on sorted arrays.

### Problems

#### P6. Valid Palindrome (Easy) — Asked at: Facebook, Microsoft
```
Check if string is palindrome (ignore non-alphanumeric, case-insensitive).

Input:  "A man, a plan, a canal: Panama"
Output: true

Hint: Two pointers from both ends, skip non-alphanumeric.
Time: O(n), Space: O(1)
```

#### P7. 3Sum (Medium) — Asked at: Amazon, Facebook, Google, Bloomberg
```
Find all unique triplets that sum to zero.

Input:  [-1, 0, 1, 2, -1, -4]
Output: [[-1,-1,2], [-1,0,1]]

Hint: Sort array. Fix one element, use two pointers for remaining pair.
      Skip duplicates.
Time: O(n²)
```

#### P8. Container With Most Water (Medium) — Asked at: Amazon, Google
```
Given array of heights, find two lines forming container with max water.

Hint: Two pointers at both ends. Move the shorter line inward.
Time: O(n), Space: O(1)
```

#### P9. Trapping Rain Water (Hard) — Asked at: Amazon, Google, Goldman Sachs
```
Given elevation map, compute how much water it can trap after rain.

Input:  [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6

Hint: Two pointers or prefix max arrays.
      water[i] = min(leftMax, rightMax) - height[i]
Time: O(n), Space: O(1) with two pointers
```

---

## Pattern 3: Sliding Window

### Concept
Maintain a window (subarray/substring) that expands and shrinks. Avoids recomputing overlapping subproblems.

### Problems

#### P10. Best Time to Buy and Sell Stock (Easy) — Asked at: Amazon, Goldman Sachs, Morgan Stanley
```
Maximize profit from one buy-sell transaction.

Input:  [7, 1, 5, 3, 6, 4]
Output: 5 (buy at 1, sell at 6)

Hint: Track minimum price so far, calculate profit at each step.
Time: O(n), Space: O(1)
```

#### P11. Longest Substring Without Repeating Characters (Medium) — Asked at: Amazon, Bloomberg, Google
```
Input:  "abcabcbb"
Output: 3 ("abc")

Hint: Sliding window with HashSet. Expand right, shrink left on duplicate.
Time: O(n)
```

#### P12. Minimum Window Substring (Hard) — Asked at: Facebook, Amazon, Google
```
Find minimum window in s containing all characters of t.

Input:  s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"

Hint: Expand right until valid, shrink left to minimize.
      Use frequency map for t, track matched count.
Time: O(n)
```

---

## Pattern 4: Stack

### Concept
LIFO structure for matching brackets, monotonic sequences, expression evaluation.

### Problems

#### P13. Valid Parentheses (Easy) — Asked at: Amazon, Bloomberg, Facebook
```
Input:  "()[]{}"
Output: true

Hint: Push opening brackets, pop and match on closing.
Time: O(n), Space: O(n)
```

#### P14. Min Stack (Medium) — Asked at: Amazon, Bloomberg
```
Design stack supporting push, pop, top, and getMin in O(1).

Hint: Use two stacks — one for values, one for minimums.
```

#### P15. Daily Temperatures (Medium) — Asked at: Facebook, Amazon
```
Given temperatures, return array: days until warmer temperature.

Input:  [73,74,75,71,69,72,76,73]
Output: [1, 1, 4, 2, 1, 1, 0, 0]

Hint: Monotonic decreasing stack. Pop when current > stack top.
Time: O(n)
```

---

## Pattern 5: Binary Search

### Concept
O(log n) search on sorted data. Key insight: identify the search space and the condition to go left vs right.

### Problems

#### P16. Search in Rotated Sorted Array (Medium) — Asked at: Amazon, Facebook, Microsoft
```
Input:  [4,5,6,7,0,1,2], target = 0
Output: 4

Hint: Modified binary search. Determine which half is sorted.
Time: O(log n)
```

#### P17. Find Minimum in Rotated Sorted Array (Medium) — Asked at: Amazon, Microsoft
```
Hint: Binary search. If mid > right, minimum is in right half.
Time: O(log n)
```

#### P18. Koko Eating Bananas (Medium) — Asked at: Google, Facebook
```
Binary search on the answer. Search space = [1, max(piles)].
```

---

## Pattern 6: Linked List

### Concept
Pointer manipulation. Common tricks: dummy node, slow/fast pointers, reversal.

### Problems

#### P19. Reverse Linked List (Easy) — Asked at: Amazon, Microsoft, Apple
```java
// Iterative
ListNode prev = null, curr = head;
while (curr != null) {
    ListNode next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
}
return prev;
// Time: O(n), Space: O(1)
```

#### P20. Detect Cycle in Linked List (Easy) — Asked at: Amazon, Microsoft
```
Floyd's cycle detection: slow (1 step), fast (2 steps).
If they meet → cycle exists.
```

#### P21. Merge Two Sorted Lists (Easy) — Asked at: Amazon, Microsoft, Apple
```
Dummy node technique. Compare heads, attach smaller.
Time: O(n+m)
```

#### P22. LRU Cache (Medium) — Asked at: Amazon, Facebook, Google, Microsoft
```
HashMap + Doubly Linked List.
Get: O(1), Put: O(1).
Most important design question for product companies.
(Full implementation in system_design/lld/qa.md)
```

---

## Pattern 7: Trees

### Concept
Recursion is your friend. Most tree problems = left subtree + right subtree + current node.

### Problems

#### P23. Invert Binary Tree (Easy) — Asked at: Google, Amazon
```java
TreeNode invertTree(TreeNode root) {
    if (root == null) return null;
    TreeNode temp = root.left;
    root.left = invertTree(root.right);
    root.right = invertTree(temp);
    return root;
}
```

#### P24. Maximum Depth of Binary Tree (Easy) — Asked at: Amazon, LinkedIn
```java
int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

#### P25. Validate BST (Medium) — Asked at: Amazon, Facebook, Bloomberg
```
Use min/max bounds. Each node must be within (min, max) range.
Left child: (min, parent.val), Right child: (parent.val, max)
```

#### P26. Lowest Common Ancestor of BST (Medium) — Asked at: Amazon, Facebook, Microsoft
```
If both values < current → go left
If both values > current → go right
Otherwise → current node is LCA
```

#### P27. Level Order Traversal (Medium) — Asked at: Amazon, Microsoft
```
BFS with queue. Process level by level.
```

#### P28. Binary Tree Right Side View (Medium) — Asked at: Facebook, Amazon
```
BFS level order, take last element of each level.
```

---

## Pattern 8: Graphs

### Concept
BFS for shortest path (unweighted), DFS for exploration/backtracking.
Represent as adjacency list: `Map<Integer, List<Integer>>`.

### Problems

#### P29. Number of Islands (Medium) — Asked at: Amazon, Google, Facebook, Microsoft
```
2D grid of '1' (land) and '0' (water). Count islands.

Hint: DFS/BFS from each unvisited '1', mark visited.
Time: O(m×n)
```

#### P30. Clone Graph (Medium) — Asked at: Facebook, Amazon
```
DFS + HashMap to track cloned nodes. Avoid cycles.
```

#### P31. Course Schedule (Medium) — Asked at: Amazon, Microsoft, Google
```
Topological sort. Detect cycle in directed graph.

Hint: Kahn's algorithm (BFS with in-degree) or DFS with coloring.
```

#### P32. Word Ladder (Hard) — Asked at: Amazon, Google, Facebook
```
BFS shortest path. Each word transformation = one edge.
```

---

## Pattern 9: Dynamic Programming

### Concept
Break problem into overlapping subproblems. Store results (memoization/tabulation).

**Framework:**
1. Define state: `dp[i]` = what?
2. Recurrence relation: `dp[i] = f(dp[i-1], ...)`
3. Base case: `dp[0] = ?`
4. Order of computation
5. Answer location: `dp[n]`?

### Problems

#### P33. Climbing Stairs (Easy) — Asked at: Amazon, Apple
```
dp[i] = dp[i-1] + dp[i-2]  (Fibonacci!)
```

#### P34. House Robber (Medium) — Asked at: Amazon, Google
```
dp[i] = max(dp[i-1], dp[i-2] + nums[i])
Can't rob adjacent houses.
```

#### P35. Longest Increasing Subsequence (Medium) — Asked at: Amazon, Microsoft, Google
```
O(n²): dp[i] = max(dp[j] + 1) for all j < i where nums[j] < nums[i]
O(n log n): Patience sorting (binary search on tails array)
```

#### P36. Coin Change (Medium) — Asked at: Amazon, Goldman Sachs
```
dp[amount] = min coins to make amount
dp[i] = min(dp[i - coin] + 1) for each coin
```

#### P37. 0/1 Knapsack (Medium) — Asked at: Many companies
```
dp[i][w] = max value using first i items with capacity w
dp[i][w] = max(dp[i-1][w], dp[i-1][w-wt[i]] + val[i])
```

#### P38. Longest Common Subsequence (Medium) — Asked at: Amazon, Google
```
dp[i][j] = LCS of s1[0..i-1] and s2[0..j-1]
If s1[i-1] == s2[j-1]: dp[i][j] = dp[i-1][j-1] + 1
Else: dp[i][j] = max(dp[i-1][j], dp[i][j-1])
```

#### P39. Edit Distance (Medium) — Asked at: Google, Amazon
```
dp[i][j] = min operations to convert s1[0..i-1] to s2[0..j-1]
Insert, Delete, Replace operations.
```

---

## Pattern 10: Backtracking

### Concept
Try all possibilities, undo bad choices. Template:
```java
void backtrack(state, choices) {
    if (isGoal(state)) { result.add(copy(state)); return; }
    for (choice : choices) {
        make(choice);
        backtrack(state, remainingChoices);
        undo(choice);  // BACKTRACK
    }
}
```

### Problems

#### P40. Subsets (Medium) — Asked at: Amazon, Facebook
```
Input: [1,2,3]
Output: [[], [1], [2], [3], [1,2], [1,3], [2,3], [1,2,3]]
```

#### P41. Permutations (Medium) — Asked at: Amazon, Facebook, Microsoft
```
Input: [1,2,3]
Output: [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
```

#### P42. N-Queens (Hard) — Asked at: Amazon, Google
```
Place N queens on NxN board. No two queens attack each other.
Classic backtracking problem.
```

---

## Pattern 11: Heap / Priority Queue

### Concept
Efficient access to min/max element. O(log n) insert/remove, O(1) peek.

### Problems

#### P43. Kth Largest Element (Medium) — Asked at: Amazon, Facebook, Google
```
Min-heap of size K. Process all elements.
Time: O(n log k)
```

#### P44. Merge K Sorted Lists (Hard) — Asked at: Amazon, Facebook, Google
```
Min-heap of K list heads. Pop min, push next from that list.
Time: O(n log k) where n = total elements
```

#### P45. Find Median from Data Stream (Hard) — Asked at: Amazon, Google, Microsoft
```
Two heaps: maxHeap (lower half) + minHeap (upper half).
Balance sizes. Median = top of larger heap or average of both tops.
```

---

## Pattern 12: Greedy

### Problems

#### P46. Jump Game (Medium) — Asked at: Amazon, Google
```
Track farthest reachable index. Return true if can reach end.
```

#### P47. Merge Intervals (Medium) — Asked at: Amazon, Google, Facebook, Bloomberg
```
Sort by start. If overlap, merge. Else add new interval.
Time: O(n log n)
```

---

## Bonus: String Problems (Very Frequently Asked)

#### P48. Longest Palindromic Substring (Medium) — Asked at: Amazon, Microsoft
```
Expand around center. For each char, expand both directions.
Time: O(n²), Space: O(1)
```

#### P49. String to Integer (atoi) (Medium) — Asked at: Amazon, Microsoft
```
Handle: whitespace, sign, overflow, invalid chars.
Edge cases are the real challenge.
```

#### P50. Implement strStr / KMP (Medium) — Asked at: Amazon, Google
```
Find first occurrence of needle in haystack.
Naive: O(nm), KMP: O(n+m)
```

---

## Cheat Sheet: Complexity Quick Reference

| Data Structure | Access | Search | Insert | Delete |
|---------------|--------|--------|--------|--------|
| Array | O(1) | O(n) | O(n) | O(n) |
| HashMap | — | O(1) | O(1) | O(1) |
| Stack/Queue | O(n) | O(n) | O(1) | O(1) |
| LinkedList | O(n) | O(n) | O(1) | O(1) |
| BST (balanced) | O(log n) | O(log n) | O(log n) | O(log n) |
| Heap | — | O(n) | O(log n) | O(log n) |

| Algorithm | Time | Space |
|-----------|------|-------|
| Binary Search | O(log n) | O(1) |
| BFS/DFS | O(V+E) | O(V) |
| Merge Sort | O(n log n) | O(n) |
| Quick Sort | O(n log n) avg | O(log n) |
| Dijkstra | O(E log V) | O(V) |

---

## Study Plan: 4-Week DSA Sprint

**Week 1:** Arrays, Hashing, Two Pointers, Sliding Window (P1-P12)  
**Week 2:** Stack, Binary Search, Linked List, Trees (P13-P28)  
**Week 3:** Graphs, DP, Backtracking (P29-P42)  
**Week 4:** Heap, Greedy, Strings, Revision (P43-P50)  

> Solve on LeetCode. Track your progress. Revisit wrong answers after 3 days.
