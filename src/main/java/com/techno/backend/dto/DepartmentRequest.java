package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Department Request DTO
 * Used for creating and updating departments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "اسم القسم بالعربية مطلوب")
    @Size(max = 250, message = "اسم القسم بالعربية لا يجب أن يتجاوز 250 حرفاً")
    private String deptName;

    private Long parentDeptCode;

    private Long deptMgrCode;
}
