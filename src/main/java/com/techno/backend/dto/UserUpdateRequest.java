package com.techno.backend.dto;

import com.techno.backend.entity.UserAccount;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Update Request DTO
 * Used for updating existing user accounts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "يجب أن يكون اسم المستخدم بين 3 و 50 حرفاً")
    private String username;

    private UserAccount.UserType userType;

    private Long employeeNo;

    private Boolean isActive;

    @Size(min = 6, message = "يجب أن تكون كلمة المرور 6 أحرف على الأقل")
    private String password; // Optional - only update if provided

    private Long assignedProjectId; // For Project Manager
    private java.util.List<Long> assignedProjectIds; // For Regional Project Manager
}
