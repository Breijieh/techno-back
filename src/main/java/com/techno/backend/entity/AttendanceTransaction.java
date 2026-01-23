package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing employee attendance transactions (check-in/check-out records).
 * Stores GPS coordinates, timestamps, and automatically calculated hours.
 * One record per employee per day with unique constraint on (employee_no, attendance_date).
 *
 * @author Techno ERP Team
 * @version 1.0
 */
@Entity
@Table(name = "emp_attendance_transactions", indexes = {
    @Index(name = "idx_attendance_emp", columnList = "employee_no"),
    @Index(name = "idx_attendance_date", columnList = "attendance_date"),
    @Index(name = "idx_attendance_emp_date", columnList = "employee_no, attendance_date", unique = true),
    @Index(name = "idx_attendance_project", columnList = "project_code")
})
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"employee", "project"})
@ToString(exclude = {"employee", "project"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @NotNull(message = "تاريخ الحضور مطلوب")
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "project_code")
    private Long projectCode;

    // ===== Check-in details =====
    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "entry_latitude", precision = 10, scale = 8)
    private BigDecimal entryLatitude;

    @Column(name = "entry_longitude", precision = 11, scale = 8)
    private BigDecimal entryLongitude;

    @Column(name = "entry_distance_meters")
    private Double entryDistanceMeters; // Calculated distance from project location

    // ===== Check-out details =====
    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "exit_latitude", precision = 10, scale = 8)
    private BigDecimal exitLatitude;

    @Column(name = "exit_longitude", precision = 11, scale = 8)
    private BigDecimal exitLongitude;

    @Column(name = "exit_distance_meters")
    private Double exitDistanceMeters; // Calculated distance from project location

    // ===== Scheduled and calculated hours =====
    @Column(name = "scheduled_hours", precision = 5, scale = 2)
    private BigDecimal scheduledHours;

    @Column(name = "working_hours", precision = 5, scale = 2)
    private BigDecimal workingHours; // Actual hours worked (exit - entry)

    @Column(name = "overtime_calc", precision = 5, scale = 2)
    private BigDecimal overtimeCalc; // Overtime hours × 1.5

    @Column(name = "delayed_calc", precision = 5, scale = 2)
    private BigDecimal delayedCalc; // Late arrival hours

    @Column(name = "early_out_calc", precision = 5, scale = 2)
    private BigDecimal earlyOutCalc; // Early departure hours

    @Column(name = "shortage_hours", precision = 5, scale = 2)
    private BigDecimal shortageHours; // Scheduled - working

    // ===== Flags =====
    @Pattern(regexp = "^[YN]$", message = "علامة الغياب يجب أن تكون Y أو N")
    @Column(name = "absence_flag", length = 1)
    @Builder.Default
    private String absenceFlag = "N";

    @Column(name = "absence_reason", length = 500)
    private String absenceReason;

    @Column(name = "absence_approved_by")
    private Long absenceApprovedBy;

    @Column(name = "absence_approved_date")
    private LocalDateTime absenceApprovedDate;

    @Pattern(regexp = "^[YN]$", message = "علامة الخروج التلقائي يجب أن تكون Y أو N")
    @Column(name = "is_auto_checkout", length = 1)
    @Builder.Default
    private String isAutoCheckout = "N";

    @Pattern(regexp = "^[YN]$", message = "علامة العمل في العطلة يجب أن تكون Y أو N")
    @Column(name = "is_holiday_work", length = 1)
    @Builder.Default
    private String isHolidayWork = "N";

    @Pattern(regexp = "^[YN]$", message = "علامة العمل في نهاية الأسبوع يجب أن تكون Y أو N")
    @Column(name = "is_weekend_work", length = 1)
    @Builder.Default
    private String isWeekendWork = "N";

    @Pattern(regexp = "^[YN]$", message = "علامة الإدخال اليدوي يجب أن تكون Y أو N")
    @Column(name = "is_manual_entry", length = 1)
    @Builder.Default
    private String isManualEntry = "N";

    @Column(name = "notes", length = 1000)
    private String notes;

    // ===== Relationships (lazy loading) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", insertable = false, updatable = false)
    private Project project;

    // ===== Helper Methods =====

    /**
     * Check if employee is marked as absent
     *
     * @return true if absent
     */
    public boolean isAbsent() {
        return "Y".equals(this.absenceFlag);
    }

    /**
     * Check if employee has checked in
     *
     * @return true if entry time is recorded
     */
    public boolean hasCheckedIn() {
        return entryTime != null;
    }

    /**
     * Check if employee has checked out
     *
     * @return true if exit time is recorded
     */
    public boolean hasCheckedOut() {
        return exitTime != null;
    }

    /**
     * Check if attendance record is complete (both check-in and check-out)
     *
     * @return true if both entry and exit times are recorded
     */
    public boolean isComplete() {
        return hasCheckedIn() && hasCheckedOut();
    }

    /**
     * Check if this was auto-checkout
     *
     * @return true if auto-checkout
     */
    public boolean isAutoCheckout() {
        return "Y".equals(this.isAutoCheckout);
    }

    /**
     * Check if this is holiday work
     *
     * @return true if holiday work
     */
    public boolean isHolidayWork() {
        return "Y".equals(this.isHolidayWork);
    }

    /**
     * Check if this is weekend work
     *
     * @return true if weekend work
     */
    public boolean isWeekendWork() {
        return "Y".equals(this.isWeekendWork);
    }

    /**
     * Check if this is a manual entry (HR created)
     *
     * @return true if manual entry
     */
    public boolean isManualEntry() {
        return "Y".equals(this.isManualEntry);
    }

    /**
     * Check if GPS coordinates are recorded for entry
     *
     * @return true if entry GPS is present
     */
    public boolean hasEntryGPS() {
        return entryLatitude != null && entryLongitude != null;
    }

    /**
     * Check if GPS coordinates are recorded for exit
     *
     * @return true if exit GPS is present
     */
    public boolean hasExitGPS() {
        return exitLatitude != null && exitLongitude != null;
    }
}
