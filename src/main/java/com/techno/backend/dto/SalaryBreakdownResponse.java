package com.techno.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for salary breakdown percentage response data.
 * Returned from salary structure API endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalaryBreakdownResponse {

    private Long serNo;
    private String employeeCategory;
    private Long transTypeCode;
    private String transTypeName;
    private BigDecimal salaryPercentage;
    private String isDeleted;

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
     * Convert percentage to display format (e.g., 0.8340 → "83.40%")
     */
    public String getPercentageDisplay() {
        if (salaryPercentage == null) {
            return "0.00%";
        }
        return salaryPercentage.multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }
}
