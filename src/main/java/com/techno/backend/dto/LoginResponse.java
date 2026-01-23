package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Response DTO
 * Contains JWT tokens and user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String userType;
    private Long expiresIn;
}

