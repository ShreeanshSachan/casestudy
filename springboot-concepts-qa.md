# Spring Boot Concepts Q&A — SneakerStore Project

## 1. Spring Boot Application Setup

**Q: What is `@SpringBootApplication` and what does it include?**

A: It is a convenience annotation that combines three annotations:
- `@Configuration` — marks the class as a source of bean definitions
- `@EnableAutoConfiguration` — tells Spring Boot to auto-configure based on classpath dependencies
- `@ComponentScan` — scans the package and sub-packages for components

Every service in this project has one:

```java
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**Q: How is the application packaged and run?**

A: Each service is a Maven project. The `spring-boot-maven-plugin` packages it as an executable JAR. It can be run with `mvn spring-boot:run` or `java -jar target/*.jar`.

---

## 2. REST Controllers

**Q: What annotations are used to define REST API endpoints?**

A: The project uses:
- `@RestController` — marks the class as a controller where every method returns a response body (combines `@Controller` + `@ResponseBody`)
- `@RequestMapping("/path")` — class-level URL prefix
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` — method-level HTTP method mappings

Example from `ProductController`:

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return new ResponseEntity<>(productService.createProduct(product), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
}
```

**Q: What is `ResponseEntity` and why is it used?**

A: `ResponseEntity` gives full control over the HTTP response — status code, headers, and body. It is used to return specific HTTP statuses (e.g., `201 CREATED` for creation, `200 OK` for success, `404 NOT FOUND` for errors).

**Q: What parameter annotations are used?**

A: Three types:
- `@PathVariable` — extracts values from URL path (`/products/{id}` → `Long id`)
- `@RequestParam` — extracts query parameters (`/users/email?email=x` → `String email`)
- `@RequestBody` — deserializes the HTTP request body into a Java object
- `@RequestHeader` — extracts HTTP header values (`X-User-Id`, `X-User-Role`)
- `@Valid` — triggers JSR-380 bean validation on request bodies

---

## 3. Dependency Injection & Stereotypes

**Q: What stereotype annotations are used?**

A: Three:
- `@Service` — marks a class as a business service facade (e.g., `UserService`, `ProductService`)
- `@Repository` — marks a class as a data repository (though JPA repositories use a different approach)
- `@Component` — generic stereotype for Spring-managed beans (used in config classes)

**Q: How is dependency injection done?**

A: Through constructor injection (the recommended approach):

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }
}
```

Spring automatically injects the dependencies when there is a single constructor. No `@Autowired` annotation is needed.

---

## 4. JPA & Database Access

**Q: What is `@Entity` and how is it used?**

A: `@Entity` marks a POJO class as a JPA entity mapped to a database table. Each entity maps to one table, each field maps to a column:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String role;
}
```

**Q: What JPA annotations are used for mapping?**

| Annotation | Purpose | Example |
|---|---|---|
| `@Entity` | Marks class as JPA entity | `@Entity` |
| `@Table` | Specifies table name | `@Table(name = "orders")` |
| `@Id` | Marks primary key | `@Id` |
| `@GeneratedValue(strategy = IDENTITY)` | Auto-increment PK | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `@Column` | Column constraints | `@Column(unique = true, nullable = false)` |
| `@Enumerated(STRING)` | Stores enum as string | `@Enumerated(EnumType.STRING)` |
| `@PrePersist` | Callback before insert | Sets `createdAt` timestamp |

**Q: What is a Spring Data JPA Repository?**

A: `JpaRepository<T, ID>` is a built-in interface that provides CRUD operations, pagination, and sorting without implementation code. Custom query methods can be defined by naming convention:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIgnoreCase(String category);
}

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}
```

Spring Data JPA automatically implements these based on method name patterns (`findBy...`, `deleteBy...`, etc.).

**Q: How does `ddl-auto=update` work?**

A: In `application.properties`, `spring.jpa.hibernate.ddl-auto=update` tells Hibernate to automatically create or alter database tables to match entity definitions at startup. This makes development fast but is not recommended for production.

---

## 5. Spring Cloud & Microservices

**Q: What is `@EnableEurekaServer`?**

A: It turns the application into a Netflix Eureka service registry. Other services register themselves with this server so they can be discovered by name. Used in the eureka-server:

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication { ... }
```

**Q: How do services register with Eureka?**

A: By adding `spring-cloud-starter-netflix-eureka-client` to the POM and configuring the Eureka URL in `application.properties`:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

Spring Boot auto-configures the Eureka client when it finds the dependency on the classpath.

**Q: How does the API Gateway route requests?**

A: Spring Cloud Gateway uses route definitions that map request paths to destination services via Eureka load balancer (`lb://` prefix):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: USER-SERVICE
          uri: lb://USER-SERVICE
          predicates:
            - Path=/users/**
```

The `lb://` prefix tells the gateway to use Eureka service discovery and client-side load balancing.

**Q: What are Feign clients?**

A: Feign is a declarative HTTP client that makes inter-service calls look like local method calls. Instead of writing RestTemplate code, you define a Java interface:

```java
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}
```

Spring Cloud OpenFeign implements the interface at runtime, making HTTP calls to the target service via Eureka discovery.

---

## 6. Spring Cloud Gateway

**Q: How does the gateway filter work in this project?**

A: The `JwtAuthenticationFilter` implements `GlobalFilter` — a reactive gateway filter that runs on every request. It checks public routes first, then validates the JWT, extracts claims, and injects `X-User-Id` / `X-User-Role` headers:

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Check if path is public → skip auth
        // 2. Extract Bearer token from Authorization header
        // 3. Validate JWT using JwtUtil
        // 4. Inject X-User-Id and X-User-Role headers
        // 5. Forward request with added headers
    }
}
```

**Q: How is CORS configured in a reactive gateway?**

A: Using a `CorsWebFilter` bean (reactive equivalent of the servlet `CorsFilter`):

```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsWebFilter(source);
}
```

---

## 7. DTOs & Data Transfer

**Q: What are DTOs and why are they used?**

A: Data Transfer Objects are simple POJOs that carry data between layers or services. They decouple the internal entity representation from the API contract. For example, `OrderResponse` combines data from three different sources (order entity + user name + product details) into a single response object:

```java
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private String userName;      // from User Service
    private Long productId;
    private String productName;   // from Product Service
    private double productPrice;  // from Product Service
    private Integer quantity;
    private double totalPrice;
    private String status;
}
```

**Q: What is `@JsonProperty(WRITE_ONLY)` used for?**

A: It tells Jackson to only include the field when deserializing (reading JSON → Java), not when serializing (Java → JSON). Used on the User password field so passwords are accepted in registration requests but never returned in responses:

```java
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

---

## 8. Validation

**Q: How is request validation done?**

A: Using Jakarta Bean Validation (`jakarta.validation`) annotations combined with `@Valid` on controller parameters:

```java
public class AddToCartRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
```

```java
@PostMapping("/add")
public ResponseEntity<String> addToCart(@Valid @RequestBody AddToCartRequest request,
                                         @RequestHeader("X-User-Id") Long userId) { ... }
```

When validation fails, Spring automatically returns a `400 Bad Request` with details about which constraint was violated.

**Q: What validation annotations are used?**

| Annotation | Purpose |
|---|---|
| `@NotNull` | Field must not be null |
| `@Min(1)` | Number must be ≥ 1 |
| `@Positive` | Number must be positive (> 0) |

---

## 9. Exception Handling

**Q: How are exceptions handled globally?**

A: Using `@ControllerAdvice` with `@ExceptionHandler` methods. A single class catches exceptions thrown by any controller and returns structured error responses:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(), 404, "NOT_FOUND",
            ex.getMessage(), request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(), 400, "BAD_REQUEST",
            ex.getMessage(), request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
```

**Q: What is the error response structure?**

A: A consistent JSON structure across all services:

```json
{
  "timestamp": "2026-07-06T12:00:00.000",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Product not found with id: 999",
  "path": "/products/999"
}
```

**Q: What custom exception classes are used?**

| Exception | When Thrown |
|---|---|
| `ResourceNotFoundException` | Entity not found by ID |
| `BadRequestException` | Invalid input, duplicate email |
| `DownstreamServiceException` | Feign call to another service fails |
| `ResourceAccessException` | Defined in payment-service but unused |

---

## 10. Configuration & Properties

**Q: How is configuration externalized?**

A: Each service has an `application.properties` file (or `application.yml` for the gateway) in `src/main/resources/`. Common configuration:

```properties
# Server
server.port=8081

# Application name (used for Eureka registration)
spring.application.name=USER-SERVICE

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/sneakerproject
spring.datasource.username=springstudent
spring.datasource.password=springstudent
spring.jpa.hibernate.ddl-auto=update

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

**Q: What custom properties are defined?**

```properties
# Payment simulation mode (payment-service)
payment.simulation.mode=random

# Feign timeouts (cart-service, order-service, payment-service)
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=5000
```

**Q: How are custom properties injected?**

A: Using `@Value` annotation:

```java
@Value("${payment.simulation.mode}")
private String simulationMode;
```

---

## 11. Security

**Q: How is Spring Security configured in this project?**

A: Two different configurations:

**API Gateway (Reactive):**
```java
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/users/login", "/users", "/products/**")
                .permitAll()
                .anyExchange().permitAll()
            );
        return http.build();
    }
}
```

**User Service (Servlet):**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Q: Why do all services permit all requests?**

A: Authentication is delegated to the API Gateway. The gateway validates the JWT and injects `X-User-Id` / `X-User-Role` headers. Downstream services trust these headers and perform ownership checks and role checks at the controller level using `@RequestHeader` parameters.

---

## 12. JWT (JSON Web Tokens)

**Q: How are JWT tokens created?**

A: Using the jjwt library. The `JwtUtil` in the user-service generates tokens with:

```java
public String generateToken(Long id, String email, String role) {
    return Jwts.builder()
        .setSubject(email)
        .claim("userId", id)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
        .compact();
}
```

**Q: How are JWT tokens validated?**

A: The gateway's `JwtUtil` parses and validates:

```java
public Claims validateToken(String token) {
    return Jwts.parser()
        .setSigningKey(SECRET_KEY)
        .parseClaimsJws(token)
        .getBody();
}
```

If the token is expired, malformed, or has a wrong signature, an exception is thrown and the request is rejected with 401.

**Q: What information is stored in the JWT?**

| Claim | Value | Purpose |
|---|---|---|
| `sub` | User email | Subject identifier |
| `userId` | User ID (Long) | Identify the user in downstream services |
| `role` | "USER" or "ADMIN" | Role-based access control |
| `iat` | Issue timestamp | Token creation time |
| `exp` | Expiry timestamp | Token expiration (1 hour) |

---

## 13. Feign & Inter-Service Communication

**Q: How does `@FeignClient` work?**

A: Spring Cloud OpenFeign dynamically generates an HTTP client for the annotated interface. At runtime, it creates a proxy that translates each method call into an HTTP request:

```java
@FeignClient(name = "USER-SERVICE")
public interface UserClient {
    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
```

Calling `userClient.getUserById(1L)` internally makes a `GET http://USER-SERVICE/users/1` request, resolves `USER-SERVICE` via Eureka, and deserializes the response into `UserDTO`.

**Q: How are auth headers propagated to Feign calls?**

A: The `FeignConfig` class registers a `RequestInterceptor` that copies headers from the current HTTP request to outgoing Feign requests:

```java
@Bean
public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            requestTemplate.header("X-User-Id", request.getHeader("X-User-Id"));
            requestTemplate.header("X-User-Role", request.getHeader("X-User-Role"));
        }
    };
}
```

This ensures the auth context (`X-User-Id`, `X-User-Role`) flows through the entire chain: Gateway → Order Service → Product/User Service.

**Q: What is a Feign ErrorDecoder?**

A: A custom `ErrorDecoder` maps HTTP error responses from downstream services to domain-specific exceptions:

```java
public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new BadRequestException("Bad request from downstream service");
            case 404 -> new ResourceNotFoundException("Resource not found in downstream service");
            default -> new DownstreamServiceException("Downstream service error");
        };
    }
}
```

---

## 14. Lombok

**Q: What is Lombok and how is it used?**

A: Lombok is a library that generates boilerplate code (getters, setters, constructors, builders) via annotations. The gateway uses `@Data` on DTOs:

```java
@Data
public class LoginRequest {
    private String email;
    private String password;
}
```

`@Data` generates `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, and `@RequiredArgsConstructor`.

**Q: Why is Lombok only used in the gateway?**

A: The project was built incrementally — Lombok usage is inconsistent. The gateway uses it, but the other services (user, product, order, cart, payment) write constructors, getters, and setters manually. Both approaches work; Lombok reduces boilerplate but adds a compile-time dependency.

---

## 15. OpenAPI / Swagger

**Q: How is API documentation set up?**

A: Using SpringDoc OpenAPI (`springdoc-openapi-starter-webflux-ui` for the reactive gateway, standard version for servlet services). The gateway aggregates docs from all services:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: USER-DOCS
          uri: lb://USER-SERVICE
          predicates:
            - Path=/users/v3/api-docs
          filters:
            - RewritePath=/users/v3/api-docs, /v3/api-docs
```

**Q: Where is Swagger UI accessible?**

A: At the gateway: `http://localhost:8080/swagger-ui.html`. It aggregates OpenAPI specs from all registered services, letting you browse and test all endpoints from one page.

---

## 16. Enums

**Q: How are enums used in entities?**

A: The payment service uses an enum for payment status, stored as a string in the database:

```java
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}
```

```java
@Entity
public class Payment {
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
```

`@Enumerated(EnumType.STRING)` stores the enum name (`"SUCCESS"`) rather than its ordinal position (`0`), which is safer for data integrity (adding new enum values won't shift existing data).

---

## 17. Maven & Build Configuration

**Q: What is the Maven POM structure?**

A: Each service has its own `pom.xml` with:
- A parent POM (either `spring-boot-starter-parent` or a custom parent)
- Dependencies specific to the service
- `spring-boot-maven-plugin` for building executable JARs

**Q: What are the key dependencies shared across services?**

| Dependency | Used By |
|---|---|
| `spring-boot-starter-web` | All business services (REST APIs) |
| `spring-boot-starter-data-jpa` | All business services (database access) |
| `spring-cloud-starter-netflix-eureka-client` | All services except eureka-server |
| `spring-boot-starter-security` | User service, API gateway |
| `spring-cloud-starter-gateway` | API gateway only |
| `spring-cloud-starter-openfeign` | Order, cart, payment services |
| `springdoc-openapi-starter-webmvc-ui` | Business services (REST docs) |
| `springdoc-openapi-starter-webflux-ui` | API gateway (reactive docs) |
| `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | User service, API gateway (JWT) |
| `lombok` | API gateway (boilerplate reduction) |
| `mysql-connector-j` | All business services (MySQL driver) |

**Q: How is the Spring Cloud BOM used?**

A: A BOM (Bill of Materials) in the POM's `<dependencyManagement>` section ensures compatible versions of all Spring Cloud dependencies:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 18. Actuator

**Q: What is Spring Boot Actuator?**

A: Actuator adds production-ready endpoints for monitoring and managing the application (health checks, metrics, environment info, etc.). The API gateway includes it:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Actuator endpoints (like `/actuator/health`) are automatically exposed and can be used by monitoring tools or container orchestrators for health checks.
