package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity representing a transaction type in the payroll system.
 * Maps to TRANSACTIONS_TYPES table in database.
 *
 * Transaction types define allowances (e.g., overtime, bonuses) and deductions
 * (e.g., late arrival, absence, loan installments) that affect employee
 * salaries.
 *
 * Standard types include:
 * - Basic Salary (1)
 * - Transportation (2)
 * - Housing (3)
 * - Communication (4)
 * - Overtime (9)
 * - Salary Raise (10)
 * - Late Deduction (20)
 * - Absence (21)
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "transactions_types", indexes = {
        @Index(name = "idx_transaction_type_active", columnList = "is_active"),
        @Index(name = "idx_transaction_type_category", columnList = "allowance_deduction")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionType extends BaseEntity {

    /**
     * Transaction type code - Primary Key
     * Standard codes:
     * 1-9: Basic salary components (Basic, Transport, Housing, etc.)
     * 10-19: Additional allowances (Raises, Bonuses)
     * 20-29: Deductions (Late, Absence, etc.)
     */
    @Id
    @Column(name = "type_code")
    private Long typeCode;

    /**
     * Transaction type name
     */
    @NotBlank(message = "اسم نوع المعاملة مطلوب")
    @Size(max = 250, message = "اسم نوع المعاملة لا يجب أن يتجاوز 250 حرفاً")
    @Column(name = "type_name", nullable = false, length = 250)
    private String typeName;

    /**
     * Category of transaction:
     * A = Allowance (adds to salary)
     * D = Deduction (subtracts from salary)
     */
    @NotNull(message = "فئة المعاملة مطلوبة")
    @Pattern(regexp = "^[AD]$", message = "فئة المعاملة يجب أن تكون 'A' (بدل) أو 'D' (خصم)")
    @Column(name = "allowance_deduction", nullable = false, length = 1)
    private String allowanceDeduction;

    /**
     * Flag indicating if this transaction is system-generated
     * Y = Generated automatically (e.g., overtime from attendance)
     * N = Manual entry by HR/Finance
     */
    @NotNull(message = "علامة التوليد التلقائي مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة التوليد التلقائي يجب أن تكون 'Y' أو 'N'")
    @Column(name = "is_system_generated", nullable = false, length = 1)
    @Builder.Default
    private String isSystemGenerated = "N";

    /**
     * Active status flag
     * Y = Active (can be used)
     * N = Inactive (archived)
     */
    @NotNull(message = "الحالة النشطة مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحالة النشطة يجب أن تكون 'Y' أو 'N'")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    // Helper methods

    /**
     * Check if this is an allowance type
     */
    public boolean isAllowance() {
        return "A".equals(this.allowanceDeduction);
    }

    /**
     * Check if this is a deduction type
     */
    public boolean isDeduction() {
        return "D".equals(this.allowanceDeduction);
    }

    /**
     * Check if this type is system-generated
     */
    public boolean isSystemGenerated() {
        return "Y".equals(this.isSystemGenerated);
    }

    /**
     * Check if this type is manually entered
     */
    public boolean isManualEntry() {
        return "N".equals(this.isSystemGenerated);
    }

    /**
     * Check if this type is active
     */
    public boolean isActive() {
        return "Y".equals(this.isActive);
    }

    /**
     * Activate this transaction type
     */
    public void activate() {
        this.isActive = "Y";
    }

    /**
     * Deactivate this transaction type
     */
    public void deactivate() {
        this.isActive = "N";
    }

    // Auto-generated Lombok methods
    public String getAllowanceDeduction() { return allowanceDeduction; }
}
