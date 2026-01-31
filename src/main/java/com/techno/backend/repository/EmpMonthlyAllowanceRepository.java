package com.techno.backend.repository;

import com.techno.backend.entity.EmpMonthlyAllowance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmpMonthlyAllowanceRepository extends JpaRepository<EmpMonthlyAllowance, Long> {

       List<EmpMonthlyAllowance> findByEmployeeNoAndIsDeleted(Long employeeNo, String isDeleted);

       @Query("SELECT a FROM EmpMonthlyAllowance a WHERE " +
                     "a.employeeNo = :employeeNo AND " +
                     "a.isDeleted = 'N' AND " +
                     "a.transStatus = 'A' AND " +
                     "(a.allowanceStartDate IS NULL OR a.allowanceStartDate <= :date) AND " +
                     "(a.allowanceEndDate IS NULL OR a.allowanceEndDate >= :date)")
       List<EmpMonthlyAllowance> findActiveAllowancesForEmployeeOnDate(
                     @Param("employeeNo") Long employeeNo,
                     @Param("date") LocalDate date);

       @Query("SELECT a FROM EmpMonthlyAllowance a WHERE " +
                     "a.employeeNo = :employeeNo AND " +
                     "a.typeCode = :typeCode AND " +
                     "a.isDeleted = 'N' AND " +
                     "a.transStatus = 'A'")
       List<EmpMonthlyAllowance> findByEmployeeAndType(
                     @Param("employeeNo") Long employeeNo,
                     @Param("typeCode") Long typeCode);

       // Phase 6: Additional queries for approval workflow
       @Query("SELECT a FROM EmpMonthlyAllowance a WHERE " +
                     "a.nextApproval = :approverId AND " +
                     "a.transStatus = 'N' AND " +
                     "a.isDeleted = 'N'")
       List<EmpMonthlyAllowance> findPendingAllowancesByApprover(
                     @Param("approverId") Long approverId);

       @Query("SELECT a FROM EmpMonthlyAllowance a WHERE " +
                     "a.transactionDate BETWEEN :startDate AND :endDate AND " +
                     "a.isDeleted = 'N'")
       List<EmpMonthlyAllowance> findByTransactionDateBetween(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       boolean existsByEmployeeNoAndTypeCodeAndTransactionDate(
                     Long employeeNo, Long typeCode, LocalDate transactionDate);

       // Phase 7: Query for auto-deletion of old pending records
       @Query("SELECT a FROM EmpMonthlyAllowance a WHERE " +
                     "a.transStatus = 'N' AND " +
                     "a.createdDate < :cutoffTime AND " +
                     "a.isDeleted = 'N'")
       List<EmpMonthlyAllowance> findPendingAllowancesOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

       /**
        * Find all allowance records with optional filters.
        * Note: Service layer provides default dates for null parameters.
        *
        * @param transStatus Transaction status (N/A/R) - optional
        * @param employeeNo  Employee number - optional
        * @param startDate   Start date (never null - service provides default)
        * @param endDate     End date (never null - service provides default)
        * @param pageable    Pagination parameters
        * @return Page of allowance records
        */
       /**
        * Find all allowance records filtered to TECHNO contract employees only.
        */
       @Query("SELECT a FROM EmpMonthlyAllowance a " +
                     "JOIN Employee e ON a.employeeNo = e.employeeNo " +
                     "WHERE e.empContractType = 'TECHNO' " +
                     "AND a.isDeleted = 'N' " +
                     "AND (:transStatus IS NULL OR a.transStatus = :transStatus) " +
                     "AND (:employeeNo IS NULL OR a.employeeNo = :employeeNo) " +
                     "AND a.transactionDate >= :startDate " +
                     "AND a.transactionDate <= :endDate")
       Page<EmpMonthlyAllowance> findAllWithFilters(
                     @Param("transStatus") String transStatus,
                     @Param("employeeNo") Long employeeNo,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     Pageable pageable);
}
