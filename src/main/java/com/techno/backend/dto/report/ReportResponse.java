package com.techno.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for report generation.
 *
 * Contains:
 * - Report file content as byte array
 * - Filename with extension
 * - MIME type for HTTP response
 * - Metadata about the report
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    /**
     * Report file content (PDF or Excel bytes).
     */
    private byte[] content;

    /**
     * Suggested filename for download.
     * Example: "Payroll_Report_20250530_143022.xlsx"
     */
    private String filename;

    /**
     * MIME type for HTTP response Content-Type header.
     * - PDF: "application/pdf"
     * - Excel: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
     */
    private String mimeType;

    /**
     * Report title.
     */
    private String title;

    /**
     * Number of data rows in the report.
     */
    private Integer rowCount;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Get file size in KB.
     *
     * @return File size in kilobytes
     */
    public double getFileSizeKB() {
        return fileSize != null ? fileSize / 1024.0 : 0;
    }

    /**
     * Get file size in MB.
     *
     * @return File size in megabytes
     */
    public double getFileSizeMB() {
        return fileSize != null ? fileSize / (1024.0 * 1024.0) : 0;
    }
}
