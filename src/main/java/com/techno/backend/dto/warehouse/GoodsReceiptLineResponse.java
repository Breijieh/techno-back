package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoodsReceiptLineResponse {

    private Long lineId;
    private Long itemCode;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal unitPrice; // Unit price from PurchaseOrder (if receipt is linked to PO)
    private BigDecimal lineTotal; // Calculated total: quantity * unitPrice (if available)
    private String notes;
}

