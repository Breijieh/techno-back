package com.techno.backend.dto.project;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new project.
 * Used in POST /api/projects endpoint.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "اسم المشروع بالعربية مطلوب")
    @Size(max = 250, message = "الاسم العربي لا يجب أن يتجاوز 250 حرفاً")
    private String projectName;

    @Size(max = 500, message = "العنوان لا يجب أن يتجاوز 500 حرف")
    private String projectAddress;

    @NotNull(message = "تاريخ البدء مطلوب")
    private LocalDate startDate;

    @NotNull(message = "تاريخ الانتهاء مطلوب")
    private LocalDate endDate;

    @NotNull(message = "مبلغ المشروع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "يجب أن يكون المبلغ أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    private BigDecimal totalProjectAmount;

    @DecimalMin(value = "0.0", message = "هامش الربح لا يمكن أن يكون سالباً")
    @DecimalMax(value = "100.0", message = "هامش الربح لا يمكن أن يتجاوز 100%")
    @Digits(integer = 3, fraction = 2, message = "تنسيق هامش الربح غير صالح")
    private BigDecimal projectProfitMargin;

    // GPS Coordinates (optional but all 3 required if any provided)
    @DecimalMin(value = "-90.0", message = "يجب أن تكون خطوط العرض بين -90 و 90")
    @DecimalMax(value = "90.0", message = "يجب أن تكون خطوط العرض بين -90 و 90")
    @Digits(integer = 2, fraction = 20, message = "تنسيق خطوط العرض غير صالح")
    private BigDecimal projectLatitude;

    @DecimalMin(value = "-180.0", message = "يجب أن تكون خطوط الطول بين -180 و 180")
    @DecimalMax(value = "180.0", message = "يجب أن تكون خطوط الطول بين -180 و 180")
    @Digits(integer = 3, fraction = 20, message = "تنسيق خطوط الطول غير صالح")
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

    @Size(max = 20, message = "الحالة لا يجب أن تتجاوز 20 حرفاً")
    private String projectStatus;

    /**
     * Time schedule ID to assign to this project (optional)
     * If provided, the schedule will be assigned to this project after creation
     */
    private Long scheduleId;

    /**
     * Validate GPS coordinates consistency
     */
    public boolean hasValidGpsCoordinates() {
        // If any GPS field is provided, all must be provided
        boolean hasLatitude = projectLatitude != null;
        boolean hasLongitude = projectLongitude != null;
        boolean hasRadius = gpsRadiusMeters != null;

        if (!hasLatitude && !hasLongitude && !hasRadius) {
            return true; // All null is valid
        }

        return hasLatitude && hasLongitude && hasRadius; // All must be present
    }

    /**
     * Validate dates
     */
    public boolean hasValidDates() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }
}
