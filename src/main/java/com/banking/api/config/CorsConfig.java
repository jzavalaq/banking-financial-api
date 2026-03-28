package com.banking.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for the Banking API.
 *
 * Never use allowAll() or "*" for production.
 * Note: @Value injection on fields is acceptable for configuration properties
 * as these are typically set during bean initialization and don't change.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins (comma-separated)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Parse allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // Parse allowed headers - avoid wildcard for security
        if ("*".equals(allowedHeaders.trim())) {
            // Instead of wildcard, explicitly list common headers
            configuration.setAllowedHeaders(Arrays.asList(
                    "Authorization",
                    "Content-Type",
                    "Accept",
                    "Origin",
                    "Access-Control-Request-Method",
                    "Access-Control-Request-Headers",
                    "X-Trace-Id",
                    "X-Requested-With"
            ));
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }

        configuration.setAllowCredentials(allowCredentials);

        // Expose headers that frontend might need
        configuration.setExposedHeaders(List.of(
                "X-Trace-Id",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
