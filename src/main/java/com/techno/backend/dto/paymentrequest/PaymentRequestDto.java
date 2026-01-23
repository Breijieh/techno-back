package com.techno.backend.dto.paymentrequest;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for submitting a supplier payment request.
 * Used in POST /api/payment-requests endpoint.
 * Requires multi-level approval before processing.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    @NotNull(message = "رمز المورد مطلوب")
    private Long supplierCode;

    @NotNull(message = "تاريخ الطلب مطلوب")
    private LocalDate requestDate;

    @NotNull(message = "مبلغ الدفع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal paymentAmount;

    @NotBlank(message = "غرض الدفع مطلوب")
    @Size(max = 500, message = "الغرض لا يجب أن يتجاوز 500 حرف")
    private String paymentPurpose;

    @Size(max = 500, message = "مسار المرفق لا يجب أن يتجاوز 500 حرف")
    private String attachmentPath;
}
