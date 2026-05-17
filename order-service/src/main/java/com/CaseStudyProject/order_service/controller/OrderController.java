package com.CaseStudyProject.order_service.controller;

import com.CaseStudyProject.order_service.dto.OrderRequest;
import com.CaseStudyProject.order_service.dto.OrderResponse;
import com.CaseStudyProject.order_service.dto.OrderStatusUpdate;
import com.CaseStudyProject.order_service.dto.UpdateOrderStatusRequest;
import com.CaseStudyProject.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing order placement and tracking.
 * Coordinates between the Order service and user-specific security context.
 */
@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /**
     * Creates a new order for the authenticated user.
     * Uses the userId passed from the API Gateway header.
     */
    @PostMapping
    public OrderResponse createOrder(@RequestBody OrderRequest request, @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /orders - Creating order for userId: {} - productId: {}", userId, request.getProductId());
        OrderResponse order = service.createOrder(request, userId);
        log.info("Order created successfully - orderId: {} - totalPrice: {}", order.getOrderId(), order.getTotalPrice());
        return order;
    }

    /**
     * Retrieves specific order details by ID.
     * Enforces security: Users can only see their own orders, while ADMINs can see any.
     */
    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role) {
        log.debug("GET /orders/{} - Fetching order", id);
        OrderResponse order = service.getOrderById(id);

        // Ownership check for non-admin users
        if (role.equals("USER") && !order.getUserId().equals(userId)) {
            log.warn("GET /orders/{} - Unauthorized access attempt by userId: {}", id, userId);
            throw new RuntimeException("Access denied");
        }

        log.info("Order found - orderId: {} - status: {}", order.getOrderId(), order.getStatus());
        return order;
    }

    /**
     * Retrieves all orders in the system.
     * Strictly restricted to the ADMIN role.
     */
    @GetMapping
    public List<OrderResponse> getAllOrders(@RequestHeader("X-User-Role") String role) {
        log.info("GET /orders - Fetching all orders");
        if (!role.equals("ADMIN")) {
            log.warn("GET /orders - Unauthorized attempt by non-ADMIN user");
            throw new RuntimeException("Only ADMIN can view all orders");
        }
        List<OrderResponse> orders = service.getAllOrders();
        log.info("Returning {} orders", orders.size());
        return orders;
    }

    /**
     * Retrieves a list of orders specifically belonging to the authenticated user.
     */
    @GetMapping("/my")
    public List<OrderResponse> getMyOrders(@RequestHeader("X-User-Id") Long userId) {
        log.info("GET /orders/my - Fetching orders for userId: {}", userId);
        List<OrderResponse> orders = service.getOrdersByUserId(userId);
        log.info("Found {} orders for userId: {}", orders.size(), userId);
        return orders;
    }

    /**
     * Updates the status of an existing order (e.g., SHIPPED, CANCELLED).
     * Strictly restricted to the ADMIN role.
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdate request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("PUT /orders/{}/status - Admin status update request by user: {}", orderId, userId);

        // Strict Role Check: Only ADMIN allowed
        if (!"ADMIN".equals(role)) {
            log.warn("Unauthorized status update attempt by userId: {} with role: {}", userId, role);
            throw new RuntimeException("Access denied: Only administrators can update order status");
        }

        // Pass control to service
        OrderResponse updatedOrder = service.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(updatedOrder);
    }
}

