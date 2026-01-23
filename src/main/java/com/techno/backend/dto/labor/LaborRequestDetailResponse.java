package com.techno.backend.dto.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for labor request detail response.
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaborRequestDetailResponse {

    private Long requestNo;
    private Integer sequenceNo;
    private String jobTitleAr;
    private String jobTitleEn;
    private Integer quantity;
    private BigDecimal dailyRate;
    private Integer assignedCount;
    private Integer remainingCount;
    private String notes;

    // Calculated fields
    private boolean isFullyAssigned;
    private BigDecimal estimatedCost;
}
