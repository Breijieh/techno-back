package com.techno.backend.repository;

import com.techno.backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Supplier entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    /**
     * Find suppliers by active status
     * @param isActive the active status ('Y' or 'N')
     * @return List of suppliers
     */
    List<Supplier> findByIsActive(Character isActive);
    
    /**
     * Find active suppliers
     * @return List of active suppliers
     */
    default List<Supplier> findActiveSuppliers() {
        return findByIsActive('Y');
    }
    
    /**
     * Check if supplier name exists
     * @param supplierName the supplier name to check
     * @return true if supplier name exists
     */
    boolean existsBySupplierName(String supplierName);
    
    /**
     * Check if supplier name exists excluding a specific supplier ID
     * @param supplierName the supplier name to check
     * @param supplierId the supplier ID to exclude
     * @return true if supplier name exists for another supplier
     */
    boolean existsBySupplierNameAndSupplierIdNot(String supplierName, Long supplierId);
    
    /**
     * Find supplier by name
     * @param supplierName the supplier name
     * @return Optional containing the supplier if found
     */
    Optional<Supplier> findBySupplierName(String supplierName);
}

