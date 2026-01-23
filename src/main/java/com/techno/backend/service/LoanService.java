package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Loan;
import com.techno.backend.entity.LoanInstallment;
import com.techno.backend.entity.LoanPostponementRequest;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.LoanInstallmentRepository;
import com.techno.backend.repository.LoanPostponementRequestRepository;
import com.techno.backend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for employee loan management.
 *
 * Handles:
 * - Loan request submission
 * - Multi-level approval workflow (HR Manager â†’ Finance Manager)
 * - Installment schedule generation on approval
 * - Active loan tracking
 * - Outstanding balance calculation
 *
 * Approval Flow for Loans (LOAN):
 * - Level 1: HR Manager
 * - Level 2: Finance Manager (final)
 *
 * Loan Rules:
 * - Installment amount = Loan Amount Ã· Number of Installments
 * - Installments deducted automatically during payroll
 * - Maximum 1 active loan per employee
 * - Minimum installments: 3, Maximum: 60 (5 years)
 * - Loan amount cannot exceed employee's monthly salary Ã— 12
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanPostponementRequestRepository postponementRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String REQUEST_TYPE = "LOAN";
    private static final String POSTPONEMENT_REQUEST_TYPE = "POSTLOAN";
    private static final int MIN_INSTALLMENTS = 3;
    private static final int MAX_INSTALLMENTS = 60;

    /**
     * Submit new loan request.
     *
     * Validates:
     * - Employee exists and is active
     * - No active loans exist
     * - Loan amount is within limits
     * - Installment count is valid
     *
     * @param employeeNo           Employee number
     * @param loanAmount           Requested loan amount
     * @param noOfInstallments     Number of monthly installments
     * @param firstInstallmentDate First installment due date
     * @return Created loan request
     */
    @Transactional
    public Loan submitLoanRequest(Long employeeNo, BigDecimal loanAmount,
            Integer noOfInstallments, LocalDate firstInstallmentDate) {
        log.info("Submitting loan request for employee {}: Amount={}, Installments={}",
                employeeNo, loanAmount, noOfInstallments);

        // Validate employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø§Ù„Ù†Ø´Ø·ÙŠÙ† ÙÙ‚Ø· ØªÙ‚Ø¯ÙŠÙ… Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù‚Ø±ÙˆØ¶");
        }

        // Validate no active loans
        List<Loan> activeLoans = loanRepository.findActiveLoansByEmployee(employeeNo);
        if (!activeLoans.isEmpty()) {
            throw new BadRequestException("Ø§Ù„Ù…ÙˆØ¸Ù Ù„Ø¯ÙŠÙ‡ Ù‚Ø±Ø¶ Ù†Ø´Ø· Ø¨Ø§Ù„ÙØ¹Ù„ (Ø±Ù‚Ù…: " +
                    activeLoans.get(0).getLoanId() + ")");
        }

        // Validate loan amount
        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶ Ù…ÙˆØ¬Ø¨Ø§Ù‹");
        }

        BigDecimal maxLoanAmount = employee.getMonthlySalary().multiply(BigDecimal.valueOf(12));
        if (loanAmount.compareTo(maxLoanAmount) > 0) {
            throw new BadRequestException(String.format(
                    "Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶ (%.2f Ø±ÙŠØ§Ù„) ÙŠØªØ¬Ø§ÙˆØ² Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ù‚ØµÙ‰ Ø§Ù„Ù…Ø³Ù…ÙˆØ­ (%.2f Ø±ÙŠØ§Ù„ = 12 Ø´Ù‡Ø± Ø±Ø§ØªØ¨)",
                    loanAmount, maxLoanAmount));
        }

        // Validate installments
        if (noOfInstallments < MIN_INSTALLMENTS || noOfInstallments > MAX_INSTALLMENTS) {
            throw new BadRequestException(String.format(
                    "ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¹Ø¯Ø¯ Ø§Ù„Ø£Ù‚Ø³Ø§Ø· Ø¨ÙŠÙ† %d Ùˆ %d",
                    MIN_INSTALLMENTS, MAX_INSTALLMENTS));
        }

        // Validate first installment date
        if (firstInstallmentDate.isBefore(LocalDate.now().plusMonths(1))) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ØªØ§Ø±ÙŠØ® Ø§Ù„Ù‚Ø³Ø· Ø§Ù„Ø£ÙˆÙ„ Ø¨Ø¹Ø¯ Ø´Ù‡Ø± ÙˆØ§Ø­Ø¯ Ø¹Ù„Ù‰ Ø§Ù„Ø£Ù‚Ù„ Ù…Ù† Ø§Ù„Ø¢Ù†");
        }

        // Calculate installment amount
        BigDecimal installmentAmount = loanAmount
                .divide(BigDecimal.valueOf(noOfInstallments), 4, RoundingMode.HALF_UP);

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService
                .initializeApproval(REQUEST_TYPE, employeeNo,
                        employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        // Create loan request
        Loan loan = Loan.builder()
                .employeeNo(employeeNo)
                .loanAmount(loanAmount)
                .noOfInstallments(noOfInstallments)
                .firstInstallmentDate(firstInstallmentDate)
                .installmentAmount(installmentAmount)
                .remainingBalance(loanAmount)
                .requestDate(LocalDate.now())
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .nextAppLevelName(approvalInfo.getNextAppLevelName())
                .isActive("Y")
                .build();

        loan = loanRepository.save(loan);

        log.info("Loan request created: ID={}, Amount={}, Installment={}, Next Approver={}",
                loan.getLoanId(), loanAmount, installmentAmount, approvalInfo.getNextApproval());

        // Publish notification event for loan submission
        publishLoanSubmittedNotification(loan, employee);

        return loan;
    }

    /**
     * Approve loan request at current approval level.
     *
     * If this is the final approval level:
     * - Set status to Approved (A)
     * - Generate installment schedule
     *
     * Otherwise:
     * - Move to next approval level
     *
     * @param loanId     Loan request ID
     * @param approverNo Approver employee number
     * @return Updated loan request
     */
    @Transactional
    public Loan approveLoan(Long loanId, Long approverNo) {
        log.info("Approving loan request {}, approver: {}", loanId, approverNo);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        // Validate request is pending approval
        if (!"N".equals(loan.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©. Ø§Ù„Ø­Ø§Ù„Ø©: " + loan.getTransStatus());
        }

        // Validate approver (Allow Admins to bypass check)
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !approvalWorkflowService.canApprove(REQUEST_TYPE, loan.getNextAppLevel(),
                approverNo, loan.getNextApproval())) {
            throw new RuntimeException("Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ Ù‡Ø°Ø§");
        }

        // Move to next level or finalize
        Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                REQUEST_TYPE, loan.getNextAppLevel(),
                loan.getEmployeeNo(), employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        loan.setTransStatus(nextLevel.getTransStatus());
        loan.setNextApproval(nextLevel.getNextApproval());
        loan.setNextAppLevel(nextLevel.getNextAppLevel());
        loan.setNextAppLevelName(nextLevel.getNextAppLevelName());

        // If fully approved, generate installment schedule
        if ("A".equals(nextLevel.getTransStatus())) {
            generateInstallmentSchedule(loan);
            loan.setApprovedDate(LocalDateTime.now());
            loan.setApprovedBy(approverNo);
            log.info("Loan fully approved. Generated {} installments for loan {}",
                    loan.getNoOfInstallments(), loan.getLoanId());

            // Save and publish final approval notification
            loan = loanRepository.save(loan);
            publishLoanFinalApprovedNotification(loan, employee);
        } else {
            log.info("Loan moved to next approval level: {}, Next Approver: {}",
                    nextLevel.getNextAppLevel(), nextLevel.getNextApproval());

            // Save and publish intermediate approval notification
            loan = loanRepository.save(loan);
            publishLoanIntermediateApprovedNotification(loan, employee);
        }

        return loan;
    }

    /**
     * Reject loan request.
     *
     * Sets status to Rejected (R).
     * No installments are generated.
     *
     * @param loanId          Loan request ID
     * @param approverNo      Approver employee number
     * @param rejectionReason Rejection reason
     * @return Updated loan request
     */
    @Transactional
    public Loan rejectLoan(Long loanId, Long approverNo, String rejectionReason) {
        log.info("Rejecting loan request {}, approver: {}", loanId, approverNo);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        // Validate request is pending approval
        if (!"N".equals(loan.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©");
        }

        // Validate approver (Allow Admins to bypass check)
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !approvalWorkflowService.canApprove(REQUEST_TYPE, loan.getNextAppLevel(),
                approverNo, loan.getNextApproval())) {
            throw new RuntimeException("Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø±ÙØ¶ Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ Ù‡Ø°Ø§");
        }

        loan.setTransStatus("R");
        loan.setNextApproval(null);
        loan.setNextAppLevel(null);
        loan.setNextAppLevelName(null);
        loan.setIsActive("N");
        loan.setRejectionReason(rejectionReason);

        log.info("Loan request {} rejected", loanId);
        loan = loanRepository.save(loan);

        // Publish rejection notification
        Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        publishLoanRejectedNotification(loan, employee, rejectionReason);

        return loan;
    }

    /**
     * Get active loans for an employee.
     *
     * @param employeeNo Employee number
     * @return List of active loans
     */
    @Transactional(readOnly = true)
    public List<Loan> getActiveLoans(Long employeeNo) {
        return loanRepository.findActiveLoansByEmployee(employeeNo);
    }

    /**
     * Get all loans for an employee (active and closed).
     *
     * @param employeeNo Employee number
     * @return List of all loans
     */
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans(Long employeeNo) {
        return loanRepository.findByEmployeeNoOrderByRequestDateDesc(employeeNo);
    }

    /**
     * Get all loan requests with optional filters.
     * Used for listing all loan requests in the loan requests page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo  Employee number - optional
     * @param startDate   Start date (optional) - filters by requestDate
     * @param endDate     End date (optional) - filters by requestDate
     * @param pageable    Pagination parameters
     * @return Page of loan requests with employee data loaded
     */
    @Transactional(readOnly = true)
    public Page<Loan> getAllLoans(String transStatus, Long employeeNo,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all loan requests with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        Page<Loan> loanPage = loanRepository.findAllWithFilters(
                transStatus, employeeNo, startDate, endDate, pageable);

        // Extract unique employee numbers for bulk fetching
        Set<Long> employeeNos = loanPage.getContent().stream()
                .map(Loan::getEmployeeNo)
                .collect(Collectors.toSet());

        // Fetch all employees in bulk
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeNos.isEmpty()) {
            employeeRepository.findAllById(employeeNos).forEach(emp -> employeeMap.put(emp.getEmployeeNo(), emp));
        }

        // Enrich loan records with employee data (lazy loading)
        // Note: The employee relationship is already defined in Loan entity
        // We just ensure it's loaded for the response
        return loanPage.map(loan -> {
            // Ensure employee is loaded (if not already)
            // The employee relationship will be used in the controller's
            // toLoanDetailsResponse method
            return loan;
        });
    }

    /**
     * Get pending loan requests for an approver.
     *
     * @param approverNo Approver employee number
     * @return List of pending loans
     */
    @Transactional(readOnly = true)
    public List<Loan> getPendingLoansForApprover(Long approverNo) {
        return loanRepository.findPendingLoansByApprover(approverNo);
    }

    /**
     * Get all pending loan requests.
     * Used by Admins to view all pending approvals.
     *
     * @return List of all pending loans
     */
    @Transactional(readOnly = true)
    public List<Loan> getAllPendingLoans() {
        return loanRepository.findAllPendingLoans();
    }

    /**
     * Get loan details including installments.
     *
     * @param loanId Loan ID
     * @return Loan with installments
     */
    @Transactional(readOnly = true)
    public Loan getLoanDetails(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        // Load installments
        List<LoanInstallment> installments = installmentRepository
                .findByLoanIdOrderByInstallmentNoAsc(loanId);
        loan.setInstallments(installments);

        return loan;
    }

    /**
     * Calculate outstanding balance for employee's active loans.
     *
     * @param employeeNo Employee number
     * @return Total outstanding balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long employeeNo) {
        return loanRepository.sumOutstandingBalanceForEmployee(employeeNo);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Generate installment schedule for an approved loan.
     *
     * Creates installment records for each monthly payment.
     * Installments are marked as UNPAID initially.
     */
    private void generateInstallmentSchedule(Loan loan) {
        log.info("Generating installment schedule for loan {}: {} installments starting {}",
                loan.getLoanId(), loan.getNoOfInstallments(), loan.getFirstInstallmentDate());

        List<LoanInstallment> installments = new ArrayList<>();
        LocalDate installmentDate = loan.getFirstInstallmentDate();

        for (int i = 1; i <= loan.getNoOfInstallments(); i++) {
            // For the last installment, adjust amount to cover any rounding difference
            BigDecimal installmentAmount = loan.getInstallmentAmount();
            if (i == loan.getNoOfInstallments()) {
                // Calculate total paid so far
                BigDecimal totalPaid = loan.getInstallmentAmount()
                        .multiply(BigDecimal.valueOf(loan.getNoOfInstallments() - 1));
                // Last installment = remaining balance
                installmentAmount = loan.getLoanAmount().subtract(totalPaid);
            }

            LoanInstallment installment = LoanInstallment.builder()
                    .loanId(loan.getLoanId())
                    .installmentNo(i)
                    .dueDate(installmentDate)
                    .installmentAmount(installmentAmount)
                    .paymentStatus("UNPAID")
                    .build();

            installments.add(installment);

            // Next installment is 1 month later
            installmentDate = installmentDate.plusMonths(1);
        }

        installmentRepository.saveAll(installments);

        log.info("Generated {} installments for loan {}", installments.size(), loan.getLoanId());
    }

    /**
     * Mark loan as inactive when fully paid.
     *
     * Called by PayrollCalculationService after processing last installment.
     *
     * @param loanId Loan ID
     */
    @Transactional
    public void markLoanAsFullyPaid(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setIsActive("N");
            loan = loanRepository.save(loan);
            log.info("Loan {} marked as fully paid and closed", loanId);

            // Publish fully paid notification
            Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                    .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
            publishLoanFullyPaidNotification(loan, employee);
        }
    }

    /**
     * Deduct payment from loan balance.
     *
     * Called by PayrollCalculationService when processing installment.
     *
     * @param loanId Loan ID
     * @param amount Payment amount
     */
    @Transactional
    public void deductPayment(Long loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        BigDecimal newBalance = loan.getRemainingBalance().subtract(amount);
        loan.setRemainingBalance(newBalance);

        // Mark as inactive if fully paid
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setIsActive("N");
        }

        loan = loanRepository.save(loan);

        log.info("Deducted {} from loan {}. Remaining balance: {}",
                amount, loanId, newBalance);

        // Publish installment paid notification (only if not fully paid yet)
        if (newBalance.compareTo(BigDecimal.ZERO) > 0) {
            Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                    .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
            publishLoanInstallmentPaidNotification(loan, employee, amount);
        }
    }

    // ==================== Loan Postponement Methods ====================

    /**
     * Submit new postponement request for a loan installment.
     *
     * Validates:
     * - Loan exists and is active
     * - Installment exists and is unpaid
     * - New due date is in the future
     * - No pending postponement for this installment
     *
     * @param loanId             Loan ID
     * @param installmentId      Installment ID
     * @param newDueDate         New due date for the installment
     * @param postponementReason Reason for postponement
     * @return Created postponement request
     */
    @Transactional
    public LoanPostponementRequest submitPostponementRequest(Long loanId, Long installmentId,
            LocalDate newDueDate, String postponementReason) {
        log.info("Submitting postponement request for loan {}, installment {}: New date={}",
                loanId, installmentId, newDueDate);

        // Validate loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        if (!"Y".equals(loan.getIsActive())) {
            throw new RuntimeException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªØ£Ø¬ÙŠÙ„ Ø£Ù‚Ø³Ø§Ø· Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ø§Ù„Ù†Ø´Ø·");
        }

        // Validate installment
        LoanInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø³Ø· ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + installmentId));

        if (!installment.getLoanId().equals(loanId)) {
            throw new RuntimeException("Ø§Ù„Ù‚Ø³Ø· Ù„Ø§ ÙŠÙ†ØªÙ…ÙŠ Ø¥Ù„Ù‰ Ù‡Ø°Ø§ Ø§Ù„Ù‚Ø±Ø¶");
        }

        if ("PAID".equals(installment.getPaymentStatus())) {
            throw new RuntimeException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªØ£Ø¬ÙŠÙ„ Ø§Ù„Ù‚Ø³Ø· Ø§Ù„Ù…Ø¯ÙÙˆØ¹ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Validate new due date
        if (!newDueDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ø³ØªØ­Ù‚Ø§Ù‚ Ø§Ù„Ø¬Ø¯ÙŠØ¯ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ÙÙŠ Ø§Ù„Ù…Ø³ØªÙ‚Ø¨Ù„");
        }

        // Check for pending postponement requests for this installment
        List<LoanPostponementRequest> pendingRequests = postponementRepository
                .findByInstallmentId(installmentId).stream()
                .filter(r -> "N".equals(r.getTransStatus()))
                .collect(Collectors.toList());
        if (!pendingRequests.isEmpty()) {
            throw new RuntimeException("ÙŠÙˆØ¬Ø¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ø·Ù„Ø¨ ØªØ£Ø¬ÙŠÙ„ Ù…Ø¹Ù„Ù‚ Ù„Ù‡Ø°Ø§ Ø§Ù„Ù‚Ø³Ø·");
        }

        // Get employee for approval workflow
        Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService
                .initializeApproval(POSTPONEMENT_REQUEST_TYPE, loan.getEmployeeNo(),
                        employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        // Create postponement request
        LoanPostponementRequest request = LoanPostponementRequest.builder()
                .loanId(loanId)
                .installmentId(installmentId)
                .currentDueDate(installment.getDueDate())
                .newDueDate(newDueDate)
                .postponementReason(postponementReason)
                .requestDate(LocalDate.now())
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .build();

        request = postponementRepository.save(request);

        log.info("Postponement request created: ID={}, Next Approver={}", request.getRequestId(),
                approvalInfo.getNextApproval());

        // Publish postponement submitted notification
        publishLoanPostponementSubmittedNotification(request, employee, loan);

        return request;
    }

    /**
     * Approve postponement request at current approval level.
     *
     * If this is the final approval level:
     * - Set status to Approved (A)
     * - Update installment due date
     * - Set installment status to POSTPONED
     *
     * Otherwise:
     * - Move to next approval level
     *
     * @param requestId  Postponement request ID
     * @param approverNo Approver employee number
     * @return Updated postponement request
     */
    @Transactional
    public LoanPostponementRequest approvePostponement(Long requestId, Long approverNo) {
        log.info("Approving postponement request {}, approver: {}", requestId, approverNo);

        LoanPostponementRequest request = postponementRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + requestId));

        // Validate request is pending approval
        if (!"N".equals(request.getTransStatus())) {
            throw new RuntimeException(
                    "Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©. Ø§Ù„Ø­Ø§Ù„Ø©: " + request.getTransStatus());
        }

        // Validate approver
        if (!approvalWorkflowService.canApprove(POSTPONEMENT_REQUEST_TYPE, request.getNextAppLevel(),
                approverNo, request.getNextApproval())) {
            throw new RuntimeException("Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù‡Ø°Ø§");
        }

        // Get loan and employee for approval workflow
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Move to next level or finalize
        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                POSTPONEMENT_REQUEST_TYPE, request.getNextAppLevel(),
                loan.getEmployeeNo(), employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        request.setTransStatus(nextLevel.getTransStatus());
        request.setNextApproval(nextLevel.getNextApproval());
        request.setNextAppLevel(nextLevel.getNextAppLevel());

        // If fully approved, update installment due date
        if ("A".equals(nextLevel.getTransStatus())) {
            LoanInstallment installment = installmentRepository.findById(request.getInstallmentId())
                    .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø³Ø· ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

            installment.setDueDate(request.getNewDueDate());
            installment.setPaymentStatus("POSTPONED");
            installmentRepository.save(installment);

            request.setApprovedDate(LocalDateTime.now());
            request.setApprovedBy(approverNo);

            log.info("Postponement fully approved. Updated installment {} due date to {}",
                    request.getInstallmentId(), request.getNewDueDate());

            // Publish postponement approved notification
            request = postponementRepository.save(request);
            publishLoanPostponementApprovedNotification(request, employee, loan);
        } else {
            log.info("Postponement moved to next approval level: {}, Next Approver: {}",
                    nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
            request = postponementRepository.save(request);
        }

        return request;
    }

    /**
     * Reject postponement request.
     *
     * Sets status to Rejected (R).
     * No installment date changes.
     *
     * @param requestId       Postponement request ID
     * @param approverNo      Approver employee number
     * @param rejectionReason Rejection reason
     * @return Updated postponement request
     */
    @Transactional
    public LoanPostponementRequest rejectPostponement(Long requestId, Long approverNo, String rejectionReason) {
        log.info("Rejecting postponement request {}, approver: {}", requestId, approverNo);

        LoanPostponementRequest request = postponementRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + requestId));

        // Validate request is pending approval
        if (!"N".equals(request.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©");
        }

        // Validate approver
        if (!approvalWorkflowService.canApprove(POSTPONEMENT_REQUEST_TYPE, request.getNextAppLevel(),
                approverNo, request.getNextApproval())) {
            throw new RuntimeException("Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø±ÙØ¶ Ø·Ù„Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù‡Ø°Ø§");
        }

        request.setTransStatus("R");
        request.setNextApproval(null);
        request.setNextAppLevel(null);
        request.setRejectionReason(rejectionReason);

        log.info("Postponement request {} rejected", requestId);
        request = postponementRepository.save(request);

        // Publish postponement rejected notification
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        publishLoanPostponementRejectedNotification(request, employee, loan, rejectionReason);

        return request;
    }

    /**
     * Get pending postponement requests for an approver.
     *
     * @param approverNo Approver employee number
     * @return List of pending postponement requests
     */
    @Transactional(readOnly = true)
    public List<LoanPostponementRequest> getPendingPostponementsForApprover(Long approverNo) {
        return postponementRepository.findPendingRequestsByApprover(approverNo);
    }

    /**
     * Get all postponement requests with optional filters.
     * Used for listing all postponement requests in the postponement page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo  Employee number - optional
     * @param startDate   Start date (optional) - filters by requestDate
     * @param endDate     End date (optional) - filters by requestDate
     * @param pageable    Pagination parameters
     * @return Page of postponement requests with employee and loan data loaded
     */
    @Transactional(readOnly = true)
    public Page<LoanPostponementRequest> getAllPostponementRequests(String transStatus, Long employeeNo,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all postponement requests with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        Page<LoanPostponementRequest> postponementPage = postponementRepository.findAllWithFilters(
                transStatus, employeeNo, startDate, endDate, pageable);

        // Extract unique loan IDs for bulk fetching
        Set<Long> loanIds = postponementPage.getContent().stream()
                .map(LoanPostponementRequest::getLoanId)
                .collect(Collectors.toSet());

        // Fetch all loans in bulk to get employee information
        Map<Long, Loan> loanMap = new HashMap<>();
        if (!loanIds.isEmpty()) {
            loanRepository.findAllById(loanIds).forEach(loan -> loanMap.put(loan.getLoanId(), loan));
        }

        // Extract unique employee numbers for bulk fetching
        Set<Long> employeeNos = loanMap.values().stream()
                .map(Loan::getEmployeeNo)
                .collect(Collectors.toSet());

        // Fetch all employees in bulk
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeNos.isEmpty()) {
            employeeRepository.findAllById(employeeNos).forEach(emp -> employeeMap.put(emp.getEmployeeNo(), emp));
        }

        // Enrich postponement records with loan and employee data
        // The relationships will be used in the controller's response mapping
        return postponementPage.map(postponement -> {
            // Ensure loan and employee are loaded (if not already)
            // The relationships will be used in the controller's
            // toPostponementDetailsResponse method
            return postponement;
        });
    }

    /**
     * Get all loan installments with optional filters.
     * Used for listing all installments in the installments schedule page.
     *
     * @param employeeNo    Employee number - optional
     * @param loanId        Loan ID - optional
     * @param paymentStatus Payment status (PAID/UNPAID/POSTPONED) - optional
     * @param startDate     Start date (optional) - filters by dueDate
     * @param endDate       End date (optional) - filters by dueDate
     * @param pageable      Pagination parameters
     * @return Page of installments with employee and loan data loaded
     */
    @Transactional(readOnly = true)
    public Page<LoanInstallment> getAllInstallments(Long employeeNo, Long loanId, String paymentStatus,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all installments with filters: employee={}, loan={}, status={}, startDate={}, endDate={}",
                employeeNo, loanId, paymentStatus, startDate, endDate);

        Page<LoanInstallment> installmentPage = installmentRepository.findAllWithFilters(
                employeeNo, loanId, paymentStatus, startDate, endDate, pageable);

        // Extract unique loan IDs for bulk fetching
        Set<Long> loanIds = installmentPage.getContent().stream()
                .map(LoanInstallment::getLoanId)
                .collect(Collectors.toSet());

        // Fetch all loans in bulk to get employee information
        Map<Long, Loan> loanMap = new HashMap<>();
        if (!loanIds.isEmpty()) {
            loanRepository.findAllById(loanIds).forEach(loan -> loanMap.put(loan.getLoanId(), loan));
        }

        // Extract unique employee numbers for bulk fetching
        Set<Long> employeeNos = loanMap.values().stream()
                .map(Loan::getEmployeeNo)
                .collect(Collectors.toSet());

        // Fetch all employees in bulk
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeNos.isEmpty()) {
            employeeRepository.findAllById(employeeNos).forEach(emp -> employeeMap.put(emp.getEmployeeNo(), emp));
        }

        // Enrich installment records with loan and employee data
        // The relationships will be used in the controller's response mapping
        return installmentPage.map(installment -> {
            // Ensure loan and employee are loaded (if not already)
            // The relationships will be used in the controller's
            // toInstallmentScheduleResponse method
            return installment;
        });
    }

    /**
     * Mass postpone all unpaid installments in a specific month to a new month.
     *
     * Admin-only operation for bulk postponement (e.g., postpone all Ramadan
     * installments).
     *
     * @param originalMonth Original month in YYYY-MM format
     * @param newMonth      New month in YYYY-MM format
     * @param reason        Postponement reason
     * @return Result summary with affected employees and installment count
     */
    @Transactional
    public MassPostponeResult massPostponeByMonth(String originalMonth, String newMonth, String reason) {
        log.info("Mass postponing installments from {} to {}", originalMonth, newMonth);

        // Parse month dates
        LocalDate startDate = LocalDate.parse(originalMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LocalDate newDueDate = LocalDate.parse(newMonth + "-15"); // Standard mid-month due date

        // Find all UNPAID installments in the original month
        List<LoanInstallment> installments = installmentRepository
                .findByDueDateBetweenAndPaymentStatus(startDate, endDate, "UNPAID");

        if (installments.isEmpty()) {
            log.info("No unpaid installments found for month {}", originalMonth);
            return new MassPostponeResult(originalMonth, newMonth, 0, 0, new ArrayList<>(), reason);
        }

        // Track affected employees
        Set<Long> affectedEmployeeNos = new HashSet<>();

        // Update all installments
        for (LoanInstallment installment : installments) {
            installment.setDueDate(newDueDate);
            installment.setPaymentStatus("POSTPONED");
            installmentRepository.save(installment);

            // Track affected employee
            Loan loan = loanRepository.findById(installment.getLoanId())
                    .orElse(null);
            if (loan != null) {
                affectedEmployeeNos.add(loan.getEmployeeNo());
            }
        }

        log.info("Mass postponed {} installments for {} employees from {} to {}",
                installments.size(), affectedEmployeeNos.size(), originalMonth, newMonth);

        return new MassPostponeResult(
                originalMonth,
                newMonth,
                affectedEmployeeNos.size(),
                installments.size(),
                new ArrayList<>(affectedEmployeeNos),
                reason);
    }

    /**
     * Result DTO for mass postponement operation.
     */
    public record MassPostponeResult(
            String originalMonth,
            String newMonth,
            int affectedEmployees,
            int totalInstallmentsPostponed,
            List<Long> affectedEmployeeNumbers,
            String reason) {
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when loan request is submitted.
     */
    private void publishLoanSubmittedNotification(Loan loan, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("noOfInstallments", loan.getNoOfInstallments().toString());
            variables.put("installmentAmount", loan.getInstallmentAmount().toString());
            variables.put("firstInstallmentDate", loan.getFirstInstallmentDate().toString());
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_SUBMITTED,
                    loan.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_SUBMITTED notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan submitted notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when loan is approved at intermediate level.
     */
    private void publishLoanIntermediateApprovedNotification(Loan loan, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("noOfInstallments", loan.getNoOfInstallments().toString());
            variables.put("currentLevel", loan.getNextAppLevel() != null ? loan.getNextAppLevel().toString() : "ØºÙŠØ± Ù…ØªØ§Ø­");
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_APPROVED_INTERMEDIATE,
                    loan.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_APPROVED_INTERMEDIATE notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan intermediate approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when loan is fully approved (final level).
     */
    private void publishLoanFinalApprovedNotification(Loan loan, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("noOfInstallments", loan.getNoOfInstallments().toString());
            variables.put("installmentAmount", loan.getInstallmentAmount().toString());
            variables.put("firstInstallmentDate", loan.getFirstInstallmentDate().toString());
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_APPROVED_FINAL,
                    loan.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_APPROVED_FINAL notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan final approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when loan is rejected.
     */
    private void publishLoanRejectedNotification(Loan loan, Employee employee, String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "");
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_REJECTED,
                    loan.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_REJECTED notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan rejection notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when loan is fully paid off.
     */
    private void publishLoanFullyPaidNotification(Loan loan, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("totalInstallments", loan.getNoOfInstallments().toString());
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_FULLY_PAID,
                    loan.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_FULLY_PAID notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan fully paid notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when loan installment is paid.
     */
    private void publishLoanInstallmentPaidNotification(Loan loan, Employee employee, BigDecimal paidAmount) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("paidAmount", paidAmount.toString());
            variables.put("remainingBalance", loan.getRemainingBalance().toString());
            variables.put("linkUrl", "/loans/" + loan.getLoanId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_INSTALLMENT_PAID,
                    loan.getEmployeeNo(),
                    NotificationPriority.LOW,
                    "LOAN",
                    loan.getLoanId(),
                    variables));

            log.debug("Published LOAN_INSTALLMENT_PAID notification for loan {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("Failed to publish loan installment paid notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when postponement request is submitted.
     */
    private void publishLoanPostponementSubmittedNotification(LoanPostponementRequest request,
            Employee employee, Loan loan) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("currentDueDate", request.getCurrentDueDate().toString());
            variables.put("newDueDate", request.getNewDueDate().toString());
            variables.put("postponementReason",
                    request.getPostponementReason() != null ? request.getPostponementReason() : "");
            variables.put("linkUrl", "/loans/" + loan.getLoanId() + "/postponements/" + request.getRequestId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_POSTPONEMENT_SUBMITTED,
                    request.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "LOAN_POSTPONEMENT",
                    request.getRequestId(),
                    variables));

            log.debug("Published LOAN_POSTPONEMENT_SUBMITTED notification for request {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to publish loan postponement submitted notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when postponement request is approved.
     */
    private void publishLoanPostponementApprovedNotification(LoanPostponementRequest request,
            Employee employee, Loan loan) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("currentDueDate", request.getCurrentDueDate().toString());
            variables.put("newDueDate", request.getNewDueDate().toString());
            variables.put("linkUrl", "/loans/" + loan.getLoanId() + "/postponements/" + request.getRequestId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_POSTPONEMENT_APPROVED,
                    loan.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LOAN_POSTPONEMENT",
                    request.getRequestId(),
                    variables));

            log.debug("Published LOAN_POSTPONEMENT_APPROVED notification for request {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to publish loan postponement approved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when postponement request is rejected.
     */
    private void publishLoanPostponementRejectedNotification(LoanPostponementRequest request,
            Employee employee, Loan loan, String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("loanAmount", loan.getLoanAmount().toString());
            variables.put("currentDueDate", request.getCurrentDueDate().toString());
            variables.put("newDueDate", request.getNewDueDate().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "");
            variables.put("linkUrl", "/loans/" + loan.getLoanId() + "/postponements/" + request.getRequestId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LOAN_POSTPONEMENT_REJECTED,
                    loan.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LOAN_POSTPONEMENT",
                    request.getRequestId(),
                    variables));

            log.debug("Published LOAN_POSTPONEMENT_REJECTED notification for request {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to publish loan postponement rejected notification: {}", e.getMessage(), e);
        }
    }
}

