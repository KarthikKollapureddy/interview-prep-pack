# React Performance Optimization — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. How does React's reconciliation (diffing) algorithm work?

React compares the new Virtual DOM tree with the previous one and applies minimal updates to the real DOM.

**Key heuristics:**
1. **Different element types** → tear down old tree, build new (`<div>` → `<span>` = full rebuild)
2. **Same element type** → update attributes only (`<div className="a">` → `<div className="b">`)
3. **Lists** → use `key` prop to match children across renders

```jsx
// ❌ Without keys: React re-renders entire list
{items.map(item => <Item data={item} />)}

// ✅ With keys: React only updates changed items
{items.map(item => <Item key={item.id} data={item} />)}
```

**Never use array index as key** if list items can be reordered, added, or removed — causes incorrect state association.

---

### Q2. What is `React.memo`? When to use it?

```jsx
// Memoized component — only re-renders if props change (shallow comparison)
const ShipmentCard = React.memo(({ shipment, onSelect }) => {
  console.log('Rendering:', shipment.id);
  return (
    <div onClick={() => onSelect(shipment.id)}>
      <h3>{shipment.trackingNumber}</h3>
      <p>{shipment.status}</p>
    </div>
  );
});

// Custom comparison function
const ShipmentCard = React.memo(({ shipment }) => { /*...*/ }, 
  (prevProps, nextProps) => prevProps.shipment.id === nextProps.shipment.id
);
```

**Use when:**
- Component renders often with same props
- Component is expensive to render (large lists, charts)
- Parent re-renders frequently but child props don't change

**Don't use when:** Component is cheap, or props always change.

---

### Q3. Explain code splitting and lazy loading.

```jsx
// Lazy load heavy components
const Dashboard = lazy(() => import('./Dashboard'));
const Analytics = lazy(() => import('./Analytics'));

function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/analytics" element={<Analytics />} />
      </Routes>
    </Suspense>
  );
}
```

**What it does:** Splits the JavaScript bundle into chunks. Each route/component is loaded only when needed → faster initial page load.

**Route-based splitting** (most common) + **Component-based splitting** (for heavy modals, charts).

---

### Q4. What is virtualization? When do you need it?

**Problem:** Rendering 10,000 items in a list creates 10,000 DOM nodes → slow scrolling, high memory.

**Solution:** Only render items visible in the viewport (+ a small buffer).

```jsx
import { FixedSizeList } from 'react-window';

function ShipmentList({ shipments }) {
  return (
    <FixedSizeList
      height={600}
      itemCount={shipments.length}
      itemSize={80}
      width="100%"
    >
      {({ index, style }) => (
        <div style={style}>
          <ShipmentRow data={shipments[index]} />
        </div>
      )}
    </FixedSizeList>
  );
}
```

**Libraries:** `react-window` (lightweight), `react-virtuoso` (auto-sizing), `@tanstack/virtual`.

---

## Scenario-Based Questions

### Q5. At FedEx, the shipment tracking dashboard re-renders all 500 shipment cards when the search input changes. How do you fix it?

```jsx
// Problem: SearchInput re-renders → parent re-renders → all ShipmentCards re-render

// Fix 1: Memo the card component
const ShipmentCard = React.memo(({ shipment }) => { /*...*/ });

// Fix 2: Stabilize callback references with useCallback
const handleSelect = useCallback((id) => {
  setSelectedId(id);
}, []);

// Fix 3: Separate search state from list (lift down, not up)
function SearchableList({ shipments }) {
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => 
    shipments.filter(s => s.trackingNumber.includes(query)),
    [shipments, query]
  );
  
  return (
    <>
      <SearchInput value={query} onChange={setQuery} />
      <VirtualizedList items={filtered} onSelect={handleSelect} />
    </>
  );
}
```

---

### Q6. At Hatio, the payment history page loads all transactions upfront and is slow. How do you optimize?

1. **Pagination** — load 20 per page (simplest)
2. **Infinite scroll** with intersection observer (better UX)
3. **Virtualization** — if using infinite scroll with 1000+ items in memory
4. **Server-side filtering** — move date range, amount filters to API (don't send all data)

```jsx
function TransactionHistory() {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: ['transactions'],
    queryFn: ({ pageParam = 0 }) => fetchTransactions({ page: pageParam }),
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.page + 1 : undefined,
  });

  const observerRef = useRef(null);
  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && hasNextPage) fetchNextPage();
    });
    if (observerRef.current) observer.observe(observerRef.current);
    return () => observer.disconnect();
  }, [hasNextPage, fetchNextPage]);

  return (
    <div>
      {data?.pages.flatMap(page => page.items).map(txn => <TxnRow key={txn.id} data={txn} />)}
      <div ref={observerRef}>{isFetchingNextPage && <Spinner />}</div>
    </div>
  );
}
```

---

## Coding Challenges

### Challenge 1: Performance Audit Dashboard
**File:** `solutions/PerformanceDashboard.jsx`  
Build a dashboard that demonstrates performance patterns:
1. Virtualized list of 10,000 items
2. Memoized row components
3. Debounced search filter
4. Lazy-loaded detail panel
5. Measure and display render counts

### Challenge 2: Optimized Data Table
**File:** `solutions/OptimizedTable.jsx`  
Build a data table with:
1. Sortable columns (memoized sort logic)
2. Filterable rows (debounced input)
3. Pagination (client-side)
4. Row selection without re-rendering unselected rows
5. Column resize (no full re-render)

---

## Gotchas & Edge Cases

### Q7. When does `useMemo` / `useCallback` NOT help?

```jsx
// ❌ Memoizing primitive values (JS already optimizes these)
const name = useMemo(() => `${first} ${last}`, [first, last]);
// String concatenation is already fast — useMemo overhead > computation cost

// ❌ Memoizing values that change every render
const data = useMemo(() => items.filter(i => i.active), [items]);
// If `items` changes every render, memo cache is always busted

// ❌ useCallback without React.memo on the child
const handleClick = useCallback(() => setCount(c => c + 1), []);
// Useless if child isn't wrapped in React.memo — child re-renders regardless
```

**Profile first, optimize second.** Use React DevTools Profiler to find actual bottlenecks.

---

### Q8. What is React's `startTransition` and `useDeferredValue`?

```jsx
// startTransition — mark updates as non-urgent
function SearchResults({ query }) {
  const [isPending, startTransition] = useTransition();
  const [results, setResults] = useState([]);

  function handleChange(e) {
    startTransition(() => {
      setResults(computeExpensiveResults(e.target.value)); // Won't block input
    });
  }
  return <>{isPending ? <Spinner /> : <List items={results} />}</>;
}

// useDeferredValue — defer a value update
function List({ items }) {
  const deferredItems = useDeferredValue(items);
  // deferredItems lags behind items during heavy renders
  // React prioritizes rendering the input field over the list
}
```
