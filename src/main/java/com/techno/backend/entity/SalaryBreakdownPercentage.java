package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity representing salary breakdown percentages for different employee categories.
 * Maps to SALARY_BREAKDOWN_PERCENTAGES table in database.
 *
 * Saudi labor law requires specific salary breakdowns:
 * - Saudi employees: 83.4% Basic Salary + 16.6% Transportation
 * - Foreign employees: 55% Basic + 13.75% Transport + 5.2% Communication + other allowances
 *
 * These percentages are used during payroll calculation to split the monthly
 * gross salary into its components.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "salary_breakdown_percentages",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_salary_breakdown_category_type",
            columnNames = {"employee_category", "trans_type_code"}
        )
    },
    indexes = {
        @Index(name = "idx_salary_breakdown_category", columnList = "employee_category"),
        @Index(name = "idx_salary_breakdown_deleted", columnList = "is_deleted")
    }
)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryBreakdownPercentage extends BaseEntity {

    /**
     * Serial number - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_no")
    private Long serNo;

    /**
     * Employee category:
     * S = Saudi
     * F = Foreign
     */
    @NotNull(message = "فئة الموظف مطلوبة")
    @Pattern(regexp = "^[SF]$", message = "فئة الموظف يجب أن تكون 'S' (سعودي) أو 'F' (أجنبي)")
    @Column(name = "employee_category", nullable = false, length = 1)
    private String employeeCategory;

    /**
     * Transaction type code
     * References TRANSACTIONS_TYPES table
     * Example: 1=Basic Salary, 2=Transportation, 4=Communication
     */
    @NotNull(message = "رمز نوع المعاملة مطلوب")
    @Column(name = "trans_type_code", nullable = false)
    private Long transTypeCode;

    /**
     * Percentage of gross salary for this component
     * Stored as decimal: 0.8340 = 83.4%, 0.1660 = 16.6%
     * All percentages for a category must sum to 1.0000 (100%)
     */
    @NotNull(message = "نسبة الراتب مطلوبة")
    @DecimalMin(value = "0.0", message = "النسبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "1.0", message = "النسبة لا يمكن أن تتجاوز 100%")
    @Digits(integer = 1, fraction = 4, message = "تنسيق النسبة غير صالح (الحد الأقصى 4 أرقام عشرية)")
    @Column(name = "salary_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal salaryPercentage;

    /**
     * Soft delete flag
     * N = Active
     * Y = Deleted (archived)
     */
    @NotNull(message = "علامة الحذف مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون 'Y' أو 'N'")
    @Column(name = "is_deleted", nullable = false, length = 1)
    @Builder.Default
    private String isDeleted = "N";

    // Relationships

    /**
     * Reference to transaction type
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_type_code", referencedColumnName = "type_code",
                insertable = false, updatable = false)
    private TransactionType transactionType;

    // Helper methods

    /**
     * Check if this breakdown is for Saudi employees
     */
    public boolean isSaudi() {
        return "S".equals(this.employeeCategory);
    }

    /**
     * Check if this breakdown is for Foreign employees
     */
    public boolean isForeign() {
        return "F".equals(this.employeeCategory);
    }

    /**
     * Check if this record is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Check if this record is active
     */
    public boolean isActive() {
        return "N".equals(this.isDeleted);
    }

    /**
     * Soft delete this record
     */
    public void delete() {
        this.isDeleted = "Y";
    }

    /**
     * Restore this record
     */
    public void restore() {
        this.isDeleted = "N";
    }

    /**
     * Convert percentage to display format (e.g., 0.8340 → "83.40%")
     */
    public String getPercentageDisplay() {
        if (salaryPercentage == null) {
            return "0.00%";
        }
        return salaryPercentage.multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    /**
     * Calculate amount based on gross salary
     * Example: 5000 SAR * 0.8340 = 4170 SAR
     */
    public BigDecimal calculateAmount(BigDecimal grossSalary) {
        if (grossSalary == null || salaryPercentage == null) {
            return BigDecimal.ZERO;
        }
        return grossSalary.multiply(salaryPercentage)
                .setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
