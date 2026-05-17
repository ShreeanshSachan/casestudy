package com.CaseStudyProject.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response object sent back to the client upon successful authentication, containing the JWT.
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token; // The generated authorization token (usually JWT)
}