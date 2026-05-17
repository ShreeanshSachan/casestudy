package com.CaseStudyProject.product_service.controller;

import com.CaseStudyProject.product_service.entity.Product;
import com.CaseStudyProject.product_service.exception.GlobalExceptionHandler;
import com.CaseStudyProject.product_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService service;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Product product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        product = new Product();
        product.setId(1L);
        product.setName("Nike Air Max");
        product.setPrice(120.50);
        product.setBrand("Nike");
        product.setCategory("Running");
        product.setDescription("Premium running shoes");
    }

    // ===========================
    // CREATE PRODUCT TESTS
    // ===========================

    @Test
    void createProduct_AsAdmin_Success() throws Exception {
        when(service.createProduct(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nike Air Max"))
                .andExpect(jsonPath("$.price").value(120.50));
    }

    @Test
    void createProduct_AsUser_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/products")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isInternalServerError());
    }

    // ===========================
    // GET ALL PRODUCTS TESTS
    // ===========================

    @Test
    void getAllProducts_Success() throws Exception {
        when(service.getAllProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nike Air Max"))
                .andExpect(jsonPath("$[0].price").value(120.50));
    }

    @Test
    void getAllProducts_EmptyList() throws Exception {
        when(service.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===========================
    // GET PRODUCT BY ID TESTS
    // ===========================

    @Test
    void getProductById_Success() throws Exception {
        when(service.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nike Air Max"));
    }

    @Test
    void getProductById_NotFound_Returns404() throws Exception {
        when(service.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    // ===========================
    // UPDATE PRODUCT TESTS
    // ===========================

    @Test
    void updateProduct_AsAdmin_Success() throws Exception {
        when(service.updateProduct(eq(1L), any(Product.class))).thenReturn(product);

        mockMvc.perform(put("/products/1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nike Air Max"));
    }

    @Test
    void updateProduct_AsUser_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(put("/products/1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateProduct_NotFound_Returns404() throws Exception {
        when(service.updateProduct(eq(99L), any(Product.class)))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(put("/products/99")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    // ===========================
    // DELETE PRODUCT TESTS
    // ===========================

    @Test
    void deleteProduct_AsAdmin_Success() throws Exception {
        doNothing().when(service).deleteProduct(1L);

        mockMvc.perform(delete("/products/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product deleted successfully"));
    }

    @Test
    void deleteProduct_AsUser_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(delete("/products/1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteProduct_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 99"))
                .when(service).deleteProduct(99L);

        mockMvc.perform(delete("/products/99")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    // ===========================
    // GET BY CATEGORY TESTS
    // ===========================

    @Test
    void getByCategory_Success() throws Exception {
        when(service.getByCategory("Running")).thenReturn(List.of(product));

        mockMvc.perform(get("/products/category/Running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Running"));
    }

    @Test
    void getByCategory_NotFound_Returns404() throws Exception {
        when(service.getByCategory("Unknown"))
                .thenThrow(new ResourceNotFoundException("No products found for category: Unknown"));

        mockMvc.perform(get("/products/category/Unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No products found for category: Unknown"));
    }
}