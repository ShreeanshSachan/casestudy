package com.CaseStudyProject.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object used to send status updates back to the ORDER-SERVICE.
 * This is primarily used after a payment transaction is finalized (e.g., setting status to "CONFIRMED").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdate {

    private String status;
}