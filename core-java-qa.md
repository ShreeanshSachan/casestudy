# Core Java Q&A — Interview Prep

## 1. OOP Concepts

**Q: What are the four pillars of OOP? Give examples from this project.**

A:

**Encapsulation** — Hiding internal state and exposing only necessary methods.

```java
@Entity
public class User {
    private Long id;
    private String name;
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

Fields are `private`. Access is controlled via `public` getters/setters. The password field has `WRITE_ONLY` access — it can be set during registration but is never exposed in JSON responses.

**Inheritance** — A class deriving from another to reuse or extend behavior.

```java
// UserRepository inherits findAll(), save(), findById() from JpaRepository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

**Polymorphism** — Same method name behaving differently based on object type.

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) { ... }

@ExceptionHandler(BadRequestException.class)
public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) { ... }
```

**Abstraction** — Hiding implementation complexity behind a simple interface.

```java
// Controller calls this — doesn't need to know about database queries
@Service
public class ProductService {
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
```

---

**Q: What is the difference between an abstract class and an interface?**

A: An `abstract class` can have both concrete and abstract methods, constructors, and instance variables. A class can extend only one. An interface can only have abstract methods (pre-Java 8) or default/static methods (Java 8+), and a class can implement multiple.

Spring uses interfaces for repositories and Feign clients because it generates implementations at runtime:

```java
// Interface — Spring provides the implementation
public interface UserRepository extends JpaRepository<User, Long> { }
```

---

## 2. Access Modifiers

**Q: Explain the access modifiers with examples.**

A:

| Modifier | Same Class | Same Package | Subclass | Everywhere |
|---|---|---|---|---|
| `private` | ✓ | ✗ | ✗ | ✗ |
| default | ✓ | ✓ | ✗ | ✗ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| `public` | ✓ | ✓ | ✓ | ✓ |

```java
public class CartService {
    private final CartRepository cartRepository;       // private: only this class
    private final ProductClient productClient;

    public CartResponse getCart(Long userId) { ... }   // public: API contract

    private void recalculateTotal(Cart cart) { ... }   // private: internal helper
}
```

---

**Q: What is the difference between `public` and `protected`?**

A: `protected` is accessible in the same package and subclasses (even in different packages). `public` is accessible from anywhere.

---

## 3. Static Keyword

**Q: What does `static` mean in Java?**

A: `static` members belong to the class itself, not to instances. All instances share the same static variable.

```java
public class JwtUtil {
    private static final String SECRET_KEY = "secretkeysecretkeysecretkeysecretkey";
    private static final long EXPIRATION = 1000 * 60 * 60;  // 1 hour

    public String generateToken(Long id, String email, String role) {
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", id)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact();
    }
}
```

`static final` fields are constants — they are initialized once and never change.

---

**Q: Can a static method access instance variables?**

A: No. A static method cannot access `this` or instance fields directly because it doesn't belong to any instance:

```java
private String name;  // instance variable

public static void print() {
    System.out.println(name);  // COMPILE ERROR: Cannot make a static reference to non-static field
}
```

---

## 4. final Keyword

**Q: What does `final` mean in three contexts?**

A:

**Final variable** — Cannot be reassigned after initialization:

```java
private final CartRepository cartRepository;  // must be set in constructor, never changed
```

**Final method** — Cannot be overridden by subclasses.

**Final class** — Cannot be extended (e.g., `String` class is final).

---

## 5. String Handling

**Q: What is the difference between `String`, `StringBuilder`, and `StringBuffer`?**

A:

| Type | Mutable | Thread-safe | Performance |
|---|---|---|---|
| `String` | No (immutable) | Yes (by design) | Slow for many concatenations |
| `StringBuilder` | Yes | No | Fast |
| `StringBuffer` | Yes | Yes (synchronized) | Slower than StringBuilder |

```java
// String — creates new object each time
String s = "Hello";
s = s + " World";  // new String object created

// StringBuilder — modifies in place
StringBuilder sb = new StringBuilder("Hello");
sb.append(" World");  // same object
```

In this project, simple `+` concatenation is used because strings are short:

```java
return "Product not found with id: " + id;
return api.get<Product>("/products/" + id);
```

---

**Q: What is the difference between `==` and `.equals()` for Strings?**

A: `==` compares **references** (memory addresses). `.equals()` compares **values** (characters).

```java
String a = new String("hello");
String b = new String("hello");

System.out.println(a == b);        // false (different objects)
System.out.println(a.equals(b));   // true (same characters)
```

---

## 6. Wrapper Classes & Autoboxing

**Q: What are wrapper classes and why are they needed?**

A: Wrapper classes (`Integer`, `Long`, `Double`, `Boolean`, etc.) wrap primitives into objects. Primitives cannot be used with generics, collections, or JPA.

```java
// Entity uses Long (wrapper), not long (primitive)
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;       // Long, not long

    private double price;  // primitive is fine for simple fields
}
```

**Autoboxing** — automatic conversion between primitive and wrapper:

```java
Long id = 1L;           // autoboxing: long → Long
long primitive = id;    // unboxing: Long → long
```

---

## 7. Exception Handling

**Q: What is the difference between checked and unchecked exceptions?**

A: **Checked** exceptions (`Exception` subclasses except `RuntimeException`) must be handled with `try/catch` or declared with `throws`. **Unchecked** exceptions (`RuntimeException` subclasses) do not require explicit handling.

This project uses only unchecked exceptions:

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

---

**Q: How does `try-catch-finally` work?**

A:

```java
try {
    // risky code
    String payload = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
    // ...
} catch (Exception e) {
    // runs only if exception occurs in try block
    return false;
}
```

The `finally` block (not shown) would run regardless of exception — used for cleanup like closing streams.

---

## 8. Collections

**Q: What is the difference between `List`, `Set`, and `Map`?**

A:

| Interface | Ordered | Allows Duplicates | Key-Value |
|---|---|---|---|
| `List` | Yes (insertion order) | Yes | No |
| `Set` | No (unless `LinkedHashSet`/`TreeSet`) | No | No |
| `Map` | No | Keys: No, Values: Yes | Yes |

Usage in this project:

```java
// List — ordered, allows duplicates (returns multiple products)
List<Product> getAllProducts()

// Optional — container that may/may not hold a value
Optional<User> findByEmail(String email)
```

---

**Q: What is `Optional` and why use it?**

A: `Optional<T>` is a container that may or may not contain a value. It forces callers to handle the "no value" case explicitly, preventing `NullPointerException`:

```java
// Instead of returning null:
public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
}
```

Common `Optional` methods:
- `orElseThrow()` — return value or throw exception
- `orElse(defaultValue)` — return value or fallback
- `ifPresent(value -> doSomething())` — execute if value exists
- `isPresent()` — check if value exists

---

## 9. Generics

**Q: What are generics and why are they useful?**

A: Generics enable type-safe parameterization of classes, interfaces, and methods. They catch type errors at compile time instead of runtime.

```java
// Without generics — unsafe, needs casting
List rawList = new ArrayList();
rawList.add("hello");
Integer num = (Integer) rawList.get(0);  // ClassCastException at runtime

// With generics — safe, no cast needed
List<String> list = new ArrayList<>();
list.add("hello");
String str = list.get(0);  // compiler guarantees type safety

// JpaRepository uses generics for entity type and ID type
public interface UserRepository extends JpaRepository<User, Long> { }
//                                                     Entity   ID
```

---

## 10. equals() and hashCode()

**Q: What is the contract between `equals()` and `hashCode()`?**

A: If two objects are equal (`.equals()` returns `true`), they **must** have the same hash code. The reverse is not required (different objects can have the same hash code). This matters when objects are used in hash-based collections (`HashSet`, `HashMap`).

```java
Product p1 = new Product(); p1.setId(1L);
Product p2 = new Product(); p2.setId(1L);

// Without proper override:
p1.equals(p2);           // false (default Object.equals compares references)
p1.hashCode() == p2.hashCode();  // false

// Correct implementation:
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Product)) return false;
    Product product = (Product) o;
    return id != null && Objects.equals(id, product.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();  // constant to avoid lazy-loading issues
}
```

---

## 11. Enums

**Q: What is an enum and how is it used?**

A: An enum defines a fixed set of named constants. Java enums are more powerful than in other languages — they can have fields, methods, and constructors.

```java
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}

// Usage in entity:
@Enumerated(EnumType.STRING)
private PaymentStatus status;
```

`EnumType.STRING` stores the name (`"SUCCESS"`) in the database. `EnumType.ORDINAL` stores the position (0, 1, 2). `STRING` is safer — it won't break if enum order changes.

---

## 12. Annotations

**Q: What are annotations and how are they processed?**

A: Annotations are metadata tags starting with `@`. They can be processed at compile time (Lombok) or runtime (Spring via reflection).

```java
@Override  // Compile-time: ensures method actually overrides a parent method

@GetMapping("/products")  // Runtime: Spring reads this and registers the route
public List<Product> getAllProducts() { ... }
```

Spring uses runtime annotation processing to configure dependency injection, HTTP routing, transaction management, and more.

---

## 13. Records (Java 16+)

**Q: What is a Java Record?**

A: A record is a concise way to create immutable data carriers. It automatically generates constructor, getters, `equals()`, `hashCode()`, and `toString()`:

```java
// Instead of writing a full class with private fields, getters, constructors:
public record LoginRequest(String email, String password) { }

// Usage:
var request = new LoginRequest("test@email.com", "password123");
request.email();     // accessor (not getEmail())
request.password();  // accessor
```

This project doesn't use records, but DTOs like `LoginRequest` and `ErrorResponse` would be good candidates.

---

## 14. this Keyword

**Q: What does `this` refer to?**

A: `this` refers to the current instance of a class. It is used to:
1. Distinguish instance variables from parameters with the same name
2. Call another constructor from the same class

```java
public class User {
    private String name;
    private String email;

    public User(String name, String email) {
        this.name = name;    // this.name = instance variable, name = parameter
        this.email = email;
    }

    public User() {
        this("Default", "default@email.com");  // calls the other constructor
    }
}
```

---

## 15. super Keyword

**Q: What does `super` refer to?**

A: `super` refers to the parent class. It is used to:
1. Call parent class constructor
2. Access parent class methods/fields that are overridden/hidden in child

```java
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);  // calls RuntimeException(String) constructor
    }
}
```

---

## 16. Constructors

**Q: What is a constructor and what types exist?**

A: A constructor initializes a new object. It has the same name as the class and no return type.

```java
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    // Constructor injection — Spring calls this automatically
    public CartService(CartRepository cartRepository, ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.productClient = productClient;
    }
}
```

Types:
- **Default constructor** — provided by Java if no constructor is defined
- **Parameterized constructor** — accepts arguments (like above)
- **Copy constructor** — creates a new object from an existing one (not used in this project)

---

## 17. Loops & Control Flow

**Q: What loop types are used in this project?**

A:

**Enhanced for-each loop** (most common):

```java
for (CartItem item : this.cart.getItems()) {
    // process each item
}
```

**Traditional for loop** (not used in this project):

```java
for (int i = 0; i < items.size(); i++) {
    process(items.get(i));
}
```

**While loop** (not used in this project):

```java
while (condition) {
    // repeat until condition is false
}
```

---

## 18. Arrays

**Q: How are arrays different from ArrayList?**

A:

| Feature | Array | ArrayList |
|---|---|---|
| Fixed size | Yes | No (grows dynamically) |
| Primitives | Allowed | Not allowed (needs wrapper) |
| Performance | Faster | Slightly slower |
| Generics | Not supported | Supported |

```java
// Array — fixed size, can hold primitives
int[] numbers = new int[5];
Product[] products = new Product[10];

// ArrayList — dynamic, only objects
List<Product> productList = new ArrayList<>();
productList.add(new Product());
```

The project uses `List` (specifically `ArrayList`) exclusively over arrays because of dynamic sizing and generics support.

---

## 19. instanceof

**Q: What is `instanceof` used for?**

A: It checks whether an object is an instance of a specific class or interface:

```java
if (ex instanceof ResourceNotFoundException) {
    // handle not found
} else if (ex instanceof BadRequestException) {
    // handle bad request
}
```

The `@ExceptionHandler` in this project handles this more elegantly through method dispatch, but internally Spring uses `instanceof`-like checks to match exceptions to handlers.

---

## 20. I/O Streams

**Q: What is the difference between byte streams and character streams?**

A: **Byte streams** (`InputStream`, `OutputStream`) read/write raw bytes. **Character streams** (`Reader`, `Writer`) read/write characters, handling encoding automatically.

Not used directly in this project — Spring Boot abstracts file handling. But the JWT library internally uses byte streams for encoding:

```java
// Internal JWT library usage (simplified):
byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
```
