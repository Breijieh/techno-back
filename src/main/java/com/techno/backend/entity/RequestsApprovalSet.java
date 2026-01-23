package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing approval workflow configuration for different request
 * types.
 *
 * Defines the approval chain for each type of request:
 * - VAC: Vacation/Leave requests (3 levels: Direct Manager → Project Manager →
 * HR)
 * - LOAN: Loan requests (2 levels: HR → Finance)
 * - INCR: Salary raise requests (4 levels: Direct Manager → HR → Finance → GM)
 * - POSTLOAN: Loan postponement (3 levels: HR → Finance → GM)
 * - PAYROLL: Payroll approval (3 levels: HR → Finance → GM)
 *
 * Features:
 * - Dynamic approval chain configuration
 * - Function-based approver resolution (GetDirectManager, GetHRManager, etc.)
 * - Close level marking for final approval
 * - Department/Project specific approval chains
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Approval System
 */
@Entity
@Table(name = "requests_approval_set", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "request_type", "level_no" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = { "department", "project" })
@ToString(exclude = { "department", "project" })
@EntityListeners(AuditingEntityListener.class)
public class RequestsApprovalSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    /**
     * Request type code:
     * VAC = Vacation/Leave
     * LOAN = Loan
     * INCR = Salary Raise
     * POSTLOAN = Loan Postponement
     * PAYROLL = Payroll Approval
     */
    @NotNull(message = "نوع الطلب مطلوب")
    @Pattern(regexp = "^(VAC|LOAN|INCR|POSTLOAN|PAYROLL|PROJ_PAYMENT|PROJ_TRANSFER|MANUAL_ATTENDANCE|ALLOW|DEDUCT|LABOR_REQ)$", message = "نوع الطلب يجب أن يكون VAC أو LOAN أو INCR أو POSTLOAN أو PAYROLL أو PROJ_PAYMENT أو PROJ_TRANSFER أو MANUAL_ATTENDANCE أو ALLOW أو DEDUCT أو LABOR_REQ")
    @Column(name = "request_type", length = 20, nullable = false)
    private String requestType;

    /**
     * Approval level number (1, 2, 3, 4)
     * Higher numbers = higher approval authority
     */
    @NotNull(message = "رقم المستوى مطلوب")
    @Column(name = "level_no", nullable = false)
    private Integer levelNo;

    /**
     * Function to resolve approver employee number:
     * - GetDirectManager: Employee's direct manager
     * - GetProjectManager: Project manager
     * - GetHRManager: HR manager (from system config)
     * - GetFinManager: Finance manager (from system config)
     * - GetGeneralManager: General manager (from system config)
     * - SpecificEmployee: Specific employee number (use departmentCode field)
     */
    @NotNull(message = "استدعاء الدالة مطلوب")
    @Column(name = "function_call", length = 50, nullable = false)
    private String functionCall;

    /**
     * Close level flag:
     * Y = This is the final approval level
     * N = More approval levels follow
     */
    @Pattern(regexp = "^[YN]$", message = "مستوى الإغلاق يجب أن يكون Y أو N")
    @Column(name = "close_level", length = 1)
    @Builder.Default
    private String closeLevel = "N";

    /**
     * Optional: Department-specific approval chain
     * If null, applies to all departments
     */
    @Column(name = "department_code")
    private Long departmentCode;

    /**
     * Optional: Project-specific approval chain
     * If null, applies to all projects
     */
    @Column(name = "project_code")
    private Long projectCode;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * Active status:
     * Y = Active approval rule
     * N = Inactive (won't be used)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون Y أو N")
    @Column(name = "is_active", length = 1)
    @Builder.Default
    private String isActive = "Y";

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

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_code", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", insertable = false, updatable = false)
    private Project project;

    // Helper methods
    public boolean isFinalLevel() {
        return "Y".equals(this.closeLevel);
    }

    public boolean isActiveRule() {
        return "Y".equals(this.isActive);
    }

    public boolean isGlobalRule() {
        return departmentCode == null && projectCode == null;
    }

    public boolean isDepartmentSpecific() {
        return departmentCode != null;
    }

    public boolean isProjectSpecific() {
        return projectCode != null;
    }
}
