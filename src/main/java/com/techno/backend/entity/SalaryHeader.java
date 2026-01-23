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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing monthly salary calculation header for an employee.
 *
 * This is the master record for each employee's monthly payroll.
 * Contains summary totals (gross, allowances, deductions, net).
 * Detailed breakdown stored in SalaryDetail.
 *
 * Features:
 * - Versioning support (can recalculate salary multiple times)
 * - 3-level approval workflow
 * - Tracks calculation history
 * - Supports both regular (W=Work) and final settlement (F=Final) salaries
 *
 * Calculation Process (8 steps per specification):
 * 1. Pro-rate basic salary for hire/termination dates
 * 2. Breakdown basic salary by nationality percentages
 * 3. Sum fixed + variable allowances
 * 4. Add overtime allowances from attendance
 * 5. Sum fixed + variable deductions
 * 6. Add late/absence deductions from attendance
 * 7. Add loan installment deductions
 * 8. Calculate net salary
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@Entity
@Table(name = "salary_header", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_no", "salary_month", "salary_version" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = { "salaryDetails" })
@ToString(exclude = { "salaryDetails" })
@EntityListeners(AuditingEntityListener.class)
public class SalaryHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private Long salaryId;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Salary month in format YYYY-MM (e.g., "2025-11")
     */
    @NotNull(message = "شهر الراتب مطلوب")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "شهر الراتب يجب أن يكون بالتنسيق YYYY-MM")
    @Column(name = "salary_month", length = 7, nullable = false)
    private String salaryMonth;

    /**
     * Salary version number (for recalculations)
     * Starts at 1, increments with each recalculation
     */
    @NotNull(message = "إصدار الراتب مطلوب")
    @Positive(message = "إصدار الراتب يجب أن يكون موجباً")
    @Column(name = "salary_version")
    @Builder.Default
    private Integer salaryVersion = 1;

    /**
     * Latest version flag:
     * Y = This is the latest/current version
     * N = Older version (superseded by recalculation)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الإصدار الأحدث يجب أن تكون Y أو N")
    @Column(name = "is_latest", length = 1)
    @Builder.Default
    private String isLatest = "Y";

    /**
     * Gross salary (basic salary before any deductions)
     * This is the pro-rated basic salary
     */
    @NotNull(message = "الراتب الإجمالي مطلوب")
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 4)
    private BigDecimal grossSalary;

    /**
     * Total of all allowances (fixed + variable + overtime)
     */
    @Column(name = "total_allowances", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    /**
     * Total of all deductions (fixed + variable + late + loan)
     */
    @Column(name = "total_deductions", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    /**
     * Total Overtime Amount (Component of Allowances)
     */
    @Column(name = "total_overtime", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalOvertime = BigDecimal.ZERO;

    /**
     * Total Absence/Late Deductions (Component of Deductions)
     */
    @Column(name = "total_absence", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalAbsence = BigDecimal.ZERO;

    /**
     * Total Loan Deductions (Component of Deductions)
     */
    @Column(name = "total_loans", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalLoans = BigDecimal.ZERO;

    /**
     * Net salary = gross + allowances - deductions
     * This is the final amount paid to employee
     */
    @NotNull(message = "الراتب الصافي مطلوب")
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 4)
    private BigDecimal netSalary;

    /**
     * Salary type:
     * W = Regular work salary
     * F = Final settlement (termination)
     */
    @Pattern(regexp = "^[WF]$", message = "نوع الراتب يجب أن يكون W أو F")
    @Column(name = "salary_type", length = 1)
    @Builder.Default
    private String salaryType = "W";

    @Column(name = "calculation_date")
    private LocalDate calculationDate;

    /**
     * Transaction status:
     * N = New (pending approval)
     * A = Approved (payroll can be processed)
     * R = Rejected
     */
    @Pattern(regexp = "^[NAR]$", message = "الحالة يجب أن تكون N أو A أو R")
    @Column(name = "trans_status", length = 1)
    @Builder.Default
    private String transStatus = "N";

    /**
     * For 3-level payroll approval workflow
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    /**
     * For recalculation tracking
     */
    @Column(name = "recalculated_by")
    private Long recalculatedBy;

    @Column(name = "recalculated_date")
    private LocalDateTime recalculatedDate;

    @Column(name = "recalculation_reason", length = 500)
    private String recalculationReason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "calculated_by")
    private Long calculatedBy;

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
    @JoinColumn(name = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    @OneToMany(mappedBy = "salaryHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<SalaryDetail> salaryDetails = new ArrayList<>();

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

    public boolean isLatestVersion() {
        return "Y".equals(this.isLatest);
    }

    public boolean isFinalSettlement() {
        return "F".equals(this.salaryType);
    }

    public boolean isRegularSalary() {
        return "W".equals(this.salaryType);
    }

    /**
     * Add a salary detail line
     */
    public void addDetail(SalaryDetail detail) {
        if (salaryDetails == null) {
            salaryDetails = new ArrayList<>();
        }
        salaryDetails.add(detail);
        detail.setSalaryHeader(this);
    }

    /**
     * Recalculate totals from detail lines
     */
    public void recalculateTotals() {
        // 1. Calculate Allowances
        this.totalAllowances = salaryDetails.stream()
                .filter(d -> "A".equals(d.getTransCategory()))
                .map(SalaryDetail::getTransAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Overtime (Type Code 9)
        this.totalOvertime = salaryDetails.stream()
                .filter(d -> "A".equals(d.getTransCategory()) && Long.valueOf(9).equals(d.getTransTypeCode()))
                .map(SalaryDetail::getTransAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calculate Deductions
        this.totalDeductions = salaryDetails.stream()
                .filter(d -> "D".equals(d.getTransCategory()))
                .map(SalaryDetail::getTransAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Absence/Late (Type Codes 20-24)
        // 20=Late, 21=Absent, 22=EarlyOut, 23=ShortHours, 24=UnpaidLeave
        this.totalAbsence = salaryDetails.stream()
                .filter(d -> "D".equals(d.getTransCategory()) &&
                        (Long.valueOf(20).equals(d.getTransTypeCode()) ||
                                Long.valueOf(21).equals(d.getTransTypeCode()) ||
                                Long.valueOf(22).equals(d.getTransTypeCode()) ||
                                Long.valueOf(23).equals(d.getTransTypeCode()) ||
                                Long.valueOf(24).equals(d.getTransTypeCode())))
                .map(SalaryDetail::getTransAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Loans (Type Code 30)
        this.totalLoans = salaryDetails.stream()
                .filter(d -> "D".equals(d.getTransCategory()) && Long.valueOf(30).equals(d.getTransTypeCode()))
                .map(SalaryDetail::getTransAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net = (Earnings) - (Deductions)
        // Note: Gross Salary breakdown is already included in totalAllowances
        this.netSalary = this.totalAllowances
                .subtract(this.totalDeductions);
    }
}
