# RxJS & Observables — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. What is an Observable? How does it differ from a Promise?

| Aspect | Promise | Observable |
|--------|---------|-----------|
| Values | Single value | Multiple values over time |
| Eager/Lazy | Eager (executes immediately) | Lazy (executes on subscribe) |
| Cancellable | No (once started, can't cancel) | Yes (unsubscribe cancels) |
| Operators | `.then()`, `.catch()` | `pipe()` with 100+ operators |
| Multicast | N/A | `share()`, `shareReplay()` |

```typescript
// Promise: single HTTP call
const promise = fetch('/api/shipments').then(r => r.json());

// Observable: HTTP call + auto-retry + transform
const shipments$ = this.http.get<Shipment[]>('/api/shipments').pipe(
  retry(3),
  map(data => data.filter(s => s.status === 'IN_TRANSIT')),
  catchError(err => of([])) // fallback to empty array
);
```

---

### Q2. Subject, BehaviorSubject, ReplaySubject — what's the difference?

```typescript
// Subject — no initial value, late subscribers miss past emissions
const subject = new Subject<string>();
subject.next('A');
subject.subscribe(v => console.log(v)); // misses 'A'
subject.next('B'); // logs: B

// BehaviorSubject — has initial value, late subscribers get LAST value
const behavior = new BehaviorSubject<string>('initial');
behavior.next('A');
behavior.subscribe(v => console.log(v)); // logs: A (latest)
behavior.next('B'); // logs: B

// ReplaySubject — replays N past values to late subscribers
const replay = new ReplaySubject<string>(2); // buffer size 2
replay.next('A');
replay.next('B');
replay.next('C');
replay.subscribe(v => console.log(v)); // logs: B, C (last 2)
```

**Use cases:**
- `Subject` — event bus, one-time notifications
- `BehaviorSubject` — current state (selected filter, logged-in user)
- `ReplaySubject` — cache recent values (last N messages)

---

### Q3. Key RxJS operators — explain the most important ones.

```typescript
// map — transform each value
source$.pipe(map(x => x * 2));

// filter — only pass values meeting condition
source$.pipe(filter(x => x > 10));

// switchMap — cancel previous inner observable, subscribe to new
searchInput$.pipe(
  switchMap(query => this.http.get(`/api/search?q=${query}`))
  // If user types fast, only the LAST request completes
);

// mergeMap — run all inner observables concurrently
clicks$.pipe(mergeMap(() => this.http.post('/api/log')));

// concatMap — queue inner observables, execute one at a time
saveActions$.pipe(concatMap(action => this.http.put('/api/save', action)));

// exhaustMap — ignore new emissions while inner is active
submitButton$.pipe(exhaustMap(() => this.http.post('/api/submit')));
// Prevents double-submit!

// combineLatest — emit when ANY source emits (combines latest from all)
combineLatest([filters$, sort$, page$]).pipe(
  switchMap(([filters, sort, page]) => this.http.get('/api/data', { params: { ...filters, sort, page } }))
);

// debounceTime — wait for pause in emissions
searchInput$.pipe(debounceTime(300)); // Wait 300ms after last keystroke

// distinctUntilChanged — skip duplicate consecutive values
searchInput$.pipe(distinctUntilChanged()); // Don't re-search same query

// takeUntil — auto-unsubscribe when notifier emits
source$.pipe(takeUntil(this.destroy$)); // Cleanup pattern
```

---

### Q4. `switchMap` vs `mergeMap` vs `concatMap` vs `exhaustMap`?

```
Input:  --A----B----C-->

switchMap(x => http(x)):
  A → starts request, B → cancels A, starts B, C → cancels B, starts C
  Result: only C's response
  USE FOR: Search typeahead, route changes

mergeMap(x => http(x)):
  A → starts, B → starts, C → starts (all concurrent)
  Result: A, B, C responses (in any order)
  USE FOR: Independent actions (logging, analytics)

concatMap(x => http(x)):
  A → starts, waits for A to complete, then B starts, then C
  Result: A, B, C responses (in order)
  USE FOR: Sequential operations (file uploads, ordered saves)

exhaustMap(x => http(x)):
  A → starts, B → IGNORED (A still active), A completes, C → starts
  Result: A and C responses
  USE FOR: Prevent double-submit
```

---

## Scenario-Based Questions

### Q5. At FedEx, build a real-time shipment search with debounce.

```typescript
@Component({ template: `
  <input [formControl]="searchControl" placeholder="Track shipment...">
  <div *ngFor="let s of results$ | async">{{ s.trackingNumber }} - {{ s.status }}</div>
`})
export class ShipmentSearch implements OnInit, OnDestroy {
  searchControl = new FormControl('');
  results$!: Observable<Shipment[]>;
  private destroy$ = new Subject<void>();

  constructor(private service: ShipmentService) {}

  ngOnInit() {
    this.results$ = this.searchControl.valueChanges.pipe(
      debounceTime(300),              // Wait 300ms after typing stops
      distinctUntilChanged(),          // Skip if same query
      filter(q => q.length >= 3),      // Min 3 characters
      switchMap(query =>               // Cancel previous request
        this.service.search(query).pipe(
          catchError(() => of([]))     // Fallback on error
        )
      ),
      takeUntil(this.destroy$)         // Cleanup
    );
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

### Q6. At NPCI, you need to poll for transaction status every 5 seconds until it's settled.

```typescript
pollTransactionStatus(txnId: string): Observable<Transaction> {
  return timer(0, 5000).pipe(  // Emit immediately, then every 5s
    switchMap(() => this.http.get<Transaction>(`/api/transactions/${txnId}`)),
    takeWhile(txn => txn.status === 'PENDING', true), // Stop when settled (inclusive)
    shareReplay(1) // Cache last value for late subscribers
  );
}
```

---

### Q7. At Hatio, multiple components need to react to a shared filter state.

```typescript
@Injectable({ providedIn: 'root' })
export class FilterService {
  private filters$ = new BehaviorSubject<Filters>({ status: 'all', dateRange: null });

  getFilters(): Observable<Filters> {
    return this.filters$.asObservable();
  }

  updateFilter(partial: Partial<Filters>) {
    this.filters$.next({ ...this.filters$.value, ...partial });
  }
}

// Component A: updates filters
this.filterService.updateFilter({ status: 'completed' });

// Component B: reacts to filter changes
this.filterService.getFilters().pipe(
  switchMap(f => this.transactionService.query(f))
).subscribe(transactions => this.transactions = transactions);
```

---

## Coding Challenges

### Challenge 1: Autocomplete with RxJS
**File:** `solutions/AutocompleteRxJS.ts`  
Build a search autocomplete:
1. debounce(300ms) + distinctUntilChanged
2. switchMap to cancel stale requests
3. Loading indicator
4. Error handling with retry(2)
5. Cache recent searches (shareReplay)

### Challenge 2: Real-time Dashboard with Observables
**File:** `solutions/RealtimeDashboard.ts`  
Build a dashboard that combines multiple data streams:
1. combineLatest for filters + sort + page
2. Polling with timer + switchMap
3. WebSocket events merged with HTTP data
4. Proper unsubscription with takeUntil

---

## Gotchas & Edge Cases

### Q8. Memory leaks — how do unsubscribed observables cause them?

```typescript
// ❌ LEAK: subscription never cleaned up
ngOnInit() {
  this.service.getData().subscribe(data => this.data = data);
  // If component is destroyed, subscription keeps running!
}

// ✅ FIX 1: async pipe (auto-unsubscribes)
// <div>{{ data$ | async }}</div>

// ✅ FIX 2: takeUntil pattern
private destroy$ = new Subject<void>();
ngOnInit() {
  this.service.getData().pipe(takeUntil(this.destroy$)).subscribe(/*...*/);
}
ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

// ✅ FIX 3: DestroyRef (Angular 16+)
destroyRef = inject(DestroyRef);
ngOnInit() {
  this.service.getData()
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(/*...*/);
}
```

---

### Q9. Hot vs Cold observables?

```typescript
// COLD: each subscriber gets its own execution (HTTP calls)
const cold$ = this.http.get('/api/data'); // Each subscribe = new HTTP request

// HOT: subscribers share the same execution (subjects, events)
const hot$ = fromEvent(button, 'click'); // All subscribers get same clicks

// Make cold → hot with share/shareReplay
const shared$ = this.http.get('/api/data').pipe(shareReplay(1));
// First subscriber triggers HTTP, subsequent subscribers get cached response
```
