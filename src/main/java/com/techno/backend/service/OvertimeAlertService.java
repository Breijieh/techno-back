package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.Employee;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Scheduled service for overtime alerts.
 *
 * This service runs a daily job at 9:00 AM to:
 * 1. Calculate total overtime hours for each employee in the current month
 * 2. Check if any employee has reached 30 hours overtime (NORMAL priority)
 * 3. Check if any employee has reached 50 hours overtime (HIGH/URGENT priority)
 * 4. Log alerts for HR Manager, Finance Manager, and General Manager
 *
 * Alert Thresholds:
 * - NORMAL: 30 hours overtime reached
 * - URGENT (HIGH): 50 hours overtime reached
 *
 * Note: Email and in-app notifications will be implemented in a future phase.
 * Currently using logging only as per Phase 8 requirements.
 *
 * Overtime Calculation:
 * - Regular overtime: Excess hours beyond scheduled hours
 * - Holiday/Weekend: All working hours Ã— 1.5 multiplier
 * - The system tracks cumulative overtime to prevent overwork and manage costs
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 8 - Batch Jobs & Automation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OvertimeAlertService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemConfigService systemConfigService;

    /**
     * Threshold for normal priority overtime alert (30 hours)
     */
    private static final double OVERTIME_THRESHOLD_NORMAL = 30.0;

    /**
     * Threshold for urgent priority overtime alert (50 hours)
     */
    private static final double OVERTIME_THRESHOLD_URGENT = 50.0;

    /**
     * Track employees who have already been alerted this month to avoid duplicate alerts.
     * Map structure: "employeeNo-yearMonth-threshold" -> true
     *
     * This is cleared at the start of each month.
     * In production, this should be persisted to database or cache.
     */
    private final Set<String> alertedEmployees = new HashSet<>();

    /**
     * Daily overtime check.
     *
     * Runs at 9:00 AM Saudi Arabia time.
     *
     * Process:
     * 1. Get the current month's date range
     * 2. Find all active employees
     * 3. For each employee, calculate total overtime hours for the month
     * 4. Check if overtime exceeds 30 hours (NORMAL) or 50 hours (URGENT)
     * 5. Log appropriate alerts for management
     * 6. Track alerted employees to avoid duplicate alerts
     *
     * This ensures:
     * - Management is alerted to excessive overtime
     * - Proactive workload management
     * - Cost control for overtime expenses
     * - Employee wellbeing monitoring
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Riyadh")
    @Transactional(readOnly = true)
    public void checkOvertimeAlerts() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        log.info("=".repeat(80));
        log.info("Starting Overtime Alert Job for {}", currentMonth);
        log.info("=".repeat(80));

        try {
            // Clear alerts from previous months
            cleanupOldAlerts(currentMonth);

            // Get month date range
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            log.info("Checking overtime for period: {} to {}", monthStart, monthEnd);

            // Get all active employees
            List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
            if (activeEmployees.isEmpty()) {
                log.info("No active employees found");
                return;
            }

            log.info("Checking overtime for {} active employees...", activeEmployees.size());

            // Track statistics
            int urgentAlerts = 0;
            int normalAlerts = 0;
            int noOvertimeCount = 0;

            // Check each employee's overtime
            for (Employee employee : activeEmployees) {
                try {
                    checkEmployeeOvertime(employee, monthStart, monthEnd, currentMonth);

                    // Update statistics based on alerts
                    String urgentKey = getAlertKey(employee.getEmployeeNo(), currentMonth, OVERTIME_THRESHOLD_URGENT);
                    String normalKey = getAlertKey(employee.getEmployeeNo(), currentMonth, OVERTIME_THRESHOLD_NORMAL);

                    if (alertedEmployees.contains(urgentKey)) {
                        urgentAlerts++;
                    } else if (alertedEmployees.contains(normalKey)) {
                        normalAlerts++;
                    } else {
                        noOvertimeCount++;
                    }

                } catch (Exception e) {
                    log.error("Failed to check overtime for employee {}: {}",
                            employee.getEmployeeNo(), e.getMessage(), e);
                }
            }

            log.info("\n" + "=".repeat(80));
            log.info("Overtime Alert Job Summary:");
            log.info("  - URGENT alerts (50+ hours): {}", urgentAlerts);
            log.info("  - NORMAL alerts (30+ hours): {}", normalAlerts);
            log.info("  - Employees within limits: {}", noOvertimeCount);
            log.info("  - Total employees checked: {}", activeEmployees.size());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Overtime Alert Job Failed: {}", e.getMessage(), e);
            log.error("=".repeat(80));
        }
    }

    /**
     * Check overtime hours for a single employee and log alerts if thresholds are exceeded.
     *
     * @param employee Employee to check
     * @param monthStart Start date of the month
     * @param monthEnd End date of the month
     * @param currentMonth Current year-month
     */
    private void checkEmployeeOvertime(Employee employee, LocalDate monthStart,
                                      LocalDate monthEnd, YearMonth currentMonth) {
        // Calculate total overtime hours for the month
        Double overtimeHours = attendanceRepository.sumOvertimeHours(
                employee.getEmployeeNo(),
                monthStart,
                monthEnd
        );

        // Skip if no overtime
        if (overtimeHours == null || overtimeHours <= 0) {
            log.debug("Employee #{} ({}): No overtime this month",
                    employee.getEmployeeNo(), employee.getEmployeeName());
            return;
        }

        // Check URGENT threshold (50 hours)
        if (overtimeHours >= OVERTIME_THRESHOLD_URGENT) {
            String alertKey = getAlertKey(employee.getEmployeeNo(), currentMonth, OVERTIME_THRESHOLD_URGENT);
            if (!alertedEmployees.contains(alertKey)) {
                logUrgentOvertimeAlert(employee, overtimeHours, currentMonth);
                alertedEmployees.add(alertKey);
            }
            return; // Don't check normal threshold if urgent was triggered
        }

        // Check NORMAL threshold (30 hours)
        if (overtimeHours >= OVERTIME_THRESHOLD_NORMAL) {
            String alertKey = getAlertKey(employee.getEmployeeNo(), currentMonth, OVERTIME_THRESHOLD_NORMAL);
            if (!alertedEmployees.contains(alertKey)) {
                logNormalOvertimeAlert(employee, overtimeHours, currentMonth);
                alertedEmployees.add(alertKey);
            }
            return;
        }

        // Log info for employees approaching threshold
        if (overtimeHours >= 20) {
            log.info("Employee #{} ({}): {} hours overtime - Approaching threshold",
                    employee.getEmployeeNo(), employee.getEmployeeName(),
                    String.format("%.2f", overtimeHours));
        }
    }

    /**
     * Log URGENT priority overtime alert (50+ hours).
     *
     * @param employee Employee who exceeded threshold
     * @param overtimeHours Total overtime hours
     * @param month Current month
     */
    private void logUrgentOvertimeAlert(Employee employee, Double overtimeHours, YearMonth month) {
        log.error("\n" + "!".repeat(80));
        log.error("URGENT OVERTIME ALERT - {} | Employee #{}", month, employee.getEmployeeNo());
        log.error("!".repeat(80));
        log.error("Employee: {} ({})", employee.getEmployeeName(), employee.getEmployeeName());
        log.error("National ID: {}", employee.getNationalId());
        log.error("Department Code: {}", employee.getPrimaryDeptCode());
        log.error("Project Code: {}", employee.getPrimaryProjectCode());
        log.error("Total Overtime: {} hours (THRESHOLD: {} hours)",
                String.format("%.2f", overtimeHours), String.format("%.0f", OVERTIME_THRESHOLD_URGENT));
        log.error("Excess: {} hours over urgent threshold",
                String.format("%.2f", overtimeHours - OVERTIME_THRESHOLD_URGENT));
        log.error("Priority: URGENT (HIGH)");
        log.error("Recipients: HR Manager, Finance Manager, General Manager");
        log.error("Action Required: Immediate review of workload and resource allocation");
        log.error("!".repeat(80) + "\n");

        // Send notification to employee
        publishOvertimeAlertNotification(employee, overtimeHours, month, "URGENT");
    }

    /**
     * Log NORMAL priority overtime alert (30+ hours).
     *
     * @param employee Employee who exceeded threshold
     * @param overtimeHours Total overtime hours
     * @param month Current month
     */
    private void logNormalOvertimeAlert(Employee employee, Double overtimeHours, YearMonth month) {
        log.warn("\n" + "*".repeat(80));
        log.warn("OVERTIME ALERT - {} | Employee #{}", month, employee.getEmployeeNo());
        log.warn("*".repeat(80));
        log.warn("Employee: {} ({})", employee.getEmployeeName(), employee.getEmployeeName());
        log.warn("National ID: {}", employee.getNationalId());
        log.warn("Department Code: {}", employee.getPrimaryDeptCode());
        log.warn("Project Code: {}", employee.getPrimaryProjectCode());
        log.warn("Total Overtime: {} hours (THRESHOLD: {} hours)",
                String.format("%.2f", overtimeHours), String.format("%.0f", OVERTIME_THRESHOLD_NORMAL));
        log.warn("Excess: {} hours over normal threshold",
                String.format("%.2f", overtimeHours - OVERTIME_THRESHOLD_NORMAL));
        log.warn("Priority: NORMAL");
        log.warn("Recipients: HR Manager, Finance Manager, General Manager");
        log.warn("Action: Review workload if overtime continues to increase");
        log.warn("*".repeat(80) + "\n");

        // Send notification to employee
        publishOvertimeAlertNotification(employee, overtimeHours, month, "NORMAL");
    }

    /**
     * Generate unique alert key for tracking.
     *
     * @param employeeNo Employee number
     * @param month Year-month
     * @param threshold Threshold value
     * @return Unique key string
     */
    private String getAlertKey(Long employeeNo, YearMonth month, double threshold) {
        return String.format("%d-%s-%.0f", employeeNo, month.toString(), threshold);
    }

    /**
     * Clean up alert tracking for previous months.
     *
     * @param currentMonth Current year-month
     */
    private void cleanupOldAlerts(YearMonth currentMonth) {
        // Remove alerts that don't match current month
        alertedEmployees.removeIf(key -> {
            String[] parts = key.split("-");
            if (parts.length >= 3) {
                String alertMonth = parts[1] + "-" + parts[2]; // Format: YYYY-MM
                return !alertMonth.equals(currentMonth.toString());
            }
            return true; // Remove invalid keys
        });

        if (!alertedEmployees.isEmpty()) {
            log.debug("Cleaned up old alerts. Remaining alerts: {}", alertedEmployees.size());
        }
    }

    /**
     * Manual trigger method for testing or on-demand checks.
     * Can be called from a controller endpoint for manual execution.
     *
     * @return Summary of the check
     */
    @Transactional(readOnly = true)
    public String performManualCheck() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        log.info("Manual overtime check triggered for {}", currentMonth);

        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();

        int urgentCount = 0;
        int normalCount = 0;
        int safeCount = 0;

        for (Employee employee : activeEmployees) {
            Double overtimeHours = attendanceRepository.sumOvertimeHours(
                    employee.getEmployeeNo(), monthStart, monthEnd);

            if (overtimeHours != null) {
                if (overtimeHours >= OVERTIME_THRESHOLD_URGENT) {
                    urgentCount++;
                } else if (overtimeHours >= OVERTIME_THRESHOLD_NORMAL) {
                    normalCount++;
                } else {
                    safeCount++;
                }
            } else {
                safeCount++;
            }
        }

        return String.format(
                "Overtime Alert Check Summary for %s:\n" +
                        "- URGENT (50+ hours): %d employees\n" +
                        "- NORMAL (30+ hours): %d employees\n" +
                        "- Within limits: %d employees\n" +
                        "- Total employees: %d",
                currentMonth,
                urgentCount,
                normalCount,
                safeCount,
                activeEmployees.size()
        );
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when overtime threshold is exceeded.
     * Notifies both the employee and managers (HR, Finance, General Manager).
     */
    private void publishOvertimeAlertNotification(Employee employee, Double overtimeHours,
                                                  YearMonth month, String alertLevel) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("employeeNo", employee.getEmployeeNo().toString());
            variables.put("overtimeHours", String.format("%.2f", overtimeHours));
            variables.put("month", month.toString());
            variables.put("threshold", alertLevel.equals("URGENT") ?
                    String.format("%.0f", OVERTIME_THRESHOLD_URGENT) :
                    String.format("%.0f", OVERTIME_THRESHOLD_NORMAL));
            variables.put("departmentCode", employee.getPrimaryDeptCode() != null ?
                    employee.getPrimaryDeptCode().toString() : "غير متاح");
            variables.put("projectCode", employee.getPrimaryProjectCode() != null ?
                    employee.getPrimaryProjectCode().toString() : "غير متاح");
            variables.put("linkUrl", "/dashboard/employees/attendance?employeeNo=" + employee.getEmployeeNo());

            // Event types for employee notification
            String employeeEventType = alertLevel.equals("URGENT") ?
                    NotificationEventType.OVERTIME_THRESHOLD_URGENT :
                    NotificationEventType.OVERTIME_THRESHOLD_NORMAL;

            // Event types for manager notifications
            String managerEventType = alertLevel.equals("URGENT") ?
                    NotificationEventType.OVERTIME_ALERT_50H :
                    NotificationEventType.OVERTIME_ALERT_30H;

            String priority = alertLevel.equals("URGENT") ?
                    NotificationPriority.URGENT : NotificationPriority.HIGH;

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    employeeEventType,
                    employee.getEmployeeNo(),
                    priority,
                    "OVERTIME_ALERT",
                    employee.getEmployeeNo(),
                    variables
            ));

            log.debug("Published {} notification for employee {}", employeeEventType, employee.getEmployeeNo());

            // Notify managers (HR Manager, Finance Manager, General Manager)
            try {
                Long hrManagerNo = systemConfigService.getHRManagerEmployeeNo();
                Long financeManagerNo = systemConfigService.getFinanceManagerEmployeeNo();
                Long generalManagerNo = systemConfigService.getGeneralManagerEmployeeNo();

                // Notify HR Manager
                if (hrManagerNo != null && !hrManagerNo.equals(employee.getEmployeeNo())) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            this,
                            managerEventType,
                            hrManagerNo,
                            priority,
                            "OVERTIME_ALERT_MANAGER",
                            employee.getEmployeeNo(),
                            variables
                    ));
                    log.debug("Published {} notification to HR Manager {}", managerEventType, hrManagerNo);
                }

                // Notify Finance Manager
                if (financeManagerNo != null && !financeManagerNo.equals(employee.getEmployeeNo())) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            this,
                            managerEventType,
                            financeManagerNo,
                            priority,
                            "OVERTIME_ALERT_MANAGER",
                            employee.getEmployeeNo(),
                            variables
                    ));
                    log.debug("Published {} notification to Finance Manager {}", managerEventType, financeManagerNo);
                }

                // Notify General Manager
                if (generalManagerNo != null && !generalManagerNo.equals(employee.getEmployeeNo())) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            this,
                            managerEventType,
                            generalManagerNo,
                            priority,
                            "OVERTIME_ALERT_MANAGER",
                            employee.getEmployeeNo(),
                            variables
                    ));
                    log.debug("Published {} notification to General Manager {}", managerEventType, generalManagerNo);
                }

            } catch (Exception e) {
                log.error("Failed to notify managers for overtime alert: {}", e.getMessage(), e);
                // Continue even if manager notifications fail
            }

        } catch (Exception e) {
            log.error("Failed to publish overtime alert notification: {}", e.getMessage(), e);
        }
    }
}
