package com.techno.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration for Techno ERP System
 * Configures CORS and other web-related settings
 * 
 * Note: UTF-8 encoding is handled by Spring Boot's auto-configuration
 * based on properties in application.properties:
 * - server.servlet.encoding.charset=UTF-8
 * - server.servlet.encoding.enabled=true
 * - server.servlet.encoding.force=true
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173") // React development servers
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

