package com.techno.backend.repository;

import com.techno.backend.entity.StoreItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StoreItem entity.
 * Provides database operations for managing store items.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Repository
public interface StoreItemRepository extends JpaRepository<StoreItem, Long> {

    /**
     * Find item by code excluding soft-deleted records
     */
    @Query("SELECT i FROM StoreItem i WHERE i.itemCode = :itemCode AND i.isDeleted = false")
    Optional<StoreItem> findById(Long itemCode);

    /**
     * Find all active items
     */
    @Query("SELECT i FROM StoreItem i WHERE i.isActive = true AND i.isDeleted = false ORDER BY i.itemName")
    List<StoreItem> findAllActive();

    /**
     * Find all items with pagination excluding soft-deleted
     */
    @Query("SELECT i FROM StoreItem i WHERE i.isDeleted = false")
    Page<StoreItem> findAll(Pageable pageable);

    /**
     * Find items by category with pagination
     */
    @Query("SELECT i FROM StoreItem i WHERE i.category.categoryCode = :categoryCode AND i.isDeleted = false")
    Page<StoreItem> findByCategoryCode(Long categoryCode, Pageable pageable);

    /**
     * Find items by category (all active)
     */
    @Query("SELECT i FROM StoreItem i WHERE i.category.categoryCode = :categoryCode AND i.isActive = true AND i.isDeleted = false ORDER BY i.itemName")
    List<StoreItem> findActiveByCategoryCode(Long categoryCode);

    /**
     * Search items by name (Arabic or English)
     */
    @Query("SELECT i FROM StoreItem i WHERE (LOWER(i.itemName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(i.itemName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND i.isDeleted = false")
    Page<StoreItem> searchByName(String searchTerm, Pageable pageable);

    /**
     * Check if item name (Arabic) already exists
     */
    @Query("SELECT COUNT(i) > 0 FROM StoreItem i WHERE i.itemName = :name AND i.isDeleted = false")
    boolean existsByItemName(String name);

    /**
     * Check if item name exists excluding specific item (for updates)
     */
    @Query("SELECT COUNT(i) > 0 FROM StoreItem i WHERE (i.itemName = :arName OR i.itemName = :enName) AND i.itemCode != :itemCode AND i.isDeleted = false")
    boolean existsByNameExcludingId(String arName, String enName, Long itemCode);

    /**
     * Count items by category code excluding deleted items
     */
    @Query("SELECT COUNT(i) FROM StoreItem i WHERE i.category.categoryCode = :categoryCode AND i.isDeleted = false")
    long countByCategoryCodeAndIsDeletedFalse(Long categoryCode);
}

