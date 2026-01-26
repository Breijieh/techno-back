package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.*;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for payroll calculation implementing the 8-step process.
 *
 * CRITICAL: This is the core payroll calculation engine for Phase 4.
 *
 * Calculation Steps (from DOCUMENT.MD):
 * 1. Get base salary from employee
 * 2. Pro-rate for hire date (if hired during month)
 * 3. Pro-rate for termination (if terminated during month)
 * 4. Breakdown salary into components by nationality percentages
 * 5. Add monthly allowances (fixed + variable + overtime)
 * 6. Add monthly deductions (fixed + variable)
 * 7. Add loan installments (and mark them as PAID)
 * 8. Calculate totals (net = gross + allowances - deductions)
 * 9. Save to SALARY_HEADER with SALARY_DETAIL lines
 *
 * Features:
 * - Pro-rating for partial months
 * - Nationality-based salary breakdown
 * - Automatic loan payment processing
 * - Versioning support for recalculations
 * - Audit trail
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Payroll System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollCalculationService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHeaderRepository salaryHeaderRepository;
    private final SalaryDetailRepository salaryDetailRepository;
    private final SalaryBreakdownPercentageRepository salaryBreakdownPercentageRepository;
    private final EmpMonthlyAllowanceRepository allowanceRepository;
    private final EmpMonthlyDeductionRepository deductionRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanRepository loanRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Calculate payroll for a single employee for a specific month.
     *
     * Implements the complete 8-step calculation process.
     *
     * @param employeeNo  Employee number
     * @param salaryMonth Salary month in YYYY-MM format (e.g., "2025-11")
     * @return Created salary header with all details
     */
    @Transactional
    public SalaryHeader calculatePayrollForEmployee(Long employeeNo, String salaryMonth) {
        log.info("Starting payroll calculation for employee {} for month {}", employeeNo, salaryMonth);

        // Load employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        // Validate employee eligibility
        validateEmployeeEligibility(employee);

        // Parse salary month
        YearMonth yearMonth = YearMonth.parse(salaryMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Check if salary already exists for this month
        var existing = salaryHeaderRepository.findLatestByEmployeeAndMonth(employeeNo, salaryMonth);
        if (existing.isPresent()) {
            log.warn("Salary already calculated for employee {} for month {}. Use recalculate instead.", employeeNo,
                    salaryMonth);
            throw new RuntimeException(
                    "Ø§Ù„Ø±Ø§ØªØ¨ Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù‡Ø°Ø§ Ø§Ù„Ù…ÙˆØ¸Ù ÙˆØ§Ù„Ø´Ù‡Ø±. Ø§Ø³ØªØ®Ø¯Ù… Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ø­Ø³Ø§Ø¨ Ù„Ø¥Ù†Ø´Ø§Ø¡ Ù†Ø³Ø®Ø© Ø¬Ø¯ÙŠØ¯Ø©.");
        }

        // STEP 1-3: Calculate pro-rated gross salary
        BigDecimal grossSalary = calculateProRatedGrossSalary(employee, monthStart, monthEnd);
        log.info("Gross salary calculated: {}", grossSalary);

        // Initialize approval workflow for payroll
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                "PAYROLL",
                employeeNo,
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());
        log.info("Approval workflow initialized - Next approver: {}, Level: {}",
                approvalInfo.getNextApproval(), approvalInfo.getNextAppLevel());

        // Create salary header
        SalaryHeader salaryHeader = SalaryHeader.builder()
                .employeeNo(employeeNo)
                .salaryMonth(salaryMonth)
                .salaryVersion(1)
                .isLatest("Y")
                .grossSalary(grossSalary)
                .totalAllowances(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.ZERO)
                .netSalary(grossSalary)
                .salaryType("W") // Regular work salary
                .calculationDate(LocalDate.now())
                .transStatus(approvalInfo.getTransStatus()) // Needs approval
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .build();

        // Save header first to get ID
        salaryHeader = salaryHeaderRepository.save(salaryHeader);
        log.info("Salary header created with ID: {}", salaryHeader.getSalaryId());

        // Line number counter
        AtomicInteger lineNo = new AtomicInteger(1);

        // STEP 4: Breakdown salary into components by nationality
        addSalaryBreakdownDetails(salaryHeader, employee, grossSalary, lineNo);

        // STEP 5: Add monthly allowances
        addMonthlyAllowances(salaryHeader, employee, yearMonth, lineNo);

        // STEP 6: Add monthly deductions
        addMonthlyDeductions(salaryHeader, employee, yearMonth, lineNo);

        // STEP 7: Add loan installments and mark them as paid
        processLoanInstallments(salaryHeader, employee, yearMonth, lineNo);

        // STEP 8: Calculate totals
        salaryHeader.recalculateTotals();
        salaryHeader = salaryHeaderRepository.save(salaryHeader);

        log.info("Payroll calculation completed for employee {}. Gross: {}, Allowances: {}, Deductions: {}, Net: {}",
                employeeNo, salaryHeader.getGrossSalary(), salaryHeader.getTotalAllowances(),
                salaryHeader.getTotalDeductions(), salaryHeader.getNetSalary());

        // Publish payroll calculated notification
        publishPayrollCalculatedNotification(salaryHeader, employee);

        return salaryHeader;
    }

    /**
     * Calculate payroll for all eligible employees in a month.
     *
     * @param salaryMonth Salary month in YYYY-MM format
     * @return List of created salary headers
     */
    @Transactional
    public List<SalaryHeader> calculatePayrollForAllEmployees(String salaryMonth) {
        log.info("Starting payroll calculation for all employees for month {}", salaryMonth);

        // Get all active employees with TECHNO contract
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(this::isEligibleForPayroll)
                .toList();

        log.info("Found {} eligible employees for payroll", employees.size());

        return employees.stream()
                .map(emp -> {
                    try {
                        return calculatePayrollForEmployee(emp.getEmployeeNo(), salaryMonth);
                    } catch (Exception e) {
                        log.error("Failed to calculate payroll for employee {}: {}",
                                emp.getEmployeeNo(), e.getMessage(), e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Recalculate payroll (create new version).
     *
     * Used when errors found after approval or adjustments needed.
     *
     * @param employeeNo  Employee number
     * @param salaryMonth Salary month
     * @param reason      Reason for recalculation
     * @return New salary header version
     */
    @Transactional
    public SalaryHeader recalculatePayroll(Long employeeNo, String salaryMonth, String reason) {
        log.info("Recalculating payroll for employee {} for month {}. Reason: {}",
                employeeNo, salaryMonth, reason);

        // Find current latest version
        SalaryHeader currentVersion = salaryHeaderRepository
                .findLatestByEmployeeAndMonth(employeeNo, salaryMonth)
                .orElseThrow(() -> new RuntimeException(
                        "Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±Ø§ØªØ¨ Ù…ÙˆØ¬ÙˆØ¯ Ù„Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ø­Ø³Ø§Ø¨"));

        // Mark current as not latest
        currentVersion.setIsLatest("N");
        salaryHeaderRepository.save(currentVersion);

        // Delete existing salary (will create new one)
        salaryHeaderRepository.findLatestByEmployeeAndMonth(employeeNo, salaryMonth)
                .ifPresent(h -> {
                    // Temporarily allow recalculation by removing the check
                });

        // Calculate new version
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        YearMonth yearMonth = YearMonth.parse(salaryMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        BigDecimal grossSalary = calculateProRatedGrossSalary(employee, monthStart, monthEnd);

        SalaryHeader newVersion = SalaryHeader.builder()
                .employeeNo(employeeNo)
                .salaryMonth(salaryMonth)
                .salaryVersion(currentVersion.getSalaryVersion() + 1)
                .isLatest("Y")
                .grossSalary(grossSalary)
                .totalAllowances(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.ZERO)
                .netSalary(grossSalary)
                .salaryType(currentVersion.getSalaryType())
                .calculationDate(LocalDate.now())
                .transStatus("N")
                .recalculationReason(reason)
                .build();

        newVersion = salaryHeaderRepository.save(newVersion);

        AtomicInteger lineNo = new AtomicInteger(1);
        addSalaryBreakdownDetails(newVersion, employee, grossSalary, lineNo);
        addMonthlyAllowances(newVersion, employee, yearMonth, lineNo);
        addMonthlyDeductions(newVersion, employee, yearMonth, lineNo);
        processLoanInstallments(newVersion, employee, yearMonth, lineNo);

        newVersion.recalculateTotals();
        newVersion = salaryHeaderRepository.save(newVersion);

        log.info("Recalculation completed. New version: {}", newVersion.getSalaryVersion());

        // Publish payroll recalculated notification
        publishPayrollRecalculatedNotification(newVersion, employee, reason);

        return newVersion;
    }

    // ==================== Private Helper Methods ====================

    /**
     * STEPS 1-3: Calculate pro-rated gross salary.
     *
     * Handles:
     * - Normal full month salary
     * - Pro-rating for employees hired during the month
     * - Pro-rating for employees terminated during the month
     *
     * Formula: (Monthly Salary Ã—Days Worked) Ã· 30
     */
    private BigDecimal calculateProRatedGrossSalary(Employee employee, LocalDate monthStart, LocalDate monthEnd) {
        BigDecimal monthlySalary = employee.getMonthlySalary();
        int totalDaysInMonth = 30; // Always use 30 for calculation (as per spec)

        // Guard: Employee hired AFTER this month ends (Future hire)
        if (employee.getHireDate() != null && employee.getHireDate().isAfter(monthEnd)) {
            log.info("Employee hired in future ({}), returning 0 salary", employee.getHireDate());
            return BigDecimal.ZERO;
        }

        // Guard: Employee terminated BEFORE this month starts (Past employee)
        if (employee.getTerminationDate() != null && employee.getTerminationDate().isBefore(monthStart)) {
            log.info("Employee terminated in past ({}), returning 0 salary", employee.getTerminationDate());
            return BigDecimal.ZERO;
        }

        LocalDate actualStartDate = monthStart;
        LocalDate actualEndDate = monthEnd;

        // Pro-rate if hired during the month
        if (employee.getHireDate() != null &&
                employee.getHireDate().isAfter(monthStart) &&
                !employee.getHireDate().isAfter(monthEnd)) {
            actualStartDate = employee.getHireDate();
            log.info("Employee hired on {} (during month), pro-rating salary", actualStartDate);
        }

        // Pro-rate if terminated during the month
        if (employee.getTerminationDate() != null &&
                !employee.getTerminationDate().isBefore(monthStart) &&
                employee.getTerminationDate().isBefore(monthEnd)) {
            actualEndDate = employee.getTerminationDate();
            log.info("Employee terminated on {} (during month), pro-rating salary", actualEndDate);
        }

        // CRITICAL VALIDATION: Ensure termination date is not before hire date
        if (actualEndDate.isBefore(actualStartDate)) {
            log.error("Invalid employee data: termination date ({}) is before hire date ({}) for employee {}. Returning 0 salary.",
                    actualEndDate, actualStartDate, employee.getEmployeeNo());
            return BigDecimal.ZERO;
        }

        // Check if full month (no pro-rating needed)
        if (actualStartDate.equals(monthStart) && actualEndDate.equals(monthEnd)) {
            log.info("Full month worked, returning full monthly salary: {}", monthlySalary);
            return monthlySalary;
        }

        // Calculate days worked
        long daysWorked = java.time.temporal.ChronoUnit.DAYS.between(actualStartDate, actualEndDate) + 1;

        // Pro-rate salary
        BigDecimal proRatedSalary = monthlySalary
                .multiply(BigDecimal.valueOf(daysWorked))
                .divide(BigDecimal.valueOf(totalDaysInMonth), 4, RoundingMode.HALF_UP);

        if (daysWorked < totalDaysInMonth) {
            log.info("Salary pro-rated: {} days worked out of 30. Salary: {} â†’ {}",
                    daysWorked, monthlySalary, proRatedSalary);
        }

        return proRatedSalary;
    }

    /**
     * STEP 4: Breakdown salary into components by nationality percentages.
     *
     * Reads percentages from SALARY_BREAKDOWN_PERCENTAGES table based on employee
     * category (Saudi/Foreign).
     * Creates one SALARY_DETAIL line for each component.
     */
    private void addSalaryBreakdownDetails(SalaryHeader header, Employee employee,
            BigDecimal grossSalary, AtomicInteger lineNo) {
        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownPercentageRepository
                .findByEmployeeCategory(employee.getEmployeeCategory());

        if (breakdowns.isEmpty()) {
            log.warn("No salary breakdown found for category {}. Using full amount as basic salary.",
                    employee.getEmployeeCategory());
            // Add full salary as one line
            SalaryDetail detail = SalaryDetail.builder()
                    .salaryId(header.getSalaryId())
                    .lineNo(lineNo.getAndIncrement())
                    .transTypeCode(1L) // Assuming 1 is basic salary type
                    .transAmount(grossSalary)
                    .transCategory("A") // Allowance
                    .build();
            header.addDetail(detail);
            salaryDetailRepository.save(detail);
            return;
        }

        for (SalaryBreakdownPercentage breakdown : breakdowns) {
            BigDecimal componentAmount = grossSalary
                    .multiply(breakdown.getSalaryPercentage())
                    .divide(BigDecimal.ONE, 4, RoundingMode.HALF_UP);

            SalaryDetail detail = SalaryDetail.builder()
                    .salaryId(header.getSalaryId())
                    .lineNo(lineNo.getAndIncrement())
                    .transTypeCode(breakdown.getTransTypeCode())
                    .transAmount(componentAmount)
                    .transCategory("A") // Allowance
                    .build();

            header.addDetail(detail);
            salaryDetailRepository.save(detail);

            log.debug("Added breakdown: type {} = {}", breakdown.getTransTypeCode(), componentAmount);
        }
    }

    /**
     * STEP 5: Add monthly allowances.
     *
     * Gets all active allowances for the employee and month.
     * Includes: fixed allowances, overtime, bonuses, etc.
     */
    private void addMonthlyAllowances(SalaryHeader header, Employee employee,
            YearMonth yearMonth, AtomicInteger lineNo) {
        LocalDate monthDate = yearMonth.atDay(15); // Mid-month for date checks

        List<EmpMonthlyAllowance> allowances = allowanceRepository
                .findActiveAllowancesForEmployeeOnDate(employee.getEmployeeNo(), monthDate);

        log.info("Found {} active allowances for employee {}", allowances.size(), employee.getEmployeeNo());

        for (EmpMonthlyAllowance allowance : allowances) {
            SalaryDetail detail = SalaryDetail.builder()
                    .salaryId(header.getSalaryId())
                    .lineNo(lineNo.getAndIncrement())
                    .transTypeCode(allowance.getTypeCode())
                    .transAmount(allowance.getAllowanceAmount())
                    .transCategory("A") // Allowance
                    .referenceTable("emp_monthly_allowances")
                    .referenceId(allowance.getTransactionNo())
                    .build();

            header.addDetail(detail);
            salaryDetailRepository.save(detail);

            log.debug("Added allowance: type {} = {}", allowance.getTypeCode(), allowance.getAllowanceAmount());
        }
    }

    /**
     * STEP 6: Add monthly deductions.
     *
     * Gets all active deductions for the employee and month.
     * Includes: insurance, fines, late penalties, absence deductions, etc.
     */
    private void addMonthlyDeductions(SalaryHeader header, Employee employee,
            YearMonth yearMonth, AtomicInteger lineNo) {
        LocalDate monthDate = yearMonth.atDay(15); // Mid-month for date checks

        List<EmpMonthlyDeduction> deductions = deductionRepository
                .findActiveDeductionsForEmployeeOnDate(employee.getEmployeeNo(), monthDate);

        log.info("Found {} active deductions for employee {}", deductions.size(), employee.getEmployeeNo());

        for (EmpMonthlyDeduction deduction : deductions) {
            SalaryDetail detail = SalaryDetail.builder()
                    .salaryId(header.getSalaryId())
                    .lineNo(lineNo.getAndIncrement())
                    .transTypeCode(deduction.getTypeCode())
                    .transAmount(deduction.getDeductionAmount())
                    .transCategory("D") // Deduction
                    .referenceTable("emp_monthly_deductions")
                    .referenceId(deduction.getTransactionNo())
                    .build();

            header.addDetail(detail);
            salaryDetailRepository.save(detail);

            log.debug("Added deduction: type {} = {}", deduction.getTypeCode(), deduction.getDeductionAmount());
        }
    }

    /**
     * STEP 7: Process loan installments.
     *
     * Gets unpaid installments due this month.
     * Adds them as deductions.
     * Marks installments as PAID.
     * Updates loan remaining balance.
     * Marks loan as inactive if fully paid.
     */
    private void processLoanInstallments(SalaryHeader header, Employee employee,
            YearMonth yearMonth, AtomicInteger lineNo) {
        List<LoanInstallment> unpaidInstallments = loanInstallmentRepository
                .findUnpaidInstallmentsForEmployeeInMonth(
                        employee.getEmployeeNo(),
                        yearMonth.getYear(),
                        yearMonth.getMonthValue());

        log.info("Found {} unpaid loan installments for employee {}", unpaidInstallments.size(),
                employee.getEmployeeNo());

        for (LoanInstallment installment : unpaidInstallments) {
            // Add to salary details as deduction
            SalaryDetail detail = SalaryDetail.builder()
                    .salaryId(header.getSalaryId())
                    .lineNo(lineNo.getAndIncrement())
                    .transTypeCode(30L) // Fixed: 30 is loan installment type code
                    .transAmount(installment.getInstallmentAmount())
                    .transCategory("D") // Deduction
                    .referenceTable("loan_installments")
                    .referenceId(installment.getInstallmentId())
                    .build();

            header.addDetail(detail);
            salaryDetailRepository.save(detail);

            // Mark installment as PAID
            installment.markAsPaid(
                    LocalDate.now(),
                    installment.getInstallmentAmount(),
                    yearMonth.toString());
            loanInstallmentRepository.save(installment);

            // Update loan remaining balance
            Loan loan = loanRepository.findById(installment.getLoanId())
                    .orElseThrow(
                            () -> new RuntimeException("Ø§Ù„Ù‚Ø±Ø¶ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + installment.getLoanId()));

            loan.deductPayment(installment.getInstallmentAmount());
            loanRepository.save(loan);

            log.info("Processed loan installment: {} for amount {}, Loan remaining: {}",
                    installment.getInstallmentId(),
                    installment.getInstallmentAmount(),
                    loan.getRemainingBalance());
        }
    }

    /**
     * Validate employee eligibility for payroll.
     */
    private void validateEmployeeEligibility(Employee employee) {
        if (!"TECHNO".equals(employee.getEmpContractType())) {
            throw new RuntimeException("ÙÙ‚Ø· Ù…ÙˆØ¸ÙÙˆ Ø¹Ù‚Ø¯ TECHNO Ù…Ø¤Ù‡Ù„ÙˆÙ† Ù„Ù„Ø±ÙˆØ§ØªØ¨");
        }

        if (!"ACTIVE".equals(employee.getEmploymentStatus()) && !"ON_LEAVE".equals(employee.getEmploymentStatus())) {
            throw new RuntimeException(
                    "ÙÙ‚Ø· Ø§Ù„Ù…ÙˆØ¸ÙÙˆÙ† Ø§Ù„Ù†Ø´Ø·ÙˆÙ† Ø£Ùˆ ÙÙŠ Ø¥Ø¬Ø§Ø²Ø© Ù…Ø¤Ù‡Ù„ÙˆÙ† Ù„Ù„Ø±ÙˆØ§ØªØ¨");
        }
    }

    /**
     * Check if employee is eligible for payroll.
     */
    private boolean isEligibleForPayroll(Employee employee) {
        return "TECHNO".equals(employee.getEmpContractType()) &&
                ("ACTIVE".equals(employee.getEmploymentStatus()) || "ON_LEAVE".equals(employee.getEmploymentStatus()));
    }

    /**
     * Approve a payroll calculation.
     * Moves through the 3-level approval workflow (HR â†’ Finance â†’ GM).
     *
     * @param salaryId   Salary header ID
     * @param approverNo Employee number of the approver
     * @return Updated salary header
     */
    @Transactional
    public SalaryHeader approvePayroll(Long salaryId, Long approverNo) {
        log.info("Approving payroll {} by approver {}", salaryId, approverNo);

        // Load salary header
        SalaryHeader salary = salaryHeaderRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + salaryId));

        // Validate status
        if ("A".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ø§Ù„Ø±Ø§ØªØ¨ Ù…Ø¹ØªÙ…Ø¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("R".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ø§Ù„Ø±Ø§ØªØ¨ Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if (!"N".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ø­Ø§Ù„Ø© Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± ØµØ§Ù„Ø­Ø©: " + salary.getTransStatus());
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                "PAYROLL", salary.getNextAppLevel(), approverNo, salary.getNextApproval())) {
            throw new RuntimeException(
                    "Employee " + approverNo + " is not authorized to approve this payroll at level " +
                            salary.getNextAppLevel());
        }

        // Get employee info for approval chain resolution
        Long employeeNo = salary.getEmployeeNo();
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        // Move to next approval level
        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                "PAYROLL",
                salary.getNextAppLevel(),
                salary.getEmployeeNo(),
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        // Update salary header with new approval state
        salary.setTransStatus(nextLevel.getTransStatus());
        salary.setNextApproval(nextLevel.getNextApproval());
        salary.setNextAppLevel(nextLevel.getNextAppLevel());

        // If fully approved (reached final level)
        if ("A".equals(nextLevel.getTransStatus())) {
            salary.setApprovedBy(approverNo);
            salary.setApprovedDate(java.time.LocalDateTime.now());
            log.info("Payroll {} fully approved by employee {}", salaryId, approverNo);

            // Save and publish final approval notification
            salary = salaryHeaderRepository.save(salary);
            publishPayrollFinalApprovedNotification(salary, employee);
        } else {
            log.info("Payroll {} moved to approval level {}, next approver: {}",
                    salaryId, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());

            // Save and publish intermediate approval notification
            salary = salaryHeaderRepository.save(salary);
            publishPayrollIntermediateApprovedNotification(salary, employee, nextLevel.getNextAppLevel());
        }

        return salary;
    }

    /**
     * Reject a payroll calculation.
     * Rejection can occur at any approval level.
     *
     * @param salaryId        Salary header ID
     * @param approverNo      Employee number of the approver
     * @param rejectionReason Reason for rejection
     * @return Updated salary header
     */
    @Transactional
    public SalaryHeader rejectPayroll(Long salaryId, Long approverNo, String rejectionReason) {
        log.info("Rejecting payroll {} by approver {}", salaryId, approverNo);

        // Load salary header
        SalaryHeader salary = salaryHeaderRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + salaryId));

        // Validate status
        if ("A".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø±ÙØ¶ Ø§Ù„Ø±Ø§ØªØ¨ Ø§Ù„Ù…Ø¹ØªÙ…Ø¯");
        }
        if ("R".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ø§Ù„Ø±Ø§ØªØ¨ Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if (!"N".equals(salary.getTransStatus())) {
            throw new RuntimeException("Ø­Ø§Ù„Ø© Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± ØµØ§Ù„Ø­Ø©: " + salary.getTransStatus());
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                "PAYROLL", salary.getNextAppLevel(), approverNo, salary.getNextApproval())) {
            throw new RuntimeException(
                    "Ø§Ù„Ù…ÙˆØ¸Ù " + approverNo + " ØºÙŠØ± Ù…Ø®ÙˆÙ„ Ø¨Ø±ÙØ¶ Ù‡Ø°Ø§ Ø§Ù„Ø±Ø§ØªØ¨");
        }

        // Validate rejection reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new RuntimeException("Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨");
        }

        // Reject the payroll
        salary.setTransStatus("R");
        salary.setRejectionReason(rejectionReason);
        salary.setApprovedBy(approverNo); // Track who rejected
        salary.setApprovedDate(java.time.LocalDateTime.now());
        salary.setNextApproval(null); // Clear approval chain
        salary.setNextAppLevel(null);

        log.info("Payroll {} rejected by employee {}. Reason: {}", salaryId, approverNo, rejectionReason);

        salary = salaryHeaderRepository.save(salary);

        // Publish payroll rejected notification
        Employee employee = employeeRepository.findById(salary.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        publishPayrollRejectedNotification(salary, employee, rejectionReason);

        return salary;
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when payroll is calculated for an employee.
     */
    private void publishPayrollCalculatedNotification(SalaryHeader salary, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("salaryMonth", salary.getSalaryMonth());
            variables.put("grossSalary", salary.getGrossSalary().toString());
            variables.put("totalAllowances", salary.getTotalAllowances().toString());
            variables.put("totalDeductions", salary.getTotalDeductions().toString());
            variables.put("netSalary", salary.getNetSalary().toString());
            variables.put("linkUrl", "/payroll/" + salary.getSalaryId());

            // Notify the next approver
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.PAYROLL_CALCULATED,
                    salary.getNextApproval(),
                    NotificationPriority.HIGH,
                    "PAYROLL",
                    salary.getSalaryId(),
                    variables));

            log.debug("Published PAYROLL_CALCULATED notification for salary {}", salary.getSalaryId());
        } catch (Exception e) {
            log.error("Failed to publish payroll calculated notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when payroll is approved at intermediate level.
     */
    private void publishPayrollIntermediateApprovedNotification(SalaryHeader salary,
            Employee employee, Integer approvalLevel) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("salaryMonth", salary.getSalaryMonth());
            variables.put("netSalary", salary.getNetSalary().toString());
            variables.put("approvalLevel", approvalLevel != null ? approvalLevel.toString() : "ØºÙŠØ± Ù…ØªØ§Ø­");
            variables.put("linkUrl", "/payroll/" + salary.getSalaryId());

            // Determine notification type based on approval level
            String eventType;
            if (approvalLevel != null && approvalLevel == 1) {
                eventType = NotificationEventType.PAYROLL_APPROVED_L1;
            } else if (approvalLevel != null && approvalLevel == 2) {
                eventType = NotificationEventType.PAYROLL_APPROVED_L2;
            } else {
                // Fallback for other intermediate levels
                eventType = NotificationEventType.PAYROLL_APPROVED_L1;
            }

            // Notify the next approver
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    eventType,
                    salary.getNextApproval(),
                    NotificationPriority.HIGH,
                    "PAYROLL",
                    salary.getSalaryId(),
                    variables));

            log.debug("Published {} notification for salary {}", eventType, salary.getSalaryId());
        } catch (Exception e) {
            log.error("Failed to publish payroll intermediate approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when payroll is fully approved (final level).
     */
    private void publishPayrollFinalApprovedNotification(SalaryHeader salary, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("salaryMonth", salary.getSalaryMonth());
            variables.put("grossSalary", salary.getGrossSalary().toString());
            variables.put("totalAllowances", salary.getTotalAllowances().toString());
            variables.put("totalDeductions", salary.getTotalDeductions().toString());
            variables.put("netSalary", salary.getNetSalary().toString());
            variables.put("linkUrl", "/payroll/" + salary.getSalaryId());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.PAYROLL_APPROVED_FINAL,
                    salary.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "PAYROLL",
                    salary.getSalaryId(),
                    variables));

            log.debug("Published PAYROLL_APPROVED_FINAL notification for salary {}", salary.getSalaryId());
        } catch (Exception e) {
            log.error("Failed to publish payroll final approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when payroll is rejected.
     */
    private void publishPayrollRejectedNotification(SalaryHeader salary, Employee employee,
            String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("salaryMonth", salary.getSalaryMonth());
            variables.put("netSalary", salary.getNetSalary().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "");
            variables.put("linkUrl", "/payroll/" + salary.getSalaryId());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.PAYROLL_REJECTED,
                    salary.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "PAYROLL",
                    salary.getSalaryId(),
                    variables));

            log.debug("Published PAYROLL_REJECTED notification for salary {}", salary.getSalaryId());
        } catch (Exception e) {
            log.error("Failed to publish payroll rejection notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when payroll is recalculated.
     */
    private void publishPayrollRecalculatedNotification(SalaryHeader salary, Employee employee,
            String recalculationReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("salaryMonth", salary.getSalaryMonth());
            variables.put("salaryVersion", salary.getSalaryVersion().toString());
            variables.put("grossSalary", salary.getGrossSalary().toString());
            variables.put("netSalary", salary.getNetSalary().toString());
            variables.put("recalculationReason", recalculationReason != null ? recalculationReason : "");
            variables.put("linkUrl", "/payroll/" + salary.getSalaryId());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.PAYROLL_RECALCULATED,
                    salary.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "PAYROLL",
                    salary.getSalaryId(),
                    variables));

            log.debug("Published PAYROLL_RECALCULATED notification for salary {}", salary.getSalaryId());
        } catch (Exception e) {
            log.error("Failed to publish payroll recalculated notification: {}", e.getMessage(), e);
        }
    }
}
