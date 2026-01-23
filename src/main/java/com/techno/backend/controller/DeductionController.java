package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.EmpMonthlyDeduction;
import com.techno.backend.service.DeductionService;
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
 * REST Controller for managing employee monthly deductions.
 *
 * Provides endpoints for:
 * - Manual deduction submission (HR/Finance/Admin)
 * - Approval/rejection workflow
 * - Querying deductions by employee, date, or approver
 * - Deletion/cancellation of deductions
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 6 - Allowances & Deductions
 */
@Slf4j
@RestController
@RequestMapping("/deductions")
@RequiredArgsConstructor
public class DeductionController {

        private final DeductionService deductionService;

        /**
         * Submit a manual deduction request.
         * Requires HR, Finance, or Admin role.
         *
         * @param request Deduction submission request
         * @return Created deduction record
         */
        @PostMapping
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<DeductionResponse> submitDeduction(
                        @Valid @RequestBody DeductionRequest request) {
                log.info("POST /api/deductions - Employee: {}, Type: {}, Amount: {}",
                                request.employeeNo, request.typeCode, request.amount);

                EmpMonthlyDeduction deduction = deductionService.submitDeduction(
                                request.employeeNo,
                                request.typeCode,
                                request.transactionDate,
                                request.amount,
                                request.notes);

                return ResponseEntity.ok(toResponse(deduction));
        }

        /**
         * Get deductions for a specific employee.
         * Accessible by employee themselves, managers, HR, Finance, and admins.
         *
         * @param employeeNo Employee number
         * @return List of employee deductions
         */
        @GetMapping("/employee/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<List<DeductionResponse>> getEmployeeDeductions(
                        @PathVariable Long employeeNo) {
                log.info("GET /api/deductions/employee/{}", employeeNo);

                List<EmpMonthlyDeduction> deductions = deductionService.getEmployeeDeductions(employeeNo);
                List<DeductionResponse> responses = deductions.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Get deductions for a specific date range.
         * Requires HR, Finance, or Admin role.
         *
         * @param startDate Start date of range
         * @param endDate   End date of range
         * @return List of deductions in the date range
         */
        @GetMapping("/month")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<List<DeductionResponse>> getDeductionsByMonth(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                log.info("GET /api/deductions/month?startDate={}&endDate={}", startDate, endDate);

                List<EmpMonthlyDeduction> deductions = deductionService.getDeductionsByDateRange(startDate, endDate);
                List<DeductionResponse> responses = deductions.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Get pending deductions for the current approver.
         * Requires HR, Finance, Manager, or Admin role.
         *
         * @param approverId Approver employee number
         * @return List of pending deductions
         */
        @GetMapping("/pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<List<DeductionResponse>> getPendingDeductions(
                        @RequestParam Long approverId) {
                log.info("GET /api/deductions/pending?approverId={}", approverId);

                List<EmpMonthlyDeduction> deductions = deductionService.getPendingDeductions(approverId);
                List<DeductionResponse> responses = deductions.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(responses);
        }

        /**
         * Approve a deduction request.
         * Requires HR, Finance, Manager, or Admin role.
         *
         * @param id      Deduction transaction number
         * @param request Approval request with approver number
         * @return Updated deduction record
         */
        @PostMapping("/{id}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<DeductionResponse> approveDeduction(
                        @PathVariable Long id,
                        @Valid @RequestBody ApprovalRequest request) {
                log.info("POST /api/deductions/{}/approve - Approver: {}", id, request.approverNo);

                EmpMonthlyDeduction deduction = deductionService.approveDeduction(id, request.approverNo);
                return ResponseEntity.ok(toResponse(deduction));
        }

        /**
         * Reject a deduction request.
         * Requires HR, Finance, Manager, or Admin role.
         *
         * @param id      Deduction transaction number
         * @param request Rejection request with approver number and reason
         * @return Updated deduction record
         */
        @PostMapping("/{id}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<DeductionResponse> rejectDeduction(
                        @PathVariable Long id,
                        @Valid @RequestBody RejectionRequest request) {
                log.info("POST /api/deductions/{}/reject - Approver: {}, Reason: {}",
                                id, request.approverNo, request.rejectionReason);

                EmpMonthlyDeduction deduction = deductionService.rejectDeduction(
                                id, request.approverNo, request.rejectionReason);
                return ResponseEntity.ok(toResponse(deduction));
        }

        /**
         * Delete a pending deduction.
         * Only pending (not approved) deductions can be deleted.
         * Requires HR, Finance, or Admin role.
         *
         * @param id          Deduction transaction number
         * @param requestorNo Employee number requesting deletion
         * @return No content
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<Void> deleteDeduction(
                        @PathVariable Long id,
                        @RequestParam Long requestorNo) {
                log.info("DELETE /api/deductions/{} - Requestor: {}", id, requestorNo);

                deductionService.deleteDeduction(id, requestorNo);
                return ResponseEntity.noContent().build();
        }

        /**
         * Get all deductions with pagination and optional filters.
         * Used for listing all deductions in the deduction requests page.
         *
         * @param transStatus   Transaction status (N/A/R) - optional
         * @param employeeNo    Employee number - optional
         * @param startDate     Start date (optional) - filters by transactionDate
         * @param endDate       End date (optional) - filters by transactionDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: transactionDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of deductions with employee names
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<DeductionResponse>>> getAllDeductions(
                        @RequestParam(required = false) String transStatus,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "transactionDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /api/deductions/list - status={}, employee={}, startDate={}, endDate={}",
                                transStatus, employeeNo, startDate, endDate);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own deductions
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<EmpMonthlyDeduction> deductionPage = deductionService.getAllDeductions(
                                transStatus, employeeNo, startDate, endDate, pageable);

                // Convert to DeductionResponse with employee names
                Page<DeductionResponse> response = deductionPage.map(this::toResponse);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø®ØµÙˆÙ…Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        /**
         * Get deduction by ID.
         *
         * @param id Deduction transaction number
         * @return Deduction record
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<DeductionResponse>> getDeductionById(@PathVariable Long id) {
                log.info("GET /api/deductions/{}", id);

                EmpMonthlyDeduction deduction = deductionService.getDeductionById(id);

                // For EMPLOYEE role, ensure they can only access their own deductions
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (!deduction.getEmployeeNo().equals(currentEmployeeNo)) {
                                throw new UnauthorizedException("ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø§Ù„ÙˆØµÙˆÙ„ ÙÙ‚Ø· Ø¥Ù„Ù‰ Ø®ØµÙˆÙ…Ø§ØªÙ‡Ù… Ø§Ù„Ø®Ø§ØµØ©");
                        }
                }

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø®ØµÙ… Ø¨Ù†Ø¬Ø§Ø­",
                                toResponse(deduction)));
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
                        log.error("No authentication found in security context");
                        throw new RuntimeException("Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù… ØºÙŠØ± Ù…ØµØ§Ø¯Ù‚ Ø¹Ù„ÙŠÙ‡");
                }

                // Assuming the principal contains employee number
                try {
                        return Long.parseLong(authentication.getName());
                } catch (NumberFormatException e) {
                        log.error("Failed to parse employee number from authentication: {}", authentication.getName());
                        throw new RuntimeException("Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± ØµØ§Ù„Ø­ ÙÙŠ Ø³ÙŠØ§Ù‚ Ø§Ù„Ù…ØµØ§Ø¯Ù‚Ø©", e);
                }
        }

        /**
         * Convert entity to response DTO.
         */
        private DeductionResponse toResponse(EmpMonthlyDeduction deduction) {
                return new DeductionResponse(
                                deduction.getTransactionNo(),
                                deduction.getEmployeeNo(),
                                deduction.getEmployee() != null ? deduction.getEmployee().getEmployeeName() : null,
                                deduction.getTypeCode(),
                                deduction.getTransactionType() != null ? deduction.getTransactionType().getTypeName()
                                                : null,
                                deduction.getTransactionDate(),
                                deduction.getDeductionAmount(),
                                deduction.getEntryReason(),
                                deduction.getTransStatus(),
                                deduction.getIsManualEntry(),
                                deduction.getApprovedDate(),
                                deduction.getApprovedBy(),
                                deduction.getRejectionReason(),
                                deduction.getNextApproval(),
                                deduction.getNextAppLevel());
        }

        // ==================== Request/Response DTOs ====================

        /**
         * Request DTO for submitting a deduction.
         */
        public record DeductionRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,

                        @NotNull(message = "Ø±Ù…Ø² Ø§Ù„Ù†ÙˆØ¹ Ù…Ø·Ù„ÙˆØ¨") Long typeCode,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ù…Ø·Ù„ÙˆØ¨") LocalDate transactionDate,

                        @NotNull(message = "Ø§Ù„Ù…Ø¨Ù„Øº Ù…Ø·Ù„ÙˆØ¨") @Positive(message = "ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø§Ù„Ù…Ø¨Ù„Øº Ø£ÙƒØ¨Ø± Ù…Ù† ØµÙØ±") BigDecimal amount,

                        String notes) {
        }

        /**
         * Response DTO for deduction data.
         */
        public record DeductionResponse(
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
         * Request DTO for approving a deduction.
         */
        public record ApprovalRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo) {
        }

        /**
         * Request DTO for rejecting a deduction.
         */
        public record RejectionRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨") String rejectionReason) {
        }
}

