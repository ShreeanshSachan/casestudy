package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object representing a comprehensive view of an order.
 * Consolidates data from the Order, User, and Product services.
 */
@Data
public class OrderResponse {
    // Unique identifier for the created order
    private Long orderId;

    // Customer identification details
    private Long userId;
    private String userName;

    // Snapshot of the product details at the time of order
    private Long productId;
    private String productName;
    private double productPrice;

    // Order quantities and calculated financial totals
    private Integer quantity;
    private double totalPrice;

    // Current lifecycle state of the order (e.g., PENDING, COMPLETED, CANCELLED)
    private String status;
}