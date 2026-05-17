package com.CaseStudyProject.cart_service.controller;

import com.CaseStudyProject.cart_service.dto.AddToCartRequest;
import com.CaseStudyProject.cart_service.dto.CartItemResponse;
import com.CaseStudyProject.cart_service.dto.CartResponse;
import com.CaseStudyProject.cart_service.dto.UpdateCartRequest;
import com.CaseStudyProject.cart_service.exception.GlobalExceptionHandler;
import com.CaseStudyProject.cart_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.cart_service.service.CartService;
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
public class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        CartItemResponse item = new CartItemResponse();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setPrice(120.50);

        cartResponse = new CartResponse();
        cartResponse.setUserId(1L);
        cartResponse.setTotalPrice(241.0);
        cartResponse.setItems(List.of(item));
    }

    // ===========================
    // ADD TO CART TESTS
    // ===========================

    @Test
    void addToCart_Success() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        doNothing().when(cartService).addToCart(eq(1L), any(AddToCartRequest.class));

        mockMvc.perform(post("/cart/add")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Item added"));
    }

    @Test
    void addToCart_ServiceThrowsException_ReturnsError() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        doThrow(new ResourceNotFoundException("Product not found"))
                .when(cartService).addToCart(eq(1L), any(AddToCartRequest.class));

        mockMvc.perform(post("/cart/add")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    // ===========================
    // UPDATE CART TESTS
    // ===========================

    @Test
    void updateCart_Success() throws Exception {
        UpdateCartRequest request = new UpdateCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        doNothing().when(cartService).updateQuantity(eq(1L), any(UpdateCartRequest.class));

        mockMvc.perform(put("/cart/update")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Updated"));
    }

    @Test
    void updateCart_ItemNotFound_ReturnsNotFound() throws Exception {
        UpdateCartRequest request = new UpdateCartRequest();
        request.setProductId(99L);
        request.setQuantity(3);

        doThrow(new ResourceNotFoundException("Item not found in cart"))
                .when(cartService).updateQuantity(eq(1L), any(UpdateCartRequest.class));

        mockMvc.perform(put("/cart/update")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item not found in cart"));
    }

    // ===========================
    // REMOVE ITEM TESTS
    // ===========================

    @Test
    void removeItem_Success() throws Exception {
        doNothing().when(cartService).removeItem(1L, 1L);

        mockMvc.perform(delete("/cart/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Removed"));
    }

    @Test
    void removeItem_NotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Item not in cart"))
                .when(cartService).removeItem(1L, 99L);

        mockMvc.perform(delete("/cart/99")
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item not in cart"));
    }

    // ===========================
    // GET CART TESTS
    // ===========================

    @Test
    void getCart_Success() throws Exception {
        when(cartService.getCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(get("/cart")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(241.0))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void getCart_EmptyCart_Success() throws Exception {
        cartResponse.setItems(List.of());
        cartResponse.setTotalPrice(0.0);

        when(cartService.getCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(get("/cart")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ===========================
    // CLEAR CART TESTS
    // ===========================

    @Test
    void clearCart_Success() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/cart/clear")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Cleared"));
    }

    @Test
    void clearCart_CartNotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Cart not found"))
                .when(cartService).clearCart(99L);

        mockMvc.perform(delete("/cart/clear")
                        .header("X-User-Id", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart not found"));
    }
}