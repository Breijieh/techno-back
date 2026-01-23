package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification summary/statistics.
 *
 * Provides quick overview of employee's notifications.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSummaryResponse {

    private long totalNotifications;
    private long unreadCount;
    private long urgentCount;
    private long highPriorityCount;
}
