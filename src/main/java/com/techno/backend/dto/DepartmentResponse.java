package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Department Response DTO
 * Used for returning department information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long deptCode;
    private String deptName;
    private Long parentDeptCode;
    private DepartmentResponse parentDepartment;
    private Long deptMgrCode;
    private String managerName;
    private Character isActive;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
    
    // For hierarchy tree structure
    private List<DepartmentResponse> children;
}


