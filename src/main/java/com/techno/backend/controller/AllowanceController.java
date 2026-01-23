package com.techno.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.EmpMonthlyAllowance;
import com.techno.backend.service.AllowanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import com.techno.backend.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing employee monthly allowances.
 *
 * Provides endpoints for:
 * - Manual allowance submission (HR/Admin)
 * - Approval/rejection workflow
 * - Querying allowances by employee, date, or approver
 * - Deletion of pending allowances
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 6 - Allowances & Deductions
 */
@RestController
@RequestMapping("/allowances")
@RequiredArgsConstructor
public class AllowanceController {

        private static final Logger log = LoggerFactory.getLogger(AllowanceController.class);

        private final AllowanceService allowanceService;

        /**
         * Submit a manual allowance request.
         * Requires HR or Admin role.
         *
         * @param request Allowance submission request
         * @return Created allowance record
         */
        @PostMapping
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
        public ResponseEntity<AllowanceResponse> submitAllowance(
                        @Valid @RequestBody AllowanceRequest request) {
                log.info("POST /allowances - Employee: {}, Type: {}, Amount: {}",
                                request.employeeNo, request.typeCode, request.amount);

                EmpMonthlyAllowance allowance = allowanceService.submitAllowance(
                                request.employeeNo,
                                request.typeCode,
                                request.transactionDate,
                                request.amount,
                                request.notes);

                return ResponseEntity.ok(toResponse(allowance));
        }

        /**
         * Get allowances for a specific employee.
         * Accessible by employee themselves, managers, HR, and admins.
         *
         * @param employeeNo Employee number
         * @return List of employee allowances
         */
        @GetMapping("/employee/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<List<AllowanceResponse>> getEmployeeAllowances(
                        @PathVariable Long employeeNo) {
                log.info("GET /allowances/employee/{}", employeeNo);

                List<EmpMonthlyAllowance> allowances = allowanceService.getEmployeeAllowances(employeeNo);
                List<AllowanceResponse> responses = allowances.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Get allowances for a specific month.
         * Requires HR, Finance, or Admin role.
         *
         * @param startDate Start date of range
         * @param endDate   End date of range
         * @return List of allowances in the date range
         */
        @GetMapping("/month")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<List<AllowanceResponse>> getAllowancesByMonth(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                log.info("GET /allowances/month?startDate={}&endDate={}", startDate, endDate);

                List<EmpMonthlyAllowance> allowances = allowanceService.getAllowancesByDateRange(startDate, endDate);
                List<AllowanceResponse> responses = allowances.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Get pending allowances for the current approver.
         * Requires HR, Manager, or Admin role.
         *
         * @param approverId Approver employee number
         * @return List of pending allowances
         */
        @GetMapping("/pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<List<AllowanceResponse>> getPendingAllowances(
                        @RequestParam Long approverId) {
                log.info("GET /allowances/pending?approverId={}", approverId);

                List<EmpMonthlyAllowance> allowances = allowanceService.getPendingAllowances(approverId);
                List<AllowanceResponse> responses = allowances.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Approve an allowance request.
         * Requires HR, Manager, or Admin role.
         *
         * @param id      Allowance transaction number
         * @param request Approval request with approver number
         * @return Updated allowance record
         */
        @PostMapping("/{id}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<AllowanceResponse> approveAllowance(
                        @PathVariable Long id,
                        @Valid @RequestBody ApprovalRequest request) {
                log.info("POST /allowances/{}/approve - Approver: {}", id, request.approverNo);

                EmpMonthlyAllowance allowance = allowanceService.approveAllowance(id, request.approverNo);
                return ResponseEntity.ok(toResponse(allowance));
        }

        /**
         * Reject an allowance request.
         * Requires HR, Manager, or Admin role.
         *
         * @param id      Allowance transaction number
         * @param request Rejection request with approver number and reason
         * @return Updated allowance record
         */
        @PostMapping("/{id}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<AllowanceResponse> rejectAllowance(
                        @PathVariable Long id,
                        @Valid @RequestBody RejectionRequest request) {
                log.info("POST /allowances/{}/reject - Approver: {}, Reason: {}",
                                id, request.approverNo, request.rejectionReason);

                EmpMonthlyAllowance allowance = allowanceService.rejectAllowance(
                                id, request.approverNo, request.rejectionReason);
                return ResponseEntity.ok(toResponse(allowance));
        }

        /**
         * Delete a pending allowance.
         * Only pending (not approved) allowances can be deleted.
         * Requires HR or Admin role.
         *
         * @param id          Allowance transaction number
         * @param requestorNo Employee number requesting deletion
         * @return No content
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
        public ResponseEntity<Void> deleteAllowance(
                        @PathVariable Long id,
                        @RequestParam Long requestorNo) {
                log.info("DELETE /allowances/{} - Requestor: {}", id, requestorNo);

                allowanceService.deleteAllowance(id, requestorNo);
                return ResponseEntity.noContent().build();
        }

        /**
         * Get all allowances with pagination and optional filters.
         * Used for listing all allowances in the allowance requests page.
         *
         * @param transStatus   Transaction status (N/A/R) - optional
         * @param employeeNo    Employee number - optional
         * @param startDate     Start date (optional) - filters by transactionDate
         * @param endDate       End date (optional) - filters by transactionDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: transactionDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of allowances with employee names
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<AllowanceResponse>>> getAllAllowances(
                        @RequestParam(required = false) String transStatus,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "transactionDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                try {
                        log.info("GET /allowances/list - status={}, employee={}, startDate={}, endDate={}, page={}, size={}",
                                        transStatus, employeeNo, startDate, endDate, page, size);

                        // Apply role-based filtering for Employees
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                                // Employees can only see their own allowances
                                Long currentEmployeeNo = getCurrentEmployeeNo();
                                if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                        employeeNo = currentEmployeeNo;
                                }
                        }

                        log.debug("Creating pageable with sortBy={}, sortDirection={}", sortBy, sortDirection);
                        Sort sort = sortDirection.equalsIgnoreCase("asc")
                                        ? Sort.by(sortBy).ascending()
                                        : Sort.by(sortBy).descending();

                        Pageable pageable = PageRequest.of(page, size, sort);

                        log.debug("Calling allowanceService.getAllAllowances...");
                        Page<EmpMonthlyAllowance> allowancePage = allowanceService.getAllAllowances(
                                        transStatus, employeeNo, startDate, endDate, pageable);

                        log.debug("Got {} allowances, converting to response...", allowancePage.getTotalElements());
                        // Convert to AllowanceResponse with employee names
                        Page<AllowanceResponse> response = allowancePage.map(this::toResponse);

                        log.info("Successfully retrieved {} allowances", response.getTotalElements());
                        return ResponseEntity.ok(ApiResponse.success(
                                        "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¨Ø¯Ù„Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                                        response));

                } catch (Exception e) {
                        log.error("ERROR in getAllAllowances: Exception type: {}, Message: {}",
                                        e.getClass().getName(), e.getMessage(), e);
                        throw e; // Rethrow to let GlobalExceptionHandler handle it
                }
        }

        /**
         * Get allowance by ID.
         *
         * @param id Allowance transaction number
         * @return Allowance record
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<AllowanceResponse>> getAllowanceById(@PathVariable Long id) {
                log.info("GET /allowances/{}", id);

                EmpMonthlyAllowance allowance = allowanceService.getAllowanceById(id);

                // For EMPLOYEE role, ensure they can only access their own allowances
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (!allowance.getEmployeeNo().equals(currentEmployeeNo)) {
                                throw new UnauthorizedException("ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø§Ù„ÙˆØµÙˆÙ„ ÙÙ‚Ø· Ø¥Ù„Ù‰ Ø¨Ø¯Ù„Ø§ØªÙ‡Ù… Ø§Ù„Ø®Ø§ØµØ©");
                        }
                }

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¨Ø¯Ù„ Ø¨Ù†Ø¬Ø§Ø­",
                                toResponse(allowance)));
        }

        /**
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
         * Convert entity to response DTO.
         */
        private AllowanceResponse toResponse(EmpMonthlyAllowance allowance) {
                return new AllowanceResponse(
                                allowance.getTransactionNo(),
                                allowance.getEmployeeNo(),
                                allowance.getEmployee() != null ? allowance.getEmployee().getEmployeeName() : null,
                                allowance.getTypeCode(),
                                allowance.getTransactionType() != null ? allowance.getTransactionType().getTypeName()
                                                : null,
                                allowance.getTransactionDate(),
                                allowance.getAllowanceAmount(),
                                allowance.getEntryReason(),
                                allowance.getTransStatus(),
                                allowance.getIsManualEntry(),
                                allowance.getApprovedDate(),
                                allowance.getApprovedBy(),
                                allowance.getRejectionReason(),
                                allowance.getNextApproval(),
                                allowance.getNextAppLevel());
        }

        // ==================== Request/Response DTOs ====================

        /**
         * Request DTO for submitting an allowance.
         */
        public record AllowanceRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,

                        @NotNull(message = "Ø±Ù…Ø² Ø§Ù„Ù†ÙˆØ¹ Ù…Ø·Ù„ÙˆØ¨") Long typeCode,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ù…Ø·Ù„ÙˆØ¨") LocalDate transactionDate,

                        @NotNull(message = "Ø§Ù„Ù…Ø¨Ù„Øº Ù…Ø·Ù„ÙˆØ¨") @Positive(message = "ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø§Ù„Ù…Ø¨Ù„Øº Ø£ÙƒØ¨Ø± Ù…Ù† ØµÙØ±") BigDecimal amount,

                        String notes) {
        }

        /**
         * Response DTO for allowance data.
         */
        public record AllowanceResponse(
                        Long transactionNo,
                        Long employeeNo,
                        String employeeName,
                        Long typeCode,
                        String typeName,
                        LocalDate transactionDate,
                        BigDecimal amount,
                        String notes,
                        String transStatus,
                        String isManualEntry,
                        LocalDateTime approvedDate,
                        Long approvedBy,
                        String rejectionReason,
                        Long nextApproval,
                        Integer nextAppLevel) {
        }

        /**
         * Request DTO for approving an allowance.
         */
        public record ApprovalRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo) {
        }

        /**
         * Request DTO for rejecting an allowance.
         */
        public record RejectionRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨") String rejectionReason) {
        }
}

