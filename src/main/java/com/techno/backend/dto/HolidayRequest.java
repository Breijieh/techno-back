package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating or updating holiday.
 * Used in POST /api/holidays and PUT /api/holidays/{id} endpoints.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayRequest {

    /**
     * Holiday date
     */
    @NotNull(message = "تاريخ العطلة مطلوب")
    private LocalDate holidayDate;

    /**
     * Arabic holiday name (e.g., "عيد الفطر", "اليوم الوطني")
     */
    @NotBlank(message = "اسم العطلة مطلوب")
    @Size(max = 200, message = "اسم العطلة يجب أن لا يتجاوز 200 حرف")
    private String holidayName;

    /**
     * Holiday year
     */
    @NotNull(message = "سنة العطلة مطلوبة")
    private Integer holidayYear;

    /**
     * Is recurring? (Y/N)
     * Y = Recurring holiday (e.g., National Day on 23rd September every year)
     * N = Non-recurring (e.g., Eid dates vary based on Hijri calendar)
     */
    @NotBlank(message = "علامة التكرار مطلوبة")
    @Pattern(regexp = "^[YN]$", message = "علامة التكرار يجب أن تكون Y أو N")
    private String isRecurring;

    /**
     * Active flag (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة النشاط يجب أن تكون Y أو N")
    private String isActive;

    /**
     * Paid flag (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "علامة الدفع يجب أن تكون Y أو N")
    private String isPaid;
}
