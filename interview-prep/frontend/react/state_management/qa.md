# React State Management — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What are the different types of state in React?

| Type | Scope | Example | Tool |
|------|-------|---------|------|
| **Local** | Single component | Form input, toggle | `useState` |
| **Lifted** | Parent + children | Shared filter state | Lift to common parent |
| **Context** | Subtree | Theme, auth, locale | `useContext` |
| **Global** | Entire app | Shopping cart, user session | Redux, Zustand |
| **Server** | Remote data | API responses | React Query, SWR |
| **URL** | Navigation | Filters, pagination | `useSearchParams` |

**Rule of thumb:** Keep state as close to where it's used as possible. Don't put everything in global state.

---

### Q2. Explain Context API — `createContext`, `Provider`, `useContext`.

```jsx
// 1. Create context
const AuthContext = createContext(null);

// 2. Provider wraps the tree
function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const login = async (credentials) => { /* ... */ };
  const logout = () => setUser(null);

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// 3. Custom hook for consuming (cleaner than raw useContext)
function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

// 4. Usage in any component
function Navbar() {
  const { user, logout } = useAuth();
  return user ? <button onClick={logout}>Logout {user.name}</button> : <LoginButton />;
}
```

---

### Q3. Redux vs Context API — when to use which?

| Criteria | Context API | Redux / Zustand |
|----------|-------------|-----------------|
| Complexity | Simple | More boilerplate (Redux) |
| Updates | Re-renders all consumers on any change | Selective subscriptions |
| DevTools | No | Redux DevTools (time travel debugging) |
| Middleware | No | Redux Saga/Thunk, async flows |
| Best for | Theme, auth, locale (low-frequency changes) | Complex state, frequent updates |

**Context re-render problem:**
```jsx
// ❌ All consumers re-render when ANY context value changes
<AppContext.Provider value={{ user, theme, cart, notifications }}>
```

**Fix:** Split into separate contexts or use Redux/Zustand.

---

### Q4. Explain Redux Toolkit (RTK) — the modern way to use Redux.

```jsx
// 1. Create a slice (reducer + actions)
import { createSlice, configureStore } from '@reduxjs/toolkit';

const cartSlice = createSlice({
  name: 'cart',
  initialState: { items: [], total: 0 },
  reducers: {
    addItem: (state, action) => {
      state.items.push(action.payload); // Immer handles immutability!
      state.total += action.payload.price;
    },
    removeItem: (state, action) => {
      const index = state.items.findIndex(i => i.id === action.payload);
      if (index !== -1) {
        state.total -= state.items[index].price;
        state.items.splice(index, 1);
      }
    },
    clearCart: () => ({ items: [], total: 0 }),
  },
});

export const { addItem, removeItem, clearCart } = cartSlice.actions;

// 2. Store
const store = configureStore({
  reducer: { cart: cartSlice.reducer },
});

// 3. Usage
function CartButton() {
  const itemCount = useSelector(state => state.cart.items.length);
  const dispatch = useDispatch();
  return <button onClick={() => dispatch(clearCart())}>Cart ({itemCount})</button>;
}
```

**RTK Query for API calls:**
```jsx
const api = createApi({
  baseQuery: fetchBaseQuery({ baseUrl: '/api/v1' }),
  endpoints: (builder) => ({
    getShipments: builder.query({ query: () => '/shipments' }),
    createShipment: builder.mutation({
      query: (body) => ({ url: '/shipments', method: 'POST', body }),
    }),
  }),
});
export const { useGetShipmentsQuery, useCreateShipmentMutation } = api;
```

---

## Scenario-Based Questions

### Q5. At FedEx, how would you structure state management for a shipment tracking dashboard?

```
State Strategy:
├── Auth state        → Context (useAuth hook)
├── Theme/locale      → Context (useTheme hook)
├── Shipment list     → React Query (server state, cached, auto-refetch)
├── Selected filters  → URL params (useSearchParams)
├── Notification count → WebSocket + Zustand (real-time global state)
└── Form state        → Local useState (ephemeral)
```

```jsx
// Server state with React Query — handles caching, refetching, loading
function ShipmentDashboard() {
  const [filters] = useSearchParams();
  const { data, isLoading, error } = useQuery({
    queryKey: ['shipments', filters.toString()],
    queryFn: () => fetchShipments(Object.fromEntries(filters)),
    staleTime: 30_000, // Data considered fresh for 30s
    refetchInterval: 60_000, // Auto-refresh every 60s
  });

  if (isLoading) return <Skeleton />;
  if (error) return <ErrorBanner />;
  return <ShipmentTable data={data} />;
}
```

---

### Q6. At Hatio, how do you handle optimistic updates in the shopping cart?

```jsx
const queryClient = useQueryClient();

const addToCartMutation = useMutation({
  mutationFn: (item) => api.addToCart(item),
  onMutate: async (newItem) => {
    // Cancel outgoing refetches
    await queryClient.cancelQueries({ queryKey: ['cart'] });
    
    // Snapshot previous value
    const previousCart = queryClient.getQueryData(['cart']);
    
    // Optimistically update
    queryClient.setQueryData(['cart'], (old) => ({
      ...old,
      items: [...old.items, newItem],
      total: old.total + newItem.price,
    }));

    return { previousCart }; // Return context for rollback
  },
  onError: (err, newItem, context) => {
    // Rollback on error
    queryClient.setQueryData(['cart'], context.previousCart);
    toast.error('Failed to add item');
  },
  onSettled: () => {
    // Refetch to sync with server
    queryClient.invalidateQueries({ queryKey: ['cart'] });
  },
});
```

---

## Coding Challenges

### Challenge 1: Mini Redux from Scratch
**File:** `solutions/MiniRedux.jsx`  
Implement a simplified Redux:
1. `createStore(reducer)` with `getState()`, `dispatch()`, `subscribe()`
2. A `Provider` component using Context
3. `useSelector` and `useDispatch` hooks
4. Test with a counter + todo list

### Challenge 2: Shopping Cart with Context
**File:** `solutions/ShoppingCart.jsx`  
Build a shopping cart using Context + useReducer:
1. CartProvider with add, remove, update quantity, clear
2. CartSummary showing item count and total
3. ProductList that dispatches add actions
4. Persist cart to localStorage

---

## Gotchas & Edge Cases

### Q7. Why does my Context provider cause unnecessary re-renders?

```jsx
// ❌ New object every render → all consumers re-render
function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  return (
    <AppContext.Provider value={{ user, setUser }}>  {/* New object ref every render */}
      {children}
    </AppContext.Provider>
  );
}

// ✅ Memoize the context value
function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const value = useMemo(() => ({ user, setUser }), [user]);
  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
```

---

### Q8. React Query vs Redux — can they coexist?

**Yes, and they should.** They solve different problems:
- **React Query** → server state (API data)
- **Redux/Zustand** → client state (UI state, user preferences)

Don't store API responses in Redux — React Query handles caching, background refresh, and stale data automatically.
