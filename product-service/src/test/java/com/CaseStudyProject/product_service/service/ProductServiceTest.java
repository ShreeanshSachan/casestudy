package com.CaseStudyProject.product_service.service;

import com.CaseStudyProject.product_service.entity.Product;
import com.CaseStudyProject.product_service.exception.BadRequestException;
import com.CaseStudyProject.product_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
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
    void createProduct_Success() {
        when(repository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        assertNotNull(result);
        assertEquals("Nike Air Max", result.getName());
        assertEquals(120.50, result.getPrice());
        verify(repository, times(1)).save(product);
    }

    // ===========================
    // GET PRODUCT BY ID TESTS
    // ===========================

    @Test
    void getProductById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nike Air Max", result.getName());
    }

    @Test
    void getProductById_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    // ===========================
    // GET ALL PRODUCTS TESTS
    // ===========================

    @Test
    void getAllProducts_Success() {
        when(repository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nike Air Max", result.get(0).getName());
    }

    @Test
    void getAllProducts_EmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===========================
    // UPDATE PRODUCT TESTS
    // ===========================

    @Test
    void updateProduct_Success() {
        Product updated = new Product();
        updated.setName("Nike Air Max 2");
        updated.setPrice(150.0);
        updated.setBrand("Nike");
        updated.setCategory("Running");
        updated.setDescription("Updated description");

        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenReturn(updated);

        Product result = productService.updateProduct(1L, updated);

        assertNotNull(result);
        assertEquals("Nike Air Max 2", result.getName());
        assertEquals(150.0, result.getPrice());
        verify(repository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(99L, product));
    }

    // ===========================
    // DELETE PRODUCT TESTS
    // ===========================

    @Test
    void deleteProduct_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(repository).delete(product);

        productService.deleteProduct(1L);

        verify(repository, times(1)).delete(product);
    }

    @Test
    void deleteProduct_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(99L));
    }

    // ===========================
    // GET BY CATEGORY TESTS
    // ===========================

    @Test
    void getByCategory_Success() {
        when(repository.findByCategoryIgnoreCase("Running"))
                .thenReturn(List.of(product));

        List<Product> result = productService.getByCategory("Running");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Running", result.get(0).getCategory());
    }

    @Test
    void getByCategory_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findByCategoryIgnoreCase("Unknown"))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getByCategory("Unknown"));
    }

    @Test
    void getByCategory_CaseInsensitive_Success() {
        when(repository.findByCategoryIgnoreCase("running"))
                .thenReturn(List.of(product));

        List<Product> result = productService.getByCategory("running");

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}