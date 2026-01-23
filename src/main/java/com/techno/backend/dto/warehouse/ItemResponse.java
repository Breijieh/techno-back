package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for store item response data.
 * Returned from item API endpoints.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemResponse {

    private Long itemCode;
    private Long categoryCode;
    private String categoryName;
    private String itemName;
    private String unitOfMeasure;
    private String itemDescription;
    private BigDecimal reorderLevel;
    private Boolean isActive;
    private BigDecimal totalQuantity;
    private Boolean needsReorder;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

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
     * Get status display text
     */
    public String getStatusDisplay() {
        return Boolean.TRUE.equals(isActive) ? "Ù†Ø´Ø·" : "ØºÙŠØ± Ù†Ø´Ø·";
    }

    /**
     * Get status color for UI
     */
    public String getStatusColor() {
        return Boolean.TRUE.equals(isActive) ? "green" : "gray";
    }

    /**
     * Get formatted reorder level
     */
    public String getFormattedReorderLevel() {
        if (reorderLevel == null) {
            return "ØºÙŠØ± Ù…ØªØ§Ø­";
        }
        return String.format("%,.2f %s", reorderLevel, unitOfMeasure != null ? unitOfMeasure : "");
    }

    /**
     * Get formatted total quantity
     */
    public String getFormattedTotalQuantity() {
        if (totalQuantity == null) {
            return "0";
        }
        return String.format("%,.2f %s", totalQuantity, unitOfMeasure != null ? unitOfMeasure : "");
    }

    /**
     * Get stock status badge
     */
    public String getStockStatusBadge() {
        if (Boolean.TRUE.equals(needsReorder)) {
            return "Ù…Ø®Ø²ÙˆÙ† Ù…Ù†Ø®ÙØ¶";
        }
        if (totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "Ù†ÙØ¯ Ø§Ù„Ù…Ø®Ø²ÙˆÙ†";
        }
        return "Ù…ØªÙˆÙØ±";
    }

    /**
     * Get stock status color
     */
    public String getStockStatusColor() {
        if (Boolean.TRUE.equals(needsReorder)) {
            return "orange";
        }
        if (totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "red";
        }
        return "green";
    }
}

