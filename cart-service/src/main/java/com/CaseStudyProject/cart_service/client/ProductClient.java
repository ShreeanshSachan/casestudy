package com.CaseStudyProject.cart_service.client;

import com.CaseStudyProject.cart_service.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for communication with the PRODUCT-SERVICE.
 * Allows the Cart Service to validate product details before adding items to a cart.
 */
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    /**
     * Retrieves product metadata (like name and price) from the Product microservice.
     * @param id The unique identifier of the product.
     * @return A DTO containing the product information.
     */
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable Long id);
}