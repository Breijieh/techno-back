package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating or updating a store item.
 * Used in POST and PUT endpoints for item management.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequest {

    @NotNull(message = "رمز الفئة مطلوب")
    private Long categoryCode;

    @NotBlank(message = "اسم الصنف بالعربية مطلوب")
    @Size(max = 250, message = "اسم الصنف بالعربية لا يجب أن يتجاوز 250 حرفاً")
    private String itemName;

    @NotBlank(message = "اسم الصنف بالإنجليزية مطلوب")
    @Size(max = 250, message = "اسم الصنف بالإنجليزية لا يجب أن يتجاوز 250 حرفاً")

    @NotBlank(message = "وحدة القياس مطلوبة")
    @Size(max = 50, message = "وحدة القياس لا يجب أن تتجاوز 50 حرفاً")
    private String unitOfMeasure;

    @Size(max = 500, message = "وصف الصنف لا يجب أن يتجاوز 500 حرف")
    private String itemDescription;

    @DecimalMin(value = "0.0", message = "مستوى إعادة الطلب يجب أن يكون أكبر من أو يساوي 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق مستوى إعادة الطلب غير صالح")
    private BigDecimal reorderLevel;

    private Boolean isActive;

    /**
     * Initial quantity to set when creating/updating item.
     * If provided, storeCode must also be provided.
     */
    @DecimalMin(value = "0.0", message = "الكمية الأولية يجب أن تكون أكبر من أو تساوي 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق الكمية الأولية غير صالح")
    private BigDecimal initialQuantity;

    /**
     * Store code where initial quantity should be set.
     * Required if initialQuantity is provided.
     */
    private Long storeCode;
}
