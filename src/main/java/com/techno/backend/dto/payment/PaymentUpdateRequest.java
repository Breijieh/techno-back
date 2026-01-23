package com.techno.backend.dto.payment;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating payment schedule details.
 * Used in PUT /api/projects/payments/{id} endpoint.
 * All fields are optional to support partial updates.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentUpdateRequest {

    private LocalDate dueDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ المستحق أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal dueAmount;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;

    /**
     * Check if any field is being updated
     */
    public boolean hasUpdates() {
        return dueDate != null || dueAmount != null || notes != null;
    }
}
