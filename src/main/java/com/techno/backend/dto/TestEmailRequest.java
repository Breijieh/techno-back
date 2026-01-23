package com.techno.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending test emails.
 *
 * Used by admin to verify SMTP configuration.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestEmailRequest {

    @NotBlank(message = "بريد المستلم مطلوب")
    @Email(message = "يجب أن يكون بريد المستلم صالحاً")
    private String recipientEmail;

    @Builder.Default
    private String subject = "بريد إلكتروني تجريبي من Techno ERP";

    @Builder.Default
    private String body = "<h2>بريد إلكتروني تجريبي</h2><p>هذا بريد إلكتروني تجريبي من نظام إشعارات Techno ERP.</p><p>إذا تلقيت هذا البريد، فإن إعدادات البريد الإلكتروني تعمل بشكل صحيح!</p>";
}
