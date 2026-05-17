package com.CaseStudyProject.payment_service.controller;

import com.CaseStudyProject.payment_service.dto.*;
import com.CaseStudyProject.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing payment transactions.
 * Orchestrates payment processing and history retrieval for users.
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Processes a new payment for a specific order.
     * Extracts 'X-User-Id' from the gateway header to ensure the payment is linked to the correct user.
     * The payment details (orderId, amount, paymentMethod).
     * The authenticated user ID from the API Gateway.
     * PaymentResponse containing the transaction ID and status.
     */
    @PostMapping("/process")
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request, @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /payments/process - Processing payment - userId: {} - orderId: {} - amount: {}", userId, request.getOrderId(), request.getAmount());
        PaymentResponse response = paymentService.processPayment(request, userId);

        log.info("Payment processed successfully - userId: {} - orderId: {} - status: {}", userId, request.getOrderId(), response.getStatus());

        return response;
    }

    /**
     * Retrieves the payment details for a specific order.
     * * @param orderId The unique identifier of the order.
     * @param userId The ID of the requesting user for ownership verification.
     * @return PaymentResponse for the associated order.
     */
    @GetMapping("/{orderId}")
    public PaymentResponse getPayment(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {

        log.debug("GET /payments/{} - Fetching payment - userId: {}", orderId, userId);

        PaymentResponse payment = paymentService.getByOrderId(orderId, userId);

        log.info("Payment retrieved - orderId: {} - status: {}", orderId, payment.getStatus());

        return payment;
    }

    /**
     * Retrieves the entire payment history for the authenticated user.
     * * @param userId The authenticated user ID from the API Gateway.
     * @return A list of all payments made by the user.
     */
    @GetMapping("/user")
    public List<PaymentResponse> getUserPayments(@RequestHeader("X-User-Id") Long userId) {
        log.info("GET /payments/user - Fetching all payments for userId: {}", userId);

        List<PaymentResponse> payments = paymentService.getPaymentsByUser(userId);

        log.info("Payments retrieved - userId: {} - count: {}", userId, payments.size());

        return payments;
    }
}