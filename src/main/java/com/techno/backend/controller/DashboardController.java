package com.techno.backend.controller;

import com.techno.backend.dto.*;
import com.techno.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Dashboard.
 * Provides endpoints for dashboard statistics and chart data.
 *
 * @author Techno HR System
 * @version 1.0
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get current employee number from security context.
     *
     * @return Employee number
     */
    private Long getCurrentEmployeeNo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
            if (principal instanceof String || auth.getName() != null) {
                try {
                    return Long.parseLong(auth.getName());
                } catch (NumberFormatException e) {
                    log.debug("Principal is not an employee number: {}", auth.getName());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get dashboard statistics.
     *
     * GET /api/dashboard/stats
     *
     * @return Dashboard statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        Long employeeNo = getCurrentEmployeeNo();
        log.info("GET /api/dashboard/stats - Employee: {}", employeeNo);

        // DashboardService handles null employeeNo by returning general stats
        DashboardStatsResponse stats = dashboardService.getDashboardStats(employeeNo);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع إحصائيات لوحة التحكم بنجاح", stats));
    }

    /**
     * Get employee distribution (Saudi vs Non-Saudi).
     *
     * GET /api/dashboard/employee-distribution
     *
     * @return Employee distribution
     */
    @GetMapping("/employee-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDistributionResponse>> getEmployeeDistribution() {
        log.info("GET /api/dashboard/employee-distribution");

        EmployeeDistributionResponse distribution = dashboardService.getEmployeeDistribution();
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع توزيع الموظفين بنجاح", distribution));
    }

    /**
     * Get attendance overview for chart.
     *
     * GET /api/dashboard/attendance-overview
     *
     * @return Attendance overview
     */
    @GetMapping("/attendance-overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceOverviewResponse>> getAttendanceOverview() {
        log.info("GET /api/dashboard/attendance-overview");

        AttendanceOverviewResponse overview = dashboardService.getAttendanceOverview();
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع نظرة عامة على الحضور بنجاح", overview));
    }

    /**
     * Get project status for chart.
     *
     * GET /api/dashboard/project-status
     *
     * @return Project status
     */
    @GetMapping("/project-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectStatusResponse>> getProjectStatus() {
        log.info("GET /api/dashboard/project-status");

        ProjectStatusResponse status = dashboardService.getProjectStatus();
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع حالة المشروع بنجاح", status));
    }
}
