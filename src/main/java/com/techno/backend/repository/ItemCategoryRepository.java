package com.techno.backend.repository;

import com.techno.backend.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ItemCategory entity.
 * Provides database operations for managing item categories.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    /**
     * Find category by code excluding soft-deleted records
     */
    @Query("SELECT c FROM ItemCategory c WHERE c.categoryCode = :categoryCode AND c.isDeleted = false")
    Optional<ItemCategory> findById(Long categoryCode);

    /**
     * Find all active categories
     */
    @Query("SELECT c FROM ItemCategory c WHERE c.isActive = true AND c.isDeleted = false ORDER BY c.categoryName")
    List<ItemCategory> findAllActive();

    /**
     * Find all categories excluding soft-deleted
     */
    @Query("SELECT c FROM ItemCategory c WHERE c.isDeleted = false ORDER BY c.categoryName")
    List<ItemCategory> findAll();

    /**
     * Check if category name (Arabic) already exists
     */
    @Query("SELECT COUNT(c) > 0 FROM ItemCategory c WHERE c.categoryName = :name AND c.isDeleted = false")
    boolean existsByCategoryName(String name);

    /**
     * Check if category name exists excluding specific category (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM ItemCategory c WHERE c.categoryName = :name AND c.categoryCode != :categoryCode AND c.isDeleted = false")
    boolean existsByNameExcludingId(String name, Long categoryCode);
}
