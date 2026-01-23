package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRejectionRequest {

    @NotBlank(message = "سبب الرفض مطلوب")
    @Size(max = 1000, message = "سبب الرفض لا يجب أن يتجاوز 1000 حرف")
    private String notes;
}

