package com.techno.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for transaction type response data.
 * Returned from transaction type API endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionTypeResponse {

    private Long typeCode;
    private String typeName;
    private String allowanceDeduction;
    private String isSystemGenerated;
    private String isActive;
    private LocalDateTime createdDate;
    private String createdBy;

    /**
     * Get display text for allowance/deduction
     */
    public String getAllowanceDeductionDisplay() {
        if ("A".equals(allowanceDeduction)) {
            return "بدل";
        } else if ("D".equals(allowanceDeduction)) {
            return "خصم";
        }
        return allowanceDeduction;
    }
}
