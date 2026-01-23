package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a construction project in the Techno ERP system.
 * Maps to PROJECTS table in database.
 *
 * Projects are the primary work locations where employees check in using GPS.
 * Each project has:
 * - GPS coordinates for location verification
 * - Project manager
 * - Financial tracking (total amount, profit margin, payment schedule)
 * - Time schedule for employee attendance
 *
 * Example projects:
 * - Kempinski Hotel Riyadh (24.664417, 46.674198, 500m radius)
 * - King Faisal University (24.664417, 46.674198, 1000m radius)
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_status", columnList = "project_status"),
        @Index(name = "idx_project_manager", columnList = "project_mgr"),
        @Index(name = "idx_project_dates", columnList = "start_date, end_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    /**
     * Project code - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_code")
    private Long projectCode;

    /**
     * Project name
     */
    @NotBlank(message = "اسم المشروع مطلوب")
    @Size(max = 250, message = "اسم المشروع لا يجب أن يتجاوز 250 حرف")
    @Column(name = "project_name", nullable = false, length = 250)
    private String projectName;

    /**
     * Project physical address
     */
    @Size(max = 500, message = "العنوان لا يجب أن يتجاوز 500 حرف")
    @Column(name = "project_address", length = 500)
    private String projectAddress;

    /**
     * Project start date
     */
    @NotNull(message = "تاريخ البدء مطلوب")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Project end date (planned completion)
     */
    @NotNull(message = "تاريخ الانتهاء مطلوب")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Total project contract amount in SAR
     */
    @NotNull(message = "مبلغ المشروع مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "المبلغ يجب أن يكون أكبر من 0")
    @Digits(integer = 12, fraction = 4, message = "تنسيق المبلغ غير صالح")
    @Column(name = "total_project_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal totalProjectAmount;

    /**
     * Expected profit margin percentage
     * Example: 15.50 means 15.5% profit margin
     */
    @DecimalMin(value = "0.0", message = "هامش الربح لا يمكن أن يكون سالباً")
    @DecimalMax(value = "100.0", message = "هامش الربح لا يمكن أن يتجاوز 100%")
    @Digits(integer = 3, fraction = 2, message = "تنسيق هامش الربح غير صالح")
    @Column(name = "project_profit_margin", precision = 5, scale = 2)
    private BigDecimal projectProfitMargin;

    /**
     * Project GPS latitude
     * Example: 24.664417 (Riyadh, Saudi Arabia)
     * Range: -90 to +90
     */
    @DecimalMin(value = "-90.0", message = "خط العرض يجب أن يكون بين -90 و 90")
    @DecimalMax(value = "90.0", message = "خط العرض يجب أن يكون بين -90 و 90")
    @Digits(integer = 2, fraction = 8, message = "تنسيق خط العرض غير صالح")
    @Column(name = "project_latitude", precision = 10, scale = 8)
    private BigDecimal projectLatitude;

    /**
     * Project GPS longitude
     * Example: 46.674198 (Riyadh, Saudi Arabia)
     * Range: -180 to +180
     */
    @DecimalMin(value = "-180.0", message = "خط الطول يجب أن يكون بين -180 و 180")
    @DecimalMax(value = "180.0", message = "خط الطول يجب أن يكون بين -180 و 180")
    @Digits(integer = 3, fraction = 8, message = "تنسيق خط الطول غير صالح")
    @Column(name = "project_longitude", precision = 10, scale = 8)
    private BigDecimal projectLongitude;

    /**
     * GPS check-in radius in meters
     * Default: 500m (0.5 km)
     * Large sites may use 1000m or more
     */
    @Min(value = 50, message = "نطاق GPS يجب أن يكون 50 متر على الأقل")
    @Max(value = 5000, message = "نطاق GPS لا يمكن أن يتجاوز 5000 متر")
    @Column(name = "gps_radius_meters")
    @Builder.Default
    private Integer gpsRadiusMeters = 500;

    /**
     * Flag to require GPS verification for check-in
     * Y = GPS check required (default)
     * N = GPS check disabled (for office/remote work)
     */
    @NotNull(message = "متطلب التحقق من GPS مطلوب")
    @Pattern(regexp = "^[YN]$", message = "علامة التحقق من GPS يجب أن تكون 'Y' أو 'N'")
    @Column(name = "require_gps_check", nullable = false, length = 1)
    @Builder.Default
    private String requireGpsCheck = "Y";

    /**
     * Number of payment installments from client
     */
    @Min(value = 1, message = "عدد الدفعات يجب أن يكون 1 على الأقل")
    @Max(value = 99, message = "عدد الدفعات لا يمكن أن يتجاوز 99")
    @Column(name = "no_of_payments")
    private Integer noOfPayments;

    /**
     * Date of first down payment from client
     */
    @Column(name = "first_down_payment_date")
    private LocalDate firstDownPaymentDate;

    /**
     * Project manager employee number
     * References EMPLOYEES_DETAILS.employee_no
     */
    @Column(name = "project_mgr")
    private Long projectMgr;

    /**
     * Techno internal project suffix code
     * Example: "C010204" - used for internal tracking
     */
    @Size(max = 50, message = "لاحقة Techno لا يجب أن تتجاوز 50 حرفاً")
    @Column(name = "techno_suffix", length = 50)
    private String technoSuffix;

    /**
     * Project status
     * Values: ACTIVE, COMPLETED, ON_HOLD, CANCELLED
     */
    @NotBlank(message = "حالة المشروع مطلوبة")
    @Size(max = 20, message = "الحالة لا يجب أن تتجاوز 20 حرف")
    @Column(name = "project_status", nullable = false, length = 20)
    @Builder.Default
    private String projectStatus = "ACTIVE";

    // Relationships

    /**
     * Reference to project manager
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_mgr", referencedColumnName = "employee_no", insertable = false, updatable = false)
    private Employee projectManager;

    /**
     * Regional Project Manager employee number
     * References EMPLOYEES_DETAILS.employee_no
     */
    @Column(name = "regional_mgr")
    private Long regionalMgr;

    /**
     * Reference to regional project manager
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regional_mgr", referencedColumnName = "employee_no", insertable = false, updatable = false)
    private Employee regionalManager;

    // Helper methods

    /**
     * Check if project is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.projectStatus);
    }

    /**
     * Check if project is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(this.projectStatus);
    }

    /**
     * Check if project is on hold
     */
    public boolean isOnHold() {
        return "ON_HOLD".equals(this.projectStatus);
    }

    /**
     * Check if project is cancelled
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(this.projectStatus);
    }

    /**
     * Check if GPS verification is required
     */
    public boolean isGpsCheckRequired() {
        return "Y".equals(this.requireGpsCheck);
    }

    /**
     * Check if project has GPS coordinates configured
     */
    public boolean hasGpsCoordinates() {
        return projectLatitude != null && projectLongitude != null;
    }

    /**
     * Check if project is currently ongoing (between start and end dates)
     */
    public boolean isOngoing() {
        LocalDate now = LocalDate.now();
        return (startDate == null || !startDate.isAfter(now)) &&
                (endDate == null || !endDate.isBefore(now));
    }

    /**
     * Check if project has started
     */
    public boolean hasStarted() {
        if (startDate == null) {
            return false;
        }
        return !startDate.isAfter(LocalDate.now());
    }

    /**
     * Check if project has ended
     */
    public boolean hasEnded() {
        if (endDate == null) {
            return false;
        }
        return endDate.isBefore(LocalDate.now());
    }

    /**
     * Calculate project duration in days
     */
    public Long getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calculate remaining days until project end
     */
    public Long getRemainingDays() {
        if (endDate == null) {
            return 0L;
        }
        LocalDate now = LocalDate.now();
        if (endDate.isBefore(now)) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(now, endDate);
    }

    /**
     * Calculate project completion percentage based on dates
     */
    public BigDecimal getCompletionPercentage() {
        if (startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }
        LocalDate now = LocalDate.now();
        if (now.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }
        if (now.isAfter(endDate)) {
            return BigDecimal.valueOf(100);
        }
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, now);
        if (totalDays == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(elapsedDays)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP);
    }
}
