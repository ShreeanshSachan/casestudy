# Backend System Document — SneakerStore Microservices

## 1. System Overview

SneakerStore is a Java-based e-commerce backend built with Spring Boot 3.3.2 and Spring Cloud 2023.0.3, following a microservices architecture. Seven services work together to provide user management, product catalog, shopping cart, order processing, and payment simulation.

**Architecture Diagram**

```
Frontend (Angular)
    │
    ▼
API Gateway (8080) ── JWT Auth + RBAC + CORS
    │
    ├──▶ Eureka Server (8761) — Service Discovery
    │
    ├──▶ User Service (8081)  ── Auth, Users
    ├──▶ Product Service (8082) ── Product Catalog
    ├──▶ Order Service (8083)  ── Orders, Feign ──▶ User + Product
    ├──▶ Cart Service (8084)   ── Cart, Feign ──▶ Product
    └──▶ Payment Service (8085) ── Payments, Feign ──▶ Order
```

**Tech Stack**

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Cloud | Spring Cloud 2023.0.3 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (Reactive) |
| Database | MySQL 8 |
| ORM | Hibernate (spring-boot-starter-data-jpa) |
| Build | Maven |
| JWT | jjwt 0.11.5 (HMAC-SHA) |
| Feign | Spring Cloud OpenFeign |
| API Docs | SpringDoc OpenAPI 2.5.0 (Swagger UI) |

**Database**
- Engine: MySQL on `localhost:3306`
- Schema: `sneakerproject`
- Credentials: `springstudent` / `springstudent`
- DDL: `hibernate.ddl-auto=update` (auto-creates tables)

---

## 2. Service Discovery — Eureka Server (Port 8761)

### 2.1 Purpose
Provides service registration and discovery. All other services register themselves by name (`USER-SERVICE`, `PRODUCT-SERVICE`, etc.) so that the gateway and Feign clients can route to them without hardcoded addresses.

### 2.2 Configuration
```properties
server.port=8761
spring.application.name=EUREKA-SERVER
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.server.enable-self-preservation=false
```

### 2.3 Endpoints
- Dashboard: `http://localhost:8761/`
- REST API: `http://localhost:8761/eureka/`

---

## 3. API Gateway (Port 8080)

### 3.1 Purpose
Single entry point for the frontend. Routes requests to the appropriate microservice, validats JWT tokens, enforces role-based access control (RBAC), and configures CORS.

### 3.2 Route Configuration

| Route Path | Destination Service |
|---|---|
| `/users/**` | `lb://USER-SERVICE` |
| `/products/**` | `lb://PRODUCT-SERVICE` |
| `/orders/**` | `lb://ORDER-SERVICE` |
| `/cart/**` | `lb://CART-SERVICE` |
| `/payments/**` | `lb://PAYMENT-SERVICE` |
| `/*/v3/api-docs` | Respective service (Swagger aggregation) |

### 3.3 CORS Configuration

| Property | Value |
|---|---|
| Allowed Origin | `http://localhost:4200` |
| Allowed Methods | `*` |
| Allowed Headers | `*` |
| Allow Credentials | `true` |

### 3.4 Authentication & RBAC

**JWT Validation Flow**

```
Request ──▶ Extract Bearer token ──▶ Validate HMAC signature ──▶ Parse claims
                                                                       │
Public routes bypass auth ──────────────┐                               │
  - POST /users/login                   │                               ▼
  - POST /users                         │                    { userId, role }
  - GET /products/**                    │                               │
  - Swagger/docs paths                  │                               ▼
                                        │                     Inject headers:
                                        │              X-User-Id, X-User-Role
                                        ▼                               │
                                  Permitted ────────────────────────────▶ Forward to service
```

**RBAC Matrix**

| Path Pattern | Required Role |
|---|---|
| `POST /products/**`, `PUT /products/**`, `DELETE /products/**` | ADMIN |
| `GET /products/**` | PUBLIC (no auth) |
| `POST /users/login`, `POST /users` | PUBLIC |
| `GET /users`, `DELETE /users/**` | ADMIN |
| `GET /users/{id}`, `PUT /users/{id}` | USER (self) or ADMIN |
| `GET /orders`, `PUT /orders/{id}/status` | ADMIN |
| `POST /orders`, `GET /orders/my`, `GET /orders/{id}` | USER |
| `/cart/**` | USER or ADMIN |
| `POST /payments/process`, `GET /payments/user` | USER |
| `GET /payments/{orderId}` | USER (own) or ADMIN |

### 3.5 Key Files

| File | Responsibility |
|---|---|
| `CorsConfig.java` | Reactive CORS filter allowing `localhost:4200` |
| `SecurityConfig.java` | Disables CSRF, configures public route patterns |
| `JwtAuthenticationFilter.java` | Global gateway filter — extracts JWT, validates, injects headers, enforces RBAC |
| `JwtUtil.java` | JWT parsing and validation utility (HMAC-SHA, secret: `"secretkeysecretkeysecretkeysecretkey"`) |

---

## 4. User Service (Port 8081)

### 4.1 Purpose
Manages user registration, authentication (JWT generation), and user profile CRUD operations.

### 4.2 Entity — `users` Table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `name` | VARCHAR(255) | NOT NULL |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL |
| `password` | VARCHAR(255) | Write-only via `@JsonProperty(WRITE_ONLY)` |
| `role` | VARCHAR(255) | `"USER"` or `"ADMIN"` |

### 4.3 API Endpoints

| Method | Path | Auth | Request | Response | Description |
|---|---|---|---|---|---|
| POST | `/users/login` | Public | `{ email, password }` | `{ token }` | Authenticate, returns JWT |
| POST | `/users` | Public | `User` JSON | `User` | Register new user |
| GET | `/users` | ADMIN | — | `User[]` | List all users |
| GET | `/users/{id}` | USER/ADMIN | — | `User` | Get user by ID |
| GET | `/users/email?email=` | USER/ADMIN | Query param | `User` | Get user by email |
| PUT | `/users/{id}` | USER/ADMIN | `User` JSON | `User` | Update profile |
| DELETE | `/users/{id}` | ADMIN | — | String | Delete user |

### 4.4 Service Logic

| Operation | Behavior |
|---|---|
| Register | Checks for duplicate email → saves with provided role |
| Login | Finds by email → compares password (plaintext) → generates JWT with claims `{ userId, role }` |
| Update | Non-ADMIN users cannot change their role; checks email uniqueness if email is being changed |

### 4.5 JWT Token

| Property | Value |
|---|---|
| Algorithm | HMAC-SHA (jjwt) |
| Secret | `secretkeysecretkeysecretkeysecretkey` |
| Expiry | 1 hour (3600000ms) |
| Claims | `userId` (Long), `role` (String), `sub` (email) |

### 4.6 Key Files

| File | Responsibility |
|---|---|
| `UserController.java` | REST endpoints with auth header validation |
| `UserService.java` | Business logic, JWT generation |
| `JwtUtil.java` | Token creation utility |
| `UserRepository.java` | JPA repository with `findByEmail()` |
| `SecurityConfig.java` | Permits all requests (auth delegated to gateway) |

---

## 5. Product Service (Port 8082)

### 5.1 Purpose
CRUD operations for the sneaker product catalog. Read endpoints are public; write endpoints require ADMIN role.

### 5.2 Entity — `product` Table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `name` | VARCHAR(255) | — |
| `price` | DOUBLE | — |
| `brand` | VARCHAR(255) | — |
| `category` | VARCHAR(255) | — |
| `description` | VARCHAR(255) | — |
| `image_url` | VARCHAR(255) | URL string for product image |

### 5.3 API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/products` | PUBLIC | List all products |
| GET | `/products/{id}` | PUBLIC | Get product by ID |
| GET | `/products/category/{category}` | PUBLIC | Filter by category (case-insensitive) |
| POST | `/products` | ADMIN | Create product |
| PUT | `/products/{id}` | ADMIN | Update product |
| DELETE | `/products/{id}` | ADMIN | Delete product |

### 5.4 Key Files

| File | Responsibility |
|---|---|
| `ProductController.java` | REST endpoints |
| `ProductService.java` | Business logic, category search |
| `ProductRepository.java` | JPA repository with `findByCategoryIgnoreCase()` |

---

## 6. Order Service (Port 8083)

### 6.1 Purpose
Handles order placement, retrieval, and status management. Each order is a single product line item. Makes Feign calls to User Service and Product Service to enrich order data with user names and product details.

### 6.2 Entity — `orders` Table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `user_id` | BIGINT | References User |
| `product_id` | BIGINT | References Product |
| `quantity` | INT | — |
| `total_price` | DOUBLE | Calculated as `product.price * quantity` |
| `status` | VARCHAR(255) | CREATED / CONFIRMED / SHIPPED / DELIVERED / CANCELLED |

### 6.3 API Endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/orders` | USER | `{ productId, quantity }` | `OrderResponse` |
| GET | `/orders/{id}` | USER/ADMIN | — | `OrderResponse` |
| GET | `/orders` | ADMIN | — | `OrderResponse[]` |
| GET | `/orders/my` | USER | — | `OrderResponse[]` |
| PUT | `/orders/{orderId}/status` | ADMIN | `{ status }` | `OrderResponse` |

### 6.4 OrderResponse Structure

```json
{
  "orderId": 1,
  "userId": 1,
  "userName": "John Doe",
  "productId": 1,
  "productName": "Air Max 270",
  "productPrice": 149.99,
  "quantity": 2,
  "totalPrice": 299.98,
  "status": "CREATED"
}
```

### 6.5 Feign Clients

| Client | Target | Method | Used For |
|---|---|---|---|
| `UserClient` | `USER-SERVICE` | `getUserById(id)` | Enriching order with user name |
| `ProductClient` | `PRODUCT-SERVICE` | `getProductById(id)` | Enriching order with product name/price |

### 6.6 Header Propagation

`FeignConfig.java` includes a `RequestInterceptor` that reads `X-User-Id` and `X-User-Role` from the current HTTP request and forwards them to downstream Feign calls, maintaining the auth context across service boundaries.

### 6.7 Key Files

| File | Responsibility |
|---|---|
| `OrderController.java` | REST endpoints |
| `OrderService.java` | Order lifecycle, Feign integration, status validation |
| `OrderRepository.java` | JPA repository with `findByUserId()` |
| `ProductClient.java` | Feign interface to Product Service |
| `UserClient.java` | Feign interface to User Service |
| `FeignConfig.java` | Header propagation interceptor |
| `FeignErrorDecoder.java` | Maps Feign HTTP errors to domain exceptions |

---

## 7. Cart Service (Port 8084)

### 7.1 Purpose
Manages a per-user shopping cart. Supports add, update quantity, remove items, and clear cart. Automatically recalculates total price on every mutation.

### 7.2 Entities

**`carts` Table**

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `user_id` | BIGINT | UNIQUE (one cart per user) |
| `total_price` | DOUBLE | Default 0.0 |

**`cart_items` Table**

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `cart_id` | BIGINT | FK → carts.id |
| `product_id` | BIGINT | FK → product.id |
| `quantity` | INT | — |
| `price` | DOUBLE | Unit price at time of add |

Unique constraint on `(cart_id, product_id)` prevents duplicate product entries.

### 7.3 API Endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/cart` | USER/ADMIN | — | `CartResponse` |
| POST | `/cart/add` | USER/ADMIN | `{ productId, quantity }` | String |
| PUT | `/cart/update` | USER/ADMIN | `{ productId, quantity }` | String |
| DELETE | `/cart/{productId}` | USER/ADMIN | — | String |
| DELETE | `/cart/clear` | USER/ADMIN | — | String |

### 7.4 CartResponse Structure

```json
{
  "userId": 1,
  "totalPrice": 299.97,
  "items": [
    { "productId": 1, "quantity": 2, "price": 149.99 },
    { "productId": 2, "quantity": 1, "price": 89.99 }
  ]
}
```

### 7.5 Business Logic

| Operation | Behavior |
|---|---|
| Add Item | Gets/creates cart → fetches product price via Feign → adds item or increments quantity → recalculates total |
| Update Quantity | If new qty ≤ 0, removes item; otherwise updates → recalculates total |
| Remove Item | Deletes specific item → recalculates total |
| Clear Cart | Deletes all items → resets total to 0.0 |

### 7.6 Feign Client

| Client | Target | Method |
|---|---|---|
| `ProductClient` | `PRODUCT-SERVICE` | `getProductById(id)` → fetches price for cart calculations |

### 7.7 Key Files

| File | Responsibility |
|---|---|
| `CartController.java` | REST endpoints |
| `CartService.java` | Cart CRUD, total recalculation, Feign integration |
| `CartRepository.java` | JPA repository with `findByUserId()` |
| `CartItemRepository.java` | JPA repository with `findByCartId()`, `findByCartIdAndProductId()`, `deleteByCartId()` |
| `ProductClient.java` | Feign interface to Product Service |

---

## 8. Payment Service (Port 8085)

### 8.1 Purpose
Processes payments with a simulation mode (configurable for testing). Validates amount, checks idempotency (one payment per order), and updates the order status upon completion.

### 8.2 Entity — `payments` Table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `order_id` | BIGINT | NOT NULL |
| `user_id` | BIGINT | NOT NULL |
| `amount` | DOUBLE | NOT NULL |
| `status` | VARCHAR(255) | ENUM: SUCCESS / FAILED / PENDING |
| `payment_method` | VARCHAR(255) | — |
| `transaction_id` | VARCHAR(255) | Generated UUID on success |
| `created_at` | DATETIME | Auto-set via `@PrePersist` |

### 8.3 API Endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/payments/process` | USER | `{ orderId, amount, paymentMethod }` | `PaymentResponse` |
| GET | `/payments/{orderId}` | USER/ADMIN | — | `PaymentResponse` |
| GET | `/payments/user` | USER | — | `PaymentResponse[]` |

### 8.4 Payment Flow

```
POST /payments/process
    │
    1. Fetch order via Feign (GET /orders/{orderId})
    │
    2. Verify order belongs to requesting user
    │
    3. Check existing payment for idempotency
    │
    4. Validate amount matches order.totalPrice
    │
    5. Simulate payment:
       │
       ├── mode=random:  50% SUCCESS, 50% FAILED
       ├── mode=success: 100% SUCCESS
       └── mode=failure: 100% FAILED
    │
    6. Persist Payment record
    │
    7. Update order status via Feign (PUT /orders/{id}/status):
       │
       ├── SUCCESS → CONFIRMED
       └── FAILED  → CANCELLED
    │
    ▼
Return PaymentResponse
```

### 8.5 Configuration

| Property | Values | Default |
|---|---|---|
| `payment.simulation.mode` | `random`, `success`, `failure` | `random` |

### 8.6 Feign Client

| Client | Target | Methods |
|---|---|---|
| `OrderClient` | `ORDER-SERVICE` | `getOrderById()` — fetch order details |
| | | `updateOrderStatus()` — update order status after payment |

### 8.7 Key Files

| File | Responsibility |
|---|---|
| `PaymentController.java` | REST endpoints |
| `PaymentService.java` | Payment processing, simulation, Feign orchestration |
| `PaymentRepository.java` | JPA repository with `findByOrderId()`, `findByUserId()` |
| `OrderClient.java` | Feign interface to Order Service |
| `PaymentStatus.java` | Enum: SUCCESS, FAILED, PENDING |

---

## 9. Database Schema

### Entity Relationship

```
users ─────┬───< orders     (userId FK)
           │
           └───< carts      (userId FK, unique)

product ───┬───< orders     (productId FK)
           │
           └───< cart_items (productId FK)

carts ─────┬───< cart_items (cartId FK)

orders ────┬───< payments   (orderId FK)
```

### Complete Table Reference

| Table | Columns | Key Constraints |
|---|---|---|
| `users` | id, name, email (unique), password, role | PK: id |
| `product` | id, name, price, brand, category, description, image_url | PK: id |
| `orders` | id, user_id, product_id, quantity, total_price, status | PK: id |
| `carts` | id, user_id (unique), total_price | PK: id |
| `cart_items` | id, cart_id, product_id, quantity, price | PK: id, UK: (cart_id, product_id) |
| `payments` | id, order_id, user_id, amount, status, payment_method, transaction_id, created_at | PK: id |

---

## 10. Inter-Service Communication

### Feign Client Dependency Map

```
ORDER-SERVICE ──Feign──▶ USER-SERVICE    (GET /users/{id})
ORDER-SERVICE ──Feign──▶ PRODUCT-SERVICE  (GET /products/{id})
CART-SERVICE  ──Feign──▶ PRODUCT-SERVICE  (GET /products/{id})
PAYMENT-SERVICE ─Feign──▶ ORDER-SERVICE   (GET /orders/{orderId})
PAYMENT-SERVICE ─Feign──▶ ORDER-SERVICE   (PUT /orders/{orderId}/status)
```

### Header Propagation

The `FeignConfig` in the order-service registers a `RequestInterceptor` that copies `X-User-Id` and `X-User-Role` from the current HTTP request context into every outgoing Feign request, ensuring the auth context is maintained across service boundaries.

### Error Handling for Feign Calls

| Downstream HTTP Status | Exception Thrown | Endpoint Behavior |
|---|---|---|
| 400 | `BadRequestException` | HTTP 400 to client |
| 404 | `ResourceNotFoundException` | HTTP 404 to client |
| 500 | `DownstreamServiceException` | HTTP 502 to client |
| Other 5xx | `DownstreamServiceException` | HTTP 502 to client |

---

## 11. Exception Handling

### Standard Error Response Format

```json
{
  "timestamp": "2026-07-06T12:00:00.000",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Product not found with id: 999",
  "path": "/products/999"
}
```

### HTTP Status Mapping

| Exception Class | HTTP Status | Error Type |
|---|---|---|
| `ResourceNotFoundException` | 404 | NOT_FOUND |
| `BadRequestException` | 400 | BAD_REQUEST |
| `DownstreamServiceException` | 502 | DOWNSTREAM_SERVICE_ERROR |
| `MissingRequestHeaderException` | 400 | BAD_REQUEST |
| `RuntimeException` / `Exception` | 500 | INTERNAL_SERVER_ERROR |

**Note:** The payment service (`GlobalException.java`) uses a simpler format — plain `ResponseEntity<String>` with a message body and HTTP status.

---

## 12. Security Architecture

### Authentication Layers

| Layer | Mechanism | Responsibility |
|---|---|---|
| API Gateway | JWT token validation + RBAC | Validates every request, enforces role-based access |
| Downstream Services | Header-based (`X-User-Id`, `X-User-Role`) | Trusts gateway-injected headers for ownership checks |
| Inter-Service (Feign) | Header propagation | Maintains auth context across service calls |

### Role Model

| Role | Capabilities |
|---|---|
| `USER` | Register, manage own profile, browse products, manage own cart, place orders, view own orders/payments |
| `ADMIN` | All USER capabilities + manage all products, view all users, view/manage all orders, view all payments |

### Credential Storage

Passwords are stored as plaintext in the database. The `SecurityConfig` defines a `BCryptPasswordEncoder` bean but it is not used — passwords are compared with direct string equality in `UserService.login()`.

---

## 13. Project File Inventory

### Per-Service File Count

| Service | Java Files | Config Files | Total |
|---|---|---|---|
| eureka-server | 1 | 1 | 2 |
| api-gateway | 4 | 2 | 6 |
| user-service | 9 | 1 | 10 |
| product-service | 7 | 1 | 8 |
| order-service | 13 | 2 | 15 |
| cart-service | 10 | 1 | 11 |
| payment-service | 12 | 1 | 13 |
| **Total** | **56** | **9** | **65** |

### Shared Patterns

| Pattern | Used Across |
|---|---|
| `@ControllerAdvice` with `@ExceptionHandler` | All 5 business services |
| `ResourceNotFoundException` → 404 | User, Product, Order, Cart |
| `BadRequestException` → 400 | User, Product, Order, Cart |
| `ErrorResponse` DTO (timestamp, status, error, message, path) | User, Product, Order, Cart |
| `application.properties` for config | All 7 services |
| Maven POM with common plugins | All 7 services |

---

## 14. How to Run

### Prerequisites
- Java 17+ JDK
- Apache Maven 3.8+
- MySQL 8+ running on `localhost:3306`
- Create database: `CREATE DATABASE sneakerproject;`

### Startup Order

```
1. Start MySQL (ensure sneakerproject exists)
2. cd eureka-server        && mvn spring-boot:run     (port 8761)
3. cd api-gateway          && mvn spring-boot:run     (port 8080)
4. cd user-service         && mvn spring-boot:run     (port 8081)
5. cd product-service      && mvn spring-boot:run     (port 8082)
6. cd order-service        && mvn spring-boot:run     (port 8083)
7. cd cart-service         && mvn spring-boot:run     (port 8084)
8. cd payment-service      && mvn spring-boot:run     (port 8085)
```

### Access Points
- **Aggregated Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Eureka Dashboard:** `http://localhost:8761/`
- **API Base:** `http://localhost:8080/`
