package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 * Used for user authentication
 * 
 * The username field accepts either:
 * - Username (e.g., "admin")
 * - National ID / Residence ID (e.g., "1111111111")
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "اسم المستخدم أو رقم الهوية الوطنية مطلوب")
    private String username;

    @NotBlank(message = "كلمة المرور مطلوبة")
    private String password;
}

