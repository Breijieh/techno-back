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
 * Service for generating project-related reports.
 *
 * Implements 4 project reports:
 * 1. Project Financial Status - Total value, payments received/made, outstanding
 * 2. Payment Schedule - All scheduled payments (incoming + outgoing)
 * 3. Labor Allocation - Employee assignments, vacant workers, over-allocated
 * 4. Transfer History - All approved transfers
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectReportService {

    private final ProjectRepository projectRepository;
    private final ProjectPaymentRequestRepository paymentRequestRepository;
    private final ProjectPaymentProcessRepository paymentProcessRepository;
    private final ProjectDuePaymentRepository duePaymentRepository;
    private final ProjectLaborAssignmentRepository laborAssignmentRepository;
    private final ProjectTransferRequestRepository transferRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    /**
     * Generate Project Financial Status Report.
     *
     * Shows financial overview for projects:
     * - Total project value
     * - Payments received
     * - Payments made
     * - Outstanding balance
     *
     * @param request Report request with optional projectCode filter
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateProjectFinancialStatus(ReportRequest request) {
        log.info("Generating Project Financial Status Report");

        List<Project> projects;
        if (request.getProjectCode() != null) {
            Optional<Project> projectOpt = projectRepository.findById(request.getProjectCode());
            projects = projectOpt.map(Collections::singletonList).orElse(Collections.emptyList());
        } else {
            projects = projectRepository.findAll();
        }

        String title = "ØªÙ‚Ø±ÙŠØ± Ø§Ù„Ø­Ø§Ù„Ø© Ø§Ù„Ù…Ø§Ù„ÙŠØ© Ù„Ù„Ù…Ø´Ø§Ø±ÙŠØ¹";
        List<String> headers = Arrays.asList(
                "Ø±Ù…Ø² Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "Ø§Ø³Ù… Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "Ø§Ù„Ù‚ÙŠÙ…Ø© Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠØ©",
                "Ø§Ù„Ù…Ø¯ÙÙˆØ¹Ø§Øª Ø§Ù„Ù…Ø³ØªÙ„Ù…Ø©",
                "Ø§Ù„Ù…Ø¯ÙÙˆØ¹Ø§Øª Ø§Ù„Ù…Ù‚Ø¯Ù…Ø©",
                "Ø§Ù„Ù…Ø³ØªØ­Ù‚Ø§Øª",
                "Ø§Ù„Ø­Ø§Ù„Ø©"
        );

        List<List<Object>> data = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalMade = BigDecimal.ZERO;

        for (Project project : projects) {
            // Calculate payments received (from project due payments)
            BigDecimal received = duePaymentRepository.findByProjectCodeOrderBySequenceNoAsc(project.getProjectCode())
                    .stream()
                    .filter(p -> "PAID".equals(p.getPaymentStatus()))
                    .map(ProjectDuePayment::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate payments made (from payment processes - linked through payment requests)
            BigDecimal made = paymentProcessRepository.findAll()
                    .stream()
                    .filter(p -> p.getPaymentRequest() != null && 
                            project.getProjectCode().equals(p.getPaymentRequest().getProjectCode()))
                    .map(ProjectPaymentProcess::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outstanding = project.getTotalProjectAmount()
                    .subtract(received)
                    .add(made);

            data.add(Arrays.asList(
                    project.getProjectCode(),
                    project.getProjectName(),
                    project.getTotalProjectAmount(),
                    received,
                    made,
                    outstanding,
                    project.getProjectStatus()
            ));

            totalValue = totalValue.add(project.getTotalProjectAmount());
            totalReceived = totalReceived.add(received);
            totalMade = totalMade.add(made);
        }

        // Add totals row
        data.add(Arrays.asList(
                "",
                "TOTAL",
                totalValue,
                totalReceived,
                totalMade,
                totalValue.subtract(totalReceived).add(totalMade),
                ""
        ));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Total Projects", projects.size());
        metadata.put("Generated On", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Payment Schedule Report.
     *
     * Shows all scheduled payments for a project.
     *
     * @param request Report request with projectCode
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generatePaymentSchedule(ReportRequest request) {
        log.info("Generating Payment Schedule Report for project: {}", request.getProjectCode());

        if (request.getProjectCode() == null) {
            throw new IllegalArgumentException("Ø±Ù…Ø² Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ù…Ø·Ù„ÙˆØ¨");
        }

        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new IllegalArgumentException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Get due payments (incoming)
        List<ProjectDuePayment> duePayments = duePaymentRepository.findByProjectCodeOrderBySequenceNoAsc(request.getProjectCode());

        // Get payment requests (outgoing)
        List<ProjectPaymentRequest> paymentRequests = paymentRequestRepository.findByProjectCode(request.getProjectCode());

        String title = "Ø¬Ø¯ÙˆÙ„ Ø§Ù„Ù…Ø¯ÙÙˆØ¹Ø§Øª - " + project.getProjectName();
        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø§Ù„Ù†ÙˆØ¹",
                "Ø§Ù„ÙˆØµÙ",
                "Ø§Ù„Ù…Ø¨Ù„Øº",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ø³ØªØ­Ù‚Ø§Ù‚"
        );

        List<List<Object>> data = new ArrayList<>();

        // Add due payments (incoming)
        for (ProjectDuePayment payment : duePayments) {
            data.add(Arrays.asList(
                    payment.getPaymentDate(),
                    "INCOMING",
                    "Project Payment",
                    payment.getDueAmount(),
                    payment.getPaymentStatus(),
                    payment.getDueDate()
            ));
        }

        // Add payment requests (outgoing)
        for (ProjectPaymentRequest request1 : paymentRequests) {
            data.add(Arrays.asList(
                    request1.getRequestDate(),
                    "OUTGOING",
                    "Supplier " + request1.getSupplierCode(), // Using supplier code since name not available
                    request1.getPaymentAmount(),
                    request1.getTransStatus(),
                    request1.getRequestDate() // Using request date as due date is not available
            ));
        }

        // Sort by date
        data.sort((a, b) -> {
            LocalDate dateA = (LocalDate) a.get(0);
            LocalDate dateB = (LocalDate) b.get(0);
            return dateA.compareTo(dateB);
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Project", project.getProjectName());
        metadata.put("Total Incoming", duePayments.size());
        metadata.put("Total Outgoing", paymentRequests.size());
        metadata.put("Generated On", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Labor Allocation Report.
     *
     * Shows employee assignments and identifies vacant/over-allocated workers.
     *
     * @param request Report request with date range and optional projectCode
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLaborAllocation(ReportRequest request) {
        log.info("Generating Labor Allocation Report");

        validateDateRange(request);

        List<ProjectLaborAssignment> assignments;
        if (request.getProjectCode() != null) {
            assignments = laborAssignmentRepository.findByProjectCode(request.getProjectCode());
        } else {
            assignments = laborAssignmentRepository.findAll();
        }

        // Filter by date range
        assignments = assignments.stream()
                .filter(a -> isWithinDateRange(a.getStartDate(), request.getStartDate(), request.getEndDate()) ||
                           isWithinDateRange(a.getEndDate(), request.getStartDate(), request.getEndDate()))
                .collect(Collectors.toList());

        String title = "ØªÙ‚Ø±ÙŠØ± ØªÙˆØ²ÙŠØ¹ Ø§Ù„Ø¹Ù…Ø§Ù„Ø©";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø±Ù…Ø² Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "Ø§Ø³Ù… Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "Ø§Ù„Ù…Ø¹Ø¯Ù„ Ø§Ù„ÙŠÙˆÙ…ÙŠ"
        );

        List<List<Object>> data = new ArrayList<>();
        Map<Long, Integer> employeeAssignmentCount = new HashMap<>();

        for (ProjectLaborAssignment assignment : assignments) {
            Employee employee = employeeRepository.findById(assignment.getEmployeeNo()).orElse(null);
            Project project = projectRepository.findById(assignment.getProjectCode()).orElse(null);

            data.add(Arrays.asList(
                    assignment.getEmployeeNo(),
                    employee != null ? employee.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    assignment.getProjectCode(),
                    project != null ? project.getProjectName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    assignment.getStartDate(),
                    assignment.getEndDate(),
                    assignment.getAssignmentStatus(),
                    assignment.getDailyRate()
            ));

            // Count assignments per employee
            employeeAssignmentCount.merge(assignment.getEmployeeNo(), 1, Integer::sum);
        }

        // Identify over-allocated employees (multiple active assignments)
        List<String> overAllocated = employeeAssignmentCount.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> {
                    Employee emp = employeeRepository.findById(e.getKey()).orElse(null);
                    return emp != null ? emp.getEmployeeName() : "Ø§Ù„Ù…ÙˆØ¸Ù " + e.getKey();
                })
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Total Assignments", assignments.size());
        metadata.put("Over-allocated Employees", overAllocated.size());
        metadata.put("Generated On", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Transfer History Report.
     *
     * Shows all approved transfer requests.
     *
     * @param request Report request with date range and optional projectCode
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateTransferHistory(ReportRequest request) {
        log.info("Generating Transfer History Report");

        validateDateRange(request);

        List<ProjectTransferRequest> transfers;
        if (request.getProjectCode() != null) {
            transfers = transferRequestRepository.findByFromProjectCode(request.getProjectCode());
            transfers.addAll(transferRequestRepository.findByToProjectCode(request.getProjectCode()));
        } else {
            transfers = transferRequestRepository.findAll();
        }

        // Filter by date range and approved status
        transfers = transfers.stream()
                .filter(t -> "A".equals(t.getTransStatus()))
                .filter(t -> isWithinDateRange(t.getTransferDate(), request.getStartDate(), request.getEndDate()))
                .collect(Collectors.toList());

        String title = "ØªÙ‚Ø±ÙŠØ± Ø³Ø¬Ù„ Ø§Ù„Ù†Ù‚Ù„Ø§Øª";
        List<String> headers = Arrays.asList(
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ù†Ù‚Ù„",
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ù…Ù† Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "Ø¥Ù„Ù‰ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹",
                "Ø³Ø¨Ø¨ Ø§Ù„Ù†Ù‚Ù„",
                "Ø§Ù„Ø­Ø§Ù„Ø©"
        );

        List<List<Object>> data = new ArrayList<>();

        for (ProjectTransferRequest transfer : transfers) {
            Employee employee = employeeRepository.findById(transfer.getEmployeeNo()).orElse(null);
            Project fromProject = projectRepository.findById(transfer.getFromProjectCode()).orElse(null);
            Project toProject = projectRepository.findById(transfer.getToProjectCode()).orElse(null);

            data.add(Arrays.asList(
                    transfer.getTransferDate(),
                    transfer.getEmployeeNo(),
                    employee != null ? employee.getEmployeeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    fromProject != null ? fromProject.getProjectName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    toProject != null ? toProject.getProjectName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ",
                    transfer.getTransferReason(),
                    transfer.getTransStatus()
            ));
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Total Transfers", transfers.size());
        metadata.put("Generated On", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    // Helper methods

    private void validateDateRange(ReportRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ ÙˆØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ù…Ø·Ù„ÙˆØ¨Ø§Ù†");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡");
        }
    }

    private boolean isWithinDateRange(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private byte[] generateReport(String title, List<String> headers, List<List<Object>> data,
                                  Map<String, Object> metadata, ReportRequest request) {
        if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
            return excelReportService.generateReport(title, headers, data, metadata);
        } else {
            return pdfReportService.generateReport(title, headers, data, metadata);
        }
    }
}


