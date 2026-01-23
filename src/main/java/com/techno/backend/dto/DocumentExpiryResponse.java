package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for employees with expiring documents (passport or residency).
 * Used in GET /api/employees/expiring-docs endpoint.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentExpiryResponse {

    private Long employeeNo;
    private String employeeName;
    private String nationalId;
    private String employeeCategory;

    // Passport information
    private String passportNo;
    private LocalDate passportExpiryDate;
    private Long daysUntilPassportExpiry;
    private Boolean passportExpired;
    private Boolean passportExpiringSoon;

    // Residency information
    private String residencyNo;
    private LocalDate residencyExpiryDate;
    private Long daysUntilResidencyExpiry;
    private Boolean residencyExpired;
    private Boolean residencyExpiringSoon;

    // Contact information
    private String email;
    private String mobile;

    // Department/Project
    private Long primaryDeptCode;
    private String primaryDeptName;
    private Long primaryProjectCode;
    private String primaryProjectName;

    /**
     * Get alert severity level for passport
     * CRITICAL: Expired or expiring within 7 days
     * HIGH: Expiring within 14 days
     * MEDIUM: Expiring within 30 days
     */
    public String getPassportAlertLevel() {
        if (passportExpired != null && passportExpired) {
            return "حرج";
        }
        if (daysUntilPassportExpiry == null) {
            return "لا يوجد";
        }
        if (daysUntilPassportExpiry <= 7) {
            return "حرج";
        }
        if (daysUntilPassportExpiry <= 14) {
            return "عالي";
        }
        if (daysUntilPassportExpiry <= 30) {
            return "متوسط";
        }
        return "منخفض";
    }

    /**
     * Get alert severity level for residency
     */
    public String getResidencyAlertLevel() {
        if (residencyExpired != null && residencyExpired) {
            return "حرج";
        }
        if (daysUntilResidencyExpiry == null) {
            return "لا يوجد";
        }
        if (daysUntilResidencyExpiry <= 7) {
            return "حرج";
        }
        if (daysUntilResidencyExpiry <= 14) {
            return "عالي";
        }
        if (daysUntilResidencyExpiry <= 30) {
            return "متوسط";
        }
        return "منخفض";
    }
}
