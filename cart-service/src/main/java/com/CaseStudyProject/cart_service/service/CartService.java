package com.CaseStudyProject.cart_service.service;

import com.CaseStudyProject.cart_service.client.ProductClient;
import com.CaseStudyProject.cart_service.dto.*;
import com.CaseStudyProject.cart_service.entity.Cart;
import com.CaseStudyProject.cart_service.entity.CartItem;
import com.CaseStudyProject.cart_service.exception.BadRequestException;
import com.CaseStudyProject.cart_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.cart_service.repository.CartItemRepository;
import com.CaseStudyProject.cart_service.repository.CartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for managing shopping cart operations.
 * Handles business logic for adding, updating, and removing items,
 * while maintaining the total price integrity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository crepo;
    private final CartItemRepository cirepo;
    private final ProductClient productClient;

    /**
     * Finds the cart associated with a user or creates a new one if none exists.
     */
    public Cart getOrCreateCart(Long userId) {
        log.debug("CartService - Getting or creating cart for userId: {}", userId);
        Cart cart = crepo.findByUserId(userId).orElseGet(() -> {
            log.info("CartService - Creating new cart for userId: {}", userId);
            return crepo.save(new Cart(userId));
        });
        log.debug("CartService - Cart retrieved - cartId: {}", cart.getId());
        return cart;
    }

    /**
     * Adds an item to the user's cart.
     * If the item exists, increments quantity; otherwise, creates a new entry.
     */
    public void addToCart(Long userId, AddToCartRequest req) {
        log.info("CartService - Adding item to cart - userId: {} - productId: {} - quantity: {}", userId, req.getProductId(), req.getQuantity());

        if (req.getQuantity() <= 0) {
            log.warn("CartService - Invalid quantity: {}", req.getQuantity());
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Cart cart = getOrCreateCart(userId);

        // External call via Feign to validate product and get current price
        log.debug("CartService - Fetching product from PRODUCT-SERVICE - productId: {}", req.getProductId());
        ProductDTO product = productClient.getProductById(req.getProductId());
        log.debug("CartService - Product fetched: {}", product.getName());

        CartItem item = cirepo.findByCartIdAndProductId(cart.getId(), req.getProductId()).orElse(null);

        if (item != null) {
            log.debug("CartService - Item already in cart, incrementing quantity");
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            log.debug("CartService - Adding new item to cart");
            item = new CartItem(
                    cart.getId(),
                    product.getId(),
                    req.getQuantity(),
                    product.getPrice());
        }

        cirepo.save(item);
        log.info("CartService - Item saved to cart - userId: {} - productId: {}", userId, req.getProductId());
        recalculateTotal(cart);
    }

    /**
     * Updates the specific quantity of a cart item.
     * Deletes the item if quantity is set to 0 or less.
     */
    public void updateQuantity(Long userId, UpdateCartRequest req) {
        log.info("CartService - Updating item quantity - userId: {} - productId: {} - newQuantity: {}", userId, req.getProductId(), req.getQuantity());

        Cart cart = getOrCreateCart(userId);
        CartItem item = cirepo.findByCartIdAndProductId(cart.getId(), req.getProductId()).orElseThrow(() -> {
            log.error("CartService - Item not found in cart - cartId: {} - productId: {}", cart.getId(), req.getProductId());
            return new ResourceNotFoundException("Item not found in cart");
        });

        if (req.getQuantity() <= 0) {
            log.info("CartService - Removing item from cart (quantity: 0) - productId: {}", req.getProductId());
            cirepo.delete(item);
        } else {
            log.debug("CartService - Updating item quantity - productId: {} - newQuantity: {}", req.getProductId(), req.getQuantity());
            item.setQuantity(req.getQuantity());
            cirepo.save(item);
        }
        recalculateTotal(cart);
    }

    /**
     * Removes an item entirely from the cart regardless of quantity.
     */
    public void removeItem(Long userId, Long productId) {
        log.info("CartService - Removing item from cart - userId: {} - productId: {}", userId, productId);

        Cart cart = getOrCreateCart(userId);
        CartItem item = cirepo.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> {
                    log.error("CartService - Item not in cart - cartId: {} - productId: {}", cart.getId(), productId);
                    return new ResourceNotFoundException("Item not in cart");
                });

        cirepo.delete(item);
        log.info("CartService - Item removed from cart - userId: {} - productId: {}", userId, productId);
        recalculateTotal(cart);
    }

    /**
     * Fetches the cart and maps it to a response DTO.
     */
    public CartResponse getCart(Long userId) {
        log.debug("CartService - Retrieving cart - userId: {}", userId);
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cirepo.findByCartId(cart.getId());
        log.info("CartService - Cart retrieved - userId: {} - itemCount: {} - totalPrice: {}", userId, items.size(), cart.getTotalPrice());
        return mapToResponse(cart, items);
    }

    /**
     * Deletes all items in a user's cart and resets the total price.
     */
    public void clearCart(Long userId) {
        log.info("CartService - Clearing cart - userId: {}", userId);

        Cart cart = crepo.findByUserId(userId).orElseThrow(() -> {
            log.error("CartService - Cart not found for userId: {}", userId);
            return new ResourceNotFoundException("Cart not found");
        });

        cirepo.deleteByCartId(cart.getId());
        cart.setTotalPrice(0.0);
        crepo.save(cart);
        log.info("CartService - Cart cleared successfully - userId: {}", userId);
    }

    /**
     * Private helper to sum up the price * quantity for all items.
     */
    private void recalculateTotal(Cart cart) {
        log.debug("CartService - Recalculating cart total - cartId: {}", cart.getId());
        List<CartItem> items = cirepo.findByCartId(cart.getId());

        double total = items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        cart.setTotalPrice(total);

        crepo.save(cart);
        log.debug("CartService - Cart total recalculated - cartId: {} - newTotal: {}", cart.getId(), total);
    }

    /**
     * Maps the internal entities to the DTOs expected by the Controller.
     */
    private CartResponse mapToResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> li = items.stream().map(
                i -> {
                    CartItemResponse cir = new CartItemResponse();
                    cir.setProductId(i.getProductId());
                    cir.setQuantity(i.getQuantity());
                    cir.setPrice(i.getPrice());
                    return cir;
                }).toList();

        CartResponse response = new CartResponse();
        response.setUserId(cart.getUserId());
        response.setTotalPrice(cart.getTotalPrice());
        response.setItems(li);

        return response;
    }
}