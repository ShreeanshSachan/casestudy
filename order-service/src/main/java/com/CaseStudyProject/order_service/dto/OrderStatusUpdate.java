package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object used for updating the current status of an order.
 */
@Data
public class OrderStatusUpdate {
    // The target status for the order (e.g., "CANCELLED", "SHIPPED")
    private String status;
}