package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.service.DocumentReportService;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PdfReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Document Reports.
 *
 * Endpoints:
 * - POST /api/reports/documents/expiring - Expiring Documents (passports and residencies)
 * - POST /api/reports/documents/expiring-passports - Expiring Passports Only
 *
 * All endpoints support both PDF and Excel formats via the 'format' parameter.
 * Date range defaults to next 14 days if not specified.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports/documents")
@Slf4j
public class DocumentReportController extends BaseReportController {

    private final DocumentReportService documentReportService;

    public DocumentReportController(ExcelReportService excelReportService,
                                   PdfReportService pdfReportService,
                                   DocumentReportService documentReportService) {
        super(excelReportService, pdfReportService);
        this.documentReportService = documentReportService;
    }

    /**
     * Generate Expiring Documents Report.
     *
     * Shows all documents (passports and residencies) expiring within a date range.
     * Defaults to next 14 days if date range not provided.
     *
     * @param request Report request with optional date range
     * @return PDF or Excel file for download
     */
    @PostMapping("/expiring")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateExpiringDocuments(@RequestBody ReportRequest request) {
        log.info("Request for Expiring Documents Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = documentReportService.generateExpiringDocuments(request);

        String filename = buildFilename("المستندات_المنتهية_الصلاحية",
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Expiring Passports Report.
     *
     * Shows only passports expiring within a date range.
     * Defaults to next 14 days if date range not provided.
     *
     * @param request Report request with optional date range
     * @return PDF or Excel file for download
     */
    @PostMapping("/expiring-passports")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateExpiringPassports(@RequestBody ReportRequest request) {
        log.info("Request for Expiring Passports Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = documentReportService.generateExpiringPassports(request);

        String filename = buildFilename("الجوازات_المنتهية_الصلاحية",
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Build filename with timestamp and extension.
     */
    private String buildFilename(String baseName, String format) {
        String extension = "EXCEL".equalsIgnoreCase(format) ? "xlsx" : "pdf";
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format("%s_%s.%s", sanitizedName, timestamp, extension);
    }

    /**
     * Build HTTP response with file download headers.
     */
    private ResponseEntity<byte[]> buildResponse(byte[] content, String filename, String mimeType) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(mimeType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    /**
     * Get MIME type based on format.
     */
    private String getMimeType(String format) {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/pdf";
        }
    }
}

