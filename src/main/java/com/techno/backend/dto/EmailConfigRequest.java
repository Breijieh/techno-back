package com.techno.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating email configuration.
 *
 * Used when admin updates SMTP settings.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfigRequest {

    @NotBlank(message = "خادم SMTP مطلوب")
    private String smtpHost;

    @NotNull(message = "منفذ SMTP مطلوب")
    @Positive(message = "يجب أن يكون منفذ SMTP موجباً")
    private Integer smtpPort;

    private String smtpUsername;
    private String smtpPassword;

    @Builder.Default
    private String smtpAuth = "Y";

    @Builder.Default
    private String smtpStarttlsEnable = "Y";

    @NotBlank(message = "البريد الإلكتروني المرسل مطلوب")
    @Email(message = "يجب أن يكون البريد الإلكتروني المرسل صالحاً")
    private String fromEmail;

    @Builder.Default
    private String fromName = "نظام تكنو للإدارة";

    @Builder.Default
    private String sendEmailsEnabled = "Y";
}
