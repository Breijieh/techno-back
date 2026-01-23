package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.dto.report.ReportResponse;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Base controller for all report endpoints.
 *
 * Provides common functionality:
 * - Report format selection (PDF vs Excel)
 * - HTTP response building with proper headers
 * - File download handling
 * - Error handling for report generation
 *
 * Concrete report controllers extend this class and implement
 * report-specific data collection and formatting logic.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseReportController {

    protected final ExcelReportService excelReportService;
    protected final PdfReportService pdfReportService;

    /**
     * Generate report based on request format (PDF or Excel).
     *
     * @param title Report title
     * @param headers Column headers
     * @param data Report data rows
     * @param metadata Report metadata (filters, date range, etc.)
     * @param request Report request containing format preference
     * @return HTTP response with report file for download
     */
    protected ResponseEntity<byte[]> generateReport(
            String title,
            List<String> headers,
            List<List<Object>> data,
            Map<String, Object> metadata,
            ReportRequest request) {

        log.info("Generating {} report: {} ({} rows)",
                request.getNormalizedFormat(), title, data.size());

        try {
            // Generate report based on format
            byte[] reportContent;
            String filename;
            String mimeType;

            if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
                reportContent = excelReportService.generateReport(title, headers, data, metadata);
                filename = buildFilename(title, "xlsx");
                mimeType = excelReportService.getMimeType();
            } else {
                // Default to PDF
                reportContent = pdfReportService.generateReport(title, headers, data, metadata);
                filename = buildFilename(title, "pdf");
                mimeType = pdfReportService.getMimeType();
            }

            // Build HTTP response with proper headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType(mimeType));
            responseHeaders.setContentDispositionFormData("attachment", filename);
            responseHeaders.setContentLength(reportContent.length);

            log.info("Report generated successfully: {} ({} bytes)", filename, reportContent.length);

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(reportContent);

        } catch (Exception e) {
            log.error("Failed to generate report '{}': {}", title, e.getMessage(), e);
            throw new RuntimeException("فشل إنشاء التقرير: " + e.getMessage(), e);
        }
    }

    /**
     * Build ReportResponse DTO (for JSON responses instead of file download).
     *
     * @param title Report title
     * @param headers Column headers
     * @param data Report data rows
     * @param metadata Report metadata
     * @param request Report request
     * @return ReportResponse DTO
     */
    protected ReportResponse buildReportResponse(
            String title,
            List<String> headers,
            List<List<Object>> data,
            Map<String, Object> metadata,
            ReportRequest request) {

        log.info("Building report response: {} ({} rows)", title, data.size());

        try {
            // Generate report based on format
            byte[] reportContent;
            String filename;
            String mimeType;

            if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
                reportContent = excelReportService.generateReport(title, headers, data, metadata);
                filename = buildFilename(title, "xlsx");
                mimeType = excelReportService.getMimeType();
            } else {
                reportContent = pdfReportService.generateReport(title, headers, data, metadata);
                filename = buildFilename(title, "pdf");
                mimeType = pdfReportService.getMimeType();
            }

            return ReportResponse.builder()
                    .content(reportContent)
                    .filename(filename)
                    .mimeType(mimeType)
                    .title(title)
                    .rowCount(data.size())
                    .fileSize((long) reportContent.length)
                    .build();

        } catch (Exception e) {
            log.error("Failed to build report response '{}': {}", title, e.getMessage(), e);
            throw new RuntimeException("فشل بناء استجابة التقرير: " + e.getMessage(), e);
        }
    }

    /**
     * Build filename with timestamp and extension.
     *
     * @param reportName Base report name
     * @param extension File extension (pdf or xlsx)
     * @return Filename with timestamp
     */
    private String buildFilename(String reportName, String extension) {
        String sanitizedName = reportName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", sanitizedName, timestamp, extension);
    }

    /**
     * Validate report request parameters.
     *
     * @param request Report request to validate
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateRequest(ReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("طلب التقرير لا يمكن أن يكون فارغاً");
        }

        if (!request.isValidFormat()) {
            throw new IllegalArgumentException(
                    "تنسيق التقرير غير صالح: " + request.getFormat() + ". يجب أن يكون PDF أو EXCEL");
        }

        // Validate date range if both provided
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("تاريخ البداية لا يمكن أن يكون بعد تاريخ النهاية");
            }
        }
    }
}
