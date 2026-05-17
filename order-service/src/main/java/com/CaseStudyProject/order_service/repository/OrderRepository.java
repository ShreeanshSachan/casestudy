package com.CaseStudyProject.order_service.repository;

import com.CaseStudyProject.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing Order persistence.
 * Provides standard CRUD operations and custom query methods for order data.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Custom query method to retrieve all orders associated with a specific user.
     * @param userId The unique identifier of the user whose orders are being fetched.
     * @return A list of Order entities belonging to the specified user.
     */
    List<Order> findByUserId(Long userId);
}