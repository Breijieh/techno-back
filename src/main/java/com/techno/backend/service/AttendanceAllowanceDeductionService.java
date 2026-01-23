package com.techno.backend.service;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.EmpMonthlyAllowance;
import com.techno.backend.entity.EmpMonthlyDeduction;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmpMonthlyAllowanceRepository;
import com.techno.backend.repository.EmpMonthlyDeductionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Service for auto-creating allowances and deductions from attendance calculations.
 *
 * This service bridges the gap between attendance tracking and payroll by automatically
 * generating allowance and deduction records based on attendance calculations.
 *
 * Auto-generated records:
 * - Overtime allowances (Type 9) - created from overtime calculations
 * - Late arrival deductions (Type 20) - created from delayed hours
 * - Early departure deductions (Type 20) - created from early out hours
 * - Shortage hour deductions (Type 20) - created from shortage hours
 * - Absence deductions (Type 21) - created for absence days
 *
 * All auto-generated records are:
 * - Pre-approved (transStatus = 'A')
 * - System-generated (isManualEntry = 'N')
 * - Non-deletable through normal means (isDeleted = 'N')
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 6 - Allowances & Deductions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceAllowanceDeductionService {

    private final EmpMonthlyAllowanceRepository allowanceRepository;
    private final EmpMonthlyDeductionRepository deductionRepository;
    private final AttendanceRepository attendanceRepository;

    // Transaction type codes
    private static final Long TYPE_CODE_OVERTIME = 9L;
    private static final Long TYPE_CODE_LATE = 20L;
    private static final Long TYPE_CODE_ABSENCE = 21L;

    /**
     * Process attendance transaction and auto-create related allowances/deductions.
     * Called after attendance calculations are complete.
     *
     * @param attendance The attendance transaction with calculated values
     */
    @Transactional
    public void processAttendanceForAllowancesDeductions(AttendanceTransaction attendance) {
        if (attendance == null || attendance.getEmployeeNo() == null) {
            log.warn("Cannot process null attendance or attendance without employee number");
            return;
        }

        log.info("Processing attendance {} for employee {} to create allowances/deductions",
                attendance.getTransactionId(), attendance.getEmployeeNo());

        // Create overtime allowance if applicable
        createOvertimeAllowance(attendance);

        // Create deductions for late arrival, early departure, and shortage
        createLateDeduction(attendance);
        createEarlyDepartureDeduction(attendance);
        createShortageDeduction(attendance);

        log.info("Completed processing attendance {} for allowances/deductions",
                attendance.getTransactionId());
    }

    /**
     * Auto-create overtime allowance if overtime hours exist.
     * Type Code: 9 (Overtime)
     */
    @Transactional
    public void createOvertimeAllowance(AttendanceTransaction attendance) {
        if (attendance.getOvertimeCalc() == null ||
            attendance.getOvertimeCalc().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No overtime, skip
        }

        LocalDate transactionDate = attendance.getAttendanceDate();

        // Check for duplicates
        if (allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                attendance.getEmployeeNo(), TYPE_CODE_OVERTIME, transactionDate)) {
            log.debug("Overtime allowance already exists for employee {} on {}",
                    attendance.getEmployeeNo(), transactionDate);
            return;
        }

        EmpMonthlyAllowance allowance = EmpMonthlyAllowance.builder()
                .employeeNo(attendance.getEmployeeNo())
                .typeCode(TYPE_CODE_OVERTIME)
                .transactionDate(transactionDate)
                .allowanceAmount(attendance.getOvertimeCalc())
                .transStatus("A") // Auto-approved
                .isManualEntry("N") // System-generated
                .isDeleted("N")
                .entryReason("Auto-generated from attendance on " + transactionDate)
                .approvedBy(null) // System approval
                .approvedDate(LocalDateTime.now())
                .build();

        allowanceRepository.save(allowance);
        log.info("Created overtime allowance {} for employee {} amount: {}",
                allowance.getTransactionNo(), attendance.getEmployeeNo(),
                attendance.getOvertimeCalc());
    }

    /**
     * Auto-create late arrival deduction if delayed hours exist.
     * Type Code: 20 (Late)
     * 
     * NOTE: This method is called daily but does NOT create daily deductions.
     * Delay deductions are aggregated monthly via aggregateMonthlyDelayDeductions() batch job.
     * This method is kept for backward compatibility but does not create deductions.
     */
    @Transactional
    public void createLateDeduction(AttendanceTransaction attendance) {
        if (attendance.getDelayedCalc() == null ||
            attendance.getDelayedCalc().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No late time, skip
        }

        // Delay deductions are now aggregated monthly, not created daily
        // The delay is stored in attendance.delayedCalc and will be aggregated at month-end
        log.debug("Delay recorded for employee {} on {}: {} hours (will be aggregated monthly)",
                attendance.getEmployeeNo(), attendance.getAttendanceDate(), attendance.getDelayedCalc());
    }

    /**
     * Auto-create early departure deduction if early out hours exist.
     * Type Code: 20 (Late) - reused for all time-based deductions
     */
    @Transactional
    public void createEarlyDepartureDeduction(AttendanceTransaction attendance) {
        if (attendance.getEarlyOutCalc() == null ||
            attendance.getEarlyOutCalc().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No early departure, skip
        }

        LocalDate transactionDate = attendance.getAttendanceDate();

        if (isDuplicateDeduction(attendance.getEmployeeNo(), TYPE_CODE_LATE,
                transactionDate, "Early departure")) {
            return;
        }

        EmpMonthlyDeduction deduction = EmpMonthlyDeduction.builder()
                .employeeNo(attendance.getEmployeeNo())
                .typeCode(TYPE_CODE_LATE)
                .transactionDate(transactionDate)
                .deductionAmount(attendance.getEarlyOutCalc())
                .transStatus("A") // Auto-approved
                .isManualEntry("N") // System-generated
                .isDeleted("N")
                .entryReason("Early departure on " + transactionDate)
                .approvedBy(null) // System approval
                .approvedDate(LocalDateTime.now())
                .build();

        deductionRepository.save(deduction);
        log.info("Created early departure deduction {} for employee {} amount: {}",
                deduction.getTransactionNo(), attendance.getEmployeeNo(),
                attendance.getEarlyOutCalc());
    }

    /**
     * Auto-create shortage hours deduction if shortage exists.
     * Type Code: 20 (Late) - reused for all time-based deductions
     */
    @Transactional
    public void createShortageDeduction(AttendanceTransaction attendance) {
        if (attendance.getShortageHours() == null ||
            attendance.getShortageHours().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No shortage, skip
        }

        // Calculate deduction amount if not already calculated
        // Note: shortageHours is in hours, need to calculate monetary value
        // For now, we'll check if there's a pre-calculated value or skip
        // This can be enhanced based on business rules

        LocalDate transactionDate = attendance.getAttendanceDate();

        if (isDuplicateDeduction(attendance.getEmployeeNo(), TYPE_CODE_LATE,
                transactionDate, "Shortage hours")) {
            return;
        }

        // Create deduction with shortage hours noted
        // Amount calculation can be added based on business rules
        BigDecimal shortageAmount = attendance.getShortageHours(); // Placeholder

        EmpMonthlyDeduction deduction = EmpMonthlyDeduction.builder()
                .employeeNo(attendance.getEmployeeNo())
                .typeCode(TYPE_CODE_LATE)
                .transactionDate(transactionDate)
                .deductionAmount(shortageAmount)
                .transStatus("A") // Auto-approved
                .isManualEntry("N") // System-generated
                .isDeleted("N")
                .entryReason("Shortage hours on " + transactionDate)
                .approvedBy(null) // System approval
                .approvedDate(LocalDateTime.now())
                .build();

        deductionRepository.save(deduction);
        log.info("Created shortage deduction {} for employee {} hours: {}",
                deduction.getTransactionNo(), attendance.getEmployeeNo(),
                attendance.getShortageHours());
    }

    /**
     * Auto-create absence deduction for a full day.
     * Type Code: 21 (Absence)
     *
     * This method is designed for batch processing of absences.
     * Amount should be calculated as: Monthly Salary / 30
     *
     * @param employeeNo The employee number
     * @param absenceDate The date of absence
     * @param deductionAmount The calculated deduction amount
     */
    @Transactional
    public void createAbsenceDeduction(Long employeeNo, LocalDate absenceDate,
                                      BigDecimal deductionAmount) {
        if (employeeNo == null || absenceDate == null || deductionAmount == null) {
            log.warn("Cannot create absence deduction with null parameters");
            return;
        }

        // Check for duplicates
        if (deductionRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                employeeNo, TYPE_CODE_ABSENCE, absenceDate)) {
            log.debug("Absence deduction already exists for employee {} on {}",
                    employeeNo, absenceDate);
            return;
        }

        EmpMonthlyDeduction deduction = EmpMonthlyDeduction.builder()
                .employeeNo(employeeNo)
                .typeCode(TYPE_CODE_ABSENCE)
                .transactionDate(absenceDate)
                .deductionAmount(deductionAmount)
                .transStatus("A") // Auto-approved
                .isManualEntry("N") // System-generated
                .isDeleted("N")
                .noOfDays(1) // Full day absence
                .entryReason("Absence on " + absenceDate)
                .approvedBy(null) // System approval
                .approvedDate(LocalDateTime.now())
                .build();

        deductionRepository.save(deduction);
        log.info("Created absence deduction {} for employee {} amount: {}",
                deduction.getTransactionNo(), employeeNo, deductionAmount);
    }

    /**
     * Aggregate monthly delay deductions for an employee.
     * 
     * This method is called at month-end to:
     * 1. Sum all delay hours from attendance records for the month
     * 2. Create a single monthly deduction record
     * 
     * @param employeeNo Employee number
     * @param yearMonth Year and month to aggregate (e.g., 2025-01)
     * @return Created deduction record, or null if no delays
     */
    @Transactional
    public EmpMonthlyDeduction aggregateMonthlyDelayDeductions(Long employeeNo, YearMonth yearMonth) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        
        log.info("Aggregating monthly delay deductions for employee {} for month {}", employeeNo, yearMonth);
        
        // Use existing repository method to sum delayed hours
        Double totalDelayHoursDouble = attendanceRepository.sumDelayedHours(employeeNo, monthStart, monthEnd);
        
        if (totalDelayHoursDouble == null || totalDelayHoursDouble <= 0) {
            log.debug("No delays to aggregate for employee {} in month {}", employeeNo, yearMonth);
            return null;
        }
        
        BigDecimal totalDelayHours = BigDecimal.valueOf(totalDelayHoursDouble)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Count days with delay for the reason field
        List<AttendanceTransaction> delayRecords = attendanceRepository.findLateArrivalsByDateRange(monthStart, monthEnd);
        long delayDays = delayRecords.stream()
                .filter(a -> a.getEmployeeNo().equals(employeeNo))
                .count();
        
        // Check if monthly deduction already exists for this month
        LocalDate monthDate = monthStart.plusDays(14); // Mid-month date for lookup
        if (isDuplicateDeduction(employeeNo, TYPE_CODE_LATE, monthDate, "Monthly delay aggregation")) {
            log.debug("Monthly delay deduction already exists for employee {} in month {}", employeeNo, yearMonth);
            return null;
        }
        
        // Create single monthly deduction record
        EmpMonthlyDeduction deduction = EmpMonthlyDeduction.builder()
                .employeeNo(employeeNo)
                .typeCode(TYPE_CODE_LATE)
                .transactionDate(monthDate) // Use mid-month date for monthly deductions
                .deductionAmount(totalDelayHours)
                .transStatus("A") // Auto-approved
                .isManualEntry("N") // System-generated
                .isDeleted("N")
                .entryReason(String.format("Monthly delay aggregation for %s (%d days with delay)", 
                        yearMonth.toString(), delayDays))
                .approvedBy(null) // System approval
                .approvedDate(LocalDateTime.now())
                .build();
        
        deduction = deductionRepository.save(deduction);
        log.info("Created monthly delay deduction {} for employee {} month {}: {} hours ({} days)",
                deduction.getTransactionNo(), employeeNo, yearMonth, totalDelayHours, delayDays);
        
        return deduction;
    }
    
    /**
     * Check if a deduction with specific reason already exists.
     * Used to prevent duplicate auto-generated deductions.
     */
    private boolean isDuplicateDeduction(Long employeeNo, Long typeCode,
                                        LocalDate transactionDate, String reasonKeyword) {
        return deductionRepository.findByEmployeeAndType(employeeNo, typeCode).stream()
                .anyMatch(d -> d.getTransactionDate() != null &&
                              d.getTransactionDate().equals(transactionDate) &&
                              d.getEntryReason() != null &&
                              d.getEntryReason().contains(reasonKeyword) &&
                              "N".equals(d.getIsDeleted()));
    }
}
