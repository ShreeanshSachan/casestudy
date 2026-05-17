package com.CaseStudyProject.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;

/**
 * Utility class for JSON Web Token (JWT) operations.
 * Responsible for verifying token integrity and extracting user claims
 * for the API Gateway's security filter.
 */
@Component
public class JwtUtil {

    /**
     * Secret key used for signing and verifying tokens.
     * Note: In production, this should be moved to a secure configuration
     * like an environment variable or Spring Cloud Config/Vault.
     */
    private final String secretkey = "secretkeysecretkeysecretkeysecretkey";
    private final Key key = Keys.hmacShaKeyFor(secretkey.getBytes());

    /**
     * Validates the JWT and parses the payload.
     * Throws an exception if the token is expired, tampered with, or malformed.
     * * @param token The Bearer token (minus the "Bearer " prefix).
     * @return The claims contained within the token body.
     */
    public Claims validateToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the User ID claim from the validated token.
     */
    public Long getUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    /**
     * Extracts the Role claim (e.g., ADMIN, USER) from the validated token.
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}