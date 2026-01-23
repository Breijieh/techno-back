package com.techno.backend.repository;

import com.techno.backend.entity.LoanInstallment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for LoanInstallment entity.
 * Handles database operations for loan installments.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

       /**
        * Find all installments for a specific loan
        */
       List<LoanInstallment> findByLoanIdOrderByInstallmentNoAsc(Long loanId);

       /**
        * Find unpaid installments for a loan
        */
       @Query("SELECT i FROM LoanInstallment i WHERE i.loanId = :loanId AND i.paymentStatus IN ('UNPAID', 'POSTPONED') ORDER BY i.dueDate ASC")
       List<LoanInstallment> findUnpaidInstallmentsByLoan(@Param("loanId") Long loanId);

       /**
        * Find unpaid installments due in a specific month
        */
       @Query("SELECT i FROM LoanInstallment i WHERE " +
                     "i.paymentStatus IN ('UNPAID', 'POSTPONED') AND " +
                     "YEAR(i.dueDate) = :year AND " +
                     "MONTH(i.dueDate) = :month " +
                     "ORDER BY i.loanId, i.installmentNo")
       List<LoanInstallment> findUnpaidInstallmentsDueInMonth(@Param("year") int year, @Param("month") int month);

       /**
        * Find unpaid installments for employee in specific month
        */
       @Query("SELECT i FROM LoanInstallment i " +
                     "JOIN i.loan l " +
                     "WHERE l.employeeNo = :employeeNo AND " +
                     "i.paymentStatus IN ('UNPAID', 'POSTPONED') AND " +
                     "YEAR(i.dueDate) = :year AND " +
                     "MONTH(i.dueDate) = :month " +
                     "ORDER BY i.dueDate")
       List<LoanInstallment> findUnpaidInstallmentsForEmployeeInMonth(
                     @Param("employeeNo") Long employeeNo,
                     @Param("year") int year,
                     @Param("month") int month);

       /**
        * Find paid installments for a loan
        */
       @Query("SELECT i FROM LoanInstallment i WHERE i.loanId = :loanId AND i.paymentStatus = 'PAID' ORDER BY i.paidDate DESC")
       List<LoanInstallment> findPaidInstallmentsByLoan(@Param("loanId") Long loanId);

       /**
        * Count unpaid installments for a loan
        */
       @Query("SELECT COUNT(i) FROM LoanInstallment i WHERE i.loanId = :loanId AND i.paymentStatus IN ('UNPAID', 'POSTPONED')")
       Long countUnpaidInstallments(@Param("loanId") Long loanId);

       /**
        * Find installments by payment status
        */
       List<LoanInstallment> findByPaymentStatus(String paymentStatus);

       /**
        * Find overdue installments
        */
       @Query("SELECT i FROM LoanInstallment i WHERE i.paymentStatus IN ('UNPAID', 'POSTPONED') AND i.dueDate < :currentDate ORDER BY i.dueDate ASC")
       List<LoanInstallment> findOverdueInstallments(@Param("currentDate") LocalDate currentDate);

       /**
        * Find installments by due date range and payment status
        */
       @Query("SELECT i FROM LoanInstallment i WHERE i.dueDate BETWEEN :startDate AND :endDate AND i.paymentStatus = :paymentStatus ORDER BY i.dueDate ASC")
       List<LoanInstallment> findByDueDateBetweenAndPaymentStatus(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("paymentStatus") String paymentStatus);

       /**
        * Find all installments with optional filters for employee, loan, status, and
        * date range.
        * Joins with Loan entity to filter by employeeNo and get employee information.
        *
        * @param employeeNo    Employee number - optional (filters via loan
        *                      relationship)
        * @param loanId        Loan ID - optional
        * @param paymentStatus Payment status (PAID/UNPAID/POSTPONED) - optional
        * @param startDate     Start date (inclusive, optional) - filters by dueDate
        * @param endDate       End date (inclusive, optional) - filters by dueDate
        * @param pageable      Pagination parameters
        * @return Page of installments
        */
       @Query(value = "SELECT li.* FROM loan_installments li " +
                     "JOIN loans l ON li.loan_id = l.loan_id " +
                     "WHERE (CAST(:employeeNo AS bigint) IS NULL OR l.employee_no = CAST(:employeeNo AS bigint)) AND " +
                     "(CAST(:loanId AS bigint) IS NULL OR li.loan_id = CAST(:loanId AS bigint)) AND " +
                     "(CAST(:paymentStatus AS varchar) IS NULL OR li.payment_status = CAST(:paymentStatus AS varchar)) AND "
                     +
                     "(CAST(:startDate AS date) IS NULL OR li.due_date >= CAST(:startDate AS date)) AND " +
                     "(CAST(:endDate AS date) IS NULL OR li.due_date <= CAST(:endDate AS date))", nativeQuery = true, countQuery = "SELECT COUNT(*) FROM loan_installments li "
                                   +
                                   "JOIN loans l ON li.loan_id = l.loan_id " +
                                   "WHERE (CAST(:employeeNo AS bigint) IS NULL OR l.employee_no = CAST(:employeeNo AS bigint)) AND "
                                   +
                                   "(CAST(:loanId AS bigint) IS NULL OR li.loan_id = CAST(:loanId AS bigint)) AND " +
                                   "(CAST(:paymentStatus AS varchar) IS NULL OR li.payment_status = CAST(:paymentStatus AS varchar)) AND "
                                   +
                                   "(CAST(:startDate AS date) IS NULL OR li.due_date >= CAST(:startDate AS date)) AND "
                                   +
                                   "(CAST(:endDate AS date) IS NULL OR li.due_date <= CAST(:endDate AS date))")
       Page<LoanInstallment> findAllWithFilters(
                     @Param("employeeNo") Long employeeNo,
                     @Param("loanId") Long loanId,
                     @Param("paymentStatus") String paymentStatus,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     Pageable pageable);
}
