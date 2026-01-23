package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entity representing Saudi Arabia public holidays (Eids and National Days).
 * Supports both Hijri-based holidays (dates vary yearly) and fixed Gregorian
 * holidays.
 *
 * @author Techno ERP Team
 * @version 1.0
 */
@Entity
@Table(name = "eids_holidays", uniqueConstraints = {
        @UniqueConstraint(name = "uk_holiday_date", columnNames = { "holiday_date" })
}, indexes = {
        @Index(name = "idx_holiday_date", columnList = "holiday_date"),
        @Index(name = "idx_holiday_year", columnList = "holiday_year")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @NotNull(message = "تاريخ العطلة مطلوب")
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @NotBlank(message = "اسم العطلة مطلوب")
    @Column(name = "holiday_name", nullable = false, length = 200)
    private String holidayName;

    @NotNull(message = "سنة العطلة مطلوبة")
    @Column(name = "holiday_year", nullable = false)
    private Integer holidayYear;

    @NotBlank(message = "علامة التكرار مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة التكرار يجب أن تكون Y أو N")
    @Column(name = "is_recurring", nullable = false, length = 1)
    @Builder.Default
    private String isRecurring = "N"; // Hijri dates may vary, so default to non-recurring

    @NotBlank(message = "علامة النشاط مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون Y أو N")
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    @NotBlank(message = "علامة الدفع مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة الدفع يجب أن تكون Y أو N")
    @Column(name = "is_paid", nullable = false, length = 1)
    @Builder.Default
    private String isPaid = "Y";

    /**
     * Helper method to check if holiday is active
     *
     * @return true if holiday is active
     */
    public boolean isActiveHoliday() {
        return "Y".equals(this.isActive);
    }

    /**
     * Helper method to check if holiday is recurring
     *
     * @return true if holiday recurs annually
     */
    public boolean isRecurringHoliday() {
        return "Y".equals(this.isRecurring);
    }
}
