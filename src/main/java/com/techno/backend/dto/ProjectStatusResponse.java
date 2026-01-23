package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for project status chart data.
 * Contains project codes and their completion percentages.
 *
 * @author Techno HR System
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStatusResponse {

    /**
     * List of project codes
     */
    private List<Long> projectCodes;

    /**
     * List of completion percentages (0-100) for each project
     */
    private List<Double> completionPercentages;
}

