package com.techno.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating transaction types.
 * Used in POST /api/transaction-types and PUT /api/transaction-types/{code} endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTypeRequest {

    @NotNull(message = "رمز نوع المعاملة مطلوب")
    private Long typeCode;

    @NotBlank(message = "اسم نوع المعاملة بالعربية مطلوب")
    @Size(max = 250, message = "الاسم العربي لا يجب أن يتجاوز 250 حرفاً")
    private String typeName;

    @NotBlank(message = "اسم نوع المعاملة بالإنجليزية مطلوب")
    @Size(max = 250, message = "الاسم الإنجليزي لا يجب أن يتجاوز 250 حرفاً")

    @NotNull(message = "فئة المعاملة مطلوبة")
    @Pattern(regexp = "^[AD]$", message = "فئة المعاملة يجب أن تكون 'A' (بدل) أو 'D' (خصم)")
    private String allowanceDeduction;

    @NotNull(message = "علامة التوليد التلقائي مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة التوليد التلقائي يجب أن تكون 'Y' أو 'N'")
    @Builder.Default
    private String isSystemGenerated = "N";

    @NotNull(message = "حالة النشاط مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون 'Y' أو 'N'")
    @Builder.Default
    private String isActive = "Y";
}
