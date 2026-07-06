# Angular Concepts Q&A — SneakerStore Project

## 1. Standalone Components

**Q: What is a standalone component and how is it used in this project?**

A: A standalone component does not belong to an NgModule. It declares its own dependencies directly in the `imports` array of the `@Component` decorator. Every component in this project (Home, Login, Cart, Checkout, PaymentStatus, Orders, Admin, Navbar) is standalone. For example:

```ts
@Component({
  selector: 'app-login',
  imports: [FormsModule],  // <-- declares its own dependencies
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login { ... }
```

There is no `app.module.ts` file. Instead, the app is bootstrapped in `main.ts` using `bootstrapApplication(App, appConfig)`.

**Q: How is a standalone app bootstrapped differently from a module-based app?**

A: Instead of `platformBrowserDynamic().bootstrapModule(AppModule)`, this project uses:

```ts
// main.ts
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
```

The `appConfig` is an `ApplicationConfig` object that provides router and HTTP client configurations.

---

## 2. @Component Decorator

**Q: What properties does the @Component decorator accept in this project?**

A: All components use four properties:
- `selector` — the custom HTML tag name (e.g., `'app-home'`)
- `imports` — array of standalone dependencies the template needs
- `templateUrl` — path to the external HTML file
- `styleUrl` — path to the external CSS file (singular, not `styleUrls` array — this is the newer standalone API)

**Q: Why is there no `styles` or `encapsulation` property?**

A: They are optional. When not specified, Angular uses `ViewEncapsulation.Emulated` by default, which scopes styles to the component. No component overrides this, and none uses inline styles.

---

## 3. @Injectable & Dependency Injection

**Q: What does `providedIn: 'root'` mean?**

A: It means the service is a singleton available application-wide without being listed in any providers array. Angular creates one instance and injects it wherever requested. All six services in this project use it:

```ts
@Injectable({
  providedIn: 'root',
})
export class ApiService { ... }
```

**Q: What is the `inject()` function and why is it used instead of constructor injection?**

A: `inject()` is a function that returns the current instance of a dependency. It is used instead of constructor-based DI throughout the project:

```ts
// Instead of:
constructor(private auth: AuthService) { }

// The project uses:
private auth = inject(AuthService);
```

This approach avoids boilerplate constructor code and works in any injection context (services, guards, pipes, etc.).

**Q: How does the dependency graph look in this project?**

A: `ApiService` depends on `HttpClient`. All domain services (Auth, Product, Cart, Order, Payment) depend on `ApiService`. Components depend on the domain services they need. No service depends on a component.

---

## 4. Lifecycle Hooks

**Q: Which lifecycle hook is used and why?**

A: Only `OnInit` is used, in six components. It fires once after the component's data-bound properties are initialized and the component is ready. Components use it for:

```ts
@Component({ ... })
export class Home implements OnInit {
  ngOnInit(): void {
    this.loadProducts();   // fetch data from API on startup
  }
}
```

**Q: Why aren't other hooks like `OnDestroy` used?**

A: None of the components need cleanup. There are no manual subscriptions (no `Subject`, no `setInterval`, no DOM event listeners) — all subscriptions use the `.subscribe()` pattern, which would ideally be unsubscribed, but since the components live for the entire route lifecycle and the HTTP observables complete on their own, it's acceptable.

---

## 5. Routing

**Q: How are routes defined in this project?**

A: Routes are defined in `app.routes.ts` as an array of route objects:

```ts
export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'home', component: Home },
  { path: 'cart', component: Cart },
  { path: 'checkout', component: Checkout },
  { path: 'payment-status', component: PaymentStatus },
  { path: 'orders', component: Orders },
  { path: 'admin', component: Admin },
  { path: '**', redirectTo: '/home' },
];
```

**Q: What is the wildcard route `'**'` for?**

A: It catches any URL that doesn't match a defined route and redirects to `/home`. This prevents users from seeing a blank page or crash if they mistype a URL.

**Q: How does the router outlet work?**

A: In `app.html`, `<router-outlet />` acts as a placeholder. Angular dynamically inserts the component that matches the current URL into this spot. The `<app-navbar />` stays outside the outlet so it appears on every page.

**Q: How is navigation state passed between pages?**

A: Using the second argument of `router.navigate()`:

```ts
// Sending page (cart):
this.router.navigate(['/checkout'], {
  state: { orderIds: [1, 2, 3], totalAmount: 299.99 },
});

// Receiving page (checkout):
ngOnInit() {
  const state = history.state as { orderIds: number[]; totalAmount: number };
  // state.orderIds, state.totalAmount
}
```

This passes data through the browser's history state without exposing it in the URL.

---

## 6. Template-Driven Forms

**Q: How is form data collected in this project?**

A: Using Angular's template-driven forms with `FormsModule` and the `[(ngModel)]` two-way binding directive:

```html
<input type="email" [(ngModel)]="email" name="email" placeholder="you@example.com" />
```

The `name` attribute is required for `ngModel` to register the field. The component property (`email`) is automatically updated when the user types.

**Q: How is form submission handled?**

A: With `(ngSubmit)` on the form element:

```html
<form (ngSubmit)="submit()" #authForm="ngForm">
```

The template reference variable `#authForm="ngForm"` gives access to the form's state, though it isn't used in this project beyond declaration.

**Q: Why is `ReactiveFormsModule` not used?**

A: Template-driven forms are simpler for basic login, checkout, and admin forms. They require less code and are easier to read. Reactive forms would be preferred for complex forms with dynamic validation rules.

---

## 7. Control Flow (@if / @for)

**Q: What is the new @-syntax for control flow?**

A: Angular 17+ introduced built-in control flow as an alternative to `*ngIf` and `*ngFor`. This project uses only the new syntax:

```html
@if (products.length === 0) {
  <p>No products found</p>
} @else {
  @for (product of products; track product.id) {
    <div>{{ product.name }}</div>
  }
}
```

**Q: What does the `track` expression do in `@for`?**

A: It tells Angular how to identify each item uniquely. This enables efficient DOM updates — when the list changes, Angular reuses existing DOM elements instead of destroying and recreating them. The project tracks by unique IDs like `product.id`, `order.orderId`, `item.productId`.

**Q: What is NOT used from the old approach?**

A: No `*ngIf`, `*ngFor`, `*ngSwitch`, or `NgIf`/`NgForOf`/`NgSwitch` imports from `CommonModule` are used anywhere.

---

## 8. Property & Event Binding

**Q: What property bindings are used in the project?**

A: Several types:
- `[src]="product.imageUrl"` — binds to an HTML attribute
- `[disabled]="processing"` — conditionally disables a button
- `[class.active]="activeTab === 'products'"` — adds/removes a CSS class
- `[class]="'status-' + order.status.toLowerCase()"` — dynamically sets the full class attribute
- `[style.background]="'#d1fae5'"` — inline style binding

**Q: What event bindings are used?**

A: Three types of events:
- `(click)="addToCart(product.id)"` — button clicks
- `(ngSubmit)="submit()"` — form submission
- `(change)="updateStatus(o.orderId, $any($event.target).value)"` — select dropdown change

**Q: What is `$any()` in the template?**

A: A type-cast function that tells Angular's type checker to treat the expression as `any` type. It is used because `$event.target` is typed as `EventTarget | null`, which doesn't have a `value` property. `$any()` bypasses the type check.

---

## 9. HttpClient & Services

**Q: How is HttpClient configured?**

A: In `app.config.ts`, the provider is added at the application level:

```ts
providers: [provideRouter(routes), provideHttpClient()]
```

This makes HttpClient available everywhere without importing `HttpClientModule`.

**Q: How does the ApiService wrap HttpClient?**

A: It injects HttpClient and provides typed helper methods that automatically attach the JWT token:

```ts
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080';

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, {
      headers: this.getHeaders(),
    });
  }
  // post, put, delete similarly...
}
```

**Q: How is the JWT token attached to every request?**

A: The `getHeaders()` method reads the token from `localStorage` and creates an `Authorization: Bearer <token>` header. Every request (`get`, `post`, `put`, `delete`) uses these headers.

**Q: Why isn't an HttpInterceptor used for the token?**

A: An interceptor would be the standard approach, but this project keeps it simple by wrapping the logic in `ApiService`. An interceptor would be better for a larger app to avoid passing headers manually in every method call.

---

## 10. Observable & Subscribe Pattern

**Q: How are API responses consumed in components?**

A: Using the observer object pattern with `.subscribe()`:

```ts
this.productService.getAll().subscribe({
  next: (data) => {
    this.products = data;
  },
  error: () => {
    this.message = 'Failed to load products';
  },
});
```

`next` handles success, `error` handles failure. No `complete` callback is used.

**Q: Why are there no RxJS operators used?**

A: The operations are simple enough that operators aren't needed. Each API call returns the data directly or shows an error message. There's no need for data transformation (`map`), error recovery (`catchError`), or combining streams (`forkJoin`, `combineLatest`).

**Q: Are there any unsubscribed observables?**

A: Yes — `.subscribe()` returns a `Subscription`, but none of the components store or unsubscribe from it. This is acceptable because HTTP observables complete after emitting one value (or error), so the subscription cleans itself up. Long-lived observables (like from `interval()` or WebSocket) would leak memory without explicit unsubscription.

---

## 11. Template Interpolation

**Q: What kinds of expressions are used in `{{ }}` interpolation?**

A: Several patterns:
- Simple property: `{{ product.name }}`
- Arithmetic: `${{ product.price }}`
- Ternary: `{{ isLoginMode ? 'Sign In' : 'Create Account' }}`
- Method call: `{{ getProductName(item.productId) }}`
- TypeScript method: `{{ order.status.toLowerCase() }}`

**Q: What is NOT allowed in interpolations?**

A: In Angular templates, you cannot use:
- `new` keyword
- Assignment operators (`=`, `+=`)
- Increment/decrement (`++`, `--`)
- Bitwise operators (`&`, `|`, `~`)
- `void`, `typeof`, `instanceof`
- Statement-like constructs (`if`, `for`, `while`)

---

## 12. Interfaces & Type Safety

**Q: How are data models defined?**

A: As exported TypeScript interfaces in the service files:

```ts
export interface Product {
  id: number;
  name: string;
  price: number;
  brand: string;
  category: string;
  description: string;
  imageUrl: string;
}
```

**Q: Why are interfaces used over classes for models?**

A: Interfaces are lightweight — they exist only at compile time and have no runtime overhead. They provide type checking without adding bundled JavaScript. Classes would be needed if the models needed behavior (methods).

**Q: Why is `any` used in some places?**

A: Some API responses are typed loosely as `any` when the exact shape is dynamic or when the response type doesn't match perfectly. For example, the `/cart/clear` endpoint returns a plain string, which is typed as `Observable<string>`.

---

## 13. JWT Handling

**Q: How is the JWT decoded on the frontend?**

A: The JWT payload is base64-encoded. It is decoded manually:

```ts
isAdmin(): boolean {
  const token = this.getToken();
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.role === 'ADMIN';
  } catch {
    return false;
  }
}
```

`token.split('.')[1]` extracts the payload part. `atob()` decodes base64. `JSON.parse()` converts it to an object.

**Q: What happens if the token is malformed?**

A: The `try/catch` block catches any errors from parsing and returns `false`, treating the user as non-admin. This prevents a crash from corrupt tokens.

---

## 14. localStorage Usage

**Q: How is localStorage used for authentication?**

A: Two items are stored:
- `token` — the JWT string, set after login and removed on logout
- `userId` — currently stored but not actively used (legacy)

**Q: What are the security implications?**

A: `localStorage` is accessible to any JavaScript running on the same origin. An XSS vulnerability could steal the token. HttpOnly cookies would be more secure but require server-side changes. This tradeoff is acceptable for a demonstration project.

---

## 15. @Component Imports Array

**Q: What goes into the `imports` array of a standalone component?**

A: Any standalone Angular directive, pipe, or component that the template uses. Examples from the project:

```ts
// Login needs FormsModule for [(ngModel)]
@Component({ imports: [FormsModule], ... })

// Navbar needs RouterLink for navigation links
@Component({ imports: [RouterLink], ... })

// Home has no special directives — empty imports
@Component({ imports: [], ... })
```

**Q: Why does the Home component have an empty `imports` array?**

A: Because its template uses only built-in Angular syntax (`@if`, `@for`, interpolation, event/property bindings) that doesn't require explicit imports. The new control flow syntax (`@if`/`@for`) is built into Angular's compiler, not a directive.

---

## 16. Styles & View Encapsulation

**Q: How are component styles scoped?**

A: By default, Angular uses `ViewEncapsulation.Emulated`, which adds unique attribute selectors to CSS rules. This means styles defined in `login.css` only apply to the Login component, even if other components have the same class names.

**Q: Where are global styles defined?**

A: In `src/styles.css`. This file contains reset styles and base font settings that apply to the entire application:

```css
body {
  font-family: 'Inter', sans-serif;
  background: #f8f9fa;
  margin: 0;
}
```

---

## 17. Application Config (app.config.ts)

**Q: What is the purpose of `app.config.ts`?**

A: It replaces the `providers` array of `AppModule` in module-based apps. It exports an `ApplicationConfig` object that configures providers at the application root:

```ts
export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient()],
};
```

**Q: How are providers added?**

A: Using provider factory functions like `provideRouter()` and `provideHttpClient()` instead of listing classes directly. This is the recommended pattern in modern Angular.
