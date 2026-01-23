package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for EmailConfig entity.
 *
 * Used in API responses to return email configuration.
 * Password is excluded for security.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfigResponse {

    private Long configId;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    // Password is intentionally excluded for security
    private String smtpAuth;
    private String smtpStarttlsEnable;
    private String fromEmail;
    private String fromName;
    private String isActive;
    private String sendEmailsEnabled;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private boolean hasPassword; // Indicates if password is set without revealing it
}
