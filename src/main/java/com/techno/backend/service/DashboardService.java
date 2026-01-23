package com.techno.backend.service;

import com.techno.backend.dto.*;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for dashboard statistics and data aggregation.
 * Aggregates data from multiple repositories and services for dashboard
 * display.
 *
 * @author Techno HR System
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final SalaryHeaderRepository salaryHeaderRepository;
    private final LeaveService leaveService;
    private final LoanService loanService;
    private final AllowanceService allowanceService;
    private final EmployeeService employeeService;
    private final AttendanceRepository attendanceRepository;

    /**
     * Get dashboard statistics.
     *
     * @param currentEmployeeNo Current authenticated user's employee number
     * @return Dashboard statistics response
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long currentEmployeeNo) {
        log.info("Fetching dashboard statistics for employee: {}", currentEmployeeNo);

        // Get current month in YYYY-MM format
        YearMonth currentYearMonth = YearMonth.now();
        String currentMonth = currentYearMonth.toString(); // e.g., "2025-12"
        LocalDate monthStart = currentYearMonth.atDay(1);
        LocalDate monthEnd = currentYearMonth.atEndOfMonth();

        // Employee counts
        Long totalEmployees = employeeRepository.count();
        Long activeEmployees = employeeRepository.countActiveEmployees();

        // Project counts
        Long totalProjects = projectRepository.count();
        Long activeProjects = projectRepository.countActiveProjects();

        // Monthly payroll - sum netSalary for current month (latest versions only)
        BigDecimal monthlyPayroll = BigDecimal.ZERO;
        try {
            List<com.techno.backend.entity.SalaryHeader> salaries = salaryHeaderRepository
                    .findAllLatestBySalaryMonth(currentMonth);
            monthlyPayroll = salaries.stream()
                    .map(com.techno.backend.entity.SalaryHeader::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error calculating monthly payroll: {}", e.getMessage());
        }

        // Pending approvals - sum of pending leaves, loans, and allowances for current
        // approver
        long pendingApprovals = 0;
        if (currentEmployeeNo != null) {
            try {
                long pendingLeaves = leaveService.getPendingLeavesForApprover(currentEmployeeNo).size();
                long pendingLoans = loanService.getPendingLoansForApprover(currentEmployeeNo).size();
                long pendingAllowances = allowanceService.getPendingAllowances(currentEmployeeNo).size();
                pendingApprovals = pendingLeaves + pendingLoans + pendingAllowances;
            } catch (Exception e) {
                log.warn("Error calculating pending approvals: {}", e.getMessage());
            }
        }

        // Expiring documents count (within 14 days)
        long expiringDocuments = 0;
        try {
            expiringDocuments = employeeService.getEmployeesWithExpiringDocuments(14).size();
        } catch (Exception e) {
            log.warn("Error calculating expiring documents: {}", e.getMessage());
        }

        // Overtime alerts - count employees with 50+ hours overtime in current month
        long overtimeAlerts = 0;
        try {
            List<Employee> activeEmployeesList = employeeRepository.findAllActiveEmployees();
            for (Employee employee : activeEmployeesList) {
                Double overtimeHours = attendanceRepository.sumOvertimeHours(
                        employee.getEmployeeNo(), monthStart, monthEnd);
                if (overtimeHours != null && overtimeHours >= 50.0) {
                    overtimeAlerts++;
                }
            }
        } catch (Exception e) {
            log.warn("Error calculating overtime alerts: {}", e.getMessage());
        }

        return DashboardStatsResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .monthlyPayroll(monthlyPayroll)
                .pendingApprovals(pendingApprovals)
                .expiringDocuments(expiringDocuments)
                .overtimeAlerts(overtimeAlerts)
                .build();
    }

    /**
     * Get employee distribution (Saudi vs Non-Saudi).
     *
     * @return Employee distribution response
     */
    @Transactional(readOnly = true)
    public EmployeeDistributionResponse getEmployeeDistribution() {
        log.info("Fetching employee distribution");

        Long saudiCount = employeeRepository.countByEmployeeCategory("S");
        Long nonSaudiCount = employeeRepository.countByEmployeeCategory("F");

        return EmployeeDistributionResponse.builder()
                .saudiCount(saudiCount != null ? saudiCount : 0L)
                .nonSaudiCount(nonSaudiCount != null ? nonSaudiCount : 0L)
                .build();
    }

    /**
     * Get attendance overview for chart (weekly data for current month).
     *
     * @return Attendance overview response
     */
    @Transactional(readOnly = true)
    public AttendanceOverviewResponse getAttendanceOverview() {
        log.info("Fetching attendance overview");

        YearMonth currentYearMonth = YearMonth.now();
        LocalDate monthStart = currentYearMonth.atDay(1);
        LocalDate monthEnd = currentYearMonth.atEndOfMonth();

        // Calculate weeks in the month (4 weeks)
        List<String> weeks = List.of("الأسبوع الأول", "الأسبوع الثاني", "الأسبوع الثالث", "الأسبوع الرابع");
        List<Long> present = new ArrayList<>();
        List<Long> absent = new ArrayList<>();
        List<Long> onLeave = new ArrayList<>();

        // Simple implementation: divide month into 4 weeks
        int daysInMonth = monthEnd.getDayOfMonth();
        int daysPerWeek = (int) Math.ceil(daysInMonth / 4.0);

        // Get all active employees to calculate attendance
        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();

        for (int week = 1; week <= 4; week++) {
            LocalDate weekStart = monthStart.plusDays((week - 1) * daysPerWeek);
            LocalDate weekEnd = week == 4 ? monthEnd : weekStart.plusDays(daysPerWeek - 1);
            if (weekEnd.isAfter(monthEnd)) {
                weekEnd = monthEnd;
            }

            // Count present and absent for all employees in this week
            long presentCount = 0;
            long absentCount = 0;
            long onLeaveCount = 0;

            for (Employee employee : activeEmployees) {
                Long attendanceDays = attendanceRepository.countAttendanceDays(
                        employee.getEmployeeNo(), weekStart, weekEnd);
                Long absenceDays = attendanceRepository.countAbsenceDays(
                        employee.getEmployeeNo(), weekStart, weekEnd);

                if (attendanceDays != null && attendanceDays > 0) {
                    presentCount += attendanceDays;
                }
                if (absenceDays != null && absenceDays > 0) {
                    absentCount += absenceDays;
                }
                // On leave calculation would require checking leave records
                // For now, estimate based on difference
                long expectedDays = java.time.temporal.ChronoUnit.DAYS.between(weekStart, weekEnd) + 1;
                long accountedDays = (attendanceDays != null ? attendanceDays : 0) +
                        (absenceDays != null ? absenceDays : 0);
                if (accountedDays < expectedDays) {
                    onLeaveCount += (expectedDays - accountedDays);
                }
            }

            present.add(presentCount);
            absent.add(absentCount);
            onLeave.add(onLeaveCount);
        }

        return AttendanceOverviewResponse.builder()
                .weeks(weeks)
                .present(present)
                .absent(absent)
                .onLeave(onLeave)
                .build();
    }

    /**
     * Get project status for chart (completion percentages).
     *
     * @return Project status response
     */
    @Transactional(readOnly = true)
    public ProjectStatusResponse getProjectStatus() {
        log.info("Fetching project status");

        List<com.techno.backend.entity.Project> projects = projectRepository.findAll();
        List<Long> projectCodes = new ArrayList<>();
        List<Double> completionPercentages = new ArrayList<>();

        for (com.techno.backend.entity.Project project : projects) {
            projectCodes.add(project.getProjectCode());

            // Calculate completion percentage based on dates
            // Simple calculation: (days elapsed / total days) * 100
            double completion = 0.0;
            if (project.getStartDate() != null && project.getEndDate() != null) {
                LocalDate today = LocalDate.now();
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                        project.getStartDate(), project.getEndDate());
                if (totalDays > 0) {
                    long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(
                            project.getStartDate(), today);
                    completion = Math.min(100.0, Math.max(0.0, (elapsedDays * 100.0) / totalDays));
                }
            }
            completionPercentages.add(completion);
        }

        return ProjectStatusResponse.builder()
                .projectCodes(projectCodes)
                .completionPercentages(completionPercentages)
                .build();
    }
}
