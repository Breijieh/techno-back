package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for employee timesheet (monthly attendance calendar).
 * Used in GET /api/attendance/timesheet endpoint.
 *
 * Contains day-by-day attendance status for a month with summary statistics.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Payroll Management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetResponse {

    private Long employeeNo;
    private String employeeName;
    private String month; // YYYY-MM format
    private Integer totalDays;
    private Integer present;
    private Integer absent;
    private Integer onLeave;
    private Integer late;
    private Integer weekends;
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalLateHours;
    private List<TimesheetDay> days;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimesheetDay {
        private LocalDate date;
        private Integer day; // Day of month (1-31)
        private String status; // Present, Absent, Leave, Weekend, Late
        private String color; // Hex color for UI
        private String textColor; // Hex color for text
        private java.time.LocalDateTime entryTime; // Optional: entry time if present
        private java.time.LocalDateTime exitTime; // Optional: exit time if present
        private BigDecimal workingHours; // Optional: hours worked
        private BigDecimal overtimeHours; // Optional: overtime hours
        private Boolean isLate; // Optional: was late arrival
    }
}

