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
 * Entity representing the actual processing of an approved payment request.
 * Maps to PROJECT_PAYMENT_PROCESS table in database.
 *
 * After a payment request is approved, finance processes the payment and records:
 * - Actual payment date
 * - Actual amount paid (may differ slightly from requested)
 * - Payment method (Bank Transfer, Check, Cash, etc.)
 * - Bank reference/check number
 *
 * Example:
 * - Payment Request #123 for SAR 50,000 approved
 * - Processed on 2024-01-15 via Bank Transfer
 * - Amount: SAR 50,000.00
 * - Bank Reference: TRX-2024-001234
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_payment_process", indexes = {
    @Index(name = "idx_payment_proc_request", columnList = "request_no"),
    @Index(name = "idx_payment_proc_date", columnList = "payment_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPaymentProcess extends BaseEntity {

    /**
     * Process ID - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_no")
    private Long processNo;

    /**
     * Payment request number - Foreign Key
     */
    @NotNull(message = "رقم الطلب مطلوب")
    @Column(name = "request_no", nullable = false, unique = true)
    private Long requestNo;

    /**
     * Actual payment date
     */
    @NotNull(message = "تاريخ الدفع مطلوب")
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Actual amount paid in SAR
     */
    @NotNull(message = "المبلغ المدفوع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal paidAmount;

    /**
     * Payment method
     * BANK_TRANSFER = Bank wire transfer
     * CHECK = Company check
     * CASH = Cash payment
     * ONLINE = Online payment
     */
    @NotBlank(message = "طريقة الدفع مطلوبة")
    @Pattern(regexp = "^(BANK_TRANSFER|CHECK|CASH|ONLINE)$",
             message = "الطريقة يجب أن تكون BANK_TRANSFER أو CHECK أو CASH أو ONLINE")
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    /**
     * Bank reference number or check number
     */
    @Size(max = 100, message = "رقم المرجع لا يجب أن يتجاوز 100 حرف")
    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    /**
     * Bank name (for bank transfers)
     */
    @Size(max = 200, message = "اسم البنك لا يجب أن يتجاوز 200 حرف")
    @Column(name = "bank_name", length = 200)
    private String bankName;

    /**
     * Processing notes/remarks
     */
    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    @Column(name = "process_notes", length = 500)
    private String processNotes;

    /**
     * Processed by (finance employee number)
     */
    @Column(name = "processed_by")
    private Long processedBy;

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
     * Reference to payment request
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_no", referencedColumnName = "request_no",
                insertable = false, updatable = false)
    private ProjectPaymentRequest paymentRequest;

    // Helper methods

    /**
     * Check if payment was made by bank transfer
     */
    public boolean isBankTransfer() {
        return "BANK_TRANSFER".equals(this.paymentMethod);
    }

    /**
     * Check if payment was made by check
     */
    public boolean isCheck() {
        return "CHECK".equals(this.paymentMethod);
    }

    /**
     * Check if payment was made by cash
     */
    public boolean isCash() {
        return "CASH".equals(this.paymentMethod);
    }

    /**
     * Check if payment was made online
     */
    public boolean isOnline() {
        return "ONLINE".equals(this.paymentMethod);
    }

    /**
     * Check if payment is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.isDeleted = "Y";
    }

    /**
     * Get formatted reference info
     */
    public String getFormattedReference() {
        if (referenceNo == null) {
            return "لا يوجد مرجع";
        }
        if (bankName != null && !bankName.isEmpty()) {
            return String.format("%s - %s", bankName, referenceNo);
        }
        return referenceNo;
    }
}
