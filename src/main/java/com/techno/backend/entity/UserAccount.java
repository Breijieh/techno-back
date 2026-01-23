package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * User Account Entity
 * Represents system users with authentication and authorization
 * Based on USER_ACCOUNTS table from DOCUMNET.MD
 */
@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "national_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "اسم المستخدم مطلوب")
    @Size(max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "كلمة المرور مطلوبة")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "رقم الهوية الوطنية مطلوب")
    @Size(max = 20)
    @Column(name = "national_id", nullable = false, unique = true, length = 20)
    private String nationalId;

    @NotNull(message = "نوع المستخدم مطلوب")
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';

    @Column(name = "employee_no")
    private Long employeeNo;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Convert(converter = com.techno.backend.converter.SafeLocalTimeConverter.class)
    @Column(name = "last_login_time", length = 8)
    private LocalTime lastLoginTime;

    /**
     * User Types (Roles) in the system
     * Each role has specific permissions as defined in the Role Permissions Matrix
     */
    public enum UserType {
        ADMIN, // System Administrator - Full access to all modules
        GENERAL_MANAGER, // General Manager - Strategic oversight, View most modules, Approve payroll
        HR_MANAGER, // HR Manager - Manage attendance, Approve leave/loans/payroll
        FINANCE_MANAGER, // Finance Manager - Approve loans and payroll, View financial data
        PROJECT_MANAGER, // Project Manager - Full project management, Approve team leave, Request
                         // warehouse
        WAREHOUSE_MANAGER, // Warehouse Manager - Full warehouse/inventory control
        EMPLOYEE, // Regular Employee - Self-service access (own data only)
        PROJECT_SECRETARY, // Project Secretary - Administrative support, Attendance management
        PROJECT_ADVISOR, // Project Advisor - Consult and Review only
        REGIONAL_PROJECT_MANAGER // Regional Project Manager - Multi-project oversight
    }

    // Auto-generated Lombok methods
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public UserType getUserType() { return userType; }
    public Long getEmployeeNo() { return employeeNo; }

    // Auto-generated Setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setIsActive(Character isActive) { this.isActive = isActive; }
    public void setEmployeeNo(Long employeeNo) { this.employeeNo = employeeNo; }
    public void setLastLoginDate(java.time.LocalDate lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    public void setLastLoginTime(java.time.LocalTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
}
