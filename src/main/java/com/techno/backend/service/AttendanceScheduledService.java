package com.techno.backend.service;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.EmpMonthlyDeduction;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled service for automated attendance operations.
 *
 * This service runs background jobs to:
 * 1. Automatically check out employees who forgot to check out (runs daily at 23:59)
 * 2. Mark absences for employees who didn't show up (runs daily at 10:00 AM)
 *
 * These batch jobs are critical for maintaining accurate attendance data
 * and ensuring payroll calculations are correct.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 2 - Attendance Batch Jobs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceScheduledService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCalculationService calculationService;
    private final HolidayService holidayService;
    private final AttendanceDayClosureService closureService;
    private final AttendanceAllowanceDeductionService allowanceDeductionService;

    /**
     * Automatically check out employees who forgot to check out.
     *
     * Runs hourly at the top of every hour.
     *
     * Process:
     * 1. Find all attendance records for today where entryTime exists but exitTime is null
     * 2. For each record, perform auto-checkout at the scheduled end time
     * 3. Mark the record with isAutoCheckout = 'Y'
     * 4. Recalculate all hours (working, overtime, delays, etc.)
     *
     * This ensures:
     * - No incomplete attendance records remain
     * - Working hours are calculated for all employees
     * - Payroll calculations can be performed accurately
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Riyadh")
    @Transactional
    public void autoCheckoutForForgottenEmployees() {
        LocalDate today = LocalDate.now();
        log.info("Starting auto-checkout job for date: {}", today);

        try {
            // Find all incomplete attendance records for today
            List<AttendanceTransaction> incompleteRecords =
                attendanceRepository.findIncompleteAttendanceByDate(today);

            if (incompleteRecords.isEmpty()) {
                log.info("No incomplete attendance records found for {}", today);
                return;
            }

            log.info("Found {} employees who haven't checked out. Processing auto-checkout...",
                    incompleteRecords.size());

            int successCount = 0;
            int errorCount = 0;

            for (AttendanceTransaction attendance : incompleteRecords) {
                try {
                    processAutoCheckout(attendance);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to auto-checkout employee {} on {}: {}",
                            attendance.getEmployeeNo(),
                            attendance.getAttendanceDate(),
                            e.getMessage(), e);
                }
            }

            log.info("Auto-checkout job completed. Success: {}, Errors: {}", successCount, errorCount);

        } catch (Exception e) {
            log.error("Auto-checkout job failed with error: {}", e.getMessage(), e);
        }
    }

    /**
     * Mark absences for employees who didn't show up for work.
     *
     * Runs daily at 2:00 AM.
     *
     * Process:
     * 1. Check if today is a holiday or weekend - if yes, skip the job
     * 2. Find all ACTIVE employees
     * 3. For each employee, check if they have an attendance record for today
     * 4. If no record exists and it's a working day, create an absence record
     * 5. Mark the record with absenceFlag = 'Y' and absenceReason = 'No show - Auto marked'
     *
     * This ensures:
     * - All employees have an attendance record for each working day
     * - Absences are automatically tracked for payroll deductions
     * - HR doesn't need to manually mark every absence
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Riyadh")
    @Transactional
    public void markAbsencesForNoShows() {
        LocalDate today = LocalDate.now();
        log.info("Starting mark absences job for date: {}", today);

        try {
            // Skip if today is a holiday
            if (holidayService.isHoliday(today)) {
                log.info("Today ({}) is a holiday. Skipping absence marking.", today);
                return;
            }

            // Skip if today is a weekend (Friday or Saturday in Saudi Arabia)
            if (isWeekend(today)) {
                log.info("Today ({}) is a weekend. Skipping absence marking.", today);
                return;
            }

            // Find all active employees
            List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
            if (activeEmployees.isEmpty()) {
                log.info("No active employees found");
                return;
            }

            log.info("Checking attendance for {} active employees...", activeEmployees.size());

            int absencesMarked = 0;
            int alreadyPresent = 0;
            int errorCount = 0;

            for (Employee employee : activeEmployees) {
                try {
                    // Check if employee already has an attendance record for today
                    if (attendanceRepository.findByEmployeeNoAndAttendanceDate(
                            employee.getEmployeeNo(), today).isPresent()) {
                        alreadyPresent++;
                        continue;
                    }

                    // Create absence record
                    markEmployeeAsAbsent(employee, today);
                    absencesMarked++;

                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to mark absence for employee {}: {}",
                            employee.getEmployeeNo(), e.getMessage(), e);
                }
            }

            log.info("Mark absences job completed. Absences marked: {}, Already present: {}, Errors: {}",
                    absencesMarked, alreadyPresent, errorCount);

        } catch (Exception e) {
            log.error("Mark absences job failed with error: {}", e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Process auto-checkout for a single attendance record.
     *
     * @param attendance The attendance record to auto-checkout
     */
    private void processAutoCheckout(AttendanceTransaction attendance) {
        Employee employee = employeeRepository.findById(attendance.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException(
                        "Employee not found: " + attendance.getEmployeeNo()));

        // Get the applicable schedule to determine checkout time
        TimeSchedule schedule = calculationService.findApplicableSchedule(
                employee.getPrimaryDeptCode(),
                attendance.getProjectCode()
        );

        // Default to 5:00 PM if no schedule found
        LocalTime scheduledEndTime = (schedule != null)
                ? schedule.getScheduledEndTime()
                : LocalTime.of(17, 0);

        // Set exit time to scheduled end time
        LocalDateTime exitTime = attendance.getAttendanceDate().atTime(scheduledEndTime);
        attendance.setExitTime(exitTime);
        attendance.setIsAutoCheckout("Y");

        // Recalculate all hours
        calculationService.calculateAttendanceHours(
                attendance,
                employee.getPrimaryDeptCode(),
                attendance.getProjectCode()
        );

        // Save updated record
        attendanceRepository.save(attendance);

        log.info("Auto-checkout successful for employee {} on {}. Exit time: {}",
                attendance.getEmployeeNo(),
                attendance.getAttendanceDate(),
                exitTime);
    }

    /**
     * Mark an employee as absent for a specific date.
     *
     * @param employee The employee to mark as absent
     * @param date The date of absence
     */
    private void markEmployeeAsAbsent(Employee employee, LocalDate date) {
        // Get the applicable schedule for this employee
        TimeSchedule schedule = calculationService.findApplicableSchedule(
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode()
        );

        // Create absence record
        AttendanceTransaction absence = AttendanceTransaction.builder()
                .employeeNo(employee.getEmployeeNo())
                .attendanceDate(date)
                .projectCode(employee.getPrimaryProjectCode())
                .absenceFlag("Y")
                .absenceReason("No show - Auto marked by system")
                .isAutoCheckout("N")
                .isManualEntry("N")
                .isHolidayWork("N")
                .isWeekendWork("N")
                .scheduledHours(schedule != null ? schedule.getRequiredHours() : null)
                .build();

        // Calculate attendance hours (will set weekend/holiday flags if applicable)
        calculationService.calculateAttendanceHours(
                absence,
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode()
        );

        attendanceRepository.save(absence);

        log.info("Absence marked for employee {} on {}", employee.getEmployeeNo(), date);
    }

    /**
     * Check if a date falls on a weekend (Friday or Saturday in Saudi Arabia).
     *
     * @param date The date to check
     * @return true if the date is Friday or Saturday
     */
    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == java.time.DayOfWeek.FRIDAY ||
               date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY;
    }

    /**
     * Automatically close attendance days 3 hours after scheduled end time.
     *
     * Runs hourly at the top of every hour.
     *
     * Process:
     * 1. Find all attendance records where exit_time is set (or auto-checkout occurred)
     * 2. For each unique date, check if 3 hours have passed since scheduled end time
     * 3. Check if date is not already closed
     * 4. Automatically close the day
     *
     * This ensures:
     * - Attendance days are closed automatically after 3 hours
     * - Prevents modifications to old attendance records
     * - Supports payroll processing
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Riyadh")
    @Transactional
    public void autoCloseAttendanceDays() {
        LocalDate today = LocalDate.now();
        log.info("Starting auto-close attendance days job for date: {}", today);

        try {
            // Get all attendance records from yesterday and earlier that have exit_time set
            // We check dates up to 7 days ago to catch any missed closures
            LocalDate checkStartDate = today.minusDays(7);
            
            List<AttendanceTransaction> completedRecords = attendanceRepository
                    .findAllByDateRange(checkStartDate, today.minusDays(1), null, null, 
                            org.springframework.data.domain.PageRequest.of(0, 10000))
                    .getContent()
                    .stream()
                    .filter(a -> a.getExitTime() != null) // Only records with exit time
                    .collect(Collectors.toList());

            if (completedRecords.isEmpty()) {
                log.info("No completed attendance records found for auto-closure");
                return;
            }

            // Get unique dates that need to be checked
            Set<LocalDate> datesToCheck = completedRecords.stream()
                    .map(AttendanceTransaction::getAttendanceDate)
                    .filter(date -> !date.isAfter(today)) // Don't close future dates
                    .collect(Collectors.toSet());

            log.info("Found {} unique dates to check for auto-closure", datesToCheck.size());

            int closedCount = 0;
            int alreadyClosedCount = 0;
            int errorCount = 0;

            for (LocalDate dateToCheck : datesToCheck) {
                try {
                    // Skip if already closed
                    if (closureService.isDateClosed(dateToCheck)) {
                        alreadyClosedCount++;
                        continue;
                    }

                    // Find an attendance record for this date to get schedule
                    AttendanceTransaction sampleRecord = completedRecords.stream()
                            .filter(a -> a.getAttendanceDate().equals(dateToCheck))
                            .findFirst()
                            .orElse(null);

                    if (sampleRecord == null) {
                        continue;
                    }

                    // Get employee to find schedule
                    Employee employee = employeeRepository.findById(sampleRecord.getEmployeeNo())
                            .orElse(null);

                    if (employee == null) {
                        continue;
                    }

                    // Get schedule
                    TimeSchedule schedule = calculationService.findApplicableSchedule(
                            employee.getPrimaryDeptCode(),
                            sampleRecord.getProjectCode());

                    if (schedule == null) {
                        log.debug("No schedule found for date {}, skipping auto-close", dateToCheck);
                        continue;
                    }

                    // Calculate scheduled end time for this date
                    LocalDateTime scheduledEndDateTime = LocalDateTime.of(dateToCheck, schedule.getScheduledEndTime());
                    
                    // Check if 3 hours have passed since scheduled end time
                    LocalDateTime threeHoursAfterEnd = scheduledEndDateTime.plusHours(3);
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(threeHoursAfterEnd) || now.isEqual(threeHoursAfterEnd)) {
                        // Auto-close the day (use system user ID 1 as closedBy)
                        closureService.closeDay(dateToCheck, 1L, 
                                "Auto-closed 3 hours after scheduled end time");
                        closedCount++;
                        log.info("Auto-closed attendance day {} (scheduled end: {}, closed at: {})",
                                dateToCheck, scheduledEndDateTime, now);
                    }

                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to auto-close date {}: {}", dateToCheck, e.getMessage(), e);
                }
            }

            log.info("Auto-close job completed. Closed: {}, Already closed: {}, Errors: {}",
                    closedCount, alreadyClosedCount, errorCount);

        } catch (Exception e) {
            log.error("Auto-close job failed with error: {}", e.getMessage(), e);
        }
    }

    /**
     * Aggregate monthly delay deductions for all employees.
     *
     * Runs on the last day of each month at 11:59 PM.
     *
     * Process:
     * 1. Get all active employees
     * 2. For each employee, aggregate delay hours for the previous month
     * 3. Create single monthly deduction record per employee
     *
     * This ensures:
     * - Delay deductions are aggregated monthly instead of daily
     * - Single deduction record per employee per month
     * - Supports payroll processing
     */
    @Scheduled(cron = "0 59 23 28-31 * *", zone = "Asia/Riyadh") // Run on last few days of month at 11:59 PM
    @Transactional
    public void aggregateMonthlyDelayDeductions() {
        LocalDate today = LocalDate.now();
        
        // Only process if today is the last day of the month
        if (today.getDayOfMonth() != today.lengthOfMonth()) {
            log.debug("Skipping monthly delay aggregation - not the last day of month. Today: {}", today);
            return;
        }
        
        // Process the current month (which is ending today)
        YearMonth currentMonth = YearMonth.from(today);
        
        log.info("=".repeat(80));
        log.info("Starting Monthly Delay Aggregation Job for {}", currentMonth);
        log.info("=".repeat(80));

        try {
            // Get all active employees
            List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
            if (activeEmployees.isEmpty()) {
                log.info("No active employees found");
                return;
            }

            log.info("Aggregating delay deductions for {} active employees...", activeEmployees.size());

            int aggregatedCount = 0;
            int noDelayCount = 0;
            int errorCount = 0;

            for (Employee employee : activeEmployees) {
                try {
                    EmpMonthlyDeduction deduction = allowanceDeductionService
                            .aggregateMonthlyDelayDeductions(employee.getEmployeeNo(), currentMonth);

                    if (deduction != null) {
                        aggregatedCount++;
                    } else {
                        noDelayCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to aggregate delays for employee {}: {}",
                            employee.getEmployeeNo(), e.getMessage(), e);
                }
            }

            log.info("\n" + "=".repeat(80));
            log.info("Monthly Delay Aggregation Job Summary:");
            log.info("  - Employees with delay deductions: {}", aggregatedCount);
            log.info("  - Employees with no delays: {}", noDelayCount);
            log.info("  - Errors: {}", errorCount);
            log.info("  - Total employees processed: {}", activeEmployees.size());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Monthly Delay Aggregation Job Failed: {}", e.getMessage(), e);
            log.error("=".repeat(80));
        }
    }
}
