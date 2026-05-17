package com.CaseStudyProject.payment_service.exception;

public class ResourceAccessException extends RuntimeException {
    public ResourceAccessException(String message) {
        super(message);
    }
}
