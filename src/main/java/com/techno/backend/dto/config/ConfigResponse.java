package com.techno.backend.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigResponse {
    private Long configId;
    private String configKey;
    private String configValue;
    private String configType;
    private String configCategory;
    private String configDescription;
    private String isActive;
    private String isEditable;
    private String defaultValue;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}
