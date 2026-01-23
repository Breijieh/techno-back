package com.techno.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Employee Contract Allowance Request DTO
 * Used for creating and updating employee contract allowances
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeContractAllowanceRequest {

    @NotNull(message = "رقم الموظف مطلوب")
    private Long employeeNo;

    @NotNull(message = "رمز نوع المعاملة مطلوب")
    private Long transTypeCode;

    @NotNull(message = "نسبة الراتب مطلوبة")
    @DecimalMin(value = "0.0", message = "النسبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "100.0", message = "النسبة لا يمكن أن تتجاوز 100")
    private BigDecimal salaryPercentage;

    @NotNull(message = "حالة النشاط مطلوبة")
    private Boolean isActive;
}

