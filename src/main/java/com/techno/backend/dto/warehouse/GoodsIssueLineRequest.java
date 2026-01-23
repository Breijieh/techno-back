package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueLineRequest {

    @NotNull(message = "رمز الصنف مطلوب")
    private Long itemCode;

    @NotNull(message = "الكمية مطلوبة")
    @DecimalMin(value = "0.0001", message = "يجب أن تكون الكمية أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق الكمية غير صالح")
    private BigDecimal quantity;

    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    private String notes;
}

