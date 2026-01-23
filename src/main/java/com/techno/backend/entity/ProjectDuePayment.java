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

/**
 * Entity representing a scheduled payment milestone for a project.
 * Maps to PROJECTS_DUE_PAYMENTS table in database.
 *
 * Each project has multiple payment milestones from the client.
 * Tracks:
 * - Due date for payment
 * - Expected amount
 * - Actual paid amount
 * - Payment status (PENDING, PARTIAL, PAID)
 *
 * Example:
 * - Project: Kempinski Hotel Riyadh
 * - Milestone 1: 30% down payment on 2024-01-15
 * - Milestone 2: 40% on completion of phase 1
 * - Milestone 3: 30% on final delivery
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "projects_due_payments", indexes = {
    @Index(name = "idx_payment_project", columnList = "project_code"),
    @Index(name = "idx_payment_status", columnList = "payment_status"),
    @Index(name = "idx_payment_due_date", columnList = "due_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDuePayment extends BaseEntity {

    /**
     * Payment ID - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * Project code - Foreign Key
     */
    @NotNull(message = "رمز المشروع مطلوب")
    @Column(name = "project_code", nullable = false)
    private Long projectCode;

    /**
     * Sequence number of payment within project
     * Example: 1, 2, 3 for first, second, third payments
     */
    @NotNull(message = "الرقم التسلسلي مطلوب")
    @Min(value = 1, message = "الرقم التسلسلي يجب أن يكون 1 على الأقل")
    @Max(value = 99, message = "الرقم التسلسلي لا يمكن أن يتجاوز 99")
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /**
     * Payment due date
     */
    @NotNull(message = "تاريخ الاستحقاق مطلوب")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Expected payment amount in SAR
     */
    @NotNull(message = "المبلغ المستحق مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "المبلغ المستحق يجب أن يكون أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "due_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal dueAmount;

    /**
     * Amount already paid (cumulative)
     */
    @NotNull(message = "المبلغ المدفوع مطلوب")
    @DecimalMin(value = "0.0", message = "المبلغ المدفوع لا يمكن أن يكون سالباً")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "paid_amount", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * Payment status
     * PENDING - No payment received
     * PARTIAL - Partial payment received
     * PAID - Fully paid
     */
    @NotBlank(message = "حالة الدفع مطلوبة")
    @Pattern(regexp = "^(PENDING|PARTIAL|PAID)$",
             message = "الحالة يجب أن تكون PENDING أو PARTIAL أو PAID")
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    /**
     * Date when payment was received (fully or last partial)
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Notes about the payment
     */
    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    @Column(name = "notes", length = 500)
    private String notes;

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
     * Check if payment is pending
     */
    public boolean isPending() {
        return "PENDING".equals(this.paymentStatus);
    }

    /**
     * Check if payment is partial
     */
    public boolean isPartial() {
        return "PARTIAL".equals(this.paymentStatus);
    }

    /**
     * Check if payment is fully paid
     */
    public boolean isPaid() {
        return "PAID".equals(this.paymentStatus);
    }

    /**
     * Check if payment is overdue
     */
    public boolean isOverdue() {
        if (dueDate == null || isPaid()) {
            return false;
        }
        return dueDate.isBefore(LocalDate.now());
    }

    /**
     * Calculate remaining amount to be paid
     */
    public BigDecimal getRemainingAmount() {
        if (dueAmount == null) {
            return BigDecimal.ZERO;
        }
        if (paidAmount == null) {
            return dueAmount;
        }
        return dueAmount.subtract(paidAmount);
    }

    /**
     * Calculate payment completion percentage
     */
    public BigDecimal getPaymentPercentage() {
        if (dueAmount == null || dueAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (paidAmount == null) {
            return BigDecimal.ZERO;
        }
        return paidAmount.multiply(BigDecimal.valueOf(100))
                .divide(dueAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate days until due date (negative if overdue)
     */
    public Long getDaysUntilDue() {
        if (dueDate == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Update payment status based on amounts
     */
    public void updateStatus() {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = "PENDING";
        } else if (paidAmount.compareTo(dueAmount) >= 0) {
            this.paymentStatus = "PAID";
            this.paymentDate = LocalDate.now();
        } else {
            this.paymentStatus = "PARTIAL";
        }
    }

    /**
     * Record a payment
     */
    public void recordPayment(BigDecimal amount) {
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        this.paidAmount = this.paidAmount.add(amount);
        updateStatus();
    }
}
