package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for attendance overview chart data.
 * Contains weekly attendance statistics.
 *
 * @author Techno HR System
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceOverviewResponse {

    /**
     * Week labels (e.g., "Week 1", "Week 2", etc.)
     */
    private List<String> weeks;

    /**
     * Present counts for each week
     */
    private List<Long> present;

    /**
     * Absent counts for each week
     */
    private List<Long> absent;

    /**
     * On leave counts for each week
     */
    private List<Long> onLeave;
}

