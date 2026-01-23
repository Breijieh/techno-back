package com.techno.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for manual attendance entry/update by HR administrators.
 * Used in POST /api/attendance/manual and PUT /api/attendance/{id} endpoints.
 *
 * Allows HR to:
 * - Create attendance records for employees who forgot to check in/out
 * - Correct erroneous attendance data
 * - Mark absences with reasons
 * - Override calculated hours
 *
 * Requires ADMIN or HR role.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualAttendanceRequest {

    /**
     * Employee number (required for new records)
     */
    @NotNull(message = "رقم الموظف مطلوب")
    private Long employeeNo;

    /**
     * Attendance date (required)
     */
    @NotNull(message = "تاريخ الحضور مطلوب")
    private LocalDate attendanceDate;

    /**
     * Project code (optional)
     */
    private Long projectCode;

    /**
     * Check-in timestamp (optional - null if absent)
     */
    private LocalDateTime entryTime;

    /**
     * GPS latitude at check-in (optional)
     */
    @DecimalMin(value = "-90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    @DecimalMax(value = "90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    private BigDecimal entryLatitude;

    /**
     * GPS longitude at check-in (optional)
     */
    @DecimalMin(value = "-180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    @DecimalMax(value = "180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    private BigDecimal entryLongitude;

    /**
     * Check-out timestamp (optional)
     */
    private LocalDateTime exitTime;

    /**
     * GPS latitude at check-out (optional)
     */
    @DecimalMin(value = "-90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    @DecimalMax(value = "90.0", message = "يجب أن تكون خطوط العرض بين -90 و +90 درجة")
    private BigDecimal exitLatitude;

    /**
     * GPS longitude at check-out (optional)
     */
    @DecimalMin(value = "-180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    @DecimalMax(value = "180.0", message = "يجب أن تكون خطوط الطول بين -180 و +180 درجة")
    private BigDecimal exitLongitude;

    /**
     * Working hours (optional - auto-calculated if null)
     */
    @DecimalMin(value = "0.0", message = "ساعات العمل لا يمكن أن تكون سالبة")
    @DecimalMax(value = "24.0", message = "ساعات العمل لا يمكن أن تتجاوز 24 ساعة")
    private BigDecimal workingHours;

    /**
     * Overtime hours (optional - auto-calculated if null)
     */
    @DecimalMin(value = "0.0", message = "ساعات العمل الإضافي لا يمكن أن تكون سالبة")
    private BigDecimal overtimeCalc;

    /**
     * Delayed hours (optional - auto-calculated if null)
     */
    @DecimalMin(value = "0.0", message = "ساعات التأخير لا يمكن أن تكون سالبة")
    private BigDecimal delayedCalc;

    /**
     * Early departure hours (optional - auto-calculated if null)
     */
    @DecimalMin(value = "0.0", message = "ساعات المغادرة المبكرة لا يمكن أن تكون سالبة")
    private BigDecimal earlyOutCalc;

    /**
     * Shortage hours (optional - auto-calculated if null)
     */
    @DecimalMin(value = "0.0", message = "ساعات النقص لا يمكن أن تكون سالبة")
    private BigDecimal shortageHours;

    /**
     * Absence flag (Y/N)
     * If Y, entryTime and exitTime should be null
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الغياب يجب أن تكون Y أو N")
    private String absenceFlag;

    /**
     * Absence reason (required if absenceFlag is Y)
     */
    @Size(max = 500, message = "سبب الغياب لا يجب أن يتجاوز 500 حرف")
    private String absenceReason;

    /**
     * Notes (optional)
     */
    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    private String notes;
}
