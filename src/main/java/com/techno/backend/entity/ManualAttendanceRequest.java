package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing a manual attendance request submitted by an employee.
 *
 * Handles manual attendance requests with multi-level approval workflow.
 * When approved, creates an actual AttendanceTransaction record.
 *
 * Approval Flow:
 * - Uses the same approval workflow system as leave/loan requests
 * - Request type: "MANUAL_ATTENDANCE"
 * - Configured in requests_approval_set table
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Entity
@Table(name = "manual_attendance_requests", indexes = {
    @Index(name = "idx_manual_attendance_emp", columnList = "employee_no"),
    @Index(name = "idx_manual_attendance_date", columnList = "attendance_date"),
    @Index(name = "idx_manual_attendance_status", columnList = "trans_status"),
    @Index(name = "idx_manual_attendance_approver", columnList = "next_approval")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"employee"})
@ToString(exclude = {"employee"})
@EntityListeners(AuditingEntityListener.class)
public class ManualAttendanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @NotNull(message = "تاريخ الحضور مطلوب")
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @NotNull(message = "وقت الدخول مطلوب")
    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @NotNull(message = "وقت الخروج مطلوب")
    @Column(name = "exit_time", nullable = false)
    private LocalTime exitTime;

    @NotBlank(message = "السبب مطلوب")
    @Size(max = 500, message = "السبب لا يجب أن يتجاوز 500 حرف")
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

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
     * Current approval level
     */
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Employee number who submitted the request
     */
    @Column(name = "requested_by")
    private Long requestedBy;

    /**
     * Employee number who gave final approval
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Size(max = 500, message = "سبب الرفض لا يجب أن يتجاوز 500 حرف")
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

    /**
     * Approve request and move to next level
     */
    public void approve(Long approverNo, Integer nextLevel, Long nextApproverNo) {
        if (nextLevel == null) {
            // Final approval
            this.transStatus = "A";
            this.approvedBy = approverNo;
            this.approvedDate = LocalDateTime.now();
            this.nextApproval = null;
            this.nextAppLevel = null;
        } else {
            // Intermediate approval
            this.nextAppLevel = nextLevel;
            this.nextApproval = nextApproverNo;
        }
    }

    /**
     * Reject request
     */
    public void reject(Long approverNo, String reason) {
        this.transStatus = "R";
        this.approvedBy = approverNo;
        this.approvedDate = LocalDateTime.now();
        this.rejectionReason = reason;
        this.nextApproval = null;
        this.nextAppLevel = null;
    }

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

