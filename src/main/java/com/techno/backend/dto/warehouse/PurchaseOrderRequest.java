package com.techno.backend.dto.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    @NotNull(message = "رمز المخزن مطلوب")
    private Long storeCode;

    @NotNull(message = "تاريخ أمر الشراء مطلوب")
    private LocalDate poDate;

    private LocalDate expectedDeliveryDate;

    @NotBlank(message = "اسم المورد مطلوب")
    @Size(max = 250, message = "اسم المورد لا يجب أن يتجاوز 250 حرفاً")
    private String supplierName;

    @NotEmpty(message = "سطور الطلب مطلوبة")
    @Valid
    private List<PurchaseOrderLineRequest> orderLines;

    @Size(max = 1000, message = "ملاحظات الموافقة لا يجب أن تتجاوز 1000 حرف")
    private String approvalNotes;
}

