package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for detailed attendance record response.
 * Used in GET /api/attendance/{id} and GET /api/attendance/list endpoints.
 *
 * Contains complete attendance information including GPS data, calculated hours,
 * and flags for special conditions (holiday work, weekend work, manual entry, etc.).
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    /**
     * Unique transaction ID
     */
    private Long transactionId;

    /**
     * Employee number
     */
    private Long employeeNo;

    /**
     * Employee's Arabic name
     */
    private String employeeName;

    /**
     * Employee's English name
     */

    /**
     * Attendance date
     */
    private LocalDate attendanceDate;

    /**
     * Day of week (1=Monday, 7=Sunday)
     */
    private Integer dayOfWeek;

    /**
     * Day name (e.g., "Monday", "Friday")
     */
    private String dayName;

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
     * Check-in timestamp
     */
    private LocalDateTime entryTime;

    /**
     * GPS latitude at check-in
     */
    private BigDecimal entryLatitude;

    /**
     * GPS longitude at check-in
     */
    private BigDecimal entryLongitude;

    /**
     * Distance from project site at check-in (meters)
     */
    private Double entryDistanceMeters;

    /**
     * Check-out timestamp
     */
    private LocalDateTime exitTime;

    /**
     * GPS latitude at check-out
     */
    private BigDecimal exitLatitude;

    /**
     * GPS longitude at check-out
     */
    private BigDecimal exitLongitude;

    /**
     * Distance from project site at check-out (meters)
     */
    private Double exitDistanceMeters;

    /**
     * Scheduled hours for this shift
     */
    private BigDecimal scheduledHours;

    /**
     * Total working hours
     */
    private BigDecimal workingHours;

    /**
     * Overtime hours
     */
    private BigDecimal overtimeCalc;

    /**
     * Delayed hours (late arrival)
     */
    private BigDecimal delayedCalc;

    /**
     * Early departure hours
     */
    private BigDecimal earlyOutCalc;

    /**
     * Shortage hours
     */
    private BigDecimal shortageHours;

    /**
     * Absence flag (Y/N)
     */
    private String absenceFlag;

    /**
     * Absence reason (if absent)
     */
    private String absenceReason;

    /**
     * Was check-out automatic? (Y/N)
     */
    private String isAutoCheckout;

    /**
     * Is this holiday work? (Y/N)
     */
    private String isHolidayWork;

    /**
     * Is this weekend work? (Y/N)
     */
    private String isWeekendWork;

    /**
     * Is this a manual entry by HR? (Y/N)
     */
    private String isManualEntry;

    /**
     * Additional notes
     */
    private String notes;

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
