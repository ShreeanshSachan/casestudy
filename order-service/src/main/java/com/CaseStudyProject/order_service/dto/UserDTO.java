package com.CaseStudyProject.order_service.dto;

import lombok.Data;

/**
 * Data Transfer Object for capturing user profile details.
 * Used for storing customer info retrieved from the User Service during order processing.
 */
@Data
public class UserDTO {
    // Unique identifier of the user from the User Service
    private Long id;

    // Full name of the customer
    private String name;

    // Email address for order notifications or identification
    private String email;

    // Authorization role (e.g., USER, ADMIN) associated with this account
    private String role;
}