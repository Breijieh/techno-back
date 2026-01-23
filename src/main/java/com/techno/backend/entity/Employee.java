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
 * Entity representing an employee in the Techno ERP system.
 * Maps to EMPLOYEES_DETAILS table in database.
 *
 * Supports both Saudi and Foreign employees with different contract types.
 * Tracks employment status, salary, leave balance, and document expiry dates.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "employees_details", indexes = {
        @Index(name = "idx_employee_national_id", columnList = "national_id", unique = true),
        @Index(name = "idx_employee_contract_type", columnList = "emp_contract_type"),
        @Index(name = "idx_employee_status", columnList = "employment_status"),
        @Index(name = "idx_employee_dept", columnList = "primary_dept_code"),
        @Index(name = "idx_employee_project", columnList = "primary_project_code"),
        @Index(name = "idx_employee_category", columnList = "employee_category")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    /**
     * Unique employee number - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_no")
    private Long employeeNo;

    /**
     * Employee full name
     */
    @NotBlank(message = "اسم الموظف مطلوب")
    @Size(max = 250, message = "اسم الموظف لا يجب أن يتجاوز 250 حرفاً")
    @Column(name = "employee_name", nullable = false, length = 250)
    private String employeeName;

    /**
     * National ID or Iqama number - Unique identifier
     */
    @NotBlank(message = "رقم الهوية الوطنية مطلوب")
    @Size(max = 20, message = "رقم الهوية الوطنية لا يجب أن يتجاوز 20 حرفاً")
    @Column(name = "national_id", nullable = false, unique = true, length = 20)
    private String nationalId;

    /**
     * Employee nationality
     */
    @NotBlank(message = "الجنسية مطلوبة")
    @Size(max = 50, message = "الجنسية لا يجب أن تتجاوز 50 حرفاً")
    @Column(name = "nationality", nullable = false, length = 50)
    private String nationality;

    /**
     * Employee category: S = Saudi, F = Foreign
     * This determines salary breakdown percentages
     */
    @NotNull(message = "فئة الموظف مطلوبة")
    @Pattern(regexp = "^[SF]$", message = "فئة الموظف يجب أن تكون 'S' (سعودي) أو 'F' (أجنبي)")
    @Column(name = "employee_category", nullable = false, length = 1)
    private String employeeCategory;

    /**
     * Passport number (required for foreign employees)
     */
    @Size(max = 50, message = "رقم جواز السفر لا يجب أن يتجاوز 50 حرفاً")
    @Column(name = "passport_no", length = 50)
    private String passportNo;

    /**
     * Passport expiry date (tracked for alerts)
     */
    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    /**
     * Residency/Iqama number (for foreign employees)
     */
    @Size(max = 50, message = "رقم الإقامة لا يجب أن يتجاوز 50 حرفاً")
    @Column(name = "residency_no", length = 50)
    private String residencyNo;

    /**
     * Residency expiry date (tracked for alerts)
     */
    @Column(name = "residency_expiry_date")
    private LocalDate residencyExpiryDate;

    /**
     * Employee hire/joining date
     */
    @NotNull(message = "تاريخ التوظيف مطلوب")
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    /**
     * Termination date (if employee is terminated)
     */
    @Column(name = "termination_date")
    private LocalDate terminationDate;

    /**
     * Employment status: ACTIVE, TERMINATED, ON_LEAVE, SUSPENDED
     */
    @NotBlank(message = "حالة التوظيف مطلوبة")
    @Size(max = 20, message = "حالة التوظيف لا يجب أن تتجاوز 20 حرفاً")
    @Column(name = "employment_status", nullable = false, length = 20)
    @Builder.Default
    private String employmentStatus = "ACTIVE";

    /**
     * Reason for termination (if applicable)
     */
    @Size(max = 500, message = "سبب الإنهاء لا يجب أن يتجاوز 500 حرف")
    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    /**
     * Contract type: TECHNO, CLIENT, CONTRACTOR
     * References CONTRACT_TYPES table
     */
    @NotBlank(message = "نوع العقد مطلوب")
    @Size(max = 20, message = "نوع العقد لا يجب أن يتجاوز 20 حرفاً")
    @Column(name = "emp_contract_type", nullable = false, length = 20)
    private String empContractType;

    /**
     * Primary department code
     * Foreign key to DEPARTMENTS table
     */
    @Column(name = "primary_dept_code")
    private Long primaryDeptCode;

    /**
     * Primary project code
     * Foreign key to PROJECTS table
     */
    @Column(name = "primary_project_code")
    private Long primaryProjectCode;

    /**
     * Monthly gross salary in SAR
     * This is the base salary before breakdown into allowances
     */
    @NotNull(message = "الراتب الشهري مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "الراتب يجب أن يكون أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق الراتب غير صالح")
    @Column(name = "monthly_salary", nullable = false, precision = 12, scale = 4)
    private BigDecimal monthlySalary;

    /**
     * Remaining annual leave balance in days
     * Can have fractional days (e.g., 15.5 days)
     */
    @DecimalMin(value = "0.0", message = "رصيد الإجازة لا يمكن أن يكون سالباً")
    @Digits(integer = 5, fraction = 2, message = "تنسيق رصيد الإجازة غير صالح")
    @Column(name = "leave_balance_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal leaveBalanceDays = BigDecimal.valueOf(30.0);

    /**
     * Employee email address
     */
    @Email(message = "تنسيق البريد الإلكتروني غير صالح")
    @Size(max = 100, message = "البريد الإلكتروني لا يجب أن يتجاوز 100 حرف")
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Employee mobile phone number
     */
    @Size(max = 20, message = "رقم الجوال لا يجب أن يتجاوز 20 حرفاً")
    @Column(name = "mobile", length = 20)
    private String mobile;

    // Relationships

    /**
     * Reference to primary department
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_dept_code", referencedColumnName = "dept_code", insertable = false, updatable = false)
    private Department primaryDepartment;

    /**
     * Reference to contract type
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_contract_type", referencedColumnName = "contract_type_code", insertable = false, updatable = false)
    private ContractType contractType;

    // Helper methods

    /**
     * Check if employee is Saudi
     */
    public boolean isSaudi() {
        return "S".equals(this.employeeCategory);
    }

    /**
     * Check if employee is Foreign
     */
    public boolean isForeign() {
        return "F".equals(this.employeeCategory);
    }

    /**
     * Check if employee is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.employmentStatus);
    }

    /**
     * Check if employee is terminated
     */
    public boolean isTerminated() {
        return "TERMINATED".equals(this.employmentStatus);
    }

    /**
     * Check if passport is expiring soon (within days threshold)
     */
    public boolean isPassportExpiringSoon(int daysThreshold) {
        if (passportExpiryDate == null) {
            return false;
        }
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return passportExpiryDate.isBefore(threshold) || passportExpiryDate.isEqual(threshold);
    }

    /**
     * Check if residency is expiring soon (within days threshold)
     */
    public boolean isResidencyExpiringSoon(int daysThreshold) {
        if (residencyExpiryDate == null) {
            return false;
        }
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return residencyExpiryDate.isBefore(threshold) || residencyExpiryDate.isEqual(threshold);
    }

    /**
     * Calculate days until passport expires
     */
    public Long getDaysUntilPassportExpiry() {
        if (passportExpiryDate == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), passportExpiryDate);
    }

    /**
     * Calculate days until residency expires
     */
    public Long getDaysUntilResidencyExpiry() {
        if (residencyExpiryDate == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), residencyExpiryDate);
    }

    /**
     * Calculate years of service
     */
    public Long getYearsOfService() {
        LocalDate endDate = terminationDate != null ? terminationDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.YEARS.between(hireDate, endDate);
    }

    /**
     * Calculate months of service
     */
    public Long getMonthsOfService() {
        LocalDate endDate = terminationDate != null ? terminationDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.MONTHS.between(hireDate, endDate);
    }

    // Auto-generated Lombok methods
    public Long getPrimaryDeptCode() { return primaryDeptCode; }
    public Long getPrimaryProjectCode() { return primaryProjectCode; }

    // Auto-generated Setters
    public void setPrimaryDeptCode(Long primaryDeptCode) { this.primaryDeptCode = primaryDeptCode; }
    public void setPrimaryProjectCode(Long primaryProjectCode) { this.primaryProjectCode = primaryProjectCode; }
}
