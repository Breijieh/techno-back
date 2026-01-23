package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing email templates for notifications.
 *
 * Stores email templates for all notification types.
 * Templates use variable substitution for dynamic content.
 *
 * Categories:
 * - LEAVE: Leave request notifications (submit, approve, reject, cancel)
 * - LOAN: Loan request notifications (submit, approve, reject, postponement)
 * - PAYROLL: Payroll notifications (calculated, approved, rejected)
 * - ATTENDANCE: Attendance alerts (late, absence, early departure)
 * - ALLOWANCE: Allowance notifications (submit, approve, reject)
 * - DEDUCTION: Deduction notifications (submit, approve, reject)
 * - SALARY_RAISE: Salary raise notifications
 * - ALERT: System alerts (document expiry, overtime, payments due)
 *
 * Template Variables:
 * Variables are replaced at runtime using {{variableName}} syntax.
 * Examples: {{employeeName}}, {{leaveFromDate}}, {{loanAmount}}
 *
 * @author Techno HR System
 * @version 2.0
 * @since Phase 9 - Notifications & Email System
 */
@Entity
@Table(name = "email_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Unique template code matching NotificationEventType
     * Examples: LEAVE_SUBMITTED, LOAN_APPROVED, PAYROLL_READY
     */
    @NotBlank(message = "رمز القالب مطلوب")
    @Column(name = "template_code", length = 100, nullable = false, unique = true)
    private String templateCode;

    /**
     * Template category for grouping
     * Values: LEAVE, LOAN, PAYROLL, ATTENDANCE, ALLOWANCE, DEDUCTION, SALARY_RAISE,
     * ALERT
     */
    @NotBlank(message = "فئة القالب مطلوبة")
    @Column(name = "template_category", length = 50, nullable = false)
    private String templateCategory;

    /**
     * Email subject
     * Supports variable substitution: {{variableName}}
     */
    @NotBlank(message = "عنوان البريد الإلكتروني مطلوب")
    @Column(name = "subject", length = 500, nullable = false)
    private String subject;

    /**
     * Email body (HTML format)
     * Supports variable substitution: {{variableName}}
     */
    @NotBlank(message = "محتوى البريد الإلكتروني مطلوب")
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * Comma-separated list of available variables for this template
     * Example: "employeeName,leaveFromDate,leaveToDate,leaveDays,approverName"
     */
    @Column(name = "available_variables", columnDefinition = "TEXT")
    private String availableVariables;

    /**
     * Template active status: Y = active, N = inactive
     */
    @Pattern(regexp = "^[YN]$", message = "حالة النشاط يجب أن تكون Y أو N")
    @Column(name = "is_active", length = 1)
    @Builder.Default
    private String isActive = "Y";

    /**
     * Timestamp when template was created
     */
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    /**
     * Timestamp when template was last modified
     */
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    // Helper methods

    /**
     * Check if template is active
     */
    public boolean isTemplateActive() {
        return "Y".equals(this.isActive);
    }

    /**
     * Activate template
     */
    public void activate() {
        this.isActive = "Y";
    }

    /**
     * Deactivate template
     */
    public void deactivate() {
        this.isActive = "N";
    }

    /**
     * Get available variables as array
     */
    public String[] getAvailableVariablesArray() {
        if (availableVariables == null || availableVariables.isEmpty()) {
            return new String[0];
        }
        return availableVariables.split(",");
    }

    /**
     * Check if template uses a specific variable
     */
    public boolean usesVariable(String variableName) {
        if (availableVariables == null || variableName == null) {
            return false;
        }
        return availableVariables.contains(variableName);
    }

    /**
     * Get subject (legacy method for compatibility with existing service calls)
     * 
     * @param language Ignored as template is now single-language
     */
    public String getSubject(String language) {
        return subject;
    }

    /**
     * Get body (legacy method for compatibility with existing service calls)
     * 
     * @param language Ignored as template is now single-language
     */
    public String getBody(String language) {
        return body;
    }
}
