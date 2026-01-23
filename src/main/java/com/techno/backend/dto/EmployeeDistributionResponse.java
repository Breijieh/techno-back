package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for employee distribution by nationality (Saudi vs Non-Saudi).
 *
 * @author Techno HR System
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDistributionResponse {

    /**
     * Count of Saudi employees
     */
    private Long saudiCount;

    /**
     * Count of Non-Saudi employees
     */
    private Long nonSaudiCount;
}

