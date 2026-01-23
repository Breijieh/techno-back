package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLineRequest {

    @NotNull(message = "رمز الصنف مطلوب")
    private Long itemCode;

    @NotNull(message = "الكمية مطلوبة")
    @DecimalMin(value = "0.0001", message = "يجب أن تكون الكمية أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق الكمية غير صالح")
    private BigDecimal quantity;

    @NotNull(message = "سعر الوحدة مطلوب")
    @DecimalMin(value = "0.0", message = "يجب أن يكون سعر الوحدة أكبر من أو يساوي 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق سعر الوحدة غير صالح")
    private BigDecimal unitPrice;

    private String notes;
}

