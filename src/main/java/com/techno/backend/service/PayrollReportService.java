package com.techno.backend.service;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.SalaryDetail;
import com.techno.backend.entity.SalaryHeader;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.SalaryDetailRepository;
import com.techno.backend.repository.SalaryHeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating payroll reports.
 *
 * Implements 3 payroll reports:
 * 1. Monthly Payroll Summary - Overview of all employees' payroll for a month
 * 2. Employee Payslip - Detailed payslip for individual employee
 * 3. Payroll Variance Report - Month-over-month comparison
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollReportService {

    private final SalaryHeaderRepository salaryHeaderRepository;
    private final SalaryDetailRepository salaryDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    /**
     * Generate Monthly Payroll Summary Report.
     *
     * Shows all employees' payroll summary for a specific month:
     * - Employee name and number
     * - Department
     * - Gross salary
     * - Total allowances
     * - Total deductions
     * - Net salary
     * - Status
     *
     * @param request Report request with month filter (YYYY-MM)
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateMonthlyPayrollSummary(ReportRequest request) {
        log.info("Generating Monthly Payroll Summary for month: {}", request.getMonth());

        // Validate month parameter
        if (request.getMonth() == null || request.getMonth().trim().isEmpty()) {
            throw new IllegalArgumentException("Ù…Ø¹Ø§Ù…Ù„ Ø§Ù„Ø´Ù‡Ø± Ù…Ø·Ù„ÙˆØ¨ (Ø§Ù„ØªÙ†Ø³ÙŠÙ‚: YYYY-MM)");
        }

        // Get all salary headers for the month
        List<SalaryHeader> salaries = salaryHeaderRepository
                .findAllLatestBySalaryMonth(request.getMonth());

        log.info("Found {} payroll records for month {}", salaries.size(), request.getMonth());

        // Build report data
        String title = "Ù…Ù„Ø®Øµ Ø§Ù„Ø±ÙˆØ§ØªØ¨ Ø§Ù„Ø´Ù‡Ø±ÙŠØ© - " + request.getMonth();
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„Ù‚Ø³Ù…",
                "Ø§Ù„Ø±Ø§ØªØ¨ Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠ",
                "Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø¨Ø¯Ù„Ø§Øª",
                "Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø®ØµÙˆÙ…Ø§Øª",
                "ØµØ§ÙÙŠ Ø§Ù„Ø±Ø§ØªØ¨",
                "Ø§Ù„Ø­Ø§Ù„Ø©"
        );

        List<List<Object>> data = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (SalaryHeader salary : salaries) {
            Employee employee = employeeRepository.findById(salary.getEmployeeNo()).orElse(null);

            List<Object> row = Arrays.asList(
                    salary.getEmployeeNo(),
                    employee != null ? employee.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    employee != null && employee.getPrimaryDeptCode() != null ?
                        "Ù‚Ø³Ù… " + employee.getPrimaryDeptCode() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                    salary.getGrossSalary(),
                    salary.getTotalAllowances(),
                    salary.getTotalDeductions(),
                    salary.getNetSalary(),
                    getStatusDescription(salary.getTransStatus())
            );
            data.add(row);

            // Accumulate totals
            totalGross = totalGross.add(salary.getGrossSalary());
            totalAllowances = totalAllowances.add(salary.getTotalAllowances());
            totalDeductions = totalDeductions.add(salary.getTotalDeductions());
            totalNet = totalNet.add(salary.getNetSalary());
        }

        // Add totals row
        data.add(Arrays.asList(
                "",
                "TOTAL",
                "",
                totalGross,
                totalAllowances,
                totalDeductions,
                totalNet,
                ""
        ));

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Month", request.getMonth());
        metadata.put("Total Employees", salaries.size());
        metadata.put("Total Gross", totalGross);
        metadata.put("Total Net", totalNet);
        metadata.put("Generated On", java.time.LocalDate.now());

        // Generate report
        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Employee Payslip Report.
     *
     * Detailed payslip for a single employee showing:
     * - Employee information
     * - Salary breakdown by transaction type
     * - Allowances and deductions details
     * - Gross and net salary
     *
     * @param request Report request with employeeNo and month
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateEmployeePayslip(ReportRequest request) {
        log.info("Generating Employee Payslip for employee: {}, month: {}",
                request.getEmployeeNo(), request.getMonth());

        // Validate parameters
        if (request.getEmployeeNo() == null) {
            throw new IllegalArgumentException("Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨");
        }
        if (request.getMonth() == null || request.getMonth().trim().isEmpty()) {
            throw new IllegalArgumentException("Ù…Ø¹Ø§Ù…Ù„ Ø§Ù„Ø´Ù‡Ø± Ù…Ø·Ù„ÙˆØ¨ (Ø§Ù„ØªÙ†Ø³ÙŠÙ‚: YYYY-MM)");
        }

        // Get employee
        Employee employee = employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + request.getEmployeeNo()));

        // Get salary header
        SalaryHeader salary = salaryHeaderRepository
                .findLatestByEmployeeAndMonth(request.getEmployeeNo(), request.getMonth())
                .orElseThrow(() -> new RuntimeException(
                        "No payroll found for employee " + request.getEmployeeNo() + " in month " + request.getMonth()));

        // Get salary details
        List<SalaryDetail> details = salaryDetailRepository.findBySalaryIdOrderByLineNoAsc(salary.getSalaryId());

        // Build report data
        String title = "Ø¥ÙŠØµØ§Ù„ Ø±Ø§ØªØ¨ - " + employee.getEmployeeName() + " - " + request.getMonth();
        List<String> headers = Arrays.asList(
                "Ø§Ù„Ù†ÙˆØ¹",
                "Ø§Ù„ÙˆØµÙ",
                "Ø§Ù„Ù…Ø¨Ù„Øº"
        );

        List<List<Object>> data = new ArrayList<>();

        // Add gross salary breakdown
        details.stream()
                .filter(d -> "A".equals(d.getTransCategory()))
                .forEach(detail -> {
                    data.add(Arrays.asList(
                            "Allowance",
                            "Type " + detail.getTransTypeCode(),
                            detail.getTransAmount()
                    ));
                });

        // Add deductions
        details.stream()
                .filter(d -> "D".equals(d.getTransCategory()))
                .forEach(detail -> {
                    data.add(Arrays.asList(
                            "Deduction",
                            "Type " + detail.getTransTypeCode(),
                            detail.getTransAmount()
                    ));
                });

        // Add summary rows
        data.add(Arrays.asList("", "", ""));
        data.add(Arrays.asList("", "Gross Salary", salary.getGrossSalary()));
        data.add(Arrays.asList("", "Total Allowances", salary.getTotalAllowances()));
        data.add(Arrays.asList("", "Total Deductions", salary.getTotalDeductions()));
        data.add(Arrays.asList("", "NET SALARY", salary.getNetSalary()));

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Employee No", employee.getEmployeeNo());
        metadata.put("Employee Name", employee.getEmployeeName());
        metadata.put("Department", employee.getPrimaryDeptCode());
        metadata.put("Month", request.getMonth());
        metadata.put("Status", getStatusDescription(salary.getTransStatus()));
        metadata.put("Calculation Date", salary.getCalculationDate());

        // Generate report
        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Payroll Variance Report.
     *
     * Month-over-month comparison showing changes in payroll:
     * - Employee-wise variance
     * - Gross salary changes
     * - Net salary changes
     * - Percentage changes
     *
     * @param request Report request with startDate and endDate (month range)
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generatePayrollVarianceReport(ReportRequest request) {
        log.info("Generating Payroll Variance Report");

        // Validate parameters - need two months for comparison
        if (request.getAdditionalFilters() == null ||
            !request.getAdditionalFilters().containsKey("previousMonth") ||
            !request.getAdditionalFilters().containsKey("currentMonth")) {
            throw new IllegalArgumentException("ÙƒÙ„Ø§ Ù…Ù† Ø§Ù„Ø´Ù‡Ø± Ø§Ù„Ø³Ø§Ø¨Ù‚ ÙˆØ§Ù„Ø´Ù‡Ø± Ø§Ù„Ø­Ø§Ù„ÙŠ Ù…Ø·Ù„ÙˆØ¨Ø§Ù†");
        }

        String previousMonth = request.getAdditionalFilters().get("previousMonth").toString();
        String currentMonth = request.getAdditionalFilters().get("currentMonth").toString();

        // Get salaries for both months
        List<SalaryHeader> previousSalaries = salaryHeaderRepository
                .findAllLatestBySalaryMonth(previousMonth);
        List<SalaryHeader> currentSalaries = salaryHeaderRepository
                .findAllLatestBySalaryMonth(currentMonth);

        // Create maps for quick lookup
        Map<Long, SalaryHeader> previousMap = previousSalaries.stream()
                .collect(Collectors.toMap(SalaryHeader::getEmployeeNo, s -> s));
        Map<Long, SalaryHeader> currentMap = currentSalaries.stream()
                .collect(Collectors.toMap(SalaryHeader::getEmployeeNo, s -> s));

        // Build report data
        String title = "ØªÙ‚Ø±ÙŠØ± Ø§Ù„ØªØ¨Ø§ÙŠÙ† ÙÙŠ Ø§Ù„Ø±ÙˆØ§ØªØ¨ - " + previousMonth + " Ù…Ù‚Ø§Ø¨Ù„ " + currentMonth;
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ù„ØµØ§ÙÙŠ Ø§Ù„Ø³Ø§Ø¨Ù‚",
                "Ø§Ù„ØµØ§ÙÙŠ Ø§Ù„Ø­Ø§Ù„ÙŠ",
                "Ø§Ù„ØªØ¨Ø§ÙŠÙ†",
                "Ù†Ø³Ø¨Ø© Ø§Ù„ØªØ¨Ø§ÙŠÙ† %"
        );

        List<List<Object>> data = new ArrayList<>();

        // Get all unique employee numbers
        Set<Long> allEmployees = new HashSet<>();
        allEmployees.addAll(previousMap.keySet());
        allEmployees.addAll(currentMap.keySet());

        for (Long employeeNo : allEmployees) {
            Employee employee = employeeRepository.findById(employeeNo).orElse(null);

            SalaryHeader prev = previousMap.get(employeeNo);
            SalaryHeader curr = currentMap.get(employeeNo);

            BigDecimal prevNet = prev != null ? prev.getNetSalary() : BigDecimal.ZERO;
            BigDecimal currNet = curr != null ? curr.getNetSalary() : BigDecimal.ZERO;
            BigDecimal variance = currNet.subtract(prevNet);

            // Calculate percentage change
            String variancePercent = "ØºÙŠØ± Ù…ØªØ§Ø­";
            if (prevNet.compareTo(BigDecimal.ZERO) > 0) {
                double percent = variance.divide(prevNet, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                variancePercent = String.format("%.2f%%", percent);
            }

            data.add(Arrays.asList(
                    employeeNo,
                    employee != null ? employee.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    prevNet,
                    currNet,
                    variance,
                    variancePercent
            ));
        }

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Previous Month", previousMonth);
        metadata.put("Current Month", currentMonth);
        metadata.put("Employees Compared", allEmployees.size());
        metadata.put("Generated On", java.time.LocalDate.now());

        // Generate report
        return generateReport(title, headers, data, metadata, request);
    }

    // ==================== Helper Methods ====================

    /**
     * Generate report in requested format.
     */
    private byte[] generateReport(String title, List<String> headers,
                                  List<List<Object>> data, Map<String, Object> metadata,
                                  ReportRequest request) {
        if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
            return excelReportService.generateReport(title, headers, data, metadata);
        } else {
            return pdfReportService.generateReport(title, headers, data, metadata);
        }
    }

    /**
     * Get status description.
     */
    private String getStatusDescription(String status) {
        return switch (status) {
            case "A" -> "Ù…ÙˆØ§ÙÙ‚ Ø¹Ù„ÙŠÙ‡";
            case "N" -> "Ù…Ø¹Ù„Ù‚";
            case "R" -> "Ù…Ø±ÙÙˆØ¶";
            default -> status;
        };
    }
}

