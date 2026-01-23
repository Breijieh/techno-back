package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an item in the warehouse management system.
 * Items belong to categories and can be stored in multiple warehouses.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Entity
@Table(name = "STORE_ITEMS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_code")
    private Long itemCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code", nullable = false)
    private ItemCategory category;

    @Column(name = "item_name", nullable = false, length = 250)
    private String itemName;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    private String unitOfMeasure; // EA, KG, L, M, etc.

    @Column(name = "item_description", length = 500)
    private String itemDescription;

    @Column(name = "reorder_level", precision = 12, scale = 4)
    private BigDecimal reorderLevel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoreBalance> balances = new ArrayList<>();

    /**
     * Check if item is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Get the item name
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Check if item is below reorder level in any warehouse
     */
    public boolean needsReorder() {
        if (reorderLevel == null || balances == null) {
            return false;
        }
        return balances.stream()
                .anyMatch(balance -> balance.getQuantityOnHand().compareTo(reorderLevel) < 0);
    }

    /**
     * Get total quantity across all warehouses
     */
    public BigDecimal getTotalQuantity() {
        if (balances == null) {
            return BigDecimal.ZERO;
        }
        return balances.stream()
                .map(StoreBalance::getQuantityOnHand)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

