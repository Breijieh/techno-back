package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PayrollReportService;
import com.techno.backend.service.PdfReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payroll Reports.
 *
 * Endpoints:
 * - POST /api/reports/payroll/monthly-summary - Monthly Payroll Summary
 * - POST /api/reports/payroll/payslip - Employee Payslip
 * - POST /api/reports/payroll/variance - Payroll Variance Report
 *
 * All endpoints support both PDF and Excel formats via the 'format' parameter.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports/payroll")
@Slf4j
@PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'ADMIN')")
public class PayrollReportController extends BaseReportController {

    private final PayrollReportService payrollReportService;

    public PayrollReportController(ExcelReportService excelReportService,
                                  PdfReportService pdfReportService,
                                  PayrollReportService payrollReportService) {
        super(excelReportService, pdfReportService);
        this.payrollReportService = payrollReportService;
    }

    /**
     * Generate Monthly Payroll Summary Report.
     *
     * Shows overview of all employees' payroll for a specific month.
     *
     * @param request Report request with month (YYYY-MM) and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/monthly-summary")
    public ResponseEntity<byte[]> generateMonthlySummary(@RequestBody ReportRequest request) {
        log.info("Request for Monthly Payroll Summary: month={}, format={}",
                request.getMonth(), request.getFormat());

        // Validate request
        validateRequest(request);

        if (request.getMonth() == null || request.getMonth().trim().isEmpty()) {
            throw new IllegalArgumentException("معامل الشهر مطلوب (التنسيق: YYYY-MM)");
        }

        // Generate report
        byte[] reportContent = payrollReportService.generateMonthlyPayrollSummary(request);

        // Build response
        String filename = buildFilename("ملخص_الرواتب_الشهرية_" + request.getMonth(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Employee Payslip Report.
     *
     * Detailed payslip for a specific employee and month.
     *
     * @param request Report request with employeeNo, month, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/payslip")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<byte[]> generatePayslip(@RequestBody ReportRequest request) {
        log.info("Request for Employee Payslip: employee={}, month={}, format={}",
                request.getEmployeeNo(), request.getMonth(), request.getFormat());

        // Validate request
        validateRequest(request);

        if (request.getEmployeeNo() == null) {
            throw new IllegalArgumentException("رقم الموظف مطلوب");
        }

        if (request.getMonth() == null || request.getMonth().trim().isEmpty()) {
            throw new IllegalArgumentException("معامل الشهر مطلوب (التنسيق: YYYY-MM)");
        }

        // Generate report
        byte[] reportContent = payrollReportService.generateEmployeePayslip(request);

        // Build response
        String filename = buildFilename("إيصال_راتب_" + request.getEmployeeNo() + "_" + request.getMonth(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Payroll Variance Report.
     *
     * Month-over-month comparison of payroll changes.
     *
     * @param request Report request with previousMonth, currentMonth in additionalFilters, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/variance")
    public ResponseEntity<byte[]> generateVarianceReport(@RequestBody ReportRequest request) {
        log.info("Request for Payroll Variance Report: format={}", request.getFormat());

        // Validate request
        validateRequest(request);

        if (request.getAdditionalFilters() == null ||
            !request.getAdditionalFilters().containsKey("previousMonth") ||
            !request.getAdditionalFilters().containsKey("currentMonth")) {
            throw new IllegalArgumentException("الشهر السابق والشهر الحالي مطلوبان في الفلاتر الإضافية");
        }

        // Generate report
        byte[] reportContent = payrollReportService.generatePayrollVarianceReport(request);

        // Build response
        String previousMonth = request.getAdditionalFilters().get("previousMonth").toString();
        String currentMonth = request.getAdditionalFilters().get("currentMonth").toString();
        String filename = buildFilename("التباين_في_الرواتب_" + previousMonth + "_مقابل_" + currentMonth,
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    // ==================== Helper Methods ====================

    /**
     * Build HTTP response for file download.
     */
    private ResponseEntity<byte[]> buildResponse(byte[] content, String filename, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(content.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
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
