package com.CaseStudyProject.product_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a product in the catalog.
 * Mapped to a database table via JPA.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /**
     * Primary key with identity-based auto-generation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The display name of the product
    private String name;

    // Unit price of the product
    private double price;

    // The manufacturer or brand (e.g., specific sneaker brands)
    private String brand;

    // Classification category for filtering (e.g., "Footwear", "Apparel")
    private String category;

    // Detailed information regarding product specifications
    private String description;

    private String imageUrl;
}