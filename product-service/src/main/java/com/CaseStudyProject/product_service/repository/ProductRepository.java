package com.CaseStudyProject.product_service.repository;

import com.CaseStudyProject.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Product entity operations.
 * Inherits standard CRUD functionality from JpaRepository.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Custom query method to find products by their category.
     * Uses "IgnoreCase" to ensure the search is not sensitive to capital letters.
     * The category name to filter by (e.g., "SNEAKERS", "sneakers").
     * A list of products matching the specified category.
     */
    List<Product> findByCategoryIgnoreCase(String category);
}