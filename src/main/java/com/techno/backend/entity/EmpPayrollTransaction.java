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
 * Entity representing employee payroll transactions.
 * Maps to EMP_PAYROLL_TRANSACTIONS table in database.
 *
 * This table stores the standard salary components for each employee.
 * When an employee's salary is set up, their monthly salary is broken down
 * into components based on their category (Saudi/Foreign) using the
 * SALARY_BREAKDOWN_PERCENTAGES table.
 *
 * Example for Saudi employee with 5000 SAR monthly salary:
 * - Transaction 1: Basic Salary = 4170 SAR (83.4%)
 * - Transaction 2: Transportation = 830 SAR (16.6%)
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "emp_payroll_transactions", indexes = {
    @Index(name = "idx_emp_payroll_employee", columnList = "employee_no"),
    @Index(name = "idx_emp_payroll_type", columnList = "trans_type_code"),
    @Index(name = "idx_emp_payroll_active", columnList = "is_active"),
    @Index(name = "idx_emp_payroll_effective_date", columnList = "effective_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpPayrollTransaction extends BaseEntity {

    /**
     * Transaction ID - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    /**
     * Employee number
     * References EMPLOYEES_DETAILS table
     */
    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Transaction type code
     * References TRANSACTIONS_TYPES table
     */
    @NotNull(message = "رمز نوع المعاملة مطلوب")
    @Column(name = "trans_type_code", nullable = false)
    private Long transTypeCode;

    /**
     * Transaction amount in SAR
     * The actual amount for this salary component
     */
    @NotNull(message = "مبلغ المعاملة مطلوب")
    @DecimalMin(value = "0.0", message = "المبلغ لا يمكن أن يكون سالباً")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "trans_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal transAmount;

    /**
     * Transaction percentage (optional)
     * Stores the percentage used to calculate this amount
     * Example: 0.8340 for 83.4% basic salary
     */
    @DecimalMin(value = "0.0", message = "النسبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "1.0", message = "النسبة لا يمكن أن تتجاوز 100%")
    @Digits(integer = 1, fraction = 4, message = "تنسيق النسبة غير صالح")
    @Column(name = "trans_percentage", precision = 5, scale = 4)
    private BigDecimal transPercentage;

    /**
     * Effective date of this transaction
     * When this salary component becomes active
     */
    @NotNull(message = "تاريخ السريان مطلوب")
    @Column(name = "effective_date", nullable = false)
    @Builder.Default
    private LocalDate effectiveDate = LocalDate.now();

    /**
     * Active status flag
     * Y = Active (currently applied)
     * N = Inactive (historical or superseded)
     */
    @NotNull(message = "حالة النشاط مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون 'Y' أو 'N'")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    // Relationships

    /**
     * Reference to employee
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", referencedColumnName = "employee_no",
                insertable = false, updatable = false)
    private Employee employee;

    /**
     * Reference to transaction type
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_type_code", referencedColumnName = "type_code",
                insertable = false, updatable = false)
    private TransactionType transactionType;

    // Helper methods

    /**
     * Check if this transaction is active
     */
    public boolean isActive() {
        return "Y".equals(this.isActive);
    }

    /**
     * Activate this transaction
     */
    public void activate() {
        this.isActive = "Y";
    }

    /**
     * Deactivate this transaction
     */
    public void deactivate() {
        this.isActive = "N";
    }

    /**
     * Check if this transaction is effective now
     */
    public boolean isEffectiveNow() {
        if (effectiveDate == null) {
            return false;
        }
        return !effectiveDate.isAfter(LocalDate.now());
    }

    /**
     * Check if this transaction is effective on a specific date
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (effectiveDate == null || date == null) {
            return false;
        }
        return !effectiveDate.isAfter(date);
    }

    /**
     * Calculate amount based on percentage and base salary
     */
    public static BigDecimal calculateAmount(BigDecimal baseSalary, BigDecimal percentage) {
        if (baseSalary == null || percentage == null) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(percentage)
                .setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
