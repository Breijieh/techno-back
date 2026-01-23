package com.techno.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Employee Contract Allowance Response DTO
 * Contains employee contract allowance information for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeContractAllowanceResponse {

    private Long recordId;
    private Long employeeNo;
    private String employeeName; // Optional, for display
    private Long transTypeCode;
    private String transactionName; // Optional, for display
    private BigDecimal salaryPercentage;
    private Boolean isActive; // Converted from Character for frontend
    private LocalDateTime createdDate;
}

