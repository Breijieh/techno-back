package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.SalaryDetail;
import com.techno.backend.entity.SalaryHeader;
import com.techno.backend.repository.SalaryDetailRepository;
import com.techno.backend.repository.SalaryHeaderRepository;
import com.techno.backend.service.PayrollCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.techno.backend.exception.UnauthorizedException;
import com.techno.backend.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * REST API Controller for Payroll Management.
 *
 * Provides endpoints for:
 * - Payroll calculation (individual & batch)
 * - Salary details retrieval
 * - Payroll history and reporting
 * - Payroll recalculation
 *
 * Access Control:
 * - Calculate payroll: HR_MANAGER, FINANCE_MANAGER, ADMIN
 * - View salary details: HR_MANAGER, FINANCE_MANAGER, ADMIN, or own salary
 * - Approve payroll: FINANCE_MANAGER, ADMIN
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@Slf4j
public class PayrollController {

        private final PayrollCalculationService payrollService;
        private final SalaryHeaderRepository salaryHeaderRepository;
        private final SalaryDetailRepository salaryDetailRepository;

        /**
         * Calculate payroll for a single employee.
         *
         * POST /api/payroll/calculate
         *
         * Request body:
         * {
         * "employeeNo": 1,
         * "salaryMonth": "2025-11"
         * }
         *
         * @param request Calculate payroll request
         * @return Calculated salary header with details
         */
        @PostMapping("/calculate")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<SalaryHeader> calculatePayroll(@Valid @RequestBody CalculatePayrollRequest request) {
                log.info("POST /api/payroll/calculate - Employee: {}, Month: {}",
                                request.employeeNo, request.salaryMonth);

                SalaryHeader salary = payrollService.calculatePayrollForEmployee(
                                request.employeeNo, request.salaryMonth);

                // Check for unapproved previous payrolls
                if (salaryHeaderRepository.existsUnapprovedPreviousPayroll(request.employeeNo, request.salaryMonth)) {
                        salary.setBlockingReason("يجب اعتماد الرواتب السابقة أولاً");
                }

                return ResponseEntity.ok(salary);
        }

        /**
         * Calculate payroll for all eligible employees.
         *
         * POST /api/payroll/calculate-all
         *
         * Request body:
         * {
         * "salaryMonth": "2025-11"
         * }
         *
         * @param request Batch calculation request
         * @return List of calculated salary headers
         */
        @PostMapping("/calculate-all")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<BatchCalculationResponse> calculatePayrollForAll(
                        @Valid @RequestBody BatchCalculateRequest request) {
                log.info("POST /api/payroll/calculate-all - Month: {}", request.salaryMonth);

                List<SalaryHeader> salaries = payrollService.calculatePayrollForAllEmployees(request.salaryMonth);

                // Populate transient field for UI warnings
                if (salaries != null) {
                        salaries.forEach(s -> {
                                if (salaryHeaderRepository.existsUnapprovedPreviousPayroll(s.getEmployeeNo(),
                                                request.salaryMonth)) {
                                        s.setBlockingReason("يجب اعتماد الرواتب السابقة أولاً");
                                }
                        });
                }

                BatchCalculationResponse response = new BatchCalculationResponse(
                                salaries.size(),
                                request.salaryMonth,
                                salaries);

                log.info("Batch payroll calculated for {} employees", salaries.size());
                return ResponseEntity.ok(response);
        }

        /**
         * Get salary details for an employee in a specific month.
         *
         * GET /api/payroll/employee/{employeeNo}/month/{salaryMonth}
         *
         * @param employeeNo  Employee number
         * @param salaryMonth Salary month (YYYY-MM)
         * @return Salary header with details
         */
        @GetMapping("/employee/{employeeNo}/month/{salaryMonth}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<SalaryDetailsResponse> getSalaryDetails(
                        @PathVariable Long employeeNo,
                        @PathVariable String salaryMonth) {
                log.info("GET /api/payroll/employee/{}/month/{}", employeeNo, salaryMonth);

                // For EMPLOYEE role, ensure they can only access their own salary
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        Object principal = auth.getPrincipal();
                        Long currentEmployeeNo = null;
                        if (principal instanceof Long) {
                                currentEmployeeNo = (Long) principal;
                        } else if (principal instanceof String) {
                                try {
                                        currentEmployeeNo = Long.parseLong((String) principal);
                                } catch (NumberFormatException e) {
                                        log.error("Invalid employee number in principal: {}", principal);
                                }
                        }
                        if (currentEmployeeNo == null || !currentEmployeeNo.equals(employeeNo)) {
                                throw new UnauthorizedException("يمكن للموظفين الوصول فقط إلى تفاصيل رواتبهم");
                        }
                }

                SalaryHeader salary = salaryHeaderRepository
                                .findLatestByEmployeeAndMonth(employeeNo, salaryMonth)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "لم يتم العثور على راتب للموظف " + employeeNo + " في الشهر "
                                                                + salaryMonth));

                // Load details
                List<SalaryDetail> details = salaryDetailRepository
                                .findBySalaryIdOrderByLineNoAsc(salary.getSalaryId());

                SalaryDetailsResponse response = new SalaryDetailsResponse(salary, details);
                return ResponseEntity.ok(response);
        }

        /**
         * Get payroll history for an employee.
         *
         * GET /api/payroll/employee/{employeeNo}/history
         *
         * @param employeeNo Employee number
         * @return Salary history (latest versions only)
         */
        @GetMapping("/employee/{employeeNo}/history")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER') or #employeeNo.toString() == authentication.name")
        public ResponseEntity<List<SalaryHeader>> getPayrollHistory(@PathVariable Long employeeNo) {
                log.info("GET /api/payroll/employee/{}/history", employeeNo);

                List<SalaryHeader> history = salaryHeaderRepository.findByEmployeeNoOrderByMonthDesc(employeeNo);

                return ResponseEntity.ok(history);
        }

        /**
         * Get all salaries for a specific month.
         *
         * GET /api/payroll/month/{salaryMonth}
         *
         * @param salaryMonth Salary month (YYYY-MM)
         * @return List of salaries for the month (latest versions only)
         */
        @GetMapping("/month/{salaryMonth}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<SalaryHeader>>> getSalariesForMonth(@PathVariable String salaryMonth) {
                log.info("GET /api/payroll/month/{}", salaryMonth);

                // For EMPLOYEE role, ensure they can only access their own salary
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                List<SalaryHeader> salaries;

                if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employee role - return only their own salary for that month
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        log.info("Employee role detected, filtering to employee number: {} for month: {}",
                                        currentEmployeeNo, salaryMonth);

                        Optional<SalaryHeader> salaryOpt = salaryHeaderRepository
                                        .findLatestByEmployeeAndMonth(currentEmployeeNo, salaryMonth);
                        salaries = salaryOpt.map(List::of).orElse(List.of());
                } else {
                        // Other roles - return all salaries for the month as normal
                        salaries = salaryHeaderRepository.findAllLatestBySalaryMonth(salaryMonth);
                }

                // Populate transient field for UI warnings
                if (salaries != null) {
                        salaries.forEach(s -> {
                                boolean hasUnapproved = salaryHeaderRepository
                                                .existsUnapprovedPreviousPayroll(s.getEmployeeNo(), salaryMonth);
                                if (hasUnapproved) {
                                        log.info("Blocking approval for employee {} in month {}: Previous payrolls unapproved.",
                                                        s.getEmployeeNo(), salaryMonth);
                                        s.setBlockingReason("يجب اعتماد الرواتب السابقة أولاً");
                                } else {
                                        // log.debug("Employee {} month {} is clear.", s.getEmployeeNo(), salaryMonth);
                                }
                        });
                }

                return ResponseEntity.ok(ApiResponse.success("تم استرجاع الرواتب بنجاح", salaries));
        }

        /**
         * Get pending payroll approvals for a specific month (status = N).
         *
         * GET /api/payroll/pending/{salaryMonth}
         *
         * @param salaryMonth Salary month
         * @return List of pending salaries for the month
         */
        @GetMapping("/pending/{salaryMonth}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<List<SalaryHeader>> getPendingApprovals(@PathVariable String salaryMonth) {
                log.info("GET /api/payroll/pending/{}", salaryMonth);

                List<SalaryHeader> pending = salaryHeaderRepository
                                .findBySalaryMonthAndStatus(salaryMonth, "N");

                // Populate transient field for UI warnings
                if (pending != null) {
                        pending.forEach(s -> {
                                if (salaryHeaderRepository.existsUnapprovedPreviousPayroll(s.getEmployeeNo(),
                                                salaryMonth)) {
                                        s.setBlockingReason("يجب اعتماد الرواتب السابقة أولاً");
                                }
                        });
                }

                return ResponseEntity.ok(pending);
        }

        /**
         * Recalculate payroll (create new version).
         *
         * POST /api/payroll/recalculate
         *
         * Request body:
         * {
         * "employeeNo": 1,
         * "salaryMonth": "2025-11",
         * "reason": "Correction needed for overtime"
         * }
         *
         * @param request Recalculation request
         * @return New salary version
         */
        @PostMapping("/recalculate")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'ADMIN')")
        public ResponseEntity<SalaryHeader> recalculatePayroll(
                        @Valid @RequestBody RecalculatePayrollRequest request) {
                log.info("POST /api/payroll/recalculate - Employee: {}, Month: {}, Reason: {}",
                                request.employeeNo, request.salaryMonth, request.reason);

                SalaryHeader newVersion = payrollService.recalculatePayroll(
                                request.employeeNo, request.salaryMonth, request.reason);

                return ResponseEntity.ok(newVersion);
        }

        /**
         * Get salary breakdown by category (Allowances vs Deductions).
         *
         * GET /api/payroll/{salaryId}/breakdown
         *
         * @param salaryId Salary header ID
         * @return Breakdown by category
         */
        @GetMapping("/{salaryId}/breakdown")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<SalaryBreakdownResponse> getSalaryBreakdown(@PathVariable Long salaryId) {
                log.info("GET /api/payroll/{}/breakdown", salaryId);

                SalaryHeader salary = salaryHeaderRepository.findById(salaryId)
                                .orElseThrow(() -> new RuntimeException("الراتب غير موجود: " + salaryId));

                List<SalaryDetail> allowances = salaryDetailRepository
                                .findBySalaryIdAndCategory(salaryId, "A");

                List<SalaryDetail> deductions = salaryDetailRepository
                                .findBySalaryIdAndCategory(salaryId, "D");

                SalaryBreakdownResponse response = new SalaryBreakdownResponse(
                                salary, allowances, deductions);

                return ResponseEntity.ok(response);
        }

        /**
         * Approve a payroll salary.
         * Moves through the 3-level approval workflow.
         *
         * POST /api/payroll/{salaryId}/approve
         *
         * @param salaryId Salary header ID
         * @param request  Approval request with approver ID
         * @return Updated salary header
         */
        @PostMapping("/{salaryId}/approve")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'ADMIN')")
        public ResponseEntity<SalaryHeader> approvePayroll(
                        @PathVariable Long salaryId,
                        @Valid @RequestBody PayrollApprovalRequest request) {
                log.info("POST /api/payroll/{}/approve - Approver: {}", salaryId, request.approverNo());

                SalaryHeader salary = payrollService.approvePayroll(salaryId, request.approverNo());
                return ResponseEntity.ok(salary);
        }

        /**
         * Reject a payroll salary.
         * Can be rejected at any approval level.
         *
         * POST /api/payroll/{salaryId}/reject
         *
         * @param salaryId Salary header ID
         * @param request  Rejection request with approver ID and reason
         * @return Updated salary header
         */
        @PostMapping("/{salaryId}/reject")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'ADMIN')")
        public ResponseEntity<SalaryHeader> rejectPayroll(
                        @PathVariable Long salaryId,
                        @Valid @RequestBody PayrollRejectionRequest request) {
                log.info("POST /api/payroll/{}/reject - Approver: {}, Reason: {}",
                                salaryId, request.approverNo(), request.rejectionReason());

                SalaryHeader salary = payrollService.rejectPayroll(
                                salaryId, request.approverNo(), request.rejectionReason());
                return ResponseEntity.ok(salary);
        }

        /**
         * Get current employee number from security context.
         *
         * @return Employee number
         */
        private Long getCurrentEmployeeNo() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                        Object principal = auth.getPrincipal();
                        if (principal instanceof Long) {
                                return (Long) principal;
                        }
                        // If principal is String (username), we need to extract employeeNo from token
                        // This shouldn't happen with the updated JWT filter, but handle it for safety
                        if (principal instanceof String) {
                                try {
                                        return Long.parseLong((String) principal);
                                } catch (NumberFormatException e) {
                                        log.error("Invalid employee number in principal: {}", principal);
                                        throw new IllegalStateException(
                                                        "لم يتم العثور على رقم الموظف في سياق الأمان. الرئيسي: "
                                                                        + principal);
                                }
                        }
                }
                throw new IllegalStateException("لم يتم العثور على رقم الموظف في سياق الأمان");
        }

        // ==================== Request/Response DTOs ====================

        public record CalculatePayrollRequest(
                        @NotNull(message = "رقم الموظف مطلوب") Long employeeNo,
                        @NotBlank(message = "شهر الراتب مطلوب") String salaryMonth) {
        }

        public record BatchCalculateRequest(
                        @NotBlank(message = "شهر الراتب مطلوب") String salaryMonth) {
        }

        public record RecalculatePayrollRequest(
                        @NotNull(message = "رقم الموظف مطلوب") Long employeeNo,
                        @NotBlank(message = "شهر الراتب مطلوب") String salaryMonth,
                        @NotBlank(message = "السبب مطلوب") String reason) {
        }

        public record BatchCalculationResponse(
                        int totalCalculated,
                        String salaryMonth,
                        List<SalaryHeader> salaries) {
        }

        public record SalaryDetailsResponse(
                        SalaryHeader header,
                        List<SalaryDetail> details) {
        }

        public record SalaryBreakdownResponse(
                        SalaryHeader header,
                        List<SalaryDetail> allowances,
                        List<SalaryDetail> deductions) {
        }

        public record PayrollApprovalRequest(
                        @NotNull(message = "رقم الموافق مطلوب") Long approverNo) {
        }

        public record PayrollRejectionRequest(
                        @NotNull(message = "رقم الموافق مطلوب") Long approverNo,
                        @NotBlank(message = "سبب الرفض مطلوب") String rejectionReason) {
        }
}
