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
 * Entity representing an employee leave request.
 *
 * Handles vacation/leave requests with multi-level approval workflow.
 * Leave balance is deducted from employee after final approval.
 *
 * Approval Flow:
 * - Level 1: Direct Manager
 * - Level 2: Project Manager
 * - Level 3: HR Manager (final)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Leave Management
 */
@Entity
@Table(name = "employee_leaves")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class EmployeeLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_id")
    private Long leaveId;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @NotNull(message = "تاريخ بداية الإجازة مطلوب")
    @Column(name = "leave_from_date", nullable = false)
    private LocalDate leaveFromDate;

    @NotNull(message = "تاريخ نهاية الإجازة مطلوب")
    @Column(name = "leave_to_date", nullable = false)
    private LocalDate leaveToDate;

    @NotNull(message = "أيام الإجازة مطلوبة")
    @Positive(message = "أيام الإجازة يجب أن تكون موجبة")
    @Column(name = "leave_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal leaveDays;

    @Column(name = "leave_reason", length = 500)
    private String leaveReason;

    @Column(name = "request_date")
    private LocalDate requestDate;

    /**
     * Transaction status:
     * N = New (pending approval)
     * A = Approved
     * R = Rejected
     */
    @Pattern(regexp = "^[NAR]$", message = "الحالة يجب أن تكون N أو A أو R")
    @Column(name = "trans_status", length = 1)
    @Builder.Default
    private String transStatus = "N";

    /**
     * Employee number of next approver in the approval chain
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    /**
     * Current approval level (1 = Direct Manager, 2 = Project Manager, 3 = HR
     * Manager)
     */
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Descriptive name of the next approval level (e.g., Direct Manager)
     */
    @Column(name = "next_app_level_name", length = 100)
    private String nextAppLevelName;

    /**
     * Employee number who gave final approval
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

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
}
