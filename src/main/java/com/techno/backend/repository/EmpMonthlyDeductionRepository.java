package com.techno.backend.repository;

import com.techno.backend.entity.EmpMonthlyDeduction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmpMonthlyDeductionRepository extends JpaRepository<EmpMonthlyDeduction, Long> {

        List<EmpMonthlyDeduction> findByEmployeeNoAndIsDeleted(Long employeeNo, String isDeleted);

        @Query("SELECT d FROM EmpMonthlyDeduction d WHERE " +
                        "d.employeeNo = :employeeNo AND " +
                        "d.isDeleted = 'N' AND " +
                        "d.transStatus = 'A' AND " +
                        "(d.deductionStartDate IS NULL OR d.deductionStartDate <= :date) AND " +
                        "(d.deductionEndDate IS NULL OR d.deductionEndDate >= :date)")
        List<EmpMonthlyDeduction> findActiveDeductionsForEmployeeOnDate(
                        @Param("employeeNo") Long employeeNo,
                        @Param("date") LocalDate date);

        @Query("SELECT d FROM EmpMonthlyDeduction d WHERE " +
                        "d.employeeNo = :employeeNo AND " +
                        "d.typeCode = :typeCode AND " +
                        "d.isDeleted = 'N' AND " +
                        "d.transStatus = 'A'")
        List<EmpMonthlyDeduction> findByEmployeeAndType(
                        @Param("employeeNo") Long employeeNo,
                        @Param("typeCode") Long typeCode);

        // Phase 6: Additional queries for approval workflow
        @Query("SELECT d FROM EmpMonthlyDeduction d WHERE " +
                        "d.nextApproval = :approverId AND " +
                        "d.transStatus = 'N' AND " +
                        "d.isDeleted = 'N'")
        List<EmpMonthlyDeduction> findPendingDeductionsByApprover(
                        @Param("approverId") Long approverId);

        @Query("SELECT d FROM EmpMonthlyDeduction d WHERE " +
                        "d.transactionDate BETWEEN :startDate AND :endDate AND " +
                        "d.isDeleted = 'N'")
        List<EmpMonthlyDeduction> findByTransactionDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        boolean existsByEmployeeNoAndTypeCodeAndTransactionDate(
                        Long employeeNo, Long typeCode, LocalDate transactionDate);

        /**
         * Find all deduction records with optional filters.
         * Note: Service layer provides default dates for null parameters.
         *
         * @param transStatus Transaction status (N/A/R) - optional
         * @param employeeNo  Employee number - optional
         * @param startDate   Start date (never null - service provides default)
         * @param endDate     End date (never null - service provides default)
         * @param pageable    Pagination parameters
         * @return Page of deduction records
         */
        @Query("SELECT d FROM EmpMonthlyDeduction d " +
                        "WHERE d.isDeleted = 'N' " +
                        "AND (:transStatus IS NULL OR d.transStatus = :transStatus) " +
                        "AND (:employeeNo IS NULL OR d.employeeNo = :employeeNo) " +
                        "AND d.transactionDate >= :startDate " +
                        "AND d.transactionDate <= :endDate")
        Page<EmpMonthlyDeduction> findAllWithFilters(
                        @Param("transStatus") String transStatus,
                        @Param("employeeNo") Long employeeNo,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);
}
