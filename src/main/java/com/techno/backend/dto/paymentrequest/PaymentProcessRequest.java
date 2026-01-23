package com.techno.backend.dto.paymentrequest;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for processing an approved payment request.
 * Used in POST /api/payment-requests/{id}/process endpoint.
 * Finance processes the payment after all approvals.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessRequest {

    @NotNull(message = "تاريخ الدفع مطلوب")
    private LocalDate paymentDate;

    @NotNull(message = "المبلغ المدفوع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal paidAmount;

    @NotBlank(message = "طريقة الدفع مطلوبة")
    @Pattern(regexp = "^(BANK_TRANSFER|CHECK|CASH|ONLINE)$",
             message = "الطريقة يجب أن تكون BANK_TRANSFER أو CHECK أو CASH أو ONLINE")
    private String paymentMethod;

    @Size(max = 100, message = "رقم المرجع لا يجب أن يتجاوز 100 حرف")
    private String referenceNo;

    @Size(max = 200, message = "اسم البنك لا يجب أن يتجاوز 200 حرف")
    private String bankName;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String processNotes;
}
