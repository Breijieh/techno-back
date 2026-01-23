package com.techno.backend.dto.payment;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for recording an actual payment against a payment schedule.
 * Used in POST /api/projects/payments/{id}/record endpoint.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecordRequest {

    @NotNull(message = "مبلغ الدفع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون مبلغ الدفع أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal paymentAmount;

    @NotNull(message = "تاريخ الدفع مطلوب")
    private LocalDate paymentDate;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;
}
