package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceResponse {

    private Long balanceId;
    private Long storeCode;
    private String storeName;
    private Long itemCode;
    private String itemName;
    private String unitOfMeasure;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal availableQuantity;
    private LocalDateTime lastTransactionDate;
    private Boolean isBelowReorderLevel;
    private BigDecimal reorderLevel;

    public String getFormattedQuantityOnHand() {
        if (quantityOnHand == null) return "0";
        return String.format("%,.2f %s", quantityOnHand, unitOfMeasure != null ? unitOfMeasure : "");
    }

    public String getFormattedAvailableQuantity() {
        if (availableQuantity == null) return "0";
        return String.format("%,.2f %s", availableQuantity, unitOfMeasure != null ? unitOfMeasure : "");
    }
}
