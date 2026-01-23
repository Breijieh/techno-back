package com.techno.backend.dto.labor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for assignment overlap information.
 * Used to provide detailed information about overlapping assignments in error responses.
 * 
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentOverlapInfo {

    /**
     * Assignment number causing the overlap
     */
    private Long assignmentNo;

    /**
     * Project code
     */
    private Long projectCode;

    /**
     * Project name in English for display
     */
    private String projectName;

    /**
     * Project name in Arabic (optional)
     */

    /**
     * Assignment start date
     */
    private LocalDate startDate;

    /**
     * Assignment end date
     */
    private LocalDate endDate;

    /**
     * Assignment status (ACTIVE, COMPLETED, CANCELLED)
     */
    private String assignmentStatus;

    /**
     * Daily rate for context (optional)
     */
    private BigDecimal dailyRate;
}
