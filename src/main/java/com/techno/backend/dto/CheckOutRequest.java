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
 * DTO for employee check-out request with GPS validation.
 * Used in POST /api/attendance/check-out endpoint.
 *
 * Employees must provide their GPS coordinates during check-out.
 * The system validates location and calculates working hours, overtime, and deductions.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckOutRequest {

    /**
     * Transaction ID of the check-in record.
     * Optional - if not provided, system will find today's check-in for the employee.
     */
    private Long transactionId;

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
