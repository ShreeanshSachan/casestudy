package com.CaseStudyProject.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard structure for returning error details to the client in a consistent format.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp; // Time the error occurred
    private int status;              // HTTP status code (e.g., 404, 500)
    private String error;            // Short error type or title
    private String message;          // Detailed description of the error
    private String path;             // API endpoint where the error originated
}