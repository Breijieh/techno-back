package com.techno.backend.repository;

import com.techno.backend.entity.SalaryBreakdownPercentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SalaryBreakdownPercentage entity.
 * Provides database access methods for salary breakdown management.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface SalaryBreakdownPercentageRepository extends JpaRepository<SalaryBreakdownPercentage, Long> {

    /**
     * Find all active salary breakdown percentages for a specific employee category
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.employeeCategory = :category AND s.isDeleted = 'N' ORDER BY s.transTypeCode ASC")
    List<SalaryBreakdownPercentage> findByEmployeeCategory(@Param("category") String category);

    /**
     * Find all salary breakdown percentages for Saudi employees (S)
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.employeeCategory = 'S' AND s.isDeleted = 'N' ORDER BY s.transTypeCode ASC")
    List<SalaryBreakdownPercentage> findSaudiBreakdown();

    /**
     * Find all salary breakdown percentages for Foreign employees (F)
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.employeeCategory = 'F' AND s.isDeleted = 'N' ORDER BY s.transTypeCode ASC")
    List<SalaryBreakdownPercentage> findForeignBreakdown();

    /**
     * Find specific breakdown by category and transaction type (active only)
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.employeeCategory = :category AND s.transTypeCode = :transTypeCode AND s.isDeleted = 'N'")
    Optional<SalaryBreakdownPercentage> findByEmployeeCategoryAndTransTypeCode(
            @Param("category") String category,
            @Param("transTypeCode") Long transTypeCode);

    /**
     * Find specific breakdown by category and transaction type (including
     * soft-deleted)
     * Used when creating/updating to check for existing records regardless of
     * deletion status
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.employeeCategory = :category AND s.transTypeCode = :transTypeCode")
    Optional<SalaryBreakdownPercentage> findByEmployeeCategoryAndTransTypeCodeIncludingDeleted(
            @Param("category") String category,
            @Param("transTypeCode") Long transTypeCode);

    /**
     * Check if breakdown exists for category and transaction type
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SalaryBreakdownPercentage s " +
            "WHERE s.employeeCategory = :category AND s.transTypeCode = :transTypeCode AND s.isDeleted = 'N'")
    boolean existsByEmployeeCategoryAndTransTypeCode(
            @Param("category") String category,
            @Param("transTypeCode") Long transTypeCode);

    /**
     * Find all active breakdowns (not deleted)
     */
    @Query("SELECT s FROM SalaryBreakdownPercentage s JOIN FETCH s.transactionType WHERE s.isDeleted = 'N' ORDER BY s.employeeCategory ASC, s.transTypeCode ASC")
    List<SalaryBreakdownPercentage> findAllActive();
}
