package com.techno.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for employee response data.
 * Returned from employee API endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {

    private Long employeeNo;
    private String employeeName;
    private String nationalId;
    private String nationality;
    private String employeeCategory;
    private String passportNo;
    private LocalDate passportExpiryDate;
    private String residencyNo;
    private LocalDate residencyExpiryDate;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String employmentStatus;
    private String terminationReason;
    private String empContractType;
    private Long primaryDeptCode;
    private String primaryDeptName;
    private Long primaryProjectCode;
    private String primaryProjectName;
    private BigDecimal monthlySalary;
    private BigDecimal leaveBalanceDays;
    private String email;
    private String mobile;

    // Additional calculated fields
    private Long yearsOfService;
    private Long monthsOfService;
    private Long daysUntilPassportExpiry;
    private Long daysUntilResidencyExpiry;
    private Boolean passportExpiringSoon;
    private Boolean residencyExpiringSoon;

    // Audit fields
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;

    /**
     * Get employee category display name
     */
    public String getEmployeeCategoryDisplay() {
        if ("S".equals(employeeCategory)) {
            return "سعودي";
        } else if ("F".equals(employeeCategory)) {
            return "أجنبي";
        }
        return employeeCategory;
    }

    /**
     * Get employment status display with icon
     */
    public String getEmploymentStatusDisplay() {
        return switch (employmentStatus != null ? employmentStatus : "") {
            case "ACTIVE" -> "نشط";
            case "TERMINATED" -> "منتهي";
            case "ON_LEAVE" -> "في إجازة";
            case "SUSPENDED" -> "معلق";
            default -> employmentStatus;
        };
    }
}
