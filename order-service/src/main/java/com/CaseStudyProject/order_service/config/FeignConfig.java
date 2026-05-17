package com.CaseStudyProject.order_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration class to customize OpenFeign behavior for the Order Service.
 */
@Configuration
public class FeignConfig {

    /**
     * Registers a custom ErrorDecoder bean.
     * This allows the application to intercept and handle specific HTTP errors
     * returned by downstream services (Product or User service) during Feign calls.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate requestTemplate) -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();

            String userId = request.getHeader("X-User-Id");
            String role = request.getHeader("X-User-Role");

            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }

            if (role != null) {
                requestTemplate.header("X-User-Role", role);
            }
        };
    }

}