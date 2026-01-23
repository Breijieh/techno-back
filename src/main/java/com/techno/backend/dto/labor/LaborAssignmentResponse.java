package com.techno.backend.dto.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for labor assignment response.
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaborAssignmentResponse {

    private Long assignmentNo;
    private Long employeeNo;
    private String employeeName;
    private Long projectCode;
    private String projectName;
    private Long requestNo;
    private Integer sequenceNo;
    private String jobTitleEn; // Specialization from linked labor request detail
    private String jobTitleAr; // Specialization in Arabic
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyRate;
    private String notes;

    // Calculated fields
    private Long totalDays;
    private BigDecimal totalCost;
    private boolean isActive;
    private String assignmentStatus; // ACTIVE, COMPLETED, CANCELLED

    // Audit
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}
