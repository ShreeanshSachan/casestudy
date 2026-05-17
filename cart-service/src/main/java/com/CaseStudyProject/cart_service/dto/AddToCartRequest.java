package com.CaseStudyProject.cart_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for adding a product to the shopping cart.
 * Includes validation constraints to ensure data integrity.
 */
@Data
public class AddToCartRequest {

    /**
     * The unique identifier of the product to be added.
     * Must not be null.
     */
    @NotNull(message = "Product ID is required")
    private Long productId;

    /**
     * The number of units to add to the cart.
     * Must be at least 1.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}