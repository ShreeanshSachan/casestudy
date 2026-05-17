package com.CaseStudyProject.user_service.dto;

import lombok.Data;

/**
 * Data Transfer Object for capturing user credentials during authentication requests.
 */
@Data
public class LoginRequest {

    private String email;    // User identification
    private String password; // Plain text password for validation
}