package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * System Log List Response DTO
 * Paginated response for system logs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogListResponse {

    private List<SystemLogResponse> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number; // current page number (0-indexed)
    private Boolean first;
    private Boolean last;

    public static SystemLogListResponse fromPage(Page<com.techno.backend.entity.SystemLog> logPage, List<SystemLogResponse> logResponses) {
        return SystemLogListResponse.builder()
                .content(logResponses)
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .size(logPage.getSize())
                .number(logPage.getNumber())
                .first(logPage.isFirst())
                .last(logPage.isLast())
                .build();
    }
}

