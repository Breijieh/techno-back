package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Overtime Alerts.
 * Provides endpoints for overtime alert statistics.
 *
 * @author Techno HR System
 * @version 1.0
 */
@RestController
@RequestMapping("/overtime-alerts")
@RequiredArgsConstructor
@Slf4j
public class OvertimeAlertController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    private static final double OVERTIME_THRESHOLD_URGENT = 50.0;

    /**
     * Get count of employees with high overtime (50+ hours).
     *
     * GET /api/overtime-alerts/count
     *
     * @return Count of employees with high overtime
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> getOvertimeAlertsCount() {
        log.info("GET /api/overtime-alerts/count");

        YearMonth currentYearMonth = YearMonth.now();
        LocalDate monthStart = currentYearMonth.atDay(1);
        LocalDate monthEnd = currentYearMonth.atEndOfMonth();

        long count = 0;
        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();

        for (Employee employee : activeEmployees) {
            Double overtimeHours = attendanceRepository.sumOvertimeHours(
                    employee.getEmployeeNo(), monthStart, monthEnd);
            if (overtimeHours != null && overtimeHours >= OVERTIME_THRESHOLD_URGENT) {
                count++;
            }
        }

        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø¹Ø¯Ø¯ ØªÙ†Ø¨ÙŠÙ‡Ø§Øª Ø§Ù„Ø¹Ù…Ù„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠ Ø¨Ù†Ø¬Ø§Ø­", count));
    }

    /**
     * Get list of employees with high overtime (50+ hours).
     *
     * GET /api/overtime-alerts/list
     *
     * @return List of employees with high overtime
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOvertimeAlertsList() {
        log.info("GET /api/overtime-alerts/list");

        YearMonth currentYearMonth = YearMonth.now();
        LocalDate monthStart = currentYearMonth.atDay(1);
        LocalDate monthEnd = currentYearMonth.atEndOfMonth();

        List<Map<String, Object>> alerts = new ArrayList<>();
        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();

        for (Employee employee : activeEmployees) {
            Double overtimeHours = attendanceRepository.sumOvertimeHours(
                    employee.getEmployeeNo(), monthStart, monthEnd);
            if (overtimeHours != null && overtimeHours >= OVERTIME_THRESHOLD_URGENT) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("employeeNo", employee.getEmployeeNo());
                alert.put("employeeName", employee.getEmployeeName());
                alert.put("overtimeHours", overtimeHours);
                alerts.add(alert);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù‚Ø§Ø¦Ù…Ø© ØªÙ†Ø¨ÙŠÙ‡Ø§Øª Ø§Ù„Ø¹Ù…Ù„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠ Ø¨Ù†Ø¬Ø§Ø­", alerts));
    }
}


