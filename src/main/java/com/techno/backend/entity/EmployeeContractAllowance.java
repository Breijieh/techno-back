package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Employee Contract Allowance Entity
 * Represents employee-specific salary breakdown percentages
 * Allows per-employee customization of allowance percentages that can override category-based defaults
 */
@Entity
@Table(name = "employee_contract_allowances", indexes = {
    @Index(name = "idx_emp_contract_allowance_employee", columnList = "employee_no"),
    @Index(name = "idx_emp_contract_allowance_type", columnList = "trans_type_code"),
    @Index(name = "idx_emp_contract_allowance_active", columnList = "is_active"),
    @Index(name = "idx_emp_contract_allowance_created_date", columnList = "created_date")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_emp_contract_allowance_emp_type", columnNames = {"employee_no", "trans_type_code"})
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeContractAllowance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    /**
     * Employee number
     * References employees_details table
     */
    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Transaction type code
     * References transactions_types table
     * Should be an allowance type (A)
     */
    @NotNull(message = "رمز نوع المعاملة مطلوب")
    @Column(name = "trans_type_code", nullable = false)
    private Long transTypeCode;

    /**
     * Salary percentage (0-100)
     * Example: 40.00 = 40%, 30.50 = 30.5%
     */
    @NotNull(message = "نسبة الراتب مطلوبة")
    @DecimalMin(value = "0.0", message = "النسبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "100.0", message = "النسبة لا يمكن أن تتجاوز 100")
    @Column(name = "salary_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal salaryPercentage;

    /**
     * Active status flag
     * Y = Active
     * N = Inactive (soft deleted)
     */
    @NotNull(message = "الحالة النشطة مطلوبة")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private Character isActive = 'Y';

    // Relationships

    /**
     * Reference to employee
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", referencedColumnName = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    /**
     * Reference to transaction type
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_type_code", referencedColumnName = "type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

    // Helper methods

    /**
     * Check if this allowance is active
     */
    public boolean isActive() {
        return 'Y' == this.isActive;
    }

    /**
     * Activate this allowance
     */
    public void activate() {
        this.isActive = 'Y';
    }

    /**
     * Deactivate this allowance
     */
    public void deactivate() {
        this.isActive = 'N';
    }
}

