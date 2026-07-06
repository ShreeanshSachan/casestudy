# Java Collections & Java 8 Q&A — Interview Prep

## PART 1: Java Collections Framework

## 1. Collections Overview

**Q: What is the Java Collections Framework?**

A: It is a unified architecture for storing and manipulating groups of objects. It consists of interfaces (`List`, `Set`, `Map`, `Queue`), implementations (`ArrayList`, `HashSet`, `HashMap`, etc.), and utility classes (`Collections`, `Arrays`).

```
Collection (interface)
    ├── List (ordered, allows duplicates)
    │   ├── ArrayList
    │   └── LinkedList
    ├── Set (no duplicates)
    │   ├── HashSet
    │   ├── LinkedHashSet
    │   └── TreeSet
    └── Queue (FIFO)
        └── LinkedList

Map (interface) — key-value pairs, no duplicate keys
    ├── HashMap
    ├── LinkedHashMap
    └── TreeMap
```

**Q: What collections are used in this project?**

A: Primarily `List` (via `ArrayList`) and `Optional`:

```java
// List — returned from repository queries
public List<Product> getAllProducts() {
    return productRepository.findAll();  // returns List<Product>
}

public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll().stream()
        .map(this::enrichOrderResponse)
        .collect(Collectors.toList());  // returns List<OrderResponse>
}
```

---

## 2. List Interface

**Q: What is `List` and how does it differ from `Set`?**

A: `List` is an ordered collection that **allows duplicates**. Elements can be accessed by index. `Set` is an unordered collection that **rejects duplicates**.

```java
List<String> list = new ArrayList<>();
list.add("A");
list.add("B");
list.add("A");       // allowed — list now has [A, B, A]
System.out.println(list.get(0));  // "A" — index-based access

Set<String> set = new HashSet<>();
set.add("A");
set.add("B");
set.add("A");        // ignored — set only has [A, B]
// No get(index) — sets don't support positional access
```

---

**Q: What is the difference between `ArrayList` and `LinkedList`?**

A:

| Feature | ArrayList | LinkedList |
|---|---|---|
| Internal Structure | Dynamic array | Doubly-linked list |
| Get by index | O(1) | O(n) |
| Add at end | O(1)* (amortized) | O(1) |
| Insert/delete middle | O(n) (shifts elements) | O(n) (traverses to position) |
| Memory | Less (only stores data) | More (stores prev/next pointers) |
| Implements | List, RandomAccess | List, Deque, Queue |

Usage rule: Use `ArrayList` for most cases (fast random access). Use `LinkedList` when you frequently insert/delete at the beginning or need a queue/stack.

In this project, all lists are `ArrayList` (returned by `findAll()` and `collect(Collectors.toList())`).

---

## 3. Set Interface

**Q: What is `HashSet` and how does it ensure uniqueness?**

A: `HashSet` uses a hash table internally. It checks duplicates using `hashCode()` first, then `equals()`:

```java
Set<Long> productIds = new HashSet<>();
productIds.add(1L);
productIds.add(2L);
productIds.add(1L);  // ignored — 1L is already present
```

The Cart entity enforces uniqueness of `(cartId, productId)` pairs via a database unique constraint:

```java
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cart_id", "product_id"})
})
public class CartItem { ... }
```

---

**Q: What is the difference between `HashSet`, `LinkedHashSet`, and `TreeSet`?**

A:

| Feature | HashSet | LinkedHashSet | TreeSet |
|---|---|---|---|
| Ordering | No guaranteed order | Insertion order | Sorted (Comparable/Comparator) |
| Performance | O(1) | O(1) | O(log n) |
| Underlying data structure | HashMap | LinkedHashMap | TreeMap (Red-Black tree) |

---

## 4. Map Interface

**Q: What is a `Map` and when would you use `HashMap` vs `TreeMap`?**

A: A `Map` stores key-value pairs. Each key maps to exactly one value, and duplicate keys are not allowed.

```java
// HashMap — O(1) operations, no ordering
Map<Long, String> productNames = new HashMap<>();
productNames.put(1L, "Air Max 270");
productNames.put(2L, "Air Force 1");
String name = productNames.get(1L);  // "Air Max 270"

// TreeMap — O(log n), keys are sorted
Map<Long, String> sorted = new TreeMap<>();
sorted.put(2L, "B");
sorted.put(1L, "A");
// Iteration order: {1=A, 2=B} (sorted by key)
```

A hypothetical use in this project — caching product details in the cart service:

```java
// If we wanted to avoid repeated Feign calls for the same product
private final Map<Long, ProductDTO> productCache = new HashMap<>();
```

---

## 5. Queue Interface

**Q: What is a `Queue` and when would you use it?**

A: A `Queue` follows FIFO (First-In-First-Out). Not used in this project, but `LinkedList` implements both `List` and `Deque`:

```java
Queue<String> queue = new LinkedList<>();
queue.offer("first");    // add to tail
queue.offer("second");
String head = queue.poll();  // "first" — removes and returns head
```

---

## 6. Utility Classes — Collections & Arrays

**Q: What static methods does `Collections` provide?**

A: `Collections` is a utility class with helper methods:

```java
List<Product> list = productRepository.findAll();

Collections.sort(list, Comparator.comparing(Product::getName));  // sort
Collections.reverse(list);       // reverse order
Collections.shuffle(list);       // randomize
Collections.emptyList();         // return immutable empty list
Collections.unmodifiableList(list);  // make read-only
```

Not used explicitly in this project since sorting is typically done at the database level via JPA queries.

---

**Q: What about `Arrays` utility class?**

A: `Arrays` provides methods for array manipulation:

```java
int[] numbers = {3, 1, 4, 1, 5};
Arrays.sort(numbers);            // {1, 1, 3, 4, 5}
int index = Arrays.binarySearch(numbers, 4);  // 3
String str = Arrays.toString(numbers);        // "[1, 1, 3, 4, 5]"
List<Integer> list = Arrays.asList(1, 2, 3);  // convert array to fixed-size list
```

---

## 7. Comparable vs Comparator

**Q: What is the difference between `Comparable` and `Comparator`?**

A:

| Feature | Comparable | Comparator |
|---|---|---|
| Package | `java.lang` | `java.util` |
| Method | `compareTo(T o)` | `compare(T o1, T o2)` |
| Used for | Natural ordering | Custom ordering |
| Target class | Must be modified | Separate class or lambda |

**Comparable** — defines natural ordering within the class:

```java
public class Product implements Comparable<Product> {
    private String name;

    @Override
    public int compareTo(Product other) {
        return this.name.compareTo(other.name);  // alphabetical ordering
    }
}

Collections.sort(products);  // uses compareTo()
```

**Comparator** — external, multiple strategies:

```java
// Sort by name alphabetically
Comparator<Product> byName = Comparator.comparing(Product::getName);
products.sort(byName);

// Sort by price descending
Comparator<Product> byPriceDesc = Comparator.comparing(Product::getPrice).reversed();
products.sort(byPriceDesc);

// Sort by category then by price
Comparator<Product> byCategoryThenPrice = Comparator
    .comparing(Product::getCategory)
    .thenComparing(Product::getPrice);
products.sort(byCategoryThenPrice);
```

Not used explicitly in this project since sorting is done at the database level.

---

## 8. Iterator & for-each

**Q: How do you iterate over a collection?**

A: Three ways:

```java
List<Product> products = productRepository.findAll();

// 1. For-each loop (enhanced for) — preferred for reading
for (Product p : products) {
    System.out.println(p.getName());
}

// 2. Stream API (Java 8) — for transformations
products.stream()
    .filter(p -> p.getPrice() > 100)
    .forEach(p -> System.out.println(p.getName()));

// 3. Explicit Iterator — needed when removing during iteration
Iterator<Product> it = products.iterator();
while (it.hasNext()) {
    Product p = it.next();
    if (p.getPrice() < 50) {
        it.remove();  // safe removal
    }
}
```

---

**Q: Why can't you remove elements from a collection in a for-each loop?**

A: Modifying a collection while iterating causes `ConcurrentModificationException`:

```java
List<Product> products = new ArrayList<>(productRepository.findAll());

// WRONG — ConcurrentModificationException
for (Product p : products) {
    if (p.getPrice() < 50) {
        products.remove(p);  // throws ConcurrentModificationException
    }
}

// CORRECT — use Iterator.remove()
Iterator<Product> it = products.iterator();
while (it.hasNext()) {
    Product p = it.next();
    if (p.getPrice() < 50) {
        it.remove();
    }
}
```

The in-memory `List` has a `modCount` field. When an iterator is created, it captures `modCount`. If the list is structurally modified outside the iterator after that, the iterator detects the mismatch and throws the exception.

---

## 9. ArrayList Internal Implementation

**Q: How does `ArrayList` grow dynamically?**

A: `ArrayList` internally uses an `Object[]` array. When the array is full, it creates a **new array 1.5 times the size** and copies all elements:

```java
// Simplified internal logic:
private Object[] elementData;
private int size;

public boolean add(E e) {
    if (size == elementData.length) {
        grow();  // create new array: int newCapacity = oldCapacity + (oldCapacity >> 1)
    }
    elementData[size++] = e;
}
```

The default initial capacity is 10. Capacity increases by 50% each time it overflows.

---

## 10. HashMap Internal Implementation

**Q: How does `HashMap` work internally?**

A: `HashMap` stores key-value pairs in an array of buckets. Each bucket is a linked list or tree (Java 8+):

```
Array of buckets:
[0]  null
[1]  Node(key1, hash1, value1) → Node(key2, hash2, value2)  // linked list
[2]  TreeNode(key3, hash3, value3) → ...                     // tree (if bucket > 8)
```

**Put operation:**
1. Compute hash: `hash = key.hashCode() ^ (key.hashCode() >>> 16)`
2. Find bucket: `index = (n - 1) & hash`
3. If bucket is empty → place new Node
4. If bucket has entries → traverse using `equals()` to find key:
   - If key exists → replace value
   - If not → add new Node at end
5. If bucket size exceeds `TREEIFY_THRESHOLD (8)` → convert linked list to tree

**Get operation:**
1. Compute hash and bucket index
2. If bucket is null → return null
3. Check first node's hash and key equality → if match, return value
4. If node is TreeNode → search tree (O(log n))
5. If node is linked list → traverse (O(n))

Default initial capacity: **16** buckets. Default load factor: **0.75**.

---

**Q: What happens when the load factor is exceeded?**

A: Rehashing — the array doubles in size (to 32), and all entries are re-indexed: `newIndex = (newCapacity - 1) & hash`. This is an O(n) operation.

---

**Q: What makes a good hash key?**

A: Immutable objects with proper `equals()` and `hashCode()`. `String`, `Integer`, `Long` are ideal. If you use a custom class as a map key, you must override both methods correctly.

---

## 11. Fail-Fast vs Fail-Safe Iterators

**Q: What is the difference between fail-fast and fail-safe iterators?**

A:

**Fail-fast** (used by `ArrayList`, `HashMap`, `HashSet`): Throws `ConcurrentModificationException` if the collection is structurally modified during iteration. Works by checking an internal `modCount` counter.

**Fail-safe** (used by `ConcurrentHashMap`, `CopyOnWriteArrayList`): Iterates over a snapshot of the collection. Modifications during iteration do not throw exceptions, but the iterator may not reflect the latest changes.

```java
// Fail-fast — throws if modified during iteration
List<String> list = new ArrayList<>();
for (String s : list) {
    list.remove(s);  // ConcurrentModificationException
}

// Fail-safe — works on snapshot
CopyOnWriteArrayList<String> safe = new CopyOnWriteArrayList<>();
for (String s : safe) {
    safe.remove(s);  // no exception, but changes aren't visible to iterator
}
```

---

## PART 2: Java 8 Features

## 12. Lambda Expressions

**Q: What is a lambda expression?**

A: A lambda is a concise way to implement a **functional interface** (an interface with a single abstract method). Syntax: `(parameters) -> expression` or `(parameters) -> { statements; }`.

```java
// Anonymous inner class (pre-Java 8):
Comparator<Product> byName = new Comparator<Product>() {
    @Override
    public int compare(Product a, Product b) {
        return a.getName().compareTo(b.getName());
    }
};

// Lambda (Java 8+):
Comparator<Product> byName = (a, b) -> a.getName().compareTo(b.getName());

// Even shorter:
Comparator<Product> byName = Comparator.comparing(Product::getName);
```

---

**Q: Where are lambdas used in this project?**

A: In the Feign configuration and authentication filter:

```java
// Gateway filter — lambda as GlobalFilter implementation
@Bean
public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            requestTemplate.header("X-User-Id",
                attrs.getRequest().getHeader("X-User-Id"));
        }
    };
}
```

And in Stream operations:

```java
payments.stream()
    .map(payment -> mapToDTO(payment))   // lambda
    .collect(Collectors.toList());
```

---

## 13. Functional Interfaces

**Q: What are the four main functional interfaces in `java.util.function`?**

A:

| Interface | Method | Signature | Use Case |
|---|---|---|---|
| `Predicate<T>` | `test(T)` | `T → boolean` | Filtering |
| `Function<T,R>` | `apply(T)` | `T → R` | Transformation |
| `Consumer<T>` | `accept(T)` | `T → void` | Side effects (print, save) |
| `Supplier<T>` | `get()` | `() → T` | Lazy generation |

```java
// Predicate — test a condition
Predicate<Product> isExpensive = p -> p.getPrice() > 200;

// Function — transform input to output
Function<Product, String> getName = p -> p.getName();

// Consumer — perform action without returning
Consumer<Product> printName = p -> System.out.println(p.getName());

// Supplier — provide a value on demand
Supplier<Long> generateId = () -> System.currentTimeMillis();
```

Usage in this project:

```java
// Stream's filter() takes a Predicate
expensiveProducts = products.stream()
    .filter(p -> p.getPrice() > 200)   // Predicate
    .collect(Collectors.toList());

// Stream's map() takes a Function
productNames = products.stream()
    .map(Product::getName)              // Function (method reference)
    .collect(Collectors.toList());

// Stream's forEach() takes a Consumer
products.forEach(p -> log.info(p.getName()));  // Consumer

// orElseThrow() takes a Supplier
userRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Not found"));  // Supplier
```

---

## 14. Method References

**Q: What are the four types of method references?**

A:

| Type | Syntax | Example |
|---|---|---|
| Static method | `Class::staticMethod` | `Integer::parseInt` |
| Instance method of specific object | `object::instanceMethod` | `this::mapToDTO` |
| Instance method of any object of a type | `Class::instanceMethod` | `String::toLowerCase` |
| Constructor | `Class::new` | `ArrayList::new` |

```java
// Lambda → Method reference
.map(payment -> this.mapToDTO(payment))   // lambda
.map(this::mapToDTO)                       // method reference (instance of specific object)

products.stream()
    .map(Product::getName)                 // method reference (instance of any Product)
    .collect(Collectors.toList());
```

---

## 15. Stream API

**Q: What is a Stream and how does it differ from a Collection?**

A: A Stream is a sequence of elements that supports functional-style operations. Key differences:

| Feature | Collection | Stream |
|---|---|---|
| Storage | Stores elements | Processes elements (no storage) |
| Modification | Add/remove elements | Read-only pipeline |
| Traversal | Can be traversed multiple times | Can be consumed once |
| Evaluation | Eager (always in memory) | Lazy (intermediate ops deferred) |
| Thread safety | External synchronization needed | Can be parallel |

```java
// Collection — stores data
List<Product> products = productRepository.findAll();

// Stream — processes data
List<String> expensiveBrands = products.stream()           // create stream
    .filter(p -> p.getPrice() > 100)                        // intermediate (lazy)
    .map(Product::getBrand)                                 // intermediate (lazy)
    .distinct()                                             // intermediate (lazy)
    .sorted()                                               // intermediate (lazy)
    .collect(Collectors.toList());                          // terminal (eager)
```

---

**Q: What is the difference between intermediate and terminal operations?**

A: **Intermediate** operations return a new Stream and are **lazy** — they don't execute until a terminal operation is called. **Terminal** operations produce a result or side effect and **close the stream**.

```java
// Intermediate (lazy — nothing happens until collect() is called):
stream.filter(p -> p.getPrice() > 100)
      .map(Product::getName)
      .sorted()

// Terminal (triggers execution):
.collect(Collectors.toList());     // produces List
.forEach(System.out::println);     // produces side effect
.count();                          // produces long
.findFirst();                      // produces Optional
.anyMatch(p -> p.getPrice() > 500); // produces boolean
```

---

**Q: What Stream operations are used in this project?**

A:

```java
// map() — transform each element
public List<PaymentResponse> getPaymentsByUser(Long userId) {
    return paymentRepository.findByUserId(userId).stream()
        .map(this::mapToDTO)               // Payment → PaymentResponse
        .collect(Collectors.toList());
}

// filter() — select elements matching condition (not used, but would be):
List<Product> expensive = products.stream()
    .filter(p -> p.getPrice() > 200)
    .collect(Collectors.toList());

// collect() — gather results into a List, Set, or Map
.collect(Collectors.toList());
.collect(Collectors.toSet());
.collect(Collectors.groupingBy(Product::getCategory));  // Map<String, List<Product>>
```

---

## 16. Optional

**Q: What is `Optional<T>` and why was it introduced?**

A: `Optional<T>` is a container that may or may not contain a non-null value. It was introduced to **force callers to handle the absent case**, eliminating `NullPointerException` caused by unchecked null returns.

```java
// Spring Data JPA returns Optional from findById():
Optional<User> optUser = userRepository.findById(1L);

// Good — handles absence explicitly
User user = optUser.orElseThrow(
    () -> new ResourceNotFoundException("User not found")
);

// Also good — provides default
User defaultUser = optUser.orElse(new User("Guest", "guest@email.com", "GUEST"));

// Also good — only executes if present
optUser.ifPresent(u -> log.info("Found user: " + u.getName()));

// BAD — calling get() without checking
User user = optUser.get();  // throws NoSuchElementException if absent
```

---

**Q: What methods does `Optional` provide?**

A:

| Method | Returns | Description |
|---|---|---|
| `isPresent()` | `boolean` | True if value exists |
| `isEmpty()` (Java 11+) | `boolean` | True if empty |
| `get()` | `T` | Returns value or throws `NoSuchElementException` |
| `orElse(T other)` | `T` | Returns value or default |
| `orElseGet(Supplier)` | `T` | Returns value or lazily supplied default |
| `orElseThrow(Supplier)` | `T` | Returns value or throws custom exception |
| `ifPresent(Consumer)` | `void` | Execute action if value exists |
| `map(Function)` | `Optional<U>` | Transform value if present |
| `filter(Predicate)` | `Optional<T>` | Filter value if present |
| `flatMap(Function)` | `Optional<U>` | Transform that returns Optional |

```java
// Chain multiple Optional operations:
String city = userRepository.findById(1L)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

---

**Q: Should all methods return `Optional`?**

A: No. Rules of thumb:
- Return `Optional` for **single-value queries** that may return no result (`findById`, `findByEmail`)
- Do **NOT** return `Optional` for: collections (return empty `List` instead), primitive wrappers, or fields in entities/DTOs

```java
// GOOD
Optional<User> findById(Long id);     // might not exist
Optional<User> findByEmail(String email);

// BAD — return empty list instead
Optional<List<Product>> findByCategory(String cat);  // wrong
List<Product> findByCategory(String cat);            // right — returns empty list
```

---

## 17. Stream vs for-loop

**Q: When would you use a Stream instead of a traditional for-loop?**

A:

Use **Stream** for:
- Declarative, readable pipeline of operations
- Complex transformations (filter → map → sort → collect)
- Parallel processing (`parallelStream()`)
- Lazy evaluation

Use **for-loop** for:
- Simple iteration with side effects
- Performance-critical code (avoid stream overhead)
- Index-based access
- Early termination with complex conditions

This project uses both:

```java
// Stream — transforming a list (declarative, readable)
public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll().stream()
        .map(this::enrichOrderResponse)
        .collect(Collectors.toList());
}

// for-loop — iterating with side effects and multiple API calls
for (CartItem item : cart.getItems()) {
    orderService.createOrder(item.getProductId(), item.getQuantity())
        .subscribe(order -> orderIds.add(order.getOrderId()));
}
```

---

## 18. Parallel Streams

**Q: How does `parallelStream()` work?**

A: It splits the stream into multiple substreams, processes them concurrently using the Fork-Join pool, and combines the results. The number of threads equals `Runtime.getRuntime().availableProcessors()`.

```java
// Sequential — one thread, one element at a time
products.stream()
    .filter(p -> expensiveCheck(p))
    .forEach(this::process);

// Parallel — multiple threads
products.parallelStream()
    .filter(p -> expensiveCheck(p))
    .forEach(this::process);
```

**Caution:** Parallel streams can cause thread-safety issues if the operations modify shared mutable state. They should only be used for CPU-intensive, independent operations. This project does not use `parallelStream()`.

---

## 19. Collectors

**Q: What are common `Collectors` methods?**

A:

```java
// Collect into a List
.collect(Collectors.toList());
.collect(Collectors.toUnmodifiableList());  // Java 10+, immutable

// Collect into a Set
.collect(Collectors.toSet());

// Collect into a Map (with key and value mappers)
.collect(Collectors.toMap(
    Product::getId,          // key: product ID
    Function.identity()      // value: product itself
));

// Group by a field
Map<String, List<Product>> byCategory = products.stream()
    .collect(Collectors.groupingBy(Product::getCategory));
// Result: { "Running": [...], "Casual": [...] }

// Join strings
String names = products.stream()
    .map(Product::getName)
    .collect(Collectors.joining(", "));
// "Air Max 270, Air Force 1, ...

// Counting
Map<String, Long> countByCategory = products.stream()
    .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
```

This project uses only `Collectors.toList()`:

```java
.collect(Collectors.toList());
```

---

## 20. Default Methods in Interfaces

**Q: What is a default method in an interface?**

A: A method with a body defined in an interface using the `default` keyword. It allows adding new methods to interfaces without breaking existing implementations.

```java
public interface ProductService {
    List<Product> getAllProducts();

    // default method — existing implementations don't need to override
    default Product getProductById(Long id) {
        return getAllProducts().stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Not found"));
    }
}
```

Spring's `JpaRepository` inherits default methods like `findAll()`, `findById()`, `save()`, etc. from parent interfaces.

---

## 21. forEach vs for-each

**Q: What is the difference between `Collection.forEach()` and the enhanced for-each loop?**

A:

```java
// Enhanced for-each — external iteration
for (Product p : products) {
    System.out.println(p.getName());
}

// forEach() — internal iteration (Java 8+)
products.forEach(p -> System.out.println(p.getName()));

// forEach() with method reference
products.forEach(System.out::println);
```

Key differences:
- `for-each` can use `break`, `continue`, `return`; `forEach` cannot
- `forEach` can be parallelized with `parallelStream().forEach()`
- `forEach` can be chained in a Stream pipeline

---

## 22. Map Operations (Java 8+)

**Q: What Java 8 methods were added to `Map`?**

A:

```java
Map<Long, Product> productMap = new HashMap<>();

// putIfAbsent — puts only if key doesn't exist
productMap.putIfAbsent(1L, product);

// computeIfAbsent — compute value only if key doesn't exist
Product p = productMap.computeIfAbsent(1L, id -> fetchFromDatabase(id));
// Equivalent to:
Product p = productMap.get(1L);
if (p == null) {
    p = fetchFromDatabase(1L);
    productMap.put(1L, p);
}

// forEach — iterate over entries
productMap.forEach((id, product) ->
    System.out.println(id + ": " + product.getName()));

// getOrDefault — return value or default
Product p = productMap.getOrDefault(1L, defaultProduct);

// merge — combine values
map.merge(key, newValue, (oldVal, newVal) -> oldVal + ", " + newVal);
```

These are not used in this project since it doesn't use in-memory maps extensively.

---

## 23. java.util.function in Practice

**Q: Walk through a real Stream pipeline from this project and explain each step.**

A: Here is the payment service's `getPaymentsByUser`:

```java
public List<PaymentResponse> getPaymentsByUser(Long userId) {
    return paymentRepository.findByUserId(userId)   // returns List<Payment>
        .stream()                                    // Stream<Payment>
        .map(this::mapToDTO)                          // Stream<PaymentResponse>
        .collect(Collectors.toList());               // List<PaymentResponse>
}
```

Step-by-step:

1. **`findByUserId(userId)`** — calls the database, returns `List<Payment>` (hardcoded query from JPA method name)
2. **`.stream()`** — creates a sequential `Stream<Payment>` from the list (no data processed yet)
3. **`.map(this::mapToDTO)`** — intermediate operation. `this::mapToDTO` is a **method reference** that implements `Function<Payment, PaymentResponse>`. For each `Payment`, it calls the mapping method to create a `PaymentResponse`. This is **lazy** — nothing happens yet.
4. **`.collect(Collectors.toList())`** — **terminal operation**. This triggers the entire pipeline: for each Payment, the map function is called, and results are accumulated into a new `ArrayList<PaymentResponse>`.

The `mapToDTO` method (the actual function):

```java
private PaymentResponse mapToDTO(Payment payment) {
    PaymentResponse dto = new PaymentResponse();
    dto.setId(payment.getId());
    dto.setOrderId(payment.getOrderId());
    dto.setUserId(payment.getUserId());
    dto.setAmount(payment.getAmount());
    dto.setStatus(payment.getStatus());
    dto.setTransactionId(payment.getTransactionId());
    dto.setCreatedAt(payment.getCreatedAt());
    return dto;
}
```

---

## 24. Common Interview Questions

**Q: How would you remove duplicates from a `List` while preserving order?**

A: Use a `LinkedHashSet`:

```java
List<String> list = Arrays.asList("A", "B", "A", "C", "B");
List<String> unique = new ArrayList<>(new LinkedHashSet<>(list));
// Result: [A, B, C] — order preserved, duplicates removed
```

---

**Q: How would you find the most frequent element in a list using streams?**

A:

```java
List<String> items = Arrays.asList("A", "B", "A", "C", "A", "B");

String mostFrequent = items.stream()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    .entrySet().stream()
    .max(Map.Entry.comparingByValue())
    .map(Map.Entry::getKey)
    .orElse(null);
// Result: "A" (appears 3 times)
```

---

**Q: What is the difference between `findFirst()` and `findAny()` in a Stream?**

A: `findFirst()` returns the first element of the stream — deterministic. `findAny()` returns any element — non-deterministic, useful with parallel streams (gives better performance).

```java
// Sequential — both return same result
products.stream().findFirst();  // always first element
products.stream().findAny();    // also first element (sequential)

// Parallel — findAny is faster
products.parallelStream().findAny();   // returns any element, potentially faster
products.parallelStream().findFirst(); // still returns first, may be slower
```

---

**Q: How does `HashMap` handle collisions in Java 8+?**

A: Before Java 8, collisions were handled by linked lists (O(n) lookup in worst case). In Java 8+, when a bucket has more than 8 entries (`TREEIFY_THRESHOLD`), the linked list is converted to a **balanced tree** (Red-Black tree, O(log n) lookup). If entries shrink below 6 (`UNTREEIFY_THRESHOLD`), it converts back to a linked list.

---

**Q: Why is `HashMap` initial capacity 16 and load factor 0.75?**

A: These are engineering tradeoffs:
- **Capacity 16**: Power of 2 (bitwise AND for bucket index: `hash & (n-1)` is faster than modulo). Small enough to not waste memory, large enough to avoid frequent resizing.
- **Load factor 0.75**: Balance between time and space. Lower (0.5) = more memory, fewer collisions. Higher (0.9) = less memory, more collisions. 0.75 is the empirically determined sweet spot.
