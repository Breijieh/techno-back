package com.techno.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System Log Response DTO
 * Contains system log information for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemLogResponse {

    private Long logId;
    private Long userId; // Nullable for system events
    private String actionType;
    private String module;
    private String description;
    private String level; // INFO, WARNING, ERROR, DEBUG
    private LocalDateTime timestamp; // createdDate from entity
    private String ipAddress; // Optional
}

