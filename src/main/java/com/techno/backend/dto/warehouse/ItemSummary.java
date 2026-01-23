package com.techno.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for lightweight item summary.
 * Used in list views and dropdown selections.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSummary {

    private Long itemCode;
    private Long categoryCode;
    private String categoryName;
    private String itemName;
    private String unitOfMeasure;
    private BigDecimal reorderLevel;
    private Boolean isActive;
    private BigDecimal totalQuantity;

    /**
     * Get item name in specified language
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Get category name in specified language
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Get formatted quantity
     */
    public String getFormattedQuantity() {
        if (totalQuantity == null) {
            return "0";
        }
        return String.format("%,.2f %s", totalQuantity, unitOfMeasure != null ? unitOfMeasure : "");
    }
}

