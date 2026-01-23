package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for holiday response.
 * Used in GET endpoints for holiday retrieval.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayResponse {

    /**
     * Holiday ID
     */
    private Long holidayId;

    /**
     * Holiday date
     */
    private LocalDate holidayDate;

    /**
     * Arabic holiday name
     */
    private String holidayName;

    /**
     * English holiday name
     */

    /**
     * Holiday year
     */
    private Integer holidayYear;

    /**
     * Is recurring? (Y/N)
     */
    private String isRecurring;

    /**
     * Active flag (Y/N)
     */
    private String isActive;

    /**
     * Paid flag (Y/N)
     */
    private String isPaid;

    /**
     * Day of week (1=Monday, 7=Sunday)
     */
    private Integer dayOfWeek;

    /**
     * Day name (e.g., "FRIDAY", "SATURDAY")
     */
    private String dayName;

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
