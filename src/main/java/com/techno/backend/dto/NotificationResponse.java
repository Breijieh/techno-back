package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Notification entity.
 *
 * Used in API responses to return notification data to clients.
 * Includes both Arabic and English content for bilingual support.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long notificationId;
    private Long employeeNo;
    private String notificationType;
    private String titleEn;
    private String titleAr;
    private String messageEn;
    private String messageAr;
    private String isRead;
    private String priority;
    private String linkUrl;
    private String referenceType;
    private Long referenceId;
    private String sentViaEmail;
    private LocalDateTime emailSentDate;
    private LocalDateTime createdDate;
    private LocalDateTime readDate;

    /**
     * Check if notification is unread
     */
    public boolean isUnread() {
        return "N".equals(isRead);
    }

    /**
     * Check if notification is high priority
     */
    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }
}
