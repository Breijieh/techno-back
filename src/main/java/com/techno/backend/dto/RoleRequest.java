package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Request DTO
 * Used for creating and updating roles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "اسم الدور مطلوب")
    @Size(max = 100, message = "اسم الدور لا يجب أن يتجاوز 100 حرف")
    private String roleName;

    @Size(max = 500, message = "الوصف لا يجب أن يتجاوز 500 حرف")
    private String description;

    private Boolean canManageEmployees;
    private Boolean canManageAttendance;
    private Boolean canManageLeave;
    private Boolean canManageLoans;
    private Boolean canManagePayroll;
    private Boolean canManageProjects;
    private Boolean canManageWarehouse;
    private Boolean canViewReports;
    private Boolean canApprove;
    private Boolean canManageSettings;
}

