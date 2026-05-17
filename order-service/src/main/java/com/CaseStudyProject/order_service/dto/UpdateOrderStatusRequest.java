package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object for handling administrative or system-level status updates.
 */
@Data
public class UpdateOrderStatusRequest {
    // The new status value to be applied to the order (e.g., "DELIVERED", "PROCESSING")
    private String status;
}