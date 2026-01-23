package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmailTemplate;
import com.techno.backend.entity.Notification;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.EmailTemplateRepository;
import com.techno.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service for notification management.
 *
 * Handles:
 * - Creating in-app notifications from events
 * - Retrieving user notifications (paginated)
 * - Marking notifications as read/unread
 * - Deleting notifications
 * - Counting unread notifications
 * - Generating notification content (bilingual)
 *
 * Notification Creation Flow:
 * 1. NotificationEvent published by business service
 * 2. NotificationEventListener receives event (async)
 * 3. handleNotificationEvent() called
 * 4. Email template fetched
 * 5. Notification entity created and saved
 * 6. EmailService triggered to send email
 *
 * Content Generation:
 * - Fetches EmailTemplate by event type
 * - Uses template subject/body as base
 * - Substitutes variables: {{variableName}} → actual value
 * - Creates bilingual notification (Arabic + English)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    /**
     * Handle notification event - main entry point from event listener.
     *
     * This method:
     * 1. Validates event data
     * 2. Fetches employee and email template
     * 3. Creates notification with substituted content
     * 4. Saves notification to database
     * 5. Triggers email sending (async)
     *
     * @param event NotificationEvent containing event details
     */
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.debug("Handling notification event: {}", event.getEventType());

            // Validate event
            if (event.getRecipientEmployeeNo() == null) {
                log.warn("Event missing recipient employee number: {}", event.getEventType());
                return;
            }

            // Fetch employee
            Optional<Employee> employeeOpt = employeeRepository.findById(event.getRecipientEmployeeNo());
            if (employeeOpt.isEmpty()) {
                log.warn("Employee not found: employeeNo={}, eventType={}",
                        event.getRecipientEmployeeNo(), event.getEventType());
                return;
            }
            Employee employee = employeeOpt.get();

            // Fetch email template
            Optional<EmailTemplate> templateOpt = emailTemplateRepository
                    .findActiveTemplateByCode(event.getEventType());

            if (templateOpt.isEmpty()) {
                log.warn("Email template not found for event type: {}", event.getEventType());
                // Create notification without template (fallback)
                createFallbackNotification(event, employee);
                return;
            }

            EmailTemplate template = templateOpt.get();

            // Create notification with template
            Notification notification = createNotificationFromTemplate(event, employee, template);

            // Save notification
            notificationRepository.save(notification);
            log.info("Notification created: id={}, type={}, employee={}",
                    notification.getNotificationId(), notification.getNotificationType(),
                    notification.getEmployeeNo());

            // Send email (async)
            if (employee.getEmail() != null && !employee.getEmail().isEmpty()) {
                emailService.sendNotificationEmail(employee, template, event.getTemplateVariables());
            } else {
                log.warn("Employee has no email address, skipping email: employeeNo={}",
                        employee.getEmployeeNo());
            }

        } catch (Exception e) {
            log.error("Error handling notification event: type={}, error={}",
                    event.getEventType(), e.getMessage(), e);
            // Don't rethrow - notification failures shouldn't break business logic
        }
    }

    /**
     * Create notification from email template.
     *
     * Substitutes template variables into subject and body.
     *
     * @param event    Notification event
     * @param employee Recipient employee
     * @param template Email template
     * @return Notification entity
     */
    private Notification createNotificationFromTemplate(
            NotificationEvent event, Employee employee, EmailTemplate template) {

        Map<String, Object> variables = event.getTemplateVariables();

        // Substitute variables in English content
        String titleEn = substituteVariables(template.getSubject(), variables);
        String messageEn = substituteVariables(template.getBody(), variables);

        // Substitute variables in Arabic content
        String titleAr = substituteVariables(template.getSubject(), variables);
        String messageAr = substituteVariables(template.getBody(), variables);

        // Build link URL if reference exists
        String linkUrl = null;
        if (event.hasReference()) {
            linkUrl = buildLinkUrl(event.getReferenceType(), event.getReferenceId());
        }

        // Create notification
        return Notification.builder()
                .employeeNo(employee.getEmployeeNo())
                .notificationType(event.getEventType())
                .titleEn(titleEn)
                .titleAr(titleAr)
                .messageEn(messageEn)
                .messageAr(messageAr)
                .priority(NotificationPriority.getValidPriority(event.getPriority()))
                .linkUrl(linkUrl)
                .referenceType(event.getReferenceType())
                .referenceId(event.getReferenceId())
                .isRead("N")
                .sentViaEmail("N")
                .createdDate(LocalDateTime.now())
                .build();
    }

    /**
     * Create fallback notification when template is missing.
     * Uses template variables to create meaningful notification content.
     *
     * @param event    Notification event
     * @param employee Recipient employee
     */
    private void createFallbackNotification(NotificationEvent event, Employee employee) {
        String category = NotificationEventType.getCategory(event.getEventType());
        Map<String, Object> variables = event.getTemplateVariables();

        // Build meaningful notification content based on event type
        String titleAr = "إشعار النظام";
        String titleEn = "System Notification";
        String messageAr = "لديك إشعار " + category + " جديد.";
        String messageEn = "You have a new " + category + " notification.";

        // Customize content for specific event types
        if (NotificationEventType.MANUAL_ATTENDANCE_REJECTED.equals(event.getEventType())) {
            titleAr = "رفض طلب الحضور اليدوي";
            titleEn = "Manual Attendance Request Rejected";
            
            String date = variables.getOrDefault("attendanceDate", "").toString();
            String reason = variables.getOrDefault("rejectionReason", "لم يتم تحديد السبب").toString();
            
            messageAr = String.format("تم رفض طلب الحضور اليدوي لتاريخ %s. السبب: %s", date, reason);
            messageEn = String.format("Your manual attendance request for %s has been rejected. Reason: %s", date, reason);
        }

        // Build link URL if reference exists
        String linkUrl = null;
        if (event.hasReference()) {
            linkUrl = buildLinkUrl(event.getReferenceType(), event.getReferenceId());
        }

        Notification notification = Notification.builder()
                .employeeNo(employee.getEmployeeNo())
                .notificationType(event.getEventType())
                .titleEn(titleEn)
                .titleAr(titleAr)
                .messageEn(messageEn)
                .messageAr(messageAr)
                .priority(NotificationPriority.getValidPriority(event.getPriority()))
                .linkUrl(linkUrl)
                .referenceType(event.getReferenceType())
                .referenceId(event.getReferenceId())
                .isRead("N")
                .sentViaEmail("N")
                .createdDate(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.warn("Created fallback notification for missing template: type={}, employee={}",
                event.getEventType(), employee.getEmployeeNo());
    }

    /**
     * Substitute template variables.
     *
     * Replaces {{variableName}} with actual values from map.
     *
     * @param template  Template string with {{variable}} placeholders
     * @param variables Map of variable values
     * @return Template with substituted values
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Build deep link URL to reference entity.
     *
     * @param referenceType Type of entity (LEAVE, LOAN, PAYROLL, etc.)
     * @param referenceId   Entity ID
     * @return URL path
     */
    private String buildLinkUrl(String referenceType, Long referenceId) {
        if (referenceType == null || referenceId == null) {
            return null;
        }

        switch (referenceType) {
            case "LEAVE":
                return "/leaves/" + referenceId;
            case "LOAN":
                return "/loans/" + referenceId;
            case "PAYROLL":
                return "/payroll/" + referenceId;
            case "ATTENDANCE":
                return "/attendance/" + referenceId;
            case "MANUAL_ATTENDANCE":
                return "/manual-attendance/" + referenceId;
            case "ALLOWANCE":
                return "/allowances/" + referenceId;
            case "DEDUCTION":
                return "/deductions/" + referenceId;
            case "SALARY_RAISE":
                return "/salary-raises/" + referenceId;
            default:
                return null;
        }
    }

    // ===========================================
    // PUBLIC API METHODS
    // ===========================================

    /**
     * Get notifications for a specific employee (paginated).
     *
     * @param employeeNo Employee number
     * @param pageable   Pagination parameters
     * @return Page of notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(Long employeeNo, Pageable pageable) {
        log.debug("Fetching notifications for employee: {}", employeeNo);
        return notificationRepository.findByEmployeeNoOrderByCreatedDateDesc(employeeNo, pageable);
    }

    /**
     * Get unread notifications count for an employee.
     *
     * @param employeeNo Employee number
     * @return Count of unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long employeeNo) {
        return notificationRepository.countByEmployeeNoAndIsRead(employeeNo, "N");
    }

    /**
     * Mark a specific notification as read.
     *
     * @param notificationId Notification ID
     * @param employeeNo     Employee number (for security verification)
     * @return Updated notification
     * @throws IllegalArgumentException if notification not found or doesn't belong
     *                                  to employee
     */
    @Transactional
    public Notification markAsRead(Long notificationId, Long employeeNo) {
        Notification notification = notificationRepository
                .findByNotificationIdAndEmployeeNo(notificationId, employeeNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found or access denied: " + notificationId));

        if (notification.isUnread()) {
            notification.markAsRead();
            notificationRepository.save(notification);
            log.debug("Marked notification as read: id={}", notificationId);
        }

        return notification;
    }

    /**
     * Mark all notifications as read for an employee.
     *
     * @param employeeNo Employee number
     * @return Number of notifications marked as read
     */
    @Transactional
    public int markAllAsRead(Long employeeNo) {
        int count = notificationRepository.markAllAsRead(employeeNo, LocalDateTime.now());
        log.info("Marked all notifications as read: employeeNo={}, count={}", employeeNo, count);
        return count;
    }

    /**
     * Delete a notification.
     *
     * @param notificationId Notification ID
     * @param employeeNo     Employee number (for security verification)
     * @throws IllegalArgumentException if notification not found or doesn't belong
     *                                  to employee
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long employeeNo) {
        Notification notification = notificationRepository
                .findByNotificationIdAndEmployeeNo(notificationId, employeeNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found or access denied: " + notificationId));

        notificationRepository.delete(notification);
        log.info("Deleted notification: id={}, employeeNo={}", notificationId, employeeNo);
    }

    /**
     * Get unread notifications for an employee.
     *
     * @param employeeNo Employee number
     * @param pageable   Pagination parameters
     * @return Page of unread notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Long employeeNo, Pageable pageable) {
        return notificationRepository.findByEmployeeNoAndIsReadOrderByCreatedDateDesc(
                employeeNo, "N", pageable);
    }

    /**
     * Get urgent/high priority notifications.
     *
     * @param employeeNo Employee number
     * @param pageable   Pagination parameters
     * @return Page of urgent notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUrgentNotifications(Long employeeNo, Pageable pageable) {
        return notificationRepository.findUrgentUnreadNotifications(employeeNo, pageable);
    }

    /**
     * Delete old notifications (retention policy).
     * Should be called by a scheduled job.
     *
     * @param retentionDays Number of days to retain notifications
     * @return Number of notifications deleted
     */
    @Transactional
    public int deleteOldNotifications(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int count = notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Deleted old notifications: count={}, cutoffDate={}", count, cutoffDate);
        return count;
    }
}
