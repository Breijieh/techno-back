package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Loan;
import com.techno.backend.entity.LoanInstallment;
import com.techno.backend.entity.LoanPostponementRequest;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.LoanInstallmentRepository;
import com.techno.backend.repository.LoanRepository;
import com.techno.backend.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for Loan Management.
 *
 * Provides endpoints for:
 * - Loan request submission
 * - Multi-level approval workflow (HR â†’ Finance)
 * - Installment schedule management
 * - Loan postponement requests
 * - Outstanding balance tracking
 * - Mass postponement operations
 *
 * Approval Flow for Loans:
 * - Level 1: HR Manager
 * - Level 2: Finance Manager (final)
 *
 * Approval Flow for Postponements:
 * - Level 1: HR Manager
 * - Level 2: Finance Manager
 * - Level 3: General Manager (final)
 *
 * Access Control:
 * - Submit loan: EMPLOYEE, MANAGER, ADMIN
 * - Approve/Reject: HR, FINANCE, ADMIN (for loans); +GM (for postponements)
 * - View own data: All authenticated users
 * - View all data: HR, FINANCE, ADMIN
 * - Mass postpone: ADMIN only
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanController {

        private final LoanService loanService;
        private final com.techno.backend.service.ApprovalWorkflowService approvalWorkflowService;
        private final LoanRepository loanRepository;
        private final LoanInstallmentRepository installmentRepository;
        private final EmployeeRepository employeeRepository;

        // ==================== Core Loan Operations ====================

        /**
         * Get approval timeline for loan request.
         *
         * GET /loans/{loanId}/timeline
         *
         * @param loanId Loan request ID
         * @return List of approval steps
         */
        @GetMapping("/{loanId}/timeline")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<List<com.techno.backend.service.ApprovalWorkflowService.ApprovalStep>> getLoanTimeline(
                        @PathVariable Long loanId) {
                log.info("GET /loans/{}/timeline", loanId);

                Loan loan = loanRepository.findById(loanId)
                                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

                // Get employee details for department/project code
                Employee employee = employeeRepository.findById(loan.getEmployeeNo())
                                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loan.getEmployeeNo()));

                List<com.techno.backend.service.ApprovalWorkflowService.ApprovalStep> timeline = approvalWorkflowService
                                .getApprovalTimeline(
                                                "LOAN",
                                                loan.getEmployeeNo(),
                                                employee.getPrimaryDeptCode(),
                                                employee.getPrimaryProjectCode(),
                                                loan.getNextAppLevel(),
                                                loan.getTransStatus());

                return ResponseEntity.ok(timeline);
        }

        /**
         * Submit new loan request.
         *
         * POST /api/loans/submit
         *
         * Validates: no active loans, amount within limits, installment count.
         * Initializes approval workflow.
         *
         * @param request Submit loan request DTO
         * @return Created loan request
         */
        @PostMapping("/submit")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<LoanDetailsResponse> submitLoanRequest(@Valid @RequestBody SubmitLoanRequest request) {
                log.info("POST /api/loans/submit - Employee: {}, Amount: {}, Installments: {}",
                                request.employeeNo, request.loanAmount, request.noOfInstallments);

                Loan loan = loanService.submitLoanRequest(
                                request.employeeNo,
                                request.loanAmount,
                                request.noOfInstallments,
                                request.firstInstallmentDate);

                log.info("Loan request created: ID={}, Amount={}, Next Approver={}",
                                loan.getLoanId(), request.loanAmount, loan.getNextApproval());

                return ResponseEntity.ok(toLoanDetailsResponse(loan, 0, loan.getNoOfInstallments()));
        }

        /**
         * Approve loan at current approval level.
         *
         * POST /api/loans/{loanId}/approve
         *
         * On final approval: generates installment schedule.
         *
         * @param loanId  Loan request ID
         * @param request Approve request with approver number
         * @return Updated loan request
         */
        @PostMapping("/{loanId}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<LoanDetailsResponse> approveLoan(
                        @PathVariable Long loanId,
                        @Valid @RequestBody ApproveLoanRequest request) {
                log.info("POST /api/loans/{}/approve - Approver: {}", loanId, request.approverNo);

                Loan loan = loanService.approveLoan(loanId, request.approverNo);

                log.info("Loan approved: ID={}, Status={}, NextLevel={}",
                                loanId, loan.getTransStatus(), loan.getNextAppLevel());

                return ResponseEntity.ok(toLoanDetailsResponse(loan, 0, loan.getNoOfInstallments()));
        }

        /**
         * Reject loan request.
         *
         * POST /api/loans/{loanId}/reject
         *
         * @param loanId  Loan request ID
         * @param request Reject request with reason
         * @return Updated loan request
         */
        @PostMapping("/{loanId}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<LoanDetailsResponse> rejectLoan(
                        @PathVariable Long loanId,
                        @Valid @RequestBody RejectLoanRequest request) {
                log.info("POST /api/loans/{}/reject - Approver: {}, Reason: {}",
                                loanId, request.approverNo, request.rejectionReason);

                Loan loan = loanService.rejectLoan(loanId, request.approverNo, request.rejectionReason);

                log.info("Loan rejected: ID={}, Status={}", loanId, loan.getTransStatus());

                return ResponseEntity.ok(toLoanDetailsResponse(loan, 0, loan.getNoOfInstallments()));
        }

        /**
         * Get authenticated user's loans.
         *
         * GET /api/loans/my-loans
         *
         * @return List of user's loans (active and closed)
         */
        @GetMapping("/my-loans")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<LoanDetailsResponse>>> getMyLoans() {
                Long employeeNo = getCurrentEmployeeNo();
                log.info("GET /api/loans/my-loans - Employee: {}", employeeNo);

                List<Loan> loans = loanService.getAllLoans(employeeNo);

                List<LoanDetailsResponse> response = loans.stream()
                                .map(loan -> {
                                        List<LoanInstallment> installments = installmentRepository
                                                        .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId());
                                        int paidCount = (int) installments.stream()
                                                        .filter(i -> "PAID".equals(i.getPaymentStatus()))
                                                        .count();
                                        int unpaidCount = installments.size() - paidCount;
                                        return toLoanDetailsResponse(loan, paidCount, unpaidCount);
                                })
                                .collect(Collectors.toList());

                log.info("Retrieved {} loans for employee {}", response.size(), employeeNo);

                return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù‚Ø±ÙˆØ¶ Ø¨Ù†Ø¬Ø§Ø­", response));
        }

        /**
         * Get active loans for employee.
         *
         * GET /api/loans/active/{employeeNo}
         *
         * @param employeeNo Employee number
         * @return List of active loans
         */
        @GetMapping("/active/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER') or #employeeNo.toString() == authentication.name")
        public ResponseEntity<List<LoanDetailsResponse>> getActiveLoans(@PathVariable Long employeeNo) {
                log.info("GET /api/loans/active/{}", employeeNo);

                List<Loan> loans = loanService.getActiveLoans(employeeNo);

                List<LoanDetailsResponse> response = loans.stream()
                                .map(loan -> {
                                        List<LoanInstallment> installments = installmentRepository
                                                        .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId());
                                        int paidCount = (int) installments.stream()
                                                        .filter(i -> "PAID".equals(i.getPaymentStatus())).count();
                                        int unpaidCount = installments.size() - paidCount;
                                        return toLoanDetailsResponse(loan, paidCount, unpaidCount);
                                })
                                .collect(Collectors.toList());

                log.info("Retrieved {} active loans for employee {}", response.size(), employeeNo);

                return ResponseEntity.ok(response);
        }

        /**
         * Get pending loan approvals for authenticated user.
         *
         * GET /api/loans/pending-approvals
         *
         * @return List of pending loans
         */
        @GetMapping("/pending-approvals")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER')")
        public ResponseEntity<ApiResponse<List<LoanDetailsResponse>>> getPendingApprovals() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                boolean hasAdminView = authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                                a.getAuthority().equals("ROLE_GENERAL_MANAGER") ||
                                                a.getAuthority().equals("ROLE_HR_MANAGER"));

                List<Loan> pendingLoans;
                if (hasAdminView) {
                        log.info("GET /api/loans/pending-approvals - Admin/Manager view, returning all pending loans");
                        pendingLoans = loanService.getAllPendingLoans();
                } else {
                        Long approverNo = getCurrentEmployeeNo();
                        log.info("GET /api/loans/pending-approvals - Approver: {}", approverNo);
                        pendingLoans = loanService.getPendingLoansForApprover(approverNo);
                }

                List<LoanDetailsResponse> response = pendingLoans.stream()
                                .map(loan -> {
                                        List<LoanInstallment> installments = installmentRepository
                                                        .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId());
                                        int paidCount = (int) installments.stream()
                                                        .filter(i -> "PAID".equals(i.getPaymentStatus())).count();
                                        int unpaidCount = installments.size() - paidCount;
                                        return toLoanDetailsResponse(loan, paidCount, unpaidCount);
                                })
                                .collect(Collectors.toList());

                return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù‚Ø±ÙˆØ¶ Ø§Ù„Ù…Ø¹Ù„Ù‚Ø© Ø¨Ù†Ø¬Ø§Ø­", response));
        }

        /**
         * Get all loan requests with optional filters.
         * Used for listing all loan requests in the loan requests page.
         *
         * GET
         * /loans/list?status=N&employeeNo=123&startDate=2025-01-01&endDate=2025-01-31&page=0&size=20
         *
         * @param transStatus   Transaction status (N/A/R) - optional
         * @param employeeNo    Employee number - optional
         * @param startDate     Start date (optional) - filters by requestDate
         * @param endDate       End date (optional) - filters by requestDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: requestDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of loan requests with employee names
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<Page<LoanDetailsResponse>>> getAllLoans(
                        @RequestParam(required = false) String transStatus,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "requestDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /loans/list - status={}, employee={}, startDate={}, endDate={}",
                                transStatus, employeeNo, startDate, endDate);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own loans
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Loan> loanPage = loanService.getAllLoans(
                                transStatus, employeeNo, startDate, endDate, pageable);

                // Convert to LoanDetailsResponse with employee names
                Page<LoanDetailsResponse> response = loanPage.map(loan -> {
                        // Count paid/unpaid installments
                        List<LoanInstallment> installments = installmentRepository
                                        .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId());
                        int paidCount = (int) installments.stream()
                                        .filter(i -> "PAID".equals(i.getPaymentStatus()))
                                        .count();
                        int unpaidCount = installments.size() - paidCount;
                        return toLoanDetailsResponse(loan, paidCount, unpaidCount);
                });

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù‚Ø±ÙˆØ¶ Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        /**
         * Get loan details.
         *
         * GET /loans/{loanId}
         *
         * @param loanId Loan ID
         * @return Loan details
         */
        @GetMapping("/{loanId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<LoanDetailsResponse> getLoanDetails(@PathVariable Long loanId) {
                log.info("GET /loans/{}", loanId);

                Loan loan = loanService.getLoanDetails(loanId);

                // Count paid/unpaid installments
                int paidCount = (int) loan.getInstallments().stream()
                                .filter(i -> "PAID".equals(i.getPaymentStatus()))
                                .count();
                int unpaidCount = loan.getInstallments().size() - paidCount;

                LoanDetailsResponse response = toLoanDetailsResponse(loan, paidCount, unpaidCount);

                log.info("Retrieved loan details: ID={}, Employee={}, Status={}",
                                loanId, loan.getEmployeeNo(), loan.getTransStatus());

                return ResponseEntity.ok(response);
        }

        /**
         * Get loan installment schedule.
         *
         * GET /api/loans/{loanId}/installments
         *
         * @param loanId Loan ID
         * @return Loan with installments
         */
        @GetMapping("/{loanId}/installments")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<LoanWithInstallmentsResponse> getLoanInstallments(@PathVariable Long loanId) {
                log.info("GET /api/loans/{}/installments", loanId);

                Loan loan = loanService.getLoanDetails(loanId);

                // Count paid/unpaid installments
                int paidCount = (int) loan.getInstallments().stream()
                                .filter(i -> "PAID".equals(i.getPaymentStatus()))
                                .count();
                int unpaidCount = loan.getInstallments().size() - paidCount;

                LoanDetailsResponse loanDetails = toLoanDetailsResponse(loan, paidCount, unpaidCount);

                List<InstallmentDetailsResponse> installments = loan.getInstallments().stream()
                                .map(this::toInstallmentDetailsResponse)
                                .collect(Collectors.toList());

                LoanWithInstallmentsResponse response = new LoanWithInstallmentsResponse(loanDetails, installments);

                log.info("Retrieved {} installments for loan {}", installments.size(), loanId);

                return ResponseEntity.ok(response);
        }

        /**
         * Get outstanding loan balance for employee.
         *
         * GET /api/loans/balance/{employeeNo}
         *
         * @param employeeNo Employee number
         * @return Outstanding balance details
         */
        @GetMapping("/balance/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER') or #employeeNo.toString() == authentication.name")
        public ResponseEntity<OutstandingBalanceResponse> getOutstandingBalance(@PathVariable Long employeeNo) {
                log.info("GET /api/loans/balance/{}", employeeNo);

                // Get employee name
                Employee employee = employeeRepository.findById(employeeNo)
                                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

                BigDecimal totalOutstanding = loanService.getOutstandingBalance(employeeNo);

                // Get active loans details
                List<Loan> activeLoans = loanService.getActiveLoans(employeeNo);

                List<LoanSummaryResponse> loanSummaries = activeLoans.stream()
                                .map(loan -> new LoanSummaryResponse(
                                                loan.getLoanId(),
                                                loan.getLoanAmount(),
                                                loan.getRemainingBalance(),
                                                (int) installmentRepository
                                                                .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId())
                                                                .stream()
                                                                .filter(i -> "UNPAID".equals(i.getPaymentStatus())
                                                                                || "POSTPONED".equals(
                                                                                                i.getPaymentStatus()))
                                                                .count()))
                                .collect(Collectors.toList());

                OutstandingBalanceResponse response = new OutstandingBalanceResponse(
                                employeeNo,
                                employee.getEmployeeName(),
                                totalOutstanding,
                                activeLoans.size(),
                                loanSummaries);

                log.info("Outstanding balance for employee {}: {} SAR across {} active loans",
                                employeeNo, totalOutstanding, activeLoans.size());

                return ResponseEntity.ok(response);
        }

        // ==================== Loan Postponement Operations ====================

        /**
         * Submit installment postponement request.
         *
         * POST /api/loans/postponement/submit
         *
         * @param request Postponement request DTO
         * @return Created postponement request
         */
        @PostMapping("/postponement/submit")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<PostponementDetailsResponse> submitPostponementRequest(
                        @Valid @RequestBody SubmitPostponementRequest request) {
                log.info("POST /api/loans/postponement/submit - Loan: {}, Installment: {}, New Date: {}",
                                request.loanId, request.installmentId, request.newDueDate);

                LoanPostponementRequest postponement = loanService.submitPostponementRequest(
                                request.loanId,
                                request.installmentId,
                                request.newDueDate,
                                request.postponementReason);

                log.info("Postponement request created: ID={}, Next Approver={}",
                                postponement.getRequestId(), postponement.getNextApproval());

                return ResponseEntity.ok(toPostponementDetailsResponse(postponement));
        }

        /**
         * Approve postponement at current level.
         *
         * POST /api/loans/postponement/{requestId}/approve
         *
         * On final approval: updates installment due date.
         *
         * @param requestId Postponement request ID
         * @param request   Approve request with approver number
         * @return Updated postponement request
         */
        @PostMapping("/postponement/{requestId}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<PostponementDetailsResponse> approvePostponement(
                        @PathVariable Long requestId,
                        @Valid @RequestBody ApprovePostponementRequest request) {
                log.info("POST /api/loans/postponement/{}/approve - Approver: {}", requestId, request.approverNo);

                LoanPostponementRequest postponement = loanService.approvePostponement(requestId, request.approverNo);

                log.info("Postponement approved: ID={}, Status={}, NextLevel={}",
                                requestId, postponement.getTransStatus(), postponement.getNextAppLevel());

                return ResponseEntity.ok(toPostponementDetailsResponse(postponement));
        }

        /**
         * Reject postponement request.
         *
         * POST /api/loans/postponement/{requestId}/reject
         *
         * @param requestId Postponement request ID
         * @param request   Reject request with reason
         * @return Updated postponement request
         */
        @PostMapping("/postponement/{requestId}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<PostponementDetailsResponse> rejectPostponement(
                        @PathVariable Long requestId,
                        @Valid @RequestBody RejectPostponementRequest request) {
                log.info("POST /api/loans/postponement/{}/reject - Approver: {}, Reason: {}",
                                requestId, request.approverNo, request.rejectionReason);

                LoanPostponementRequest postponement = loanService.rejectPostponement(
                                requestId,
                                request.approverNo,
                                request.rejectionReason);

                log.info("Postponement rejected: ID={}, Status={}", requestId, postponement.getTransStatus());

                return ResponseEntity.ok(toPostponementDetailsResponse(postponement));
        }

        /**
         * Get pending postponement approvals.
         *
         * GET /api/loans/postponement/pending-approvals
         *
         * @return List of pending postponements
         */
        @GetMapping("/postponement/pending-approvals")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<List<LoanPostponementRequest>> getPendingPostponements() {
                Long approverNo = getCurrentEmployeeNo();
                log.info("GET /api/loans/postponement/pending-approvals - Approver: {}", approverNo);

                List<LoanPostponementRequest> pendingPostponements = loanService
                                .getPendingPostponementsForApprover(approverNo);

                log.info("Retrieved {} pending postponements for approver {}", pendingPostponements.size(), approverNo);

                return ResponseEntity.ok(pendingPostponements);
        }

        /**
         * Mass postpone installments by month.
         *
         * POST /api/loans/postponement/mass-postpone
         *
         * Admin-only endpoint for bulk postponement (e.g., Ramadan).
         *
         * @param request Mass postpone request
         * @return Summary of postponements
         */
        @PostMapping("/postponement/mass-postpone")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<MassPostponeResponse> massPostpone(@Valid @RequestBody MassPostponeRequest request) {
                log.info("POST /api/loans/postponement/mass-postpone - From: {}, To: {}",
                                request.originalMonth, request.newMonth);

                LoanService.MassPostponeResult result = loanService.massPostponeByMonth(
                                request.originalMonth,
                                request.newMonth,
                                request.reason);

                MassPostponeResponse response = new MassPostponeResponse(
                                result.originalMonth(),
                                result.newMonth(),
                                result.affectedEmployees(),
                                result.totalInstallmentsPostponed(),
                                result.affectedEmployeeNumbers());

                log.info("Mass postponed {} installments for {} employees from {} to {}",
                                result.totalInstallmentsPostponed(), result.affectedEmployees(),
                                request.originalMonth, request.newMonth);

                return ResponseEntity.ok(response);
        }

        /**
         * Get unpaid installments for a loan.
         * Used by the postponement form to populate installment dropdown.
         *
         * GET /api/loans/{loanId}/installments/unpaid
         *
         * @param loanId Loan ID
         * @return List of unpaid/postponed installments
         */
        @GetMapping("/{loanId}/installments/unpaid")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<InstallmentDetailsResponse>>> getUnpaidInstallments(
                        @PathVariable Long loanId) {
                log.info("GET /loans/{}/installments/unpaid", loanId);

                Loan loan = loanService.getLoanDetails(loanId);

                // Filter unpaid and postponed installments
                List<InstallmentDetailsResponse> unpaidInstallments = loan.getInstallments().stream()
                                .filter(i -> "UNPAID".equals(i.getPaymentStatus())
                                                || "POSTPONED".equals(i.getPaymentStatus()))
                                .map(this::toInstallmentDetailsResponse)
                                .collect(Collectors.toList());

                log.info("Retrieved {} unpaid installments for loan {}", unpaidInstallments.size(), loanId);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø£Ù‚Ø³Ø§Ø· ØºÙŠØ± Ø§Ù„Ù…Ø¯ÙÙˆØ¹Ø© Ø¨Ù†Ø¬Ø§Ø­",
                                unpaidInstallments));
        }

        /**
         * Get all postponement requests with pagination and optional filters.
         * Used for listing all postponement requests in the postponement page.
         *
         * @param transStatus   Transaction status (N/A/R) - optional
         * @param employeeNo    Employee number - optional
         * @param startDate     Start date (optional) - filters by requestDate
         * @param endDate       End date (optional) - filters by requestDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: requestDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of postponement requests with employee names
         */
        @GetMapping("/postponement/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<PostponementDetailsResponse>>> getAllPostponementRequests(
                        @RequestParam(required = false) String transStatus,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "requestDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /loans/postponement/list - status={}, employee={}, startDate={}, endDate={}",
                                transStatus, employeeNo, startDate, endDate);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own postponements
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<LoanPostponementRequest> postponementPage = loanService.getAllPostponementRequests(
                                transStatus, employeeNo, startDate, endDate, pageable);

                // Convert to PostponementDetailsResponse with employee names
                Page<PostponementDetailsResponse> response = postponementPage.map(this::toPostponementDetailsResponse);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø·Ù„Ø¨Ø§Øª Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        /**
         * Get all loan installments with pagination and optional filters.
         * Used for listing all installments in the installments schedule page.
         *
         * @param employeeNo    Employee number - optional
         * @param loanId        Loan ID - optional
         * @param paymentStatus Payment status (PAID/UNPAID/POSTPONED) - optional
         * @param startDate     Start date (optional) - filters by dueDate
         * @param endDate       End date (optional) - filters by dueDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: dueDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of installments with employee names
         */
        @GetMapping("/installments/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<InstallmentScheduleResponse>>> getAllInstallments(
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) Long loanId,
                        @RequestParam(required = false) String paymentStatus,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "dueDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /loans/installments/list - employee={}, loan={}, status={}, startDate={}, endDate={}",
                                employeeNo, loanId, paymentStatus, startDate, endDate);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own installments
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                // Map entity field name to database column name for native query sorting
                String sortColumn = mapFieldToColumn(sortBy);

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortColumn).ascending()
                                : Sort.by(sortColumn).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<LoanInstallment> installmentPage = loanService.getAllInstallments(
                                employeeNo, loanId, paymentStatus, startDate, endDate, pageable);

                // Convert to InstallmentScheduleResponse with employee names
                Page<InstallmentScheduleResponse> response = installmentPage.map(this::toInstallmentScheduleResponse);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø£Ù‚Ø³Ø§Ø· Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        // ==================== Helper Methods ====================

        /**
         * Maps entity field names (camelCase) to database column names (snake_case).
         * Used for native query sorting where Spring Data JPA needs database column
         * names.
         *
         * @param fieldName Entity field name in camelCase (e.g., "dueDate")
         * @return Database column name in snake_case (e.g., "due_date")
         */
        private String mapFieldToColumn(String fieldName) {
                // Map common fields explicitly
                Map<String, String> fieldMapping = Map.of(
                                "dueDate", "due_date",
                                "installmentNo", "installment_no",
                                "loanId", "loan_id",
                                "paymentStatus", "payment_status",
                                "paidDate", "paid_date");

                // Return mapped column name or convert camelCase to snake_case automatically
                return fieldMapping.getOrDefault(fieldName,
                                fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase());
        }

        /**
         * Get current authenticated user's employee number.
         *
         * Extracts employee number from security context.
         * Uses SecurityContextHolder to get the current authenticated user.
         *
         * @return Employee number from security context
         * @throws RuntimeException if authentication is missing or invalid
         */
        private Long getCurrentEmployeeNo() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                        log.warn("No authentication found in security context");
                        return null;
                }

                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                        return (Long) principal;
                }

                if (principal instanceof String || authentication.getName() != null) {
                        try {
                                return Long.parseLong(authentication.getName());
                        } catch (NumberFormatException e) {
                                log.debug("Principal is not an employee number: {}", authentication.getName());
                                return null;
                        }
                }

                return null;
        }

        /**
         * Convert Loan entity to LoanDetailsResponse DTO.
         *
         * @param loan        Loan entity
         * @param paidCount   Number of paid installments
         * @param unpaidCount Number of unpaid installments
         * @return Loan details response DTO with enriched employee names
         */
        private LoanDetailsResponse toLoanDetailsResponse(Loan loan, int paidCount, int unpaidCount) {
                // Get employee name
                String employeeName = employeeRepository.findById(loan.getEmployeeNo())
                                .map(Employee::getEmployeeName)
                                .orElse(null);

                // Get next approver name
                String nextApproverName = null;
                if (loan.getNextApproval() != null) {
                        nextApproverName = employeeRepository.findById(loan.getNextApproval())
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // Get approved by name
                String approvedByName = null;
                if (loan.getApprovedBy() != null) {
                        approvedByName = employeeRepository.findById(loan.getApprovedBy())
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // Get status description
                String status = loan.getTransStatus();
                String statusDescription = "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ";
                if (status != null) {
                        statusDescription = switch (status) {
                                case "N" -> "Ù‚ÙŠØ¯ Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©";
                                case "A" -> "Ù…Ø¹ØªÙ…Ø¯";
                                case "R" -> "Ù…Ø±ÙÙˆØ¶";
                                default -> "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ (" + status + ")";
                        };
                }

                return new LoanDetailsResponse(
                                loan.getLoanId(),
                                loan.getEmployeeNo(),
                                employeeName,
                                loan.getLoanAmount(),
                                loan.getNoOfInstallments(),
                                loan.getFirstInstallmentDate(),
                                loan.getInstallmentAmount(),
                                loan.getRemainingBalance(),
                                loan.getRequestDate(),
                                loan.getTransStatus(),
                                statusDescription,
                                loan.getNextApproval(),
                                nextApproverName,
                                loan.getNextAppLevel(),
                                loan.getNextAppLevelName(),
                                loan.getApprovedBy(),
                                approvedByName,
                                loan.getApprovedDate(),
                                loan.getIsActive(),
                                loan.getRejectionReason(),
                                paidCount,
                                unpaidCount);
        }

        /**
         * Convert LoanInstallment entity to InstallmentDetailsResponse DTO.
         *
         * @param installment Loan installment entity
         * @return Installment details response DTO
         */
        private InstallmentDetailsResponse toInstallmentDetailsResponse(LoanInstallment installment) {
                return new InstallmentDetailsResponse(
                                installment.getInstallmentId(),
                                installment.getLoanId(),
                                installment.getInstallmentNo(),
                                installment.getDueDate(),
                                installment.getInstallmentAmount(),
                                installment.getPaidDate(),
                                installment.getPaidAmount(),
                                installment.getPaymentStatus(),
                                installment.getSalaryMonth());
        }

        /**
         * Convert LoanInstallment entity to InstallmentScheduleResponse DTO.
         *
         * @param installment Installment entity
         * @return Installment schedule response DTO with enriched employee names and
         *         postponed date
         */
        private InstallmentScheduleResponse toInstallmentScheduleResponse(LoanInstallment installment) {
                // Get loan to access employee information
                Loan loan = loanRepository.findById(installment.getLoanId())
                                .orElse(null);

                // Get employee name
                String employeeName = null;
                Long employeeNo = null;
                if (loan != null) {
                        employeeNo = loan.getEmployeeNo();
                        employeeName = employeeRepository.findById(employeeNo)
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // If installment is POSTPONED, the dueDate is the new postponed date
                LocalDate postponedTo = null;
                if ("POSTPONED".equals(installment.getPaymentStatus())) {
                        postponedTo = installment.getDueDate();
                }

                return new InstallmentScheduleResponse(
                                installment.getInstallmentId(),
                                installment.getLoanId(),
                                installment.getInstallmentNo(),
                                employeeNo,
                                employeeName,
                                installment.getDueDate(),
                                installment.getInstallmentAmount(),
                                installment.getPaymentStatus(),
                                installment.getPaidDate(),
                                installment.getPaidAmount(),
                                postponedTo,
                                installment.getSalaryMonth());
        }

        /**
         * Convert LoanPostponementRequest entity to PostponementDetailsResponse DTO.
         *
         * @param postponement Postponement request entity
         * @return Postponement details response DTO with enriched employee names and
         *         formatted dates
         */
        private PostponementDetailsResponse toPostponementDetailsResponse(LoanPostponementRequest postponement) {
                // Get loan to access employee information
                Loan loan = loanRepository.findById(postponement.getLoanId())
                                .orElse(null);

                // Get employee name
                String employeeName = null;
                Long employeeNo = null;
                if (loan != null) {
                        employeeNo = loan.getEmployeeNo();
                        employeeName = employeeRepository.findById(employeeNo)
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // Format dates as YYYY-MM for frontend
                String originalMonth = postponement.getCurrentDueDate() != null
                                ? postponement.getCurrentDueDate().toString().substring(0, 7) // YYYY-MM
                                : null;
                String newMonth = postponement.getNewDueDate() != null
                                ? postponement.getNewDueDate().toString().substring(0, 7) // YYYY-MM
                                : null;

                return new PostponementDetailsResponse(
                                postponement.getRequestId(),
                                postponement.getLoanId(),
                                postponement.getInstallmentId(),
                                employeeNo,
                                employeeName,
                                postponement.getCurrentDueDate(),
                                postponement.getNewDueDate(),
                                originalMonth,
                                newMonth,
                                postponement.getPostponementReason(),
                                postponement.getRequestDate(),
                                postponement.getTransStatus(),
                                postponement.getNextApproval(),
                                postponement.getNextAppLevel(),
                                postponement.getApprovedDate(),
                                postponement.getApprovedBy(),
                                postponement.getRejectionReason());
        }

        // ==================== Request/Response DTOs ====================

        public record SubmitLoanRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,

                        @NotNull(message = "Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶ Ù…Ø·Ù„ÙˆØ¨") @Positive(message = "Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù…ÙˆØ¬Ø¨Ø§Ù‹") BigDecimal loanAmount,

                        @NotNull(message = "Ø¹Ø¯Ø¯ Ø§Ù„Ø£Ù‚Ø³Ø§Ø· Ù…Ø·Ù„ÙˆØ¨") @Min(value = 3, message = "Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ø¯Ù†Ù‰ 3 Ø£Ù‚Ø³Ø§Ø· Ù…Ø·Ù„ÙˆØ¨Ø©") @Max(value = 60, message = "Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ù‚ØµÙ‰ 60 Ù‚Ø³Ø·Ø§Ù‹ Ù…Ø³Ù…ÙˆØ­Ø§Ù‹") Integer noOfInstallments,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù‚Ø³Ø· Ø§Ù„Ø£ÙˆÙ„ Ù…Ø·Ù„ÙˆØ¨") @Future(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù‚Ø³Ø· Ø§Ù„Ø£ÙˆÙ„ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ÙÙŠ Ø§Ù„Ù…Ø³ØªÙ‚Ø¨Ù„") LocalDate firstInstallmentDate) {
        }

        public record ApproveLoanRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo) {
        }

        public record RejectLoanRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨") @Size(max = 500, message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² 500 Ø­Ø±Ù") String rejectionReason) {
        }

        public record SubmitPostponementRequest(
                        @NotNull(message = "Ù…Ø¹Ø±Ù Ø§Ù„Ù‚Ø±Ø¶ Ù…Ø·Ù„ÙˆØ¨") Long loanId,

                        @NotNull(message = "Ù…Ø¹Ø±Ù Ø§Ù„Ù‚Ø³Ø· Ù…Ø·Ù„ÙˆØ¨") Long installmentId,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ø³ØªØ­Ù‚Ø§Ù‚ Ø§Ù„Ø¬Ø¯ÙŠØ¯ Ù…Ø·Ù„ÙˆØ¨") @Future(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ø³ØªØ­Ù‚Ø§Ù‚ Ø§Ù„Ø¬Ø¯ÙŠØ¯ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ÙÙŠ Ø§Ù„Ù…Ø³ØªÙ‚Ø¨Ù„") LocalDate newDueDate,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù…Ø·Ù„ÙˆØ¨") @Size(max = 500, message = "Ø§Ù„Ø³Ø¨Ø¨ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² 500 Ø­Ø±Ù") String postponementReason) {
        }

        public record ApprovePostponementRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo) {
        }

        public record RejectPostponementRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨") String rejectionReason) {
        }

        public record MassPostponeRequest(
                        @NotBlank(message = "Ø§Ù„Ø´Ù‡Ø± Ø§Ù„Ø£ØµÙ„ÙŠ Ù…Ø·Ù„ÙˆØ¨ (ØªÙ†Ø³ÙŠÙ‚ YYYY-MM)") @Pattern(regexp = "\\d{4}-\\d{2}", message = "Ø§Ù„Ø´Ù‡Ø± ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨ØªÙ†Ø³ÙŠÙ‚ YYYY-MM") String originalMonth,

                        @NotBlank(message = "Ø§Ù„Ø´Ù‡Ø± Ø§Ù„Ø¬Ø¯ÙŠØ¯ Ù…Ø·Ù„ÙˆØ¨ (ØªÙ†Ø³ÙŠÙ‚ YYYY-MM)") @Pattern(regexp = "\\d{4}-\\d{2}", message = "Ø§Ù„Ø´Ù‡Ø± ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨ØªÙ†Ø³ÙŠÙ‚ YYYY-MM") String newMonth,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„ØªØ£Ø¬ÙŠÙ„ Ù…Ø·Ù„ÙˆØ¨") String reason) {
        }

        public record LoanDetailsResponse(
                        Long loanId,
                        Long employeeNo,
                        String employeeName,
                        BigDecimal loanAmount,
                        Integer noOfInstallments,
                        LocalDate firstInstallmentDate,
                        BigDecimal installmentAmount,
                        BigDecimal remainingBalance,
                        LocalDate requestDate,
                        String transStatus,
                        String statusDescription,
                        Long nextApproval,
                        String nextApproverName,
                        Integer nextAppLevel,
                        String nextAppLevelName,
                        Long approvedBy,
                        String approvedByName,
                        LocalDateTime approvedDate,
                        String isActive,
                        String rejectionReason,
                        int paidInstallments,
                        int unpaidInstallments) {
        }

        public record LoanWithInstallmentsResponse(
                        LoanDetailsResponse loan,
                        List<InstallmentDetailsResponse> installments) {
        }

        public record InstallmentDetailsResponse(
                        Long installmentId,
                        Long loanId,
                        Integer installmentNo,
                        LocalDate dueDate,
                        BigDecimal installmentAmount,
                        LocalDate paidDate,
                        BigDecimal paidAmount,
                        String paymentStatus,
                        String salaryMonth) {
        }

        public record PostponementDetailsResponse(
                        Long requestId,
                        Long loanId,
                        Long installmentId,
                        Long employeeNo,
                        String employeeName,
                        LocalDate currentDueDate,
                        LocalDate newDueDate,
                        String originalMonth, // YYYY-MM format
                        String newMonth, // YYYY-MM format
                        String postponementReason,
                        LocalDate requestDate,
                        String transStatus,
                        Long nextApproval,
                        Integer nextAppLevel,
                        LocalDateTime approvedDate,
                        Long approvedBy,
                        String rejectionReason) {
        }

        public record InstallmentScheduleResponse(
                        Long installmentId,
                        Long loanId,
                        Integer installmentNo,
                        Long employeeNo,
                        String employeeName,
                        LocalDate dueDate,
                        BigDecimal installmentAmount,
                        String paymentStatus,
                        LocalDate paidDate,
                        BigDecimal paidAmount,
                        LocalDate postponedTo, // New due date if POSTPONED, null otherwise
                        String salaryMonth) {
        }

        public record OutstandingBalanceResponse(
                        Long employeeNo,
                        String employeeName,
                        BigDecimal totalOutstanding,
                        int activeLoansCount,
                        List<LoanSummaryResponse> activeLoans) {
        }

        public record LoanSummaryResponse(
                        Long loanId,
                        BigDecimal loanAmount,
                        BigDecimal remainingBalance,
                        int remainingInstallments) {
        }

        public record MassPostponeResponse(
                        String originalMonth,
                        String newMonth,
                        int affectedEmployees,
                        int totalInstallmentsPostponed,
                        List<Long> affectedEmployeeNumbers) {
        }
}

