package com.CaseStudyProject.user_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utility class for generating and managing JSON Web Tokens (JWT).
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    // Secret key used for signing the JWT
    private final String secretkey = "secretkeysecretkeysecretkeysecretkey";

    // Token expiration time set to 1 hour (1000ms * 60s * 60m)
    private final long expiry = 1000 * 60 * 60;

    // Cryptographic key generated from the secret string using HMAC-SHA algorithm
    private final Key key = Keys.hmacShaKeyFor(secretkey.getBytes());

    /**
     * Creates a signed JWT containing user identity and authorization claims.
     * id The unique database ID of the user.
     * email The user's email, used as the 'subject' of the token.
     * role The user's assigned role for access control.
     * returns A compact, URL-safe JWT string.
     */
    public String generateToken(Long id, String email, String role){

        return Jwts.builder()
                .setSubject(email) // Principal identifier
                .claim("userId", id) // Custom claim for internal user reference
                .claim("role", role) // Custom claim for role-based authorization
                .setIssuedAt(new Date()) // Creation timestamp
                .setExpiration(new Date(System.currentTimeMillis() + expiry)) // Validity period
                .signWith(key) // Digital signature to prevent tampering
                .compact(); // Final serialization to String
    }

}