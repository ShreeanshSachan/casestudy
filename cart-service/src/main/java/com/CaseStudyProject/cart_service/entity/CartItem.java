package com.CaseStudyProject.cart_service.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing an individual line item within a shopping cart.
 * Maps to the "cart_items" table with a unique constraint to prevent duplicate
 * products within the same cart.
 */
@Entity
@Data
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"cartId", "productId"}))
public class CartItem {

    /**
     * Primary key for the cart item entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Logical reference to the parent Cart entity's ID.
     */
    private Long cartId;

    /**
     * Reference to the product being added (sourced from Product Service).
     */
    private Long productId;

    /**
     * The number of units of the product in this specific cart.
     */
    private Integer quantity;

    /**
     * The unit price of the product at the time it was added or last updated.
     */
    private Double price;

    /**
     * Default constructor required by JPA.
     */
    public CartItem() {
    }

    /**
     * Parameterized constructor for creating a new line item.
     * * @param cartId    The ID of the parent cart.
     * @param productId The ID of the product.
     * @param quantity  The number of items.
     * @param price     The price per unit.
     */
    public CartItem(Long cartId, Long productId, Integer quantity, Double price) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    /* * Note: Standard Getters and Setters are provided by Lombok's @Data.
     * Manual overrides are only necessary if you need custom logic during
     * access or mutation.
     */
}