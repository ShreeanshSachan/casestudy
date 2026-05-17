package com.CaseStudyProject.product_service.service;

import com.CaseStudyProject.product_service.entity.Product;
import com.CaseStudyProject.product_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class containing business logic for Product management.
 * Acts as an intermediary between the Controller and the Repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    /**
     * Persists a new product to the database.
     */
    public Product createProduct(Product product) {
        log.info("ProductService - Creating product: {}", product.getName());
        Product savedProduct = repository.save(product);
        log.info("ProductService - Product saved successfully - id: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Retrieves a product by ID or throws a custom exception if not found.
     */
    public Product getProductById(Long id) {
        log.debug("ProductService - Fetching product by id: {}", id);
        Product product = repository.findById(id).orElseThrow(() -> {
                    log.error("ProductService - Product not found with id: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });

        log.debug("ProductService - Product found: {}", product.getName());
        return product;
    }

    /**
     * Returns a list of all products currently in the database.
     */
    public List<Product> getAllProducts() {
        log.debug("ProductService - Fetching all products");
        List<Product> products = repository.findAll();
        log.debug("ProductService - Total products: {}", products.size());
        return products;
    }

    /**
     * Deletes a product after verifying its existence.
     */
    public void deleteProduct(Long id) {
        log.info("ProductService - Deleting product with id: {}", id);
        Product existing = getProductById(id); // Reuses logic to ensure product exists
        repository.delete(existing);
        log.info("ProductService - Product deleted successfully - id: {}", id);
    }

    /**
     * Updates an existing product by mapping fields from the updated object.
     */
    public Product updateProduct(Long id, Product updated) {
        log.info("ProductService - Updating product with id: {}", id);
        Product existing = getProductById(id);

        // Update existing entity state
        existing.setName(updated.getName());
        existing.setPrice(updated.getPrice());
        existing.setBrand(updated.getBrand());
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());

        Product savedProduct = repository.save(existing);
        log.info("ProductService - Product updated successfully - id: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Retrieves products filtered by category name (case-insensitive).
     * Throws exception if the resulting list is empty.
     */
    public List<Product> getByCategory(String category) {
        log.debug("ProductService - Fetching products by category: {}", category);
        List<Product> products = repository.findByCategoryIgnoreCase(category);

        if (products.isEmpty()) {
            log.warn("ProductService - No products found for category: {}", category);
            throw new ResourceNotFoundException("No products found for category: " + category);
        }

        log.info("ProductService - Found {} products in category: {}", products.size(), category);
        return products;
    }
}