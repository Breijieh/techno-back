package com.techno.backend.constants;

/**
 * Constants for notification priority levels.
 *
 * Defines four priority levels used to categorize notification importance:
 * - LOW: Informational notifications (check-in, check-out)
 * - MEDIUM: Standard notifications (requests submitted, intermediate approvals)
 * - HIGH: Important notifications (final approvals, rejections, salary information)
 * - URGENT: Critical notifications (payroll issues, expired documents, excessive overtime)
 *
 * Priority levels affect:
 * - Notification display order (urgent first)
 * - Email sending priority
 * - UI highlighting and badges
 * - Filtering and sorting
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
public final class NotificationPriority {

    // Prevent instantiation
    private NotificationPriority() {
        throw new UnsupportedOperationException("هذه فئة مساعدة ولا يمكن إنشاء مثيل منها");
    }

    /**
     * LOW priority - Informational notifications
     * Examples:
     * - Check-in confirmation
     * - Check-out confirmation
     * - Loan installment paid
     */
    public static final String LOW = "LOW";

    /**
     * MEDIUM priority - Standard business notifications
     * Examples:
     * - Leave request submitted
     * - Loan request submitted
     * - Intermediate approvals
     * - Allowance/deduction submitted
     * - Late arrival notice
     */
    public static final String MEDIUM = "MEDIUM";

    /**
     * HIGH priority - Important notifications requiring attention
     * Examples:
     * - Leave approved/rejected
     * - Loan approved/rejected
     * - Payroll ready
     * - Allowance/deduction approved/rejected
     * - Salary raise processed
     * - Document expiring soon
     * - Absence detected
     */
    public static final String HIGH = "HIGH";

    /**
     * URGENT priority - Critical notifications requiring immediate action
     * Examples:
     * - Payroll rejected
     * - Document expired
     * - Overtime threshold exceeded (50+ hours)
     * - System critical alerts
     */
    public static final String URGENT = "URGENT";

    /**
     * Default priority for notifications
     */
    public static final String DEFAULT = MEDIUM;

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    /**
     * Check if priority is valid
     *
     * @param priority Priority to check
     * @return true if valid priority level
     */
    public static boolean isValid(String priority) {
        return LOW.equals(priority) ||
               MEDIUM.equals(priority) ||
               HIGH.equals(priority) ||
               URGENT.equals(priority);
    }

    /**
     * Get priority level as integer for sorting
     * Higher number = higher priority
     *
     * @param priority Priority constant
     * @return Priority level (1-4)
     */
    public static int getPriorityLevel(String priority) {
        if (priority == null) {
            return 2; // MEDIUM
        }
        switch (priority) {
            case URGENT:
                return 4;
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                return 2; // MEDIUM
        }
    }

    /**
     * Get priority from level number
     *
     * @param level Priority level (1-4)
     * @return Priority constant
     */
    public static String getPriorityFromLevel(int level) {
        switch (level) {
            case 4:
                return URGENT;
            case 3:
                return HIGH;
            case 2:
                return MEDIUM;
            case 1:
                return LOW;
            default:
                return MEDIUM;
        }
    }

    /**
     * Check if priority is high or urgent
     *
     * @param priority Priority to check
     * @return true if HIGH or URGENT
     */
    public static boolean isHighPriority(String priority) {
        return HIGH.equals(priority) || URGENT.equals(priority);
    }

    /**
     * Check if priority is urgent
     *
     * @param priority Priority to check
     * @return true if URGENT
     */
    public static boolean isUrgent(String priority) {
        return URGENT.equals(priority);
    }

    /**
     * Get default priority if null or invalid
     *
     * @param priority Priority to validate
     * @return Valid priority or DEFAULT
     */
    public static String getValidPriority(String priority) {
        if (priority == null || !isValid(priority)) {
            return DEFAULT;
        }
        return priority;
    }
}
