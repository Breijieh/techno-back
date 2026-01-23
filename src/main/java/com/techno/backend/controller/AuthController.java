package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.LoginRequest;
import com.techno.backend.dto.LoginResponse;
import com.techno.backend.dto.RefreshTokenRequest;
import com.techno.backend.dto.RegisterRequest;
import com.techno.backend.dto.UserInfoResponse;
import com.techno.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles authentication-related endpoints
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * User login endpoint
     * 
     * @param request the login request
     * @return LoginResponse with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("تم تسجيل الدخول بنجاح", response));
    }

    /**
     * POST /api/auth/register
     * Register new user (admin only)
     * 
     * @param request the registration request
     * @return UserInfoResponse
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());
        UserInfoResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم تسجيل المستخدم بنجاح", response));
    }

    /**
     * GET /api/auth/me
     * Get current authenticated user information
     * 
     * @return UserInfoResponse
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser() {
        UserInfoResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع معلومات المستخدم بنجاح", response));
    }

    /**
     * POST /api/auth/refresh
     * Refresh access token using refresh token
     * 
     * @param request the refresh token request
     * @return LoginResponse with new tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الرمز المميز بنجاح", response));
    }

    /**
     * POST /api/auth/logout
     * Logout endpoint (stateless - client removes token)
     * 
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        String message = authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }
}

