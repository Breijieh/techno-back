package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.NotificationResponse;
import com.techno.backend.dto.NotificationSummaryResponse;
import com.techno.backend.entity.Notification;
import com.techno.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Notifications.
 *
 * Provides endpoints for:
 * - Viewing notifications (paginated)
 * - Counting unread notifications
 * - Marking notifications as read
 * - Deleting notifications
 *
 * Security:
 * - All endpoints require authentication
 * - Users can only access their own notifications
 * - Employee number extracted from security context
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get current employee number from security context.
     *
     * @return Employee number
     */
    private Long getCurrentEmployeeNo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
            if (principal instanceof String || auth.getName() != null) {
                try {
                    return Long.parseLong(auth.getName());
                } catch (NumberFormatException e) {
                    log.debug("Principal is not an employee number: {}", auth.getName());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get my notifications (paginated).
     *
     * GET /api/notifications/my?page=0&size=20
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of notifications
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long employeeNo = getCurrentEmployeeNo();
        log.info("GET /api/notifications/my - employeeNo={}, page={}, size={}", employeeNo, page, size);

        if (employeeNo == null) {
            return ResponseEntity.ok(ApiResponse.success(Page.empty()));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getMyNotifications(employeeNo, pageable);

        // Convert to DTOs
        Page<NotificationResponse> response = notifications.map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get unread notifications count.
     *
     * GET /api/notifications/unread-count
     *
     * @return Unread count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long employeeNo = getCurrentEmployeeNo();
        log.info("GET /api/notifications/unread-count - employeeNo={}", employeeNo);

        if (employeeNo == null) {
            return ResponseEntity.ok(ApiResponse.success("تم استرجاع عدد غير المقروء", 0L));
        }

        long count = notificationService.getUnreadCount(employeeNo);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع عدد غير المقروء", count));
    }

    /**
     * Get notification summary/statistics.
     *
     * GET /api/notifications/summary
     *
     * @return Notification summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getNotificationSummary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        Long employeeNo = getCurrentEmployeeNo();
        log.info("GET /api/notifications/summary - employeeNo={}", employeeNo);

        Pageable pageable = PageRequest.of(page, size);
        long total = notificationService.getMyNotifications(employeeNo, pageable).getTotalElements();
        long unread = notificationService.getUnreadCount(employeeNo);
        long urgent = notificationService.getUrgentNotifications(employeeNo, PageRequest.of(0, 1000))
                .getTotalElements();

        NotificationSummaryResponse summary = NotificationSummaryResponse.builder()
                .totalNotifications(total)
                .unreadCount(unread)
                .urgentCount(urgent)
                .highPriorityCount(0) // Can be calculated if needed
                .build();

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get unread notifications (paginated).
     *
     * GET /api/notifications/unread?page=0&size=20
     *
     * @param page Page number
     * @param size Page size
     * @return Page of unread notifications
     */
    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long employeeNo = getCurrentEmployeeNo();
        log.info("GET /api/notifications/unread - employeeNo={}, page={}, size={}", employeeNo, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUnreadNotifications(employeeNo, pageable);

        Page<NotificationResponse> response = notifications.map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mark a specific notification as read.
     *
     * PUT /api/notifications/{id}/read
     *
     * @param id Notification ID
     * @return Updated notification
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        Long employeeNo = getCurrentEmployeeNo();
        log.info("PUT /api/notifications/{}/read - employeeNo={}", id, employeeNo);

        try {
            Notification notification = notificationService.markAsRead(id, employeeNo);
            NotificationResponse response = toResponse(notification);

            return ResponseEntity.ok(ApiResponse.success("تم تمييز الإشعار كمقروء", response));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to mark notification as read: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read.
     *
     * PUT /api/notifications/mark-all-read
     *
     * @return Number of notifications marked as read
     */
    @PutMapping("/mark-all-read")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        Long employeeNo = getCurrentEmployeeNo();
        log.info("PUT /api/notifications/mark-all-read - employeeNo={}", employeeNo);

        int count = notificationService.markAllAsRead(employeeNo);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("تم تمييز %d إشعار(ات) كمقروء", count), count));
    }

    /**
     * Delete a notification.
     *
     * DELETE /api/notifications/{id}
     *
     * @param id Notification ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        Long employeeNo = getCurrentEmployeeNo();
        log.info("DELETE /api/notifications/{} - employeeNo={}", id, employeeNo);

        try {
            notificationService.deleteNotification(id, employeeNo);
            return ResponseEntity.ok(ApiResponse.success("تم حذف الإشعار", null));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete notification: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Convert Notification entity to NotificationResponse DTO.
     *
     * @param notification Notification entity
     * @return NotificationResponse DTO
     */
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .employeeNo(notification.getEmployeeNo())
                .notificationType(notification.getNotificationType())
                .titleEn(notification.getTitleEn())
                .titleAr(notification.getTitleAr())
                .messageEn(notification.getMessageEn())
                .messageAr(notification.getMessageAr())
                .isRead(notification.getIsRead())
                .priority(notification.getPriority())
                .linkUrl(notification.getLinkUrl())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .sentViaEmail(notification.getSentViaEmail())
                .emailSentDate(notification.getEmailSentDate())
                .createdDate(notification.getCreatedDate())
                .readDate(notification.getReadDate())
                .build();
    }
}
