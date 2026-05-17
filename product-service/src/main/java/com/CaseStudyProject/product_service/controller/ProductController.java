package com.CaseStudyProject.product_service.controller;

import com.CaseStudyProject.product_service.entity.Product;
import com.CaseStudyProject.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing product-related operations.
 * Includes role-based access control enforced via request headers.
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    /**
     * Creates a new product.
     * Restricted to users with the ADMIN role.
     */
    @PostMapping
    public Product create(@RequestBody Product product, @RequestHeader("X-User-Role") String role) {
        log.info("POST /products - Creating product: {} (Brand: {})", product.getName(), product.getBrand());

        // Manual role check: only ADMIN is permitted to perform write operations
        if (!role.equals("ADMIN")) {
            log.warn("POST /products - Unauthorized attempt by non-ADMIN user");
            throw new RuntimeException("Only ADMIN can create products");
        }

        Product createdProduct = service.createProduct(product);
        log.info("Product created successfully - id: {}", createdProduct.getId());
        return createdProduct;
    }

    /**
     * Retrieves a single product by its unique ID.
     * Open to all authenticated roles.
     */
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        log.debug("GET /products/{} - Fetching product", id);
        Product product = service.getProductById(id);
        log.debug("Product found - name: {}", product.getName());
        return product;
    }

    /**
     * Retrieves a list of all available products.
     */
    @GetMapping
    public List<Product> getAll() {
        log.debug("GET /products - Fetching all products");
        List<Product> products = service.getAllProducts();
        log.info("Returning {} products", products.size());
        return products;
    }

    /**
     * Updates an existing product's details.
     * Restricted to ADMIN role.
     */
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product, @RequestHeader("X-User-Role") String role) {
        log.info("PUT /products/{} - Updating product", id);

        if (!role.equals("ADMIN")) {
            log.warn("PUT /products/{} - Unauthorized attempt by non-ADMIN user", id);
            throw new RuntimeException("Only ADMIN can update products");
        }

        Product updatedProduct = service.updateProduct(id, product);
        log.info("Product updated successfully - id: {}", updatedProduct.getId());
        return updatedProduct;
    }

    /**
     * Deletes a product from the system.
     * Restricted to ADMIN role.
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, @RequestHeader("X-User-Role") String role) {
        log.info("DELETE /products/{} - Deleting product", id);

        if (!role.equals("ADMIN")) {
            log.warn("DELETE /products/{} - Unauthorized attempt by non-ADMIN user", id);
            throw new RuntimeException("Only ADMIN can delete products");
        }

        service.deleteProduct(id);
        log.info("Product deleted successfully - id: {}", id);
        return "Product deleted successfully";
    }

    /**
     * Retrieves products filtered by a specific category (e.g., "Sneakers").
     */
    @GetMapping("/category/{category}")
    public List<Product> getByCategory(@PathVariable String category) {
        log.debug("GET /products/category/{} - Fetching products by category", category);
        List<Product> products = service.getByCategory(category);
        log.info("Found {} products in category: {}", products.size(), category);
        return products;
    }
}