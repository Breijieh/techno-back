package com.techno.backend.repository;

import com.techno.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entity.
 *
 * Provides database operations for notification management:
 * - Query notifications by employee
 * - Count unread notifications
 * - Mark notifications as read
 * - Filter by type, priority, date range
 * - Delete old notifications
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific employee (paginated)
     * Ordered by most recent first
     *
     * @param employeeNo Employee number
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    Page<Notification> findByEmployeeNoOrderByCreatedDateDesc(Long employeeNo, Pageable pageable);

    /**
     * Find unread notifications for a specific employee (paginated)
     *
     * @param employeeNo Employee number
     * @param isRead Read status ('N' for unread)
     * @param pageable Pagination parameters
     * @return Page of unread notifications
     */
    Page<Notification> findByEmployeeNoAndIsReadOrderByCreatedDateDesc(
        Long employeeNo, String isRead, Pageable pageable);

    /**
     * Find notifications by type for a specific employee
     *
     * @param employeeNo Employee number
     * @param notificationType Notification type
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    Page<Notification> findByEmployeeNoAndNotificationTypeOrderByCreatedDateDesc(
        Long employeeNo, String notificationType, Pageable pageable);

    /**
     * Find notifications by priority for a specific employee
     *
     * @param employeeNo Employee number
     * @param priority Priority level
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    Page<Notification> findByEmployeeNoAndPriorityOrderByCreatedDateDesc(
        Long employeeNo, String priority, Pageable pageable);

    /**
     * Count unread notifications for a specific employee
     *
     * @param employeeNo Employee number
     * @param isRead Read status ('N' for unread)
     * @return Count of unread notifications
     */
    long countByEmployeeNoAndIsRead(Long employeeNo, String isRead);

    /**
     * Count all notifications for a specific employee
     *
     * @param employeeNo Employee number
     * @return Total count of notifications
     */
    long countByEmployeeNo(Long employeeNo);

    /**
     * Find a specific notification for an employee
     * Used to verify ownership before update/delete
     *
     * @param notificationId Notification ID
     * @param employeeNo Employee number
     * @return Optional notification
     */
    Optional<Notification> findByNotificationIdAndEmployeeNo(Long notificationId, Long employeeNo);

    /**
     * Find notifications by reference (e.g., all notifications for a specific leave request)
     *
     * @param referenceType Reference type (LEAVE, LOAN, PAYROLL, etc.)
     * @param referenceId Reference ID
     * @return List of notifications
     */
    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * Mark all unread notifications as read for a specific employee
     *
     * @param employeeNo Employee number
     * @param readDate Timestamp when marked as read
     * @return Number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 'Y', n.readDate = :readDate " +
           "WHERE n.employeeNo = :employeeNo AND n.isRead = 'N'")
    int markAllAsRead(@Param("employeeNo") Long employeeNo, @Param("readDate") LocalDateTime readDate);

    /**
     * Mark a specific notification as read
     *
     * @param notificationId Notification ID
     * @param employeeNo Employee number (for security)
     * @param readDate Timestamp when marked as read
     * @return Number of notifications updated (0 or 1)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 'Y', n.readDate = :readDate " +
           "WHERE n.notificationId = :notificationId AND n.employeeNo = :employeeNo")
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("employeeNo") Long employeeNo,
                   @Param("readDate") LocalDateTime readDate);

    /**
     * Mark a specific notification as unread
     *
     * @param notificationId Notification ID
     * @param employeeNo Employee number (for security)
     * @return Number of notifications updated (0 or 1)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 'N', n.readDate = NULL " +
           "WHERE n.notificationId = :notificationId AND n.employeeNo = :employeeNo")
    int markAsUnread(@Param("notificationId") Long notificationId,
                     @Param("employeeNo") Long employeeNo);

    /**
     * Delete old notifications (retention policy)
     * Should be called by a scheduled job
     *
     * @param beforeDate Delete notifications created before this date
     * @return Number of notifications deleted
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdDate < :beforeDate")
    int deleteOldNotifications(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Find notifications created within a date range
     *
     * @param employeeNo Employee number
     * @param fromDate Start date
     * @param toDate End date
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.employeeNo = :employeeNo " +
           "AND n.createdDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY n.createdDate DESC")
    Page<Notification> findByEmployeeNoAndDateRange(
        @Param("employeeNo") Long employeeNo,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable);

    /**
     * Find urgent/high priority unread notifications
     *
     * @param employeeNo Employee number
     * @param pageable Pagination parameters
     * @return Page of urgent notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.employeeNo = :employeeNo " +
           "AND n.isRead = 'N' " +
           "AND n.priority IN ('URGENT', 'HIGH') " +
           "ORDER BY n.createdDate DESC")
    Page<Notification> findUrgentUnreadNotifications(
        @Param("employeeNo") Long employeeNo,
        Pageable pageable);

    /**
     * Count notifications by type for analytics
     *
     * @param notificationType Notification type
     * @return Count of notifications
     */
    long countByNotificationType(String notificationType);

    /**
     * Find latest N notifications for an employee
     *
     * @param employeeNo Employee number
     * @param pageable Pagination (use PageRequest.of(0, n))
     * @return List of latest notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.employeeNo = :employeeNo " +
           "ORDER BY n.createdDate DESC")
    List<Notification> findLatestNotifications(@Param("employeeNo") Long employeeNo, Pageable pageable);
}
