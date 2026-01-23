package com.techno.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration
 * Loads JWT properties from application.properties
 * 
 * Token Expiration (per DOCUMNET.MD):
 * - Access Token: 24 hours (86400000 ms)
 * - Refresh Token: 7 days (604800000 ms)
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Value("${jwt.header-name}")
    private String headerName;

    public String getSecret() {
        return secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public Long getRefreshExpiration() {
        return refreshExpiration;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public String getHeaderName() {
        return headerName;
    }
}
