# Frontend System Document — SneakerStore

## 1. Overview

SneakerStore is a single-page application (SPA) built with Angular 19+ using the standalone component architecture. It serves as the frontend for a Java Spring Boot microservices backend, providing an e-commerce interface for browsing sneakers, managing a shopping cart, placing orders, processing payments, and administering the product catalog.

**Tech Stack**

| Layer | Technology |
|---|---|
| Framework | Angular 21 (standalone components) |
| Language | TypeScript |
| Styling | Plain CSS with custom properties |
| HTTP | @angular/common/http (HttpClient) |
| Routing | @angular/router |
| Forms | @angular/forms (template-driven) |
| Build | Angular CLI (Vite/esbuild under the hood) |
| Package Manager | npm |

**Backend Interface**
- API Gateway URL: `http://localhost:8080`
- All HTTP requests pass through the Spring Cloud Gateway, which handles JWT validation, role-based access control, and request routing to the appropriate microservice.

---

## 2. Project Structure

```
frontend/
├── public/                          # Static assets (favicon)
├── src/
│   ├── index.html                   # SPA entry HTML
│   ├── main.ts                      # App bootstrap
│   ├── styles.css                   # Global styles
│   └── app/
│       ├── app.config.ts            # Application-level providers
│       ├── app.routes.ts            # Route definitions
│       ├── app.ts / app.html / app.css   # Root component
│       ├── services/                # Data access layer
│       │   ├── api.service.ts       # Base HTTP client (JWT injection)
│       │   ├── auth.ts              # Authentication & user service
│       │   ├── product.ts           # Product catalog service
│       │   ├── cart.ts              # Shopping cart service
│       │   ├── order.ts             # Order service
│       │   └── payment.ts           # Payment service
│       ├── navbar/                  # Top navigation bar
│       ├── login/                   # Login & registration
│       ├── home/                    # Product listing (landing page)
│       ├── cart/                    # Shopping cart
│       ├── checkout/                # Payment form
│       ├── payment-status/          # Payment result
│       ├── orders/                  # Order history
│       └── admin/                   # Admin dashboard
└── package.json
```

---

## 3. Component Tree & Responsibilities

```
App (root)
├── Navbar
│   - Brand logo link → /home
│   - Navigation links: Products, Cart, Orders, Admin*, Login/Logout
│   - Auth-aware visibility (conditional rendering based on token/role)
│
├── Router Outlet (switches based on route)
│   │
│   ├── Login (route: /login)
│   │   - Toggle between Sign In / Register
│   │   - Form validation (HTML5 required)
│   │   - On success: store JWT → navigate to /home
│   │
│   ├── Home (route: /home, default redirect: /)
│   │   - Hero banner section
│   │   - Product grid (fetched from GET /products)
│   │   - "Add to Cart" button per product
│   │   - Redirects to /login if user is unauthenticated
│   │
│   ├── Cart (route: /cart)
│   │   - Lists cart items with product name, quantity, price
│   │   - Remove item button per row
│   │   - "Clear Cart" button
│   │   - Order summary sidebar (subtotal, shipping, total)
│   │   - "Proceed to Checkout" → creates orders → navigates to /checkout
│   │   - Empty state with link back to /home
│   │
│   ├── Checkout (route: /checkout)
│   │   - Guarded: redirects to /cart if no order data in navigation state
│   │   - Left column: order summary (products, quantities, totals)
│   │   - Right column: payment form
│   │     - Card number, cardholder name, expiry, CVV
│   │     - Payment method selector (credit/debit/PayPal)
│   │   - "Pay $X" button → processes all payments → navigates to /payment-status
│   │
│   ├── PaymentStatus (route: /payment-status)
│   │   - Reads result from navigation state
│   │   - Success state: green checkmark, transaction ID, amount, "View Orders" button
│   │   - Failure state: red X, amount, error hint, "Back to Cart" button
│   │
│   ├── Orders (route: /orders)
│   │   - Order history table: ID, product, quantity, total, status
│   │   - Color-coded status badges (CREATED=amber, CONFIRMED=blue, etc.)
│   │   - Empty state with "Start Shopping" link
│   │
│   └── Admin (route: /admin)
│       - Guarded: redirects to /home if user role !== ADMIN
│       - Three tabs: Products, Orders, Users
│       - Products tab: table + add product form + delete button per row
│       - Orders tab: all users' orders + status update dropdown
│       - Users tab: all users with role badges
```

**Component Dependencies**

| Component | Imports from Angular | Service Dependencies |
|---|---|---|
| Navbar | RouterLink | AuthService |
| Login | FormsModule | AuthService, Router |
| Home | — | ProductService, CartService, AuthService, Router |
| Cart | RouterLink | CartService, OrderService, ProductService, Router |
| Checkout | FormsModule | PaymentService, OrderService, Router |
| PaymentStatus | RouterLink | — |
| Orders | RouterLink | OrderService |
| Admin | FormsModule | AuthService, ProductService, OrderService, Router |

---

## 4. Routing Architecture

**Route Table** (`app.routes.ts`)

| Path | Component | Auth Required | Notes |
|---|---|---|---|
| `/` | — | No | Redirects to /home |
| `/home` | Home | No | Public landing page |
| `/login` | Login | No | Login & registration |
| `/cart` | Cart | Yes (token) | Shows error if unauthenticated |
| `/checkout` | Checkout | Yes (state) | Redirects to /cart if no order state |
| `/payment-status` | PaymentStatus | No (state) | Reads browser history state |
| `/orders` | Orders | Yes (token) | Shows error if unauthenticated |
| `/admin` | Admin | Yes (ADMIN role) | Redirects to /home if not admin |
| `**` | — | No | Catch-all, redirects to /home |

**Navigation Flow (User Journey)**

```
/home (browse) ──[add to cart]──→ /login (if unauthenticated)
    │                                       │
    │                                  [login success]
    │                                       │
    └──────[add to cart]───────────────────/
         │
    /cart ──[proceed to checkout]──→ /checkout
         │                               │
         │                          [pay now]
         │                               │
         └─────────────────── /payment-status
                                     │
                              [view orders]
                                     │
                               /orders ──→ logout → /login
```

**State Passing Mechanism**

Data flows between pages using Angular's router navigation state (`history.state`), not URL parameters:

| Transition | Data Passed |
|---|---|
| Cart → Checkout | `{ orderIds: number[], totalAmount: number }` |
| Checkout → PaymentStatus | `{ success: boolean, transactionId: string, totalAmount: number, orderIds: number[] }` |

This approach keeps sensitive data (order IDs) out of the URL and prevents users from navigating directly to intermediate pages.

---

## 5. Data Flow & Service Layer

**Service Layer Architecture**

```
┌─────────────┐     ┌───────────────────┐     ┌──────────────┐
│  Components  │────▶│  Domain Services  │────▶│  ApiService  │────▶ HTTP API
│  (UI logic)  │     │  (business logic) │     │  (HTTP wrap) │
└─────────────┘     └───────────────────┘     └──────────────┘
                           │                        │
                     AuthService              HttpClient
                     ProductService           localStorage
                     CartService              JWT injection
                     OrderService
                     PaymentService
```

**ApiService (Base HTTP Layer)**

Responsible for:
- Centralizing the API base URL (`http://localhost:8080`)
- Automatically attaching the JWT Bearer token from localStorage to every request
- Providing typed generic methods: `get<T>()`, `post<T>()`, `put<T>()`, `delete<T>()`
- Setting `Content-Type: application/json` on all requests

```ts
class ApiService {
  get<T>(path): Observable<T>
  post<T>(path, body): Observable<T>
  put<T>(path, body): Observable<T>
  delete<T>(path): Observable<T>
}
```

**Domain Services**

Each service wraps ApiService with domain-specific methods and TypeScript interfaces:

| Service | Key Methods | Response Types |
|---|---|---|
| AuthService | `login()`, `register()`, `getAllUsers()`, `isAdmin()`, `isLoggedIn()`, `logout()` | LoginResponse, User |
| ProductService | `getAll()`, `getById()`, `getByCategory()`, `createProduct()`, `deleteProduct()` | Product |
| CartService | `getCart()`, `addItem()`, `removeItem()`, `clearCart()` | CartResponse |
| OrderService | `createOrder()`, `getMyOrders()`, `getAllOrders()`, `updateOrderStatus()` | OrderResponse |
| PaymentService | `processPayment()`, `getPaymentByOrder()` | PaymentResponse |

**Data Interfaces (TypeScript)**

```ts
interface Product {
  id: number; name: string; price: number; brand: string;
  category: string; description: string; imageUrl: string;
}

interface CartResponse {
  userId: number; totalPrice: number;
  items: Array<{ productId: number; quantity: number; price: number }>;
}

interface OrderResponse {
  orderId: number; userId: number; userName: string;
  productId: number; productName: string; productPrice: number;
  quantity: number; totalPrice: number; status: string;
}

interface PaymentResponse {
  id: number; orderId: number; userId: number;
  amount: number; status: string; transactionId: string; createdAt: string;
}
```

---

## 6. Authentication & Authorization

**Token Lifecycle**

```
Login Form ──POST /users/login──→ Backend validates ──→ Returns JWT
     │                                                     │
     │                                            Store in localStorage
     │                                                     │
     │                                   ┌─────────────────┘
     │                                   ▼
     │                          Every API call:
     │                          ApiService reads token from localStorage
     │                          Attaches Authorization: Bearer <token>
     │                          Gateway validates JWT, injects X-User-Id & X-User-Role
     │                                   │
     └─────────────────── Logout: remove token from localStorage ──→ redirect to /login
```

**Frontend Authorization Checks**

| Check | Method | Where Used |
|---|---|---|
| Is user logged in? | `AuthService.isLoggedIn()` → checks for token in localStorage | Navbar (show/hide Cart, Orders, Logout) |
| Is user admin? | `AuthService.isAdmin()` → decodes JWT payload → checks `role === 'ADMIN'` | Navbar (show Admin link), Admin component (redirect guard) |
| Add to cart? | `Home.addToCart()` → checks `isLoggedIn()` → redirects to `/login` if false | Home component |

**JWT Payload Decoding**

```ts
isAdmin(): boolean {
  const token = localStorage.getItem('token');
  if (!token) return false;
  const payload = JSON.parse(atob(token.split('.')[1]));  // decode base64
  return payload.role === 'ADMIN';
}
```

---

## 7. Checkout & Payment Flow

**Sequence of Operations**

```
1. Cart Page
   │  User clicks "Proceed to Checkout"
   │
2. For each cart item:
   │  POST /orders { productId, quantity }
   │  → Collect returned orderId
   │
3. Clear cart:
   │  DELETE /cart/clear
   │
4. Navigate to /checkout with state:
   │  { orderIds: [...], totalAmount: ... }
   │
5. Checkout Page
   │  Load order details: GET /orders/{id} for each orderId
   │  User fills card form
   │  Clicks "Pay $X"
   │
6. For each orderId:
   │  POST /payments/process { orderId, amount, paymentMethod }
   │  → Track success/failure
   │
7. Navigate to /payment-status with state:
      { success: true/false, transactionId, totalAmount, orderIds }
```

**Error Handling in Checkout**

| Scenario | Behavior |
|---|---|
| Order creation fails | `cart.ordering` set to false, error message shown, stay on cart page |
| Some payments fail | `allSuccess` set to false, navigate to payment-status with failure |
| All payments fail | Navigate to payment-status with failure, "Back to Cart" link shown |
| User navigates directly to /checkout | Redirect to /cart (no state found) |

---

## 8. Admin Dashboard

**Access Control**

Two layers:
1. **UI layer**: Navbar conditionally renders Admin link. Admin component's `ngOnInit` calls `AuthService.isAdmin()` and redirects to `/home` on failure.
2. **API layer**: Backend endpoints enforce ADMIN role via gateway JWT filter. Even if a non-admin manually hits `/admin`, all API calls will return 403.

**Tab Structure**

| Tab | Data Source | Actions |
|---|---|---|
| Products | `GET /products` | Add product (`POST /products`), Delete product (`DELETE /products/{id}`) |
| Orders | `GET /orders` | Update status (`PUT /orders/{orderId}/status`) |
| Users | `GET /users` | Read-only table |

**Add Product Form**

Fields: name, brand, category, price (number), imageUrl, description (textarea). Uses template-driven forms with `[(ngModel)]` bindings. On save, calls `ProductService.createProduct()`, then refreshes the product table.

---

## 9. Styling Architecture

**Approach**: Plain CSS with zero external UI libraries. All styles are hand-written.

**Style Layers**

| Layer | File | Scope |
|---|---|---|
| Global reset & base | `src/styles.css` | Entire app |
| Component styles | `*.component.css` per component | Scoped via Angular emulated encapsulation |

**Design System**

| Token | Value | Usage |
|---|---|---|
| Background | `#f8f9fa` | Page backgrounds |
| Primary text | `#1a1a2e` | Headings, body text |
| Accent (navy) | `#1a1a2e` | Buttons, nav background |
| Accent (red) | `#e94560` | Brand color, primary CTA, hover states |
| Success | `#16a34a` | Payment success, add to cart toast |
| Error | `#dc2626` | Error messages, delete buttons |
| Card background | `#ffffff` | Product cards, form cards |
| Border/divider | `#f0f0f0` | Table rows, card borders |
| Font | `'Inter', sans-serif` | All text |

**Responsive Breakpoints**

| Breakpoint | Behavior |
|---|---|
| >768px | Side-by-side layouts (cart + summary, checkout columns) |
| ≤768px | Single column stacking for all grid layouts |

**Component Size Breakdown**

| Component | ~Lines of CSS | Key Layout Technique |
|---|---|---|
| Navbar | 45 | Flexbox |
| Login | 80 | Flexbox centering |
| Home | 110 | CSS Grid (auto-fill) |
| Cart | 80 | CSS Grid (2-column) |
| Checkout | 70 | CSS Grid (2-column) |
| PaymentStatus | 55 | Flexbox centering |
| Orders | 60 | CSS Grid (table-like) |
| Admin | 100 | CSS Grid (2-column form) |

---

## 10. State Management

**Approach**: No state management library (no NgRx, Signals, or Akita). State is managed through:
1. **localStorage**: JWT token persistence
2. **Component properties**: All UI state is local to each component
3. **Router state**: Data passed between pages via `history.state`

**What State Lives Where**

| Piece of State | Location | Type |
|---|---|---|
| JWT token | `localStorage` | Persistent (survives refresh) |
| Current products list | `Home.products` | Ephemeral (refetched on mount) |
| Cart data | `Cart.cart` | Ephemeral (refetched on mount) |
| Order list | `Orders.orders` | Ephemeral (refetched on mount) |
| Current navigation order IDs | `history.state` | Ephemeral (survives forward navigation only) |
| Form input values | Component properties with `ngModel` | Ephemeral |

**Why Not a State Library?**
- The app has no deeply nested state
- No cross-component shared state beyond the JWT (which is in localStorage)
- No real-time updates or WebSocket connections
- Each page fetches its own data on mount, so there's no stale state concern

---

## 11. Error Handling Strategy

**Pattern**: Per-component error handling via subscription error callbacks.

```ts
this.productService.getAll().subscribe({
  next: (data) => this.products = data,
  error: () => this.message = 'Failed to load products',
});
```

**Error Messages by Component**

| Component | Error Triggers | User-Facing Message |
|---|---|---|
| Home | Product API call fails | "Failed to load products" |
| Home | Add to cart API fails | "Failed to add to cart" |
| Login | Login API fails | "Invalid email or password" |
| Login | Register API fails | Error response message or "Registration failed" |
| Cart | Cart API fails | "Please login to view your cart" |
| Cart | Remove/clear fails | "Failed to remove item" / "Failed to clear cart" |
| Cart | Order creation fails | "Failed to create order" |
| Checkout | Payment API fails | "All payments failed. Please try again." |
| Checkout | Invalid form | "Please fill in all payment details" |
| Orders | Orders API fails | "Failed to load orders" |
| Admin | Any API fails | Varies by tab ("Failed to load products", etc.) |

**Missing Infrastructure**
- No global HTTP error interceptor
- No retry logic
- No offline detection
- No error tracking/logging service

---

## 12. Performance Considerations

**Current Performance Characteristics**

| Metric | Value |
|---|---|
| Initial bundle size | ~305 kB (raw), ~78 kB (transfer) |
| Number of HTTP requests on page load | 1 (product list) + font CSS |
| Angular version | 21 (up-to-date) |
| Lazy loading | Not implemented (all components eagerly loaded) |
| Change detection | Default (Zone.js) |

**Potential Optimizations**

| Issue | Improvement |
|---|---|
| All components eagerly loaded | Use `loadComponent` in routes for lazy loading admin/orders/checkout |
| No image optimization | Add lazy loading (`loading="lazy"` on product images) |
| Repeated product detail API calls | Cache product details or inline them in cart response |
| Global styles not purged | None (no CSS framework, so minimal unused CSS) |
| Font loaded from external CDN | Self-host Inter font or use system font stack |

---

## 13. Dependencies (package.json)

| Package | Purpose |
|---|---|
| @angular/core | Framework runtime, DI, change detection |
| @angular/router | Client-side routing |
| @angular/forms | Template-driven forms (ngModel) |
| @angular/common/http | HTTP client |
| @angular/build | Build infrastructure (Vite/esbuild) |
| @angular/cli | Development tooling |
| typescript | Language compiler |
| rxjs | Reactive extensions (Observable) |

No third-party UI libraries (no Angular Material, Bootstrap, PrimeNG, Tailwind).

---

## 14. Security

| Concern | Current Status | Recommendation |
|---|---|---|
| XSS | Minimal risk — Angular auto-escapes template expressions | Add Content Security Policy header |
| JWT theft | Stored in localStorage (accessible to any JS) | Migrate to HttpOnly cookies |
| CSRF | Not applicable (token in header, not cookie) | Already secure |
| Role spoofing | Not possible — backend enforces roles | Already secure |
| Direct URL access to protected routes | Handled per-component (redirect or error) | Add route guards for consistency |
| Sensitive data in URLs | Not present — uses history.state | Already secure |

---

## 15. File Inventory & Line Counts

| File | Lines (approx) | Purpose |
|---|---|---|
| `app.config.ts` | 10 | Application providers |
| `app.routes.ts` | 20 | Route definitions |
| `app.ts` | 12 | Root component class |
| `app.html` | 3 | Root template |
| `app.css` | 0 | Empty |
| `services/api.service.ts` | 45 | Base HTTP client |
| `services/auth.ts` | 60 | Authentication service |
| `services/product.ts` | 35 | Product service |
| `services/cart.ts` | 40 | Cart service |
| `services/order.ts` | 35 | Order service |
| `services/payment.ts` | 30 | Payment service |
| `navbar/navbar.ts` | 25 | Navbar component |
| `navbar/navbar.html` | 18 | Navbar template |
| `navbar/navbar.css` | 45 | Navbar styles |
| `login/login.ts` | 55 | Login component |
| `login/login.html` | 40 | Login template |
| `login/login.css` | 80 | Login styles |
| `home/home.ts` | 45 | Home component |
| `home/home.html` | 35 | Home template |
| `home/home.css` | 110 | Home styles |
| `cart/cart.ts` | 100 | Cart component |
| `cart/cart.html` | 50 | Cart template |
| `cart/cart.css` | 80 | Cart styles |
| `checkout/checkout.ts` | 90 | Checkout component |
| `checkout/checkout.html` | 55 | Checkout template |
| `checkout/checkout.css` | 70 | Checkout styles |
| `payment-status/payment-status.ts` | 25 | Payment status component |
| `payment-status/payment-status.html` | 25 | Payment status template |
| `payment-status/payment-status.css` | 55 | Payment status styles |
| `orders/orders.ts` | 25 | Orders component |
| `orders/orders.html` | 30 | Orders template |
| `orders/orders.css` | 60 | Orders styles |
| `admin/admin.ts` | 85 | Admin component |
| `admin/admin.html` | 85 | Admin template |
| `admin/admin.css` | 100 | Admin styles |
| `styles.css` | 15 | Global styles |
| `index.html` | 13 | SPA entry HTML |
| `main.ts` | 6 | Bootstrap |
| **Total** | **~1,750** | |
