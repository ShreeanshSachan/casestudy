package com.CaseStudyProject.cart_service.exception;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard data carrier for error details in the Cart Service.
 * Ensures the frontend or calling services receive a predictable error format.
 */
@Data
public class ErrorResponse {

    // When the error occurred
    private LocalDateTime timestamp;

    // HTTP Status code (e.g., 404, 400)
    private int status;

    // Brief error category (e.g., "NOT_FOUND")
    private String error;

    // Detailed explanation of the error
    private String message;

    // The API endpoint that was being accessed
    private String path;

    /**
     * All-arguments constructor to manually populate the response
     * within GlobalExceptionHandler.
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}