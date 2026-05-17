package com.CaseStudyProject.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Swagger docs - public
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/users/v3/api-docs/**",
                                "/products/v3/api-docs/**",
                                "/orders/v3/api-docs/**",
                                "/cart/v3/api-docs/**",
                                "/payments/v3/api-docs/**"
                        ).permitAll()

                        // Public endpoints
                        .pathMatchers("/users/login", "/users").permitAll()
                        .pathMatchers("GET", "/products/**").permitAll()

                        // Everything else needs JWT
                        .anyExchange().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}