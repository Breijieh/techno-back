package com.techno.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for AttendanceCalculator utility class.
 * Tests all time calculation methods including working hours, overtime, delays, etc.
 *
 * @author Techno HR System
 * @version 1.0
 */
@DisplayName("Attendance Calculator Tests")
class AttendanceCalculatorTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 1, 18);

    // ==================== Working Hours Calculation Tests ====================

    @Test
    @DisplayName("Calculate working hours for standard 8-hour day")
    void calculateWorkingHours_StandardDay_Returns8Hours() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 0));
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(17, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(entry, exit);

        assertThat(hours).isEqualByComparingTo(new BigDecimal("9.00"));
    }

    @Test
    @DisplayName("Calculate working hours for midnight-crossing shift")
    void calculateWorkingHours_MidnightCrossing_ReturnsCorrectHours() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(22, 0));
        LocalDateTime exit = LocalDateTime.of(TEST_DATE.plusDays(1), LocalTime.of(6, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(entry, exit);

        assertThat(hours).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    @DisplayName("Calculate working hours with null entry should return null")
    void calculateWorkingHours_NullEntry_ReturnsNull() {
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(17, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(null, exit);

        assertThat(hours).isNull();
    }

    @Test
    @DisplayName("Calculate working hours with null exit should return null")
    void calculateWorkingHours_NullExit_ReturnsNull() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(entry, null);

        assertThat(hours).isNull();
    }

    @Test
    @DisplayName("Calculate working hours when exit before entry should return 0")
    void calculateWorkingHours_ExitBeforeEntry_ReturnsZero() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(17, 0));
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(entry, exit);

        assertThat(hours).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate working hours with same entry and exit should return 0")
    void calculateWorkingHours_SameEntryExit_ReturnsZero() {
        LocalDateTime time = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 0));

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(time, time);

        assertThat(hours).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @CsvSource({
            "08:00, 17:00, 9.00",    // Standard 9-hour day
            "08:00, 16:00, 8.00",    // 8-hour day
            "09:00, 18:00, 9.00",    // 9-hour day starting at 9
            "07:00, 15:00, 8.00",    // 8-hour day starting at 7
            "08:30, 17:30, 9.00",    // 9 hours with 30-min offset
            "08:15, 17:15, 9.00"     // 9 hours with 15-min offset
    })
    @DisplayName("Calculate working hours for various time ranges")
    void calculateWorkingHours_VariousRanges_ReturnsCorrectHours(
            String entryStr, String exitStr, String expectedHours) {
        LocalTime entryTime = LocalTime.parse(entryStr);
        LocalTime exitTime = LocalTime.parse(exitStr);
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, entryTime);
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, exitTime);

        BigDecimal hours = AttendanceCalculator.calculateWorkingHours(entry, exit);

        assertThat(hours).isEqualByComparingTo(new BigDecimal(expectedHours));
    }

    // ==================== Overtime Calculation Tests ====================

    @Test
    @DisplayName("Calculate overtime when working hours exceed scheduled hours")
    void calculateOvertime_ExceedsScheduled_ReturnsOvertime() {
        BigDecimal workingHours = new BigDecimal("10.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, scheduledHours, false, false
        );

        assertThat(overtime).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("Calculate overtime when working hours equal scheduled hours should return 0")
    void calculateOvertime_EqualsScheduled_ReturnsZero() {
        BigDecimal workingHours = new BigDecimal("8.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, scheduledHours, false, false
        );

        assertThat(overtime).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate overtime when working hours less than scheduled should return 0")
    void calculateOvertime_LessThanScheduled_ReturnsZero() {
        BigDecimal workingHours = new BigDecimal("7.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, scheduledHours, false, false
        );

        assertThat(overtime).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate overtime for holiday work should multiply all hours by 1.5")
    void calculateOvertime_HolidayWork_ReturnsAllHoursTimes1_5() {
        BigDecimal workingHours = new BigDecimal("4.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, scheduledHours, true, false
        );

        // 4 hours × 1.5 = 6 hours
        assertThat(overtime).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    @DisplayName("Calculate overtime for weekend work should multiply all hours by 1.5")
    void calculateOvertime_WeekendWork_ReturnsAllHoursTimes1_5() {
        BigDecimal workingHours = new BigDecimal("8.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, scheduledHours, false, true
        );

        // 8 hours × 1.5 = 12 hours
        assertThat(overtime).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Calculate overtime with null working hours should return 0")
    void calculateOvertime_NullWorkingHours_ReturnsZero() {
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                null, scheduledHours, false, false
        );

        assertThat(overtime).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate overtime with null scheduled hours should return 0")
    void calculateOvertime_NullScheduledHours_ReturnsZero() {
        BigDecimal workingHours = new BigDecimal("10.00");

        BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                workingHours, null, false, false
        );

        assertThat(overtime).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Delay Calculation Tests ====================

    @Test
    @DisplayName("Calculate delay when entry exactly at scheduled start should return 0")
    void calculateDelayedHours_ExactlyOnTime_ReturnsZero() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 0));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 15;

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        assertThat(delay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate delay when entry within grace period should return 0")
    void calculateDelayedHours_WithinGracePeriod_ReturnsZero() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 15));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 15;

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        assertThat(delay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate delay when entry 1 minute after grace period")
    void calculateDelayedHours_OneMinuteAfterGrace_ReturnsCorrectDelay() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 16));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 15;

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        // 1 minute = 0.02 hours (rounded to 2 decimals)
        assertThat(delay).isEqualByComparingTo(new BigDecimal("0.02"));
    }

    @Test
    @DisplayName("Calculate delay when entry 30 minutes late should return 0.25 hours")
    void calculateDelayedHours_30MinutesLate_Returns0_25Hours() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 30));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 15;

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        // 15 minutes after grace period = 0.25 hours
        assertThat(delay).isEqualByComparingTo(new BigDecimal("0.25"));
    }

    @Test
    @DisplayName("Calculate delay when entry 1 hour late should return 0.75 hours")
    void calculateDelayedHours_OneHourLate_Returns0_75Hours() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(9, 0));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 15;

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        // 45 minutes after grace period = 0.75 hours
        assertThat(delay).isEqualByComparingTo(new BigDecimal("0.75"));
    }

    @Test
    @DisplayName("Calculate delay with null grace period should use default 15 minutes")
    void calculateDelayedHours_NullGracePeriod_UsesDefault15() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 15));
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, null, TEST_DATE
        );

        assertThat(delay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate delay with custom grace period")
    void calculateDelayedHours_CustomGracePeriod_WorksCorrectly() {
        LocalDateTime entry = LocalDateTime.of(TEST_DATE, LocalTime.of(8, 20));
        LocalTime scheduledStart = LocalTime.of(8, 0);
        Integer gracePeriod = 20; // 20 minutes grace period

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                entry, scheduledStart, gracePeriod, TEST_DATE
        );

        assertThat(delay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate delay with null entry should return 0")
    void calculateDelayedHours_NullEntry_ReturnsZero() {
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal delay = AttendanceCalculator.calculateDelayedHours(
                null, scheduledStart, 15, TEST_DATE
        );

        assertThat(delay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Early Departure Calculation Tests ====================

    @Test
    @DisplayName("Calculate early departure when exit exactly at scheduled end should return 0")
    void calculateEarlyDeparture_ExactlyOnTime_ReturnsZero() {
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(17, 0));
        LocalTime scheduledEnd = LocalTime.of(17, 0);
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate early departure when exit after scheduled end should return 0")
    void calculateEarlyDeparture_AfterScheduledEnd_ReturnsZero() {
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(18, 0));
        LocalTime scheduledEnd = LocalTime.of(17, 0);
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate early departure when exit 1 hour before scheduled end")
    void calculateEarlyDeparture_OneHourEarly_Returns1Hour() {
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(16, 0));
        LocalTime scheduledEnd = LocalTime.of(17, 0);
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Calculate early departure when exit 1.5 hours before scheduled end")
    void calculateEarlyDeparture_OneAndHalfHoursEarly_Returns1_5Hours() {
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(15, 30));
        LocalTime scheduledEnd = LocalTime.of(17, 0);
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(new BigDecimal("1.50"));
    }

    @Test
    @DisplayName("Calculate early departure for midnight-crossing shift")
    void calculateEarlyDeparture_MidnightCrossing_HandlesCorrectly() {
        // Exit at 05:00, scheduled end at 06:00 (next day)
        LocalDateTime exit = LocalDateTime.of(TEST_DATE.plusDays(1), LocalTime.of(5, 0));
        LocalTime scheduledEnd = LocalTime.of(6, 0);
        LocalTime scheduledStart = LocalTime.of(22, 0); // Night shift starting at 10 PM

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Calculate early departure when exit before scheduled start should return 0")
    void calculateEarlyDeparture_BeforeScheduledStart_ReturnsZero() {
        // Exit at 01:18, scheduled start at 08:00, scheduled end at 17:00
        // Employee never actually started work, so no early departure
        LocalDateTime exit = LocalDateTime.of(TEST_DATE, LocalTime.of(1, 18));
        LocalTime scheduledEnd = LocalTime.of(17, 0);
        LocalTime scheduledStart = LocalTime.of(8, 0);

        BigDecimal earlyOut = AttendanceCalculator.calculateEarlyDeparture(
                exit, scheduledEnd, scheduledStart, TEST_DATE
        );

        assertThat(earlyOut).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Shortage Hours Calculation Tests ====================

    @Test
    @DisplayName("Calculate shortage when working hours less than scheduled")
    void calculateShortageHours_LessThanScheduled_ReturnsShortage() {
        BigDecimal workingHours = new BigDecimal("7.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal shortage = AttendanceCalculator.calculateShortageHours(
                workingHours, scheduledHours
        );

        assertThat(shortage).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Calculate shortage when working hours equal scheduled should return 0")
    void calculateShortageHours_EqualsScheduled_ReturnsZero() {
        BigDecimal workingHours = new BigDecimal("8.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal shortage = AttendanceCalculator.calculateShortageHours(
                workingHours, scheduledHours
        );

        assertThat(shortage).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate shortage when working hours exceed scheduled should return 0")
    void calculateShortageHours_ExceedsScheduled_ReturnsZero() {
        BigDecimal workingHours = new BigDecimal("9.00");
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal shortage = AttendanceCalculator.calculateShortageHours(
                workingHours, scheduledHours
        );

        assertThat(shortage).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate shortage with null working hours should return 0")
    void calculateShortageHours_NullWorkingHours_ReturnsZero() {
        BigDecimal scheduledHours = new BigDecimal("8.00");

        BigDecimal shortage = AttendanceCalculator.calculateShortageHours(
                null, scheduledHours
        );

        assertThat(shortage).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Conversion Tests ====================

    @Test
    @DisplayName("Convert hours to minutes")
    void hoursToMinutes_ConvertsCorrectly() {
        assertThat(AttendanceCalculator.hoursToMinutes(new BigDecimal("1.00"))).isEqualTo(60);
        assertThat(AttendanceCalculator.hoursToMinutes(new BigDecimal("0.50"))).isEqualTo(30);
        assertThat(AttendanceCalculator.hoursToMinutes(new BigDecimal("8.50"))).isEqualTo(510);
        assertThat(AttendanceCalculator.hoursToMinutes(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("Convert minutes to hours")
    void minutesToHours_ConvertsCorrectly() {
        assertThat(AttendanceCalculator.minutesToHours(60)).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(AttendanceCalculator.minutesToHours(30)).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(AttendanceCalculator.minutesToHours(510)).isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(AttendanceCalculator.minutesToHours(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(AttendanceCalculator.minutesToHours(0)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Formatting Tests ====================

    @Test
    @DisplayName("Format hours in short format")
    void formatHours_ShortFormat_ReturnsDecimalHours() {
        String formatted = AttendanceCalculator.formatHours(new BigDecimal("8.50"), false);
        assertThat(formatted).isEqualTo("8.50 hours");
    }

    @Test
    @DisplayName("Format hours in long format")
    void formatHours_LongFormat_ReturnsHoursMinutes() {
        String formatted = AttendanceCalculator.formatHours(new BigDecimal("8.50"), true);
        assertThat(formatted).isEqualTo("8h 30m");
    }

    @Test
    @DisplayName("Format hours with zero minutes in long format")
    void formatHours_LongFormat_ZeroMinutes_ReturnsHoursOnly() {
        String formatted = AttendanceCalculator.formatHours(new BigDecimal("8.00"), true);
        assertThat(formatted).isEqualTo("8h");
    }

    @Test
    @DisplayName("Format null hours should return 0 hours")
    void formatHours_NullHours_ReturnsZeroHours() {
        String formatted = AttendanceCalculator.formatHours(null, false);
        assertThat(formatted).isEqualTo("0 hours");
    }

    // ==================== Midnight Crossing Tests ====================

    @Test
    @DisplayName("Detect midnight-crossing shift")
    void isMidnightCrossingShift_DetectsCorrectly() {
        assertThat(AttendanceCalculator.isMidnightCrossingShift(
                LocalTime.of(22, 0), LocalTime.of(6, 0)
        )).isTrue();

        assertThat(AttendanceCalculator.isMidnightCrossingShift(
                LocalTime.of(8, 0), LocalTime.of(17, 0)
        )).isFalse();
    }

    @Test
    @DisplayName("Calculate scheduled duration for midnight-crossing shift")
    void calculateScheduledDuration_MidnightCrossing_ReturnsCorrectDuration() {
        BigDecimal duration = AttendanceCalculator.calculateScheduledDuration(
                LocalTime.of(22, 0), LocalTime.of(6, 0)
        );

        assertThat(duration).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    @DisplayName("Calculate scheduled duration for same-day shift")
    void calculateScheduledDuration_SameDay_ReturnsCorrectDuration() {
        BigDecimal duration = AttendanceCalculator.calculateScheduledDuration(
                LocalTime.of(8, 0), LocalTime.of(17, 0)
        );

        assertThat(duration).isEqualByComparingTo(new BigDecimal("9.00"));
    }

    // ==================== Rounding Tests ====================

    @Test
    @DisplayName("Round hours to nearest quarter hour")
    void roundToQuarterHour_RoundsCorrectly() {
        assertThat(AttendanceCalculator.roundToQuarterHour(new BigDecimal("8.23")))
                .isEqualByComparingTo(new BigDecimal("8.25"));
        assertThat(AttendanceCalculator.roundToQuarterHour(new BigDecimal("8.12")))
                .isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(AttendanceCalculator.roundToQuarterHour(new BigDecimal("8.38")))
                .isEqualByComparingTo(new BigDecimal("8.50"));
    }
}
