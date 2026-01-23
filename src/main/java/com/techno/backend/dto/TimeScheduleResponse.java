package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for time schedule response.
 * Used in GET endpoints for time schedule retrieval.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeScheduleResponse {

    /**
     * Schedule ID
     */
    private Long scheduleId;

    /**
     * Schedule name
     */
    private String scheduleName;

    /**
     * Department code
     */
    private Long departmentCode;

    /**
     * Department name (Arabic)
     */
    private String departmentNameAr;

    /**
     * Department name (English)
     */
    private String departmentNameEn;

    /**
     * Project code
     */
    private Long projectCode;

    /**
     * Project name (Arabic)
     */
    private String projectNameAr;

    /**
     * Project name (English)
     */
    private String projectNameEn;

    /**
     * Scheduled start time
     */
    private LocalTime scheduledStartTime;

    /**
     * Scheduled end time
     */
    private LocalTime scheduledEndTime;

    /**
     * Required working hours
     */
    private BigDecimal requiredHours;

    /**
     * Grace period in minutes
     */
    private Integer gracePeriodMinutes;

    /**
     * Active flag (Y/N)
     */
    private String isActive;

    /**
     * Does this schedule cross midnight?
     */
    private Boolean crossesMidnight;

    /**
     * Grace end time (start time + grace period)
     */
    private LocalTime graceEndTime;

    /**
     * Record creation timestamp
     */
    private LocalDateTime createdDate;

    /**
     * User who created this record
     */
    private String createdBy;

    /**
     * Last modification timestamp
     */
    private LocalDateTime modifiedDate;

    /**
     * User who last modified this record
     */
    private String modifiedBy;
}
