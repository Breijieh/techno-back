package com.techno.backend.repository;

import com.techno.backend.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TransactionType entity.
 * Provides database access methods for transaction type management.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {

    /**
     * Find all active transaction types
     */
    @Query("SELECT t FROM TransactionType t WHERE t.isActive = 'Y' ORDER BY t.typeCode ASC")
    List<TransactionType> findAllActive();

    /**
     * Find all allowance types (A)
     */
    @Query("SELECT t FROM TransactionType t WHERE t.allowanceDeduction = 'A' AND t.isActive = 'Y' ORDER BY t.typeCode ASC")
    List<TransactionType> findAllAllowances();

    /**
     * Find all deduction types (D)
     */
    @Query("SELECT t FROM TransactionType t WHERE t.allowanceDeduction = 'D' AND t.isActive = 'Y' ORDER BY t.typeCode ASC")
    List<TransactionType> findAllDeductions();

    /**
     * Find system-generated transaction types
     */
    @Query("SELECT t FROM TransactionType t WHERE t.isSystemGenerated = 'Y' AND t.isActive = 'Y' ORDER BY t.typeCode ASC")
    List<TransactionType> findAllSystemGenerated();

    /**
     * Find manual entry transaction types
     */
    @Query("SELECT t FROM TransactionType t WHERE t.isSystemGenerated = 'N' AND t.isActive = 'Y' ORDER BY t.typeCode ASC")
    List<TransactionType> findAllManualEntry();

    /**
     * Check if transaction type code exists
     */
    boolean existsByTypeCode(Long typeCode);

    /**
     * Find by type code (optional for non-existent types)
     */
    Optional<TransactionType> findByTypeCode(Long typeCode);
}
