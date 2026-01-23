package com.techno.backend.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigUpdateRequest {

    @NotBlank(message = "قيمة الإعداد مطلوبة")
    @Size(max = 500, message = "قيمة الإعداد لا يجب أن تتجاوز 500 حرف")
    private String configValue;
}
