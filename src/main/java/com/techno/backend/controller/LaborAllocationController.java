package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.labor.*;
import com.techno.backend.service.LaborAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Labor Allocation Management.
 * @since Phase 10 - Projects
 */
@RestController
@RequestMapping("/labor")
@RequiredArgsConstructor
@Slf4j
public class LaborAllocationController {

    private final LaborAllocationService laborAllocationService;

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborRequestResponse>> createLaborRequest(
            @Valid @RequestBody LaborRequestDto request) {

        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("POST /labor/requests - Requester: {}", currentEmployeeNo);

        LaborRequestResponse response = laborAllocationService.createLaborRequest(request, currentEmployeeNo);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "تم إنشاء طلب العمالة بنجاح",
                response
        ));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<LaborRequestResponse>> getRequestById(@PathVariable Long id) {
        log.info("GET /labor/requests/{}", id);

        LaborRequestResponse response = laborAllocationService.getRequestById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلب العمالة بنجاح",
                response
        ));
    }

    @GetMapping("/requests/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<LaborRequestResponse>>> getRequestsByProject(
            @PathVariable Long projectId) {

        log.info("GET /labor/requests/project/{}", projectId);

        List<LaborRequestResponse> requests = laborAllocationService.getRequestsByProject(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلبات عمالة المشروع بنجاح",
                requests
        ));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<LaborRequestResponse>>> getAllLaborRequests() {
        log.info("GET /labor/requests");

        List<LaborRequestResponse> requests = laborAllocationService.getAllLaborRequests();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع جميع طلبات العمالة بنجاح",
                requests
        ));
    }

    /**
     * Update a labor request.
     * Can only update OPEN requests that haven't been approved yet.
     * 
     * PUT /labor/requests/{id}
     * 
     * @param id Request number
     * @param request Update request
     * @return Updated labor request
     */
    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborRequestResponse>> updateLaborRequest(
            @PathVariable Long id,
            @Valid @RequestBody LaborRequestDto request) {

        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("PUT /labor/requests/{} - Updated by: {}", id, currentEmployeeNo);

        LaborRequestResponse response = laborAllocationService.updateLaborRequest(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "تم تحديث طلب العمالة بنجاح",
                response
        ));
    }

    @GetMapping("/requests/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<LaborRequestResponse>>> getOpenRequests() {
        log.info("GET /labor/requests/open");

        List<LaborRequestResponse> requests = laborAllocationService.getOpenRequests();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلبات العمالة المفتوحة بنجاح",
                requests
        ));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborAssignmentResponse>> assignLabor(
            @Valid @RequestBody LaborAssignmentDto request) {

        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("POST /labor/assignments - Assigned by: {}", currentEmployeeNo);

        LaborAssignmentResponse response = laborAllocationService.assignLabor(request, currentEmployeeNo);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "تم تعيين العمالة بنجاح",
                response
        ));
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<LaborAssignmentResponse>> getAssignmentById(@PathVariable Long id) {
        log.info("GET /labor/assignments/{}", id);

        LaborAssignmentResponse response = laborAllocationService.getAssignmentById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع التعيين بنجاح",
                response
        ));
    }

    @GetMapping("/assignments/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<LaborAssignmentResponse>>> getAssignmentsByProject(
            @PathVariable Long projectId) {

        log.info("GET /labor/assignments/project/{}", projectId);

        List<LaborAssignmentResponse> assignments = laborAllocationService.getAssignmentsByProject(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تعيينات المشروع بنجاح",
                assignments
        ));
    }

    @GetMapping("/assignments/employee/{employeeNo}")
    public ResponseEntity<ApiResponse<List<LaborAssignmentResponse>>> getAssignmentsByEmployee(
            @PathVariable Long employeeNo) {

        log.info("GET /labor/assignments/employee/{}", employeeNo);

        // Allow employees to view their own assignments, or admins/HR to view any
        Long currentEmployeeNo = getCurrentEmployeeNo();
        boolean isAdminOrHR = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER"));
        
        if (!isAdminOrHR && !employeeNo.equals(currentEmployeeNo)) {
            log.warn("Employee {} attempted to access assignments for employee {}", currentEmployeeNo, employeeNo);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                    "ليس لديك صلاحية للوصول إلى تعيينات هذا الموظف"
            ));
        }

        List<LaborAssignmentResponse> assignments = laborAllocationService.getAssignmentsByEmployee(employeeNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تعيينات الموظف بنجاح",
                assignments
        ));
    }

    /**
     * Get all labor assignments.
     * 
     * GET /labor/assignments
     * 
     * @return List of all labor assignments
     */
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<LaborAssignmentResponse>>> getAllAssignments() {
        log.info("GET /labor/assignments");

        List<LaborAssignmentResponse> assignments = laborAllocationService.getAllAssignments();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع جميع تعيينات العمالة بنجاح",
                assignments
        ));
    }

    /**
     * Update a labor assignment.
     * 
     * PUT /labor/assignments/{id}
     * 
     * @param id Assignment number
     * @param request Update request
     * @return Updated labor assignment
     */
    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborAssignmentResponse>> updateLaborAssignment(
            @PathVariable Long id,
            @Valid @RequestBody LaborAssignmentDto request) {

        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("PUT /labor/assignments/{} - Updated by: {}", id, currentEmployeeNo);

        LaborAssignmentResponse response = laborAllocationService.updateLaborAssignment(id, request, currentEmployeeNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم تحديث تعيين العمالة بنجاح",
                response
        ));
    }

    /**
     * Delete a labor assignment (soft delete).
     * 
     * DELETE /labor/assignments/{id}
     * 
     * @param id Assignment number
     * @return Success response
     */
    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteLaborAssignment(@PathVariable Long id) {
        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("DELETE /labor/assignments/{} - Deleted by: {}", id, currentEmployeeNo);

        laborAllocationService.deleteLaborAssignment(id, currentEmployeeNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم حذف تعيين العمالة بنجاح",
                null
        ));
    }

    /**
     * Approve a labor request.
     * 
     * POST /labor/requests/{id}/approve
     * 
     * @param id Request number
     * @param request Approval request with notes
     * @return Updated labor request
     */
    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborRequestResponse>> approveLaborRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {

        Long approverNo = getCurrentEmployeeNo();
        String notes = request != null ? request.get("notes") : null;
        log.info("POST /labor/requests/{}/approve - Approver: {}", id, approverNo);

        LaborRequestResponse response = laborAllocationService.approveLaborRequest(id, approverNo, notes);

        return ResponseEntity.ok(ApiResponse.success(
                "تم اعتماد طلب العمالة بنجاح",
                response
        ));
    }

    /**
     * Reject a labor request.
     * 
     * POST /labor/requests/{id}/reject
     * 
     * @param id Request number
     * @param request Rejection request with reason
     * @return Updated labor request
     */
    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<LaborRequestResponse>> rejectLaborRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Long approverNo = getCurrentEmployeeNo();
        String reason = request.get("reason");
        log.info("POST /labor/requests/{}/reject - Approver: {}, Reason: {}", id, approverNo, reason);

        LaborRequestResponse response = laborAllocationService.rejectLaborRequest(id, approverNo, reason);

        return ResponseEntity.ok(ApiResponse.success(
                "تم رفض طلب العمالة بنجاح",
                response
        ));
    }

    private Long getCurrentEmployeeNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authentication found in security context");
            throw new RuntimeException("المستخدم غير مصادق عليه");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse employee number from authentication: {}", authentication.getName());
            throw new RuntimeException("رقم الموظف غير صالح في سياق المصادقة");
        }
    }
}
