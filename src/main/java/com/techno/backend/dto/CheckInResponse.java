package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for check-in response.
 * Returned after successful employee check-in.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {

    /**
     * Generated transaction ID for this attendance record
     */
    private Long transactionId;

    /**
     * Employee number who checked in
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
     * Project code where employee checked in
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
     * Distance from project site in meters
     */
    private Double entryDistanceMeters;

    /**
     * Scheduled start time for this shift
     */
    private String scheduledStartTime;

    /**
     * Whether the check-in was within the grace period
     */
    private Boolean withinGracePeriod;

    /**
     * Number of minutes late (0 if on time)
     */
    private Integer minutesLate;

    /**
     * Success message
     */
    private String message;
}
