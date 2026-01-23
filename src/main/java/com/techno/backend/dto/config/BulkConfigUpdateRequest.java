package com.techno.backend.dto.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkConfigUpdateRequest {

    @NotEmpty(message = "تحديثات الإعدادات لا يمكن أن تكون فارغة")
    private Map<String, String> configs; // key -> value mapping
}
