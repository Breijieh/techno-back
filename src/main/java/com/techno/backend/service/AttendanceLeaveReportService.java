package com.techno.backend.service;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating attendance, leave, and loan reports.
 *
 * Implements 8 reports:
 * 1. Attendance Summary Report - Daily attendance overview
 * 2. Late Arrival Report - Employees with late arrivals
 * 3. Absence Report - Absence tracking
 * 4. Overtime Report - Overtime hours worked
 * 5. Leave Balance Report - Current leave balances
 * 6. Leave History Report - Leave request history
 * 7. Loan Summary Report - Active loans overview
 * 8. Loan Payment Schedule Report - Installment schedule
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceLeaveReportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    /**
     * Generate Attendance Summary Report.
     *
     * Daily attendance overview for a date range.
     *
     * @param request Report request with startDate and endDate
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateAttendanceSummary(ReportRequest request) {
        log.info("Generating Attendance Summary Report: {} to {}",
                request.getStartDate(), request.getEndDate());

        validateDateRange(request);

        // Get attendance records for date range
        List<AttendanceTransaction> attendances = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            attendances.addAll(attendanceRepository.findByAttendanceDate(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        log.info("Found {} attendance records", attendances.size());

        // Build report data
        String title = String.format("Ù…Ù„Ø®Øµ Ø§Ù„Ø­Ø¶ÙˆØ± - Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                request.getStartDate(), request.getEndDate());

        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "ÙˆÙ‚Øª Ø§Ù„Ø¯Ø®ÙˆÙ„",
                "ÙˆÙ‚Øª Ø§Ù„Ø®Ø±ÙˆØ¬",
                "Ø³Ø§Ø¹Ø§Øª Ø§Ù„Ø¹Ù…Ù„",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "Ø³Ø§Ø¹Ø§Øª Ø§Ù„ØªØ£Ø®ÙŠØ±"
        );

        List<List<Object>> data = attendances.stream()
                .map(att -> {
                    Employee emp = employeeRepository.findById(att.getEmployeeNo()).orElse(null);
                    String status = att.isAbsent() ? "ØºØ§Ø¦Ø¨" :
                                   (att.hasCheckedOut() ? "Ù…ÙƒØªÙ…Ù„" :
                                   (att.hasCheckedIn() ? "ØºÙŠØ± Ù…ÙƒØªÙ…Ù„" : "Ù„Ù… ÙŠØ³Ø¬Ù„ Ø§Ù„Ø¯Ø®ÙˆÙ„"));
                    return Arrays.<Object>asList(
                            att.getAttendanceDate(),
                            att.getEmployeeNo(),
                            emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                            att.getEntryTime(),
                            att.getExitTime(),
                            att.getWorkingHours() != null ? att.getWorkingHours() : BigDecimal.ZERO,
                            status,
                            att.getDelayedCalc() != null ? att.getDelayedCalc() : BigDecimal.ZERO
                    );
                })
                .collect(Collectors.toList());

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø³Ø¬Ù„Ø§Øª", attendances.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Late Arrival Report.
     *
     * List of employees who arrived late in the date range.
     *
     * @param request Report request with startDate and endDate
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLateArrivalReport(ReportRequest request) {
        log.info("Generating Late Arrival Report: {} to {}",
                request.getStartDate(), request.getEndDate());

        validateDateRange(request);

        // Get attendance records with late arrivals
        List<AttendanceTransaction> lateArrivals = attendanceRepository
                .findLateArrivalsByDateRange(request.getStartDate(), request.getEndDate());

        log.info("Found {} late arrival records", lateArrivals.size());

        String title = String.format("ØªÙ‚Ø±ÙŠØ± Ø§Ù„ØªØ£Ø®ÙŠØ± - Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                request.getStartDate(), request.getEndDate());

        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„Ù‚Ø³Ù…",
                "ÙˆÙ‚Øª Ø§Ù„Ø¯Ø®ÙˆÙ„",
                "Ø³Ø§Ø¹Ø§Øª Ø§Ù„ØªØ£Ø®ÙŠØ±",
                "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹"
        );

        List<List<Object>> data = lateArrivals.stream()
                .map(att -> {
                    Employee emp = employeeRepository.findById(att.getEmployeeNo()).orElse(null);
                    return Arrays.<Object>asList(
                            att.getAttendanceDate(),
                            att.getEmployeeNo(),
                            emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                            emp != null && emp.getPrimaryDeptCode() != null ?
                                    "Ù‚Ø³Ù… " + emp.getPrimaryDeptCode() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            att.getEntryTime(),
                            att.getDelayedCalc(),
                            att.getProjectCode() != null ? "Ù…Ø´Ø±ÙˆØ¹ " + att.getProjectCode() : "ØºÙŠØ± Ù…ØªØ§Ø­"
                    );
                })
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("Ø¹Ø¯Ø¯ Ø­Ø§Ù„Ø§Øª Ø§Ù„ØªØ£Ø®ÙŠØ±", lateArrivals.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Absence Report.
     *
     * Employees who were absent in the date range.
     *
     * @param request Report request with startDate and endDate
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateAbsenceReport(ReportRequest request) {
        log.info("Generating Absence Report: {} to {}",
                request.getStartDate(), request.getEndDate());

        validateDateRange(request);

        // Get absence records for each date in range
        List<AttendanceTransaction> absences = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            absences.addAll(attendanceRepository.findAbsencesByDate(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        log.info("Found {} absence records", absences.size());

        String title = String.format("ØªÙ‚Ø±ÙŠØ± Ø§Ù„ØºÙŠØ§Ø¨ - Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                request.getStartDate(), request.getEndDate());

        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„Ù‚Ø³Ù…",
                "Ø³Ø¨Ø¨ Ø§Ù„ØºÙŠØ§Ø¨",
                "ÙˆØ§ÙÙ‚ Ø¹Ù„ÙŠÙ‡",
                "Ù…Ù„Ø§Ø­Ø¸Ø§Øª"
        );

        List<List<Object>> data = absences.stream()
                .map(att -> {
                    Employee emp = employeeRepository.findById(att.getEmployeeNo()).orElse(null);
                    return Arrays.<Object>asList(
                            att.getAttendanceDate(),
                            att.getEmployeeNo(),
                            emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                            emp != null && emp.getPrimaryDeptCode() != null ?
                                    "Ù‚Ø³Ù… " + emp.getPrimaryDeptCode() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            att.getAbsenceReason() != null ? att.getAbsenceReason() : "",
                            att.getAbsenceApprovedBy() != null ? att.getAbsenceApprovedBy() : "ØºÙŠØ± Ù…ÙˆØ§ÙÙ‚ Ø¹Ù„ÙŠÙ‡",
                            att.getNotes() != null ? att.getNotes() : ""
                    );
                })
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("Ø¹Ø¯Ø¯ Ø­Ø§Ù„Ø§Øª Ø§Ù„ØºÙŠØ§Ø¨", absences.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Overtime Report.
     *
     * Overtime hours worked by employees.
     *
     * @param request Report request with startDate and endDate
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateOvertimeReport(ReportRequest request) {
        log.info("Generating Overtime Report: {} to {}",
                request.getStartDate(), request.getEndDate());

        validateDateRange(request);

        // Get attendance records for date range and filter for overtime
        List<AttendanceTransaction> overtimeRecords = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            List<AttendanceTransaction> dailyRecords = attendanceRepository.findByAttendanceDate(currentDate);
            overtimeRecords.addAll(dailyRecords.stream()
                    .filter(att -> att.getOvertimeCalc() != null &&
                                  att.getOvertimeCalc().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList()));
            currentDate = currentDate.plusDays(1);
        }

        log.info("Found {} overtime records", overtimeRecords.size());

        String title = String.format("ØªÙ‚Ø±ÙŠØ± Ø§Ù„Ø¹Ù…Ù„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠ - Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                request.getStartDate(), request.getEndDate());

        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„Ù‚Ø³Ù…",
                "Ø³Ø§Ø¹Ø§Øª Ø§Ù„Ø¹Ù…Ù„",
                "Ø³Ø§Ø¹Ø§Øª Ø§Ù„Ø¹Ù…Ù„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠ",
                "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹"
        );

        BigDecimal totalOvertimeHours = BigDecimal.ZERO;

        List<List<Object>> data = new ArrayList<>();
        for (AttendanceTransaction att : overtimeRecords) {
            Employee emp = employeeRepository.findById(att.getEmployeeNo()).orElse(null);
            BigDecimal overtimeHours = att.getOvertimeCalc() != null ?
                    att.getOvertimeCalc() : BigDecimal.ZERO;

            data.add(Arrays.asList(
                    att.getAttendanceDate(),
                    att.getEmployeeNo(),
                    emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    emp != null && emp.getPrimaryDeptCode() != null ?
                            "Ù‚Ø³Ù… " + emp.getPrimaryDeptCode() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                    att.getWorkingHours() != null ? att.getWorkingHours() : BigDecimal.ZERO,
                    overtimeHours,
                    att.getProjectCode() != null ? "Ù…Ø´Ø±ÙˆØ¹ " + att.getProjectCode() : "ØºÙŠØ± Ù…ØªØ§Ø­"
            ));

            totalOvertimeHours = totalOvertimeHours.add(overtimeHours);
        }

        // Add total row
        data.add(Arrays.asList(
                "",
                "",
                "TOTAL",
                "",
                "",
                totalOvertimeHours,
                ""
        ));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø³Ø§Ø¹Ø§Øª Ø§Ù„Ø¹Ù…Ù„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠ", totalOvertimeHours);
        metadata.put("Ø¹Ø¯Ø¯ Ø§Ù„Ø³Ø¬Ù„Ø§Øª", overtimeRecords.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Leave Balance Report.
     *
     * Current leave balances for all employees.
     *
     * @param request Report request with optional employeeNo filter
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLeaveBalanceReport(ReportRequest request) {
        log.info("Generating Leave Balance Report");

        // Get employees
        List<Employee> employees;
        if (request.getEmployeeNo() != null) {
            Employee emp = employeeRepository.findById(request.getEmployeeNo()).orElse(null);
            employees = emp != null ? Arrays.asList(emp) : new ArrayList<>();
        } else {
            employees = employeeRepository.findAll();
        }

        log.info("Found {} employee records", employees.size());

        String title = "ØªÙ‚Ø±ÙŠØ± Ø±ØµÙŠØ¯ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø§Øª";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„Ù‚Ø³Ù…",
                "Ø­Ø§Ù„Ø© Ø§Ù„ØªÙˆØ¸ÙŠÙ",
                "Ø±ØµÙŠØ¯ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø§Øª Ø¨Ø§Ù„Ø£ÙŠØ§Ù…"
        );

        List<List<Object>> data = employees.stream()
                .map(emp -> Arrays.<Object>asList(
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        emp.getPrimaryDeptCode() != null ? "Ù‚Ø³Ù… " + emp.getPrimaryDeptCode() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                        emp.getEmploymentStatus(),
                        emp.getLeaveBalanceDays() != null ? emp.getLeaveBalanceDays() : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        if (request.getEmployeeNo() != null) {
            metadata.put("Ø§Ù„Ù…ÙˆØ¸Ù", request.getEmployeeNo());
        }
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ†", employees.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Leave History Report.
     *
     * Leave request history for a date range.
     *
     * @param request Report request with startDate and endDate
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLeaveHistoryReport(ReportRequest request) {
        log.info("Generating Leave History Report: {} to {}",
                request.getStartDate(), request.getEndDate());

        validateDateRange(request);

        // Get all leaves and filter by date range
        List<EmployeeLeave> leaves = employeeLeaveRepository.findAll().stream()
                .filter(leave ->
                        (leave.getLeaveFromDate().isAfter(request.getStartDate().minusDays(1)) &&
                         leave.getLeaveFromDate().isBefore(request.getEndDate().plusDays(1))) ||
                        (leave.getLeaveToDate().isAfter(request.getStartDate().minusDays(1)) &&
                         leave.getLeaveToDate().isBefore(request.getEndDate().plusDays(1))))
                .collect(Collectors.toList());

        log.info("Found {} leave records", leaves.size());

        String title = String.format("ØªÙ‚Ø±ÙŠØ± Ø³Ø¬Ù„ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø§Øª - Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                request.getStartDate(), request.getEndDate());

        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ù…Ù† ØªØ§Ø±ÙŠØ®",
                "Ø¥Ù„Ù‰ ØªØ§Ø±ÙŠØ®",
                "Ø§Ù„Ø£ÙŠØ§Ù…",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø·Ù„Ø¨",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "ÙˆØ§ÙÙ‚ Ø¹Ù„ÙŠÙ‡"
        );

        List<List<Object>> data = leaves.stream()
                .map(leave -> {
                    Employee emp = employeeRepository.findById(leave.getEmployeeNo()).orElse(null);
                    return Arrays.<Object>asList(
                            leave.getEmployeeNo(),
                            emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                            leave.getLeaveFromDate(),
                            leave.getLeaveToDate(),
                            leave.getLeaveDays(),
                            leave.getRequestDate(),
                            getLeaveStatus(leave.getTransStatus()),
                            leave.getApprovedBy() != null ? leave.getApprovedBy() : "Ù…Ø¹Ù„Ù‚"
                    );
                })
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø§Øª", leaves.size());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Loan Summary Report.
     *
     * Overview of all active loans.
     *
     * @param request Report request with optional status filter
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLoanSummaryReport(ReportRequest request) {
        log.info("Generating Loan Summary Report");

        // Get loans
        List<Loan> loans;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            // Filter by status after fetching all
            String filterStatus = request.getStatus();
            loans = loanRepository.findAll().stream()
                    .filter(loan -> filterStatus.equals(loan.getTransStatus()))
                    .collect(Collectors.toList());
        } else {
            loans = loanRepository.findAll();
        }

        log.info("Found {} loan records", loans.size());

        String title = "ØªÙ‚Ø±ÙŠØ± Ù…Ù„Ø®Øµ Ø§Ù„Ù‚Ø±ÙˆØ¶";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶",
                "Ø¹Ø¯Ø¯ Ø§Ù„Ø£Ù‚Ø³Ø§Ø·",
                "Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø³Ø·",
                "Ø§Ù„Ø±ØµÙŠØ¯ Ø§Ù„Ù…ØªØ¨Ù‚ÙŠ",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©"
        );

        BigDecimal totalLoanAmount = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        List<List<Object>> data = new ArrayList<>();
        for (Loan loan : loans) {
            Employee emp = employeeRepository.findById(loan.getEmployeeNo()).orElse(null);

            data.add(Arrays.asList(
                    loan.getEmployeeNo(),
                    emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    loan.getLoanAmount(),
                    loan.getNoOfInstallments(),
                    loan.getInstallmentAmount(),
                    loan.getRemainingBalance(),
                    getLoanStatus(loan.getTransStatus(), loan.getIsActive()),
                    loan.getApprovedDate() != null ? loan.getApprovedDate().toLocalDate() : null
            ));

            totalLoanAmount = totalLoanAmount.add(loan.getLoanAmount());
            totalRemaining = totalRemaining.add(loan.getRemainingBalance());
        }

        // Add totals
        data.add(Arrays.asList(
                "",
                "TOTAL",
                totalLoanAmount,
                "",
                "",
                totalRemaining,
                "",
                ""
        ));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù‚Ø±ÙˆØ¶", loans.size());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø¨Ù„Øº", totalLoanAmount);
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…ØªØ¨Ù‚ÙŠ", totalRemaining);
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Loan Payment Schedule Report.
     *
     * Installment payment schedule for a specific loan.
     *
     * @param request Report request with loanId in additionalFilters
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLoanPaymentSchedule(ReportRequest request) {
        log.info("Generating Loan Payment Schedule Report");

        // Validate loanId
        if (request.getAdditionalFilters() == null ||
            !request.getAdditionalFilters().containsKey("loanId")) {
            throw new IllegalArgumentException("Ù…Ø¹Ø±Ù Ø§Ù„Ù‚Ø±Ø¶ Ù…Ø·Ù„ÙˆØ¨ ÙÙŠ Ø§Ù„ÙÙ„Ø§ØªØ± Ø§Ù„Ø¥Ø¶Ø§ÙÙŠØ©");
        }

        Long loanId = Long.valueOf(request.getAdditionalFilters().get("loanId").toString());

        // Get loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + loanId));

        // Get installments
        List<LoanInstallment> installments = loanInstallmentRepository
                .findByLoanIdOrderByInstallmentNoAsc(loanId);

        Employee emp = employeeRepository.findById(loan.getEmployeeNo()).orElse(null);

        log.info("Found {} installments for loan {}", installments.size(), loanId);

        String title = String.format("Ø¬Ø¯ÙˆÙ„ Ø³Ø¯Ø§Ø¯ Ø§Ù„Ù‚Ø±Ø¶ - %s (Ø§Ù„Ù‚Ø±Ø¶ Ø±Ù‚Ù… %d)",
                emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ", loanId);

        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù‚Ø³Ø·",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ø³ØªØ­Ù‚Ø§Ù‚",
                "Ø§Ù„Ù…Ø¨Ù„Øº",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¯ÙØ¹",
                "Ø§Ù„Ù…Ø¨Ù„Øº Ø§Ù„Ù…Ø¯ÙÙˆØ¹",
                "Ø´Ù‡Ø± Ø§Ù„Ø±Ø§ØªØ¨"
        );

        List<List<Object>> data = installments.stream()
                .map(inst -> Arrays.<Object>asList(
                        inst.getInstallmentNo(),
                        inst.getDueDate(),
                        inst.getInstallmentAmount(),
                        inst.getPaymentStatus(),
                        inst.getPaidDate(),
                        inst.getPaidAmount() != null ? inst.getPaidAmount() : BigDecimal.ZERO,
                        inst.getSalaryMonth() != null ? inst.getSalaryMonth() : "ØºÙŠØ± Ù…ØªØ§Ø­"
                ))
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø§Ù„Ù…ÙˆØ¸Ù", emp != null ? emp.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
        metadata.put("Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø±Ø¶", loan.getLoanAmount());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø£Ù‚Ø³Ø§Ø·", loan.getNoOfInstallments());
        metadata.put("Ù…Ø¨Ù„Øº Ø§Ù„Ù‚Ø³Ø·", loan.getInstallmentAmount());
        metadata.put("Ø§Ù„Ø±ØµÙŠØ¯ Ø§Ù„Ù…ØªØ¨Ù‚ÙŠ", loan.getRemainingBalance());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    // ==================== Helper Methods ====================

    private void validateDateRange(ReportRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ ÙˆØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ù…Ø·Ù„ÙˆØ¨Ø§Ù†");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡");
        }
    }

    private byte[] generateReport(String title, List<String> headers,
                                  List<List<Object>> data, Map<String, Object> metadata,
                                  ReportRequest request) {
        if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
            return excelReportService.generateReport(title, headers, data, metadata);
        } else {
            return pdfReportService.generateReport(title, headers, data, metadata);
        }
    }

    private String getLeaveStatus(String status) {
        return switch (status) {
            case "A" -> "Ù…ÙˆØ§ÙÙ‚ Ø¹Ù„ÙŠÙ‡";
            case "N" -> "Ù…Ø¹Ù„Ù‚";
            case "R" -> "Ù…Ø±ÙÙˆØ¶";
            default -> status;
        };
    }

    private String getLoanStatus(String status, String isActive) {
        if ("A".equals(status) && "Y".equals(isActive)) {
            return "Ù†Ø´Ø·";
        } else if ("A".equals(status) && "N".equals(isActive)) {
            return "Ù…Ø¯ÙÙˆØ¹ Ø¨Ø§Ù„ÙƒØ§Ù…Ù„";
        } else {
            return switch (status) {
                case "N" -> "Ù…Ø¹Ù„Ù‚";
                case "R" -> "Ù…Ø±ÙÙˆØ¶";
                default -> status;
            };
        }
    }
}

