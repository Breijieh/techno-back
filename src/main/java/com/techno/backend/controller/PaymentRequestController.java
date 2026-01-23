package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.paymentrequest.PaymentProcessRequest;
import com.techno.backend.dto.paymentrequest.PaymentRequestDto;
import com.techno.backend.dto.paymentrequest.PaymentRequestResponse;
import com.techno.backend.service.PaymentRequestService;
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
 * REST Controller for Project Payment Request Management.
 * Handles supplier payment requests with multi-level approval workflow.
 *
 * Base URL: /api/payment-requests
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@RestController
@RequestMapping("/payment-requests")
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestController {

        private final PaymentRequestService paymentRequestService;

        /**
         * Submit a new payment request.
         *
         * POST /api/payment-requests
         *
         * @param request     Payment request DTO
         * @param currentUser Currently authenticated user
         * @return Created payment request
         */
        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<PaymentRequestResponse>> submitPaymentRequest(
                        @Valid @RequestBody PaymentRequestDto request) {

                Long currentEmployeeNo = getCurrentEmployeeNo();
                log.info("POST /api/payment-requests - Submitting payment request for project: {}, Employee: {}",
                                request.getProjectCode(), currentEmployeeNo);

                PaymentRequestResponse response = paymentRequestService.submitPaymentRequest(
                                request, currentEmployeeNo);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                "تم إرسال طلب الدفع بنجاح",
                                response));
        }

        /**
         * Get payment request by ID.
         *
         * GET /api/payment-requests/{id}
         *
         * @param id Request number
         * @return Payment request details
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<PaymentRequestResponse>> getRequestById(@PathVariable Long id) {
                log.info("GET /api/payment-requests/{}", id);

                PaymentRequestResponse response = paymentRequestService.getRequestById(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع طلب الدفع بنجاح",
                                response));
        }

        /**
         * Get pending requests for current approver.
         *
         * GET /api/payment-requests/pending
         *
         * @param currentUser Currently authenticated user
         * @return List of pending requests awaiting approval
         */
        @GetMapping("/pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<List<PaymentRequestResponse>>> getPendingRequests() {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (isAdmin) {
                        log.info("GET /api/payment-requests/pending - Admin view, returning all pending requests");
                        List<PaymentRequestResponse> requests = paymentRequestService.getAllPendingRequests();
                        return ResponseEntity.ok(ApiResponse.success(
                                        "تم استرجاع الطلبات المعلقة بنجاح",
                                        requests));
                }

                Long approverNo = getCurrentEmployeeNo();
                log.info("GET /api/payment-requests/pending - Approver: {}", approverNo);

                List<PaymentRequestResponse> requests = paymentRequestService.getPendingRequestsByApprover(
                                approverNo);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع الطلبات المعلقة بنجاح",
                                requests));
        }

        /**
         * Get all payment requests for a project.
         *
         * GET /api/payment-requests/project/{projectId}
         *
         * @param projectId Project code
         * @return List of payment requests
         */
        @GetMapping("/project/{projectId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<List<PaymentRequestResponse>>> getRequestsByProject(
                        @PathVariable Long projectId) {

                log.info("GET /api/payment-requests/project/{}", projectId);

                List<PaymentRequestResponse> requests = paymentRequestService.getRequestsByProject(projectId);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع طلبات دفع المشروع بنجاح",
                                requests));
        }

        /**
         * Get approved but not processed requests.
         *
         * GET /api/payment-requests/approved-pending
         *
         * @return List of approved pending processing requests
         */
        @GetMapping("/approved-pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<List<PaymentRequestResponse>>> getApprovedPending() {
                log.info("GET /api/payment-requests/approved-pending");

                List<PaymentRequestResponse> requests = paymentRequestService.getApprovedNotProcessed();

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع الطلبات المعتمدة المعلقة بنجاح",
                                requests));
        }

        /**
         * Get all payment requests (excluding deleted).
         * Used by General Manager and Admin to view all requests regardless of status.
         *
         * GET /api/payment-requests/all
         *
         * @return List of all payment requests
         */
        @GetMapping("/all")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER')")
        public ResponseEntity<ApiResponse<List<PaymentRequestResponse>>> getAllPaymentRequests() {
                log.info("GET /api/payment-requests/all - Fetching all payment requests");

                List<PaymentRequestResponse> requests = paymentRequestService.getAllPaymentRequests();

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع جميع طلبات الدفع بنجاح",
                                requests));
        }

        /**
         * Approve a payment request.
         *
         * POST /api/payment-requests/{id}/approve
         *
         * @param id          Request number
         * @param currentUser Currently authenticated user
         * @return Updated payment request
         */
        @PostMapping("/{id}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<PaymentRequestResponse>> approveRequest(
                        @PathVariable Long id) {

                Long approverNo = getCurrentEmployeeNo();
                log.info("POST /api/payment-requests/{}/approve - Approver: {}", id, approverNo);

                PaymentRequestResponse response = paymentRequestService.approvePaymentRequest(
                                id, approverNo);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم اعتماد طلب الدفع بنجاح",
                                response));
        }

        /**
         * Reject a payment request.
         *
         * POST /api/payment-requests/{id}/reject
         *
         * @param id          Request number
         * @param body        Request body with rejection reason
         * @param currentUser Currently authenticated user
         * @return Updated payment request
         */
        @PostMapping("/{id}/reject")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT_MANAGER', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<PaymentRequestResponse>> rejectRequest(
                        @PathVariable Long id,
                        @RequestBody Map<String, String> body) {

                Long approverNo = getCurrentEmployeeNo();
                String reason = body.get("reason");
                log.info("POST /api/payment-requests/{}/reject - Approver: {}", id, approverNo);

                PaymentRequestResponse response = paymentRequestService.rejectPaymentRequest(
                                id, approverNo, reason);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم رفض طلب الدفع بنجاح",
                                response));
        }

        /**
         * Process an approved payment request.
         *
         * POST /api/payment-requests/{id}/process
         *
         * @param id          Request number
         * @param request     Payment processing details
         * @param currentUser Currently authenticated user
         * @return Success message
         */
        @PostMapping("/{id}/process")
        @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER')")
        public ResponseEntity<ApiResponse<Void>> processPayment(
                        @PathVariable Long id,
                        @Valid @RequestBody PaymentProcessRequest request) {

                Long financeEmployeeNo = getCurrentEmployeeNo();
                log.info("POST /api/payment-requests/{}/process - Finance: {}", id, financeEmployeeNo);

                paymentRequestService.processPayment(id, request, financeEmployeeNo);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم معالجة الدفع بنجاح",
                                null));
        }

        // ==================== Helper Methods ====================

        /**
         * Get current authenticated user's employee number.
         *
         * Extracts employee number from security context.
         * The JWT filter sets the principal to the employee number for easy access.
         *
         * @return Employee number from security context
         * @throws RuntimeException if authentication is missing or invalid
         */
        private Long getCurrentEmployeeNo() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                        log.error("No authentication found in security context");
                        throw new RuntimeException("المستخدم غير مصادق عليه");
                }

                // The JWT filter sets the principal to the employee number (Long) if available
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                        log.debug("Found employee number {} from security context", principal);
                        return (Long) principal;
                }

                // Fallback: try to parse as Long if it's a String representation
                if (principal instanceof String) {
                        try {
                                Long employeeNo = Long.parseLong((String) principal);
                                log.debug("Parsed employee number {} from string principal", employeeNo);
                                return employeeNo;
                        } catch (NumberFormatException e) {
                                log.error("Failed to parse employee number from principal: {}", principal);
                                throw new RuntimeException("رقم الموظف غير صالح في سياق المصادقة");
                        }
                }

                log.error("Unexpected principal type: {} (class: {})", principal,
                                principal != null ? principal.getClass().getName() : "null");
                throw new RuntimeException("لم يتم العثور على رقم الموظف في سياق الأمان. نوع الرئيسي: "
                                + (principal != null ? principal.getClass().getName() : "null"));
        }
}
