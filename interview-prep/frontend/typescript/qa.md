# TypeScript Essentials — Interview Q&A

> 10 questions covering types, interfaces, generics, utility types, React+TS patterns  
> Priority: **P1** — Expected for fullstack roles, especially React + TypeScript codebases

---

### Q1. What is TypeScript and why use it over JavaScript?

**Answer:**

TypeScript = JavaScript + Static Type System (compiled to JavaScript).

| Feature | JavaScript | TypeScript |
|---------|-----------|------------|
| Type checking | Runtime only (errors in production!) | Compile-time (errors in IDE) |
| Autocompletion | Limited | Full IntelliSense |
| Refactoring | Risky (no type safety) | Safe (compiler catches breaks) |
| Documentation | Comments only | Types ARE documentation |
| Learning curve | Lower | Slightly higher |

```typescript
// JavaScript — no safety:
function add(a, b) { return a + b; }
add("5", 3);  // "53" — silent bug!

// TypeScript — caught at compile time:
function add(a: number, b: number): number { return a + b; }
add("5", 3);  // ❌ Compile error: Argument of type 'string' is not assignable to 'number'
```

---

### Q2. Explain Type vs Interface — when to use which?

**Answer:**

```typescript
// Interface — defines the shape of an object:
interface User {
  id: number;
  name: string;
  email: string;
  role?: string;           // optional property
  readonly createdAt: Date; // read-only
}

// Type alias — can define any type:
type ID = number | string;            // union type
type Status = "active" | "inactive";  // literal type
type Point = { x: number; y: number };
type Callback = (data: string) => void;  // function type
```

**Key differences:**
| Feature | Interface | Type |
|---------|-----------|------|
| Extend/inherit | `extends` keyword | `&` (intersection) |
| Declaration merging | ✅ (can re-declare to add fields) | ❌ |
| Union types | ❌ | ✅ `type A = B \| C` |
| Primitives, tuples | ❌ | ✅ `type ID = string` |
| Implements (class) | ✅ | ✅ |

```typescript
// Extend interface:
interface Employee extends User {
  department: string;
  salary: number;
}

// Extend type (intersection):
type Employee = User & {
  department: string;
  salary: number;
};

// Declaration merging (only interface):
interface User { id: number; }
interface User { name: string; }
// Now User has both id and name — useful for library augmentation
```

**Rule of thumb:** Use `interface` for object shapes (especially in APIs/props). Use `type` for unions, primitives, tuples, and function signatures.

---

### Q3. Explain Generics in TypeScript.

**Answer:**

```typescript
// Without generics — loses type info:
function identity(arg: any): any {
  return arg;
}
const result = identity("hello");  // type: any (lost!)

// With generics — preserves type:
function identity<T>(arg: T): T {
  return arg;
}
const result = identity("hello");  // type: string ✅
const num = identity(42);          // type: number ✅
```

**Generic interfaces and classes:**
```typescript
// Generic API response:
interface ApiResponse<T> {
  data: T;
  status: number;
  message: string;
}

// Usage:
const userResponse: ApiResponse<User> = await fetchUser(1);
const ordersResponse: ApiResponse<Order[]> = await fetchOrders();

// Generic with constraints:
interface HasId { id: number; }

function findById<T extends HasId>(items: T[], id: number): T | undefined {
  return items.find(item => item.id === id);
}
// T must have an 'id' property

// Generic with default:
interface PaginatedResponse<T, M = {}> {
  data: T[];
  total: number;
  meta: M;
}
```

---

### Q4. Explain the most important Utility Types.

**Answer:**

```typescript
interface User {
  id: number;
  name: string;
  email: string;
  password: string;
  role: "admin" | "user";
}

// Partial<T> — all properties optional:
type UpdateUserDto = Partial<User>;
// { id?: number; name?: string; email?: string; ... }

// Required<T> — all properties required:
type RequiredUser = Required<Partial<User>>;

// Pick<T, K> — select specific properties:
type UserProfile = Pick<User, "id" | "name" | "email">;
// { id: number; name: string; email: string }

// Omit<T, K> — exclude specific properties:
type CreateUserDto = Omit<User, "id">;
// { name: string; email: string; password: string; role: ... }

// Readonly<T> — all properties readonly:
type ImmutableUser = Readonly<User>;
// Cannot modify any property after creation

// Record<K, V> — object with specific key-value types:
type UserRoles = Record<string, "admin" | "user" | "guest">;
// { [key: string]: "admin" | "user" | "guest" }

// Exclude<T, U> — remove types from union:
type NonAdmin = Exclude<"admin" | "user" | "guest", "admin">;
// "user" | "guest"

// Extract<T, U> — keep types in union:
type AdminOnly = Extract<"admin" | "user" | "guest", "admin">;
// "admin"

// ReturnType<T> — get function return type:
function createUser() { return { id: 1, name: "Alice" }; }
type NewUser = ReturnType<typeof createUser>;
// { id: number; name: string }

// Parameters<T> — get function parameter types:
type CreateUserParams = Parameters<typeof createUser>;
// []
```

---

### Q5. Explain Union Types, Intersection Types, and Type Narrowing.

**Answer:**

```typescript
// Union type (OR): value can be one of several types
type Result = string | number;
type Status = "loading" | "success" | "error";

// Intersection type (AND): value must satisfy ALL types
type Employee = User & { department: string; salary: number };

// Discriminated union (tagged union) — VERY common in React:
type ApiState<T> =
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: string };

// Type narrowing — TypeScript narrows the type based on checks:
function handleState(state: ApiState<User>) {
  switch (state.status) {
    case "loading":
      // TS knows: state is { status: "loading" }
      return <Spinner />;
    case "success":
      // TS knows: state has 'data' property
      return <UserCard user={state.data} />;
    case "error":
      // TS knows: state has 'error' property
      return <ErrorMessage message={state.error} />;
  }
}

// Type guards:
function isString(value: unknown): value is string {
  return typeof value === "string";
}

function process(input: string | number) {
  if (isString(input)) {
    console.log(input.toUpperCase());  // TS knows it's string
  } else {
    console.log(input.toFixed(2));     // TS knows it's number
  }
}
```

---

### Q6. React + TypeScript — Component Props Typing.

**Answer:**

```typescript
// Functional component with typed props:
interface ButtonProps {
  label: string;
  onClick: () => void;
  variant?: "primary" | "secondary" | "danger";  // optional with literal types
  disabled?: boolean;
  icon?: React.ReactNode;           // any renderable content
  children?: React.ReactNode;
  className?: string;
}

const Button: React.FC<ButtonProps> = ({ label, onClick, variant = "primary", disabled = false, children }) => {
  return (
    <button className={`btn btn-${variant}`} onClick={onClick} disabled={disabled}>
      {children || label}
    </button>
  );
};

// Usage:
<Button label="Submit" onClick={handleSubmit} variant="primary" />
<Button label="Delete" onClick={handleDelete} variant="danger" disabled={isLoading} />
```

```typescript
// Typing event handlers:
const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  setQuery(e.target.value);
};

const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
};

const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
  console.log(e.currentTarget);
};
```

```typescript
// Generic component (reusable list):
interface ListProps<T> {
  items: T[];
  renderItem: (item: T) => React.ReactNode;
  keyExtractor: (item: T) => string | number;
}

function List<T>({ items, renderItem, keyExtractor }: ListProps<T>) {
  return (
    <ul>
      {items.map(item => (
        <li key={keyExtractor(item)}>{renderItem(item)}</li>
      ))}
    </ul>
  );
}

// Usage — TypeScript infers T from items:
<List
  items={users}
  renderItem={(user) => <span>{user.name}</span>}
  keyExtractor={(user) => user.id}
/>
```

---

### Q7. React Hooks with TypeScript.

**Answer:**

```typescript
// useState — type is inferred, but can be explicit:
const [count, setCount] = useState(0);                    // inferred: number
const [user, setUser] = useState<User | null>(null);      // explicit: User or null
const [items, setItems] = useState<string[]>([]);         // explicit: string array

// useRef:
const inputRef = useRef<HTMLInputElement>(null);          // DOM ref
const timerRef = useRef<number>(0);                       // mutable ref (no DOM)

// useEffect — no type needed (returns void or cleanup fn):
useEffect(() => {
  const controller = new AbortController();
  fetchData(controller.signal);
  return () => controller.abort();  // cleanup
}, [dependency]);

// useReducer:
type Action =
  | { type: "INCREMENT" }
  | { type: "DECREMENT" }
  | { type: "SET"; payload: number };

interface State {
  count: number;
}

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "INCREMENT": return { count: state.count + 1 };
    case "DECREMENT": return { count: state.count - 1 };
    case "SET":       return { count: action.payload };
  }
}

const [state, dispatch] = useReducer(reducer, { count: 0 });
dispatch({ type: "SET", payload: 42 });  // ✅ type-safe
dispatch({ type: "UNKNOWN" });           // ❌ compile error

// useContext:
interface ThemeContextType {
  theme: "light" | "dark";
  toggle: () => void;
}
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function useTheme(): ThemeContextType {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be within ThemeProvider");
  return context;
}

// Custom hook with typed return:
function useFetch<T>(url: string): { data: T | null; loading: boolean; error: string | null } {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(url)
      .then(res => res.json())
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [url]);

  return { data, loading, error };
}

// Usage:
const { data: users, loading } = useFetch<User[]>("/api/users");
```

---

### Q8. Explain `unknown` vs `any` vs `never`.

**Answer:**

```typescript
// any — opt out of type checking (AVOID):
let a: any = "hello";
a.foo.bar.baz();  // no error — TypeScript doesn't check anything
// Defeats the purpose of TypeScript

// unknown — type-safe version of any (PREFER):
let b: unknown = "hello";
b.toUpperCase();            // ❌ Error! Must narrow type first
if (typeof b === "string") {
  b.toUpperCase();          // ✅ OK after type check
}

// never — represents values that never occur:
function throwError(msg: string): never {
  throw new Error(msg);  // function never returns
}

// Exhaustive check:
type Shape = "circle" | "square";
function area(shape: Shape): number {
  switch (shape) {
    case "circle": return Math.PI * 10;
    case "square": return 100;
    default:
      const _exhaustive: never = shape;  // compile error if a case is missed
      return _exhaustive;
  }
}
```

---

### Q9. How do you type API responses and fetch calls?

**Answer:**

```typescript
// Define response types:
interface User {
  id: number;
  name: string;
  email: string;
}

interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}

// Type-safe fetch wrapper:
async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

// Usage:
const user = await api<User>("/api/users/1");
const users = await api<PaginatedResponse<User>>("/api/users?page=1");

// With Axios (already typed):
const { data } = await axios.get<User>("/api/users/1");
// data is typed as User automatically
```

---

### Q10. Common TypeScript mistakes and how to avoid them.

**Answer:**

```typescript
// ❌ Mistake 1: Using 'any' everywhere
function processData(data: any) { ... }
// ✅ Fix: Use proper types or 'unknown' + narrowing

// ❌ Mistake 2: Not handling null/undefined
const user = users.find(u => u.id === 1);
console.log(user.name);  // ❌ 'user' is possibly undefined
// ✅ Fix: Optional chaining or guard
console.log(user?.name);
if (user) console.log(user.name);

// ❌ Mistake 3: Type assertion instead of type guard
const input = document.getElementById("name") as HTMLInputElement;
// ✅ Safer: Check first
const input = document.getElementById("name");
if (input instanceof HTMLInputElement) { input.value = "hello"; }

// ❌ Mistake 4: Enum for simple unions
enum Status { Active, Inactive }  // generates runtime code
// ✅ Use const assertion or literal union:
type Status = "active" | "inactive";  // zero runtime overhead
// Or: const STATUS = { ACTIVE: "active", INACTIVE: "inactive" } as const;

// ❌ Mistake 5: Not using strict mode
// tsconfig.json:
{
  "compilerOptions": {
    "strict": true,              // enables ALL strict checks
    "noUncheckedIndexedAccess": true,  // array access returns T | undefined
    "forceConsistentCasingInImports": true
  }
}
```

---

## Quick Reference: tsconfig.json for React + TS

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"]
}
```
