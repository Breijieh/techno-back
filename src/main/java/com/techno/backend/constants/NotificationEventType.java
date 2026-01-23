package com.techno.backend.constants;

/**
 * Constants for all notification event types in the system.
 *
 * Each constant represents a specific business event that triggers a notification.
 * These codes are used as:
 * - Event type identifiers when publishing notifications
 * - Template codes for email templates
 * - Notification type classification in the database
 *
 * Total: 56 notification types across 9 categories:
 * - Leave Management (5 types)
 * - Loan Management (9 types)
 * - Payroll Processing (6 types)
 * - Attendance Tracking (6 types)
 * - Allowances (3 types)
 * - Deductions (3 types)
 * - Salary Raises (1 type)
 * - Project Management (10 types)
 * - System Alerts (13 types)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
public final class NotificationEventType {

    // Prevent instantiation
    private NotificationEventType() {
        throw new UnsupportedOperationException("هذه فئة مساعدة ولا يمكن إنشاء مثيل منها");
    }

    // ===========================================
    // LEAVE MANAGEMENT (5 types)
    // ===========================================

    /**
     * Leave request submitted - notify next approver
     * Recipient: Next approver in workflow
     * Priority: MEDIUM
     */
    public static final String LEAVE_SUBMITTED = "LEAVE_SUBMITTED";

    /**
     * Leave request approved (intermediate level) - notify next approver
     * Recipient: Next approver in workflow
     * Priority: MEDIUM
     */
    public static final String LEAVE_APPROVED_INTERMEDIATE = "LEAVE_APPROVED_INTERMEDIATE";

    /**
     * Leave request fully approved (final level) - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String LEAVE_APPROVED_FINAL = "LEAVE_APPROVED_FINAL";

    /**
     * Leave request rejected - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";

    /**
     * Leave request cancelled - notify approvers
     * Recipient: All approvers in workflow
     * Priority: MEDIUM
     */
    public static final String LEAVE_CANCELLED = "LEAVE_CANCELLED";

    // ===========================================
    // LOAN MANAGEMENT (9 types)
    // ===========================================

    /**
     * Loan request submitted - notify HR Manager
     * Recipient: HR Manager
     * Priority: MEDIUM
     */
    public static final String LOAN_SUBMITTED = "LOAN_SUBMITTED";

    /**
     * Loan request approved (intermediate level) - notify Finance Manager
     * Recipient: Finance Manager
     * Priority: MEDIUM
     */
    public static final String LOAN_APPROVED_INTERMEDIATE = "LOAN_APPROVED_INTERMEDIATE";

    /**
     * Loan request fully approved - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String LOAN_APPROVED_FINAL = "LOAN_APPROVED_FINAL";

    /**
     * Loan request rejected - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String LOAN_REJECTED = "LOAN_REJECTED";

    /**
     * Loan postponement request submitted - notify approvers
     * Recipient: HR Manager or Finance Manager
     * Priority: MEDIUM
     */
    public static final String LOAN_POSTPONEMENT_SUBMITTED = "LOAN_POSTPONEMENT_SUBMITTED";

    /**
     * Loan postponement approved - notify employee
     * Recipient: Employee who requested postponement
     * Priority: HIGH
     */
    public static final String LOAN_POSTPONEMENT_APPROVED = "LOAN_POSTPONEMENT_APPROVED";

    /**
     * Loan postponement rejected - notify employee
     * Recipient: Employee who requested postponement
     * Priority: HIGH
     */
    public static final String LOAN_POSTPONEMENT_REJECTED = "LOAN_POSTPONEMENT_REJECTED";

    /**
     * Loan fully paid - notify employee
     * Recipient: Employee
     * Priority: MEDIUM
     */
    public static final String LOAN_FULLY_PAID = "LOAN_FULLY_PAID";

    /**
     * Loan installment paid - notify employee
     * Recipient: Employee
     * Priority: LOW
     */
    public static final String LOAN_INSTALLMENT_PAID = "LOAN_INSTALLMENT_PAID";

    // ===========================================
    // PAYROLL PROCESSING (6 types)
    // ===========================================

    /**
     * Payroll calculated and ready for approval - notify Finance
     * Recipient: Finance Manager
     * Priority: HIGH
     */
    public static final String PAYROLL_CALCULATED = "PAYROLL_CALCULATED";

    /**
     * Payroll approved Level 1 - notify Level 2 approver
     * Recipient: Finance Manager
     * Priority: MEDIUM
     */
    public static final String PAYROLL_APPROVED_L1 = "PAYROLL_APPROVED_L1";

    /**
     * Payroll approved Level 2 - notify Level 3 approver
     * Recipient: General Manager
     * Priority: MEDIUM
     */
    public static final String PAYROLL_APPROVED_L2 = "PAYROLL_APPROVED_L2";

    /**
     * Payroll fully approved - notify employee (salary ready)
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String PAYROLL_APPROVED_FINAL = "PAYROLL_APPROVED_FINAL";

    /**
     * Payroll rejected - notify HR/Finance
     * Recipient: HR Manager and Finance Manager
     * Priority: URGENT
     */
    public static final String PAYROLL_REJECTED = "PAYROLL_REJECTED";

    /**
     * Payroll recalculated - notify Finance
     * Recipient: Finance Manager
     * Priority: HIGH
     */
    public static final String PAYROLL_RECALCULATED = "PAYROLL_RECALCULATED";

    // ===========================================
    // ATTENDANCE TRACKING (6 types)
    // ===========================================

    /**
     * Employee checked in - notify employee (optional)
     * Recipient: Employee
     * Priority: LOW
     */
    public static final String ATTENDANCE_CHECK_IN = "ATTENDANCE_CHECK_IN";

    /**
     * Employee checked out - notify employee
     * Recipient: Employee
     * Priority: LOW
     */
    public static final String ATTENDANCE_CHECK_OUT = "ATTENDANCE_CHECK_OUT";

    /**
     * Manual attendance record created - notify employee
     * Recipient: Employee
     * Priority: MEDIUM
     */
    public static final String ATTENDANCE_MANUAL_CREATED = "ATTENDANCE_MANUAL_CREATED";

    /**
     * Late arrival detected - notify employee and manager
     * Recipient: Employee and Manager
     * Priority: MEDIUM
     */
    public static final String ATTENDANCE_LATE_ARRIVAL = "ATTENDANCE_LATE_ARRIVAL";

    /**
     * Early departure detected - notify employee and manager
     * Recipient: Employee and Manager
     * Priority: MEDIUM
     */
    public static final String ATTENDANCE_EARLY_DEPARTURE = "ATTENDANCE_EARLY_DEPARTURE";

    /**
     * Absence detected - notify employee and manager
     * Recipient: Employee and Manager
     * Priority: HIGH
     */
    public static final String ATTENDANCE_ABSENCE = "ATTENDANCE_ABSENCE";

    /**
     * Manual attendance request rejected - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String MANUAL_ATTENDANCE_REJECTED = "MANUAL_ATTENDANCE_REJECTED";

    // ===========================================
    // ALLOWANCES (3 types)
    // ===========================================

    /**
     * Manual allowance submitted - notify HR Manager
     * Recipient: HR Manager
     * Priority: MEDIUM
     */
    public static final String ALLOWANCE_SUBMITTED = "ALLOWANCE_SUBMITTED";

    /**
     * Allowance approved - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String ALLOWANCE_APPROVED = "ALLOWANCE_APPROVED";

    /**
     * Allowance rejected - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String ALLOWANCE_REJECTED = "ALLOWANCE_REJECTED";

    // ===========================================
    // DEDUCTIONS (3 types)
    // ===========================================

    /**
     * Manual deduction submitted - notify Finance Manager
     * Recipient: Finance Manager
     * Priority: MEDIUM
     */
    public static final String DEDUCTION_SUBMITTED = "DEDUCTION_SUBMITTED";

    /**
     * Deduction approved - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String DEDUCTION_APPROVED = "DEDUCTION_APPROVED";

    /**
     * Deduction rejected - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String DEDUCTION_REJECTED = "DEDUCTION_REJECTED";

    // ===========================================
    // SALARY RAISES (1 type)
    // ===========================================

    /**
     * Salary raise processed - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String SALARY_RAISE_PROCESSED = "SALARY_RAISE_PROCESSED";

    // ===========================================
    // SYSTEM ALERTS (13 types)
    // ===========================================

    /**
     * Passport expired - notify employee
     * Recipient: Employee
     * Priority: URGENT
     */
    public static final String DOCUMENT_PASSPORT_EXPIRED = "DOCUMENT_PASSPORT_EXPIRED";

    /**
     * Residency expired - notify employee
     * Recipient: Employee
     * Priority: URGENT
     */
    public static final String DOCUMENT_RESIDENCY_EXPIRED = "DOCUMENT_RESIDENCY_EXPIRED";

    /**
     * Passport expiring soon - notify employee
     * Recipient: Employee
     * Priority: HIGH/MEDIUM
     */
    public static final String DOCUMENT_PASSPORT_EXPIRING = "DOCUMENT_PASSPORT_EXPIRING";

    /**
     * Residency expiring soon - notify employee
     * Recipient: Employee
     * Priority: HIGH/MEDIUM
     */
    public static final String DOCUMENT_RESIDENCY_EXPIRING = "DOCUMENT_RESIDENCY_EXPIRING";

    /**
     * Employee overtime reached 30 hours - notify employee
     * Recipient: Employee
     * Priority: HIGH
     */
    public static final String OVERTIME_THRESHOLD_NORMAL = "OVERTIME_THRESHOLD_NORMAL";

    /**
     * Employee overtime reached 50 hours - notify employee (urgent)
     * Recipient: Employee
     * Priority: URGENT
     */
    public static final String OVERTIME_THRESHOLD_URGENT = "OVERTIME_THRESHOLD_URGENT";

    /**
     * Document expired - notify HR Manager
     * Recipient: HR Manager
     * Priority: URGENT
     */
    public static final String DOCUMENT_EXPIRY_CRITICAL = "DOCUMENT_EXPIRY_CRITICAL";

    /**
     * Document expiring in 7 days - notify HR Manager
     * Recipient: HR Manager
     * Priority: HIGH
     */
    public static final String DOCUMENT_EXPIRY_HIGH = "DOCUMENT_EXPIRY_HIGH";

    /**
     * Document expiring in 14 days - notify HR Manager
     * Recipient: HR Manager
     * Priority: MEDIUM
     */
    public static final String DOCUMENT_EXPIRY_MEDIUM = "DOCUMENT_EXPIRY_MEDIUM";

    /**
     * Passport expiring - notify HR Manager
     * Recipient: HR Manager
     * Priority: HIGH
     */
    public static final String PASSPORT_EXPIRING = "PASSPORT_EXPIRING";

    /**
     * Residency expiring - notify HR Manager
     * Recipient: HR Manager
     * Priority: HIGH
     */
    public static final String RESIDENCY_EXPIRING = "RESIDENCY_EXPIRING";

    /**
     * Employee overtime reached 30 hours - notify managers
     * Recipient: HR, Finance, General Manager
     * Priority: MEDIUM
     */
    public static final String OVERTIME_ALERT_30H = "OVERTIME_ALERT_30H";

    /**
     * Employee overtime reached 50 hours - notify managers (urgent)
     * Recipient: HR, Finance, General Manager
     * Priority: URGENT
     */
    public static final String OVERTIME_ALERT_50H = "OVERTIME_ALERT_50H";

    /**
     * Project payment due soon - notify project managers
     * Recipient: Project Manager, Regional Manager, Finance
     * Priority: HIGH
     */
    public static final String PAYMENT_DUE_ALERT = "PAYMENT_DUE_ALERT";

    /**
     * Labor request closed - notify managers
     * Recipient: Department and Project Managers
     * Priority: MEDIUM
     */
    public static final String LABOR_REQUEST_CLOSED = "LABOR_REQUEST_CLOSED";

    // ===========================================
    // PROJECT MANAGEMENT (10 types)
    // ===========================================

    /**
     * Payment request submitted - notify project manager
     * Recipient: Project Manager
     * Priority: MEDIUM
     */
    public static final String PAYMENT_REQUEST_SUBMITTED = "PAYMENT_REQUEST_SUBMITTED";

    /**
     * Payment request approved (intermediate level) - notify next approver
     * Recipient: Regional Manager or Finance Manager
     * Priority: MEDIUM
     */
    public static final String PAYMENT_REQUEST_APPROVED_INTERMEDIATE = "PAYMENT_REQUEST_APPROVED_INTERMEDIATE";

    /**
     * Payment request fully approved - notify requester
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String PAYMENT_REQUEST_APPROVED_FINAL = "PAYMENT_REQUEST_APPROVED_FINAL";

    /**
     * Payment request rejected - notify requester
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String PAYMENT_REQUEST_REJECTED = "PAYMENT_REQUEST_REJECTED";

    /**
     * Employee transfer request submitted - notify current project manager
     * Recipient: Current Project Manager
     * Priority: MEDIUM
     */
    public static final String TRANSFER_SUBMITTED = "TRANSFER_SUBMITTED";

    /**
     * Transfer request approved (intermediate level) - notify target project manager
     * Recipient: Target Project Manager
     * Priority: MEDIUM
     */
    public static final String TRANSFER_APPROVED_INTERMEDIATE = "TRANSFER_APPROVED_INTERMEDIATE";

    /**
     * Transfer request fully approved - notify employee
     * Recipient: Employee being transferred
     * Priority: HIGH
     */
    public static final String TRANSFER_APPROVED_FINAL = "TRANSFER_APPROVED_FINAL";

    /**
     * Transfer request rejected - notify employee
     * Recipient: Employee who submitted request
     * Priority: HIGH
     */
    public static final String TRANSFER_REJECTED = "TRANSFER_REJECTED";

    /**
     * Labor request created - notify HR
     * Recipient: HR Manager
     * Priority: MEDIUM
     */
    public static final String LABOR_REQUEST_CREATED = "LABOR_REQUEST_CREATED";

    /**
     * Worker assigned to project - notify employee and project manager
     * Recipient: Employee and Project Manager
     * Priority: MEDIUM
     */
    public static final String LABOR_ASSIGNED = "LABOR_ASSIGNED";

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    /**
     * Get category for a notification type
     *
     * @param eventType Event type constant
     * @return Category name (LEAVE, LOAN, PAYROLL, etc.)
     */
    public static String getCategory(String eventType) {
        if (eventType == null) {
            return "UNKNOWN";
        }

        if (eventType.startsWith("LEAVE_")) return "LEAVE";
        if (eventType.startsWith("LOAN_")) return "LOAN";
        if (eventType.startsWith("PAYROLL_")) return "PAYROLL";
        if (eventType.startsWith("ATTENDANCE_") || eventType.equals("MANUAL_ATTENDANCE_REJECTED")) return "ATTENDANCE";
        if (eventType.startsWith("ALLOWANCE_")) return "ALLOWANCE";
        if (eventType.startsWith("DEDUCTION_")) return "DEDUCTION";
        if (eventType.startsWith("SALARY_RAISE_")) return "SALARY_RAISE";

        // Project Management notifications
        if (eventType.startsWith("PAYMENT_REQUEST_") ||
            eventType.startsWith("TRANSFER_") ||
            eventType.equals("LABOR_REQUEST_CREATED") ||
            eventType.equals("LABOR_ASSIGNED")) {
            return "PROJECT";
        }

        // System Alerts (including PAYMENT_DUE_ALERT and LABOR_REQUEST_CLOSED)
        if (eventType.startsWith("DOCUMENT_") || eventType.startsWith("PASSPORT_") ||
            eventType.startsWith("RESIDENCY_") || eventType.startsWith("OVERTIME_") ||
            eventType.equals("PAYMENT_DUE_ALERT") || eventType.equals("LABOR_REQUEST_CLOSED")) {
            return "ALERT";
        }

        return "UNKNOWN";
    }

    /**
     * Check if event type is valid
     *
     * @param eventType Event type to check
     * @return true if valid event type
     */
    public static boolean isValid(String eventType) {
        return !"UNKNOWN".equals(getCategory(eventType));
    }
}
