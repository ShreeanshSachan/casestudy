package com.CaseStudyProject.cart_service.dto;

import lombok.Data;

/**
 * Data Transfer Object for carrying product information from the Product Service.
 * Used within the Cart Service to access name and pricing data for calculations.
 */
@Data
public class ProductDTO {

    // The unique identifier retrieved from the Product microservice
    private Long id;

    // The display name of the product
    private String name;

    /**
     * The unit price of the product.
     * Used by the Cart Service to calculate the total price of the user's shopping session.
     */
    private Double price;

}