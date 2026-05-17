package com.CaseStudyProject.order_service.client;

import com.CaseStudyProject.order_service.config.FeignConfig;
import com.CaseStudyProject.order_service.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for inter-service communication with the PRODUCT-SERVICE.
 * Enables the Order Service to request product details via REST.
 */
@FeignClient(name = "PRODUCT-SERVICE", configuration = FeignConfig.class)
public interface ProductClient {

    /**
     * Synchronously fetches product information from the Product Service by ID.
     * @param id The ID of the product to retrieve.
     * @return A Data Transfer Object containing the product details.
     */
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}