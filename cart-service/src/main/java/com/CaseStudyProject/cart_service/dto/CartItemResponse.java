package com.CaseStudyProject.cart_service.dto;

import lombok.Data;

/**
 * Data Transfer Object representing an individual entry within the shopping cart.
 * Contains essential product information and quantity for display purposes.
 */
@Data
public class CartItemResponse {

    // The unique identifier of the product in the item row
    private Long productId;

    // The current number of units of this product in the cart
    private Integer quantity;

    /**
     * The unit price or subtotal price for this item.
     * Usually reflects the price at the time the cart is viewed.
     */
    private Double price;

}