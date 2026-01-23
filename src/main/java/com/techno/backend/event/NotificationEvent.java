package com.techno.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Application event published when a notification should be created.
 *
 * This event is published by service classes after successful business operations
 * and is handled asynchronously by NotificationEventListener.
 *
 * Event-driven architecture benefits:
 * - Loose coupling between business logic and notification system
 * - Async processing doesn't block business operations
 * - Transaction safety (events fire after successful commit)
 * - Easy to test (can disable event publishing)
 * - No circular dependencies
 *
 * Usage example in a service:
 * <pre>
 * {@code
 * @Autowired
 * private ApplicationEventPublisher eventPublisher;
 *
 * public void submitLeaveRequest(Leave leave) {
 *     // ... business logic ...
 *     leaveRepository.save(leave);
 *
 *     // Publish notification event
 *     eventPublisher.publishEvent(new NotificationEvent(
 *         this,
 *         NotificationEventType.LEAVE_SUBMITTED,
 *         leave.getNextApproval(),
 *         NotificationPriority.MEDIUM,
 *         "LEAVE",
 *         leave.getLeaveId(),
 *         Map.of(
 *             "employeeName", employee.getEmployeeName(),
 *             "leaveFromDate", leave.getLeaveFromDate(),
 *             "leaveToDate", leave.getLeaveToDate()
 *         )
 *     ));
 * }
 * }
 * </pre>
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Getter
public class NotificationEvent extends ApplicationEvent {

    /**
     * Notification event type (matches NotificationEventType constants)
     * Example: LEAVE_SUBMITTED, LOAN_APPROVED, PAYROLL_READY
     */
    private final String eventType;

    /**
     * Employee number of notification recipient
     * This employee will receive the in-app notification and email
     */
    private final Long recipientEmployeeNo;

    /**
     * Notification priority (LOW, MEDIUM, HIGH, URGENT)
     * From NotificationPriority constants
     */
    private final String priority;

    /**
     * Type of referenced entity (LEAVE, LOAN, PAYROLL, ATTENDANCE, etc.)
     * Used for deep linking and tracking
     */
    private final String referenceType;

    /**
     * ID of referenced entity
     * Example: leaveId, loanId, payrollHeaderId
     */
    private final Long referenceId;

    /**
     * Template variables for email and notification content
     * These variables are substituted into email templates using {{variableName}} syntax
     *
     * Common variables:
     * - employeeName: Employee's full name
     * - leaveFromDate, leaveToDate, leaveDays: Leave details
     * - loanAmount, installments, monthlyAmount: Loan details
     * - salaryMonth, grossSalary, netSalary: Payroll details
     * - approverName: Name of approver
     * - rejectionReason: Reason for rejection
     * - linkUrl: Deep link to entity
     */
    private final Map<String, Object> templateVariables;

    /**
     * Constructor with all parameters
     *
     * @param source Source object (typically the service that published the event)
     * @param eventType Notification event type
     * @param recipientEmployeeNo Employee number of recipient
     * @param priority Notification priority
     * @param referenceType Type of referenced entity
     * @param referenceId ID of referenced entity
     * @param templateVariables Variables for template substitution
     */
    public NotificationEvent(
            Object source,
            String eventType,
            Long recipientEmployeeNo,
            String priority,
            String referenceType,
            Long referenceId,
            Map<String, Object> templateVariables) {

        super(source);
        this.eventType = eventType;
        this.recipientEmployeeNo = recipientEmployeeNo;
        this.priority = priority;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.templateVariables = templateVariables != null
            ? new HashMap<>(templateVariables)
            : new HashMap<>();
    }

    /**
     * Constructor with default priority (MEDIUM)
     *
     * @param source Source object
     * @param eventType Notification event type
     * @param recipientEmployeeNo Employee number of recipient
     * @param referenceType Type of referenced entity
     * @param referenceId ID of referenced entity
     * @param templateVariables Variables for template substitution
     */
    public NotificationEvent(
            Object source,
            String eventType,
            Long recipientEmployeeNo,
            String referenceType,
            Long referenceId,
            Map<String, Object> templateVariables) {

        this(source, eventType, recipientEmployeeNo, "MEDIUM",
             referenceType, referenceId, templateVariables);
    }

    /**
     * Constructor without reference (for system-wide notifications)
     *
     * @param source Source object
     * @param eventType Notification event type
     * @param recipientEmployeeNo Employee number of recipient
     * @param priority Notification priority
     * @param templateVariables Variables for template substitution
     */
    public NotificationEvent(
            Object source,
            String eventType,
            Long recipientEmployeeNo,
            String priority,
            Map<String, Object> templateVariables) {

        this(source, eventType, recipientEmployeeNo, priority,
             null, null, templateVariables);
    }

    /**
     * Get template variable value
     *
     * @param key Variable name
     * @return Variable value or null if not found
     */
    public Object getVariable(String key) {
        return templateVariables.get(key);
    }

    /**
     * Get template variable as String
     *
     * @param key Variable name
     * @return Variable value as String or null
     */
    public String getVariableAsString(String key) {
        Object value = templateVariables.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Check if event has a reference
     *
     * @return true if referenceType and referenceId are set
     */
    public boolean hasReference() {
        return referenceType != null && referenceId != null;
    }

    /**
     * Check if event is high priority or urgent
     *
     * @return true if priority is HIGH or URGENT
     */
    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    /**
     * Check if event is urgent
     *
     * @return true if priority is URGENT
     */
    public boolean isUrgent() {
        return "URGENT".equals(priority);
    }

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "eventType='" + eventType + '\'' +
                ", recipientEmployeeNo=" + recipientEmployeeNo +
                ", priority='" + priority + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId=" + referenceId +
                ", variablesCount=" + templateVariables.size() +
                '}';
    }
}

