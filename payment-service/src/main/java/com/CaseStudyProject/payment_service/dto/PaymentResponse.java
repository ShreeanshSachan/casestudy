package com.CaseStudyProject.payment_service.dto;

import com.CaseStudyProject.payment_service.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the final state of a payment transaction.
 * Returned to the client after processing a payment or when querying transaction history.
 */
@Data
@Builder
public class PaymentResponse {

    private Long id;

    private Long orderId;

    private Long userId;

    private Double amount;

    private PaymentStatus status;

    private String transactionId;

    private LocalDateTime createdAt;
}