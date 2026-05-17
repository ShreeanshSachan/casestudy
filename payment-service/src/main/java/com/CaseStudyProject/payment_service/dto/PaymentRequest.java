package com.CaseStudyProject.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an incoming payment request.
 * Contains validation constraints to ensure payments have valid orders and positive amounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;

    private String paymentMethod;
}