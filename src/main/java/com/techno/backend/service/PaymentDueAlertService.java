package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.ProjectDuePayment;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.ProjectDuePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled service for project payment due alerts.
 *
 * This service runs a daily job at 9:00 AM to:
 * 1. Check for project payments (from clients to Techno) expiring within 30 days
 * 2. Check for project payments expiring within 14 days
 * 3. Check for project payments expiring within 2 days (urgent)
 * 4. Log alerts for Project Manager, Regional Manager, and Finance
 *
 * Alert Thresholds:
 * - NORMAL: 30 days before due date
 * - HIGH: 14 days before due date
 * - URGENT: 2 days before due date
 *
 * Note: Email and in-app notifications will be implemented in a future phase.
 * Currently using logging only as per Phase 8 requirements.
 *
 * Project Payments Context:
 * - These are payments FROM clients TO Techno (project revenue)
 * - Tracked in PROJECTS_DUE_PAYMENTS table
 * - Each project can have multiple payment installments
 * - Payment schedule is created when project is set up
 *
 * IMPORTANT: This service requires the ProjectsDuePayments entity to be implemented.
 * Once the entity and repository are created, this service will query for payments
 * due within the specified thresholds and log appropriate alerts.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 8 - Batch Jobs & Automation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentDueAlertService {

    private final ProjectDuePaymentRepository paymentsRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Threshold for normal priority alert (30 days before due)
     */
    private static final int THRESHOLD_NORMAL_DAYS = 30;

    /**
     * Threshold for high priority alert (14 days before due)
     */
    private static final int THRESHOLD_HIGH_DAYS = 14;

    /**
     * Threshold for urgent priority alert (2 days before due)
     */
    private static final int THRESHOLD_URGENT_DAYS = 2;

    /**
     * Daily payment due check.
     *
     * Runs at 9:00 AM Saudi Arabia time.
     *
     * Process:
     * 1. Get current date
     * 2. Find all project payments with status = 'UNPAID'
     * 3. For each payment, check if due_date is within alert thresholds
     * 4. Log alerts with appropriate priority level
     * 5. Recipients: Project Manager, Regional Manager, Finance
     *
     * This ensures:
     * - Proactive tracking of client payments
     * - Timely follow-up on overdue payments
     * - Better cash flow management
     * - Early detection of payment delays
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Riyadh")
    @Transactional(readOnly = true)
    public void checkPaymentDueAlerts() {
        LocalDate today = LocalDate.now();

        log.info("=".repeat(80));
        log.info("Starting Payment Due Alert Job at {}", today);
        log.info("=".repeat(80));

        try {
            // Check for overdue payments (CRITICAL)
            List<ProjectDuePayment> overduePayments = paymentsRepository.findOverduePayments(today);
            notifyOverduePayments(overduePayments);

            // Check URGENT (2 days)
            LocalDate urgentDate = today.plusDays(THRESHOLD_URGENT_DAYS);
            List<ProjectDuePayment> urgentPayments =
                    paymentsRepository.findPaymentsDueWithinPeriod(today, urgentDate);
            notifyUrgentPayments(urgentPayments);

            // Check HIGH (14 days)
            LocalDate highDate = today.plusDays(THRESHOLD_HIGH_DAYS);
            List<ProjectDuePayment> highPayments =
                    paymentsRepository.findPaymentsDueWithinPeriod(urgentDate.plusDays(1), highDate);
            notifyHighPriorityPayments(highPayments);

            // Check NORMAL (30 days)
            LocalDate normalDate = today.plusDays(THRESHOLD_NORMAL_DAYS);
            List<ProjectDuePayment> normalPayments =
                    paymentsRepository.findPaymentsDueWithinPeriod(highDate.plusDays(1), normalDate);
            notifyNormalPriorityPayments(normalPayments);

            int totalAlerts = overduePayments.size() + urgentPayments.size() +
                    highPayments.size() + normalPayments.size();

            log.info("=".repeat(80));
            log.info("Payment Due Alert Job Completed Successfully");
            log.info("Total Alerts: {} (Overdue: {}, Urgent: {}, High: {}, Normal: {})",
                    totalAlerts, overduePayments.size(), urgentPayments.size(),
                    highPayments.size(), normalPayments.size());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Payment Due Alert Job Failed: {}", e.getMessage(), e);
            log.error("=".repeat(80));
        }
    }

    /**
     * Notify overdue payments (CRITICAL priority).
     *
     * Called when payments are past their due date.
     * Recipients: Project Manager, Finance Manager, General Manager
     */
    private void notifyOverduePayments(List<ProjectDuePayment> payments) {
        if (payments.isEmpty()) {
            log.debug("No overdue payments found");
            return;
        }

        log.error("\n" + "!".repeat(80));
        log.error("CRITICAL: {} OVERDUE PROJECT PAYMENTS", payments.size());
        log.error("!".repeat(80));

        for (ProjectDuePayment payment : payments) {
            long daysOverdue = ChronoUnit.DAYS.between(payment.getDueDate(), LocalDate.now());
            log.error("Payment #{} - Project {}: {} SAR - {} days overdue",
                    payment.getSequenceNo(),
                    payment.getProject().getProjectName(),
                    payment.getDueAmount(),
                    daysOverdue);

            // Publish notification event
            publishPaymentAlert(payment, NotificationPriority.URGENT,
                    "OVERDUE", daysOverdue + " days overdue");
        }

        log.error("Priority: CRITICAL - IMMEDIATE action required");
        log.error("!".repeat(80) + "\n");
    }

    /**
     * Notify urgent payments (2 days before due).
     */
    private void notifyUrgentPayments(List<ProjectDuePayment> payments) {
        if (payments.isEmpty()) {
            log.debug("No urgent payments found (within 2 days)");
            return;
        }

        log.warn("\n" + "!".repeat(80));
        log.warn("URGENT: {} PROJECT PAYMENTS DUE WITHIN 2 DAYS", payments.size());
        log.warn("!".repeat(80));

        for (ProjectDuePayment payment : payments) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), payment.getDueDate());
            log.warn("Payment #{} - Project {}: {} SAR - Due in {} days ({})",
                    payment.getSequenceNo(),
                    payment.getProject().getProjectName(),
                    payment.getDueAmount(),
                    daysUntilDue,
                    payment.getDueDate());

            publishPaymentAlert(payment, NotificationPriority.URGENT,
                    "DUE_SOON", "Due in " + daysUntilDue + " days");
        }

        log.warn("Priority: URGENT - Contact client immediately");
        log.warn("!".repeat(80) + "\n");
    }

    /**
     * Notify high priority payments (14 days before due).
     */
    private void notifyHighPriorityPayments(List<ProjectDuePayment> payments) {
        if (payments.isEmpty()) {
            log.debug("No high priority payments found (within 14 days)");
            return;
        }

        log.warn("\n" + "*".repeat(80));
        log.warn("HIGH PRIORITY: {} PROJECT PAYMENTS DUE WITHIN 14 DAYS", payments.size());
        log.warn("*".repeat(80));

        for (ProjectDuePayment payment : payments) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), payment.getDueDate());
            log.warn("Payment #{} - Project {}: {} SAR - Due: {}",
                    payment.getSequenceNo(),
                    payment.getProject().getProjectName(),
                    payment.getDueAmount(),
                    payment.getDueDate());

            publishPaymentAlert(payment, NotificationPriority.HIGH,
                    "UPCOMING", "Due in " + daysUntilDue + " days");
        }

        log.warn("Priority: HIGH - Prepare follow-up");
        log.warn("*".repeat(80) + "\n");
    }

    /**
     * Notify normal priority payments (30 days before due).
     */
    private void notifyNormalPriorityPayments(List<ProjectDuePayment> payments) {
        if (payments.isEmpty()) {
            log.debug("No normal priority payments found (within 30 days)");
            return;
        }

        log.info("\n" + "=".repeat(80));
        log.info("NORMAL: {} PROJECT PAYMENTS DUE WITHIN 30 DAYS", payments.size());
        log.info("=".repeat(80));

        for (ProjectDuePayment payment : payments) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), payment.getDueDate());
            log.info("Payment #{} - Project {}: {} SAR - Due: {}",
                    payment.getSequenceNo(),
                    payment.getProject().getProjectName(),
                    payment.getDueAmount(),
                    payment.getDueDate());

            publishPaymentAlert(payment, NotificationPriority.MEDIUM,
                    "REMINDER", "Due in " + daysUntilDue + " days");
        }

        log.info("Priority: NORMAL - Monitor timeline");
        log.info("=".repeat(80) + "\n");
    }

    /**
     * Publish notification event for payment alert.
     *
     * @param payment Payment information
     * @param priority Notification priority
     * @param alertType Type of alert (OVERDUE, DUE_SOON, UPCOMING, REMINDER)
     * @param message Alert message
     */
    private void publishPaymentAlert(ProjectDuePayment payment, String priority,
                                      String alertType, String message) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectName", payment.getProject().getProjectName());
            variables.put("projectCode", payment.getProjectCode().toString());
            variables.put("paymentSequence", payment.getSequenceNo().toString());
            variables.put("dueAmount", payment.getDueAmount().toString());
            variables.put("dueDate", payment.getDueDate().toString());
            variables.put("alertType", alertType);
            variables.put("message", message);
            variables.put("linkUrl", "/projects/" + payment.getProjectCode() + "/payments");

            // Notify project manager
            Long projectManager = payment.getProject().getProjectMgr();
            if (projectManager != null) {
                eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        NotificationEventType.PAYMENT_DUE_ALERT,
                        projectManager,
                        priority,
                        "PAYMENT_ALERT",
                        payment.getPaymentId(),
                        variables
                ));
            }

            log.debug("Published payment alert notification for payment ID: {}", payment.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to publish payment alert notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger method for testing.
     *
     * @return Summary of the check
     */
    @Transactional(readOnly = true)
    public String performManualCheck() {
        log.info("Manual payment due check triggered");
        checkPaymentDueAlerts();

        LocalDate today = LocalDate.now();
        int overdueCount = paymentsRepository.findOverduePayments(today).size();
        int urgentCount = paymentsRepository.findPaymentsDueWithinPeriod(today, today.plusDays(THRESHOLD_URGENT_DAYS)).size();
        int highCount = paymentsRepository.findPaymentsDueWithinPeriod(today.plusDays(THRESHOLD_URGENT_DAYS + 1), today.plusDays(THRESHOLD_HIGH_DAYS)).size();
        int normalCount = paymentsRepository.findPaymentsDueWithinPeriod(today.plusDays(THRESHOLD_HIGH_DAYS + 1), today.plusDays(THRESHOLD_NORMAL_DAYS)).size();

        return String.format("Ø§ÙƒØªÙ…Ù„Øª ÙØ­Øµ ØªÙ†Ø¨ÙŠÙ‡Ø§Øª Ø§Ø³ØªØ­Ù‚Ø§Ù‚ Ø§Ù„Ø¯ÙØ¹:\n" +
                "- Ù…ØªØ£Ø®Ø±: %d Ø¯ÙØ¹Ø© (Ø­Ø±Ø¬)\n" +
                "- Ø¹Ø§Ø¬Ù„ (ÙŠÙˆÙ…ÙŠÙ†): %d Ø¯ÙØ¹Ø©\n" +
                "- Ø¹Ø§Ù„ÙŠ (14 ÙŠÙˆÙ…): %d Ø¯ÙØ¹Ø©\n" +
                "- Ø¹Ø§Ø¯ÙŠ (30 ÙŠÙˆÙ…): %d Ø¯ÙØ¹Ø©\n" +
                "- Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„ØªÙ†Ø¨ÙŠÙ‡Ø§Øª: %d\n" +
                "- Ù…Ø¬Ø¯ÙˆÙ„: ÙŠÙˆÙ…ÙŠØ§Ù‹ Ø§Ù„Ø³Ø§Ø¹Ø© 9:00 ØµØ¨Ø§Ø­Ø§Ù‹ Ø¨ØªÙˆÙ‚ÙŠØª Ø§Ù„Ø³Ø¹ÙˆØ¯ÙŠØ©",
                overdueCount, urgentCount, highCount, normalCount,
                overdueCount + urgentCount + highCount + normalCount);
    }
}

