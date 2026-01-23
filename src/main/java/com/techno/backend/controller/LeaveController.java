package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmployeeLeave;
import com.techno.backend.repository.EmployeeLeaveRepository;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.service.LeaveAccrualService;
import com.techno.backend.service.LeaveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import java.util.stream.Collectors;

/**
 * REST API Controller for Leave Management.
 *
 * Provides endpoints for:
 * - Leave request submission
 * - Multi-level approval workflow
 * - Leave cancellation
 * - Leave history and balance tracking
 * - Team leave management
 *
 * Approval Flow:
 * - Level 1: Direct Manager
 * - Level 2: Project Manager
 * - Level 3: HR Manager (final)
 *
 * Access Control:
 * - Submit leave: EMPLOYEE, MANAGER, ADMIN
 * - Approve/Reject: MANAGER, HR, ADMIN (must be in approval chain)
 * - View own data: All authenticated users
 * - View team/all data: MANAGER, HR, ADMIN
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Leave Management
 */
@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
@Slf4j
public class LeaveController {

        private final LeaveService leaveService;
        private final LeaveAccrualService leaveAccrualService;
        private final com.techno.backend.service.ApprovalWorkflowService approvalWorkflowService;
        private final EmployeeLeaveRepository leaveRepository;
        private final EmployeeRepository employeeRepository;

        /**
         * Submit new leave request.
         *
         * POST /api/leaves/submit
         *
         * Validates balance, checks overlaps, initializes approval workflow.
         *
         * @param request Submit leave request DTO
         * @return Created leave request with approval info
         */
        @PostMapping("/submit")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<EmployeeLeave> submitLeaveRequest(
                        @Valid @RequestBody SubmitLeaveRequest request) {
                log.info("POST /api/leaves/submit - Employee: {}, From: {}, To: {}",
                                request.employeeNo, request.leaveFromDate, request.leaveToDate);

                EmployeeLeave leave = leaveService.submitLeaveRequest(
                                request.employeeNo,
                                request.leaveFromDate,
                                request.leaveToDate,
                                request.leaveReason);

                log.info("Leave request created: ID={}, Days={}, Next Approver={}",
                                leave.getLeaveId(), leave.getLeaveDays(), leave.getNextApproval());

                return ResponseEntity.ok(leave);
        }

        /**
         * Approve leave request at current approval level.
         *
         * POST /api/leaves/{leaveId}/approve
         *
         * Validates approver authorization, moves to next level or finalizes.
         * Balance deducted only on final approval.
         *
         * @param leaveId Leave request ID
         * @param request Approve request with approver number
         * @return Updated leave request
         */
        @PostMapping("/{leaveId}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<EmployeeLeave> approveLeave(
                        @PathVariable Long leaveId,
                        @Valid @RequestBody ApproveLeaveRequest request) {
                log.info("POST /api/leaves/{}/approve - Approver: {}", leaveId, request.approverNo);

                EmployeeLeave leave = leaveService.approveLeave(leaveId, request.approverNo);

                log.info("Leave approved: ID={}, Status={}, NextLevel={}",
                                leaveId, leave.getTransStatus(), leave.getNextAppLevel());

                return ResponseEntity.ok(leave);
        }

        /**
         * Reject leave request.
         *
         * POST /api/leaves/{leaveId}/reject
         *
         * @param leaveId Leave request ID
         * @param request Reject request with reason
         * @return Updated leave request
         */
        @PostMapping("/{leaveId}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<EmployeeLeave> rejectLeave(
                        @PathVariable Long leaveId,
                        @Valid @RequestBody RejectLeaveRequest request) {
                log.info("POST /api/leaves/{}/reject - Approver: {}, Reason: {}",
                                leaveId, request.approverNo, request.rejectionReason);

                EmployeeLeave leave = leaveService.rejectLeave(
                                leaveId,
                                request.approverNo,
                                request.rejectionReason);

                log.info("Leave rejected: ID={}, Status={}", leaveId, leave.getTransStatus());

                return ResponseEntity.ok(leave);
        }

        /**
         * Cancel own leave request.
         *
         * POST /api/leaves/{leaveId}/cancel
         *
         * Can cancel pending or approved leaves (before start date).
         * Balance refunded if leave was approved.
         *
         * @param leaveId Leave request ID
         * @param request Cancel request with reason
         * @return Updated leave request
         */
        @PostMapping("/{leaveId}/cancel")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<EmployeeLeave> cancelLeaveRequest(
                        @PathVariable Long leaveId,
                        @Valid @RequestBody CancelLeaveRequest request) {
                log.info("POST /api/leaves/{}/cancel - Employee: {}, Reason: {}",
                                leaveId, request.employeeNo, request.cancellationReason);

                EmployeeLeave leave = leaveService.cancelLeaveRequest(
                                leaveId,
                                request.employeeNo,
                                request.cancellationReason);

                log.info("Leave cancelled: ID={}, Status={}", leaveId, leave.getTransStatus());

                return ResponseEntity.ok(leave);
        }

        /**
         * Get authenticated user's leave requests.
         *
         * GET /api/leaves/my-requests
         *
         * Returns all leaves (pending, approved, rejected) for current user.
         *
         * @return List of employee's leaves
         */
        @GetMapping("/my-requests")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<List<EmployeeLeave>> getMyLeaveRequests() {
                Long employeeNo = getCurrentEmployeeNo();
                log.info("GET /api/leaves/my-requests - Employee: {}", employeeNo);

                List<EmployeeLeave> leaves = leaveService.getEmployeeLeaveHistory(employeeNo);

                log.info("Retrieved {} leave requests for employee {}", leaves.size(), employeeNo);

                return ResponseEntity.ok(leaves);
        }

        /**
         * Get pending leave approvals for authenticated user.
         *
         * GET /api/leaves/pending-approvals
         *
         * Returns leaves where nextApproval = current user's employeeNo.
         *
         * @return List of pending leaves
         */
        @GetMapping("/pending-approvals")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<List<EmployeeLeave>> getPendingApprovals() {
                Long approverNo = getCurrentEmployeeNo();
                log.info("GET /api/leaves/pending-approvals - Approver: {}", approverNo);

                List<EmployeeLeave> pendingLeaves = leaveService.getPendingLeavesForApprover(approverNo);

                log.info("Retrieved {} pending approvals for approver {}", pendingLeaves.size(), approverNo);

                return ResponseEntity.ok(pendingLeaves);
        }

        /**
         * Get leave history for specific employee.
         *
         * GET /api/leaves/employee/{employeeNo}
         *
         * @param employeeNo Employee number
         * @return List of employee's approved leaves
         */
        @GetMapping("/employee/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER') or #employeeNo.toString() == authentication.name")
        public ResponseEntity<List<EmployeeLeave>> getEmployeeLeaveHistory(
                        @PathVariable Long employeeNo) {
                log.info("GET /api/leaves/employee/{}", employeeNo);

                List<EmployeeLeave> leaves = leaveService.getEmployeeLeaveHistory(employeeNo);

                log.info("Retrieved {} leave requests for employee {}", leaves.size(), employeeNo);

                return ResponseEntity.ok(leaves);
        }

        /**
         * Get all leave requests with optional filters.
         * Used for listing all leave requests in the leave requests page.
         *
         * GET
         * /leaves/list?status=N&employeeNo=123&startDate=2025-01-01&endDate=2025-01-31&page=0&size=20
         *
         * @param transStatus   Transaction status (N/A/R) - optional
         * @param employeeNo    Employee number - optional
         * @param startDate     Start date (optional) - filters by leaveFromDate
         * @param endDate       End date (optional) - filters by leaveToDate
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: requestDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of leave requests with employee names
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<Page<LeaveDetailsResponse>>> getAllLeaves(
                        @RequestParam(required = false) String transStatus,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "requestDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /leaves/list - status={}, employee={}, startDate={}, endDate={}",
                                transStatus, employeeNo, startDate, endDate);

                // Apply role-based filtering for Project Managers
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_MANAGER"))) {
                        // For Project Managers, we might want to filter by their project
                        // For now, we'll let them see all leaves (can be enhanced later)
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<EmployeeLeave> leavePage = leaveService.getAllLeaves(
                                transStatus, employeeNo, startDate, endDate, pageable);

                // Convert to LeaveDetailsResponse with employee names
                Page<LeaveDetailsResponse> response = leavePage.map(this::toLeaveDetailsResponse);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        /**
         * Get leave request details.
         *
         * GET /leaves/{leaveId}
         *
         * @param leaveId Leave request ID
         * @return Leave details with employee info
         */
        @GetMapping("/{leaveId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<EmployeeLeave> getLeaveDetails(@PathVariable Long leaveId) {
                log.info("GET /leaves/{}", leaveId);

                EmployeeLeave leave = leaveRepository.findById(leaveId)
                                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leaveId));

                log.info("Retrieved leave details: ID={}, Employee={}, Status={}",
                                leaveId, leave.getEmployeeNo(), leave.getTransStatus());

                return ResponseEntity.ok(leave);
        }

        /**
         * Get approval timeline for leave request.
         *
         * GET /leaves/{leaveId}/timeline
         *
         * @param leaveId Leave request ID
         * @return List of approval steps
         */
        @GetMapping("/{leaveId}/timeline")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<List<com.techno.backend.service.ApprovalWorkflowService.ApprovalStep>> getLeaveTimeline(
                        @PathVariable Long leaveId) {
                log.info("GET /leaves/{}/timeline", leaveId);

                EmployeeLeave leave = leaveRepository.findById(leaveId)
                                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leaveId));

                // Get employee details for department/project code
                Employee employee = employeeRepository.findById(leave.getEmployeeNo())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leave.getEmployeeNo()));

                List<com.techno.backend.service.ApprovalWorkflowService.ApprovalStep> timeline = approvalWorkflowService
                                .getApprovalTimeline(
                                                "VAC",
                                                leave.getEmployeeNo(),
                                                employee.getPrimaryDeptCode(),
                                                employee.getPrimaryProjectCode(),
                                                leave.getNextAppLevel(),
                                                leave.getTransStatus());

                return ResponseEntity.ok(timeline);
        }

        /**
         * Get leave balance for employee.
         *
         * GET /api/leaves/balance/{employeeNo}
         *
         * Returns current balance and available days.
         *
         * @param employeeNo Employee number
         * @return Leave balance details
         */
        @GetMapping("/balance/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER') or #employeeNo.toString() == authentication.name")
        public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(
                        @PathVariable Long employeeNo) {
                log.info("GET /api/leaves/balance/{}", employeeNo);

                // Get employee for name
                Employee employee = employeeRepository.findById(employeeNo)
                                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

                BigDecimal balance = leaveService.getLeaveBalance(employeeNo);

                // Get all leaves for this employee
                List<EmployeeLeave> allLeaves = leaveService.getEmployeeLeaveHistory(employeeNo);

                // Calculate pending leaves (status = 'N')
                BigDecimal pendingLeaves = allLeaves.stream()
                                .filter(l -> "N".equals(l.getTransStatus()))
                                .map(EmployeeLeave::getLeaveDays)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate approved future leaves (status = 'A' and leave hasn't started yet)
                BigDecimal approvedFutureLeaves = allLeaves.stream()
                                .filter(l -> "A".equals(l.getTransStatus()))
                                .filter(l -> l.getLeaveFromDate().isAfter(LocalDate.now()))
                                .map(EmployeeLeave::getLeaveDays)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Available balance = current balance (pending not yet deducted from balance in
                // DB)
                BigDecimal availableBalance = balance.subtract(pendingLeaves);

                LeaveBalanceResponse response = new LeaveBalanceResponse(
                                employeeNo,
                                employee.getEmployeeName(),
                                balance,
                                pendingLeaves,
                                approvedFutureLeaves,
                                availableBalance);

                log.info("Leave balance for employee {}: Current={}, Pending={}, Available={}",
                                employeeNo, balance, pendingLeaves, availableBalance);

                return ResponseEntity.ok(response);
        }

        /**
         * Get all employees' leave balances with pagination and optional filters.
         * Used for listing all employees' leave balances in the leave balance page.
         *
         * @param employeeNo       Employee number - optional
         * @param departmentCode   Department code - optional
         * @param employmentStatus Employment status - optional (defaults to ACTIVE)
         * @param page             Page number (default: 0)
         * @param size             Page size (default: 20)
         * @param sortBy           Sort field (default: employeeNo)
         * @param sortDirection    Sort direction (default: asc)
         * @return Page of leave balances with employee names
         */
        @GetMapping("/balance/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<LeaveBalanceDetailsResponse>>> getAllLeaveBalances(
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) Long departmentCode,
                        @RequestParam(required = false) String employmentStatus,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "employeeNo") String sortBy,
                        @RequestParam(defaultValue = "asc") String sortDirection) {

                log.info("GET /leaves/balance/list - employee={}, department={}, status={}",
                                employeeNo, departmentCode, employmentStatus);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own leave balance
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Employee> employeePage = leaveService.getAllLeaveBalances(
                                employeeNo, departmentCode, employmentStatus, pageable);

                // Convert to LeaveBalanceDetailsResponse
                Page<LeaveBalanceDetailsResponse> response = employeePage.map(this::toLeaveBalanceDetailsResponse);

                return ResponseEntity.ok(ApiResponse.success(
                                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø£Ø±ØµØ¯Ø© Ø§Ù„Ø¥Ø¬Ø§Ø²Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                                response));
        }

        /**
         * Get team leave requests for manager.
         *
         * GET /api/leaves/team/{managerNo}
         *
         * Returns all leaves for employees reporting to this manager.
         * Note: Simplified version returns all pending approvals for now.
         *
         * @param managerNo Manager employee number
         * @return Team leave requests
         */
        @GetMapping("/team/{managerNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<TeamLeaveResponse> getTeamLeaveRequests(
                        @PathVariable Long managerNo) {
                log.info("GET /api/leaves/team/{}", managerNo);

                // Get manager details
                Employee manager = employeeRepository.findById(managerNo)
                                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…Ø¯ÙŠØ± ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + managerNo));

                // Get all pending leaves for this manager (simplified version)
                List<EmployeeLeave> teamLeaves = leaveService.getPendingLeavesForApprover(managerNo);

                // Convert to response DTOs
                List<LeaveDetailsResponse> leaveDetails = teamLeaves.stream()
                                .map(this::toLeaveDetailsResponse)
                                .collect(Collectors.toList());

                TeamLeaveResponse response = new TeamLeaveResponse(
                                managerNo,
                                manager.getEmployeeName(),
                                leaveDetails.size(), // Simplified: count of pending approvals
                                leaveDetails);

                log.info("Retrieved {} team leave requests for manager {}", leaveDetails.size(), managerNo);

                return ResponseEntity.ok(response);
        }

        /**
         * One-time initialization of leave balances for all employees.
         * Sets balance to 30 days for everyone.
         */
        @PostMapping("/init-balances")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Void>> initializeBalances() {
                log.info("POST /api/leaves/init-balances - Initializing all employee balances to 30 days");
                leaveAccrualService.initializeExistingEmployees(BigDecimal.valueOf(30.0));
                return ResponseEntity
                                .ok(ApiResponse.success("ØªÙ… ØªÙ‡ÙŠØ¦Ø© Ø¬Ù…ÙŠØ¹ Ø£Ø±ØµØ¯Ø© Ø¥Ø¬Ø§Ø²Ø§Øª Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø¥Ù„Ù‰ 30 ÙŠÙˆÙ…Ø§Ù‹", null));
        }

        // ==================== Helper Methods ====================

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
         * Convert EmployeeLeave entity to LeaveDetailsResponse DTO.
         *
         * @param leave Employee leave entity
         * @return Leave details response DTO with enriched employee names
         */
        private LeaveDetailsResponse toLeaveDetailsResponse(EmployeeLeave leave) {
                // Get employee name
                String employeeName = employeeRepository.findById(leave.getEmployeeNo())
                                .map(Employee::getEmployeeName)
                                .orElse(null);

                // Get next approver name
                String nextApproverName = null;
                if (leave.getNextApproval() != null) {
                        nextApproverName = employeeRepository.findById(leave.getNextApproval())
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // Get approved by name
                String approvedByName = null;
                if (leave.getApprovedBy() != null) {
                        approvedByName = employeeRepository.findById(leave.getApprovedBy())
                                        .map(Employee::getEmployeeName)
                                        .orElse(null);
                }

                // Get status description
                String statusDescription = switch (leave.getTransStatus()) {
                        case "N" -> "Pending Approval";
                        case "A" -> "Approved";
                        case "R" -> "Rejected";
                        case "C" -> "Cancelled";
                        default -> "Unknown";
                };

                return new LeaveDetailsResponse(
                                leave.getLeaveId(),
                                leave.getEmployeeNo(),
                                employeeName,
                                leave.getLeaveFromDate(),
                                leave.getLeaveToDate(),
                                leave.getLeaveDays(),
                                leave.getLeaveReason(),
                                leave.getRequestDate(),
                                leave.getTransStatus(),
                                statusDescription,
                                leave.getNextApproval(),
                                nextApproverName,
                                leave.getNextAppLevel(),
                                leave.getNextAppLevelName(),
                                leave.getApprovedBy(),
                                approvedByName,
                                leave.getApprovedDate(),
                                leave.getRejectionReason());
        }

        /**
         * Convert Employee entity to LeaveBalanceDetailsResponse DTO.
         *
         * @param employee Employee entity
         * @return Leave balance details response DTO
         */
        private LeaveBalanceDetailsResponse toLeaveBalanceDetailsResponse(Employee employee) {
                // Get leave balance (annual leave balance from leaveBalanceDays)
                BigDecimal annualLeaveBalance = employee.getLeaveBalanceDays() != null
                                ? employee.getLeaveBalanceDays()
                                : BigDecimal.ZERO;

                // Sick leave and emergency leave are not tracked separately in the backend
                // Set them to 0 for now
                BigDecimal sickLeaveBalance = BigDecimal.ZERO;
                BigDecimal emergencyLeaveBalance = BigDecimal.ZERO;

                // Get last updated date (use modifiedDate if available, otherwise createdDate)
                LocalDateTime lastUpdated = employee.getModifiedDate() != null
                                ? employee.getModifiedDate()
                                : employee.getCreatedDate();

                return new LeaveBalanceDetailsResponse(
                                employee.getEmployeeNo(),
                                employee.getEmployeeName(),
                                annualLeaveBalance,
                                sickLeaveBalance,
                                emergencyLeaveBalance,
                                lastUpdated != null ? lastUpdated.toLocalDate() : null);
        }

        // ==================== Request/Response DTOs ====================

        public record SubmitLeaveRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ø¨Ø¯Ø§ÙŠØ© Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù…Ø·Ù„ÙˆØ¨") LocalDate leaveFromDate,

                        @NotNull(message = "ØªØ§Ø±ÙŠØ® Ù†Ù‡Ø§ÙŠØ© Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù…Ø·Ù„ÙˆØ¨") LocalDate leaveToDate,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù…Ø·Ù„ÙˆØ¨") @Size(max = 500, message = "Ø§Ù„Ø³Ø¨Ø¨ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² 500 Ø­Ø±Ù") String leaveReason) {
        }

        public record ApproveLeaveRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo) {
        }

        public record RejectLeaveRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ù…Ø·Ù„ÙˆØ¨") Long approverNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨") @Size(max = 500, message = "Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² 500 Ø­Ø±Ù") String rejectionReason) {
        }

        public record CancelLeaveRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,

                        @NotBlank(message = "Ø³Ø¨Ø¨ Ø§Ù„Ø¥Ù„ØºØ§Ø¡ Ù…Ø·Ù„ÙˆØ¨") @Size(max = 500, message = "Ø³Ø¨Ø¨ Ø§Ù„Ø¥Ù„ØºØ§Ø¡ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² 500 Ø­Ø±Ù") String cancellationReason) {
        }

        public record LeaveDetailsResponse(
                        Long leaveId,
                        Long employeeNo,
                        String employeeName,
                        LocalDate leaveFromDate,
                        LocalDate leaveToDate,
                        BigDecimal leaveDays,
                        String leaveReason,
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
                        String rejectionReason) {
        }

        public record LeaveBalanceResponse(
                        Long employeeNo,
                        String employeeName,
                        BigDecimal currentBalance,
                        BigDecimal pendingLeaves,
                        BigDecimal approvedLeaves,
                        BigDecimal availableBalance) {
        }

        public record LeaveBalanceDetailsResponse(
                        Long employeeNo,
                        String employeeName,
                        BigDecimal annualLeaveBalance,
                        BigDecimal sickLeaveBalance,
                        BigDecimal emergencyLeaveBalance,
                        LocalDate lastUpdated) {
        }

        public record TeamLeaveResponse(
                        Long managerNo,
                        String managerName,
                        int totalTeamMembers,
                        List<LeaveDetailsResponse> teamLeaveRequests) {
        }
}

