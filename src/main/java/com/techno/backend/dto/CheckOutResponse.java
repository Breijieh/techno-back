package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for check-out response.
 * Returned after successful employee check-out with calculated hours.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckOutResponse {

    /**
     * Transaction ID of this attendance record
     */
    private Long transactionId;

    /**
     * Employee number who checked out
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
     * Date of attendance
     */
    private LocalDate attendanceDate;

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
     * Total working hours
     */
    private BigDecimal workingHours;

    /**
     * Scheduled hours for this shift
     */
    private BigDecimal scheduledHours;

    /**
     * Overtime hours (if any)
     */
    private BigDecimal overtimeCalc;

    /**
     * Delayed hours (late arrival beyond grace period)
     */
    private BigDecimal delayedCalc;

    /**
     * Early departure hours
     */
    private BigDecimal earlyOutCalc;

    /**
     * Shortage hours (if didn't meet scheduled hours)
     */
    private BigDecimal shortageHours;

    /**
     * Was this work on a holiday?
     */
    private Boolean isHolidayWork;

    /**
     * Was this work on a weekend?
     */
    private Boolean isWeekendWork;

    /**
     * Success message
     */
    private String message;

    /**
     * Summary of the day's work (e.g., "Worked 8.5 hours, 0.5 overtime")
     */
    private String summary;
}
