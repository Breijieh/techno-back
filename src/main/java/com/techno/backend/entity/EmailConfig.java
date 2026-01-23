package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing email/SMTP configuration.
 *
 * Stores SMTP server settings for sending emails:
 * - SMTP host and port
 * - Authentication credentials
 * - From email address and name
 * - TLS/SSL settings
 * - Email sending enable/disable flag
 *
 * System supports single active configuration at a time.
 * Typically configured once during initial setup.
 *
 * Features:
 * - Password encryption support (encrypted before storage)
 * - Test connection functionality
 * - Enable/disable email sending globally
 * - Support for different SMTP providers (Gmail, Outlook, custom)
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Entity
@Table(name = "email_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    /**
     * SMTP server hostname
     * Examples: smtp.gmail.com, smtp.office365.com, mail.company.com
     */
    @NotBlank(message = "خادم SMTP مطلوب")
    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    /**
     * SMTP server port
     * Common ports: 587 (TLS), 465 (SSL), 25 (unencrypted)
     */
    @NotNull(message = "منفذ SMTP مطلوب")
    @Positive(message = "منفذ SMTP يجب أن يكون رقماً موجباً")
    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    /**
     * SMTP username (usually email address)
     */
    @Column(name = "smtp_username")
    private String smtpUsername;

    /**
     * SMTP password (encrypted before storage)
     */
    @Column(name = "smtp_password", length = 500)
    private String smtpPassword;

    /**
     * Enable SMTP authentication: Y = enabled, N = disabled
     */
    @Pattern(regexp = "^[YN]$", message = "مصادقة SMTP يجب أن تكون Y أو N")
    @Column(name = "smtp_auth", length = 1)
    @Builder.Default
    private String smtpAuth = "Y";

    /**
     * Enable STARTTLS: Y = enabled, N = disabled
     */
    @Pattern(regexp = "^[YN]$", message = "STARTTLS يجب أن يكون Y أو N")
    @Column(name = "smtp_starttls_enable", length = 1)
    @Builder.Default
    private String smtpStarttlsEnable = "Y";

    /**
     * From email address (sender address)
     */
    @NotBlank(message = "البريد الإلكتروني للمرسل مطلوب")
    @Email(message = "البريد الإلكتروني للمرسل يجب أن يكون صالحاً")
    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    /**
     * From name (sender display name)
     */
    @Column(name = "from_name")
    @Builder.Default
    private String fromName = "نظام تكنو للإدارة";

    /**
     * Configuration active status: Y = active, N = inactive
     * Only one configuration can be active at a time
     */
    @Pattern(regexp = "^[YN]$", message = "حالة النشاط يجب أن تكون Y أو N")
    @Column(name = "is_active", length = 1)
    @Builder.Default
    private String isActive = "Y";

    /**
     * Email sending global switch: Y = enabled, N = disabled
     * When disabled, notifications are created but emails are not sent
     */
    @Pattern(regexp = "^[YN]$", message = "تفعيل إرسال البريد يجب أن يكون Y أو N")
    @Column(name = "send_emails_enabled", length = 1)
    @Builder.Default
    private String sendEmailsEnabled = "Y";

    /**
     * Timestamp when configuration was created
     */
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    /**
     * Timestamp when configuration was last modified
     */
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    // Helper methods

    /**
     * Check if configuration is active
     */
    public boolean isConfigActive() {
        return "Y".equals(this.isActive);
    }

    /**
     * Check if email sending is enabled
     */
    public boolean isEmailSendingEnabled() {
        return "Y".equals(this.sendEmailsEnabled);
    }

    /**
     * Check if SMTP authentication is enabled
     */
    public boolean isSmtpAuthEnabled() {
        return "Y".equals(this.smtpAuth);
    }

    /**
     * Check if STARTTLS is enabled
     */
    public boolean isStartTlsEnabled() {
        return "Y".equals(this.smtpStarttlsEnable);
    }

    /**
     * Enable email sending
     */
    public void enableEmailSending() {
        this.sendEmailsEnabled = "Y";
    }

    /**
     * Disable email sending
     */
    public void disableEmailSending() {
        this.sendEmailsEnabled = "N";
    }

    /**
     * Activate configuration
     */
    public void activate() {
        this.isActive = "Y";
    }

    /**
     * Deactivate configuration
     */
    public void deactivate() {
        this.isActive = "N";
    }

    /**
     * Check if credentials are provided
     */
    public boolean hasCredentials() {
        return smtpUsername != null && !smtpUsername.isEmpty()
            && smtpPassword != null && !smtpPassword.isEmpty();
    }
}
