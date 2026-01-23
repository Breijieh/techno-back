package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.ManualAttendanceRequest;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.service.ManualAttendanceRequestService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * REST Controller for managing manual attendance requests.
 *
 * Provides endpoints for:
 * - Manual attendance request submission
 * - Approval/rejection workflow
 * - Request listing and details
 *
 * Approval Flow:
 * - Uses ApprovalWorkflowService with request type "MANUAL_ATTENDANCE"
 * - Configured in requests_approval_set table
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Slf4j
@RestController
@RequestMapping("/manual-attendance")
@RequiredArgsConstructor
public class ManualAttendanceRequestController {

    private final ManualAttendanceRequestService requestService;
    private final EmployeeRepository employeeRepository;

    /**
     * Submit a new manual attendance request.
     * Requires Employee role.
     *
     * @param request Submit request DTO
     * @return Created request
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ManualAttendanceRequestDetailsResponse>> submitRequest(
            @Valid @RequestBody SubmitManualAttendanceRequest request) {
        log.info("POST /manual-attendance/submit - Employee: {}, Date: {}, Entry: {}, Exit: {}",
                request.employeeNo, request.attendanceDate, request.entryTime, request.exitTime);

        Long requestedBy = getCurrentEmployeeNo();

        ManualAttendanceRequest manualRequest = requestService.submitRequest(
                request.employeeNo,
                request.attendanceDate,
                LocalTime.parse(request.entryTime),
                LocalTime.parse(request.exitTime),
                request.reason,
                requestedBy
        );

        return ResponseEntity.ok(ApiResponse.success(
                "تم تقديم طلب الحضور اليدوي بنجاح",
                toDetailsResponse(manualRequest)
        ));
    }

    /**
     * Get all manual attendance requests with optional filters.
     * Used for listing all requests in the manual attendance page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo Employee number - optional
     * @param startDate Start date (optional) - filters by attendanceDate
     * @param endDate End date (optional) - filters by attendanceDate
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: requestDate)
     * @param sortDirection Sort direction (default: desc)
     * @return Page of requests with employee names
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'PROJECT_SECRETARY', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<ManualAttendanceRequestDetailsResponse>>> getAllRequests(
            @RequestParam(required = false) String transStatus,
            @RequestParam(required = false) Long employeeNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("GET /manual-attendance/list - status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        // Apply role-based filtering for Employees
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            // Employees can only see their own requests
            Long currentEmployeeNo = getCurrentEmployeeNo();
            if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                employeeNo = currentEmployeeNo;
            }
        }

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ManualAttendanceRequest> requestPage = requestService.getAllRequests(
                transStatus, employeeNo, startDate, endDate, pageable
        );

        // Convert to response DTOs
        Page<ManualAttendanceRequestDetailsResponse> response = requestPage.map(this::toDetailsResponse);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلبات الحضور اليدوي بنجاح",
                response
        ));
    }

    /**
     * Get request details by ID.
     *
     * @param requestId Request ID
     * @return Request details
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'PROJECT_SECRETARY', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ManualAttendanceRequestDetailsResponse>> getRequestById(
            @PathVariable Long requestId) {
        log.info("GET /manual-attendance/{}", requestId);

        ManualAttendanceRequest request = requestService.getRequestById(requestId);

        // Apply role-based access control
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            Long currentEmployeeNo = getCurrentEmployeeNo();
            if (!request.getEmployeeNo().equals(currentEmployeeNo)) {
                throw new ResourceNotFoundException("الطلب غير موجود");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع الطلب بنجاح",
                toDetailsResponse(request)
        ));
    }

    /**
     * Approve request at current approval level.
     * Requires Admin, HR, or PM role.
     *
     * @param requestId Request ID
     * @param request Approval request with approver number
     * @return Updated request
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ManualAttendanceRequestDetailsResponse>> approveRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApproveManualAttendanceRequest request) {
        log.info("POST /manual-attendance/{}/approve - Approver: {}", requestId, request.approverNo);

        ManualAttendanceRequest manualRequest = requestService.approveRequest(requestId, request.approverNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم اعتماد الطلب بنجاح",
                toDetailsResponse(manualRequest)
        ));
    }

    /**
     * Reject request.
     * Requires Admin, HR, or PM role.
     *
     * @param requestId Request ID
     * @param request Rejection request with approver number and reason
     * @return Updated request
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ManualAttendanceRequestDetailsResponse>> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectManualAttendanceRequest request) {
        log.info("POST /manual-attendance/{}/reject - Approver: {}, Reason: {}",
                requestId, request.approverNo, request.rejectionReason);

        ManualAttendanceRequest manualRequest = requestService.rejectRequest(
                requestId, request.approverNo, request.rejectionReason);

        return ResponseEntity.ok(ApiResponse.success(
                "تم رفض الطلب بنجاح",
                toDetailsResponse(manualRequest)
        ));
    }

    // ==================== Helper Methods ====================

    /**
     * Get current authenticated user's employee number.
     */
    private Long getCurrentEmployeeNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authentication found in security context");
            throw new ResourceNotFoundException("المستخدم غير مصادق عليه");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse employee number from authentication: {}", authentication.getName());
            throw new ResourceNotFoundException("رقم الموظف غير صالح في سياق المصادقة");
        }
    }

    /**
     * Convert ManualAttendanceRequest entity to response DTO.
     */
    private ManualAttendanceRequestDetailsResponse toDetailsResponse(ManualAttendanceRequest request) {
        // Get employee name
        String employeeName = employeeRepository.findById(request.getEmployeeNo())
                .map(Employee::getEmployeeName)
                .orElse(null);

        // Get next approver name
        String nextApproverName = null;
        if (request.getNextApproval() != null) {
            nextApproverName = employeeRepository.findById(request.getNextApproval())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        // Get approved by name
        String approvedByName = null;
        if (request.getApprovedBy() != null) {
            approvedByName = employeeRepository.findById(request.getApprovedBy())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        // Get status description
        String statusDescription = switch (request.getTransStatus()) {
            case "N" -> "Pending Approval";
            case "A" -> "Approved";
            case "R" -> "Rejected";
            default -> "Unknown";
        };

        return new ManualAttendanceRequestDetailsResponse(
                request.getRequestId(),
                request.getEmployeeNo(),
                employeeName,
                request.getAttendanceDate(),
                request.getEntryTime().toString(),
                request.getExitTime().toString(),
                request.getReason(),
                request.getTransStatus(),
                statusDescription,
                request.getRequestDate(),
                request.getRequestedBy(),
                request.getNextApproval(),
                nextApproverName,
                request.getNextAppLevel(),
                request.getApprovedBy(),
                approvedByName,
                request.getApprovedDate(),
                request.getRejectionReason()
        );
    }

    // ==================== Request/Response DTOs ====================

    public record SubmitManualAttendanceRequest(
            @NotNull(message = "رقم الموظف مطلوب")
            Long employeeNo,

            @NotNull(message = "تاريخ الحضور مطلوب")
            LocalDate attendanceDate,

            @NotBlank(message = "وقت الدخول مطلوب")
            String entryTime, // HH:mm format

            @NotBlank(message = "وقت الخروج مطلوب")
            String exitTime, // HH:mm format

            @NotBlank(message = "السبب مطلوب")
            @Size(max = 500, message = "السبب لا يمكن أن يتجاوز 500 حرف")
            String reason
    ) {}

    public record ApproveManualAttendanceRequest(
            @NotNull(message = "رقم الموافق مطلوب")
            Long approverNo
    ) {}

    public record RejectManualAttendanceRequest(
            @NotNull(message = "رقم الموافق مطلوب")
            Long approverNo,

            @NotBlank(message = "سبب الرفض مطلوب")
            @Size(max = 500, message = "سبب الرفض لا يمكن أن يتجاوز 500 حرف")
            String rejectionReason
    ) {}

    public record ManualAttendanceRequestDetailsResponse(
            Long requestId,
            Long employeeNo,
            String employeeName,
            LocalDate attendanceDate,
            String entryTime,
            String exitTime,
            String reason,
            String transStatus,
            String statusDescription,
            LocalDate requestDate,
            Long requestedBy,
            Long nextApproval,
            String nextApproverName,
            Integer nextAppLevel,
            Long approvedBy,
            String approvedByName,
            LocalDateTime approvedDate,
            String rejectionReason
    ) {}
}

