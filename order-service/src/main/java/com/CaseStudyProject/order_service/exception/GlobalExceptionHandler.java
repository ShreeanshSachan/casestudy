package com.CaseStudyProject.order_service.exception;

import com.CaseStudyProject.order_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Global exception handler for the Order Service.
 * Centralizes error management and ensures all API errors return a standard ErrorResponse structure.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Catches ResourceNotFoundException (e.g., specific order or product not found).
     * Returns 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Catches BadRequestException (e.g., invalid order details or validation failures).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Catches errors originating from Feign clients or other microservice dependencies.
     * Returns 502 Bad Gateway to indicate a failure in a downstream component.
     */
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleDownstream(
            DownstreamServiceException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.BAD_GATEWAY.value(),
                        "DOWNSTREAM_SERVICE_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ),
                HttpStatus.BAD_GATEWAY
        );
    }

    /**
     * Fallback handler for all other unhandled runtime exceptions.
     * Returns 500 Internal Server Error with a generic message to mask system details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "Something went wrong",
                        request.getRequestURI()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}