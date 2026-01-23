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

/**
 * Entity representing monthly allowances for employees.
 *
 * Allowances can be:
 * - Fixed (permanent part of salary structure)
 * - Variable (one-time or periodical)
 * - Auto-generated (from attendance overtime)
 * - Manual entry (by HR/admin)
 *
 * Examples:
 * - Housing allowance (fixed)
 * - Transportation allowance (fixed)
 * - Overtime allowance (auto-generated from attendance)
 * - Performance bonus (manual, one-time)
 * - Salary raise (requires 4-level approval)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@Entity
@Table(name = "emp_monthly_allowances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class EmpMonthlyAllowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_no")
    private Long transactionNo;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Transaction type code (references TRANSACTION_TYPES table)
     * Example codes:
     * - 1: Housing Allowance
     * - 2: Transportation Allowance
     * - 3: Overtime Allowance
     * - 10: Salary Raise (requires approval)
     */
    @NotNull(message = "رمز النوع مطلوب")
    @Column(name = "type_code", nullable = false)
    private Long typeCode;

    @NotNull(message = "مبلغ البدل مطلوب")
    @Positive(message = "مبلغ البدل يجب أن يكون رقماً موجباً")
    @Column(name = "allowance_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal allowanceAmount;

    /**
     * Start date for periodical allowances
     * For one-time allowances, this can be null
     */
    @Column(name = "allowance_start_date")
    private LocalDate allowanceStartDate;

    /**
     * End date for periodical allowances
     * For permanent allowances, this is null
     */
    @Column(name = "allowance_end_date")
    private LocalDate allowanceEndDate;

    /**
     * Transaction status:
     * N = New (pending approval for raises)
     * A = Approved/Active
     * R = Rejected
     */
    @Pattern(regexp = "^[NAR]$", message = "الحالة يجب أن تكون N أو A أو R")
    @Column(name = "trans_status", length = 1)
    @Builder.Default
    private String transStatus = "A";

    /**
     * Overtime hours (for overtime allowance type)
     * Populated from attendance system
     */
    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    /**
     * Periodical allowance flag:
     * Y = Recurring (applied every month)
     * N = One-time
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الدورية يجب أن تكون Y أو N")
    @Column(name = "periodical_allowance", length = 1)
    @Builder.Default
    private String periodicalAllowance = "N";

    /**
     * Manual entry flag:
     * Y = Manually entered by HR/admin
     * N = Auto-generated (from attendance/system)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الإدخال اليدوي يجب أن تكون Y أو N")
    @Column(name = "is_manual_entry", length = 1)
    @Builder.Default
    private String isManualEntry = "N";

    @Column(name = "entry_reason", length = 500)
    private String entryReason;

    /**
     * Deletion flag (soft delete):
     * Y = Deleted
     * N = Active
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون Y أو N")
    @Column(name = "is_deleted", length = 1)
    @Builder.Default
    private String isDeleted = "N";

    /**
     * For approval workflow (salary raises only)
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
     * Rejection reason if request was rejected
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

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

    public boolean isActive() {
        return "N".equals(this.isDeleted);
    }

    public boolean isPeriodical() {
        return "Y".equals(this.periodicalAllowance);
    }

    public boolean isManual() {
        return "Y".equals(this.isManualEntry);
    }

    /**
     * Check if allowance is active for a specific date
     */
    public boolean isActiveOn(LocalDate date) {
        if ("Y".equals(this.isDeleted)) {
            return false;
        }

        if (allowanceStartDate != null && date.isBefore(allowanceStartDate)) {
            return false;
        }

        if (allowanceEndDate != null && date.isAfter(allowanceEndDate)) {
            return false;
        }

        return true;
    }

    // Auto-generated Lombok methods
    public Long getTransactionNo() { return transactionNo; }
    public Long getEmployeeNo() { return employeeNo; }
    public Employee getEmployee() { return employee; }
    public Long getTypeCode() { return typeCode; }
    public TransactionType getTransactionType() { return transactionType; }
    public java.time.LocalDate getTransactionDate() { return transactionDate; }
    public java.math.BigDecimal getAllowanceAmount() { return allowanceAmount; }
    public String getEntryReason() { return entryReason; }
    public String getTransStatus() { return transStatus; }
    public String getIsManualEntry() { return isManualEntry; }
    public java.time.LocalDateTime getApprovedDate() { return approvedDate; }
    public Long getApprovedBy() { return approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getNextApproval() { return nextApproval; }
    public Integer getNextAppLevel() { return nextAppLevel; }
    public String getIsDeleted() { return isDeleted; }

    // Auto-generated Setters
    public void setTransactionNo(Long transactionNo) { this.transactionNo = transactionNo; }
    public void setEmployeeNo(Long employeeNo) { this.employeeNo = employeeNo; }
    public void setTypeCode(Long typeCode) { this.typeCode = typeCode; }
    public void setTransactionDate(java.time.LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setAllowanceAmount(java.math.BigDecimal allowanceAmount) { this.allowanceAmount = allowanceAmount; }
    public void setEntryReason(String entryReason) { this.entryReason = entryReason; }
    public void setTransStatus(String transStatus) { this.transStatus = transStatus; }
    public void setIsManualEntry(String isManualEntry) { this.isManualEntry = isManualEntry; }
    public void setApprovedDate(java.time.LocalDateTime approvedDate) { this.approvedDate = approvedDate; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setNextApproval(Long nextApproval) { this.nextApproval = nextApproval; }
    public void setNextAppLevel(Integer nextAppLevel) { this.nextAppLevel = nextAppLevel; }
    public void setIsDeleted(String isDeleted) { this.isDeleted = isDeleted; }
    public void setOvertimeHours(java.math.BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public void setPeriodicalAllowance(String periodicalAllowance) { this.periodicalAllowance = periodicalAllowance; }
    public void setAllowanceStartDate(java.time.LocalDate allowanceStartDate) { this.allowanceStartDate = allowanceStartDate; }
    public void setAllowanceEndDate(java.time.LocalDate allowanceEndDate) { this.allowanceEndDate = allowanceEndDate; }
}
