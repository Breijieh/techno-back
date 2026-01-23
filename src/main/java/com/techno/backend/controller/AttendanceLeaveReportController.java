package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.service.AttendanceLeaveReportService;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PdfReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Attendance, Leave, and Loan Reports.
 *
 * Endpoints:
 * - POST /api/reports/attendance/summary - Attendance Summary
 * - POST /api/reports/attendance/late-arrivals - Late Arrival Report
 * - POST /api/reports/attendance/absences - Absence Report
 * - POST /api/reports/attendance/overtime - Overtime Report
 * - POST /api/reports/leave/balance - Leave Balance Report
 * - POST /api/reports/leave/history - Leave History Report
 * - POST /api/reports/loan/summary - Loan Summary Report
 * - POST /api/reports/loan/payment-schedule - Loan Payment Schedule
 *
 * All endpoints support both PDF and Excel formats via the 'format' parameter.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports")
@Slf4j
public class AttendanceLeaveReportController extends BaseReportController {

    private final AttendanceLeaveReportService attendanceLeaveReportService;

    public AttendanceLeaveReportController(ExcelReportService excelReportService,
                                          PdfReportService pdfReportService,
                                          AttendanceLeaveReportService attendanceLeaveReportService) {
        super(excelReportService, pdfReportService);
        this.attendanceLeaveReportService = attendanceLeaveReportService;
    }

    // ==================== Attendance Reports ====================

    /**
     * Generate Attendance Summary Report.
     *
     * Daily attendance overview for a date range.
     *
     * @param request Report request with startDate, endDate, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/attendance/summary")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateAttendanceSummary(@RequestBody ReportRequest request) {
        log.info("Request for Attendance Summary: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = attendanceLeaveReportService.generateAttendanceSummary(request);

        String filename = buildFilename("ملخص_الحضور_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Late Arrival Report.
     *
     * List of employees who arrived late.
     *
     * @param request Report request with startDate, endDate, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/attendance/late-arrivals")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateLateArrivalReport(@RequestBody ReportRequest request) {
        log.info("Request for Late Arrival Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = attendanceLeaveReportService.generateLateArrivalReport(request);

        String filename = buildFilename("التأخيرات_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Absence Report.
     *
     * List of employees who were absent.
     *
     * @param request Report request with startDate, endDate, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/attendance/absences")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateAbsenceReport(@RequestBody ReportRequest request) {
        log.info("Request for Absence Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = attendanceLeaveReportService.generateAbsenceReport(request);

        String filename = buildFilename("الغيابات_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Overtime Report.
     *
     * Overtime hours worked by employees.
     *
     * @param request Report request with startDate, endDate, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/attendance/overtime")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateOvertimeReport(@RequestBody ReportRequest request) {
        log.info("Request for Overtime Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = attendanceLeaveReportService.generateOvertimeReport(request);

        String filename = buildFilename("العمل_الإضافي_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    // ==================== Leave Reports ====================

    /**
     * Generate Leave Balance Report.
     *
     * Current leave balances for all employees.
     *
     * @param request Report request with optional employeeNo filter and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/leave/balance")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<byte[]> generateLeaveBalanceReport(@RequestBody ReportRequest request) {
        log.info("Request for Leave Balance Report: employeeNo={}, format={}",
                request.getEmployeeNo(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = attendanceLeaveReportService.generateLeaveBalanceReport(request);

        String filename;
        if (request.getEmployeeNo() != null) {
            filename = buildFilename("رصيد_الإجازات_" + request.getEmployeeNo(),
                    request.getNormalizedFormat());
        } else {
            filename = buildFilename("رصيد_الإجازات_الكل",
                    request.getNormalizedFormat());
        }

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Leave History Report.
     *
     * Leave request history for a date range.
     *
     * @param request Report request with startDate, endDate, and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/leave/history")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateLeaveHistoryReport(@RequestBody ReportRequest request) {
        log.info("Request for Leave History Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = attendanceLeaveReportService.generateLeaveHistoryReport(request);

        String filename = buildFilename("سجل_الإجازات_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    // ==================== Loan Reports ====================

    /**
     * Generate Loan Summary Report.
     *
     * Overview of all active loans.
     *
     * @param request Report request with optional status filter and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/loan/summary")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateLoanSummaryReport(@RequestBody ReportRequest request) {
        log.info("Request for Loan Summary Report: status={}, format={}",
                request.getStatus(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = attendanceLeaveReportService.generateLoanSummaryReport(request);

        String filename;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            filename = buildFilename("Loan_Summary_" + request.getStatus(),
                    request.getNormalizedFormat());
        } else {
            filename = buildFilename("Loan_Summary_All",
                    request.getNormalizedFormat());
        }

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Loan Payment Schedule Report.
     *
     * Installment payment schedule for a specific loan.
     *
     * @param request Report request with loanId in additionalFilters and format
     * @return PDF or Excel file for download
     */
    @PostMapping("/loan/payment-schedule")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<byte[]> generateLoanPaymentSchedule(@RequestBody ReportRequest request) {
        log.info("Request for Loan Payment Schedule Report: format={}", request.getFormat());

        validateRequest(request);

        if (request.getAdditionalFilters() == null ||
            !request.getAdditionalFilters().containsKey("loanId")) {
            throw new IllegalArgumentException("معرف القرض مطلوب في الفلاتر الإضافية");
        }

        byte[] reportContent = attendanceLeaveReportService.generateLoanPaymentSchedule(request);

        Long loanId = Long.valueOf(request.getAdditionalFilters().get("loanId").toString());
        String filename = buildFilename("جدول_دفعات_القرض_" + loanId,
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
