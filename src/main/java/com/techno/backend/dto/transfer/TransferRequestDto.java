package com.techno.backend.dto.transfer;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for submitting employee transfer request.
 * Used in POST /api/transfers endpoint.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDto {

    @NotNull(message = "رقم الموظف مطلوب")
    private Long employeeNo;

    @NotNull(message = "رمز المشروع المصدر مطلوب")
    private Long fromProjectCode;

    @NotNull(message = "رمز المشروع الهدف مطلوب")
    private Long toProjectCode;

    @NotNull(message = "تاريخ النقل مطلوب")
    private LocalDate transferDate;

    @Size(max = 500, message = "سبب النقل لا يجب أن يتجاوز 500 حرف")
    private String transferReason;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String remarks;
}
