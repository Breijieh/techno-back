package com.techno.backend.dto;

import com.techno.backend.entity.UserAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO
 * Used for creating new user accounts (admin only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "اسم المستخدم مطلوب")
    private String username;

    @NotBlank(message = "كلمة المرور مطلوبة")
    private String password;

    @NotBlank(message = "رقم الهوية الوطنية مطلوب")
    private String nationalId;

    @NotNull(message = "نوع المستخدم مطلوب")
    private UserAccount.UserType userType;

    private Long employeeNo;

    private Long assignedProjectId; // For Project Manager
    private java.util.List<Long> assignedProjectIds; // For Regional Project Manager
}
