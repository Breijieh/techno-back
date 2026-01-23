package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Loan;
import com.techno.backend.entity.LoanInstallment;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.LoanRepository;
import com.techno.backend.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Loan Reports.
 *
 * Endpoints:
 * - GET /api/reports/loans/installments?month=YYYY-MM - Loan Installments Report
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanReportController {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Get loan installments report for a specific month.
     * Used for the loan installments report page.
     *
     * GET /api/reports/loans/installments?month=2025-12
     *
     * @param month Month in YYYY-MM format
     * @return List of installments for the month
     */
    @GetMapping("/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<InstallmentScheduleResponse>>> getLoanInstallmentsReport(
            @RequestParam String month) {

        log.info("GET /api/reports/loans/installments?month={}", month);

        // Parse month (YYYY-MM) to startDate and endDate
        LocalDate startDate;
        LocalDate endDate;
        try {
            String[] parts = month.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("ØªÙ†Ø³ÙŠÙ‚ Ø§Ù„Ø´Ù‡Ø± ØºÙŠØ± ØµØ§Ù„Ø­. Ø§Ù„Ù…ØªÙˆÙ‚Ø¹ YYYY-MMØŒ Ø§Ù„Ù…Ø³ØªÙ„Ù…: " + month);
            }
            int year = Integer.parseInt(parts[0]);
            int monthValue = Integer.parseInt(parts[1]);
            
            if (monthValue < 1 || monthValue > 12) {
                throw new IllegalArgumentException("Ù‚ÙŠÙ…Ø© Ø§Ù„Ø´Ù‡Ø± ØºÙŠØ± ØµØ§Ù„Ø­Ø©. ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† Ø¨ÙŠÙ† 1 Ùˆ 12ØŒ Ø§Ù„Ù…Ø³ØªÙ„Ù…: " + monthValue);
            }
            
            // First day of the month
            startDate = LocalDate.of(year, monthValue, 1);
            // Last day of the month
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        } catch (NumberFormatException e) {
            log.error("Invalid month format: {}", month, e);
            throw new IllegalArgumentException("ØªÙ†Ø³ÙŠÙ‚ Ø§Ù„Ø´Ù‡Ø± ØºÙŠØ± ØµØ§Ù„Ø­. Ø§Ù„Ù…ØªÙˆÙ‚Ø¹ YYYY-MMØŒ Ø§Ù„Ù…Ø³ØªÙ„Ù…: " + month);
        } catch (Exception e) {
            log.error("Error parsing month: {}", month, e);
            throw new IllegalArgumentException("ØªÙ†Ø³ÙŠÙ‚ Ø§Ù„Ø´Ù‡Ø± ØºÙŠØ± ØµØ§Ù„Ø­. Ø§Ù„Ù…ØªÙˆÙ‚Ø¹ YYYY-MMØŒ Ø§Ù„Ù…Ø³ØªÙ„Ù…: " + month);
        }

        // Get all installments for the month (use large page size to get all records)
        // Use database column name for sorting in native query
        Pageable pageable = PageRequest.of(0, 10000, Sort.by("due_date").ascending());
        Page<LoanInstallment> installmentPage = loanService.getAllInstallments(
                null, null, null, startDate, endDate, pageable
        );

        // Convert to InstallmentScheduleResponse with employee names
        List<InstallmentScheduleResponse> response = installmentPage.getContent().stream()
                .map(this::toInstallmentScheduleResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ ØªÙ‚Ø±ÙŠØ± Ø£Ù‚Ø³Ø§Ø· Ø§Ù„Ù‚Ø±Ø¶ Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Convert LoanInstallment entity to InstallmentScheduleResponse DTO.
     *
     * @param installment Installment entity
     * @return Installment schedule response DTO with enriched employee names
     */
    private InstallmentScheduleResponse toInstallmentScheduleResponse(LoanInstallment installment) {
        // Get loan to access employee information
        Loan loan = loanRepository.findById(installment.getLoanId())
                .orElse(null);

        // Get employee name
        Long employeeNo = null;
        String employeeName = null;
        if (loan != null) {
            employeeNo = loan.getEmployeeNo();
            employeeName = employeeRepository.findById(employeeNo)
                    .map(emp -> emp.getEmployeeName() != null ? emp.getEmployeeName() : emp.getEmployeeName())
                    .orElse("Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
        }

        // Determine postponedTo date - if status is POSTPONED, the dueDate is the new date
        LocalDate postponedTo = "POSTPONED".equals(installment.getPaymentStatus()) 
                ? installment.getDueDate() 
                : null;

        return new InstallmentScheduleResponse(
                installment.getInstallmentId(),
                installment.getLoanId(),
                installment.getInstallmentNo(),
                employeeNo,
                employeeName,
                installment.getDueDate(),
                installment.getInstallmentAmount(),
                installment.getPaymentStatus(),
                installment.getPaidDate(),
                installment.getPaidAmount(),
                postponedTo,
                installment.getSalaryMonth()
        );
    }

    /**
     * Response DTO for installment schedule with employee information.
     * Matches the structure from LoanController for consistency.
     */
    public record InstallmentScheduleResponse(
            Long installmentId,
            Long loanId,
            Integer installmentNo,
            Long employeeNo,
            String employeeName,
            LocalDate dueDate,
            java.math.BigDecimal installmentAmount,
            String paymentStatus,
            LocalDate paidDate,
            java.math.BigDecimal paidAmount,
            LocalDate postponedTo, // New due date if POSTPONED, null otherwise
            String salaryMonth
    ) {}
}


