package com.techno.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO for creating or updating time schedule.
 * Used in POST /api/schedules and PUT /api/schedules/{id} endpoints.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeScheduleRequest {

    /**
     * Schedule name (e.g., "Morning Shift", "Night Shift", "Riyadh Office Hours")
     */
    @NotBlank(message = "اسم الجدول مطلوب")
    @Size(max = 100, message = "اسم الجدول لا يجب أن يتجاوز 100 حرف")
    private String scheduleName;

    /**
     * Department code (optional - if null, applies to all departments)
     */
    private Long departmentCode;

    /**
     * Project code (optional - if specified, overrides department schedule)
     */
    private Long projectCode;

    /**
     * Scheduled start time (e.g., 08:00)
     */
    @NotNull(message = "وقت البدء المجدول مطلوب")
    private LocalTime scheduledStartTime;

    /**
     * Scheduled end time (e.g., 17:00)
     * Can be before start time for midnight-crossing shifts
     */
    @NotNull(message = "وقت الانتهاء المجدول مطلوب")
    private LocalTime scheduledEndTime;

    /**
     * Required working hours (e.g., 8.00)
     * Note: This is automatically calculated from scheduledStartTime and scheduledEndTime.
     * If provided, it will be overridden with the calculated value.
     */
    @DecimalMin(value = "0.0", message = "الساعات المطلوبة لا يمكن أن تكون سالبة")
    @DecimalMax(value = "24.0", message = "الساعات المطلوبة لا يمكن أن تتجاوز 24 ساعة")
    private BigDecimal requiredHours;

    /**
     * Grace period in minutes for late arrivals (default: 15)
     */
    @NotNull(message = "فترة السماح مطلوبة")
    @Min(value = 0, message = "فترة السماح لا يمكن أن تكون سالبة")
    @Max(value = 120, message = "فترة السماح لا يمكن أن تتجاوز 120 دقيقة")
    private Integer gracePeriodMinutes;

    /**
     * Active flag (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون Y أو N")
    private String isActive;
}
