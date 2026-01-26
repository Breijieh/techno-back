package com.techno.backend.service;

import com.techno.backend.config.JwtConfig;
import com.techno.backend.dto.LoginRequest;
import com.techno.backend.dto.LoginResponse;
import com.techno.backend.dto.RefreshTokenRequest;
import com.techno.backend.dto.RegisterRequest;
import com.techno.backend.dto.UserInfoResponse;
import com.techno.backend.entity.UserAccount;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.exception.UnauthorizedException;
import com.techno.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles authentication operations: login, register, refresh token, logout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    private final JwtConfig jwtConfig;
    private final com.techno.backend.repository.EmployeeRepository employeeRepository;

    /**
     * Authenticate user and return JWT tokens
     * 
     * @param request the login request
     * @return LoginResponse with tokens
     * @throws UnauthorizedException if credentials are invalid or user is inactive
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Find user by username or National ID
            UserAccount user = userService.findByUsernameOrNationalId(request.getUsername());

            // Check if user is active
            if (user.getIsActive() == null || user.getIsActive() != 'Y') {
                throw new UnauthorizedException("حساب المستخدم غير نشط");
            }

            // Authenticate using Spring Security
            // Use the actual username from the found user account, not the identifier
            // passed in
            // This is important because UserDetailsService returns UserDetails with the
            // username field
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), // Use actual username, not the identifier
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = tokenProvider.generateToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            // Update last login
            userService.updateLastLogin(user.getUserId());

            log.info("User logged in successfully: {}", user.getUsername());

            return LoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .type("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .userType(user.getUserType().name())
                    .expiresIn(jwtConfig.getExpiration())
                    .build();

        } catch (ResourceNotFoundException e) {
            log.error("User not found: {}", request.getUsername());
            throw new UnauthorizedException("اسم المستخدم أو كلمة المرور غير صحيحة");
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getUsername());
            throw new UnauthorizedException("اسم المستخدم أو كلمة المرور غير صحيحة");
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new UnauthorizedException("فشل المصادقة: " + e.getMessage());
        }
    }

    /**
     * Register new user (admin only)
     * 
     * @param request the registration request
     * @return UserInfoResponse
     */
    @Transactional
    public UserInfoResponse register(RegisterRequest request) {
        UserAccount user = userService.createUser(request);

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .build();
    }

    /**
     * Refresh access token using refresh token
     * 
     * @param request the refresh token request
     * @return LoginResponse with new tokens
     * @throws UnauthorizedException if refresh token is invalid
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("رمز التحديث غير صالح");
        }

        // Check if it's actually a refresh token
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("الرمز ليس رمز تحديث");
        }

        // Extract username from token
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserAccount user = userService.findByUsername(username);

        // Check if user is still active
        if (user.getIsActive() == null || user.getIsActive() != 'Y') {
            throw new UnauthorizedException("حساب المستخدم غير نشط");
        }

        // Generate new tokens
        String newAccessToken = tokenProvider.generateToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .type("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .userType(user.getUserType().name())
                .expiresIn(jwtConfig.getExpiration())
                .build();
    }

    /**
     * Logout user (stateless - client removes token)
     * 
     * @param token the JWT token (optional, for logging)
     * @return success message
     */
    public String logout(String token) {
        // In a stateless JWT system, logout is handled client-side
        // The client simply removes the token
        // Optionally, we could maintain a blacklist of tokens here
        log.info("User logged out");
        return "تم تسجيل الخروج بنجاح";
    }

    /**
     * Get current authenticated user information
     * 
     * @return UserInfoResponse
     * @throws UnauthorizedException if user not authenticated
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("getCurrentUser - Authentication: {}, Authenticated: {}",
                authentication != null ? authentication.getName() : "null",
                authentication != null ? authentication.isAuthenticated() : false);

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            log.error("User not authenticated or anonymous user detected");
            throw new UnauthorizedException("المستخدم غير مصادق عليه");
        }

        UserAccount user;
        Object principal = authentication.getPrincipal();

        // The JWT filter sets the principal to employee number if available, otherwise
        // username
        if (principal instanceof Long) {
            // Principal is employee number
            Long employeeNo = (Long) principal;
            log.debug("Getting user info for employee number: {}", employeeNo);
            user = userService.findByEmployeeNo(employeeNo);
        } else if (principal instanceof String) {
            // Principal is username
            String username = (String) principal;
            log.debug("Getting user info for username: {}", username);
            user = userService.findByUsername(username);
        } else {
            log.error("Unexpected principal type: {}", principal != null ? principal.getClass() : "null");
            throw new UnauthorizedException("رئيس المصادقة غير صالح");
        }

        UserInfoResponse.UserInfoResponseBuilder builder = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime());

        // Add contract type if employee exists
        if (user.getEmployeeNo() != null) {
            try {
                employeeRepository.findById(user.getEmployeeNo())
                        .ifPresent(emp -> builder.empContractType(emp.getEmpContractType()));
            } catch (Exception e) {
                log.warn("Could not fetch employee details for user: {}", user.getUsername());
            }
        }

        return builder.build();
    }
}
