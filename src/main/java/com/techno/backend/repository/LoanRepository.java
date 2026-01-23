package com.techno.backend.repository;

import com.techno.backend.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Loan entity.
 * Handles database operations for loan requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

       /**
        * Find all loans for a specific employee
        */
       Page<Loan> findByEmployeeNo(Long employeeNo, Pageable pageable);

       /**
        * Find all loans for a specific employee ordered by date
        */
       List<Loan> findByEmployeeNoOrderByRequestDateDesc(Long employeeNo);

       /**
        * Find loans by employee and status
        */
       Page<Loan> findByEmployeeNoAndTransStatus(Long employeeNo, String transStatus, Pageable pageable);

       /**
        * Find active loans for employee
        */
       @Query("SELECT l FROM Loan l WHERE l.employeeNo = :employeeNo AND l.isActive = 'Y' AND l.transStatus = 'A' ORDER BY l.requestDate DESC")
       List<Loan> findActiveLoansByEmployee(@Param("employeeNo") Long employeeNo);

       /**
        * Find all pending loans (status = 'N')
        */
       @Query("SELECT l FROM Loan l WHERE l.transStatus = 'N' ORDER BY l.requestDate ASC")
       List<Loan> findAllPendingLoans();

       /**
        * Find pending loans for a specific approver
        */
       @Query("SELECT l FROM Loan l WHERE l.nextApproval = :approverId AND l.transStatus = 'N' ORDER BY l.requestDate ASC")
       List<Loan> findPendingLoansByApprover(@Param("approverId") Long approverId);

       /**
        * Find approved loans for employee
        */
       @Query("SELECT l FROM Loan l WHERE l.employeeNo = :employeeNo AND l.transStatus = 'A' ORDER BY l.requestDate DESC")
       List<Loan> findApprovedLoansByEmployee(@Param("employeeNo") Long employeeNo);

       /**
        * Calculate total outstanding loan balance for employee
        */
       @Query("SELECT COALESCE(SUM(l.remainingBalance), 0) FROM Loan l WHERE " +
                     "l.employeeNo = :employeeNo AND " +
                     "l.isActive = 'Y' AND " +
                     "l.transStatus = 'A'")
       BigDecimal sumOutstandingBalanceForEmployee(@Param("employeeNo") Long employeeNo);

       /**
        * Check if employee has active loans
        */
       @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Loan l WHERE " +
                     "l.employeeNo = :employeeNo AND " +
                     "l.isActive = 'Y' AND " +
                     "l.transStatus = 'A'")
       boolean hasActiveLoans(@Param("employeeNo") Long employeeNo);

       /**
        * Find pending loans older than specified date (for auto-approval)
        */
       @Query("SELECT l FROM Loan l WHERE " +
                     "l.transStatus = 'N' AND " +
                     "l.requestDate <= :cutoffDate " +
                     "ORDER BY l.requestDate ASC")
       List<Loan> findPendingLoansOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

       /**
        * Find all loan records with optional filters for status, employee, and date
        * range.
        *
        * @param transStatus Transaction status (N/A/R) - optional
        * @param employeeNo  Employee number - optional
        * @param startDate   Start date (inclusive, optional) - filters by requestDate
        * @param endDate     End date (inclusive, optional) - filters by requestDate
        * @param pageable    Pagination parameters
        * @return Page of loan records
        */
       @Query("SELECT l FROM Loan l WHERE " +
                     "(:transStatus IS NULL OR l.transStatus = :transStatus) AND " +
                     "(:employeeNo IS NULL OR l.employeeNo = :employeeNo) AND " +
                     "(:startDate IS NULL OR l.requestDate >= :startDate) AND " +
                     "(:endDate IS NULL OR l.requestDate <= :endDate) " +
                     "ORDER BY l.requestDate DESC, l.loanId DESC")
       Page<Loan> findAllWithFilters(
                     @Param("transStatus") String transStatus,
                     @Param("employeeNo") Long employeeNo,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     Pageable pageable);
}
