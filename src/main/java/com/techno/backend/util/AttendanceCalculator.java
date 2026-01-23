package com.techno.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Attendance Calculator Utility Class
 *
 * Provides time and hours calculations for the Attendance System, including:
 * - Working hours calculation
 * - Overtime calculation (with 1.5x multiplier for holidays/weekends)
 * - Late arrival (delay) calculation with grace period support
 * - Early departure calculation
 * - Shortage hours calculation
 * - Midnight-crossing shift support
 *
 * All calculations return BigDecimal values rounded to 2 decimal places.
 * Hours are calculated in decimal format (e.g., 8.5 hours = 8 hours 30 minutes).
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Slf4j
public class AttendanceCalculator {

    /**
     * Overtime multiplier for holiday and weekend work
     * Saudi Arabia labor law: Holiday/weekend hours are paid at 1.5x rate
     */
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");

    /**
     * Scale for BigDecimal rounding (2 decimal places for hours)
     */
    private static final int DECIMAL_SCALE = 2;

    /**
     * Rounding mode for all calculations
     */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private AttendanceCalculator() {
        throw new UnsupportedOperationException("هذه فئة مساعدة ولا يمكن إنشاء مثيل منها");
    }

    /**
     * Calculates the total working hours between entry and exit times.
     *
     * Handles both same-day and midnight-crossing shifts.
     *
     * @param entryTime Check-in timestamp
     * @param exitTime Check-out timestamp
     * @return Working hours as BigDecimal (2 decimal places), or null if either time is null
     */
    public static BigDecimal calculateWorkingHours(LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null) {
            log.debug("Cannot calculate working hours: entry or exit time is null");
            return null;
        }

        if (exitTime.isBefore(entryTime)) {
            log.warn("Exit time {} is before entry time {}. Returning 0 hours.", exitTime, entryTime);
            return BigDecimal.ZERO;
        }

        Duration duration = Duration.between(entryTime, exitTime);
        long totalMinutes = duration.toMinutes();
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), DECIMAL_SCALE, ROUNDING_MODE);

        log.debug("Working hours calculated: {} hours (Entry: {}, Exit: {})", hours, entryTime, exitTime);
        return hours;
    }

    /**
     * Calculates overtime hours based on working hours and scheduled hours.
     *
     * Overtime = Working Hours - Scheduled Hours (if positive)
     *
     * @param workingHours Total hours worked
     * @param scheduledHours Required hours per schedule
     * @param isHolidayWork true if work was on a holiday
     * @param isWeekendWork true if work was on a weekend
     * @return Overtime hours (multiplied by 1.5 for holiday/weekend), or BigDecimal.ZERO if no overtime
     */
    public static BigDecimal calculateOvertime(BigDecimal workingHours, BigDecimal scheduledHours,
                                              boolean isHolidayWork, boolean isWeekendWork) {
        if (workingHours == null || scheduledHours == null) {
            log.debug("Cannot calculate overtime: working hours or scheduled hours is null");
            return BigDecimal.ZERO;
        }

        // For holiday/weekend work, ALL hours are overtime at 1.5x rate
        if (isHolidayWork || isWeekendWork) {
            BigDecimal overtime = workingHours.multiply(OVERTIME_MULTIPLIER)
                    .setScale(DECIMAL_SCALE, ROUNDING_MODE);
            log.info("Holiday/Weekend overtime calculated: {} hours (Working: {} x 1.5)", overtime, workingHours);
            return overtime;
        }

        // Regular overtime: only hours exceeding scheduled hours
        BigDecimal overtime = workingHours.subtract(scheduledHours);
        if (overtime.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("No overtime: working hours {} <= scheduled hours {}", workingHours, scheduledHours);
            return BigDecimal.ZERO;
        }

        overtime = overtime.setScale(DECIMAL_SCALE, ROUNDING_MODE);
        log.debug("Regular overtime calculated: {} hours (Working: {} - Scheduled: {})",
                overtime, workingHours, scheduledHours);
        return overtime;
    }

    /**
     * Calculates delayed (late arrival) hours beyond the grace period.
     *
     * If entry time is within grace period, no delay is calculated.
     * Delay = Entry Time - (Scheduled Start Time + Grace Period)
     *
     * @param entryTime Actual check-in time
     * @param scheduledStartTime Expected start time
     * @param gracePeriodMinutes Grace period in minutes (default: 15)
     * @param attendanceDate Date of attendance (for proper date handling)
     * @return Delayed hours as BigDecimal, or BigDecimal.ZERO if arrived on time
     */
    public static BigDecimal calculateDelayedHours(LocalDateTime entryTime, LocalTime scheduledStartTime,
                                                  Integer gracePeriodMinutes, LocalDate attendanceDate) {
        if (entryTime == null || scheduledStartTime == null || attendanceDate == null) {
            log.debug("Cannot calculate delay: missing required parameters");
            return BigDecimal.ZERO;
        }

        int gracePeriod = (gracePeriodMinutes != null) ? gracePeriodMinutes : 15;

        // Scheduled start time with grace period
        LocalDateTime graceEndTime = LocalDateTime.of(attendanceDate, scheduledStartTime)
                .plusMinutes(gracePeriod);

        if (entryTime.isBefore(graceEndTime) || entryTime.isEqual(graceEndTime)) {
            log.debug("No delay: entry time {} is within grace period (ends at {})", entryTime, graceEndTime);
            return BigDecimal.ZERO;
        }

        Duration delayDuration = Duration.between(graceEndTime, entryTime);
        long delayMinutes = delayDuration.toMinutes();
        BigDecimal delayHours = BigDecimal.valueOf(delayMinutes)
                .divide(BigDecimal.valueOf(60), DECIMAL_SCALE, ROUNDING_MODE);

        log.info("Delay calculated: {} hours (Entry: {}, Grace End: {}, Delay: {} minutes)",
                delayHours, entryTime, graceEndTime, delayMinutes);
        return delayHours;
    }

    /**
     * Calculates early departure hours if employee left before scheduled end time.
     *
     * Early Out = Scheduled End Time - Exit Time (if positive)
     * 
     * Note: Early departure is only calculated if the employee actually worked.
     * If exit time is before scheduled start time, it's considered they never started work,
     * so early departure is not calculated (returns 0).
     *
     * @param exitTime Actual check-out time
     * @param scheduledEndTime Expected end time
     * @param scheduledStartTime Expected start time (to validate if work actually started)
     * @param attendanceDate Date of attendance
     * @return Early departure hours as BigDecimal, or BigDecimal.ZERO if left on time or later, or if never started work
     */
    public static BigDecimal calculateEarlyDeparture(LocalDateTime exitTime, LocalTime scheduledEndTime,
                                                     LocalTime scheduledStartTime, LocalDate attendanceDate) {
        if (exitTime == null || scheduledEndTime == null || attendanceDate == null) {
            log.debug("Cannot calculate early departure: missing required parameters");
            return BigDecimal.ZERO;
        }

        // Handle midnight-crossing shifts
        LocalDateTime scheduledEnd = LocalDateTime.of(attendanceDate, scheduledEndTime);
        LocalDateTime scheduledStart = LocalDateTime.of(attendanceDate, scheduledStartTime);

        // If exit time is on the next day and schedule crosses midnight, adjust scheduled end time
        if (exitTime.toLocalDate().isAfter(attendanceDate) && scheduledEndTime.isBefore(LocalTime.of(12, 0))) {
            scheduledEnd = scheduledEnd.plusDays(1);
        }

        // If exit time is before scheduled start time, employee never actually started work
        // Don't calculate early departure in this case
        if (exitTime.isBefore(scheduledStart)) {
            log.debug("Exit time {} is before scheduled start time {}. Employee never started work, no early departure calculated.",
                    exitTime, scheduledStart);
            return BigDecimal.ZERO;
        }

        if (exitTime.isAfter(scheduledEnd) || exitTime.isEqual(scheduledEnd)) {
            log.debug("No early departure: exit time {} is at or after scheduled end {}", exitTime, scheduledEnd);
            return BigDecimal.ZERO;
        }

        Duration earlyDuration = Duration.between(exitTime, scheduledEnd);
        long earlyMinutes = earlyDuration.toMinutes();
        BigDecimal earlyHours = BigDecimal.valueOf(earlyMinutes)
                .divide(BigDecimal.valueOf(60), DECIMAL_SCALE, ROUNDING_MODE);

        log.info("Early departure calculated: {} hours (Exit: {}, Scheduled End: {}, Early: {} minutes)",
                earlyHours, exitTime, scheduledEnd, earlyMinutes);
        return earlyHours;
    }

    /**
     * Calculates shortage hours (difference between scheduled and working hours).
     *
     * Shortage = Scheduled Hours - Working Hours (if positive)
     *
     * This is used for deduction calculations when employee doesn't meet required hours.
     * 
     * Note: If scheduled hours is very small (less than grace period of 15 minutes = 0.25 hours),
     * and the employee checked in (has entry time), the shortage is ignored to avoid penalizing
     * very short test schedules or quick check-ins.
     *
     * @param workingHours Actual hours worked
     * @param scheduledHours Required hours per schedule
     * @return Shortage hours as BigDecimal, or BigDecimal.ZERO if met or exceeded scheduled hours,
     *         or if scheduled hours is less than grace period (0.25 hours = 15 minutes)
     */
    public static BigDecimal calculateShortageHours(BigDecimal workingHours, BigDecimal scheduledHours) {
        if (workingHours == null || scheduledHours == null) {
            log.debug("Cannot calculate shortage: working or scheduled hours is null");
            return BigDecimal.ZERO;
        }

        // Grace period threshold: 15 minutes = 0.25 hours
        // If scheduled hours is less than grace period, don't calculate shortage
        // This prevents penalizing employees for very short test schedules
        BigDecimal gracePeriodHours = new BigDecimal("0.25"); // 15 minutes
        if (scheduledHours.compareTo(gracePeriodHours) < 0) {
            log.debug("Scheduled hours {} is less than grace period ({}). No shortage calculated to avoid penalizing short schedules.",
                    scheduledHours, gracePeriodHours);
            return BigDecimal.ZERO;
        }

        BigDecimal shortage = scheduledHours.subtract(workingHours);
        if (shortage.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("No shortage: working hours {} >= scheduled hours {}", workingHours, scheduledHours);
            return BigDecimal.ZERO;
        }

        shortage = shortage.setScale(DECIMAL_SCALE, ROUNDING_MODE);
        log.debug("Shortage calculated: {} hours (Scheduled: {} - Working: {})",
                shortage, scheduledHours, workingHours);
        return shortage;
    }

    /**
     * Converts hours (BigDecimal) to minutes (Integer).
     *
     * @param hours Hours as BigDecimal
     * @return Total minutes as Integer
     */
    public static Integer hoursToMinutes(BigDecimal hours) {
        if (hours == null) {
            return 0;
        }
        return hours.multiply(BigDecimal.valueOf(60))
                .setScale(0, ROUNDING_MODE)
                .intValue();
    }

    /**
     * Converts minutes (Integer) to hours (BigDecimal).
     *
     * @param minutes Total minutes
     * @return Hours as BigDecimal (2 decimal places)
     */
    public static BigDecimal minutesToHours(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Formats hours to a human-readable string (e.g., "8.5 hours" or "8h 30m").
     *
     * @param hours Hours as BigDecimal
     * @param longFormat true for "8h 30m" format, false for "8.5 hours"
     * @return Formatted string
     */
    public static String formatHours(BigDecimal hours, boolean longFormat) {
        if (hours == null) {
            return "0 hours";
        }

        if (!longFormat) {
            return hours.setScale(DECIMAL_SCALE, ROUNDING_MODE) + " hours";
        }

        int totalMinutes = hoursToMinutes(hours);
        int hrs = totalMinutes / 60;
        int mins = totalMinutes % 60;

        if (mins == 0) {
            return hrs + "h";
        }
        return hrs + "h " + mins + "m";
    }

    /**
     * Checks if a work shift crosses midnight.
     *
     * @param startTime Scheduled start time
     * @param endTime Scheduled end time
     * @return true if end time is before start time (midnight crossing)
     */
    public static boolean isMidnightCrossingShift(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isBefore(startTime);
    }

    /**
     * Calculates the scheduled duration considering midnight crossing.
     *
     * @param startTime Scheduled start time
     * @param endTime Scheduled end time
     * @return Duration as BigDecimal in hours
     */
    public static BigDecimal calculateScheduledDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            log.debug("Cannot calculate scheduled duration: start or end time is null");
            return BigDecimal.ZERO;
        }

        Duration duration;
        if (isMidnightCrossingShift(startTime, endTime)) {
            // Calculate time from start to midnight, plus midnight to end
            duration = Duration.between(startTime, LocalTime.MAX)
                    .plus(Duration.between(LocalTime.MIN, endTime))
                    .plusNanos(1_000_000); // Add 1ms for the transition through midnight
            log.debug("Midnight-crossing shift detected: {} to {}", startTime, endTime);
        } else {
            duration = Duration.between(startTime, endTime);
        }

        long totalMinutes = duration.toMinutes();
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), DECIMAL_SCALE, ROUNDING_MODE);

        log.debug("Scheduled duration calculated: {} hours (Start: {}, End: {})", hours, startTime, endTime);
        return hours;
    }

    /**
     * Rounds hours to the nearest quarter hour (0.25).
     *
     * Useful for time rounding policies (e.g., 8.23 hours → 8.25 hours).
     *
     * @param hours Hours to round
     * @return Rounded hours
     */
    public static BigDecimal roundToQuarterHour(BigDecimal hours) {
        if (hours == null) {
            return BigDecimal.ZERO;
        }

        // Convert to minutes, round to nearest 15, convert back
        int totalMinutes = hoursToMinutes(hours);
        int roundedMinutes = Math.round(totalMinutes / 15.0f) * 15;
        return minutesToHours(roundedMinutes);
    }
}