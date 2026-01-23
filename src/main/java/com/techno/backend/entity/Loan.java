package com.techno.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an employee loan.
 *
 * Employees can request loans with installment-based repayment.
 * Installments are automatically deducted from monthly salary during payroll
 * calculation.
 *
 * Approval Flow:
 * - Level 1: HR Manager
 * - Level 2: Finance Manager (final)
 *
 * Features:
 * - Installment schedule generation on approval
 * - Monthly automatic deduction from salary
 * - Postponement requests for installments
 * - Automatic loan closure when fully paid
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = { "installments" })
@ToString(exclude = { "installments" })
@EntityListeners(AuditingEntityListener.class)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @NotNull(message = "مبلغ القرض مطلوب")
    @Positive(message = "مبلغ القرض يجب أن يكون موجباً")
    @Column(name = "loan_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal loanAmount;

    @NotNull(message = "عدد الأقساط مطلوب")
    @Positive(message = "عدد الأقساط يجب أن يكون موجباً")
    @Column(name = "no_of_installments", nullable = false)
    private Integer noOfInstallments;

    @NotNull(message = "تاريخ القسط الأول مطلوب")
    @Column(name = "first_installment_date", nullable = false)
    private LocalDate firstInstallmentDate;

    /**
     * Amount to be deducted each month
     * Calculated as: loanAmount / noOfInstallments
     */
    @NotNull(message = "مبلغ القسط مطلوب")
    @Column(name = "installment_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal installmentAmount;

    /**
     * Remaining unpaid balance
     * Updated after each installment payment
     * When reaches 0, loan is marked inactive
     */
    @NotNull(message = "الرصيد المتبقي مطلوب")
    @Column(name = "remaining_balance", nullable = false, precision = 12, scale = 4)
    private BigDecimal remainingBalance;

    @Column(name = "request_date")
    private LocalDate requestDate;

    /**
     * Transaction status:
     * N = New (pending approval)
     * A = Approved
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
     * Current approval level (1 = HR, 2 = Finance)
     */
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Descriptive name of the next approval level (e.g., HR Manager)
     */
    @Column(name = "next_app_level_name", length = 100)
    private String nextAppLevelName;

    /**
     * Employee number who gave final approval
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Loan active status:
     * Y = Active (has unpaid installments)
     * N = Closed (fully paid or inactive)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون Y أو N")
    @Column(name = "is_active", length = 1)
    @Builder.Default
    private String isActive = "Y";

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
    @JoinColumn(name = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    @JsonManagedReference
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoanInstallment> installments = new ArrayList<>();

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

    public boolean isActiveLoan() {
        return "Y".equals(this.isActive);
    }

    public boolean isFullyPaid() {
        return remainingBalance != null && remainingBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Deduct payment from remaining balance
     * 
     * @param amount Amount to deduct
     */
    public void deductPayment(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }

        if (this.remainingBalance == null) {
            this.remainingBalance = this.loanAmount;
        }
        this.remainingBalance = this.remainingBalance.subtract(amount);

        // Mark loan as inactive if fully paid
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.isActive = "N";
            this.remainingBalance = BigDecimal.ZERO;
        }
    }
}
