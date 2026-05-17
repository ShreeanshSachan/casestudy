package com.CaseStudyProject.order_service.controller;

import com.CaseStudyProject.order_service.client.ProductClient;
import com.CaseStudyProject.order_service.dto.ProductDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class FeignTestController {

    private final ProductClient productClient;

    public FeignTestController(ProductClient productClient) {
        this.productClient = productClient;
    }

    @GetMapping("/products/{id}")
    public ProductDTO testProduct(@PathVariable Long id) {
        return productClient.getProductById(id);
    }
}
