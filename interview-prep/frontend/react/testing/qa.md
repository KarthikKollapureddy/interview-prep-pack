# React Testing — Interview Q&A

> 12 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What tools do you use for React testing?

| Tool | Purpose |
|------|---------|
| **Jest** | Test runner, assertions, mocking |
| **React Testing Library (RTL)** | Render components, query DOM, simulate events |
| **MSW (Mock Service Worker)** | Mock API calls at network level |
| **Cypress / Playwright** | E2E browser testing |

**Testing philosophy (RTL):** Test the way users interact with your app, not implementation details. Query by text, role, label — not by class names or component internals.

---

### Q2. RTL queries — which to use?

| Query | Use when |
|-------|----------|
| `getByRole('button', { name: 'Submit' })` | Interactive elements (preferred) |
| `getByText('Hello')` | Static text |
| `getByLabelText('Email')` | Form inputs |
| `getByPlaceholderText('Search...')` | Inputs (if no label) |
| `getByTestId('custom-element')` | Last resort |
| `queryByText('...')` | Assert something is NOT present |
| `findByText('...')` | Async — wait for element to appear |

```jsx
// ✅ GOOD: tests user-visible behavior
screen.getByRole('button', { name: /submit/i });
screen.getByLabelText(/email/i);

// ❌ BAD: tests implementation details
container.querySelector('.btn-primary');
wrapper.find('SubmitButton').props().onClick();
```

---

### Q3. How do you test async operations in React?

```jsx
test('loads and displays shipment data', async () => {
  render(<ShipmentTracker trackingNumber="123456" />);
  
  // Assert loading state
  expect(screen.getByText(/loading/i)).toBeInTheDocument();
  
  // Wait for data to load
  const trackingNumber = await screen.findByText('123456');
  expect(trackingNumber).toBeInTheDocument();
  
  // Assert loading is gone
  expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
});
```

**`waitFor` for assertions:**
```jsx
await waitFor(() => {
  expect(screen.getByText('Delivered')).toBeInTheDocument();
}, { timeout: 3000 });
```

---

## Scenario-Based Questions

### Q4. At FedEx, write tests for a ShipmentSearch component.

```jsx
// Component renders a search input and displays results from API
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  rest.get('/api/v1/shipments', (req, res, ctx) => {
    const query = req.url.searchParams.get('q');
    if (query === '123') {
      return res(ctx.json([{ trackingNumber: '123456789012', status: 'IN_TRANSIT' }]));
    }
    return res(ctx.json([]));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

test('searches and displays results', async () => {
  const user = userEvent.setup();
  render(<ShipmentSearch />);
  
  await user.type(screen.getByRole('searchbox'), '123');
  await user.click(screen.getByRole('button', { name: /search/i }));
  
  const result = await screen.findByText('123456789012');
  expect(result).toBeInTheDocument();
  expect(screen.getByText('IN_TRANSIT')).toBeInTheDocument();
});

test('shows empty state for no results', async () => {
  const user = userEvent.setup();
  render(<ShipmentSearch />);
  
  await user.type(screen.getByRole('searchbox'), 'nonexistent');
  await user.click(screen.getByRole('button', { name: /search/i }));
  
  expect(await screen.findByText(/no shipments found/i)).toBeInTheDocument();
});

test('handles API error gracefully', async () => {
  server.use(rest.get('/api/v1/shipments', (req, res, ctx) => res(ctx.status(500))));
  
  const user = userEvent.setup();
  render(<ShipmentSearch />);
  
  await user.type(screen.getByRole('searchbox'), '123');
  await user.click(screen.getByRole('button', { name: /search/i }));
  
  expect(await screen.findByText(/error/i)).toBeInTheDocument();
});
```

---

### Q5. At Hatio, how do you test a component that uses Context?

```jsx
// Create a test wrapper with providers
function renderWithProviders(ui, { initialUser = null, ...options } = {}) {
  function Wrapper({ children }) {
    return (
      <AuthProvider initialUser={initialUser}>
        <CartProvider>
          <QueryClientProvider client={new QueryClient()}>
            {children}
          </QueryClientProvider>
        </CartProvider>
      </AuthProvider>
    );
  }
  return render(ui, { wrapper: Wrapper, ...options });
}

test('shows user name when logged in', () => {
  renderWithProviders(<Navbar />, {
    initialUser: { name: 'Karthik', role: 'admin' },
  });
  expect(screen.getByText('Karthik')).toBeInTheDocument();
});

test('shows login button when not logged in', () => {
  renderWithProviders(<Navbar />);
  expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
});
```

---

## Coding Challenges

### Challenge 1: Test Suite for a Form
**File:** `solutions/FormTests.jsx`  
Write complete tests for a registration form:
1. Renders all fields (name, email, password, confirm password)
2. Shows validation errors for empty fields
3. Shows error when passwords don't match
4. Disables submit button during submission
5. Calls onSubmit with correct data on success
6. Shows server error on API failure

### Challenge 2: Test Custom Hook
**File:** `solutions/HookTests.jsx`  
Test a custom `usePagination` hook:
1. Returns correct page data
2. next/prev change the page
3. Can't go below page 1 or above max
4. Reset works
5. Total pages calculation is correct

---

## Gotchas & Edge Cases

### Q6. `userEvent` vs `fireEvent` — which to use?

```jsx
// fireEvent — dispatches DOM events directly (lower level)
fireEvent.click(button);
fireEvent.change(input, { target: { value: 'hello' } });

// userEvent — simulates real user interactions (preferred)
const user = userEvent.setup();
await user.click(button);
await user.type(input, 'hello'); // fires keydown, keypress, keyup, input, change
```

**Always prefer `userEvent`** — it simulates the complete event chain like a real user, catching bugs that `fireEvent` misses (e.g., disabled button clicks).

---

### Q7. Testing components that use `useEffect` with API calls?

```jsx
// ❌ Don't mock useEffect or internal state
jest.mock('react', () => ({ ...jest.requireActual('react'), useEffect: jest.fn() }));

// ✅ Mock at the API layer (MSW or jest.mock the fetch function)
// Then assert on what the user sees, not on internal state
```
