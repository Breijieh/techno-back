package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing attendance day closure.
 *
 * Tracks which attendance days are closed, preventing further modifications
 * to attendance records for those dates. Used for payroll processing and
 * data integrity.
 *
 * Business Rules:
 * - One closure record per attendance date (unique constraint)
 * - Closed days prevent attendance modifications
 * - Days can be reopened to allow modifications again
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Entity
@Table(name = "attendance_day_closures", indexes = {
    @Index(name = "idx_closure_date", columnList = "attendance_date"),
    @Index(name = "idx_closure_status", columnList = "is_closed")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_closure_date", columnNames = "attendance_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class AttendanceDayClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closure_id")
    private Long closureId;

    @NotNull(message = "تاريخ الحضور مطلوب")
    @Column(name = "attendance_date", nullable = false, unique = true)
    private LocalDate attendanceDate;

    /**
     * Closure status flag:
     * Y = Closed (attendance cannot be modified)
     * N = Open (attendance can be modified)
     */
    @Pattern(regexp = "^[YN]$", message = "حالة الإغلاق يجب أن تكون Y أو N")
    @Column(name = "is_closed", length = 1, nullable = false)
    @Builder.Default
    private String isClosed = "N";

    /**
     * Employee number who closed the day
     */
    @Column(name = "closed_by")
    private Long closedBy;

    /**
     * Timestamp when the day was closed
     */
    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    /**
     * Employee number who reopened the day
     */
    @Column(name = "reopened_by")
    private Long reopenedBy;

    /**
     * Timestamp when the day was reopened
     */
    @Column(name = "reopened_date")
    private LocalDateTime reopenedDate;

    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Audit fields
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "modified_by")
    private Long modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    // Helper methods

    /**
     * Close the attendance day.
     *
     * @param closedBy Employee number who is closing the day
     * @param notes Optional notes about the closure
     */
    public void close(Long closedBy, String notes) {
        this.isClosed = "Y";
        this.closedBy = closedBy;
        this.closedDate = LocalDateTime.now();
        this.reopenedBy = null;
        this.reopenedDate = null;
        if (notes != null) {
            this.notes = notes;
        }
    }

    /**
     * Reopen the attendance day.
     *
     * @param reopenedBy Employee number who is reopening the day
     * @param notes Optional notes about the reopening
     */
    public void reopen(Long reopenedBy, String notes) {
        this.isClosed = "N";
        this.reopenedBy = reopenedBy;
        this.reopenedDate = LocalDateTime.now();
        if (notes != null) {
            this.notes = notes;
        }
    }

    public boolean isClosed() {
        return "Y".equals(this.isClosed);
    }

    public boolean isOpen() {
        return "N".equals(this.isClosed);
    }
}

