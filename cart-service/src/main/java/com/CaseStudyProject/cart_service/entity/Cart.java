package com.CaseStudyProject.cart_service.entity;

import jakarta.persistence.*;

/**
 * Entity representing the root shopping cart for a specific user.
 * Each user is restricted to exactly one cart via a unique constraint on userId.
 */
@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(columnNames = "userId"))
public class Cart {

    /**
     * Primary key for the cart record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who owns this cart.
     * Enforced as unique to ensure a 1:1 relationship between users and active carts.
     */
    private Long userId;

    /**
     * The aggregate cost of all items currently in the cart.
     * Defaults to 0.0 for new carts.
     */
    private Double totalPrice = 0.0;

    // Standard Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    /**
     * Default constructor required by JPA.
     */
    public Cart(){}

    /**
     * Convenience constructor to initialize a new cart for a specific user.
     * @param userId The ID of the customer.
     */
    public Cart(Long userId){
        this.userId = userId;
        this.totalPrice = 0.0;
    }
}