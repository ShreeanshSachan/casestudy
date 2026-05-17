package com.CaseStudyProject.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for providing consistent error feedback from the Product Service.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    // Exact time the exception was caught
    private LocalDateTime timestamp;

    // HTTP response status code (e.g., 400, 404, 500)
    private int status;

    // Short string representing the error type
    private String error;

    // Human-readable message explaining what went wrong
    private String message;

    // The specific URI path where the error occurred
    private String path;
}