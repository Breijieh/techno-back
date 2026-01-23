package com.techno.backend.dto.warehouse;

import jakarta.validation.Valid;
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
public class GoodsReceiptRequest {

    @NotNull(message = "رمز المخزن مطلوب")
    private Long storeCode;

    @NotNull(message = "تاريخ الاستلام مطلوب")
    private LocalDate receiptDate;

    @NotNull(message = "نوع الاستلام مطلوب")
    @Size(max = 50, message = "نوع الاستلام لا يجب أن يتجاوز 50 حرفاً")
    private String receiptType; // "MANUAL" or "PO"

    private Long purchaseOrderId;

    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    private String notes;

    @NotEmpty(message = "سطور الاستلام مطلوبة")
    @Valid
    private List<GoodsReceiptLineRequest> receiptLines;
}

