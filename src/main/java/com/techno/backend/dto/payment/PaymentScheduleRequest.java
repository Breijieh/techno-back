package com.techno.backend.dto.payment;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for adding a payment milestone/schedule to a project.
 * Used in POST /api/projects/{projectId}/payments endpoint.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentScheduleRequest {

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    @NotNull(message = "رقم التسلسل مطلوب")
    @Min(value = 1, message = "رقم التسلسل يجب أن يكون 1 على الأقل")
    @Max(value = 99, message = "رقم التسلسل لا يمكن أن يتجاوز 99")
    private Integer sequenceNo;

    @NotNull(message = "تاريخ الاستحقاق مطلوب")
    private LocalDate dueDate;

    @NotNull(message = "المبلغ المستحق مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ المستحق أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal dueAmount;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;
}
