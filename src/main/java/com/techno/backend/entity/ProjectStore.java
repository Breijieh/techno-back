package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a warehouse/store associated with a project.
 * Each project can have multiple warehouses for inventory management.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Entity
@Table(name = "PROJECT_STORES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_code")
    private Long storeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", nullable = false)
    private Project project;

    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    @Column(name = "store_location", length = 500)
    private String storeLocation;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "store_manager_id")
    private Long storeManagerId;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoreBalance> balances = new ArrayList<>();

    /**
     * Check if store is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Get the store name
     */
    public String getStoreName() {
        return storeName;
    }

    /**
     * Get total number of items in this store
     */
    public int getItemCount() {
        return balances != null ? balances.size() : 0;
    }
}

