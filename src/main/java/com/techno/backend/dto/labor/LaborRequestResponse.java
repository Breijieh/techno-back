package com.techno.backend.dto.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for labor request response.
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaborRequestResponse {

    private Long requestNo;
    private Long projectCode;
    private String projectName;
    private LocalDate requestDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestStatus;
    private String notes;
    private Long requestedBy;
    private String requestedByName;
    private Long approvedBy;
    private String approvedByName;
    private LocalDate approvalDate;

    // Detail lines
    private List<LaborRequestDetailResponse> details;

    // Calculated fields
    private Integer totalPositions;
    private Integer totalAssigned;
    private Integer remainingPositions;
    private BigDecimal totalEstimatedCost;

    // Audit
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}
