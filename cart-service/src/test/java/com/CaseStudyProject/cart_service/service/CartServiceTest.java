package com.CaseStudyProject.cart_service.service;

import com.CaseStudyProject.cart_service.client.ProductClient;
import com.CaseStudyProject.cart_service.dto.*;
import com.CaseStudyProject.cart_service.entity.Cart;
import com.CaseStudyProject.cart_service.entity.CartItem;
import com.CaseStudyProject.cart_service.exception.BadRequestException;
import com.CaseStudyProject.cart_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.cart_service.repository.CartItemRepository;
import com.CaseStudyProject.cart_service.repository.CartRepository;
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
public class CartServiceTest {

    @Mock
    private CartRepository crepo;

    @Mock
    private CartItemRepository cirepo;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartItem cartItem;
    private ProductDTO productDTO;
    private AddToCartRequest addToCartRequest;
    private UpdateCartRequest updateCartRequest;

    @BeforeEach
    void setUp() {
        cart = new Cart(1L);
        cart.setId(1L);
        cart.setTotalPrice(0.0);

        cartItem = new CartItem(1L, 1L, 2, 120.50);
        cartItem.setId(1L);

        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setName("Nike Air Max");
        productDTO.setPrice(120.50);

        addToCartRequest = new AddToCartRequest();
        addToCartRequest.setProductId(1L);
        addToCartRequest.setQuantity(2);

        updateCartRequest = new UpdateCartRequest();
        updateCartRequest.setProductId(1L);
        updateCartRequest.setQuantity(3);
    }

    // ===========================
    // GET OR CREATE CART TESTS
    // ===========================

    @Test
    void getOrCreateCart_ExistingCart_ReturnsCart() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(crepo, never()).save(any());
    }

    @Test
    void getOrCreateCart_NewCart_CreatesAndReturnsCart() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
        verify(crepo, times(1)).save(any(Cart.class));
    }

    // ===========================
    // ADD TO CART TESTS
    // ===========================

    @Test
    void addToCart_NewItem_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cirepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(cirepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.addToCart(1L, addToCartRequest);

        verify(cirepo, times(1)).save(any(CartItem.class));
    }

    @Test
    void addToCart_ExistingItem_IncrementsQuantity() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cirepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(cirepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.addToCart(1L, addToCartRequest);

        // Quantity should be incremented
        verify(cirepo, times(1)).save(argThat(item -> item.getQuantity() == 4));
    }

    @Test
    void addToCart_InvalidQuantity_ThrowsBadRequest() {
        addToCartRequest.setQuantity(0);

        assertThrows(BadRequestException.class,
                () -> cartService.addToCart(1L, addToCartRequest));
        verify(cirepo, never()).save(any());
    }

    // ===========================
    // UPDATE QUANTITY TESTS
    // ===========================

    @Test
    void updateQuantity_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cirepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(cirepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.updateQuantity(1L, updateCartRequest);

        verify(cirepo, times(1)).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_ZeroQuantity_RemovesItem() {
        updateCartRequest.setQuantity(0);

        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cirepo.findByCartId(1L)).thenReturn(List.of());
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.updateQuantity(1L, updateCartRequest);

        verify(cirepo, times(1)).delete(cartItem);
    }

    @Test
    void updateQuantity_ItemNotFound_ThrowsResourceNotFoundException() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateQuantity(1L, updateCartRequest));
    }

    // ===========================
    // REMOVE ITEM TESTS
    // ===========================

    @Test
    void removeItem_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cirepo.findByCartId(1L)).thenReturn(List.of());
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.removeItem(1L, 1L);

        verify(cirepo, times(1)).delete(cartItem);
    }

    @Test
    void removeItem_NotFound_ThrowsResourceNotFoundException() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartIdAndProductId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeItem(1L, 99L));
    }

    // ===========================
    // GET CART TESTS
    // ===========================

    @Test
    void getCart_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartId(1L)).thenReturn(List.of(cartItem));

        CartResponse result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getCart_EmptyCart_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cirepo.findByCartId(1L)).thenReturn(List.of());

        CartResponse result = cartService.getCart(1L);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }

    // ===========================
    // CLEAR CART TESTS
    // ===========================

    @Test
    void clearCart_Success() {
        when(crepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        doNothing().when(cirepo).deleteByCartId(1L);
        when(crepo.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(1L);

        verify(cirepo, times(1)).deleteByCartId(1L);
        verify(crepo, times(1)).save(any(Cart.class));
    }

    @Test
    void clearCart_CartNotFound_ThrowsResourceNotFoundException() {
        when(crepo.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.clearCart(99L));
    }
}