# Angular Components & Architecture — Interview Q&A

> 14 scenario-based questions (FedEx / NPCI / Hatio level)  
> Grouped: Conceptual → Scenario → Code Challenge → Gotchas

---

## Conceptual Questions

### Q1. Angular architecture — Modules, Components, Services, Directives.

```
Angular App
├── AppModule (root)
│   ├── Components (UI building blocks)
│   │   ├── ShipmentListComponent
│   │   └── ShipmentDetailComponent
│   ├── Services (business logic, HTTP, state)
│   │   └── ShipmentService
│   ├── Directives (DOM manipulation)
│   │   └── HighlightDirective
│   ├── Pipes (data transformation in templates)
│   │   └── DateFormatPipe
│   └── Guards (route protection)
│       └── AuthGuard
├── SharedModule (reusable across features)
│   ├── CommonComponents
│   └── CommonPipes
└── Feature Modules (lazy-loaded)
    ├── ShipmentModule
    └── AnalyticsModule
```

**Key decorators:**
- `@Component` — UI with template, styles
- `@Injectable` — services (singleton by default with `providedIn: 'root'`)
- `@NgModule` — grouping declarations, imports, providers
- `@Directive` — custom DOM behavior
- `@Pipe` — data transformation

---

### Q2. Component lifecycle hooks — what are they?

```typescript
export class ShipmentComponent implements OnInit, OnChanges, OnDestroy {
  
  ngOnChanges(changes: SimpleChanges) {
    // Input property changed — runs BEFORE ngOnInit and on every change
  }

  ngOnInit() {
    // Component initialized — fetch data here (not in constructor!)
    this.loadShipments();
  }

  ngDoCheck() { /* Custom change detection */ }
  ngAfterContentInit() { /* After ng-content projected */ }
  ngAfterContentChecked() { /* After content checked */ }
  ngAfterViewInit() { /* After view + child views initialized */ }
  ngAfterViewChecked() { /* After view checked */ }

  ngOnDestroy() {
    // Cleanup: unsubscribe observables, clear timers
    this.subscription.unsubscribe();
  }
}
```

**Most used:** `ngOnInit` (setup), `ngOnChanges` (react to input changes), `ngOnDestroy` (cleanup).

---

### Q3. Component communication — parent/child, sibling.

```typescript
// Parent → Child: @Input()
@Component({ selector: 'shipment-card', template: '...' })
export class ShipmentCard {
  @Input() shipment!: Shipment;
}

// Child → Parent: @Output() + EventEmitter
@Component({ selector: 'shipment-card', template: '...' })
export class ShipmentCard {
  @Output() selected = new EventEmitter<string>();
  onSelect() { this.selected.emit(this.shipment.id); }
}

// Usage:
// <shipment-card [shipment]="s" (selected)="onShipmentSelected($event)"></shipment-card>

// Sibling communication: via shared service
@Injectable({ providedIn: 'root' })
export class ShipmentSelectionService {
  private selectedSubject = new BehaviorSubject<string | null>(null);
  selected$ = this.selectedSubject.asObservable();
  select(id: string) { this.selectedSubject.next(id); }
}
```

---

### Q4. Change detection strategies — Default vs OnPush.

```typescript
// Default: checks ENTIRE component tree on any event (slower)
@Component({ changeDetection: ChangeDetectionStrategy.Default })

// OnPush: only checks when @Input reference changes or event fires in this component
@Component({ changeDetection: ChangeDetectionStrategy.OnPush })
export class ShipmentList {
  @Input() shipments!: Shipment[]; // Must use immutable data!
}
```

**OnPush triggers re-render when:**
1. `@Input()` reference changes (not mutation!)
2. DOM event fires within this component
3. Observable bound with `async` pipe emits
4. `ChangeDetectorRef.markForCheck()` called manually

**Performance tip:** Use `OnPush` for all presentational components.

---

## Scenario-Based Questions

### Q5. At FedEx, design a component architecture for the shipment tracking dashboard.

```
AppComponent
├── HeaderComponent (logo, user menu)
├── SidebarComponent (navigation)
└── RouterOutlet
    └── DashboardPage
        ├── FilterPanelComponent (status, date range, service type)
        ├── ShipmentTableComponent [OnPush]
        │   └── ShipmentRowComponent [OnPush] (one per shipment)
        ├── PaginationComponent
        └── ShipmentDetailPanel (sliding panel)
            ├── TrackingTimelineComponent
            └── ShipmentActionsComponent (cancel, reroute)
```

```typescript
// Smart component (container) — handles data fetching
@Component({ template: `
  <filter-panel (filterChange)="onFilter($event)"></filter-panel>
  <shipment-table [shipments]="shipments$ | async" (select)="onSelect($event)">
  </shipment-table>
  <shipment-detail *ngIf="selectedId" [shipmentId]="selectedId"></shipment-detail>
`})
export class DashboardPage {
  shipments$ = this.service.getShipments();
  selectedId: string | null = null;

  constructor(private service: ShipmentService) {}
  onFilter(f: Filter) { this.shipments$ = this.service.getShipments(f); }
  onSelect(id: string) { this.selectedId = id; }
}

// Dumb component (presentational) — pure @Input/@Output
@Component({
  selector: 'shipment-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '...'
})
export class ShipmentTable {
  @Input() shipments!: Shipment[];
  @Output() select = new EventEmitter<string>();
}
```

---

### Q6. At Hatio, you need reusable form components across multiple pages. How?

```typescript
// Reusable form field with ControlValueAccessor
@Component({
  selector: 'currency-input',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: CurrencyInputComponent,
    multi: true,
  }],
  template: `<input [value]="displayValue" (input)="onInput($event)" (blur)="onTouched()">`
})
export class CurrencyInputComponent implements ControlValueAccessor {
  displayValue = '';
  onChange: (value: number) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: number) { this.displayValue = formatCurrency(value); }
  registerOnChange(fn: any) { this.onChange = fn; }
  registerOnTouched(fn: any) { this.onTouched = fn; }

  onInput(event: Event) {
    const raw = (event.target as HTMLInputElement).value;
    this.onChange(parseCurrency(raw));
  }
}

// Usage: <currency-input formControlName="amount"></currency-input>
```

---

## Coding Challenges

### Challenge 1: Reusable Data Table Component
**File:** `solutions/DataTableComponent.ts`  
Build a reusable Angular data table:
1. Generic column definition (header, field, sortable, template)
2. Sorting by clicking headers
3. Client-side pagination
4. Row selection with checkbox
5. @Input/@Output interface

### Challenge 2: Smart/Dumb Component Pattern
**File:** `solutions/SmartDumbPattern.ts`  
Implement a product listing feature:
1. ProductListPage (smart) — fetches data, handles actions
2. ProductCard (dumb, OnPush) — displays single product
3. SearchFilter (dumb) — emits filter changes
4. Cart integration via service

---

## Gotchas & Edge Cases

### Q7. Why does `ngOnChanges` not fire for object mutations?

```typescript
// Parent:
this.shipment.status = 'DELIVERED'; // ❌ Mutation — ngOnChanges NOT called

this.shipment = { ...this.shipment, status: 'DELIVERED' }; // ✅ New reference — ngOnChanges fires
```

Angular checks **reference equality** (===). Mutating an object keeps the same reference. With `OnPush`, the child won't re-render at all.

---

### Q8. Standalone components (Angular 14+) — the future?

```typescript
@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, ShipmentCardComponent],
  template: '...'
})
export class DashboardComponent { }
// No NgModule needed!
```

Standalone components reduce boilerplate and enable better tree-shaking. Angular is moving toward a module-less architecture.
