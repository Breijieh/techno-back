package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.transfer.TransferRequestDto;
import com.techno.backend.dto.transfer.TransferResponse;
import com.techno.backend.service.TransferService;
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
 * REST Controller for Employee Transfer Management.
 * Handles employee transfers between projects with multi-level approval
 * workflow.
 *
 * Base URL: /api/transfers
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    /**
     * Submit a new employee transfer request.
     *
     * POST /api/transfers
     *
     * @param request Transfer request DTO
     * @return Created transfer request
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<TransferResponse>> submitTransferRequest(
            @Valid @RequestBody TransferRequestDto request) {

        Long currentEmployeeNo = getCurrentEmployeeNo();
        log.info("POST /api/transfers - Submitting transfer request for employee: {}, Requester: {}",
                request.getEmployeeNo(), currentEmployeeNo);

        TransferResponse response = transferService.submitTransferRequest(request, currentEmployeeNo);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "تم إرسال طلب النقل بنجاح",
                response));
    }

    /**
     * Get transfer request by ID.
     *
     * GET /api/transfers/{id}
     *
     * @param id Transfer request number
     * @return Transfer request details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'HR')")
    public ResponseEntity<ApiResponse<TransferResponse>> getRequestById(@PathVariable Long id) {
        log.info("GET /api/transfers/{}", id);

        TransferResponse response = transferService.getRequestById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلب النقل بنجاح",
                response));
    }

    /**
     * Get pending requests for current approver.
     *
     * GET /api/transfers/pending
     *
     * @return List of pending requests awaiting approval
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getPendingRequests() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.info("GET /api/transfers/pending - Admin view, returning all pending requests");
            List<TransferResponse> requests = transferService.getAllPendingRequests();
            return ResponseEntity.ok(ApiResponse.success(
                    "تم استرجاع الطلبات المعلقة بنجاح",
                    requests));
        }

        Long approverNo = getCurrentEmployeeNo();
        log.info("GET /api/transfers/pending - Approver: {}", approverNo);

        List<TransferResponse> requests = transferService.getPendingRequestsByApprover(approverNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع الطلبات المعلقة بنجاح",
                requests));
    }

    /**
     * Get all transfer requests for a project.
     *
     * GET /api/transfers/project/{projectId}
     *
     * @param projectId Project code
     * @return List of transfer requests
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getRequestsByProject(
            @PathVariable Long projectId) {

        log.info("GET /api/transfers/project/{}", projectId);

        List<TransferResponse> requests = transferService.getRequestsByProject(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلبات نقل المشروع بنجاح",
                requests));
    }

    /**
     * Get approved but not executed transfers.
     *
     * GET /api/transfers/approved-pending
     *
     * @return List of approved pending execution transfers
     */
    @GetMapping("/approved-pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getApprovedPending() {
        log.info("GET /api/transfers/approved-pending");

        List<TransferResponse> requests = transferService.getApprovedNotExecuted();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع عمليات النقل المعتمدة المعلقة بنجاح",
                requests));
    }

    /**
     * Get all transfer requests (for admins and managers).
     *
     * GET /api/transfers
     *
     * @return List of all transfer requests
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getAllTransferRequests() {
        log.info("GET /api/transfers - Getting all transfer requests");

        List<TransferResponse> requests = transferService.getAllTransferRequests();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع طلبات النقل بنجاح",
                requests
        ));
    }

    /**
     * Approve a transfer request.
     *
     * POST /api/transfers/{id}/approve
     *
     * @param id Transfer request number
     * @return Updated transfer request
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<TransferResponse>> approveRequest(@PathVariable Long id) {

        Long approverNo = getCurrentEmployeeNo();
        log.info("POST /api/transfers/{}/approve - Approver: {}", id, approverNo);

        TransferResponse response = transferService.approveTransferRequest(id, approverNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم اعتماد طلب النقل بنجاح",
                response));
    }

    /**
     * Reject a transfer request.
     *
     * POST /api/transfers/{id}/reject
     *
     * @param id   Transfer request number
     * @param body Request body with rejection reason
     * @return Updated transfer request
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<TransferResponse>> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Long approverNo = getCurrentEmployeeNo();
        String reason = body.get("reason");
        log.info("POST /api/transfers/{}/reject - Approver: {}", id, approverNo);

        TransferResponse response = transferService.rejectTransferRequest(id, approverNo, reason);

        return ResponseEntity.ok(ApiResponse.success(
                "تم رفض طلب النقل بنجاح",
                response));
    }

    /**
     * Execute an approved transfer.
     *
     * POST /api/transfers/{id}/execute
     *
     * @param id Transfer request number
     * @return Success message
     */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<TransferResponse>> executeTransfer(@PathVariable Long id) {

        Long executorNo = getCurrentEmployeeNo();
        log.info("POST /api/transfers/{}/execute - Executor: {}", id, executorNo);

        TransferResponse response = transferService.executeTransfer(id, executorNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم تنفيذ النقل بنجاح",
                response));
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
                log.debug("Principal is not an employee number (likely a system user): {}", authentication.getName());
                return null;
            }
        }

        return null;
    }
}
