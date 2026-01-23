package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SYSTEM_CONFIG")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_config_seq")
    @SequenceGenerator(name = "system_config_seq", sequenceName = "system_config_seq", allocationSize = 1)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "config_key", unique = true, nullable = false, length = 50)
    @NotBlank(message = "مفتاح الإعداد مطلوب")
    @Size(max = 50, message = "مفتاح الإعداد لا يجب أن يتجاوز 50 حرفاً")
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    @NotBlank(message = "قيمة الإعداد مطلوبة")
    @Size(max = 500, message = "قيمة الإعداد لا يجب أن تتجاوز 500 حرف")
    private String configValue;

    @Column(name = "config_type", nullable = false, length = 20)
    @NotBlank(message = "نوع الإعداد مطلوب")
    @Pattern(regexp = "STRING|NUMBER|BOOLEAN|JSON", message = "نوع الإعداد يجب أن يكون STRING أو NUMBER أو BOOLEAN أو JSON")
    private String configType;

    @Column(name = "config_category", length = 50)
    @Size(max = 50, message = "فئة الإعداد لا يجب أن تتجاوز 50 حرفاً")
    private String configCategory;

    @Column(name = "config_description", length = 1000)
    @Size(max = 1000, message = "وصف الإعداد لا يجب أن يتجاوز 1000 حرف")
    private String configDescription;

    @Column(name = "is_active", nullable = false, length = 1)
    @Pattern(regexp = "[YN]", message = "الحالة النشطة يجب أن تكون Y أو N")
    @Builder.Default
    private String isActive = "Y";

    @Column(name = "is_editable", nullable = false, length = 1)
    @Pattern(regexp = "[YN]", message = "إمكانية التعديل يجب أن تكون Y أو N")
    @Builder.Default
    private String isEditable = "Y";

    @Column(name = "default_value", length = 500)
    @Size(max = 500, message = "القيمة الافتراضية لا يجب أن تتجاوز 500 حرف")
    private String defaultValue;

    @Column(name = "is_deleted", nullable = false, length = 1)
    @Pattern(regexp = "[YN]", message = "علامة الحذف يجب أن تكون Y أو N")
    @Builder.Default
    private String isDeleted = "N";

    // Helper methods for type-safe value retrieval
    public Integer getIntValue() {
        if (!"NUMBER".equals(configType)) {
            throw new IllegalStateException("نوع الإعداد ليس NUMBER");
        }
        return Integer.valueOf(configValue);
    }

    public Double getDoubleValue() {
        if (!"NUMBER".equals(configType)) {
            throw new IllegalStateException("نوع الإعداد ليس NUMBER");
        }
        return Double.valueOf(configValue);
    }

    public Boolean getBooleanValue() {
        if (!"BOOLEAN".equals(configType)) {
            throw new IllegalStateException("نوع الإعداد ليس BOOLEAN");
        }
        return "Y".equals(configValue) || "TRUE".equalsIgnoreCase(configValue);
    }
}
