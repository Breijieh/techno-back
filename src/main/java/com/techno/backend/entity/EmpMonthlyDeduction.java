package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing monthly deductions from employee salary.
 *
 * Deductions can be:
 * - Fixed (permanent part of salary structure, e.g., insurance)
 * - Variable (one-time or periodical, e.g., fines)
 * - Auto-generated (from attendance or loan installments)
 * - Manual entry (by HR/admin)
 *
 * Examples:
 * - Insurance deduction (fixed)
 * - Loan installment (auto-generated from loans)
 * - Late arrival penalty (auto-generated from attendance)
 * - Absence deduction (auto-generated from attendance)
 * - Administrative fine (manual, one-time)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@Entity
@Table(name = "emp_monthly_deductions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class EmpMonthlyDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_no")
    private Long transactionNo;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Transaction type code (references TRANSACTION_TYPES table)
     * Example codes:
     * - 20: Insurance Deduction
     * - 21: Loan Installment
     * - 22: Late Arrival Penalty
     * - 23: Absence Deduction
     * - 24: Early Departure Penalty
     */
    @NotNull(message = "رمز النوع مطلوب")
    @Column(name = "type_code", nullable = false)
    private Long typeCode;

    @NotNull(message = "مبلغ الخصم مطلوب")
    @Positive(message = "مبلغ الخصم يجب أن يكون موجباً")
    @Column(name = "deduction_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal deductionAmount;

    /**
     * Start date for periodical deductions
     * For one-time deductions, this can be null
     */
    @Column(name = "deduction_start_date")
    private LocalDate deductionStartDate;

    /**
     * End date for periodical deductions
     * For permanent deductions, this is null
     */
    @Column(name = "deduction_end_date")
    private LocalDate deductionEndDate;

    /**
     * Transaction status:
     * N = New (pending approval for manual entries)
     * A = Active/Applied/Approved
     * R = Rejected
     * C = Cancelled
     */
    @Pattern(regexp = "^[NARC]$", message = "الحالة يجب أن تكون N أو A أو R أو C")
    @Column(name = "trans_status", length = 1)
    @Builder.Default
    private String transStatus = "A";

    /**
     * Number of days (for absence/late deductions)
     * Populated from attendance system
     */
    @Column(name = "no_of_days")
    private Integer noOfDays;

    /**
     * Manual entry flag:
     * Y = Manually entered by HR/admin
     * N = Auto-generated (from attendance/loans/system)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الإدخال اليدوي يجب أن تكون Y أو N")
    @Column(name = "is_manual_entry", length = 1)
    @Builder.Default
    private String isManualEntry = "N";

    @Column(name = "entry_reason", length = 500)
    private String entryReason;

    /**
     * Deletion flag (soft delete):
     * Y = Deleted
     * N = Active
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون Y أو N")
    @Column(name = "is_deleted", length = 1)
    @Builder.Default
    private String isDeleted = "N";

    /**
     * For approval workflow (manual deductions only)
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    @Column(name = "next_app_level")
    private Integer nextAppLevel;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

    // Helper methods
    public boolean isActive() {
        return "A".equals(this.transStatus) && "N".equals(this.isDeleted);
    }

    public boolean isPending() {
        return "N".equals(this.transStatus);
    }

    public boolean isRejected() {
        return "R".equals(this.transStatus);
    }

    public boolean isCancelled() {
        return "C".equals(this.transStatus);
    }

    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    public boolean isManual() {
        return "Y".equals(this.isManualEntry);
    }

    /**
     * Check if deduction is active for a specific date
     */
    public boolean isActiveOn(LocalDate date) {
        if (!isActive()) {
            return false;
        }

        if (deductionStartDate != null && date.isBefore(deductionStartDate)) {
            return false;
        }

        if (deductionEndDate != null && date.isAfter(deductionEndDate)) {
            return false;
        }

        return true;
    }
}
