package com.techno.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
 * Entity representing a single installment of a loan.
 *
 * Generated automatically when a loan is approved.
 * Each installment is deducted monthly during payroll calculation.
 *
 * Payment Status Flow:
 * UNPAID → PAID (when deducted from salary)
 *
 * Features:
 * - Automatic payment during payroll
 * - Postponement support
 * - Tracks which payroll month the payment was made
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Loan Management
 */
@Entity
@Table(name = "loan_installments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "loan_id", "installment_no" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = { "loan" })
@ToString(exclude = { "loan" })
@EntityListeners(AuditingEntityListener.class)
public class LoanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "installment_id")
    private Long installmentId;

    @NotNull(message = "معرف القرض مطلوب")
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @NotNull(message = "رقم القسط مطلوب")
    @Positive(message = "يجب أن يكون رقم القسط موجباً")
    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @NotNull(message = "تاريخ الاستحقاق مطلوب")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull(message = "مبلغ القسط مطلوب")
    @Positive(message = "يجب أن يكون مبلغ القسط موجباً")
    @Column(name = "installment_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal installmentAmount;

    /**
     * Date when payment was deducted from salary
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    /**
     * Actual amount paid (usually equals installmentAmount)
     */
    @Column(name = "paid_amount", precision = 12, scale = 4)
    private BigDecimal paidAmount;

    /**
     * Payment status:
     * UNPAID = Not yet deducted
     * PAID = Deducted from salary
     * POSTPONED = Due date moved to future month
     */
    @Pattern(regexp = "^(UNPAID|PAID|POSTPONED)$", message = "حالة الدفع يجب أن تكون UNPAID أو PAID أو POSTPONED")
    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    /**
     * Payroll month when payment was deducted
     * Format: YYYY-MM (e.g., "2025-11")
     */
    @Column(name = "salary_month", length = 7)
    private String salaryMonth;

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
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private Loan loan;

    // Helper methods
    public boolean isPaid() {
        return "PAID".equals(this.paymentStatus);
    }

    public boolean isUnpaid() {
        return "UNPAID".equals(this.paymentStatus);
    }

    public boolean isPostponed() {
        return "POSTPONED".equals(this.paymentStatus);
    }

    /**
     * Mark installment as paid
     * 
     * @param paymentDate  Date of payment
     * @param amount       Amount paid
     * @param payrollMonth Payroll month (YYYY-MM format)
     */
    public void markAsPaid(LocalDate paymentDate, BigDecimal amount, String payrollMonth) {
        this.paidDate = paymentDate;
        this.paidAmount = amount;
        this.salaryMonth = payrollMonth;
        this.paymentStatus = "PAID";
    }

    /**
     * Postpone installment to new date
     * 
     * @param newDueDate New due date
     */
    public void postpone(LocalDate newDueDate) {
        this.dueDate = newDueDate;
        this.paymentStatus = "POSTPONED";
    }
}
