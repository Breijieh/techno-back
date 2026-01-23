package com.techno.backend.repository;

import com.techno.backend.entity.EmpPayrollTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for EmpPayrollTransaction entity.
 * Provides database access methods for employee payroll transaction management.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface EmpPayrollTransactionRepository extends JpaRepository<EmpPayrollTransaction, Long> {

    /**
     * Find all active payroll transactions for a specific employee
     */
    @Query("SELECT t FROM EmpPayrollTransaction t WHERE t.employeeNo = :employeeNo AND t.isActive = 'Y' ORDER BY t.transTypeCode ASC")
    List<EmpPayrollTransaction> findActiveByEmployeeNo(@Param("employeeNo") Long employeeNo);

    /**
     * Find all payroll transactions for a specific employee (active and inactive)
     */
    @Query("SELECT t FROM EmpPayrollTransaction t WHERE t.employeeNo = :employeeNo ORDER BY t.effectiveDate DESC, t.transTypeCode ASC")
    List<EmpPayrollTransaction> findAllByEmployeeNo(@Param("employeeNo") Long employeeNo);

    /**
     * Find active transactions effective as of a specific date
     */
    @Query("SELECT t FROM EmpPayrollTransaction t WHERE " +
           "t.employeeNo = :employeeNo AND " +
           "t.isActive = 'Y' AND " +
           "t.effectiveDate <= :asOfDate " +
           "ORDER BY t.transTypeCode ASC")
    List<EmpPayrollTransaction> findActiveByEmployeeNoAndEffectiveDate(
        @Param("employeeNo") Long employeeNo,
        @Param("asOfDate") LocalDate asOfDate
    );

    /**
     * Find transactions by employee and transaction type
     */
    @Query("SELECT t FROM EmpPayrollTransaction t WHERE " +
           "t.employeeNo = :employeeNo AND " +
           "t.transTypeCode = :transTypeCode AND " +
           "t.isActive = 'Y'")
    List<EmpPayrollTransaction> findByEmployeeNoAndTransTypeCode(
        @Param("employeeNo") Long employeeNo,
        @Param("transTypeCode") Long transTypeCode
    );

    /**
     * Delete all transactions for a specific employee (soft delete by setting isActive = 'N')
     */
    @Query("UPDATE EmpPayrollTransaction t SET t.isActive = 'N' WHERE t.employeeNo = :employeeNo")
    void deactivateByEmployeeNo(@Param("employeeNo") Long employeeNo);
}
