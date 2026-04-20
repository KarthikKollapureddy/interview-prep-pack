# DSA Approach Guide — Pattern-Based Problem Solving

> Concepts from [kdn251/interviews](https://github.com/kdn251/interviews), [williamfiset/Algorithms](https://github.com/williamfiset/Algorithms), [trekhleb/javascript-algorithms](https://github.com/trekhleb/javascript-algorithms)
> Covers: How to approach each pattern, template code, complexity analysis, common mistakes
> **Priority: P0** — Coding rounds test pattern recognition, not memorization

---

## Pattern Recognition Framework

```
Read Problem → Identify Pattern → Apply Template → Optimize

Step 1: What data structure is natural?
  Array sorted?         → Binary Search / Two Pointers
  Need O(1) lookup?     → HashMap / HashSet
  Need ordering?        → TreeMap / PriorityQueue
  Hierarchical data?    → Tree / Graph
  Undo/history?         → Stack
  Level-by-level?       → Queue / BFS

Step 2: What's the relationship?
  Subarray/substring?   → Sliding Window
  Subsequence?          → DP
  Permutations?         → Backtracking
  Shortest path?        → BFS (unweighted) / Dijkstra (weighted)
  Connected components? → DFS / Union-Find
  Top K elements?       → Heap
  Intervals?            → Sort by start, merge/sweep
```

---

## 1. Two Pointers Pattern

```
When: Sorted array, find pair/triplet, remove duplicates, merge

Template (Opposite Direction):
  int left = 0, right = arr.length - 1;
  while (left < right) {
      int sum = arr[left] + arr[right];
      if (sum == target) return new int[]{left, right};
      else if (sum < target) left++;
      else right--;
  }

Template (Same Direction — Fast/Slow):
  int slow = 0;
  for (int fast = 0; fast < arr.length; fast++) {
      if (arr[fast] != val) {
          arr[slow++] = arr[fast];  // keep non-val elements
      }
  }

Classic Problems:
  ┌────────────────────────────┬───────────┬─────────────────────┐
  │ Problem                    │ Time      │ Key Insight         │
  ├────────────────────────────┼───────────┼─────────────────────┤
  │ Two Sum II (sorted)        │ O(n)      │ Opposite pointers   │
  │ 3Sum                       │ O(n²)     │ Fix one + 2-pointer │
  │ Container With Most Water  │ O(n)      │ Move shorter side   │
  │ Remove Duplicates (sorted) │ O(n)      │ Same direction      │
  │ Trapping Rain Water        │ O(n)      │ Two pointers + max  │
  │ Valid Palindrome           │ O(n)      │ Opposite + skip     │
  └────────────────────────────┴───────────┴─────────────────────┘

Common Mistake: Forgetting to sort first for 3Sum (must sort!)
```

---

## 2. Sliding Window Pattern

```
When: Contiguous subarray/substring, fixed or variable size window

Template (Variable Size):
  int left = 0, maxLen = 0;
  Map<Character, Integer> window = new HashMap<>();

  for (int right = 0; right < s.length(); right++) {
      // Expand: add s[right] to window
      window.merge(s.charAt(right), 1, Integer::sum);

      // Shrink: while window is invalid
      while (windowInvalid(window)) {
          // Remove s[left] from window
          window.merge(s.charAt(left), -1, Integer::sum);
          left++;
      }

      // Update answer
      maxLen = Math.max(maxLen, right - left + 1);
  }

Template (Fixed Size K):
  int sum = 0;
  for (int i = 0; i < k; i++) sum += arr[i];  // first window
  int maxSum = sum;
  for (int i = k; i < arr.length; i++) {
      sum += arr[i] - arr[i - k];  // slide: add right, remove left
      maxSum = Math.max(maxSum, sum);
  }

Classic Problems:
  ┌─────────────────────────────────┬───────┬──────────────────────┐
  │ Problem                         │ Time  │ Window Type          │
  ├─────────────────────────────────┼───────┼──────────────────────┤
  │ Longest Substring No Repeat     │ O(n)  │ Variable (HashSet)   │
  │ Minimum Window Substring        │ O(n)  │ Variable (HashMap)   │
  │ Max Sum Subarray Size K         │ O(n)  │ Fixed                │
  │ Fruits Into Baskets             │ O(n)  │ Variable (≤2 types)  │
  │ Longest Repeating Replacement   │ O(n)  │ Variable (count+K)   │
  │ Permutation in String           │ O(n)  │ Fixed (freq match)   │
  └─────────────────────────────────┴───────┴──────────────────────┘

Key Insight: Variable window → expand right, shrink left while invalid
```

---

## 3. Binary Search Pattern

```
When: Sorted data, find target/boundary, minimize/maximize answer

Template (Standard):
  int left = 0, right = arr.length - 1;
  while (left <= right) {
      int mid = left + (right - left) / 2;  // avoid overflow
      if (arr[mid] == target) return mid;
      else if (arr[mid] < target) left = mid + 1;
      else right = mid - 1;
  }
  return -1;  // not found

Template (Left Boundary — first occurrence):
  int left = 0, right = arr.length;
  while (left < right) {
      int mid = left + (right - left) / 2;
      if (arr[mid] < target) left = mid + 1;
      else right = mid;  // don't skip mid
  }
  return left;  // first index >= target

Template (Binary Search on Answer):
  // "What's the minimum capacity to ship in D days?"
  int left = maxWeight, right = totalWeight;
  while (left < right) {
      int mid = left + (right - left) / 2;
      if (canShipInDays(mid, D)) right = mid;
      else left = mid + 1;
  }
  return left;

Classic Problems:
  ┌────────────────────────────────┬───────────┬──────────────────────┐
  │ Problem                        │ Time      │ Variant              │
  ├────────────────────────────────┼───────────┼──────────────────────┤
  │ Search in Rotated Array        │ O(log n)  │ Modified BS          │
  │ Find Min in Rotated Array      │ O(log n)  │ Boundary search      │
  │ Koko Eating Bananas            │ O(n log m)│ BS on answer         │
  │ Capacity to Ship Packages      │ O(n log s)│ BS on answer         │
  │ Median of Two Sorted Arrays    │ O(log mn) │ Partition-based BS   │
  │ Search 2D Matrix               │ O(log mn) │ Treat as 1D          │
  └────────────────────────────────┴───────────┴──────────────────────┘

Key Insight: "Binary search on answer" — if you can verify a solution,
  binary search the solution space
```

---

## 4. Dynamic Programming Pattern

```
When: Overlapping subproblems + optimal substructure
Signs: "minimum cost", "maximum profit", "number of ways", "is it possible"

DP Framework:
  1. Define state: dp[i] = answer for subproblem i
  2. Find recurrence: dp[i] = f(dp[i-1], dp[i-2], ...)
  3. Base case: dp[0] = ...
  4. Build order: bottom-up (iterative) or top-down (memoization)

Common DP Categories:
  ┌───────────────────────┬───────────────────────────────────────┐
  │ Category              │ Examples                              │
  ├───────────────────────┼───────────────────────────────────────┤
  │ 1D DP                 │ Climbing Stairs, House Robber,        │
  │                       │ Coin Change, Decode Ways              │
  │ 2D DP                 │ Unique Paths, LCS, Edit Distance      │
  │ Knapsack              │ 0/1 Knapsack, Partition Equal Subset  │
  │ String DP             │ Longest Palindrome, Edit Distance     │
  │ Interval DP           │ Burst Balloons, Matrix Chain          │
  │ State Machine DP      │ Buy/Sell Stock, House Robber          │
  │ Tree DP               │ Max Path Sum, House Robber III        │
  └───────────────────────┴───────────────────────────────────────┘

Template (1D Bottom-Up):
  // Coin Change: min coins to make amount
  int[] dp = new int[amount + 1];
  Arrays.fill(dp, amount + 1);
  dp[0] = 0;
  for (int i = 1; i <= amount; i++) {
      for (int coin : coins) {
          if (coin <= i) {
              dp[i] = Math.min(dp[i], dp[i - coin] + 1);
          }
      }
  }
  return dp[amount] > amount ? -1 : dp[amount];

Template (2D — LCS):
  int[][] dp = new int[m + 1][n + 1];
  for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
          if (s1.charAt(i-1) == s2.charAt(j-1))
              dp[i][j] = dp[i-1][j-1] + 1;
          else
              dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
      }
  }

Space Optimization: If dp[i] only depends on dp[i-1],
  use 1D array (rolling array technique):
  int[] dp = new int[n + 1];  // instead of int[m+1][n+1]

Common Mistake: Wrong state definition leads to wrong recurrence.
  Always verify with small examples before coding.
```

---

## 5. Graph Patterns (BFS/DFS)

```
When: Connected components, shortest path, cycle detection, topological sort

BFS Template (Shortest Path in Unweighted Graph):
  Queue<int[]> queue = new LinkedList<>();
  boolean[][] visited = new boolean[m][n];
  queue.offer(new int[]{startR, startC});
  visited[startR][startC] = true;
  int level = 0;

  while (!queue.isEmpty()) {
      int size = queue.size();
      for (int i = 0; i < size; i++) {
          int[] curr = queue.poll();
          if (curr[0] == endR && curr[1] == endC) return level;

          for (int[] dir : dirs) {
              int nr = curr[0] + dir[0], nc = curr[1] + dir[1];
              if (nr >= 0 && nr < m && nc >= 0 && nc < n
                  && !visited[nr][nc] && grid[nr][nc] != 1) {
                  visited[nr][nc] = true;
                  queue.offer(new int[]{nr, nc});
              }
          }
      }
      level++;
  }

DFS Template (Connected Components):
  void dfs(int[][] grid, int r, int c, boolean[][] visited) {
      if (r < 0 || r >= m || c < 0 || c >= n
          || visited[r][c] || grid[r][c] == 0) return;
      visited[r][c] = true;
      for (int[] dir : dirs) dfs(grid, r + dir[0], c + dir[1], visited);
  }

Topological Sort (Kahn's Algorithm — BFS):
  // For DAG: dependency ordering
  int[] indegree = new int[n];
  for (int[] edge : edges) indegree[edge[1]]++;

  Queue<Integer> queue = new LinkedList<>();
  for (int i = 0; i < n; i++)
      if (indegree[i] == 0) queue.offer(i);

  List<Integer> order = new ArrayList<>();
  while (!queue.isEmpty()) {
      int node = queue.poll();
      order.add(node);
      for (int neighbor : graph.get(node)) {
          if (--indegree[neighbor] == 0)
              queue.offer(neighbor);
      }
  }
  // If order.size() < n → cycle exists!

Classic Problems:
  ┌──────────────────────────────┬────────────────┬───────────────────┐
  │ Problem                      │ Algorithm      │ Key Insight       │
  ├──────────────────────────────┼────────────────┼───────────────────┤
  │ Number of Islands            │ DFS/BFS        │ Count components  │
  │ Rotting Oranges              │ Multi-source BFS│ All rotten at t=0│
  │ Course Schedule              │ Topological Sort│ Cycle detection   │
  │ Word Ladder                  │ BFS            │ Each word = node  │
  │ Clone Graph                  │ DFS + HashMap  │ Map old→new       │
  │ Pacific Atlantic Water Flow  │ Reverse DFS    │ DFS from edges    │
  └──────────────────────────────┴────────────────┴───────────────────┘
```

---

## 6. Tree Patterns

```
Three Traversal Templates:
  // Recursive
  void inorder(TreeNode node) {
      if (node == null) return;
      inorder(node.left);
      process(node);        // In-order: left → root → right
      inorder(node.right);
  }

  // Iterative (Stack-based Inorder)
  Stack<TreeNode> stack = new Stack<>();
  TreeNode curr = root;
  while (curr != null || !stack.isEmpty()) {
      while (curr != null) {
          stack.push(curr);
          curr = curr.left;
      }
      curr = stack.pop();
      process(curr);
      curr = curr.right;
  }

  // Level Order (BFS)
  Queue<TreeNode> queue = new LinkedList<>();
  queue.offer(root);
  while (!queue.isEmpty()) {
      int size = queue.size();
      for (int i = 0; i < size; i++) {
          TreeNode node = queue.poll();
          process(node);
          if (node.left != null) queue.offer(node.left);
          if (node.right != null) queue.offer(node.right);
      }
  }

Common Tree Patterns:
  Height/Depth: return 1 + max(height(left), height(right))
  Path Sum:     carry running sum, check at leaves
  LCA:          if both sides return non-null, current is LCA
  Serialize:    preorder + null markers → "1,2,#,#,3,4,#,#,5,#,#"

BST Property: left < root < right
  → Inorder traversal gives sorted output
  → Search/Insert/Delete: O(log n) average, O(n) worst (skewed)
  → Validate BST: inorder and check ascending
```

---

## 7. Heap / Priority Queue Pattern

```
When: Top K, K-th largest, merge K sorted, scheduling

Template (Top K Elements):
  // K largest — use MIN heap of size K
  PriorityQueue<Integer> minHeap = new PriorityQueue<>();
  for (int num : nums) {
      minHeap.offer(num);
      if (minHeap.size() > k) minHeap.poll();  // remove smallest
  }
  return minHeap.peek();  // k-th largest

Template (Merge K Sorted Lists):
  PriorityQueue<ListNode> pq = new PriorityQueue<>(
      (a, b) -> a.val - b.val);
  for (ListNode head : lists)
      if (head != null) pq.offer(head);

  ListNode dummy = new ListNode(0), curr = dummy;
  while (!pq.isEmpty()) {
      ListNode node = pq.poll();
      curr.next = node;
      curr = curr.next;
      if (node.next != null) pq.offer(node.next);
  }

Classic Problems:
  ┌────────────────────────────┬───────────────┬───────────────────────┐
  │ Problem                    │ Heap Type     │ Time                  │
  ├────────────────────────────┼───────────────┼───────────────────────┤
  │ Kth Largest Element        │ Min (size K)  │ O(n log k)            │
  │ Top K Frequent Elements    │ Min (size K)  │ O(n log k)            │
  │ Merge K Sorted Lists       │ Min           │ O(n log k)            │
  │ Find Median from Stream    │ Max + Min     │ O(log n) per insert   │
  │ Task Scheduler             │ Max           │ O(n log 26)           │
  │ Meeting Rooms II           │ Min (end time)│ O(n log n)            │
  └────────────────────────────┴───────────────┴───────────────────────┘

Two Heap Pattern (Median):
  maxHeap (left half) ← | → minHeap (right half)
  Invariant: maxHeap.peek() <= minHeap.peek()
             sizes differ by at most 1
  Median = maxHeap.peek() or avg(maxHeap.peek(), minHeap.peek())
```

---

## 8. Backtracking Pattern

```
When: Generate all combinations/permutations/subsets, constraint satisfaction

Template:
  void backtrack(List<List<Integer>> result, List<Integer> current,
                 int[] nums, int start) {
      result.add(new ArrayList<>(current));  // add current state

      for (int i = start; i < nums.length; i++) {
          // Skip duplicates (for problems with duplicate elements)
          if (i > start && nums[i] == nums[i-1]) continue;

          current.add(nums[i]);             // choose
          backtrack(result, current, nums, i + 1); // explore
          current.remove(current.size() - 1); // un-choose (backtrack)
      }
  }

Variations:
  Subsets:      start from index i, don't reuse → backtrack(i+1)
  Permutations: use visited[] array, start from 0
  Combinations: start from index i, target sum decreases
  With repeats:  backtrack(i) instead of backtrack(i+1)

Classic Problems:
  ┌────────────────────────────┬───────────────────────────────────┐
  │ Problem                    │ Key Decision                      │
  ├────────────────────────────┼───────────────────────────────────┤
  │ Subsets                    │ Include or skip each element      │
  │ Permutations               │ Place each unused element next    │
  │ Combination Sum            │ Can reuse elements (i not i+1)   │
  │ Letter Combinations Phone  │ Choose one char per digit         │
  │ N-Queens                   │ Place queen row by row            │
  │ Word Search                │ DFS in 4 directions, mark visited│
  │ Palindrome Partitioning    │ Try all partition points          │
  └────────────────────────────┴───────────────────────────────────┘

Time Complexity: Usually exponential
  Subsets: O(2^n), Permutations: O(n!), N-Queens: O(n!)
```

---

## 9. Interval Pattern

```
When: Overlapping intervals, merge, insert, minimum rooms

Template (Merge Intervals):
  Arrays.sort(intervals, (a, b) -> a[0] - b[0]);  // sort by start
  List<int[]> merged = new ArrayList<>();
  for (int[] interval : intervals) {
      if (merged.isEmpty() || merged.getLast()[1] < interval[0]) {
          merged.add(interval);  // no overlap
      } else {
          merged.getLast()[1] = Math.max(
              merged.getLast()[1], interval[1]);  // merge
      }
  }

Key Insight: Always sort by start time first.
  Overlap condition: prev.end >= curr.start

Classic Problems:
  ┌────────────────────────────┬───────────────────────────────────┐
  │ Problem                    │ Approach                          │
  ├────────────────────────────┼───────────────────────────────────┤
  │ Merge Intervals            │ Sort + merge overlapping          │
  │ Insert Interval            │ Find position + merge             │
  │ Non-overlapping Intervals  │ Greedy: keep shorter end          │
  │ Meeting Rooms I            │ Sort + check overlap              │
  │ Meeting Rooms II           │ Sort starts+ends or min-heap      │
  │ Minimum Platforms          │ Count concurrent events           │
  └────────────────────────────┴───────────────────────────────────┘
```

---

## 10. Complexity Cheat Sheet

```
┌────────────────────┬───────────┬────────────────────────────┐
│ Algorithm          │ Time      │ Space                      │
├────────────────────┼───────────┼────────────────────────────┤
│ Binary Search      │ O(log n)  │ O(1)                       │
│ Two Pointers       │ O(n)      │ O(1)                       │
│ Sliding Window     │ O(n)      │ O(k) window                │
│ HashMap lookup     │ O(1) avg  │ O(n)                       │
│ Sorting            │ O(n log n)│ O(n) or O(log n)           │
│ BFS/DFS            │ O(V + E)  │ O(V)                       │
│ Dijkstra           │ O(E log V)│ O(V)                       │
│ DP (1D)            │ O(n)      │ O(n) or O(1) optimized     │
│ DP (2D)            │ O(mn)     │ O(mn) or O(n) optimized    │
│ Heap operations    │ O(log n)  │ O(n)                       │
│ Backtracking       │ O(2^n)/O(n!)│ O(n) recursion stack     │
│ Trie operations    │ O(L)      │ O(ALPHABET * L * N)        │
└────────────────────┴───────────┴────────────────────────────┘

Interview Tips:
  1. Always state time AND space complexity
  2. Mention average vs worst case if different
  3. Amortized analysis: "O(1) amortized for ArrayList.add()"
  4. When asked to optimize: move from O(n²) → O(n log n) → O(n)
```

---

*Sources: [kdn251/interviews](https://github.com/kdn251/interviews), [williamfiset/Algorithms](https://github.com/williamfiset/Algorithms), [NeetCode patterns](https://neetcode.io/)*
