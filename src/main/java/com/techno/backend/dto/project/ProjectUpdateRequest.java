package com.techno.backend.dto.project;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating an existing project.
 * Used in PUT /api/projects/{id} endpoint.
 * All fields are optional to support partial updates.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectUpdateRequest {

    @Size(max = 250, message = "الاسم العربي لا يجب أن يتجاوز 250 حرفاً")
    private String projectName;

    @Size(max = 250, message = "الاسم الإنجليزي لا يجب أن يتجاوز 250 حرفاً")

    @Size(max = 500, message = "العنوان لا يجب أن يتجاوز 500 حرف")
    private String projectAddress;

    private LocalDate startDate;

    private LocalDate endDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal totalProjectAmount;

    @DecimalMin(value = "0.0", message = "هامش الربح لا يمكن أن يكون سالباً")
    @DecimalMax(value = "100.0", message = "هامش الربح لا يمكن أن يتجاوز 100%")
    @Digits(integer = 3, fraction = 2, message = "تنسيق هامش الربح غير صالح")
    private BigDecimal projectProfitMargin;

    // GPS Coordinates
    @DecimalMin(value = "-90.0", message = "يجب أن تكون خطوط العرض بين -90 و 90")
    @DecimalMax(value = "90.0", message = "يجب أن تكون خطوط العرض بين -90 و 90")
    @Digits(integer = 2, fraction = 8, message = "تنسيق خطوط العرض غير صالح")
    private BigDecimal projectLatitude;

    @DecimalMin(value = "-180.0", message = "يجب أن تكون خطوط الطول بين -180 و 180")
    @DecimalMax(value = "180.0", message = "يجب أن تكون خطوط الطول بين -180 و 180")
    @Digits(integer = 3, fraction = 8, message = "تنسيق خطوط الطول غير صالح")
    private BigDecimal projectLongitude;

    @Min(value = 50, message = "نطاق GPS يجب أن يكون 50 متر على الأقل")
    @Max(value = 5000, message = "نطاق GPS لا يمكن أن يتجاوز 5000 متر")
    private Integer gpsRadiusMeters;

    @Pattern(regexp = "^[YN]$", message = "علامة التحقق من GPS يجب أن تكون 'Y' أو 'N'")
    private String requireGpsCheck;

    @Min(value = 1, message = "عدد المدفوعات يجب أن يكون 1 على الأقل")
    @Max(value = 99, message = "عدد المدفوعات لا يمكن أن يتجاوز 99")
    private Integer noOfPayments;

    private LocalDate firstDownPaymentDate;

    private Long projectMgr;

    @Size(max = 50, message = "لاحقة Techno لا يجب أن تتجاوز 50 حرفاً")
    private String technoSuffix;

    @Pattern(regexp = "^(ACTIVE|COMPLETED|ON_HOLD|CANCELLED)$",
             message = "الحالة يجب أن تكون ACTIVE أو COMPLETED أو ON_HOLD أو CANCELLED")
    private String projectStatus;

    /**
     * Time schedule ID to assign to this project (optional)
     * If provided, the schedule will be assigned to this project after update
     */
    private Long scheduleId;

    /**
     * Check if any field is being updated
     */
    public boolean hasUpdates() {
        return projectName != null || projectAddress != null ||
               startDate != null || endDate != null || totalProjectAmount != null ||
               projectProfitMargin != null || projectLatitude != null ||
               projectLongitude != null || gpsRadiusMeters != null ||
               requireGpsCheck != null || noOfPayments != null ||
               firstDownPaymentDate != null || projectMgr != null ||
               technoSuffix != null || projectStatus != null || scheduleId != null;
    }

    /**
     * Check if scheduleId is being updated (provided in request)
     * This allows distinguishing between "not provided" and "null" (remove assignment)
     */
    public boolean hasScheduleIdUpdate() {
        // In JSON, if scheduleId is not provided, it will be null
        // If it's explicitly set to null, it will also be null
        // We can't distinguish, so we'll always update if scheduleId is in the request
        // For explicit removal, frontend should send scheduleId: null
        return true; // Always update if this method is called (scheduleId was in request)
    }

    /**
     * Validate GPS coordinates consistency if updating GPS fields
     */
    public boolean hasValidGpsUpdate() {
        boolean updatingLatitude = projectLatitude != null;
        boolean updatingLongitude = projectLongitude != null;
        boolean updatingRadius = gpsRadiusMeters != null;

        // If updating any GPS field, must provide all three
        if (updatingLatitude || updatingLongitude || updatingRadius) {
            return updatingLatitude && updatingLongitude && updatingRadius;
        }

        return true; // Not updating GPS is valid
    }

    /**
     * Validate dates if updating date fields
     */
    public boolean hasValidDateUpdate() {
        if (startDate != null && endDate != null) {
            return !endDate.isBefore(startDate);
        }
        return true; // Partial date update will be validated with existing dates
    }
}
