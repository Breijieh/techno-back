package com.techno.backend.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for project response data.
 * Returned from project API endpoints.
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
public class ProjectResponse {

    private Long projectCode;
    private String projectName;
    private String projectAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalProjectAmount;
    private BigDecimal projectProfitMargin;

    // GPS Coordinates
    private BigDecimal projectLatitude;
    private BigDecimal projectLongitude;
    private Integer gpsRadiusMeters;
    private String requireGpsCheck;
    private Boolean hasGpsCoordinates;

    // Payment information
    private Integer noOfPayments;
    private LocalDate firstDownPaymentDate;

    // Project manager details
    private Long projectMgr;
    private String projectManagerName;

    private String technoSuffix;
    private String projectStatus;

    // Time schedule assigned to this project
    private Long scheduleId;

    // Calculated fields
    private Long durationDays;
    private Long remainingDays;
    private BigDecimal completionPercentage;
    private Boolean isActive;
    private Boolean isCompleted;
    private Boolean isOngoing;
    private Boolean hasStarted;
    private Boolean hasEnded;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    /**
     * Get project status display name
     */
    public String getProjectStatusDisplay() {
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
     * Get GPS check requirement display
     */
    public String getGpsCheckDisplay() {
        return "Y".equals(requireGpsCheck) ? "مطلوب" : "غير مطلوب";
    }

    /**
     * Get formatted GPS location
     */
    public String getFormattedGpsLocation() {
        if (projectLatitude == null || projectLongitude == null) {
            return "لا توجد إحداثيات GPS";
        }
        return String.format("%.6f, %.6f (النطاق: %d م)",
                projectLatitude.doubleValue(),
                projectLongitude.doubleValue(),
                gpsRadiusMeters != null ? gpsRadiusMeters : 0);
    }

    /**
     * Get project duration display
     */
    public String getDurationDisplay() {
        if (durationDays == null) {
            return "غير معروف";
        }
        long years = durationDays / 365;
        long months = (durationDays % 365) / 30;
        long days = (durationDays % 365) % 30;

        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append(" سنة");
        }
        if (months > 0) {
            if (sb.length() > 0) sb.append(" و ");
            sb.append(months).append(" شهر");
        }
        if (days > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" و ");
            sb.append(days).append(" يوم");
        }
        return sb.toString();
    }
}
