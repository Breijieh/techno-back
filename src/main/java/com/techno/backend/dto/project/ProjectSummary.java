package com.techno.backend.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight DTO for project summary in list views.
 * Used for pagination and list endpoints to reduce payload size.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectSummary {

    private Long projectCode;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalProjectAmount;
    private String projectStatus;

    // Project manager summary
    private Long projectMgr;
    private String projectManagerName;

    // Location summary
    private String projectAddress;
    private Boolean hasGpsCoordinates;

    // Calculated summary fields
    private Long durationDays;
    private Long remainingDays;
    private BigDecimal completionPercentage;
    private Boolean isActive;
    private Boolean isOngoing;

    /**
     * Get project status display name
     */
    public String getStatusDisplay() {
        if (projectStatus == null) {
            return "غير معروف";
        }
        return switch (projectStatus) {
            case "ACTIVE" -> "نشط";
            case "COMPLETED" -> "مكتمل";
            case "ON_HOLD" -> "معلق";
            case "CANCELLED" -> "ملغي";
            default -> projectStatus;
        };
    }

    /**
     * Get formatted amount
     */
    public String getFormattedAmount() {
        if (totalProjectAmount == null) {
            return "0.00 ريال";
        }
        return String.format("%,.2f ريال", totalProjectAmount);
    }

    /**
     * Get status badge color for UI
     */
    public String getStatusColor() {
        if (projectStatus == null) {
            return "gray";
        }
        return switch (projectStatus) {
            case "ACTIVE" -> "green";
            case "COMPLETED" -> "blue";
            case "ON_HOLD" -> "yellow";
            case "CANCELLED" -> "red";
            default -> "gray";
        };
    }
}
