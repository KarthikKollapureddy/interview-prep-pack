# JavaScript Core — Interview Q&A

> 15 questions covering closures, event loop, promises, prototypes, `this`, and ES6+ features  
> Priority: **P0** — Mandatory for fullstack developer interviews (FedEx, NPCI, Walmart)

---

## Conceptual Questions

### Q1. Explain Closures in JavaScript. Give a practical example.

**Answer:**
A closure is a function that **remembers variables from its outer scope** even after the outer function has returned.

```javascript
function createCounter() {
    let count = 0;               // Outer variable
    return {
        increment: () => ++count,  // Inner function "closes over" count
        getCount: () => count
    };
}

const counter = createCounter();
counter.increment();  // 1
counter.increment();  // 2
counter.getCount();   // 2
// count is not accessible directly — it's private!
```

**Why closures matter:**
- **Data privacy** — emulate private variables (module pattern)
- **Callbacks** — event handlers, setTimeout, API calls all use closures
- **Currying & partial application**

**Classic interview trap — closure in a loop:**
```javascript
// BUG: prints 3, 3, 3
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);
}

// FIX 1: Use let (block scope)
for (let i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);  // 0, 1, 2
}

// FIX 2: IIFE
for (var i = 0; i < 3; i++) {
    ((j) => setTimeout(() => console.log(j), 100))(i);
}
```

---

### Q2. Explain the JavaScript Event Loop. How does async code execute?

**Answer:**
JavaScript is **single-threaded** but handles async operations via the Event Loop.

```
┌──────────────────────────┐
│       Call Stack          │  ← Executes sync code (LIFO)
│  (one thing at a time)   │
└────────────┬─────────────┘
             │ When stack is empty, dequeue from:
             ▼
┌──────────────────────────┐
│  Microtask Queue          │  ← Promises (.then), queueMicrotask
│  (higher priority)        │     Process ALL microtasks first
└────────────┬─────────────┘
             │ Then:
             ▼
┌──────────────────────────┐
│  Macrotask Queue          │  ← setTimeout, setInterval, I/O
│  (lower priority)         │     Process ONE macrotask, then check microtasks
└──────────────────────────┘
```

**Execution order:**
1. Execute all sync code on the call stack
2. When stack is empty → drain the **entire microtask queue**
3. Execute **one macrotask** from the macrotask queue
4. Go back to step 2

**Classic interview question — What's the output?**
```javascript
console.log('1');                          // Sync
setTimeout(() => console.log('2'), 0);     // Macrotask
Promise.resolve().then(() => console.log('3')); // Microtask
console.log('4');                          // Sync

// Output: 1, 4, 3, 2
```

**Explanation:**
- `1` and `4` execute immediately (sync, on call stack)
- Promise `.then` goes to **microtask** queue
- `setTimeout` goes to **macrotask** queue
- Stack empty → microtasks first (`3`) → then macrotask (`2`)

---

### Q3. What is Hoisting? How does it differ for `var`, `let`, `const`, and functions?

**Answer:**
Hoisting is JavaScript's behavior of **moving declarations to the top of their scope** during compilation (not assignments).

| Declaration | Hoisted? | Initialized? | Temporal Dead Zone? |
|-------------|---------|-------------|-------------------|
| `var` | Yes | Yes (`undefined`) | No |
| `let` | Yes | No | Yes |
| `const` | Yes | No | Yes |
| `function declaration` | Yes | Yes (entire function) | No |
| `function expression` | Like `var`/`let`/`const` | No | Depends |

```javascript
// var — hoisted and initialized as undefined
console.log(a);  // undefined (not error!)
var a = 5;

// let/const — hoisted but NOT initialized (TDZ)
console.log(b);  // ReferenceError: Cannot access 'b' before initialization
let b = 5;

// function declaration — fully hoisted
greet();         // "Hello!" — works!
function greet() { console.log("Hello!"); }

// function expression — NOT fully hoisted
sayHi();         // TypeError: sayHi is not a function
var sayHi = function() { console.log("Hi!"); };
```

---

### Q4. Explain `this` keyword in different contexts.

**Answer:**
`this` refers to different objects depending on **how a function is called**:

| Context | `this` refers to |
|---------|-----------------|
| Global scope (non-strict) | `window` (browser) / `global` (Node) |
| Global scope (strict) | `undefined` |
| Object method | The object |
| `new` constructor | The newly created object |
| Arrow function | Lexically inherited from parent scope |
| `call()` / `apply()` / `bind()` | Explicitly set |
| Event handler (DOM) | The element that fired the event |

```javascript
const obj = {
    name: "Karthik",
    greet() { console.log(this.name); },         // "Karthik" — obj
    greetArrow: () => console.log(this.name)      // undefined — lexical (window)
};

obj.greet();       // "Karthik"
obj.greetArrow();  // undefined (arrow captures outer `this`)

const fn = obj.greet;
fn();              // undefined (lost context — `this` is window/global)
fn.call(obj);      // "Karthik" (explicitly set)
```

**Arrow functions** don't have their own `this` — they inherit from the enclosing lexical scope. This is why they're perfect for callbacks but bad for object methods.

---

### Q5. Promises, async/await — Explain the evolution of async handling in JS.

**Answer:**

**1. Callbacks (old way):**
```javascript
getData(function(result) {
    getMore(result, function(result2) {
        getFinal(result2, function(result3) {
            // Callback hell / pyramid of doom
        });
    });
});
```

**2. Promises (ES6):**
```javascript
getData()
    .then(result => getMore(result))
    .then(result2 => getFinal(result2))
    .then(result3 => console.log(result3))
    .catch(err => console.error(err))
    .finally(() => cleanup());
```

**3. async/await (ES2017) — syntactic sugar over Promises:**
```javascript
async function fetchData() {
    try {
        const result = await getData();
        const result2 = await getMore(result);
        const result3 = await getFinal(result2);
        return result3;
    } catch (err) {
        console.error(err);
    }
}
```

**Promise states:**
```
Pending → Fulfilled (resolved) ──→ .then()
       → Rejected             ──→ .catch()
```

**Parallel execution:**
```javascript
// Sequential (slow) — each await waits for previous
const a = await fetchA();
const b = await fetchB();

// Parallel (fast) — both start immediately
const [a, b] = await Promise.all([fetchA(), fetchB()]);

// Promise.allSettled — doesn't short-circuit on rejection
const results = await Promise.allSettled([fetchA(), fetchB()]);
```

---

### Q6. Explain Prototypal Inheritance in JavaScript.

**Answer:**
JavaScript uses **prototypes** instead of classical inheritance. Every object has a hidden `[[Prototype]]` (accessible via `__proto__` or `Object.getPrototypeOf()`).

```javascript
// Prototype chain:
const animal = { eat() { return "eating"; } };
const dog = Object.create(animal);
dog.bark = function() { return "woof"; };

dog.bark();   // "woof" — found on dog
dog.eat();    // "eating" — found on dog.__proto__ (animal)
dog.toString(); // found on Object.prototype

// Chain: dog → animal → Object.prototype → null
```

**ES6 Classes (syntactic sugar over prototypes):**
```javascript
class Animal {
    constructor(name) { this.name = name; }
    eat() { return `${this.name} is eating`; }
}

class Dog extends Animal {
    bark() { return "Woof!"; }
}

const d = new Dog("Rex");
d instanceof Dog;    // true
d instanceof Animal; // true
```

---

### Q7. What are the differences between `==` and `===`?

**Answer:**

| Operator | Name | Comparison | Type Coercion? |
|----------|------|-----------|---------------|
| `==` | Abstract equality | Value only | Yes |
| `===` | Strict equality | Value + Type | No |

```javascript
0 == ""       // true  (both coerce to 0)
0 === ""      // false (number vs string)
null == undefined  // true  (special rule)
null === undefined // false
NaN == NaN    // false (NaN is not equal to anything)
NaN === NaN   // false
```

**Best practice:** Always use `===` to avoid unexpected coercion bugs.

---

## Scenario / Interview Deep-Dive Questions

### Q8. Debounce vs Throttle — Implement both.

**Answer:**

**Debounce** — waits until user STOPS doing something for X ms (search input):
```javascript
function debounce(fn, delay) {
    let timer;
    return function(...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

// Usage: only fires 300ms after user stops typing
input.addEventListener('input', debounce(handleSearch, 300));
```

**Throttle** — fires at most once every X ms (scroll, resize):
```javascript
function throttle(fn, limit) {
    let inThrottle = false;
    return function(...args) {
        if (!inThrottle) {
            fn.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// Usage: fires at most once every 200ms during scroll
window.addEventListener('scroll', throttle(handleScroll, 200));
```

---

### Q9. What is the difference between `null`, `undefined`, and undeclared?

**Answer:**

| Term | Meaning | typeof |
|------|---------|--------|
| `undefined` | Variable declared but no value assigned | `"undefined"` |
| `null` | Intentionally empty / no value | `"object"` (historical bug) |
| Undeclared | Variable never declared with var/let/const | `ReferenceError` |

```javascript
let a;
console.log(a);       // undefined
console.log(null);    // null
console.log(b);       // ReferenceError: b is not defined

// Check for null/undefined:
if (value == null) { }    // catches both null and undefined
if (value === null) { }   // only null
if (value === undefined) {} // only undefined
```

---

### Q10. Explain `var` vs `let` vs `const`.

**Answer:**

| Feature | `var` | `let` | `const` |
|---------|-------|-------|---------|
| Scope | Function | Block | Block |
| Hoisting | Yes (initialized to undefined) | Yes (TDZ) | Yes (TDZ) |
| Re-declaration | Allowed | Not allowed | Not allowed |
| Re-assignment | Allowed | Allowed | Not allowed |
| Global property | Yes (window.x) | No | No |

```javascript
if (true) {
    var a = 1;    // function-scoped — leaks out
    let b = 2;    // block-scoped — stays inside
    const c = 3;  // block-scoped — stays inside
}
console.log(a); // 1
console.log(b); // ReferenceError
console.log(c); // ReferenceError
```

**const gotcha — objects are mutable:**
```javascript
const obj = { name: "Karthik" };
obj.name = "Updated";  // Allowed! const prevents reassignment, not mutation
obj = {};              // TypeError: Assignment to constant variable
```

---

### Q11. What are Higher-Order Functions? Give examples.

**Answer:**
A higher-order function is a function that **takes a function as argument** or **returns a function**.

```javascript
// Array HOFs (most asked):
const nums = [1, 2, 3, 4, 5];

nums.map(n => n * 2);           // [2, 4, 6, 8, 10] — transform
nums.filter(n => n > 3);        // [4, 5] — filter
nums.reduce((sum, n) => sum + n, 0); // 15 — accumulate
nums.find(n => n > 3);          // 4 — first match
nums.some(n => n > 3);          // true — any match?
nums.every(n => n > 0);         // true — all match?
nums.forEach(n => console.log(n)); // side effects

// Custom HOF:
function withLogging(fn) {
    return function(...args) {
        console.log(`Calling ${fn.name} with`, args);
        const result = fn(...args);
        console.log(`Result:`, result);
        return result;
    };
}
```

---

### Q12. Explain Spread operator, Rest parameters, and Destructuring.

**Answer:**

```javascript
// SPREAD (...) — expands iterable
const arr1 = [1, 2, 3];
const arr2 = [...arr1, 4, 5];        // [1, 2, 3, 4, 5]
const obj1 = { a: 1 };
const obj2 = { ...obj1, b: 2 };      // { a: 1, b: 2 }

// REST (...) — collects remaining into array
function sum(...nums) {
    return nums.reduce((a, b) => a + b, 0);
}
sum(1, 2, 3); // 6

// DESTRUCTURING — unpack values
const { name, age = 25 } = user;             // Object destructuring with default
const [first, , third] = [10, 20, 30];       // Array destructuring (skip 2nd)
const { address: { city } } = user;          // Nested destructuring

// Function parameter destructuring:
function greet({ name, role = "developer" }) {
    return `Hi ${name}, ${role}`;
}
```

---

### Q13. What is the Module system in JavaScript? CommonJS vs ES Modules.

**Answer:**

| Feature | CommonJS (CJS) | ES Modules (ESM) |
|---------|---------------|------------------|
| Syntax | `require()` / `module.exports` | `import` / `export` |
| Loading | Synchronous | Async (static analysis) |
| Where | Node.js (default) | Browsers, modern Node |
| Tree-shaking | No | Yes (dead code elimination) |

```javascript
// CommonJS
const express = require('express');
module.exports = { myFunction };

// ES Modules
import express from 'express';
export const myFunction = () => {};
export default class App {}
```

---

### Q14. What is Event Delegation? Why is it useful?

**Answer:**
Event delegation attaches a **single event listener to a parent** instead of individual listeners on each child. It uses **event bubbling**.

```javascript
// BAD: 1000 listeners for 1000 items
document.querySelectorAll('.item').forEach(item => {
    item.addEventListener('click', handleClick);
});

// GOOD: 1 listener using delegation
document.getElementById('list').addEventListener('click', (e) => {
    if (e.target.matches('.item')) {
        handleClick(e);
    }
});
```

**Benefits:**
- **Performance** — fewer event listeners in memory
- **Dynamic elements** — works for elements added later via JS
- **Cleaner code** — single handler with conditional logic

---

### Q15. What are WeakMap and WeakSet? When would you use them?

**Answer:**
`WeakMap` and `WeakSet` hold **weak references** to objects — allowing garbage collection when no other reference exists.

| Feature | Map | WeakMap |
|---------|-----|---------|
| Key types | Any | Objects only |
| Enumerable | Yes (`forEach`, `keys`) | No |
| GC-friendly | No (holds strong ref) | Yes (weak ref) |

```javascript
// Use case: private data for DOM elements
const metadata = new WeakMap();

function track(element) {
    metadata.set(element, { clicks: 0 });
}

// When element is removed from DOM and dereferenced,
// the WeakMap entry is automatically garbage collected
```

---

## Quick Reference

### JS Interview Cheat Sheet

| Topic | Key Point |
|-------|-----------|
| Closures | Function remembers outer scope variables |
| Event Loop | Microtasks (Promises) before Macrotasks (setTimeout) |
| Hoisting | var = undefined, let/const = TDZ, function = fully hoisted |
| `this` | Depends on HOW function is called, not WHERE defined |
| Arrow functions | No own `this`, `arguments`, or `prototype` |
| `==` vs `===` | Always use `===` to avoid type coercion |
| var vs let vs const | var = function scope, let/const = block scope |
| Promises | Pending → Fulfilled/Rejected. Use `Promise.all` for parallel |
| async/await | Syntactic sugar over Promises. Always use try/catch |
| Prototypes | JS inheritance chain: obj → proto → Object.prototype → null |
| Debounce vs Throttle | Debounce = wait for pause, Throttle = limit frequency |
| Event delegation | Single listener on parent, use event bubbling |

---

### Q16. ES Modules vs CommonJS — what's the difference?

**Answer:**

```js
// CommonJS (Node.js default — synchronous, runtime resolution):
const fs = require('fs');              // import
module.exports = { myFunction };       // export
module.exports = myFunction;           // default export

// ES Modules (ESM — standard, static, async):
import fs from 'fs';                   // import
import { readFile } from 'fs';        // named import
export const myFunction = () => {};    // named export
export default myFunction;             // default export
```

| Feature | CommonJS (`require`) | ES Modules (`import`) |
|---------|---------------------|----------------------|
| Loading | Synchronous | Asynchronous |
| Resolution | Runtime (dynamic) | Compile-time (static) |
| Tree-shaking | ❌ Not possible | ✅ Dead code elimination |
| Top-level await | ❌ No | ✅ Yes |
| `this` at top | `module.exports` | `undefined` |
| Browser support | ❌ (needs bundler) | ✅ Native |

**Dynamic imports (code splitting):**
```js
// Load module on demand — returns a Promise
const module = await import('./heavyModule.js');
module.doSomething();

// React lazy loading:
const LazyComponent = React.lazy(() => import('./HeavyComponent'));
```

---

### Q17. WeakMap and WeakSet — what are they and when to use them?

**Answer:**

```js
// WeakMap: keys MUST be objects, and they're held WEAKLY
// If no other reference to the key exists → garbage collected automatically
const cache = new WeakMap();

let user = { name: 'Karthik' };
cache.set(user, { visits: 10 });

user = null;  // The { name: 'Karthik' } object AND its cache entry are GC'd!
// No memory leak — unlike regular Map which would hold the reference forever

// WeakSet: same concept — objects held weakly
const processed = new WeakSet();
function process(obj) {
    if (processed.has(obj)) return; // already processed
    processed.add(obj);
    // ... do work
}
```

| Feature | Map/Set | WeakMap/WeakSet |
|---------|---------|-----------------|
| Key types | Any | Objects only |
| GC behavior | Prevents GC of keys | Allows GC (weak reference) |
| Iterable | ✅ `forEach`, `for...of` | ❌ Not iterable |
| `.size` | ✅ | ❌ Not available |
| Use case | General storage | Caches, metadata, DOM tracking |

**Real-world uses:**
- **DOM metadata:** Track extra data on DOM nodes without memory leaks
- **Private data:** Store private fields for class instances
- **Caching:** Cache computed values that auto-clean when source object is GC'd
