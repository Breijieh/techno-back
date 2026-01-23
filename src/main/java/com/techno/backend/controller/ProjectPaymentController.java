package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.payment.PaymentRecordRequest;
import com.techno.backend.dto.payment.PaymentScheduleRequest;
import com.techno.backend.dto.payment.PaymentScheduleResponse;
import com.techno.backend.dto.payment.PaymentUpdateRequest;
import com.techno.backend.service.ProjectPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Project Payment Schedule Management.
 * Provides endpoints for payment milestones, tracking, and recording payments.
 *
 * Base URL: /api/projects/payments
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectPaymentController {

    private final ProjectPaymentService paymentService;

    /**
     * Add a payment schedule/milestone to a project.
     *
     * POST /api/projects/{projectId}/payments
     *
     * @param projectId Project code
     * @param request Payment schedule request
     * @return Created payment schedule
     */
    @PostMapping("/{projectId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PaymentScheduleResponse>> addPaymentSchedule(
            @PathVariable Long projectId,
            @Valid @RequestBody PaymentScheduleRequest request) {

        log.info("POST /api/projects/{}/payments - Adding payment schedule", projectId);

        // Set project code from path variable
        request.setProjectCode(projectId);

        PaymentScheduleResponse response = paymentService.addPaymentSchedule(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "تم إضافة جدول الدفع بنجاح",
                response
        ));
    }

    /**
     * Get all payments for a specific project.
     *
     * GET /api/projects/{projectId}/payments
     *
     * @param projectId Project code
     * @return List of payment schedules
     */
    @GetMapping("/{projectId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<PaymentScheduleResponse>>> getProjectPayments(
            @PathVariable Long projectId) {

        log.info("GET /api/projects/{}/payments", projectId);

        List<PaymentScheduleResponse> payments = paymentService.getProjectPayments(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع مدفوعات المشروع بنجاح",
                payments
        ));
    }

    /**
     * Get payment schedule by ID.
     *
     * GET /api/projects/payments/{id}
     *
     * @param id Payment ID
     * @return Payment schedule details
     */
    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PaymentScheduleResponse>> getPaymentById(@PathVariable Long id) {
        log.info("GET /api/projects/payments/{}", id);

        PaymentScheduleResponse payment = paymentService.getPaymentById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع جدول الدفع بنجاح",
                payment
        ));
    }

    /**
     * Update payment schedule details.
     *
     * PUT /api/projects/payments/{id}
     *
     * @param id Payment ID
     * @param request Update request
     * @return Updated payment schedule
     */
    @PutMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PaymentScheduleResponse>> updatePaymentSchedule(
            @PathVariable Long id,
            @Valid @RequestBody PaymentUpdateRequest request) {

        log.info("PUT /api/projects/payments/{} - Updating payment schedule", id);

        PaymentScheduleResponse payment = paymentService.updatePaymentSchedule(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "تم تحديث جدول الدفع بنجاح",
                payment
        ));
    }

    /**
     * Delete payment schedule.
     *
     * DELETE /api/projects/payments/{id}
     *
     * @param id Payment ID
     * @return Success message
     */
    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deletePaymentSchedule(@PathVariable Long id) {
        log.info("DELETE /api/projects/payments/{} - Deleting payment schedule", id);

        paymentService.deletePaymentSchedule(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم حذف جدول الدفع بنجاح",
                null
        ));
    }

    /**
     * Record an actual payment against a payment schedule.
     *
     * POST /api/projects/payments/{id}/record
     *
     * @param id Payment schedule ID
     * @param request Payment record request
     * @return Updated payment schedule
     */
    @PostMapping("/payments/{id}/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PaymentScheduleResponse>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRecordRequest request) {

        log.info("POST /api/projects/payments/{}/record - Recording payment", id);

        PaymentScheduleResponse payment = paymentService.recordPayment(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "تم تسجيل الدفع بنجاح",
                payment
        ));
    }

    /**
     * Get all overdue payments (past due date and not fully paid).
     *
     * GET /api/projects/payments/overdue
     *
     * @return List of overdue payments
     */
    @GetMapping("/payments/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<PaymentScheduleResponse>>> getOverduePayments() {
        log.info("GET /api/projects/payments/overdue");

        List<PaymentScheduleResponse> overduePayments = paymentService.getOverduePayments();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع المدفوعات المتأخرة بنجاح",
                overduePayments
        ));
    }

    /**
     * Get payments due within specified number of days.
     *
     * GET /api/projects/payments/due-soon?days=7
     *
     * @param days Number of days ahead to check (default: 7)
     * @return List of upcoming due payments
     */
    @GetMapping("/payments/due-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<ApiResponse<List<PaymentScheduleResponse>>> getPaymentsDueSoon(
            @RequestParam(defaultValue = "7") int days) {

        log.info("GET /api/projects/payments/due-soon - days: {}", days);

        List<PaymentScheduleResponse> upcomingPayments = paymentService.getPaymentsDueWithinDays(days);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع المدفوعات القادمة بنجاح",
                upcomingPayments
        ));
    }

    /**
     * Calculate total remaining amount for a project.
     *
     * GET /api/projects/{projectId}/payments/remaining
     *
     * @param projectId Project code
     * @return Total remaining unpaid amount
     */
    @GetMapping("/{projectId}/payments/remaining")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getProjectRemainingAmount(@PathVariable Long projectId) {
        log.info("GET /api/projects/{}/payments/remaining", projectId);

        BigDecimal remainingAmount = paymentService.calculateProjectRemainingAmount(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "تم حساب المبلغ المتبقي بنجاح",
                remainingAmount
        ));
    }
}
