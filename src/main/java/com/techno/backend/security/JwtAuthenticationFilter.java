package com.techno.backend.security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techno.backend.config.JwtConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens
 * Sets SecurityContext if token is valid
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("JWT token extracted: {}", jwt != null ? "present" : "not present");

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                String userType = tokenProvider.getUserTypeFromToken(jwt);
                Long employeeNo = tokenProvider.getEmployeeNoFromToken(jwt);
                log.debug("Token validated for user: {}, type: {}, employeeNo: {}", username, userType, employeeNo);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Create authentication token with employee number as principal for easy access
                    // Store employee number as principal if available, otherwise use username
                    Object principal = (employeeNo != null) ? employeeNo : username;
                    
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userType);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(authority)
                    );
                    
                    // Store additional details in authentication details
                    WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();
                    authentication.setDetails(detailsSource.buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("SecurityContext set for user: {}, employeeNo: {}", username, employeeNo);
                }
            } else {
                log.debug("JWT token validation failed or token not present");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from request header
     * 
     * @param request the HTTP request
     * @return JWT token or null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.getHeaderName());
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtConfig.getTokenPrefix())) {
            String token = bearerToken.substring(jwtConfig.getTokenPrefix().length());
            // Remove ALL whitespace - JWT tokens must not contain any whitespace
            // This handles cases where tokens might have been stored with whitespace
            return token != null ? token.replaceAll("\\s+", "") : null;
        }
        
        return null;
    }
}

