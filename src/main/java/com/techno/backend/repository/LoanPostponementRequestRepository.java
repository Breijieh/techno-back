package com.techno.backend.repository;

import com.techno.backend.entity.LoanPostponementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for LoanPostponementRequest entity.
 * Handles database operations for loan installment postponement requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Repository
public interface LoanPostponementRequestRepository extends JpaRepository<LoanPostponementRequest, Long> {

    /**
     * Find all postponement requests for a specific loan
     */
    Page<LoanPostponementRequest> findByLoanId(Long loanId, Pageable pageable);

    /**
     * Find postponement requests by status
     */
    Page<LoanPostponementRequest> findByTransStatus(String transStatus, Pageable pageable);

    /**
     * Find all pending postponement requests
     */
    @Query("SELECT p FROM LoanPostponementRequest p WHERE p.transStatus = 'N' ORDER BY p.requestDate ASC")
    List<LoanPostponementRequest> findAllPendingRequests();

    /**
     * Find pending requests for a specific approver
     */
    @Query("SELECT p FROM LoanPostponementRequest p WHERE p.nextApproval = :approverId AND p.transStatus = 'N' ORDER BY p.requestDate ASC")
    List<LoanPostponementRequest> findPendingRequestsByApprover(@Param("approverId") Long approverId);

    /**
     * Find requests for a specific installment
     */
    List<LoanPostponementRequest> findByInstallmentId(Long installmentId);

    /**
     * Find approved requests for a loan
     */
    @Query("SELECT p FROM LoanPostponementRequest p WHERE p.loanId = :loanId AND p.transStatus = 'A' ORDER BY p.approvedDate DESC")
    List<LoanPostponementRequest> findApprovedRequestsByLoan(@Param("loanId") Long loanId);

    /**
     * Find all postponement requests with optional filters for status, employee, and date range.
     * Joins with Loan entity to filter by employeeNo.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo Employee number - optional (filters via loan relationship)
     * @param startDate Start date (inclusive, optional) - filters by requestDate
     * @param endDate End date (inclusive, optional) - filters by requestDate
     * @param pageable Pagination parameters
     * @return Page of postponement requests
     */
    @Query("SELECT p FROM LoanPostponementRequest p " +
           "JOIN Loan l ON p.loanId = l.loanId " +
           "WHERE (:transStatus IS NULL OR p.transStatus = :transStatus) AND " +
           "(:employeeNo IS NULL OR l.employeeNo = :employeeNo) AND " +
           "(:startDate IS NULL OR p.requestDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.requestDate <= :endDate) " +
           "ORDER BY p.requestDate DESC, p.requestId DESC")
    Page<LoanPostponementRequest> findAllWithFilters(
            @Param("transStatus") String transStatus,
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}
