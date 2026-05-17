package com.CaseStudyProject.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing an Order record in the database.
 * Mapped to the "orders" table.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Primary key with auto-incrementing identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References the ID of the user who placed the order (from User Service)
    private Long userId;

    // References the ID of the product being purchased (from Product Service)
    private Long productId;

    // The number of items purchased in this specific order
    private int quantity;

    // The calculated total cost (Product Price * Quantity) at the time of purchase
    private double totalPrice;

    /**
     * The current state of the order.
     * Common values: PENDING, COMPLETED, CANCELLED.
     */
    private String status;
}