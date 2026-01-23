package com.techno.backend.dto.labor;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for assigning labor to project.
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborAssignmentDto {

    @NotNull(message = "رقم الموظف مطلوب")
    private Long employeeNo;

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    private Long requestNo;
    private Integer sequenceNo;

    @NotNull(message = "تاريخ البدء مطلوب")
    private LocalDate startDate;

    @NotNull(message = "تاريخ الانتهاء مطلوب")
    private LocalDate endDate;

    @NotNull(message = "المعدل اليومي مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المعدل اليومي أكبر من 0")
    @Digits(integer = 10, fraction = 4, message = "تنسيق المعدل اليومي غير صالح")
    private BigDecimal dailyRate;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;
}
