package com.CaseStudyProject.payment_service.repository;

import com.CaseStudyProject.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Payment entity persistence.
 * Provides abstraction for database operations related to transaction records.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);

}