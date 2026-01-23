package com.techno.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a request to postpone a loan installment.
 *
 * Employees can request to postpone specific installments to a different month.
 * Unlimited postponements allowed per company policy.
 *
 * Approval Flow:
 * - Level 1: HR Manager
 * - Level 2: Finance Manager
 * - Level 3: General Manager (final)
 *
 * Features:
 * - Individual installment postponement
 * - Mass postponement by admin (e.g., postpone all Ramadan installments)
 * - Automatic due date adjustment on approval
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Entity
@Table(name = "loan_postponement_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class LoanPostponementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @NotNull(message = "معرف القرض مطلوب")
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @NotNull(message = "معرف القسط مطلوب")
    @Column(name = "installment_id", nullable = false)
    private Long installmentId;

    @NotNull(message = "تاريخ الاستحقاق الحالي مطلوب")
    @Column(name = "current_due_date", nullable = false)
    private LocalDate currentDueDate;

    @NotNull(message = "تاريخ الاستحقاق الجديد مطلوب")
    @Column(name = "new_due_date", nullable = false)
    private LocalDate newDueDate;

    @Column(name = "postponement_reason", length = 500)
    private String postponementReason;

    @Column(name = "request_date")
    private LocalDate requestDate;

    /**
     * Transaction status:
     * N = New (pending approval)
     * A = Approved (due date updated)
     * R = Rejected
     */
    @Pattern(regexp = "^[NAR]$", message = "الحالة يجب أن تكون N أو A أو R")
    @Column(name = "trans_status", length = 1)
    @Builder.Default
    private String transStatus = "N";

    /**
     * Employee number of next approver in the approval chain
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    /**
     * Current approval level (1 = HR, 2 = Finance, 3 = GM)
     */
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Employee number who gave final approval
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    /**
     * Rejection reason if request was rejected
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Audit fields
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "modified_by")
    private Long modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    // Relationships
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private Loan loan;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id", insertable = false, updatable = false)
    private LoanInstallment installment;

    // Helper methods
    public boolean isApproved() {
        return "A".equals(this.transStatus);
    }

    public boolean isRejected() {
        return "R".equals(this.transStatus);
    }

    public boolean isPending() {
        return "N".equals(this.transStatus);
    }

    /**
     * Calculate number of months postponed
     */
    public int getMonthsPostponed() {
        if (currentDueDate == null || newDueDate == null) {
            return 0;
        }

        int currentMonths = currentDueDate.getYear() * 12 + currentDueDate.getMonthValue();
        int newMonths = newDueDate.getYear() * 12 + newDueDate.getMonthValue();

        return newMonths - currentMonths;
    }
}
