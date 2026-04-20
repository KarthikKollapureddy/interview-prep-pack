# React Hooks — Interview Q&A

> 16 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What are React Hooks? Why were they introduced?

Hooks let you use state and lifecycle features in **function components** (previously only available in class components).

**Before hooks (class component):**
```jsx
class Counter extends React.Component {
  state = { count: 0 };
  componentDidMount() { document.title = `Count: ${this.state.count}`; }
  componentDidUpdate() { document.title = `Count: ${this.state.count}`; }
  render() {
    return <button onClick={() => this.setState({ count: this.state.count + 1 })}>
      {this.state.count}
    </button>;
  }
}
```

**With hooks:**
```jsx
function Counter() {
  const [count, setCount] = useState(0);
  useEffect(() => { document.title = `Count: ${count}`; }, [count]);
  return <button onClick={() => setCount(c => c + 1)}>{count}</button>;
}
```

**Why hooks:**
- Reuse stateful logic (custom hooks) without HOCs/render props
- Split related logic together (instead of spreading across lifecycle methods)
- Simpler code, easier to test
- No `this` binding confusion

---

### Q2. Explain `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`.

```jsx
// useState — state in function components
const [items, setItems] = useState([]);

// useEffect — side effects (API calls, subscriptions, DOM manipulation)
useEffect(() => {
  fetchShipments().then(setItems);
  return () => { /* cleanup: unsubscribe */ }; // Cleanup on unmount
}, [dependency]); // Re-run when dependency changes. [] = mount only.

// useRef — mutable value that persists across renders without causing re-render
const inputRef = useRef(null);
inputRef.current.focus(); // Direct DOM access

// useMemo — memoize expensive computation
const sortedItems = useMemo(() => 
  items.sort((a, b) => a.date - b.date), [items]);

// useCallback — memoize function reference (prevent child re-renders)
const handleClick = useCallback(() => {
  setCount(c => c + 1);
}, []); // Same function reference across renders
```

**When to memoize:** Only when you have a measurable performance issue. Premature memoization adds complexity without benefit.

---

### Q3. Rules of Hooks — what are they and why?

1. **Only call hooks at the top level** — never inside conditions, loops, or nested functions
2. **Only call hooks from React functions** — function components or custom hooks

```jsx
// ❌ BAD: conditional hook
if (isLoggedIn) {
  const [user, setUser] = useState(null); // React tracks hooks by call order!
}

// ✅ GOOD: conditional logic inside hook
const [user, setUser] = useState(null);
useEffect(() => {
  if (isLoggedIn) fetchUser().then(setUser);
}, [isLoggedIn]);
```

**Why:** React identifies hooks by their call ORDER. If the order changes between renders, React can't match state to the correct hook.

---

### Q4. What is `useReducer`? When to use it over `useState`?

```jsx
const reducer = (state, action) => {
  switch (action.type) {
    case 'ADD_ITEM': return { ...state, items: [...state.items, action.payload] };
    case 'REMOVE_ITEM': return { ...state, items: state.items.filter(i => i.id !== action.payload) };
    case 'SET_LOADING': return { ...state, loading: action.payload };
    case 'SET_ERROR': return { ...state, error: action.payload, loading: false };
    default: return state;
  }
};

const [state, dispatch] = useReducer(reducer, { items: [], loading: false, error: null });

dispatch({ type: 'ADD_ITEM', payload: newItem });
```

**Use `useReducer` when:**
- State logic is complex (multiple related values)
- Next state depends on previous state
- You want predictable state transitions (like Redux)

**Use `useState` when:** Simple, independent state values.

---

## Scenario-Based Questions

### Q5. At FedEx, build a custom hook for fetching shipment data with loading/error states.

```jsx
function useShipment(trackingNumber) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!trackingNumber) return;
    
    const controller = new AbortController(); // Prevent stale responses
    setLoading(true);
    setError(null);

    fetch(`/api/v1/shipments/${trackingNumber}`, { signal: controller.signal })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(setData)
      .catch(err => {
        if (err.name !== 'AbortError') setError(err.message);
      })
      .finally(() => setLoading(false));

    return () => controller.abort(); // Cleanup: cancel on unmount or re-fetch
  }, [trackingNumber]);

  return { data, loading, error };
}

// Usage
function ShipmentTracker({ trackingNumber }) {
  const { data, loading, error } = useShipment(trackingNumber);
  
  if (loading) return <Spinner />;
  if (error) return <ErrorBanner message={error} />;
  return <ShipmentDetails shipment={data} />;
}
```

**Key patterns:** AbortController for cleanup, loading/error states, dependency array.

---

### Q6. At Hatio, build a `useDebounce` hook for search input.

```jsx
function useDebounce(value, delay = 300) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer); // Reset timer if value changes before delay
  }, [value, delay]);

  return debouncedValue;
}

// Usage: search bar
function TransactionSearch() {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 500);

  useEffect(() => {
    if (debouncedQuery) {
      searchTransactions(debouncedQuery).then(setResults);
    }
  }, [debouncedQuery]); // Only fires after 500ms of no typing

  return <input value={query} onChange={e => setQuery(e.target.value)} />;
}
```

---

### Q7. Build a `useLocalStorage` hook for persisting state.

```jsx
function useLocalStorage(key, initialValue) {
  const [value, setValue] = useState(() => {
    try {
      const stored = localStorage.getItem(key);
      return stored ? JSON.parse(stored) : initialValue;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (err) {
      console.warn(`Failed to save to localStorage: ${err}`);
    }
  }, [key, value]);

  return [value, setValue];
}

// Usage
const [theme, setTheme] = useLocalStorage('theme', 'light');
```

---

## Coding Challenges

### Challenge 1: Custom useFetch Hook
**File:** `solutions/UseFetch.jsx`  
Build a production-ready `useFetch` hook:
1. Loading, error, data states
2. AbortController for cleanup
3. Automatic refetch on dependency change
4. Cache responses (avoid refetching same URL)
5. Support for POST/PUT with body

### Challenge 2: useInfiniteScroll Hook
**File:** `solutions/UseInfiniteScroll.jsx`  
Build a hook for infinite scrolling:
1. Detect when user scrolls near bottom (IntersectionObserver)
2. Automatically fetch next page
3. Merge new data with existing
4. Handle loading/error/hasMore states
5. Demo with a mock API

---

## Gotchas & Edge Cases

### Q8. Stale closure problem — what is it?

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      console.log(count); // ❌ Always logs 0 (stale closure!)
      setCount(count + 1); // ❌ Always sets to 1
    }, 1000);
    return () => clearInterval(interval);
  }, []); // Empty deps = captures count=0 forever
}

// ✅ FIX: use functional update
setCount(prevCount => prevCount + 1); // Uses latest value
```

---

### Q9. `useEffect` dependency array — common mistakes?

```jsx
// ❌ Missing dependency (ESLint warns)
useEffect(() => {
  fetchData(userId); // userId is used but not in deps
}, []); // Will never refetch when userId changes

// ❌ Object/array in deps causes infinite loop
useEffect(() => {
  fetchData(filters);
}, [filters]); // If filters = { status: 'active' } is created each render, 
               // new reference every time → infinite loop!

// ✅ FIX: use individual primitive values
useEffect(() => {
  fetchData({ status: filterStatus });
}, [filterStatus]); // Primitive string, stable reference
```

---

### Q10. useContext — share state without prop drilling.

**Answer:**

```jsx
// 1. Create context:
const ThemeContext = React.createContext('light');

// 2. Provider wraps the tree:
function App() {
  const [theme, setTheme] = useState('dark');
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <Header />
      <Main />
    </ThemeContext.Provider>
  );
}

// 3. Consume anywhere (no prop drilling!):
function Header() {
  const { theme, setTheme } = useContext(ThemeContext);
  return (
    <header className={theme}>
      <button onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
        Toggle Theme
      </button>
    </header>
  );
}
```

**⚠️ Performance gotcha:** Every component that calls `useContext(ThemeContext)` re-renders when the context value changes — even if it only uses part of the value.

**Fix:** Split contexts by concern, or use `useMemo` on the value:
```jsx
const value = useMemo(() => ({ theme, setTheme }), [theme]);
<ThemeContext.Provider value={value}>
```

**When to use Context vs Redux/Zustand:**
| Scenario | Context | Redux/Zustand |
|----------|---------|---------------|
| Theme, locale, auth | ✅ | Overkill |
| Frequent updates (typing, drag) | ❌ (re-renders) | ✅ |
| Complex state logic | ❌ | ✅ |
| DevTools, middleware | ❌ | ✅ |
