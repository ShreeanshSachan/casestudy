package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object used to hold a subset of product details
 * fetched from the Product Service via Feign Client.
 */
@Data
public class ProductDTO {
    // Unique identifier of the product in the Product Service
    private Long id;

    // Name of the product (e.g., specific sneaker model)
    private String name;

    // Current price of the product at the time of retrieval
    private double price;
}