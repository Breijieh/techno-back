package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.Employee;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled service for document expiry alerts.
 *
 * This service runs a daily job at 8:00 AM to:
 * 1. Check for passports expiring within 14 days
 * 2. Check for residencies (Iqama) expiring within 14 days
 * 3. Check for already expired documents
 * 4. Log alerts for HR Manager
 *
 * Alert Priority Levels:
 * - CRITICAL: Expired or expiring within 7 days
 * - HIGH: Expiring within 14 days
 * - MEDIUM: Expiring within 30 days
 *
 * Note: Email and in-app notifications will be implemented in a future phase.
 * Currently using logging only as per Phase 8 requirements.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 8 - Batch Jobs & Automation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentExpiryAlertService {

    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Default alert threshold: 14 days as per requirements
     */
    private static final int DEFAULT_ALERT_THRESHOLD_DAYS = 14;

    /**
     * Critical alert threshold: 7 days
     */
    private static final int CRITICAL_ALERT_THRESHOLD_DAYS = 7;

    /**
     * Daily document expiry check.
     *
     * Runs at 8:00 AM Saudi Arabia time.
     *
     * Process:
     * 1. Find all employees with expired documents
     * 2. Find all employees with documents expiring within 14 days
     * 3. Categorize alerts by severity (CRITICAL, HIGH)
     * 4. Log detailed alerts for HR Manager
     *
     * This ensures:
     * - HR is alerted well in advance of document expirations
     * - No legal compliance issues due to expired residencies
     * - Proactive document renewal management
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Riyadh")
    @Transactional(readOnly = true)
    public void checkDocumentExpiry() {
        LocalDate today = LocalDate.now();
        log.info("=".repeat(80));
        log.info("Starting Document Expiry Alert Job at {}", today);
        log.info("=".repeat(80));

        try {
            // Check expired documents first
            checkExpiredDocuments(today);

            // Check expiring documents (within thresholds)
            checkExpiringDocuments(today);

            log.info("=".repeat(80));
            log.info("Document Expiry Alert Job Completed Successfully");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Document Expiry Alert Job Failed: {}", e.getMessage(), e);
            log.error("=".repeat(80));
        }
    }

    /**
     * Check for expired passports and residencies.
     *
     * @param today Current date
     */
    private void checkExpiredDocuments(LocalDate today) {
        log.info("\n--- CRITICAL: Checking Expired Documents ---");

        // Check expired passports
        List<Employee> expiredPassports = employeeRepository.findEmployeesWithExpiredPassports(today);
        if (!expiredPassports.isEmpty()) {
            log.error("CRITICAL ALERT: {} employees have EXPIRED PASSPORTS!", expiredPassports.size());
            expiredPassports.forEach(emp -> {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                        emp.getPassportExpiryDate(), today);
                log.error("  - Employee #{} ({}): Passport EXPIRED {} days ago (Expired: {})",
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        daysOverdue,
                        emp.getPassportExpiryDate());

                // Send notification to employee
                publishDocumentExpiredNotification(emp, "PASSPORT", emp.getPassportExpiryDate(), daysOverdue);
            });
        } else {
            log.info("  âœ“ No expired passports found");
        }

        // Check expired residencies
        List<Employee> expiredResidencies = employeeRepository.findEmployeesWithExpiredResidencies(today);
        if (!expiredResidencies.isEmpty()) {
            log.error("CRITICAL ALERT: {} employees have EXPIRED RESIDENCIES (Iqama)!", expiredResidencies.size());
            expiredResidencies.forEach(emp -> {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                        emp.getResidencyExpiryDate(), today);
                log.error("  - Employee #{} ({}): Residency EXPIRED {} days ago (Expired: {})",
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        daysOverdue,
                        emp.getResidencyExpiryDate());

                // Send notification to employee
                publishDocumentExpiredNotification(emp, "RESIDENCY", emp.getResidencyExpiryDate(), daysOverdue);
            });
        } else {
            log.info("  âœ“ No expired residencies found");
        }
    }

    /**
     * Check for documents expiring within threshold periods.
     *
     * @param today Current date
     */
    private void checkExpiringDocuments(LocalDate today) {
        // Critical threshold: 7 days
        checkExpiringDocumentsByThreshold(today, CRITICAL_ALERT_THRESHOLD_DAYS, "CRITICAL");

        // High priority threshold: 14 days
        checkExpiringDocumentsByThreshold(today, DEFAULT_ALERT_THRESHOLD_DAYS, "HIGH");
    }

    /**
     * Check for documents expiring within a specific threshold.
     *
     * @param today Current date
     * @param daysThreshold Number of days to check ahead
     * @param alertLevel Alert severity level
     */
    private void checkExpiringDocumentsByThreshold(LocalDate today, int daysThreshold, String alertLevel) {
        LocalDate expiryDate = today.plusDays(daysThreshold);

        log.info("\n--- {} PRIORITY: Checking Documents Expiring within {} days ---",
                alertLevel, daysThreshold);

        // Check expiring passports
        List<Employee> expiringPassports = employeeRepository.findEmployeesWithExpiringPassports(
                today, expiryDate);

        // Filter to only those within the exact threshold (not already caught by more urgent threshold)
        int lowerThreshold = (alertLevel.equals("CRITICAL")) ? 0 : CRITICAL_ALERT_THRESHOLD_DAYS;
        List<Employee> filteredPassports = expiringPassports.stream()
                .filter(emp -> {
                    long daysUntilExpiry = emp.getDaysUntilPassportExpiry();
                    return daysUntilExpiry > lowerThreshold && daysUntilExpiry <= daysThreshold;
                })
                .toList();

        if (!filteredPassports.isEmpty()) {
            log.warn("{} ALERT: {} employees have passports expiring within {} days:",
                    alertLevel, filteredPassports.size(), daysThreshold);
            filteredPassports.forEach(emp -> {
                log.warn("  - Employee #{} ({}): Passport expires in {} days ({})",
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        emp.getDaysUntilPassportExpiry(),
                        emp.getPassportExpiryDate());

                // Send notification to employee
                publishDocumentExpiringNotification(emp, "PASSPORT", emp.getPassportExpiryDate(),
                        emp.getDaysUntilPassportExpiry(), alertLevel);
            });
        } else {
            log.info("  âœ“ No passports expiring in {} days range", daysThreshold);
        }

        // Check expiring residencies
        List<Employee> expiringResidencies = employeeRepository.findEmployeesWithExpiringResidencies(
                today, expiryDate);

        // Filter to only those within the exact threshold
        List<Employee> filteredResidencies = expiringResidencies.stream()
                .filter(emp -> {
                    long daysUntilExpiry = emp.getDaysUntilResidencyExpiry();
                    return daysUntilExpiry > lowerThreshold && daysUntilExpiry <= daysThreshold;
                })
                .toList();

        if (!filteredResidencies.isEmpty()) {
            log.warn("{} ALERT: {} employees have residencies expiring within {} days:",
                    alertLevel, filteredResidencies.size(), daysThreshold);
            filteredResidencies.forEach(emp -> {
                log.warn("  - Employee #{} ({}): Residency expires in {} days ({})",
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        emp.getDaysUntilResidencyExpiry(),
                        emp.getResidencyExpiryDate());

                // Send notification to employee
                publishDocumentExpiringNotification(emp, "RESIDENCY", emp.getResidencyExpiryDate(),
                        emp.getDaysUntilResidencyExpiry(), alertLevel);
            });
        } else {
            log.info("  âœ“ No residencies expiring in {} days range", daysThreshold);
        }
    }

    /**
     * Manual trigger method for testing or on-demand checks.
     * Can be called from a controller endpoint for manual execution.
     *
     * @return Summary of the check
     */
    @Transactional(readOnly = true)
    public String performManualCheck() {
        LocalDate today = LocalDate.now();
        log.info("Manual document expiry check triggered");

        List<Employee> expiredPassports = employeeRepository.findEmployeesWithExpiredPassports(today);
        List<Employee> expiredResidencies = employeeRepository.findEmployeesWithExpiredResidencies(today);

        LocalDate expiryDate14Days = today.plusDays(DEFAULT_ALERT_THRESHOLD_DAYS);
        List<Employee> expiringPassports = employeeRepository.findEmployeesWithExpiringPassports(
                today, expiryDate14Days);
        List<Employee> expiringResidencies = employeeRepository.findEmployeesWithExpiringResidencies(
                today, expiryDate14Days);

        return String.format(
                "Document Expiry Check Summary:\n" +
                        "- Expired Passports: %d\n" +
                        "- Expired Residencies: %d\n" +
                        "- Passports Expiring (14 days): %d\n" +
                        "- Residencies Expiring (14 days): %d",
                expiredPassports.size(),
                expiredResidencies.size(),
                expiringPassports.size(),
                expiringResidencies.size()
        );
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when document has expired.
     */
    private void publishDocumentExpiredNotification(Employee employee, String documentType,
                                                    LocalDate expiryDate, long daysOverdue) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("documentType", documentType);
            variables.put("expiryDate", expiryDate.toString());
            variables.put("daysOverdue", String.valueOf(daysOverdue));
            variables.put("linkUrl", "/employees/" + employee.getEmployeeNo() + "/documents");

            String eventType = documentType.equals("PASSPORT") ?
                    NotificationEventType.DOCUMENT_PASSPORT_EXPIRED :
                    NotificationEventType.DOCUMENT_RESIDENCY_EXPIRED;

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    eventType,
                    employee.getEmployeeNo(),
                    NotificationPriority.URGENT,
                    "DOCUMENT_EXPIRY",
                    employee.getEmployeeNo(),
                    variables
            ));

            log.debug("Published {} notification for employee {}", eventType, employee.getEmployeeNo());
        } catch (Exception e) {
            log.error("Failed to publish document expired notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when document is expiring soon.
     */
    private void publishDocumentExpiringNotification(Employee employee, String documentType,
                                                     LocalDate expiryDate, long daysUntilExpiry,
                                                     String alertLevel) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("documentType", documentType);
            variables.put("expiryDate", expiryDate.toString());
            variables.put("daysUntilExpiry", String.valueOf(daysUntilExpiry));
            variables.put("linkUrl", "/employees/" + employee.getEmployeeNo() + "/documents");

            String eventType = documentType.equals("PASSPORT") ?
                    NotificationEventType.DOCUMENT_PASSPORT_EXPIRING :
                    NotificationEventType.DOCUMENT_RESIDENCY_EXPIRING;

            // Set priority based on alert level
            String priority = alertLevel.equals("CRITICAL") ?
                    NotificationPriority.HIGH : NotificationPriority.MEDIUM;

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    eventType,
                    employee.getEmployeeNo(),
                    priority,
                    "DOCUMENT_EXPIRY",
                    employee.getEmployeeNo(),
                    variables
            ));

            log.debug("Published {} notification for employee {}", eventType, employee.getEmployeeNo());
        } catch (Exception e) {
            log.error("Failed to publish document expiring notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send manual reminder notification for expiring document.
     * Can be triggered by HR Manager or Admin from the UI.
     *
     * @param employee Employee to send reminder to
     * @param documentType Document type (PASSPORT or RESIDENCY)
     */
    @Transactional
    public void sendManualDocumentReminder(Employee employee, String documentType) {
        log.info("Manual reminder requested for employee {} - documentType: {}", 
                employee.getEmployeeNo(), documentType);

        LocalDate today = LocalDate.now();
        
        if ("PASSPORT".equals(documentType)) {
            if (employee.getPassportExpiryDate() == null) {
                throw new IllegalArgumentException("Ø§Ù„Ù…ÙˆØ¸Ù Ù„Ø§ ÙŠÙ…ØªÙ„Ùƒ ØªØ§Ø±ÙŠØ® Ø§Ù†ØªÙ‡Ø§Ø¡ Ø¬ÙˆØ§Ø² Ø³ÙØ±");
            }
            
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    today, employee.getPassportExpiryDate());
            
            boolean isExpired = employee.getPassportExpiryDate().isBefore(today);
            String alertLevel = isExpired ? "CRITICAL" : 
                    (daysUntilExpiry <= 7 ? "CRITICAL" : 
                     (daysUntilExpiry <= 14 ? "HIGH" : 
                      (daysUntilExpiry <= 30 ? "MEDIUM" : "LOW")));
            
            if (isExpired) {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                        employee.getPassportExpiryDate(), today);
                publishDocumentExpiredNotification(employee, "PASSPORT", 
                        employee.getPassportExpiryDate(), daysOverdue);
            } else {
                publishDocumentExpiringNotification(employee, "PASSPORT",
                        employee.getPassportExpiryDate(), daysUntilExpiry, alertLevel);
            }
            
        } else if ("RESIDENCY".equals(documentType)) {
            if (employee.getResidencyExpiryDate() == null) {
                throw new IllegalArgumentException("Ø§Ù„Ù…ÙˆØ¸Ù Ù„Ø§ ÙŠÙ…ØªÙ„Ùƒ ØªØ§Ø±ÙŠØ® Ø§Ù†ØªÙ‡Ø§Ø¡ Ø§Ù„Ø¥Ù‚Ø§Ù…Ø©");
            }
            
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    today, employee.getResidencyExpiryDate());
            
            boolean isExpired = employee.getResidencyExpiryDate().isBefore(today);
            String alertLevel = isExpired ? "CRITICAL" : 
                    (daysUntilExpiry <= 7 ? "CRITICAL" : 
                     (daysUntilExpiry <= 14 ? "HIGH" : 
                      (daysUntilExpiry <= 30 ? "MEDIUM" : "LOW")));
            
            if (isExpired) {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                        employee.getResidencyExpiryDate(), today);
                publishDocumentExpiredNotification(employee, "RESIDENCY", 
                        employee.getResidencyExpiryDate(), daysOverdue);
            } else {
                publishDocumentExpiringNotification(employee, "RESIDENCY",
                        employee.getResidencyExpiryDate(), daysUntilExpiry, alertLevel);
            }
        } else {
            throw new IllegalArgumentException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø³ØªÙ†Ø¯ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† PASSPORT Ø£Ùˆ RESIDENCY");
        }
        
        log.info("Manual reminder sent successfully for employee {} - documentType: {}", 
                employee.getEmployeeNo(), documentType);
    }
}
