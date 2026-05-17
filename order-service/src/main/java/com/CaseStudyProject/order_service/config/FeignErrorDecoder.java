package com.CaseStudyProject.order_service.config;

import com.CaseStudyProject.order_service.exception.*;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Custom error decoder for Feign clients to map downstream HTTP error statuses
 * to local domain-specific exceptions.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    /**
     * Intercepts the HTTP response from a Feign call and returns a specific Exception.
     * * @param methodKey The identifier for the Feign client method being executed.
     * @param response  The HTTP response received from the downstream service.
     * @return An Exception that will be thrown in the calling service.
     */
    @Override
    public Exception decode(String methodKey, Response response) {

        // Evaluate the HTTP status code returned by the downstream service
        switch (response.status()) {

            case 400:
                // Maps 400 Bad Request to a local BadRequestException
                return new BadRequestException("Bad request to downstream service");

            case 404:
                // Maps 404 Not Found to a local ResourceNotFoundException
                return new ResourceNotFoundException("Resource not found in downstream service");

            case 500:
                // Maps 500 Internal Server Error to a generic downstream failure
                return new DownstreamServiceException("Downstream service failed");

            default:
                // Fallback for any other unexpected status codes
                return new DownstreamServiceException("Unexpected downstream error");
        }
    }
}