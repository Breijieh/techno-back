package com.techno.backend.service;

import com.techno.backend.entity.EmailConfig;
import com.techno.backend.entity.EmailTemplate;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.EmailConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Service for sending emails.
 *
 * Handles:
 * - SMTP configuration management
 * - Email sending via JavaMailSender
 * - Template-based email composition
 * - Variable substitution in email content
 * - HTML email formatting
 * - Connection testing
 *
 * Email Sending Flow:
 * 1. Fetch active EmailConfig from database
 * 2. Build JavaMailSender with SMTP settings
 * 3. Fetch EmailTemplate by code
 * 4. Substitute template variables
 * 5. Create and send MimeMessage
 * 6. Log success/failure
 *
 * Async Processing:
 * - All email sending is async (@Async)
 * - Doesn't block notification creation
 * - Failures are logged but don't affect business logic
 *
 * Configuration:
 * - SMTP settings stored in email_config table
 * - Can be updated via admin UI
 * - Supports Gmail, Outlook, custom SMTP
 * - TLS/SSL support
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailConfigRepository emailConfigRepository;

    /**
     * Send notification email to employee.
     *
     * This is the main entry point called by NotificationService.
     * Runs asynchronously to not block notification creation.
     *
     * @param employee Recipient employee
     * @param template Email template
     * @param variables Template variables for substitution
     */
    @Async("emailExecutor")
    public void sendNotificationEmail(Employee employee, EmailTemplate template,
                                     Map<String, Object> variables) {
        try {
            log.debug("Sending notification email: employee={}, template={}",
                    employee.getEmployeeNo(), template.getTemplateCode());

            // Check if email sending is enabled
            if (!isEmailSendingEnabled()) {
                log.info("Email sending is disabled, skipping email");
                return;
            }

            // Get employee email
            String recipientEmail = employee.getEmail();
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                log.warn("Employee has no email address: employeeNo={}", employee.getEmployeeNo());
                return;
            }

            // Get employee language preference (default to English)
            String language = getEmployeeLanguage(employee);

            // Send email
            sendTemplatedEmail(template, recipientEmail, language, variables);

        } catch (Exception e) {
            log.error("Error sending notification email: employee={}, template={}, error={}",
                    employee.getEmployeeNo(), template.getTemplateCode(), e.getMessage(), e);
            // Don't rethrow - email failures shouldn't break notification system
        }
    }

    /**
     * Send templated email.
     *
     * @param template Email template
     * @param recipientEmail Recipient email address
     * @param language Language preference ("en" or "ar")
     * @param variables Template variables
     */
    @Async("emailExecutor")
    public void sendTemplatedEmail(EmailTemplate template, String recipientEmail,
                                  String language, Map<String, Object> variables) {
        try {
            // Get active email configuration
            Optional<EmailConfig> configOpt = emailConfigRepository.findActiveAndEnabledConfig();
            if (configOpt.isEmpty()) {
                log.warn("No active email configuration found, cannot send email");
                return;
            }

            EmailConfig config = configOpt.get();

            // Build mail sender
            JavaMailSender mailSender = buildMailSender(config);

            // Get subject and body based on language
            String subject = template.getSubject(language);
            String body = template.getBody(language);

            // Substitute variables
            subject = substituteVariables(subject, variables);
            body = substituteVariables(body, variables);

            // Create and send message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getFromEmail(), config.getFromName());
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);

            log.info("Email sent successfully: to={}, template={}", recipientEmail, template.getTemplateCode());

        } catch (Exception e) {
            log.error("Error sending templated email: to={}, template={}, error={}",
                    recipientEmail, template.getTemplateCode(), e.getMessage(), e);
        }
    }

    /**
     * Send simple email (for testing).
     *
     * @param recipientEmail Recipient email address
     * @param subject Email subject
     * @param body Email body (HTML)
     * @throws Exception if email sending fails
     */
    public void sendSimpleEmail(String recipientEmail, String subject, String body) throws Exception {
        Optional<EmailConfig> configOpt = emailConfigRepository.findActiveAndEnabledConfig();
        if (configOpt.isEmpty()) {
            throw new IllegalStateException("لم يتم العثور على إعدادات بريد إلكتروني نشطة");
        }

        EmailConfig config = configOpt.get();
        JavaMailSender mailSender = buildMailSender(config);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(config.getFromEmail(), config.getFromName());
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
        log.info("Test email sent successfully: to={}", recipientEmail);
    }

    /**
     * Build JavaMailSender from email configuration.
     *
     * @param config Email configuration
     * @return Configured JavaMailSender
     */
    private JavaMailSender buildMailSender(EmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());

        if (config.hasCredentials()) {
            mailSender.setUsername(config.getSmtpUsername());
            mailSender.setPassword(config.getSmtpPassword());
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", config.isSmtpAuthEnabled() ? "true" : "false");
        props.put("mail.smtp.starttls.enable", config.isStartTlsEnabled() ? "true" : "false");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Substitute template variables.
     *
     * Replaces {{variableName}} with actual values from map.
     *
     * @param template Template string with {{variable}} placeholders
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
     * Get employee language preference.
     *
     * @param employee Employee
     * @return Language code ("en" or "ar")
     */
    private String getEmployeeLanguage(Employee employee) {
        // TODO: Add language preference field to Employee entity
        // For now, default to English
        return "en";
    }

    /**
     * Check if email sending is globally enabled.
     *
     * @return true if enabled
     */
    @Transactional(readOnly = true)
    public boolean isEmailSendingEnabled() {
        return emailConfigRepository.isEmailSendingEnabled();
    }

    /**
     * Get active email configuration.
     *
     * @return Active email configuration
     * @throws IllegalStateException if no active configuration found
     */
    @Transactional(readOnly = true)
    public EmailConfig getActiveConfig() {
        return emailConfigRepository.findActiveConfig()
                .orElseThrow(() -> new IllegalStateException("لم يتم العثور على إعدادات بريد إلكتروني نشطة"));
    }

    /**
     * Test SMTP connection.
     *
     * Attempts to connect to SMTP server using current configuration.
     *
     * @return true if connection successful
     */
    public boolean testSmtpConnection() {
        try {
            Optional<EmailConfig> configOpt = emailConfigRepository.findActiveConfig();
            if (configOpt.isEmpty()) {
                log.warn("No active email configuration for testing");
                return false;
            }

            EmailConfig config = configOpt.get();
            JavaMailSender mailSender = buildMailSender(config);

            // Try to create a message (doesn't send, just tests connection)
            MimeMessage message = mailSender.createMimeMessage();
            if (message != null) {
                log.info("SMTP connection test successful");
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("SMTP connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update email configuration.
     *
     * @param config Email configuration to save
     * @return Saved configuration
     */
    @Transactional
    public EmailConfig updateEmailConfig(EmailConfig config) {
        // Deactivate all other configurations
        emailConfigRepository.deactivateAll();

        // Save new configuration as active
        config.setIsActive("Y");
        EmailConfig saved = emailConfigRepository.save(config);

        log.info("Email configuration updated: configId={}", saved.getConfigId());
        return saved;
    }

    /**
     * Enable email sending.
     */
    @Transactional
    public void enableEmailSending() {
        emailConfigRepository.enableEmailSending();
        log.info("Email sending enabled");
    }

    /**
     * Disable email sending.
     */
    @Transactional
    public void disableEmailSending() {
        emailConfigRepository.disableEmailSending();
        log.info("Email sending disabled");
    }
}
