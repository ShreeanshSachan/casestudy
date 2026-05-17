package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object for capturing order placement details from the client.
 */
@Data
public class OrderRequest {
    // Note: userId is typically extracted from the security context/header rather than the request body
    // private Long userId;

    // The unique identifier of the product being purchased
    private Long productId;

    // The number of units the user wants to order
    private Integer quantity;
}