package com.CaseStudyProject.cart_service.repository;

import com.CaseStudyProject.cart_service.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing Cart persistence.
 * Focuses on high-level cart operations linked to specific users.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Retrieves the shopping cart assigned to a specific user.
     * Since there is a unique constraint on userId, this returns a single Optional result.
     * * @param id The unique identifier of the user (from User Service).
     * @return An Optional containing the Cart if it exists, otherwise empty.
     */
    Optional<Cart> findByUserId(Long id);
}