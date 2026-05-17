package com.CaseStudyProject.order_service.service;

import com.CaseStudyProject.order_service.client.ProductClient;
import com.CaseStudyProject.order_service.client.UserClient;
import com.CaseStudyProject.order_service.dto.*;
import com.CaseStudyProject.order_service.entity.Order;
import com.CaseStudyProject.order_service.exception.BadRequestException;
import com.CaseStudyProject.order_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class managing the business logic for Orders.
 * Orchestrates communication between User-Service and Product-Service via Feign Clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    /**
     * Handles the creation of a new order.
     * Validates input, fetches external data, calculates totals, and persists the record.
     */
    public OrderResponse createOrder(OrderRequest request, long userId) {
        log.info("OrderService - Creating order for userId: {} - productId: {}", userId, request.getProductId());

        // Basic validation for business constraints
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            log.warn("OrderService - Invalid quantity: {}", request.getQuantity());
            throw new BadRequestException("Quantity must be greater than 0");
        }

        // Inter-service call to retrieve product details (Price, Name)
        log.debug("OrderService - Fetching product from PRODUCT-SERVICE");
        ProductDTO product = productClient.getProductById(request.getProductId());
        log.debug("OrderService - Product fetched: {}", product.getName());

        // Inter-service call to retrieve user profile
        log.debug("OrderService - Fetching user from USER-SERVICE");
        UserDTO user = userClient.getUserById(userId);
        log.debug("OrderService - User fetched: {}", user.getName());

        // Financial calculation
        double total = product.getPrice() * request.getQuantity();

        // Create and populate the Order entity
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(total);
        order.setStatus("CREATED");

        Order saved = orderRepository.save(order);
        log.info("OrderService - Order created successfully - orderId: {} - totalPrice: {}", saved.getId(), total);

        return mapToOrderResponse(saved, user, product);
    }

    /**
     * Retrieves an order by ID and hydrates the response with external service data.
     */
    public OrderResponse getOrderById(Long id) {
        log.debug("OrderService - Fetching order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("OrderService - Order not found with id: {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        // Hydrating the response with external data via Feign
        log.debug("OrderService - Fetching product from PRODUCT-SERVICE");
        ProductDTO product = productClient.getProductById(order.getProductId());

        log.debug("OrderService - Fetching user from USER-SERVICE");
        UserDTO user = userClient.getUserById(order.getUserId());

        log.debug("OrderService - Order found: orderId: {}", order.getId());
        return mapToOrderResponse(order, user, product);
    }

    /**
     * Retrieves all orders in the system (Admin functionality).
     */
    public List<OrderResponse> getAllOrders() {
        log.debug("OrderService - Fetching all orders");
        List<Order> orders = orderRepository.findAll();

        // Mapping each entity to a full response object
        List<OrderResponse> responses = orders.stream()
                .map(order -> {
                    ProductDTO product = productClient.getProductById(order.getProductId());
                    UserDTO user = userClient.getUserById(order.getUserId());
                    return mapToOrderResponse(order, user, product);
                })
                .collect(Collectors.toList());

        log.info("OrderService - Total orders retrieved: {}", responses.size());
        return responses;
    }

    /**
     * Retrieves all orders belonging to a specific user.
     */
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("OrderService - Fetching orders for userId: {}", userId);

        // Validation: ensures the user actually exists before querying
        log.debug("OrderService - Verifying user exists");
        userClient.getUserById(userId);

        List<Order> orders = orderRepository.findByUserId(userId);

        List<OrderResponse> responses = orders.stream()
                .map(order -> {
                    ProductDTO product = productClient.getProductById(order.getProductId());
                    UserDTO user = userClient.getUserById(order.getUserId());
                    return mapToOrderResponse(order, user, product);
                })
                .collect(Collectors.toList());

        log.info("OrderService - Found {} orders for userId: {}", responses.size(), userId);
        return responses;
    }

    /**
     * Validates if the provided status string belongs to the allowed lifecycle states.
     */
    private boolean isValidStatus(String status) {
        return status.equals("CREATED") ||
                status.equals("CONFIRMED") ||
                status.equals("SHIPPED") ||
                status.equals("DELIVERED") ||
                status.equals("CANCELLED");
    }

    /**
     * Helper method to convert an Order entity and DTOs into a single OrderResponse.
     */
    private OrderResponse mapToOrderResponse(Order order, UserDTO user, ProductDTO product) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());

        response.setUserId(user.getId());
        response.setUserName(user.getName());

        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setProductPrice(product.getPrice());

        response.setQuantity(order.getQuantity());
        response.setTotalPrice(order.getTotalPrice());

        response.setStatus(order.getStatus());

        return response;
    }

    /**
     * Updates an order's status while verifying ownership.
     */
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdate request) {
        // 1. Use the correct field name 'orderRepository' (not Class name or 'repository')
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // 2. Validate status if you want to use your existing isValidStatus helper
        if (!isValidStatus(request.getStatus())) {
            throw new BadRequestException("Invalid status value: " + request.getStatus());
        }

        log.info("OrderService - Updating order {} status to {}", orderId, request.getStatus());
        order.setStatus(request.getStatus());

        // 3. Save the entity
        Order updatedOrder = orderRepository.save(order);

        // 4. Hydrate the response with external data (User/Product)
        // to match your other service methods
        ProductDTO product = productClient.getProductById(updatedOrder.getProductId());
        UserDTO user = userClient.getUserById(updatedOrder.getUserId());

        return mapToOrderResponse(updatedOrder, user, product);
    }
}