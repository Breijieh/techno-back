package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an item category in the warehouse management system.
 * Categories are used to organize and classify store items.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Entity
@Table(name = "ITEM_CATEGORIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_code")
    private Long categoryCode;

    @Column(name = "category_name", nullable = false, length = 200)
    private String categoryName;

    @Column(name = "category_description", length = 500)
    private String categoryDescription;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoreItem> items = new ArrayList<>();

    /**
     * Check if category is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Get total number of items in this category
     */
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
