package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an employee transfer request between projects.
 * Maps to PROJECT_TRANSFER_REQUESTS table in database.
 *
 * Handles:
 * - Employee transfer from one project to another
 * - Multi-level approval workflow (Current Project Mgr → Target Project Mgr)
 * - Transfer effective date tracking
 * - Employee project assignment update after approval
 *
 * Example:
 * - Transfer Employee #123 from Project A to Project B
 * - Effective date: 2024-02-01
 * - Requires approval from both project managers
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_transfer_requests", indexes = {
    @Index(name = "idx_transfer_employee", columnList = "employee_no"),
    @Index(name = "idx_transfer_from_project", columnList = "from_project_code"),
    @Index(name = "idx_transfer_to_project", columnList = "to_project_code"),
    @Index(name = "idx_transfer_status", columnList = "trans_status"),
    @Index(name = "idx_transfer_approver", columnList = "next_approval")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTransferRequest extends BaseEntity {

    /**
     * Transfer request number - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_no")
    private Long transferNo;

    /**
     * Employee number to be transferred
     */
    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Source project code (from)
     */
    @NotNull(message = "المشروع المصدر مطلوب")
    @Column(name = "from_project_code", nullable = false)
    private Long fromProjectCode;

    /**
     * Target project code (to)
     */
    @NotNull(message = "المشروع الهدف مطلوب")
    @Column(name = "to_project_code", nullable = false)
    private Long toProjectCode;

    /**
     * Effective transfer date
     */
    @NotNull(message = "تاريخ النقل مطلوب")
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    /**
     * Reason for transfer
     */
    @NotBlank(message = "سبب النقل مطلوب")
    @Size(max = 500, message = "السبب لا يجب أن يتجاوز 500 حرف")
    @Column(name = "transfer_reason", nullable = false, length = 500)
    private String transferReason;

    /**
     * Transaction status
     * P = Pending approval
     * A = Approved
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
     * 1 = Current Project Manager
     * 2 = Target Project Manager
     */
    @Min(value = 1, message = "مستوى الموافقة يجب أن يكون 1 على الأقل")
    @Max(value = 2, message = "مستوى الموافقة لا يمكن أن يتجاوز 2")
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
     * Requested by employee number (usually HR or current project manager)
     */
    @Column(name = "requested_by")
    private Long requestedBy;

    /**
     * Whether transfer has been executed
     * Y = Employee project updated
     * N = Not yet executed
     */
    @NotNull(message = "حالة التنفيذ مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "حالة التنفيذ يجب أن تكون Y أو N")
    @Column(name = "is_executed", nullable = false, length = 1)
    @Builder.Default
    private String isExecuted = "N";

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
     * Reference to employee
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", referencedColumnName = "employee_no",
                insertable = false, updatable = false)
    private Employee employee;

    /**
     * Reference to source project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_project_code", referencedColumnName = "project_code",
                insertable = false, updatable = false)
    private Project fromProject;

    /**
     * Reference to target project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_project_code", referencedColumnName = "project_code",
                insertable = false, updatable = false)
    private Project toProject;

    // Helper methods

    /**
     * Check if transfer is pending approval
     */
    public boolean isPending() {
        return "P".equals(this.transStatus);
    }

    /**
     * Check if transfer is approved
     */
    public boolean isApproved() {
        return "A".equals(this.transStatus);
    }

    /**
     * Check if transfer is rejected
     */
    public boolean isRejected() {
        return "R".equals(this.transStatus);
    }

    /**
     * Check if transfer has been executed
     */
    public boolean isExecuted() {
        return "Y".equals(this.isExecuted);
    }

    /**
     * Check if transfer is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Approve transfer and move to next level
     */
    public void approve(Long approverNo, Integer nextLevel, Long nextApproverNo) {
        if (nextLevel == null || nextLevel > 2) {
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
     * Reject transfer
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
     * Mark as executed (employee project updated)
     */
    public void markAsExecuted() {
        this.isExecuted = "Y";
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.isDeleted = "Y";
    }

    /**
     * Validate transfer is not circular (from != to)
     */
    public boolean isValidTransfer() {
        return !fromProjectCode.equals(toProjectCode);
    }
}
