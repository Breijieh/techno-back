package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.EmailConfigRequest;
import com.techno.backend.dto.EmailConfigResponse;
import com.techno.backend.dto.TestEmailRequest;
import com.techno.backend.entity.EmailConfig;
import com.techno.backend.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Email Configuration.
 *
 * Provides endpoints for:
 * - Viewing email configuration
 * - Updating SMTP settings
 * - Testing SMTP connection
 * - Enabling/disabling email sending
 * - Sending test emails
 *
 * Security:
 * - All endpoints require ADMIN role
 * - Password is never returned in responses
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@RestController
@RequestMapping("/email-config")
@RequiredArgsConstructor
@Slf4j
public class EmailConfigController {

    private final EmailService emailService;

    /**
     * Get active email configuration.
     *
     * GET /api/email-config
     *
     * Password is excluded from response for security.
     *
     * @return Active email configuration
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailConfigResponse>> getEmailConfig() {
        log.info("GET /api/email-config");

        try {
            EmailConfig config = emailService.getActiveConfig();
            EmailConfigResponse response = toResponse(config);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalStateException e) {
            log.warn("No active email configuration found");
            return ResponseEntity.ok(ApiResponse.success(
                    "لم يتم العثور على إعدادات البريد الإلكتروني", null));
        }
    }

    /**
     * Update email configuration.
     *
     * PUT /api/email-config
     *
     * Creates or updates SMTP configuration.
     * Deactivates previous configuration if exists.
     *
     * @param request Email configuration request
     * @return Updated configuration
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailConfigResponse>> updateEmailConfig(
            @Valid @RequestBody EmailConfigRequest request) {

        log.info("PUT /api/email-config - host={}, port={}", request.getSmtpHost(), request.getSmtpPort());

        try {
            // Convert request to entity
            EmailConfig config = EmailConfig.builder()
                    .smtpHost(request.getSmtpHost())
                    .smtpPort(request.getSmtpPort())
                    .smtpUsername(request.getSmtpUsername())
                    .smtpPassword(request.getSmtpPassword())
                    .smtpAuth(request.getSmtpAuth())
                    .smtpStarttlsEnable(request.getSmtpStarttlsEnable())
                    .fromEmail(request.getFromEmail())
                    .fromName(request.getFromName())
                    .sendEmailsEnabled(request.getSendEmailsEnabled())
                    .build();

            EmailConfig saved = emailService.updateEmailConfig(config);
            EmailConfigResponse response = toResponse(saved);

            return ResponseEntity.ok(ApiResponse.success(
                    "تم تحديث إعدادات البريد الإلكتروني بنجاح", response));

        } catch (Exception e) {
            log.error("Failed to update email configuration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("فشل تحديث الإعدادات: " + e.getMessage()));
        }
    }

    /**
     * Test SMTP connection.
     *
     * POST /api/email-config/test
     *
     * Attempts to connect to SMTP server using current configuration.
     * Does not send any emails.
     *
     * @return Connection test result
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> testSmtpConnection() {
        log.info("POST /api/email-config/test");

        boolean success = emailService.testSmtpConnection();

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(
                    "تم الاتصال بـ SMTP بنجاح", true));
        } else {
            return ResponseEntity.ok(ApiResponse.error(
                    "فشل الاتصال بـ SMTP. يرجى التحقق من الإعدادات.", false));
        }
    }

    /**
     * Send test email.
     *
     * POST /api/email-config/test-email
     *
     * Sends a test email to verify SMTP configuration.
     *
     * @param request Test email request
     * @return Test email result
     */
    @PostMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(
            @Valid @RequestBody TestEmailRequest request) {

        log.info("POST /api/email-config/test-email - to={}", request.getRecipientEmail());

        try {
            emailService.sendSimpleEmail(
                    request.getRecipientEmail(),
                    request.getSubject(),
                    request.getBody()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    "تم إرسال بريد إلكتروني تجريبي بنجاح إلى " + request.getRecipientEmail(), null));

        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("فشل إرسال بريد تجريبي: " + e.getMessage()));
        }
    }

    /**
     * Enable email sending.
     *
     * POST /api/email-config/enable
     *
     * @return Success message
     */
    @PostMapping("/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enableEmailSending() {
        log.info("POST /api/email-config/enable");

        try {
            emailService.enableEmailSending();
            return ResponseEntity.ok(ApiResponse.success("تم تفعيل إرسال البريد الإلكتروني", null));

        } catch (Exception e) {
            log.error("Failed to enable email sending: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("فشل تفعيل إرسال البريد الإلكتروني: " + e.getMessage()));
        }
    }

    /**
     * Disable email sending.
     *
     * POST /api/email-config/disable
     *
     * @return Success message
     */
    @PostMapping("/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> disableEmailSending() {
        log.info("POST /api/email-config/disable");

        try {
            emailService.disableEmailSending();
            return ResponseEntity.ok(ApiResponse.success("تم إلغاء تفعيل إرسال البريد الإلكتروني", null));

        } catch (Exception e) {
            log.error("Failed to disable email sending: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("فشل إلغاء تفعيل إرسال البريد الإلكتروني: " + e.getMessage()));
        }
    }

    /**
     * Check if email sending is enabled.
     *
     * GET /api/email-config/status
     *
     * @return Email sending status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> getEmailSendingStatus() {
        log.info("GET /api/email-config/status");

        boolean enabled = emailService.isEmailSendingEnabled();
        return ResponseEntity.ok(ApiResponse.success(
                "Email sending is " + (enabled ? "enabled" : "disabled"), enabled));
    }

    /**
     * Convert EmailConfig entity to EmailConfigResponse DTO.
     *
     * Password is excluded for security.
     *
     * @param config EmailConfig entity
     * @return EmailConfigResponse DTO
     */
    private EmailConfigResponse toResponse(EmailConfig config) {
        return EmailConfigResponse.builder()
                .configId(config.getConfigId())
                .smtpHost(config.getSmtpHost())
                .smtpPort(config.getSmtpPort())
                .smtpUsername(config.getSmtpUsername())
                // Password is intentionally excluded
                .smtpAuth(config.getSmtpAuth())
                .smtpStarttlsEnable(config.getSmtpStarttlsEnable())
                .fromEmail(config.getFromEmail())
                .fromName(config.getFromName())
                .isActive(config.getIsActive())
                .sendEmailsEnabled(config.getSendEmailsEnabled())
                .createdDate(config.getCreatedDate())
                .modifiedDate(config.getModifiedDate())
                .hasPassword(config.hasCredentials())
                .build();
    }
}
