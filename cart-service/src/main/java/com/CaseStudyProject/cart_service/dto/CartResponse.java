package com.CaseStudyProject.cart_service.dto;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object representing the full state of a user's shopping cart.
 * Consolidates individual cart items and calculates the aggregate total.
 */
@Data
public class CartResponse {

    // The unique identifier of the user who owns this cart
    private Long userId;

    /**
     * The sum of (price * quantity) for all items currently in the cart.
     */
    private Double totalPrice;

    /**
     * A collection of detailed line items currently present in the cart.
     */
    private List<CartItemResponse> items;

}