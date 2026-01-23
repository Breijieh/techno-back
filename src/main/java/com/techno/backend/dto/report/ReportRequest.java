package com.techno.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Generic request DTO for report generation.
 *
 * Contains common parameters needed for all reports:
 * - Date range filtering
 * - Format selection (PDF/Excel)
 * - Additional filters specific to report type
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    /**
     * Report format: "PDF" or "EXCEL"
     */
    private String format;

    /**
     * Start date for date range filtering.
     * Optional - if not provided, report includes all dates.
     */
    private LocalDate startDate;

    /**
     * End date for date range filtering.
     * Optional - if not provided, uses current date.
     */
    private LocalDate endDate;

    /**
     * Department code filter (optional).
     * Used in department-specific reports.
     */
    private Long departmentCode;

    /**
     * Project code filter (optional).
     * Used in project-specific reports.
     */
    private Long projectCode;

    /**
     * Employee number filter (optional).
     * Used in employee-specific reports.
     */
    private Long employeeNo;

    /**
     * Status filter (optional).
     * Examples: "ACTIVE", "APPROVED", "PENDING", etc.
     */
    private String status;

    /**
     * Month filter in YYYY-MM format (optional).
     * Used in monthly reports (payroll, attendance).
     */
    private String month;

    /**
     * Year filter (optional).
     * Used in annual reports.
     */
    private Integer year;

    /**
     * Additional custom filters as key-value pairs.
     * Allows report-specific parameters without changing the DTO.
     */
    private Map<String, Object> additionalFilters;

    /**
     * Validate report format.
     *
     * @return true if format is valid (PDF or EXCEL)
     */
    public boolean isValidFormat() {
        return format != null && (format.equalsIgnoreCase("PDF") || format.equalsIgnoreCase("EXCEL"));
    }

    /**
     * Get format in uppercase.
     *
     * @return Normalized format string
     */
    public String getNormalizedFormat() {
        return format != null ? format.toUpperCase() : "PDF";
    }
}
