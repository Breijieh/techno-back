package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a labor request header for temporary/daily workers.
 * Maps to PROJECT_LABOR_REQUEST_HEADER table in database.
 *
 * A labor request specifies:
 * - Which project needs workers
 * - How many workers of each type
 * - Daily rates for each position
 * - Request approval status
 *
 * Example:
 * - Project: Kempinski Hotel Riyadh
 * - Request Date: 2024-01-15
 * - Positions: 5 Carpenters, 3 Electricians, 2 Plumbers
 * - Status: OPEN (awaiting worker assignments)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Entity
@Table(name = "project_labor_request_header", indexes = {
        @Index(name = "idx_labor_req_project", columnList = "project_code"),
        @Index(name = "idx_labor_req_status", columnList = "request_status"),
        @Index(name = "idx_labor_req_date", columnList = "request_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLaborRequestHeader extends BaseEntity {

    /**
     * Labor request number - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_no")
    private Long requestNo;

    /**
     * Project code - Foreign Key
     */
    @NotNull(message = "رمز المشروع مطلوب")
    @Column(name = "project_code", nullable = false)
    private Long projectCode;

    /**
     * Request date
     */
    @NotNull(message = "تاريخ الطلب مطلوب")
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /**
     * Start date for labor requirement
     */
    @NotNull(message = "تاريخ البدء مطلوب")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date for labor requirement (estimated)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Requested by employee number (usually project manager)
     */
    @NotNull(message = "الطالب مطلوب")
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    /**
     * Request status
     * OPEN = Open, awaiting assignments
     * PARTIAL = Partially fulfilled
     * CLOSED = Fully fulfilled
     * CANCELLED = Cancelled
     */
    @NotBlank(message = "حالة الطلب مطلوبة")
    @Pattern(regexp = "^(OPEN|PARTIAL|CLOSED|CANCELLED|PENDING)$", message = "الحالة يجب أن تكون OPEN أو PARTIAL أو CLOSED أو CANCELLED أو PENDING")
    @Column(name = "request_status", nullable = false, length = 20)
    @Builder.Default
    private String requestStatus = "OPEN";

    /**
     * Request notes/remarks
     */
    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    @Column(name = "request_notes", length = 1000)
    private String requestNotes;

    /**
     * Next approver employee number
     */
    @Column(name = "next_approval")
    private Long nextApproval;

    /**
     * Next approval level
     */
    @Column(name = "next_app_level")
    private Integer nextAppLevel;

    /**
     * Transaction status:
     * N = Needs Approval
     * A = Approved
     * R = Rejected
     * P = Pending (Transfer) - Optional, we can reuse N
     */
    @Pattern(regexp = "^[NARP]$", message = "حالة المعاملة يجب أن تكون N أو A أو R")
    @Column(name = "trans_status", length = 1)
    private String transStatus;

    /**
     * Approved by (HR Manager)
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * Approval date
     */
    @Column(name = "approval_date")
    private LocalDate approvalDate;

    /**
     * Soft delete flag
     * Y = Deleted
     * N = Active
     */
    @NotNull(message = "حالة الحذف مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحذف يجب أن تكون Y أو N")
    @Column(name = "is_deleted", nullable = false, length = 1)
    @Builder.Default
    private String isDeleted = "N";

    // Relationships

    /**
     * Reference to project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", referencedColumnName = "project_code", insertable = false, updatable = false)
    private Project project;

    /**
     * Labor request details (positions and quantities)
     */
    @OneToMany(mappedBy = "laborRequestHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectLaborRequestDetail> details = new ArrayList<>();

    // Helper methods

    /**
     * Check if request is open
     */
    public boolean isOpen() {
        return "OPEN".equals(this.requestStatus);
    }

    /**
     * Check if request is partially fulfilled
     */
    public boolean isPartial() {
        return "PARTIAL".equals(this.requestStatus);
    }

    /**
     * Check if request is closed
     */
    public boolean isClosed() {
        return "CLOSED".equals(this.requestStatus);
    }

    /**
     * Check if request is cancelled
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(this.requestStatus);
    }

    /**
     * Check if request is deleted
     */
    public boolean isDeleted() {
        return "Y".equals(this.isDeleted);
    }

    /**
     * Close the request
     */
    public void closeRequest() {
        this.requestStatus = "CLOSED";
    }

    /**
     * Cancel the request
     */
    public void cancelRequest() {
        this.requestStatus = "CANCELLED";
    }

    /**
     * Mark as partial
     */
    public void markAsPartial() {
        this.requestStatus = "PARTIAL";
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.isDeleted = "Y";
    }

    /**
     * Add detail line
     */
    public void addDetail(ProjectLaborRequestDetail detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
        detail.setRequestNo(this.requestNo);
    }

    /**
     * Get total requested positions count
     */
    public int getTotalPositions() {
        if (details == null || details.isEmpty()) {
            return 0;
        }
        return details.stream()
                .mapToInt(ProjectLaborRequestDetail::getQuantity)
                .sum();
    }
}
