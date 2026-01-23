package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity representing a detail line in a labor request (specific position/job title).
 * Maps to PROJECT_LABOR_REQUEST_DETAIL table in database.
 *
 * Each detail line specifies:
 * - Job title/position needed
 * - Number of workers required
 * - Daily rate for the position
 * - Number of workers assigned
 *
 * Example:
 * - Labor Request #100
 * - Detail Line 1: 5 Carpenters @ SAR 150/day
 * - Detail Line 2: 3 Electricians @ SAR 200/day
 * - Detail Line 3: 2 Plumbers @ SAR 180/day
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_labor_request_detail", indexes = {
    @Index(name = "idx_labor_detail_request", columnList = "request_no"),
    @Index(name = "idx_labor_detail_sequence", columnList = "request_no, sequence_no")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLaborRequestDetail extends BaseEntity {

    /**
     * Composite primary key: Request number + Sequence number
     */
    @EmbeddedId
    private ProjectLaborRequestDetailId id;

    /**
     * Request number - part of composite key
     */
    @Column(name = "request_no", insertable = false, updatable = false)
    private Long requestNo;

    /**
     * Sequence number - part of composite key
     */
    @Column(name = "sequence_no", insertable = false, updatable = false)
    private Integer sequenceNo;

    /**
     * Job title/position in Arabic
     */
    @NotBlank(message = "المسمى الوظيفي بالعربية مطلوب")
    @Size(max = 200, message = "المسمى الوظيفي بالعربية لا يجب أن يتجاوز 200 حرف")
    @Column(name = "job_title_ar", nullable = false, length = 200)
    private String jobTitleAr;

    /**
     * Job title/position in English
     */
    @NotBlank(message = "المسمى الوظيفي بالإنجليزية مطلوب")
    @Size(max = 200, message = "المسمى الوظيفي بالإنجليزية لا يجب أن يتجاوز 200 حرف")
    @Column(name = "job_title_en", nullable = false, length = 200)
    private String jobTitleEn;

    /**
     * Number of workers required for this position
     */
    @NotNull(message = "الكمية مطلوبة")
    @Min(value = 1, message = "الكمية يجب أن تكون 1 على الأقل")
    @Max(value = 999, message = "الكمية لا يمكن أن تتجاوز 999")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Daily rate in SAR for this position
     */
    @NotNull(message = "المعدل اليومي مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "المعدل اليومي يجب أن يكون أكبر من 0")
    @Digits(integer = 8, fraction = 2, message = "تنسيق المعدل اليومي غير صالح")
    @Column(name = "daily_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal dailyRate;

    /**
     * Number of workers assigned to this position
     */
    @NotNull(message = "عدد المعينين مطلوب")
    @Min(value = 0, message = "عدد المعينين لا يمكن أن يكون سالباً")
    @Column(name = "assigned_count", nullable = false)
    @Builder.Default
    private Integer assignedCount = 0;

    /**
     * Notes specific to this position
     */
    @Size(max = 500, message = "الملاحظات لا يجب أن تتجاوز 500 حرف")
    @Column(name = "position_notes", length = 500)
    private String positionNotes;

    // Relationships

    /**
     * Reference to labor request header
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_no", referencedColumnName = "request_no",
                insertable = false, updatable = false)
    private ProjectLaborRequestHeader laborRequestHeader;

    // Helper methods

    /**
     * Check if position is fully assigned
     */
    public boolean isFullyAssigned() {
        return assignedCount != null && quantity != null &&
               assignedCount.compareTo(quantity) >= 0;
    }

    /**
     * Check if position is partially assigned
     */
    public boolean isPartiallyAssigned() {
        return assignedCount != null && assignedCount > 0 && !isFullyAssigned();
    }

    /**
     * Get remaining positions to assign
     */
    public int getRemainingPositions() {
        if (quantity == null) {
            return 0;
        }
        if (assignedCount == null) {
            return quantity;
        }
        int remaining = quantity - assignedCount;
        return Math.max(remaining, 0);
    }

    /**
     * Calculate total cost for this position (quantity * daily rate)
     */
    public BigDecimal getTotalCost() {
        if (quantity == null || dailyRate == null) {
            return BigDecimal.ZERO;
        }
        return dailyRate.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Increment assigned count
     */
    public void incrementAssigned() {
        if (this.assignedCount == null) {
            this.assignedCount = 0;
        }
        this.assignedCount++;
    }

    /**
     * Decrement assigned count
     */
    public void decrementAssigned() {
        if (this.assignedCount == null || this.assignedCount == 0) {
            return;
        }
        this.assignedCount--;
    }
}
