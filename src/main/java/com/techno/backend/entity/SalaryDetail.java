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
import java.time.LocalDateTime;

/**
 * Entity representing individual line items in monthly salary calculation.
 *
 * Each record represents one allowance or deduction component.
 * Multiple detail lines make up one salary header.
 *
 * Examples:
 * - Line 1: Basic Salary (Allowance)
 * - Line 2: Housing Allowance (Allowance)
 * - Line 3: Overtime Payment (Allowance)
 * - Line 4: Insurance Deduction (Deduction)
 * - Line 5: Loan Installment (Deduction)
 * - Line 6: Late Penalty (Deduction)
 *
 * Reference tracking:
 * - Can link to source record (loan, attendance, etc.)
 * - referenceTable: Table name (e.g., "loans", "emp_monthly_allowances")
 * - referenceId: ID in that table
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@Entity
@Table(name = "salary_detail", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"salary_id", "line_no"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"salaryHeader"})
@ToString(exclude = {"salaryHeader"})
@EntityListeners(AuditingEntityListener.class)
public class SalaryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @NotNull(message = "معرف الراتب مطلوب")
    @Column(name = "salary_id", nullable = false)
    private Long salaryId;

    /**
     * Line number (sequence within salary header)
     * Used for ordering detail lines
     */
    @NotNull(message = "رقم السطر مطلوب")
    @Positive(message = "رقم السطر يجب أن يكون موجباً")
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    /**
     * Transaction type code (references TRANSACTION_TYPES table)
     * Examples:
     * - 1: Housing Allowance
     * - 2: Transportation Allowance
     * - 3: Overtime Allowance
     * - 20: Insurance Deduction
     * - 21: Loan Installment
     * - 22: Late Penalty
     */
    @NotNull(message = "رمز نوع المعاملة مطلوب")
    @Column(name = "trans_type_code", nullable = false)
    private Long transTypeCode;

    @NotNull(message = "مبلغ المعاملة مطلوب")
    @Column(name = "trans_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal transAmount;

    /**
     * Transaction category:
     * A = Allowance (adds to salary)
     * D = Deduction (subtracts from salary)
     */
    @NotNull(message = "فئة المعاملة مطلوبة")
    @Pattern(regexp = "^[AD]$", message = "فئة المعاملة يجب أن تكون A أو D")
    @Column(name = "trans_category", length = 1, nullable = false)
    private String transCategory;

    /**
     * Reference to source table (optional)
     * Examples: "loans", "emp_monthly_allowances", "attendance_transactions"
     */
    @Column(name = "reference_table", length = 50)
    private String referenceTable;

    /**
     * ID in the reference table (optional)
     * Example: loan_id, transaction_no, attendance_id
     */
    @Column(name = "reference_id")
    private Long referenceId;

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
    @JoinColumn(name = "salary_id", insertable = false, updatable = false)
    private SalaryHeader salaryHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

    // Helper methods
    public boolean isAllowance() {
        return "A".equals(this.transCategory);
    }

    public boolean isDeduction() {
        return "D".equals(this.transCategory);
    }

    public boolean hasReference() {
        return referenceTable != null && referenceId != null;
    }
}
