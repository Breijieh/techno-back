package com.techno.backend.security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techno.backend.config.JwtConfig;
import com.techno.backend.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Provider
 * Handles JWT token generation, validation, and claim extraction
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfig jwtConfig;

    /**
     * Get the signing key from the secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token from UserAccount
     * 
     * @param userAccount the user account
     * @return JWT access token
     */
    public String generateToken(UserAccount userAccount) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userAccount.getUserId());
        claims.put("username", userAccount.getUsername());
        claims.put("userType", userAccount.getUserType().name());
        claims.put("employeeNo", userAccount.getEmployeeNo());
        
        return createToken(claims, userAccount.getUsername(), jwtConfig.getExpiration());
    }

    /**
     * Generate refresh token from UserAccount
     * 
     * @param userAccount the user account
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserAccount userAccount) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userAccount.getUserId());
        claims.put("username", userAccount.getUsername());
        claims.put("type", "refresh");
        
        return createToken(claims, userAccount.getUsername(), jwtConfig.getRefreshExpiration());
    }

    /**
     * Create JWT token with claims
     * 
     * @param claims the claims to include
     * @param subject the subject (username)
     * @param expiration the expiration time in milliseconds
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from token
     * 
     * @param token the JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     * 
     * @param token the JWT token
     * @return user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /**
     * Extract user type from token
     * 
     * @param token the JWT token
     * @return user type
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * Extract employee number from token
     * 
     * @param token the JWT token
     * @return employee number
     */
    public Long getEmployeeNoFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object employeeNo = claims.get("employeeNo");
        if (employeeNo instanceof Number) {
            return ((Number) employeeNo).longValue();
        }
        return null;
    }

    /**
     * Extract expiration date from token
     * 
     * @param token the JWT token
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     * 
     * @param token the JWT token
     * @param claimsResolver function to extract the claim
     * @param <T> the type of the claim
     * @return the claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Get all claims from token
     * 
     * @param token the JWT token
     * @return all claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     * 
     * @param token the JWT token
     * @return true if expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token
     * 
     * @param token the JWT token
     * @param username the username to validate against
     * @return true if valid
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token (without username check)
     * 
     * @param token the JWT token
     * @return true if valid
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is a refresh token
     * 
     * @param token the JWT token
     * @return true if refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}

