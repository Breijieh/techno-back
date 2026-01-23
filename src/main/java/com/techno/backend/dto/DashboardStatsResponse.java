package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for dashboard statistics response.
 * Aggregates key metrics for the dashboard overview.
 *
 * @author Techno HR System
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    /**
     * Total number of employees in the system
     */
    private Long totalEmployees;

    /**
     * Number of active employees
     */
    private Long activeEmployees;

    /**
     * Total number of projects
     */
    private Long totalProjects;

    /**
     * Number of active projects
     */
    private Long activeProjects;

    /**
     * Total monthly payroll amount for current month (in SAR)
     */
    private BigDecimal monthlyPayroll;

    /**
     * Total number of pending approvals (leaves + loans + allowances)
     */
    private Long pendingApprovals;

    /**
     * Number of documents expiring soon (within 14 days)
     */
    private Long expiringDocuments;

    /**
     * Number of employees with high overtime (50+ hours)
     */
    private Long overtimeAlerts;
}

