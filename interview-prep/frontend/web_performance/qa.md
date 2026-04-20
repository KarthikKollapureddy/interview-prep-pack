# Web Performance & Modern Frontend — Interview Q&A

> Concepts from [yangshun/front-end-interview-handbook](https://github.com/yangshun/front-end-interview-handbook) (43.9K ⭐), [lydiahallie/javascript-questions](https://github.com/lydiahallie/javascript-questions) (65.3K ⭐)
> Covers: Core Web Vitals, SSR/SSG, Accessibility, Web Security, Performance Optimization
> **Priority: P1** — Every frontend interview covers performance and modern web standards

---

## Q1. Core Web Vitals — What metrics does Google care about?

```
Core Web Vitals = Google's UX metrics (affect SEO ranking)

Three Key Metrics:
  ┌──────────────────┬───────────────────┬──────────┬──────────┐
  │ Metric           │ What It Measures  │ Good     │ Poor     │
  ├──────────────────┼───────────────────┼──────────┼──────────┤
  │ LCP              │ Largest Contentful│ < 2.5s   │ > 4.0s   │
  │ (Loading)        │ Paint — main      │          │          │
  │                  │ content visible   │          │          │
  │                  │                   │          │          │
  │ INP              │ Interaction to    │ < 200ms  │ > 500ms  │
  │ (Interactivity)  │ Next Paint —      │          │          │
  │                  │ responsiveness    │          │          │
  │                  │                   │          │          │
  │ CLS              │ Cumulative Layout │ < 0.1    │ > 0.25   │
  │ (Visual Stability)│ Shift — things   │          │          │
  │                  │ moving around     │          │          │
  └──────────────────┴───────────────────┴──────────┴──────────┘

How to Improve LCP:
  ✓ Optimize largest image (compress, webp, lazy load below fold)
  ✓ Preload critical resources: <link rel="preload" as="image">
  ✓ Remove render-blocking CSS/JS
  ✓ Use CDN for static assets
  ✓ Server-side rendering for initial paint

How to Improve INP:
  ✓ Break long tasks (> 50ms) into smaller chunks
  ✓ Use requestIdleCallback for non-critical work
  ✓ Debounce/throttle event handlers
  ✓ Use Web Workers for heavy computation
  ✓ Avoid layout thrashing

How to Improve CLS:
  ✓ Set width/height on images and videos
  ✓ Reserve space for ads/embeds
  ✓ Use CSS aspect-ratio
  ✓ Avoid inserting content above existing content
  ✓ Use transform animations (not top/left)

Measuring:
  Lighthouse (Chrome DevTools)
  PageSpeed Insights (Google)
  web-vitals npm library
  Chrome UX Report (CrUX) — real user data
```

---

## Q2. SSR vs SSG vs CSR vs ISR — Rendering Strategies.

```
CSR (Client-Side Rendering) — Traditional SPA:
  Browser loads empty HTML → downloads JS → renders content
  ✓ Rich interactivity, fast page transitions
  ✗ Slow initial load, poor SEO, blank page flash
  Use: Dashboards, internal tools, highly interactive apps

SSR (Server-Side Rendering) — Next.js getServerSideProps:
  Server renders HTML per request → sends complete page
  ✓ Fast first paint, great SEO, dynamic content
  ✗ Server load per request, TTFB slower
  Use: E-commerce product pages, news articles

SSG (Static Site Generation) — Next.js getStaticProps:
  Build-time: generate HTML for all pages → serve from CDN
  ✓ Fastest possible load (pre-built), great SEO
  ✗ Build time grows with pages, stale data
  Use: Blogs, documentation, marketing pages

ISR (Incremental Static Regeneration) — Next.js revalidate:
  Serve stale page, regenerate in background after interval
  ✓ Best of SSG + SSR: fast + fresh
  ✗ Slightly stale data during revalidation window
  Use: Product catalogs, content that updates periodically

  ┌─────────┬───────────┬──────┬──────────┬───────────┐
  │ Strategy│ First Load│ SEO  │ Server   │ Freshness │
  ├─────────┼───────────┼──────┼──────────┼───────────┤
  │ CSR     │ Slow      │ Poor │ None     │ Real-time │
  │ SSR     │ Fast      │ Great│ Per-req  │ Real-time │
  │ SSG     │ Fastest   │ Great│ Build    │ Stale     │
  │ ISR     │ Fast      │ Great│ Periodic │ Near-real │
  └─────────┴───────────┴──────┴──────────┴───────────┘

React Server Components (RSC) — Next.js 13+ App Router:
  - Components run on server by default
  - Zero client JS for server components
  - Stream HTML progressively
  - 'use client' directive for interactive components
```

---

## Q3. JavaScript Performance — Event Loop, Web Workers, Memory.

```
Event Loop Deep Dive (from lydiahallie):
  ┌──────────────┐
  │  Call Stack   │ ← executes sync code
  └──────┬───────┘
         │ (when empty)
  ┌──────▼───────┐
  │ Microtask    │ ← Promise.then, queueMicrotask, MutationObserver
  │ Queue        │   (ALL processed before next macrotask)
  └──────┬───────┘
         │ (when empty)
  ┌──────▼───────┐
  │ Macrotask    │ ← setTimeout, setInterval, I/O, requestAnimationFrame
  │ Queue        │   (ONE processed, then check microtasks again)
  └──────────────┘

  Order: Sync → Microtasks (all) → 1 Macrotask → Microtasks → ...

  console.log('1');                    // sync
  setTimeout(() => console.log('2'));  // macrotask
  Promise.resolve().then(() =>
      console.log('3'));               // microtask
  console.log('4');                    // sync
  // Output: 1, 4, 3, 2

Web Workers:
  // main.js — offload heavy computation
  const worker = new Worker('worker.js');
  worker.postMessage({ data: largeArray });
  worker.onmessage = (e) => console.log(e.data);

  // worker.js — runs in separate thread
  self.onmessage = (e) => {
      const result = heavyComputation(e.data);
      self.postMessage(result);
  };

Memory Leak Patterns:
  1. Forgotten timers: setInterval without clearInterval
  2. Detached DOM nodes: reference to removed elements
  3. Closures holding large objects
  4. Event listeners not cleaned up
  5. Global variables accumulating data

  // Fix: use WeakRef, WeakMap, cleanup in useEffect return
```

---

## Q4. Web Accessibility (a11y) — Essential Knowledge.

```
WCAG 2.1 (Web Content Accessibility Guidelines):
  Four Principles: POUR
  Perceivable:    content available to all senses
  Operable:       UI navigable by keyboard, sufficient time
  Understandable: content readable, predictable behavior
  Robust:         works with assistive technologies

Essential Practices:
  1. Semantic HTML:
     ✗ <div onclick="...">Click me</div>
     ✓ <button onClick="...">Click me</button>

  2. Images: always provide alt text
     <img src="chart.png" alt="Sales grew 40% in Q3 2024" />
     Decorative images: alt="" (empty, not missing)

  3. ARIA (when semantic HTML isn't enough):
     <div role="dialog" aria-labelledby="title" aria-modal="true">
       <h2 id="title">Confirm Delete</h2>
     </div>

  4. Keyboard Navigation:
     All interactive elements focusable (Tab/Shift+Tab)
     Focus trap in modals
     Skip navigation link: <a href="#main">Skip to content</a>

  5. Color Contrast:
     Normal text: 4.5:1 ratio minimum
     Large text: 3:1 ratio minimum

  6. Form Labels:
     <label htmlFor="email">Email</label>
     <input id="email" type="email" aria-required="true" />

ARIA Roles:
  role="alert"      → live region for important messages
  role="navigation"  → nav landmark
  role="dialog"      → modal dialog
  aria-live="polite" → announces content changes to screen readers

Testing Tools:
  axe DevTools, Lighthouse accessibility audit,
  NVDA/VoiceOver screen readers, keyboard-only testing
```

---

## Q5. Frontend Security — XSS, CSRF, CSP.

```
XSS (Cross-Site Scripting):
  Attacker injects malicious script into your page

  Types:
    Stored XSS:    script saved in DB, served to all users
    Reflected XSS: script in URL, reflected in response
    DOM-based XSS: script manipulates DOM directly

  Prevention:
    ✓ React auto-escapes JSX: {userInput} is safe
    ✗ dangerouslySetInnerHTML — NEVER use with user input
    ✓ DOMPurify for sanitizing HTML
    ✓ Content-Security-Policy header
    ✓ HttpOnly cookies (JS can't access)

CSRF (Cross-Site Request Forgery):
  Attacker tricks user's browser into making unwanted requests

  Prevention:
    ✓ CSRF tokens (synchronizer token pattern)
    ✓ SameSite cookie attribute (Lax or Strict)
    ✓ Check Origin/Referer headers
    ✓ Use custom headers (X-Requested-With) for API calls

Content Security Policy (CSP):
  // HTTP header restricting what resources can load
  Content-Security-Policy:
    default-src 'self';
    script-src 'self' 'nonce-abc123';
    style-src 'self' 'unsafe-inline';
    img-src 'self' cdn.example.com;
    connect-src 'self' api.example.com;

  → Blocks inline scripts, external scripts from unknown domains
  → Prevents most XSS attacks

Other Security Headers:
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY (prevents clickjacking)
  Strict-Transport-Security: max-age=31536000
  Referrer-Policy: strict-origin-when-cross-origin
```

---

## Q6. JavaScript Tricky Concepts — Advanced Patterns.

```
From lydiahallie/javascript-questions (155 questions, 65K ⭐):

1. Generators & Iterators:
   function* idGenerator() {
       let id = 0;
       while (true) yield id++;
   }
   const gen = idGenerator();
   gen.next().value; // 0
   gen.next().value; // 1
   // Lazy evaluation — values computed on demand

2. Proxy & Reflect:
   const handler = {
       get: (target, prop) => {
           console.log(`Accessing ${prop}`);
           return Reflect.get(target, prop);
       },
       set: (target, prop, value) => {
           if (prop === 'age' && typeof value !== 'number')
               throw new TypeError('Age must be a number');
           return Reflect.set(target, prop, value);
       }
   };
   const user = new Proxy({}, handler);
   // Used by: Vue 3 reactivity, validation libraries

3. WeakRef & FinalizationRegistry (ES2021):
   // Weak reference — doesn't prevent garbage collection
   let obj = { data: 'heavy' };
   const weakRef = new WeakRef(obj);
   obj = null; // eligible for GC
   weakRef.deref(); // obj or undefined if GC'd
   // Use case: caching without memory leaks

4. Structured Clone (Modern deep copy):
   const copy = structuredClone(original);
   // Handles: Date, RegExp, Map, Set, ArrayBuffer, circular refs
   // Better than: JSON.parse(JSON.stringify(x)) — handles more types

5. Private Class Fields (#):
   class BankAccount {
       #balance = 0;
       deposit(amount) { this.#balance += amount; }
       get balance() { return this.#balance; }
   }
   const account = new BankAccount();
   account.#balance; // SyntaxError!

6. Optional Chaining & Nullish Coalescing:
   const street = user?.address?.street;  // undefined if any null
   const name = input ?? 'default';       // only null/undefined
   // ?? vs ||: '' ?? 'default' → '', '' || 'default' → 'default'
```

---

## Q7. Frontend System Design — How to approach.

```
Frontend System Design is becoming common at mid-senior interviews

Framework for Answering:
  1. Requirements Clarification (2 min)
     - Users: how many concurrent?
     - Features: core vs nice-to-have
     - Platforms: mobile, desktop, offline?

  2. Architecture (5 min)
     ┌───────────────────────────────────────┐
     │           Component Tree              │
     │  App → Layout → Header + Content      │
     │  Content → Feed → FeedItem → Actions  │
     └───────────────────────────────────────┘

  3. Data Model & API Design (5 min)
     - What state is needed?
     - Client state vs server state?
     - API endpoints and shapes

  4. Deep Dive (10 min)
     - Rendering strategy (CSR/SSR/SSG)
     - State management approach
     - Performance optimizations
     - Real-time features (WebSocket/SSE)
     - Error handling & loading states
     - Accessibility

Common Frontend System Design Questions:
  ┌──────────────────────┬──────────────────────────────────┐
  │ System               │ Key Considerations               │
  ├──────────────────────┼──────────────────────────────────┤
  │ News Feed (Twitter)  │ Infinite scroll, virtualization, │
  │                      │ optimistic updates, real-time    │
  │                      │                                  │
  │ Chat Application     │ WebSocket, message queue, typing │
  │                      │ indicators, offline support      │
  │                      │                                  │
  │ E-commerce Product   │ Image carousel, SSR for SEO,     │
  │ Page                 │ cart state, A/B testing          │
  │                      │                                  │
  │ Autocomplete Search  │ Debouncing, caching, keyboard    │
  │                      │ navigation, highlighted matches  │
  │                      │                                  │
  │ Spreadsheet (Excel)  │ Virtual scrolling, cell formula  │
  │                      │ computation, undo/redo stack     │
  └──────────────────────┴──────────────────────────────────┘
```

---

## Q8. Modern CSS & Layout — Interview Essentials.

```
Flexbox vs Grid:
  Flexbox: 1D layout (row OR column)
    .container { display: flex; justify-content: space-between; }

  Grid: 2D layout (rows AND columns)
    .container {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 16px;
    }

  Rule of thumb:
    Navigation bar → Flexbox
    Page layout → Grid
    Card layout → Grid
    Inline elements → Flexbox

CSS Container Queries (modern responsive design):
  @container (min-width: 400px) {
      .card { grid-template-columns: 1fr 2fr; }
  }
  // Component responds to its container, not viewport

CSS Specificity (interview favorite):
  Inline styles:  1000
  #id:            100
  .class:         10
  element:        1
  !important:     overrides all (avoid!)

  Example: div.card#main = 1 + 10 + 100 = 111

CSS-in-JS vs Utility CSS:
  CSS Modules:  .module.css, scoped by default
  Tailwind CSS: utility-first, className="flex items-center gap-4"
  Styled-Components: CSS-in-JS, template literals
  CSS Modules are most common in Next.js projects

Modern CSS Features:
  - :has() selector — "parent selector"
  - Container queries — responsive to parent
  - CSS Nesting — native (no Sass needed)
  - color-mix() — dynamic color mixing
  - View Transitions API — native page transitions
```

---

## Q9. Testing Frontend Applications.

```
Testing Pyramid for Frontend:
  ┌─────────────┐
  │   E2E Tests │ ← Cypress, Playwright (few, slow, high confidence)
  │  (10-20%)   │
  ├─────────────┤
  │ Integration │ ← RTL + MSW (test components together)
  │  (30-40%)   │
  ├─────────────┤
  │ Unit Tests  │ ← Jest/Vitest (many, fast, isolated)
  │  (40-60%)   │
  └─────────────┘

Testing Library Philosophy:
  "The more your tests resemble the way your software is used,
   the more confidence they can give you."

  ✗ Test implementation details (state, methods)
  ✓ Test behavior (what user sees and does)

  // Good: test what user sees
  const { getByRole, getByText } = render(<LoginForm />);
  await userEvent.type(getByRole('textbox', {name: /email/i}), 'a@b.com');
  await userEvent.click(getByRole('button', {name: /submit/i}));
  expect(getByText('Welcome')).toBeInTheDocument();

  // Bad: test internals
  expect(component.state.isLoggedIn).toBe(true);  // ✗

Mock Service Worker (MSW) for API mocking:
  import { http, HttpResponse } from 'msw';
  const handlers = [
      http.get('/api/users', () =>
          HttpResponse.json([{ id: 1, name: 'Karthik' }])),
  ];

Snapshot Testing:
  ✓ Detect unexpected UI changes
  ✗ Brittle — update snapshots constantly
  Better: use visual regression testing (Chromatic, Percy)
```

---

## Q10. Build Tools & Module Bundlers.

```
Bundler Comparison:
  ┌──────────┬─────────────┬──────────────┬───────────────────┐
  │ Tool     │ Speed       │ Config       │ Best For          │
  ├──────────┼─────────────┼──────────────┼───────────────────┤
  │ Webpack  │ Slow        │ Complex      │ Legacy, complex   │
  │ Vite     │ Very Fast   │ Minimal      │ Modern dev (HMR)  │
  │ esbuild  │ Fastest     │ Programmatic │ Library bundling  │
  │ Turbopack│ Fast        │ Next.js      │ Next.js projects  │
  │ Rollup   │ Fast        │ Moderate     │ Libraries         │
  └──────────┴─────────────┴──────────────┴───────────────────┘

Why Vite is Fast:
  1. Dev: uses native ES modules (no bundling!)
  2. Pre-bundles dependencies with esbuild (100x faster than JS)
  3. HMR: only updates changed module (not full rebuild)
  4. Build: uses Rollup for production optimization

Key Concepts:
  Tree Shaking: remove unused exports (dead code elimination)
    import { map } from 'lodash-es';  // only imports map
    import _ from 'lodash';           // imports everything ✗

  Code Splitting: split into chunks, load on demand
    const AdminPanel = lazy(() => import('./AdminPanel'));

  Module Federation: share modules between micro-frontends
    Webpack 5 feature for micro-frontend architecture
```

---

*Sources: [yangshun/front-end-interview-handbook](https://github.com/yangshun/front-end-interview-handbook), [lydiahallie/javascript-questions](https://github.com/lydiahallie/javascript-questions), [web.dev](https://web.dev/), MDN*
