package com.techno.backend.dto.labor;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for labor request detail line item.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborRequestDetailDto {

    @NotNull(message = "رقم التسلسل مطلوب")
    @Min(value = 1, message = "يجب أن يكون التسلسل 1 على الأقل")
    private Integer sequenceNo;

    @NotBlank(message = "المسمى الوظيفي بالعربية مطلوب")
    @Size(max = 200, message = "المسمى الوظيفي بالعربية لا يجب أن يتجاوز 200 حرف")
    private String jobTitleAr;

    @NotBlank(message = "المسمى الوظيفي بالإنجليزية مطلوب")
    @Size(max = 200, message = "المسمى الوظيفي بالإنجليزية لا يجب أن يتجاوز 200 حرف")
    private String jobTitleEn;

    @NotNull(message = "الكمية مطلوبة")
    @Min(value = 1, message = "يجب أن تكون الكمية 1 على الأقل")
    private Integer quantity;

    @NotNull(message = "المعدل اليومي مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المعدل اليومي أكبر من 0")
    @Digits(integer = 10, fraction = 4, message = "تنسيق المعدل اليومي غير صالح")
    private BigDecimal dailyRate;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;
}
