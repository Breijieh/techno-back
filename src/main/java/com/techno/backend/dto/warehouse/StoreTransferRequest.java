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
public class StoreTransferRequest {

    @NotNull(message = "رمز المخزن المصدر مطلوب")
    private Long fromStoreCode;

    @NotNull(message = "رمز المخزن الوجهة مطلوب")
    private Long toStoreCode;

    @NotNull(message = "تاريخ التحويل مطلوب")
    private LocalDate transferDate;

    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    private String notes;

    @NotEmpty(message = "سطور التحويل مطلوبة")
    @Valid
    private List<StoreTransferLineRequest> transferLines;
}

