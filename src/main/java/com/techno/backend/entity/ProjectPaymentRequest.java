package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a payment request for project suppliers/contractors.
 * Maps to PROJECT_PAYMENT_REQUEST table in database.
 *
 * Handles:
 * - Payment requests to suppliers for materials, services
 * - Multi-level approval workflow (Project Mgr → Regional Mgr → Finance Mgr)
 * - Payment processing tracking
 *
 * Example:
 * - Request from supplier ABC for SAR 50,000 for construction materials
 * - Requires 3-level approval before payment processing
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_payment_request", indexes = {
    @Index(name = "idx_payment_req_project", columnList = "project_code"),
    @Index(name = "idx_payment_req_supplier", columnList = "supplier_code"),
    @Index(name = "idx_payment_req_status", columnList = "trans_status"),
    @Index(name = "idx_payment_req_approver", columnList = "next_approval")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPaymentRequest extends BaseEntity {

    /**
     * Payment request number - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_no")
    private Long requestNo;

    /**
     * Project code - Foreign Key
     */
    @NotNull(message = "رمز المشروع مطلوب")
    @Column(name = "project_code", nullable = false)
    private Long projectCode;

    /**
     * Supplier code - Foreign Key
     */
    @NotNull(message = "رمز المورد مطلوب")
    @Column(name = "supplier_code", nullable = false)
    private Long supplierCode;

    /**
     * Request date
     */
    @NotNull(message = "تاريخ الطلب مطلوب")
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /**
     * Requested payment amount in SAR
     */
    @NotNull(message = "مبلغ الدفع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "المبلغ يجب أن يكون أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal paymentAmount;

    /**
     * Payment purpose/description
     */
    @NotBlank(message = "غرض الدفع مطلوب")
    @Size(max = 500, message = "الغرض لا يجب أن يتجاوز 500 حرف")
    @Column(name = "payment_purpose", nullable = false, length = 500)
    private String paymentPurpose;

    /**
     * Request transaction status
     * P = Pending approval
     * A = Approved (final)
     * R = Rejected
     */
    @NotBlank(message = "حالة المعاملة مطلوبة")
    @Pattern(regexp = "^[PAR]$", message = "الحالة يجب أن تكون P (معلق) أو A (موافق عليه) أو R (مرفوض)")
    @Column(name = "trans_status", nullable = false, length = 1)
    @Builder.Default
    private String transStatus = "P";

    /**
     * Next approver employee number
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    /**
     * Next approval level
     * 1 = Project Manager
     * 2 = Regional Manager
     * 3 = Finance Manager
     */
    @Min(value = 1, message = "مستوى الموافقة يجب أن يكون 1 على الأقل")
    @Max(value = 3, message = "مستوى الموافقة لا يمكن أن يتجاوز 3")
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Approved by (final approver)
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * Approval date
     */
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    /**
     * Rejection reason
     */
    @Size(max = 500, message = "سبب الرفض لا يجب أن يتجاوز 500 حرف")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Requested by employee number
     */
    @Column(name = "requested_by")
    private Long requestedBy;

    /**
     * Supporting document attachment path
     */
    @Size(max = 500, message = "مسار المرفق لا يجب أن يتجاوز 500 حرف")
    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    /**
     * Payment processing status
     * N = Not processed
     * Y = Processed
     */
    @NotNull(message = "حالة المعالجة مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "حالة المعالجة يجب أن تكون Y أو N")
    @Column(name = "is_processed", nullable = false, length = 1)
    @Builder.Default
    private String isProcessed = "N";

    /**
     * Soft delete flag
     * Y = Deleted
     * N = Active
     */
    @NotNull(message = "حالة الحذف مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون Y أو N")
    @Column(name = "is_deleted", nullable = false, length = 1)
    @Builder.Default
    private String isDeleted = "N";

    // Relationships

    /**
     * Reference to project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", referencedColumnName = "project_code",
                insertable = false, updatable = false)
    private Project project;

    // Helper methods

    /**
     * Check if request is pending approval
     */
    public boolean isPending() {
        return "P".equals(this.transStatus);
    }

    /**
     * Check if request is approved
     */
    public boolean isApproved() {
        return "A".equals(this.transStatus);
    }

    /**
     * Check if request is rejected
     */
    public boolean isRejected() {
        return "R".equals(this.transStatus);
    }

    /**
     * Check if payment has been processed
     */
    public boolean isProcessed() {
        return "Y".equals(this.isProcessed);
    }

    /**
     * Check if request is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Approve request and move to next level
     */
    public void approve(Long approverNo, Integer nextLevel, Long nextApproverNo) {
        if (nextLevel == null || nextLevel > 3) {
            // Final approval
            this.transStatus = "A";
            this.approvedBy = approverNo;
            this.approvedDate = LocalDateTime.now();
            this.nextApproval = null;
            this.nextAppLevel = null;
        } else {
            // Intermediate approval
            this.nextAppLevel = nextLevel;
            this.nextApproval = nextApproverNo;
        }
    }

    /**
     * Reject request
     */
    public void reject(Long approverNo, String reason) {
        this.transStatus = "R";
        this.approvedBy = approverNo;
        this.approvedDate = LocalDateTime.now();
        this.rejectionReason = reason;
        this.nextApproval = null;
        this.nextAppLevel = null;
    }

    /**
     * Mark as processed
     */
    public void markAsProcessed() {
        this.isProcessed = "Y";
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.isDeleted = "Y";
    }
}
