package com.CaseStudyProject.api_gateway.filter;

import com.CaseStudyProject.api_gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for handling JWT authentication and route-level authorization.
 * Acts as the security gatekeeper for the entire microservices ecosystem.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtUtil jwt;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1. Skip security for public routes (Login, Register, Product Browsing)
        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header format
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validate Token integrity and extract claims
            Claims claims = jwt.validateToken(token);
            Long userId = jwt.getUserId(claims);
            String role = jwt.getRole(claims);

            // 4. Perform RBAC (Role-Based Access Control) at the Gateway level
            if (!isAuthorized(path, method, role)) {
                return forbidden(exchange);
            }

            // 5. Header Propagation: Inject user metadata for downstream services
            // This allows services like Order/Payment to know who the user is without re-validating JWT
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(builder -> builder
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Role", role)
                    )
                    .build();

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    /**
     * Determines if a request can bypass authentication.
     */
    private boolean isPublic(String path, HttpMethod method) {
        if (path.equals("/users/login")) return true;
        if (path.equals("/users") && method == HttpMethod.POST) return true; // Registration
        if (path.startsWith("/products") && method == HttpMethod.GET) return true; // Public catalog
        if (path.startsWith("/swagger-ui")) return true;
        if (path.startsWith("/v3/api-docs")) return true;
        if (path.startsWith("/webjars")) return true;

        // Service-specific docs
        if (path.contains("/v3/api-docs")) return true;

        // Auth endpoints
        if (path.equals("/users/login")) return true;
        if (path.equals("/users") && method == HttpMethod.POST) return true;

        // Product browsing
        if (path.startsWith("/products") && method == HttpMethod.GET) return true;
        return false;
    }

    /**
     * Centralized Authorization Matrix.
     */
    private boolean isAuthorized(String path, HttpMethod method, String role) {

        // PRODUCT-SERVICE: Admin can modify, everyone (auth) can read
        if (path.startsWith("/products")) {
            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE) {
                return "ADMIN".equals(role);
            }
            return true;
        }

        // ORDER-SERVICE: Admin full access, User limited to own orders
        if (path.startsWith("/orders")) {
            if ("ADMIN".equals(role)) return true;
            if ("USER".equals(role)) {
                return method == HttpMethod.POST ||
                        path.equals("/orders/my") ||
                        path.matches("/orders/\\d+");
            }
            return false;
        }

        // USER-SERVICE: Admin only for list/delete
        if (path.startsWith("/users")) {
            if ("ADMIN".equals(role)) return true;
            return !path.equals("/users") && method != HttpMethod.DELETE;
        }

        // CART-SERVICE: Requires authenticated session
        if (path.startsWith("/cart")) {
            return "USER".equals(role) || "ADMIN".equals(role);
        }

        // PAYMENT-SERVICE: Specific user endpoints
        if (path.startsWith("/payments")) {
            if ("ADMIN".equals(role)) return true;
            return "USER".equals(role) && (
                    (path.equals("/payments/process") && method == HttpMethod.POST) ||
                            (path.equals("/payments/user") && method == HttpMethod.GET) ||
                            (method == HttpMethod.GET && path.matches("/payments/\\d+"))
            );
        }

        return true;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}