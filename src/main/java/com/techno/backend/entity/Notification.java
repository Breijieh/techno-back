package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an in-app notification.
 *
 * Notifications are created for all important business events:
 * - Leave requests (submit, approve, reject, cancel)
 * - Loan requests (submit, approve, reject, postponement)
 * - Payroll (calculated, approved, rejected)
 * - Attendance (late, absence, early departure)
 * - Allowances & Deductions (submit, approve, reject)
 * - Salary raises
 * - System alerts (document expiry, overtime, payments due)
 *
 * Features:
 * - Bilingual support (Arabic + English)
 * - Priority levels (LOW, MEDIUM, HIGH, URGENT)
 * - Email integration tracking
 * - Deep links to reference entities
 * - Read/unread status
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    /**
     * Employee who receives this notification
     */
    @NotNull(message = "رقم الموظف مطلوب")
    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    /**
     * Notification type code (e.g., LEAVE_SUBMITTED, LOAN_APPROVED, PAYROLL_READY)
     * Maps to NotificationEventType constants
     */
    @NotBlank(message = "نوع الإشعار مطلوب")
    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

    /**
     * English notification title (short summary)
     */
    @NotBlank(message = "العنوان بالإنجليزية مطلوب")
    @Column(name = "title_en", nullable = false)
    private String titleEn;

    /**
     * Arabic notification title (short summary)
     */
    @NotBlank(message = "العنوان بالعربية مطلوب")
    @Column(name = "title_ar", nullable = false)
    private String titleAr;

    /**
     * English notification message (detailed content)
     */
    @NotBlank(message = "الرسالة بالإنجليزية مطلوبة")
    @Column(name = "message_en", columnDefinition = "TEXT", nullable = false)
    private String messageEn;

    /**
     * Arabic notification message (detailed content)
     */
    @NotBlank(message = "الرسالة بالعربية مطلوبة")
    @Column(name = "message_ar", columnDefinition = "TEXT", nullable = false)
    private String messageAr;

    /**
     * Read status: Y = read, N = unread
     */
    @Pattern(regexp = "^[YN]$", message = "حالة القراءة يجب أن تكون Y أو N")
    @Column(name = "is_read", length = 1)
    @Builder.Default
    private String isRead = "N";

    /**
     * Priority level: LOW, MEDIUM, HIGH, URGENT
     */
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|URGENT)$", message = "الأولوية يجب أن تكون LOW أو MEDIUM أو HIGH أو URGENT")
    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    /**
     * Deep link URL to reference entity (optional)
     * Example: /leaves/123, /loans/456, /payroll/2025-11
     */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * Type of referenced entity (LEAVE, LOAN, PAYROLL, ATTENDANCE, etc.)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * ID of referenced entity
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Whether email was sent: Y = sent, N = not sent
     */
    @Pattern(regexp = "^[YN]$", message = "الإرسال عبر البريد الإلكتروني يجب أن يكون Y أو N")
    @Column(name = "sent_via_email", length = 1)
    @Builder.Default
    private String sentViaEmail = "N";

    /**
     * Timestamp when email was sent (null if not sent)
     */
    @Column(name = "email_sent_date")
    private LocalDateTime emailSentDate;

    /**
     * Timestamp when notification was created
     */
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    /**
     * Timestamp when notification was read (null if unread)
     */
    @Column(name = "read_date")
    private LocalDateTime readDate;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", insertable = false, updatable = false)
    private Employee employee;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    // Helper methods

    /**
     * Check if notification is unread
     */
    public boolean isUnread() {
        return "N".equals(this.isRead);
    }

    /**
     * Check if notification is read
     */
    public boolean isNotificationRead() {
        return "Y".equals(this.isRead);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = "Y";
        this.readDate = LocalDateTime.now();
    }

    /**
     * Mark notification as unread
     */
    public void markAsUnread() {
        this.isRead = "N";
        this.readDate = null;
    }

    /**
     * Check if email was sent
     */
    public boolean wasEmailSent() {
        return "Y".equals(this.sentViaEmail);
    }

    /**
     * Mark email as sent
     */
    public void markEmailAsSent() {
        this.sentViaEmail = "Y";
        this.emailSentDate = LocalDateTime.now();
    }

    /**
     * Check if notification is urgent
     */
    public boolean isUrgent() {
        return "URGENT".equals(this.priority);
    }

    /**
     * Check if notification is high priority
     */
    public boolean isHighPriority() {
        return "HIGH".equals(this.priority) || "URGENT".equals(this.priority);
    }
}
