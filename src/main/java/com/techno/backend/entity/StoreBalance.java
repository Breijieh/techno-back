package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the balance of an item in a specific warehouse.
 * Tracks quantity on hand, reserved quantity, and last transaction date.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Entity
@Table(name = "STORE_BALANCES",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_code", "item_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_code", nullable = false)
    private ProjectStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", nullable = false)
    private StoreItem item;

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Get available quantity (on hand - reserved)
     */
    public BigDecimal getAvailableQuantity() {
        BigDecimal reserved = quantityReserved != null ? quantityReserved : BigDecimal.ZERO;
        return quantityOnHand.subtract(reserved);
    }

    /**
     * Check if there is sufficient quantity available
     */
    public boolean hasSufficientQuantity(BigDecimal required) {
        if (required == null) {
            return true;
        }
        return getAvailableQuantity().compareTo(required) >= 0;
    }

    /**
     * Check if balance is zero
     */
    public boolean isZeroBalance() {
        return quantityOnHand.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if item is below reorder level
     */
    public boolean isBelowReorderLevel() {
        if (item == null || item.getReorderLevel() == null) {
            return false;
        }
        return quantityOnHand.compareTo(item.getReorderLevel()) < 0;
    }
}
