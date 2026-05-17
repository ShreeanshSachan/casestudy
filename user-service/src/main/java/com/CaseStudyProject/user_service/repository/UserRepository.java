package com.CaseStudyProject.user_service.repository;

import com.CaseStudyProject.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access layer for User entities.
 * Extends JpaRepository to provide standard CRUD operations and custom query methods.
 */
public interface UserRepository extends JpaRepository<User,Long> {

    /**
     * Retrieves a user by their email address.
     * param email The unique email to search for.
     * return An Optional containing the User if found, or empty if not.
     */
    Optional<User> findByEmail(String email);
}