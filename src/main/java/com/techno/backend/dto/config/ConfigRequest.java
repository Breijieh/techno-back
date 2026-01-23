package com.techno.backend.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigRequest {

    @NotBlank(message = "مفتاح الإعداد مطلوب")
    @Size(max = 50, message = "مفتاح الإعداد لا يجب أن يتجاوز 50 حرفاً")
    private String configKey;

    @NotBlank(message = "قيمة الإعداد مطلوبة")
    @Size(max = 500, message = "قيمة الإعداد لا يجب أن تتجاوز 500 حرف")
    private String configValue;

    @NotBlank(message = "نوع الإعداد مطلوب")
    @Pattern(regexp = "STRING|NUMBER|BOOLEAN|JSON", message = "نوع الإعداد يجب أن يكون STRING أو NUMBER أو BOOLEAN أو JSON")
    private String configType;

    @Size(max = 50)
    private String configCategory;

    @Size(max = 1000)
    private String configDescription;

    @Pattern(regexp = "[YN]")
    private String isActive;

    @Pattern(regexp = "[YN]")
    private String isEditable;

    @Size(max = 500)
    private String defaultValue;
}
