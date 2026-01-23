package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for employee search/filter criteria.
 * Used in GET /api/employees and /api/employees/search endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSearchRequest {

    /**
     * Search text - searches in employee names (Arabic & English) and national ID
     */
    private String searchText;

    /**
     * Filter by employee category (S=Saudi, F=Foreign)
     */
    private String employeeCategory;

    /**
     * Filter by employment status (ACTIVE, TERMINATED, ON_LEAVE, SUSPENDED)
     */
    private String employmentStatus;

    /**
     * Filter by contract type (TECHNO, CLIENT, CONTRACTOR)
     */
    private String contractType;

    /**
     * Filter by department code
     */
    private Long departmentCode;

    /**
     * Filter by project code
     */
    private Long projectCode;

    /**
     * Filter by nationality
     */
    private String nationality;

    /**
     * Page number (0-based)
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size (number of records per page)
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * Sort field
     * Values: employeeNo, employeeEnName, hireDate, salary, etc.
     */
    @Builder.Default
    private String sortBy = "employeeNo";

    /**
     * Sort direction (asc or desc)
     */
    @Builder.Default
    private String sortDirection = "asc";
}
