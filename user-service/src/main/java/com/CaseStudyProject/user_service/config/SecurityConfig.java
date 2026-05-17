package com.CaseStudyProject.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class to define security constraints and password encoding for the User Service.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     * Currently, disables CSRF and permits all requests, including Swagger documentation.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disabling CSRF as it is typically handled differently in stateless microservices
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Currently allowing all other requests
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * Defines a BCryptPasswordEncoder bean for hashing and verifying passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}