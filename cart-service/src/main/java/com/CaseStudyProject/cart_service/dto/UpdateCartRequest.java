package com.CaseStudyProject.cart_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for modifying the quantity of an item already in the cart.
 */
@Data
public class UpdateCartRequest {

    /**
     * The unique identifier of the product to be updated.
     */
    @NotNull(message = "Product ID is required")
    private Long productId;

    /**
     * The new quantity to be set for this product.
     * Usually replaces the existing quantity in the cart.
     */
    @NotNull(message = "Quantity is required")
    private Integer quantity;

}