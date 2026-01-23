package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Response DTO
 * Contains role information with permissions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private Long roleId;
    private String roleName;
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
    private Character isActive;
}

