package com.CaseStudyProject.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard Data Transfer Object for communicating error details from the Order Service.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    // Timestamp of when the exception occurred
    private LocalDateTime timestamp;

    // HTTP Status code (e.g., 400 for Bad Request, 404 for Not Found)
    private int status;

    // Short string representing the error type/category
    private String error;

    // Human-readable message explaining the cause of the error
    private String message;

    // The specific API endpoint path where the error was triggered
    private String path;
}