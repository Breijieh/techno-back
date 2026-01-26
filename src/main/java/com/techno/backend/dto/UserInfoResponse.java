package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * User Information Response DTO
 * Contains user information for /auth/me endpoint
 * Excludes sensitive data like password hash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private Long userId;
    private String username;
    private String nationalId;
    private String userType;
    private Long employeeNo;
    private Character isActive;
    private LocalDate lastLoginDate;
    private LocalTime lastLoginTime;
    private String empContractType;
}
