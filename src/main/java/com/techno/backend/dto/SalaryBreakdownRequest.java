package com.techno.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating or updating salary breakdown percentages.
 * Used in POST /api/salary-structure endpoint.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryBreakdownRequest {

    @NotNull(message = "فئة الموظف مطلوبة")
    @Pattern(regexp = "^[SF]$", message = "فئة الموظف يجب أن تكون 'S' (سعودي) أو 'F' (أجنبي)")
    private String employeeCategory;

    @NotNull(message = "رمز نوع المعاملة مطلوب")
    private Long transTypeCode;

    @NotNull(message = "نسبة الراتب مطلوبة")
    @DecimalMin(value = "0.0", message = "النسبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "1.0", message = "النسبة لا يمكن أن تتجاوز 100%")
    @Digits(integer = 1, fraction = 4, message = "تنسيق النسبة غير صالح (حد أقصى 4 منازل عشرية)")
    private BigDecimal salaryPercentage;
}
