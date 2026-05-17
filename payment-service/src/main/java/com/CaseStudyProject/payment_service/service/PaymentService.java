package com.CaseStudyProject.payment_service.service;

import com.CaseStudyProject.payment_service.client.OrderClient;
import com.CaseStudyProject.payment_service.dto.OrderResponseDTO;
import com.CaseStudyProject.payment_service.dto.OrderStatusUpdate;
import com.CaseStudyProject.payment_service.dto.PaymentRequest;
import com.CaseStudyProject.payment_service.dto.PaymentResponse;
import com.CaseStudyProject.payment_service.entity.Payment;
import com.CaseStudyProject.payment_service.entity.PaymentStatus;
import com.CaseStudyProject.payment_service.exception.BadRequestException;
import com.CaseStudyProject.payment_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.*;

/**
 * Service class for handling payment orchestration.
 * Manages the transition of an order from 'CREATED' to 'CONFIRMED' or 'CANCELLED'
 * based on the result of a payment simulation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repo;
    private final OrderClient orderClient;

    @Value("${payment.simulation.mode:random}")
    private String simMode;

    /**
     * Orchestrates the payment process.
     * 1. Verifies order ownership and existence via Order-Service.
     * 2. Checks for existing payments to prevent double-billing.
     * 3. Validates that the payment amount matches the order total.
     * 4. Simulates the transaction and updates the order status accordingly.
     */
    public PaymentResponse processPayment(PaymentRequest req, Long userId) {
        log.info("PaymentService - Processing payment - userId: {} - orderId: {} - amount: {}", userId, req.getOrderId(), req.getAmount());

        OrderResponseDTO order;

        // Verify order exists in ORDER-SERVICE
        try {
            log.debug("PaymentService - Fetching order from ORDER-SERVICE - orderId: {}", req.getOrderId());
            order = orderClient.getOrderById(req.getOrderId(), userId, "USER");
            log.debug("PaymentService - Order fetched successfully - orderId: {}", req.getOrderId());
        } catch (Exception e) {
            log.error("PaymentService - Failed to fetch order - orderId: {} - error: {}", req.getOrderId(), e.getMessage());
            throw new ResourceNotFoundException("Order not found with id: " + req.getOrderId());
        }

        // Authorization: Ensure user is paying for their own order
        if (!order.getUserId().equals(userId)) {
            log.warn("PaymentService - Unauthorized payment attempt - userId: {} - orderOwnerId: {}", userId, order.getUserId());
            throw new BadRequestException("You are not allowed to pay for this order");
        }

        // Idempotency: Prevent multiple payments for the same order
        repo.findByOrderId(order.getOrderId()).ifPresent(p -> {
            log.warn("PaymentService - Payment already exists for orderId: {}", order.getOrderId());
            throw new BadRequestException("Payment already exists for this order");
        });

        // Integrity: Validate amount
        if (!Objects.equals(order.getTotalPrice(), req.getAmount())) {
            log.warn("PaymentService - Amount mismatch - expected: {} - provided: {}", order.getTotalPrice(), req.getAmount());
            throw new BadRequestException("Payment amount mismatch");
        }

        // Transaction Simulation
        log.debug("PaymentService - Simulating payment");
        PaymentStatus status = simulatePayment();
        log.info("PaymentService - Payment simulation result: {}", status);

        // Build and persist Payment record
        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .userId(userId)
                .amount(req.getAmount())
                .status(status)
                .paymentMethod(req.getPaymentMethod())
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        payment = repo.save(payment);
        log.info("PaymentService - Payment saved - paymentId: {} - orderId: {} - status: {}", payment.getId(), payment.getOrderId(), status);

        // Callback to ORDER-SERVICE to finalize order lifecycle
        OrderStatusUpdate updateDTO = new OrderStatusUpdate();
        if (status == PaymentStatus.SUCCESS) {
            updateDTO.setStatus("CONFIRMED");
        } else {
            updateDTO.setStatus("CANCELLED");
        }

        try {
            log.debug("PaymentService - Calling ORDER-SERVICE to update status - orderId: {}", order.getOrderId());
            orderClient.updateOrderStatus(order.getOrderId(), updateDTO, userId , "ADMIN");
            log.info("PaymentService - Order status updated successfully - orderId: {} - newStatus: {}", order.getOrderId(), updateDTO.getStatus());
        } catch (Exception ex) {
            // Log as warning rather than failure; the payment is recorded, order status can be synced later if needed
            log.warn("PaymentService - Failed to update order status - orderId: {} - error: {}", order.getOrderId(), ex.getMessage());
        }

        return mapToDTO(payment);
    }

    /**
     * Logic for simulating a transaction based on configuration properties.
     */
    private PaymentStatus simulatePayment() {
        log.debug("PaymentService - Simulating payment with mode: {}", simMode);
        return switch (simMode.toLowerCase()) {
            case "success" -> PaymentStatus.SUCCESS;
            case "failure" -> PaymentStatus.FAILED;
            default -> (new Random().nextInt(2) == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        };
    }

    /**
     * Retrieves a payment by the associated Order ID.
     */
    public PaymentResponse getByOrderId(Long orderId, Long userId) {
        log.debug("PaymentService - Fetching payment by orderId: {} - userId: {}", orderId, userId);
        Payment payment = repo.findByOrderId(orderId).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found for orderId: " + orderId));

        if (!payment.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to payment details");
        }

        return mapToDTO(payment);
    }

    /**
     * Fetches all payment history for a user.
     */
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return repo.findByUserId(userId).stream().map(this::mapToDTO).toList();
    }

    private PaymentResponse mapToDTO(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}