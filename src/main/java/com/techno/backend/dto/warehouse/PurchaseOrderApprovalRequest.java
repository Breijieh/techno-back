package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderApprovalRequest {

    @Size(max = 1000, message = "ملاحظات الموافقة لا يجب أن تتجاوز 1000 حرف")
    private String notes;
}

