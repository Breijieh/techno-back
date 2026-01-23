package com.techno.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for employee check-in request with GPS validation.
 * Used in POST /api/attendance/check-in endpoint.
 *
 * Employees must provide their GPS coordinates during check-in.
 * The system validates that the employee is within the allowed radius of the project site.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    /**
     * Project code where employee is checking in.
     * Must match an active project in the system.
     */
    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    /**
     * Employee's current latitude (GPS coordinate).
     * Valid range: -90 to +90 degrees
     */
    @NotNull(message = "خطوط العرض مطلوبة للتحقق من GPS")
    @DecimalMin(value = "-90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    @DecimalMax(value = "90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    private BigDecimal latitude;

    /**
     * Employee's current longitude (GPS coordinate).
     * Valid range: -180 to +180 degrees
     */
    @NotNull(message = "خطوط الطول مطلوبة للتحقق من GPS")
    @DecimalMin(value = "-180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    @DecimalMax(value = "180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    private BigDecimal longitude;
}
