package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated employee list response.
 * Includes pagination metadata and employee list.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListResponse {

    private List<EmployeeResponse> employees;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
