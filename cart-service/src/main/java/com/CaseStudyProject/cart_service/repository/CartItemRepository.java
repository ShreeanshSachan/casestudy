package com.CaseStudyProject.cart_service.repository;

import com.CaseStudyProject.cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing individual CartItem persistence.
 * Handles the fine-grained operations for products within a specific cart.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Retrieves all items currently associated with a specific cart.
     * Used for calculating totals and displaying the full cart view.
     */
    List<CartItem> findByCartId(Long id);

    /**
     * Finds a specific product entry within a user's cart.
     * Useful for checking if an item should be updated (incremented)
     * instead of creating a new row.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Removes all items for a given cart ID.
     * Typically called when a user clears their cart or completes an order.
     */
    void deleteByCartId(Long cartId);
}