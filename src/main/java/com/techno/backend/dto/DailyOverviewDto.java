package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for comprehensive daily overview for an employee.
 * Provides context about today's work status, project, and attendance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyOverviewDto {
    private LocalDate date;
    private String dayName;
    private boolean isWorkDay;
    private boolean isHoliday;
    private boolean isWeekend;
    private String holidayName;
    private AttendanceResponse attendance;
    private String projectName;
    private Long projectCode;
    private String statusAr;
    private String statusColor;
}
