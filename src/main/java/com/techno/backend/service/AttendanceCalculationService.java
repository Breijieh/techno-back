package com.techno.backend.service;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.Holiday;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.repository.HolidayRepository;
import com.techno.backend.repository.TimeScheduleRepository;
import com.techno.backend.repository.WeekendDayRepository;
import com.techno.backend.util.AttendanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Service class for Attendance Calculation logic.
 * Handles all automatic calculations for attendance records including:
 * - Working hours calculation
 * - Overtime calculation (with holiday/weekend multiplier)
 * - Late arrival (delay) calculation with grace period
 * - Early departure calculation
 * - Shortage hours calculation
 * - Holiday/weekend detection
 *
 * This service is used by AttendanceService to compute all time-related fields.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceCalculationService {

    private final HolidayRepository holidayRepository;
    private final WeekendDayRepository weekendDayRepository;
    private final TimeScheduleRepository timeScheduleRepository;

    /**
     * Performs all automatic calculations for an attendance record.
     * Updates the attendance transaction with calculated hours and flags.
     *
     * This method should be called:
     * - After check-out (to calculate final hours)
     * - When creating manual attendance entries
     * - When updating attendance records
     *
     * @param attendance The attendance transaction to calculate
     * @param departmentCode Employee's department code (for schedule lookup)
     * @param projectCode Project code (for schedule lookup, overrides department)
     */
    @Transactional
    public void calculateAttendanceHours(AttendanceTransaction attendance,
                                        Long departmentCode,
                                        Long projectCode) {

        if (attendance == null) {
            log.warn("Cannot calculate hours: attendance record is null");
            return;
        }

        log.info("Calculating attendance hours for Employee {} on {}",
                attendance.getEmployeeNo(), attendance.getAttendanceDate());

        // Step 1: Detect holiday and weekend work
        boolean isHoliday = isHolidayDate(attendance.getAttendanceDate());
        boolean isWeekend = isWeekendDate(attendance.getAttendanceDate());

        attendance.setIsHolidayWork(isHoliday ? "Y" : "N");
        attendance.setIsWeekendWork(isWeekend ? "Y" : "N");

        // Step 2: Get applicable time schedule
        TimeSchedule schedule = findApplicableSchedule(departmentCode, projectCode);
        if (schedule == null) {
            log.warn("No time schedule found for department {} or project {}. Using hardcoded default 8-hour schedule.",
                    departmentCode, projectCode);
            schedule = createDefaultSchedule();
        }

        BigDecimal scheduledHours = schedule.getRequiredHours();
        attendance.setScheduledHours(scheduledHours);
        log.info("Set scheduledHours to {} for employee {} (from schedule: {})",
                scheduledHours, attendance.getEmployeeNo(), schedule.getScheduleName());

        // Step 3: Calculate working hours (if both entry and exit times are present)
        if (attendance.hasCheckedIn() && attendance.hasCheckedOut()) {
            BigDecimal workingHours = AttendanceCalculator.calculateWorkingHours(
                    attendance.getEntryTime(),
                    attendance.getExitTime()
            );
            attendance.setWorkingHours(workingHours);

            // Step 4: Calculate overtime
            BigDecimal overtime = AttendanceCalculator.calculateOvertime(
                    workingHours,
                    scheduledHours,
                    isHoliday,
                    isWeekend
            );
            attendance.setOvertimeCalc(overtime);

            // Step 5: Calculate shortage hours
            BigDecimal shortage = AttendanceCalculator.calculateShortageHours(
                    workingHours,
                    scheduledHours
            );
            attendance.setShortageHours(shortage);

        } else {
            log.debug("Cannot calculate working hours: missing entry or exit time");
            attendance.setWorkingHours(null);
            attendance.setOvertimeCalc(BigDecimal.ZERO);
            attendance.setShortageHours(null);
        }

        // Step 6: Calculate delayed hours (late arrival)
        if (attendance.hasCheckedIn()) {
            BigDecimal delayedHours = AttendanceCalculator.calculateDelayedHours(
                    attendance.getEntryTime(),
                    schedule.getScheduledStartTime(),
                    schedule.getGracePeriodMinutes(),
                    attendance.getAttendanceDate()
            );
            attendance.setDelayedCalc(delayedHours);
        } else {
            attendance.setDelayedCalc(BigDecimal.ZERO);
        }

        // Step 7: Calculate early departure hours
        if (attendance.hasCheckedOut()) {
            BigDecimal earlyOutHours = AttendanceCalculator.calculateEarlyDeparture(
                    attendance.getExitTime(),
                    schedule.getScheduledEndTime(),
                    schedule.getScheduledStartTime(),
                    attendance.getAttendanceDate()
            );
            attendance.setEarlyOutCalc(earlyOutHours);
        } else {
            attendance.setEarlyOutCalc(BigDecimal.ZERO);
        }

        log.info("Attendance calculation completed for Employee {}: Working={}, Overtime={}, Delayed={}, EarlyOut={}, Shortage={}",
                attendance.getEmployeeNo(),
                attendance.getWorkingHours(),
                attendance.getOvertimeCalc(),
                attendance.getDelayedCalc(),
                attendance.getEarlyOutCalc(),
                attendance.getShortageHours());
    }

    /**
     * Checks if a given date is a public holiday.
     *
     * @param date Date to check
     * @return true if the date is an active holiday
     */
    @Transactional(readOnly = true)
    public boolean isHolidayDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        boolean isHoliday = holidayRepository.isHoliday(date);
        if (isHoliday) {
            Optional<Holiday> holiday = holidayRepository.findByHolidayDate(date);
            holiday.ifPresent(h -> log.info("Date {} is a holiday: {}", date, h.getHolidayName()));
        }
        return isHoliday;
    }

    /**
     * Checks if a given date is a weekend day.
     * For Saudi Arabia: Friday (5) and Saturday (6) in ISO-8601 format.
     *
     * @param date Date to check
     * @return true if the date is a weekend day
     */
    @Transactional(readOnly = true)
    public boolean isWeekendDate(LocalDate date) {
        if (date == null) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayNumber = dayOfWeek.getValue(); // 1=Monday, 7=Sunday

        boolean isWeekend = weekendDayRepository.isWeekendDay(dayNumber);
        if (isWeekend) {
            log.debug("Date {} ({}) is a weekend day", date, dayOfWeek);
        }
        return isWeekend;
    }

    /**
     * Finds the applicable time schedule for an employee.
     * Priority: Project schedule > Department schedule > Default schedule.
     *
     * @param departmentCode Employee's department code
     * @param projectCode Employee's project code (may be null)
     * @return TimeSchedule or null if not found
     */
    @Transactional(readOnly = true)
    public TimeSchedule findApplicableSchedule(Long departmentCode, Long projectCode) {

        log.debug("Finding schedule for departmentCode: {}, projectCode: {}", departmentCode, projectCode);

        // Priority 1: Project-specific schedule
        if (projectCode != null) {
            Optional<TimeSchedule> projectSchedule = timeScheduleRepository
                    .findByProjectCodeAndIsActive(projectCode, "Y");
            if (projectSchedule.isPresent()) {
                TimeSchedule schedule = projectSchedule.get();
                log.info("Found project-specific schedule: ID={}, Name={}, RequiredHours={}, Start={}, End={}",
                        schedule.getScheduleId(), schedule.getScheduleName(), schedule.getRequiredHours(),
                        schedule.getScheduledStartTime(), schedule.getScheduledEndTime());
                return schedule;
            } else {
                log.warn("No active project schedule found for projectCode: {}. " +
                        "Please check if a schedule with project_code={} and is_active='Y' exists in the time_schedule table.",
                        projectCode, projectCode);
            }
        }

        // Priority 2: Department-specific schedule
        if (departmentCode != null) {
            Optional<TimeSchedule> deptSchedule = timeScheduleRepository
                    .findByDepartmentCodeAndIsActive(departmentCode, "Y");
            if (deptSchedule.isPresent()) {
                TimeSchedule schedule = deptSchedule.get();
                log.info("Found department-specific schedule: ID={}, Name={}, RequiredHours={}, Start={}, End={}",
                        schedule.getScheduleId(), schedule.getScheduleName(), schedule.getRequiredHours(),
                        schedule.getScheduledStartTime(), schedule.getScheduledEndTime());
                return schedule;
            } else {
                log.debug("No active department schedule found for departmentCode: {}", departmentCode);
            }
        }

        // Priority 3: Default/general schedule
        Optional<TimeSchedule> defaultSchedule = timeScheduleRepository.findDefaultSchedule();
        if (defaultSchedule.isPresent()) {
            TimeSchedule schedule = defaultSchedule.get();
            log.info("Using default schedule: ID={}, Name={}, RequiredHours={}, Start={}, End={}",
                    schedule.getScheduleId(), schedule.getScheduleName(), schedule.getRequiredHours(),
                    schedule.getScheduledStartTime(), schedule.getScheduledEndTime());
            return schedule;
        } else {
            log.warn("No default schedule found in database");
        }

        log.warn("No schedule found for department {} or project {}. Will use hardcoded default (8 hours).",
                departmentCode, projectCode);
        return null;
    }

    /**
     * Creates a default time schedule when none exists in the database.
     * Default: 08:00 - 17:00 (8 hours), 15 minutes grace period.
     *
     * @return Default TimeSchedule (not persisted)
     */
    private TimeSchedule createDefaultSchedule() {
        return TimeSchedule.builder()
                .scheduleName("Ø§Ù„Ø¬Ø¯ÙˆÙ„ Ø§Ù„Ø§ÙØªØ±Ø§Ø¶ÙŠ")
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .isActive("Y")
                .build();
    }

    /**
     * Calculates summary statistics for attendance records in a date range.
     * Returns total working hours, overtime, delays, etc.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Summary string (e.g., "120 hours worked, 10 hours overtime")
     */
    @Transactional(readOnly = true)
    public String calculateAttendanceSummary(Long employeeNo, LocalDate startDate, LocalDate endDate) {
        // This method can be expanded to provide rich summary data
        // For now, returning a placeholder
        return String.format("Ù…Ù„Ø®Øµ Ø§Ù„Ø­Ø¶ÙˆØ± Ù„Ù„Ù…ÙˆØ¸Ù %d Ù…Ù† %s Ø¥Ù„Ù‰ %s",
                employeeNo, startDate, endDate);
    }

    /**
     * Checks if an employee arrived within the grace period.
     *
     * @param attendance Attendance record
     * @param schedule Time schedule
     * @return true if arrived within grace period
     */
    public boolean isWithinGracePeriod(AttendanceTransaction attendance, TimeSchedule schedule) {
        if (attendance == null || !attendance.hasCheckedIn() || schedule == null) {
            return false;
        }

        BigDecimal delayedHours = AttendanceCalculator.calculateDelayedHours(
                attendance.getEntryTime(),
                schedule.getScheduledStartTime(),
                schedule.getGracePeriodMinutes(),
                attendance.getAttendanceDate()
        );

        return delayedHours.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calculates how many minutes late an employee was.
     *
     * @param attendance Attendance record
     * @param schedule Time schedule
     * @return Minutes late (0 if on time or early)
     */
    public Integer calculateMinutesLate(AttendanceTransaction attendance, TimeSchedule schedule) {
        if (attendance == null || !attendance.hasCheckedIn() || schedule == null) {
            return 0;
        }

        BigDecimal delayedHours = AttendanceCalculator.calculateDelayedHours(
                attendance.getEntryTime(),
                schedule.getScheduledStartTime(),
                0, // No grace period for exact calculation
                attendance.getAttendanceDate()
        );

        return AttendanceCalculator.hoursToMinutes(delayedHours);
    }
}

