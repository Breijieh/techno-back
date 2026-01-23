package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating email templates.
 *
 * Templates are pre-seeded but can be customized by admin.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateRequest {

    @NotBlank(message = "الموضوع بالإنجليزية مطلوب")
    private String subjectEn;

    @NotBlank(message = "المحتوى بالإنجليزية مطلوب")
    private String bodyEn;

    @NotBlank(message = "الموضوع بالعربية مطلوب")
    private String subjectAr;

    @NotBlank(message = "المحتوى بالعربية مطلوب")
    private String bodyAr;

    @Builder.Default
    private String isActive = "Y";
}
