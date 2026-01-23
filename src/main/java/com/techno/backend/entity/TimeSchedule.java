package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Entity representing work time schedules.
 * Can be assigned to specific departments, projects, or used as default.
 * Priority: Project-specific > Department-specific > Default
 *
 * @author Techno ERP Team
 * @version 1.0
 */
@Entity
@Table(name = "time_schedule", indexes = {
    @Index(name = "idx_schedule_dept", columnList = "department_code"),
    @Index(name = "idx_schedule_project", columnList = "project_code")
})
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"department", "project"})
@ToString(exclude = {"department", "project"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @NotBlank(message = "اسم الجدول مطلوب")
    @Column(name = "schedule_name", nullable = false, length = 100)
    private String scheduleName;

    @Column(name = "department_code")
    private Long departmentCode;

    @Column(name = "project_code")
    private Long projectCode;

    @NotNull(message = "وقت البداية المقرر مطلوب")
    @Column(name = "scheduled_start_time", nullable = false)
    private LocalTime scheduledStartTime; // e.g., 08:00

    @NotNull(message = "وقت النهاية المقرر مطلوب")
    @Column(name = "scheduled_end_time", nullable = false)
    private LocalTime scheduledEndTime; // e.g., 17:00

    @NotNull(message = "الساعات المطلوبة مطلوبة")
    @DecimalMin(value = "0.0", message = "الساعات المطلوبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "24.0", message = "الساعات المطلوبة لا يمكن أن تتجاوز 24 ساعة")
    @Column(name = "required_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal requiredHours; // e.g., 8.00

    @NotNull(message = "دقائق فترة السماح مطلوبة")
    @DecimalMin(value = "0", message = "فترة السماح لا يمكن أن تكون سالبة")
    @DecimalMax(value = "120", message = "فترة السماح لا يمكن أن تتجاوز 120 دقيقة")
    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer gracePeriodMinutes = 15; // Allow 15 min late without deduction

    @NotBlank(message = "علامة الحالة النشطة مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحالة النشطة يجب أن تكون Y أو N")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    // Relationships (lazy loading)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_code", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", insertable = false, updatable = false)
    private Project project;

    /**
     * Helper method to check if schedule is active
     *
     * @return true if schedule is active
     */
    public boolean isActiveSchedule() {
        return "Y".equals(this.isActive);
    }

    /**
     * Helper method to check if this is a default schedule (not tied to dept/project)
     *
     * @return true if this is a default schedule
     */
    public boolean isDefaultSchedule() {
        return departmentCode == null && projectCode == null;
    }

    /**
     * Helper method to check if schedule crosses midnight
     * (e.g., night shift from 23:00 to 07:00)
     *
     * @return true if end time is before start time
     */
    public boolean crossesMidnight() {
        return scheduledEndTime.isBefore(scheduledStartTime);
    }

    /**
     * Calculate scheduled duration handling midnight crossing
     *
     * @return Duration of the schedule
     */
    public Duration getScheduledDuration() {
        if (crossesMidnight()) {
            // Duration from start to midnight + midnight to end
            return Duration.between(scheduledStartTime, LocalTime.MAX)
                .plus(Duration.between(LocalTime.MIN, scheduledEndTime))
                .plusNanos(1000000); // Add 1ms because LocalTime.MAX is 23:59:59.999999999
        }
        return Duration.between(scheduledStartTime, scheduledEndTime);
    }

    /**
     * Get the time with grace period applied
     *
     * @return Start time + grace period
     */
    public LocalTime getGraceEndTime() {
        return scheduledStartTime.plusMinutes(gracePeriodMinutes);
    }
}
