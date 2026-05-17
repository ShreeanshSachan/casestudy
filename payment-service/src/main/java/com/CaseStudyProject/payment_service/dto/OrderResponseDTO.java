package com.CaseStudyProject.payment_service.dto;

import lombok.Data;

/**
 * Data Transfer Object received from the ORDER-SERVICE.
 * Used by the Payment Service to verify that the payment amount matches the
 * order's total price before finalizing the transaction.
 */
@Data
public class OrderResponseDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private Double totalPrice;
    private String status;

}