package com.techno.backend.dto.labor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for submitting labor request.
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborRequestDto {

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    @NotNull(message = "تاريخ البدء مطلوب")
    private LocalDate startDate;

    @NotNull(message = "تاريخ الانتهاء مطلوب")
    private LocalDate endDate;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;

    @NotNull(message = "تفاصيل الطلب مطلوبة")
    @NotEmpty(message = "يجب أن يكون هناك سطر تفاصيل واحد على الأقل")
    @Valid
    private List<LaborRequestDetailDto> details;
}
