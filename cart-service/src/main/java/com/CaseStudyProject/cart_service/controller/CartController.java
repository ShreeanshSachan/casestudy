package com.CaseStudyProject.cart_service.controller;

import com.CaseStudyProject.cart_service.dto.AddToCartRequest;
import com.CaseStudyProject.cart_service.dto.CartResponse;
import com.CaseStudyProject.cart_service.dto.UpdateCartRequest;
import com.CaseStudyProject.cart_service.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing the shopping cart.
 * Interfaces with the Gateway to receive authenticated user IDs via HTTP headers.
 */
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Adds a specific product to the user's cart.
     * Extracts 'X-User-Id' from the request header (populated by the API Gateway).
     */
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestHeader("X-User-Id") Long userId, @RequestBody AddToCartRequest req) {
        log.info("POST /cart/add - Adding item to cart - userId: {} - productId: {} - quantity: {}", userId, req.getProductId(), req.getQuantity());
        cartService.addToCart(userId, req);
        log.info("Item added to cart successfully - userId: {}", userId);
        return ResponseEntity.ok("Item added");
    }

    /**
     * Updates the quantity of an existing item in the cart.
     */
    @PutMapping("/update")
    public ResponseEntity<String> update(@RequestHeader("X-User-Id") Long userId, @RequestBody UpdateCartRequest req) {
        log.info("PUT /cart/update - Updating cart item - userId: {} - productId: {} - newQuantity: {}", userId, req.getProductId(), req.getQuantity());
        cartService.updateQuantity(userId, req);
        log.info("Cart item updated - userId: {}", userId);
        return ResponseEntity.ok("Updated");
    }

    /**
     * Removes a single product from the user's cart.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<String> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long productId) {
        log.info("DELETE /cart/{} - Removing item from cart - userId: {}", productId, userId);
        cartService.removeItem(userId, productId);
        log.info("Item removed from cart - userId: {} - productId: {}", userId, productId);
        return ResponseEntity.ok("Removed");
    }

    /**
     * Retrieves the current state of the user's shopping cart,
     * including product details and total price.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        log.debug("GET /cart - Fetching cart for userId: {}", userId);
        CartResponse cart = cartService.getCart(userId);
        log.info("Cart retrieved - userId: {} - totalItems: {} - totalPrice: {}", userId, cart.getItems().size(), cart.getTotalPrice());
        return ResponseEntity.ok(cart);
    }

    /**
     * Flushes all items from the user's cart.
     * Typically called after a successful order placement.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clear(@RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /cart/clear - Clearing cart for userId: {}", userId);
        cartService.clearCart(userId);
        log.info("Cart cleared - userId: {}", userId);
        return ResponseEntity.ok("Cleared");
    }
}