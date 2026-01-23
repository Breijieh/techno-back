package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PdfReportService;
import com.techno.backend.service.ProjectReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Project Reports.
 *
 * Endpoints:
 * - POST /api/reports/projects/financial-status - Project Financial Status
 * - POST /api/reports/projects/payment-schedule - Payment Schedule
 * - POST /api/reports/projects/labor-allocation - Labor Allocation
 * - POST /api/reports/projects/transfer-history - Transfer History
 *
 * All endpoints support both PDF and Excel formats via the 'format' parameter.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports/projects")
@Slf4j
public class ProjectReportController extends BaseReportController {

    private final ProjectReportService projectReportService;

    public ProjectReportController(ExcelReportService excelReportService,
                                  PdfReportService pdfReportService,
                                  ProjectReportService projectReportService) {
        super(excelReportService, pdfReportService);
        this.projectReportService = projectReportService;
    }

    /**
     * Generate Project Financial Status Report.
     *
     * Shows financial overview for projects.
     *
     * @param request Report request with optional projectCode filter
     * @return PDF or Excel file for download
     */
    @PostMapping("/financial-status")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<byte[]> generateFinancialStatus(@RequestBody ReportRequest request) {
        log.info("Request for Project Financial Status Report: projectCode={}, format={}",
                request.getProjectCode(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = projectReportService.generateProjectFinancialStatus(request);

        String filename = buildFilename("الحالة_المالية_للمشاريع",
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Payment Schedule Report.
     *
     * Shows all scheduled payments for a project.
     *
     * @param request Report request with projectCode
     * @return PDF or Excel file for download
     */
    @PostMapping("/payment-schedule")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<byte[]> generatePaymentSchedule(@RequestBody ReportRequest request) {
        log.info("Request for Payment Schedule Report: projectCode={}, format={}",
                request.getProjectCode(), request.getFormat());

        validateRequest(request);

        if (request.getProjectCode() == null) {
            throw new IllegalArgumentException("رمز المشروع مطلوب");
        }

        byte[] reportContent = projectReportService.generatePaymentSchedule(request);

        String filename = buildFilename("جدول_المدفوعات_مشروع_" + request.getProjectCode(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Labor Allocation Report.
     *
     * Shows employee assignments and identifies vacant/over-allocated workers.
     *
     * @param request Report request with date range and optional projectCode
     * @return PDF or Excel file for download
     */
    @PostMapping("/labor-allocation")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateLaborAllocation(@RequestBody ReportRequest request) {
        log.info("Request for Labor Allocation Report: {} to {}, projectCode={}, format={}",
                request.getStartDate(), request.getEndDate(), request.getProjectCode(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = projectReportService.generateLaborAllocation(request);

        String filename = buildFilename("توزيع_العمالة_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Transfer History Report.
     *
     * Shows all approved transfer requests.
     *
     * @param request Report request with date range and optional projectCode
     * @return PDF or Excel file for download
     */
    @PostMapping("/transfer-history")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateTransferHistory(@RequestBody ReportRequest request) {
        log.info("Request for Transfer History Report: {} to {}, projectCode={}, format={}",
                request.getStartDate(), request.getEndDate(), request.getProjectCode(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = projectReportService.generateTransferHistory(request);

        String filename = buildFilename("سجل_النقلات_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
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

