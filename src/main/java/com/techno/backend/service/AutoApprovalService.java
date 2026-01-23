package com.techno.backend.service;

import com.techno.backend.entity.EmployeeLeave;
import com.techno.backend.entity.Loan;
import com.techno.backend.entity.EmpMonthlyAllowance;
import com.techno.backend.repository.EmployeeLeaveRepository;
import com.techno.backend.repository.LoanRepository;
import com.techno.backend.repository.EmpMonthlyAllowanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for auto-approval of pending requests after timeout.
 *
 * This service runs background jobs to:
 * 1. Auto-approve leave requests after 48 hours if no response
 * 2. Auto-approve loan requests after 48 hours if no response (at current level)
 * 3. Auto-approve allowance requests after 48 hours if no response
 *
 * Auto-approval rules:
 * - Only applies to pending requests (status = 'N')
 * - Only if request is older than 48 hours
 * - Moves to next approval level (or finalizes if last level)
 * - Sends notification to employee about auto-approval
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Auto-Approval Timeout
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoApprovalService {

    private final EmployeeLeaveRepository leaveRepository;
    private final LoanRepository loanRepository;
    private final EmpMonthlyAllowanceRepository allowanceRepository;
    private final LeaveService leaveService;
    private final LoanService loanService;
    private final AllowanceService allowanceService;
    private static final int AUTO_APPROVAL_TIMEOUT_HOURS = 48;

    /**
     * Auto-approve pending leave requests after 48 hours.
     *
     * Runs every hour.
     *
     * Process:
     * 1. Find all pending leave requests older than 48 hours
     * 2. For each request, auto-approve at current level
     * 3. Move to next level or finalize if last level
     * 4. Send notification to employee
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Riyadh")
    @Transactional
    public void autoApprovePendingLeaves() {
        log.info("Starting auto-approval job for pending leave requests");

        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(2); // 48 hours = 2 days
            List<EmployeeLeave> pendingLeaves = leaveRepository.findPendingLeavesOlderThan(cutoffDate);

            if (pendingLeaves.isEmpty()) {
                log.info("No pending leave requests older than {} hours", AUTO_APPROVAL_TIMEOUT_HOURS);
                return;
            }

            log.info("Found {} pending leave requests to auto-approve", pendingLeaves.size());

            int successCount = 0;
            int errorCount = 0;

            for (EmployeeLeave leave : pendingLeaves) {
                try {
                    // Auto-approve using the next approver (system auto-approval)
                    // Use a system user ID (0) or the next approver's ID
                    Long approverNo = leave.getNextApproval() != null ? leave.getNextApproval() : 0L;
                    
                    // Approve at current level
                    leaveService.approveLeave(leave.getLeaveId(), approverNo);
                    successCount++;
                    
                    log.info("Auto-approved leave request {} (older than {} hours)", 
                            leave.getLeaveId(), AUTO_APPROVAL_TIMEOUT_HOURS);
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to auto-approve leave request {}: {}", 
                            leave.getLeaveId(), e.getMessage(), e);
                }
            }

            log.info("Auto-approval job for leaves completed. Success: {}, Errors: {}", 
                    successCount, errorCount);

        } catch (Exception e) {
            log.error("Auto-approval job for leaves failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-approve pending loan requests after 48 hours.
     *
     * Runs every hour.
     */
    @Scheduled(cron = "0 15 * * * *", zone = "Asia/Riyadh")
    @Transactional
    public void autoApprovePendingLoans() {
        log.info("Starting auto-approval job for pending loan requests");

        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(2); // 48 hours = 2 days
            List<Loan> pendingLoans = loanRepository.findPendingLoansOlderThan(cutoffDate);

            if (pendingLoans.isEmpty()) {
                log.info("No pending loan requests older than {} hours", AUTO_APPROVAL_TIMEOUT_HOURS);
                return;
            }

            log.info("Found {} pending loan requests to auto-approve", pendingLoans.size());

            int successCount = 0;
            int errorCount = 0;

            for (Loan loan : pendingLoans) {
                try {
                    Long approverNo = loan.getNextApproval() != null ? loan.getNextApproval() : 0L;
                    loanService.approveLoan(loan.getLoanId(), approverNo);
                    successCount++;
                    
                    log.info("Auto-approved loan request {} (older than {} hours)", 
                            loan.getLoanId(), AUTO_APPROVAL_TIMEOUT_HOURS);
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to auto-approve loan request {}: {}", 
                            loan.getLoanId(), e.getMessage(), e);
                }
            }

            log.info("Auto-approval job for loans completed. Success: {}, Errors: {}", 
                    successCount, errorCount);

        } catch (Exception e) {
            log.error("Auto-approval job for loans failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-approve pending allowance requests after 48 hours.
     *
     * Runs every hour.
     */
    @Scheduled(cron = "0 30 * * * *", zone = "Asia/Riyadh")
    @Transactional
    public void autoApprovePendingAllowances() {
        log.info("Starting auto-approval job for pending allowance requests");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(AUTO_APPROVAL_TIMEOUT_HOURS);
            List<EmpMonthlyAllowance> pendingAllowances = 
                    allowanceRepository.findPendingAllowancesOlderThan(cutoffTime);

            if (pendingAllowances.isEmpty()) {
                log.info("No pending allowance requests older than {} hours", AUTO_APPROVAL_TIMEOUT_HOURS);
                return;
            }

            log.info("Found {} pending allowance requests to auto-approve", pendingAllowances.size());

            int successCount = 0;
            int errorCount = 0;

            for (EmpMonthlyAllowance allowance : pendingAllowances) {
                try {
                    Long approverNo = allowance.getNextApproval() != null ? allowance.getNextApproval() : 0L;
                    allowanceService.approveAllowance(allowance.getTransactionNo(), approverNo);
                    successCount++;
                    
                    log.info("Auto-approved allowance request {} (older than {} hours)", 
                            allowance.getTransactionNo(), AUTO_APPROVAL_TIMEOUT_HOURS);
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to auto-approve allowance request {}: {}", 
                            allowance.getTransactionNo(), e.getMessage(), e);
                }
            }

            log.info("Auto-approval job for allowances completed. Success: {}, Errors: {}", 
                    successCount, errorCount);

        } catch (Exception e) {
            log.error("Auto-approval job for allowances failed: {}", e.getMessage(), e);
        }
    }
}

