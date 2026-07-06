# Implementation Q&A — SneakerStore E-Commerce Application

## 1. Architecture & Design Decisions

**Q: Why use a microservices architecture instead of a monolithic backend?**

A: The backend is split into seven services (API Gateway, Eureka, User, Product, Order, Cart, Payment) to decouple concerns. Each service owns its domain logic and can be scaled independently. For example, if the payment system gets heavy load, only the payment-service needs more replicas. The gateway acts as a single entry point, routing requests to the correct service and handling cross-cutting concerns like JWT validation and CORS.

**Q: Why is there an API Gateway between the frontend and the backend services?**

A: The gateway (Spring Cloud Gateway on port 8080) is the single interface for the frontend. Without it, the Angular app would need to know the addresses of all six backend services. The gateway also centralizes authentication — every request passes through the JwtAuthenticationFilter, which validates the token, extracts user claims, and injects X-User-Id / X-User-Role headers downstream. This means individual services don't need their own auth logic.

**Q: Why use Eureka for service discovery?**

A: In a microservices setup, service locations change (ports, hosts, replicas). Eureka lets services register themselves and discover each other by name rather than hardcoded URLs. For example, the order-service calls the product-service via `http://product-service` (the Eureka service ID) instead of `http://localhost:8082`. This makes the system resilient to port changes and enables load balancing.

**Q: Why does the frontend communicate through the gateway instead of calling services directly?**

A: A few reasons: (1) CORS is configured once on the gateway rather than on every service; (2) authentication is handled in one place — the gateway's JWT filter validates the token before any request reaches a backend service; (3) the frontend only needs to know one URL (`localhost:8080`) instead of six different service URLs.

---

## 2. Authentication & Authorization

**Q: How does the login flow work from frontend to backend?**

A: The user enters email and password on the Angular login page, which sends a POST to `/users/login` via the gateway. The user-service validates the credentials, generates a JWT containing the userId and role, and returns it. The frontend stores this token in `localStorage`. On every subsequent request, the frontend attaches it as an `Authorization: Bearer <token>` header. The gateway's filter validates the JWT, extracts the claims, and injects X-User-Id and X-User-Role headers before forwarding to the backend service.

**Q: How does the frontend know if a user is an admin?**

A: The JWT payload is a base64-encoded JSON object containing the `role` field. The frontend decodes the token client-side (`atob(token.split('.')[1])`) to read the role without needing to call the server. This is used to conditionally show the Admin link in the navbar and to protect the `/admin` route — if a non-admin navigates there, the component redirects to `/home`.

**Q: Is it safe to decode the JWT on the frontend to check the role?**

A: For UI purposes only, yes. The frontend decodes the token to decide what to show or hide. But the real authorization happens on the backend — the gateway's filter and the downstream services check the role from the validated JWT claims. A user cannot escalate privileges by modifying the frontend code because the backend enforces access control independently.

**Q: What happens if the JWT expires while the user is browsing?**

A: Currently, the application does not handle token refresh. If the token expires, the next API call will return a 401 error, which the user sees as a failure message (e.g., "Failed to load orders"). The user would need to log in again. A production version would implement refresh tokens or silent token renewal using HTTP interceptor.

---

## 3. Product Catalog

**Q: How does the product listing page work?**

A: The Home component calls `ProductService.getAll()` on init, which hits `GET /products` through the gateway. The product-service queries all products from the MySQL database and returns them. The frontend renders them in a CSS grid of cards showing image, brand, name, price, and an "Add to Cart" button.

**Q: How does the "Add to Cart" button work for unauthenticated users?**

A: Before calling the cart API, the Home component checks `AuthService.isLoggedIn()`. If false, it redirects to `/login` instead of making the API call. This prevents a 401 error and gives the user a clear path to authenticate.

**Q: How are products added and deleted from the admin dashboard?**

A: The admin dashboard calls `ProductService.createProduct()` (POST /products) with the form data when saving, and `ProductService.deleteProduct()` (DELETE /products/{id}) when deleting. Both endpoints require ADMIN role, enforced by the gateway's JWT filter. If a regular user somehow tries to call these, the backend returns 403 Forbidden.

---

## 4. Cart & Checkout Flow

**Q: Walk me through the complete checkout flow.**

A: 
1. User clicks "Add to Cart" on the product page → POST /cart/add with productId and quantity
2. User navigates to `/cart` → GET /cart returns all items with total price
3. User clicks "Proceed to Checkout" → for each cart item, the frontend calls POST /orders to create a separate order, then clears the cart and navigates to `/checkout` with the order IDs passed via router state
4. On the checkout page, the user fills in card details and clicks "Pay Now" → for each order, the frontend calls POST /payments/process
5. After all payments are processed, the frontend navigates to `/payment-status` with the results via router state
6. The payment-status page shows a green success card with the transaction ID or a red failure card with a retry suggestion

**Q: Why does the frontend create one order per cart item instead of one order for the whole cart?**

A: The backend's Order entity is designed for single-product orders — each Order has a single productId and quantity. There is no "multi-line order" concept. To handle a cart with multiple items, the frontend iterates and creates one order per cart item. This is a design limitation of the backend that the frontend works around.

**Q: How does the payment simulation work?**

A: The payment-service has a configurable `payment.simulation.mode` property. In `random` mode, it randomly succeeds or fails (~50/50). In `success` mode, every payment succeeds. In `failure` mode, every payment fails. On success, the order status is set to CONFIRMED and a transaction ID is generated. On failure, the order is CANCELLED. This is useful for testing without a real payment gateway.

**Q: What happens if one payment succeeds and another fails in a multi-order checkout?**

A: The current frontend logic treats the entire checkout as failed if any single payment fails (`allSuccess = false`). It navigates to the payment-status page with `success: false`. The user can then go back to the cart and retry. A more sophisticated approach would handle partial success — showing which orders succeeded and letting the user retry only the failed ones.

**Q: How are order IDs passed from the checkout page to the payment-status page?**

A: Angular's router state is used. After creating orders, the cart component navigates with `this.router.navigate(['/checkout'], { state: { orderIds, totalAmount } })`. The checkout component reads `history.state` to get the data. Same mechanism passes results to the payment-status page. This avoids exposing order IDs in the URL while still allowing data to survive page transitions within the same tab session.

---

## 5. Admin Dashboard

**Q: What features does the admin dashboard provide?**

A: Three tabs:
- **Products**: View all products in a table, add new products via a form, delete existing products
- **Orders**: View all orders across all users, update order status via dropdown (CREATED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED)
- **Users**: View all registered users with their roles

**Q: How does the admin dashboard enforce that only admins can access it?**

A: Two layers:
1. **Frontend**: The navbar conditionally renders the Admin link only if `AuthService.isAdmin()` returns true. The Admin component's `ngOnInit` checks `isAdmin()` and redirects to `/home` if false.
2. **Backend**: All admin-only endpoints (POST/DELETE products, GET all users, GET all orders, PUT order status) require the ADMIN role, enforced by the gateway's JWT filter. Even if a regular user manually types `/admin` in the URL, their API calls will fail with 403.

**Q: Why is there no admin registration flow?**

A: The registration endpoint (`POST /users`) accepts a `role` field, but the frontend's registration form always sends `role: 'USER'`. Admin users would need to be created directly via the backend (e.g., by calling the API with an ADMIN role, or by inserting into the database). This is intentional — admin accounts should not be creatable through the public registration form for security reasons.

---

## 6. Frontend Architecture

**Q: Why was Angular chosen for the frontend?**

A: The project specification required Angular. It provides a structured framework with dependency injection, HttpClient for API communication, FormsModule for template-driven forms, and the Angular CLI for scaffolding. The standalone component model (Angular 17+) simplifies the setup by removing the need for NgModules.

**Q: How is the API service structured?**

A: The `ApiService` wraps Angular's `HttpClient` with automatic JWT header injection. Every HTTP method (GET, POST, PUT, DELETE) reads the token from `localStorage`, creates headers with `Authorization: Bearer <token>`, and makes the request. All other services (`AuthService`, `ProductService`, `CartService`, `OrderService`, `PaymentService`) depend on `ApiService` and only provide domain-specific methods and interfaces.

**Q: Why use `localStorage` for the JWT instead of cookies or sessionStorage?**

A: `localStorage` persists across browser tabs and sessions, so the user stays logged in even after closing and reopening the browser. It's simpler than cookie-based auth for an SPA. The tradeoff is that `localStorage` is accessible to any JavaScript running on the same origin, making it vulnerable to XSS attacks. A production app would use HttpOnly cookies or at minimum implement Content Security Policy headers.

**Q: How does the frontend handle navigation state for multi-page flows like checkout?**

A: The checkout flow spans three pages (cart → checkout → payment-status). Data like order IDs, total amounts, and payment results are passed using Angular's router state (`history.state`). This approach keeps sensitive data out of the URL while still allowing the browser's back/forward navigation to work (though without proper state, users will be redirected — e.g., the checkout page redirects to `/cart` if no order data is found in history state).

**Q: Why are all services providedIn: 'root'?**

A: `providedIn: 'root'` makes each service a singleton available application-wide without needing to add it to any providers array. Since services like `ApiService`, `AuthService`, and `CartService` need to share state (like the auth token) across all components, singletons are the correct choice.

---

## 7. Error Handling & Edge Cases

**Q: How are API errors handled on the frontend?**

A: Every HTTP call subscribes with `next` and `error` handlers. On error, a user-friendly message is set on the component (e.g., "Failed to load products", "Invalid email or password"). There is no global HTTP error interceptor — errors are handled per-component, which means some messages are inconsistent. A production app would benefit from a centralized error handler.

**Q: What happens if a user navigates directly to `/checkout` without going through the cart?**

A: The Checkout component checks `history.state.orderIds` in `ngOnInit`. If there's no order data, it immediately redirects to `/cart`. This prevents users from seeing an empty or broken checkout page.

**Q: What happens if the backend services are not running?**

A: Every API call will fail with a network error (net::ERR_CONNECTION_REFUSED). The component error handlers will show messages like "Failed to load products" or "Please login to view your cart". The app doesn't crash — it gracefully shows error states. However, there's no retry mechanism or loading skeleton, so the experience is not ideal.

**Q: How does the frontend handle the case where the cart is empty and the user clicks "Proceed to Checkout"?**

A: The checkout method in the Cart component has an early return: `if (this.cart.items.length === 0) return;`. This prevents an unnecessary API call and navigation to the checkout page with an empty order.

---

## 8. Potential Improvements

**Q: What would you improve if you had more time?**

A: Several areas:
- **Loading states**: Add spinner/skeleton components while API calls are in progress
- **Token refresh**: Implement automatic token refresh when the JWT expires
- **Global error interceptor**: Centralize error handling with toast notifications instead of per-component messages
- **Form validation**: Add proper validators (required fields, email format, card number format) with inline error messages
- **Search & filter**: Add product search bar and category filter on the home page
- **Quantity update in cart**: Allow users to change item quantities directly in the cart view
- **Responseive design**: Further improve mobile layout for the admin tables and checkout form
- **Order cancellation**: Let users cancel their own orders from the orders page if status is still CREATED
- **Unit tests**: Add tests for services and components
- **Environment config**: Move the API base URL to an environment file instead of hardcoding it in ApiService

**Q: How would you add product search functionality?**

A: The backend doesn't have a search endpoint, so I'd either:
1. Add a search endpoint to the product-service (e.g., `GET /products/search?q=...`)
2. Or fetch all products on the frontend and filter client-side using JavaScript's `filter()` and `includes()` — simpler but doesn't scale to large catalogs

**Q: How would you implement image upload instead of image URLs?**

A: The current product form accepts an image URL string. For actual image upload, I'd:
1. Add a multipart file upload endpoint to the product-service
2. Store images on the filesystem or in cloud storage (S3)
3. Serve images via a static resource handler or CDN
4. On the frontend, replace the text input with a file picker (`<input type="file">`) and use `FormData` to upload the file
