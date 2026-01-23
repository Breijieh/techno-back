package com.techno.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reset Password Request DTO
 * Used for resetting user passwords
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @Size(min = 6, message = "يجب أن تكون كلمة المرور 6 أحرف على الأقل")
    private String newPassword;

    private Boolean generatePassword; // If true, generate random password instead of using newPassword
}

