package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity representing weekend days configuration.
 * For Saudi Arabia: Friday (5) and Saturday (6) using ISO-8601 standard.
 * ISO-8601: Monday=1, Tuesday=2, Wednesday=3, Thursday=4, Friday=5, Saturday=6,
 * Sunday=7
 *
 * @author Techno ERP Team
 * @version 1.0
 */
@Entity
@Table(name = "weekend_days")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekendDay extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekend_id")
    private Long weekendId;

    @NotNull(message = "يوم الأسبوع مطلوب")
    @Min(value = 1, message = "يوم الأسبوع يجب أن يكون بين 1 (الاثنين) و 7 (الأحد)")
    @Max(value = 7, message = "يوم الأسبوع يجب أن يكون بين 1 (الاثنين) و 7 (الأحد)")
    @Column(name = "day_of_week", nullable = false, unique = true)
    private Integer dayOfWeek; // 1=Monday, 7=Sunday (ISO-8601)

    @NotBlank(message = "اسم اليوم مطلوب")
    @Column(name = "day_name", nullable = false, length = 50)
    private String dayName;

    @NotBlank(message = "علامة الحالة النشطة مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الحالة النشطة يجب أن تكون Y أو N")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    /**
     * Helper method to check if weekend day is active
     *
     * @return true if weekend day is active
     */
    public boolean isActiveWeekend() {
        return "Y".equals(this.isActive);
    }

    /**
     * Helper method to check if this is Friday
     *
     * @return true if day is Friday (5)
     */
    public boolean isFriday() {
        return dayOfWeek != null && dayOfWeek == 5;
    }

    /**
     * Helper method to check if this is Saturday
     *
     * @return true if day is Saturday (6)
     */
    public boolean isSaturday() {
        return dayOfWeek != null && dayOfWeek == 6;
    }
}
