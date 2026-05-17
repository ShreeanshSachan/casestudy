package com.CaseStudyProject.payment_service.client;

import com.CaseStudyProject.payment_service.dto.OrderResponseDTO;
import com.CaseStudyProject.payment_service.dto.OrderStatusUpdate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client for interacting with the ORDER-SERVICE.
 * Used by the Payment Service to verify order details before processing and
 * to update order status upon payment completion.
 */
@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {

    /**
     * Retrieves specific order details for validation.
     * Passes security headers to ensure the Order Service can verify ownership.
     * * @param orderId The ID of the order to fetch.
     * @param userId The ID of the user requesting the order (extracted from Security Context).
     * @param role The role of the user (e.g., ROLE_USER).
     * @return OrderResponseDTO containing price and current status.
     */
    @GetMapping("/orders/{orderId}")
    OrderResponseDTO getOrderById(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * Updates the status of an order after a payment attempt.
     * * @param orderId The ID of the order to update.
     * @param request The status update body (e.g., "CONFIRMED" or "CANCELLED").
     * @param userId The ID of the user performing the update.
     * @return The updated OrderResponseDTO.
     */
    @PutMapping("/orders/{orderId}/status")
    OrderResponseDTO updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdate request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );
}