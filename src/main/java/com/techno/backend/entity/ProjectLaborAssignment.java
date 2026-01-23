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
 * Entity representing an actual assignment of a worker to a project.
 * Maps to PROJECT_LABOR_ASSIGNMENTS table in database.
 *
 * Tracks:
 * - Which employee is assigned
 * - To which project
 * - For which labor request
 * - Start and end dates
 * - Daily rate
 * - Total cost calculation
 *
 * Business Rules:
 * - No overlapping assignments for the same employee
 * - Assignment dates must be within project dates
 * - End date must be >= start date
 *
 * Example:
 * - Employee #500 (Carpenter)
 * - Assigned to Project: Kempinski Hotel
 * - From: 2024-01-15 to 2024-03-15 (60 days)
 * - Daily Rate: SAR 150
 * - Total Cost: SAR 9,000
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_labor_assignments", indexes = {
    @Index(name = "idx_labor_assign_employee", columnList = "employee_no"),
    @Index(name = "idx_labor_assign_project", columnList = "project_code"),
    @Index(name = "idx_labor_assign_request", columnList = "request_no"),
    @Index(name = "idx_labor_assign_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_labor_assign_status", columnList = "assignment_status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLaborAssignment extends BaseEntity {

    /**
     * Assignment ID - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_no")
    private Long assignmentNo;

    /**
     * Labor request number - Foreign Key
     * Optional - can be null for direct assignments not linked to a labor request
     */
    @Column(name = "request_no")
    private Long requestNo;

    /**
     * Employee number (daily/temporary worker) - Foreign Key
     */
    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Project code - Foreign Key
     */
    @NotNull(message = "رمز المشروع مطلوب")
    @Column(name = "project_code", nullable = false)
    private Long projectCode;

    /**
     * Assignment start date
     */
    @NotNull(message = "تاريخ البدء مطلوب")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Assignment end date (planned or actual)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Daily rate in SAR
     */
    @NotNull(message = "المعدل اليومي مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المعدل اليومي أكبر من 0")
    @Digits(integer = 8, fraction = 2, message = "تنسيق المعدل اليومي غير صالح")
    @Column(name = "daily_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal dailyRate;

    /**
     * Assignment status
     * ACTIVE = Currently active
     * COMPLETED = Assignment ended
     * CANCELLED = Assignment cancelled
     */
    @NotBlank(message = "حالة التعيين مطلوبة")
    @Pattern(regexp = "^(ACTIVE|COMPLETED|CANCELLED)$",
             message = "الحالة يجب أن تكون ACTIVE أو COMPLETED أو CANCELLED")
    @Column(name = "assignment_status", nullable = false, length = 20)
    @Builder.Default
    private String assignmentStatus = "ACTIVE";

    /**
     * Assigned by (HR employee number)
     */
    @Column(name = "assigned_by")
    private Long assignedBy;

    /**
     * Assignment notes
     */
    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    @Column(name = "assignment_notes", length = 500)
    private String assignmentNotes;

    /**
     * Actual end date (if ended early)
     */
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    /**
     * Soft delete flag
     * Y = Deleted
     * N = Active
     */
    @NotNull(message = "حالة الحذف مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون Y أو N")
    @Column(name = "is_deleted", nullable = false, length = 1)
    @Builder.Default
    private String isDeleted = "N";

    // Relationships

    /**
     * Reference to employee
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", referencedColumnName = "employee_no",
                insertable = false, updatable = false)
    private Employee employee;

    /**
     * Reference to project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", referencedColumnName = "project_code",
                insertable = false, updatable = false)
    private Project project;

    /**
     * Reference to labor request header
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_no", referencedColumnName = "request_no",
                insertable = false, updatable = false)
    private ProjectLaborRequestHeader laborRequest;

    // Helper methods

    /**
     * Check if assignment is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.assignmentStatus);
    }

    /**
     * Check if assignment is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(this.assignmentStatus);
    }

    /**
     * Check if assignment is cancelled
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(this.assignmentStatus);
    }

    /**
     * Check if assignment is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Calculate total working days
     */
    public long getTotalDays() {
        LocalDate effectiveEndDate = actualEndDate != null ? actualEndDate :
                                     endDate != null ? endDate : LocalDate.now();

        if (startDate == null) {
            return 0L;
        }

        // If end date is before start date, return 0
        if (effectiveEndDate.isBefore(startDate)) {
            return 0L;
        }

        return java.time.temporal.ChronoUnit.DAYS.between(startDate, effectiveEndDate) + 1;
    }

    /**
     * Calculate total cost (days * daily rate)
     */
    public BigDecimal getTotalCost() {
        if (dailyRate == null) {
            return BigDecimal.ZERO;
        }
        long days = getTotalDays();
        return dailyRate.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Check if assignment is currently ongoing
     */
    public boolean isOngoing() {
        if (!isActive()) {
            return false;
        }

        LocalDate now = LocalDate.now();

        // Must have started
        if (startDate == null || startDate.isAfter(now)) {
            return false;
        }

        // Must not have ended
        if (endDate != null && endDate.isBefore(now)) {
            return false;
        }

        return true;
    }

    /**
     * Check if dates overlap with another assignment
     */
    public boolean overlapsWith(LocalDate otherStart, LocalDate otherEnd) {
        if (startDate == null || otherStart == null) {
            return false;
        }

        LocalDate thisEnd = endDate != null ? endDate : LocalDate.MAX;
        LocalDate thatEnd = otherEnd != null ? otherEnd : LocalDate.MAX;

        // Check for overlap: (start1 <= end2) AND (start2 <= end1)
        return !startDate.isAfter(thatEnd) && !otherStart.isAfter(thisEnd);
    }

    /**
     * End the assignment
     */
    public void endAssignment(LocalDate endingDate) {
        this.actualEndDate = endingDate;
        this.assignmentStatus = "COMPLETED";
    }

    /**
     * Cancel the assignment
     */
    public void cancelAssignment() {
        this.assignmentStatus = "CANCELLED";
        this.actualEndDate = LocalDate.now();
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.isDeleted = "Y";
    }

    /**
     * Validate assignment dates
     */
    public boolean hasValidDates() {
        if (startDate == null) {
            return false;
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            return false;
        }
        return true;
    }
}
