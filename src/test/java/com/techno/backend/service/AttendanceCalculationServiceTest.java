package com.techno.backend.service;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.Holiday;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.repository.HolidayRepository;
import com.techno.backend.repository.TimeScheduleRepository;
import com.techno.backend.repository.WeekendDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AttendanceCalculationService.
 * Tests all calculation logic including holiday/weekend detection, schedule finding, etc.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Calculation Service Tests")
class AttendanceCalculationServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private WeekendDayRepository weekendDayRepository;

    @Mock
    private TimeScheduleRepository timeScheduleRepository;

    @InjectMocks
    private AttendanceCalculationService calculationService;

    private TimeSchedule testSchedule;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 1, 18); // Saturday

        testSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduleName("Standard Schedule")
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .isActive("Y")
                .build();
    }

    // ==================== Holiday Detection Tests ====================

    @Test
    @DisplayName("Detect holiday date should return true when date is holiday")
    void isHolidayDate_IsHoliday_ReturnsTrue() {
        when(holidayRepository.isHoliday(testDate)).thenReturn(true);
        when(holidayRepository.findByHolidayDate(testDate))
                .thenReturn(Optional.of(Holiday.builder()
                        .holidayName("Eid al-Fitr")
                        .build()));

        boolean result = calculationService.isHolidayDate(testDate);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Detect holiday date should return false when date is not holiday")
    void isHolidayDate_NotHoliday_ReturnsFalse() {
        when(holidayRepository.isHoliday(testDate)).thenReturn(false);

        boolean result = calculationService.isHolidayDate(testDate);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Detect holiday date with null date should return false")
    void isHolidayDate_NullDate_ReturnsFalse() {
        boolean result = calculationService.isHolidayDate(null);

        assertThat(result).isFalse();
    }

    // ==================== Weekend Detection Tests ====================

    @Test
    @DisplayName("Detect weekend date should return true for Friday")
    void isWeekendDate_Friday_ReturnsTrue() {
        LocalDate friday = LocalDate.of(2025, 1, 17); // Friday
        when(weekendDayRepository.isWeekendDay(5)).thenReturn(true); // Friday = 5

        boolean result = calculationService.isWeekendDate(friday);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Detect weekend date should return true for Saturday")
    void isWeekendDate_Saturday_ReturnsTrue() {
        LocalDate saturday = LocalDate.of(2025, 1, 18); // Saturday
        when(weekendDayRepository.isWeekendDay(6)).thenReturn(true); // Saturday = 6

        boolean result = calculationService.isWeekendDate(saturday);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Detect weekend date should return false for Sunday")
    void isWeekendDate_Sunday_ReturnsFalse() {
        LocalDate sunday = LocalDate.of(2025, 1, 19); // Sunday
        when(weekendDayRepository.isWeekendDay(7)).thenReturn(false); // Sunday = 7

        boolean result = calculationService.isWeekendDate(sunday);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Detect weekend date with null date should return false")
    void isWeekendDate_NullDate_ReturnsFalse() {
        boolean result = calculationService.isWeekendDate(null);

        assertThat(result).isFalse();
    }

    // ==================== Schedule Finding Tests ====================

    @Test
    @DisplayName("Find schedule should prioritize project schedule over department schedule")
    void findApplicableSchedule_ProjectSchedule_Prioritized() {
        TimeSchedule projectSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .projectCode(101L)
                .scheduledStartTime(LocalTime.of(7, 0))
                .scheduledEndTime(LocalTime.of(16, 0))
                .requiredHours(new BigDecimal("9.00"))
                .build();

        TimeSchedule deptSchedule = TimeSchedule.builder()
                .scheduleId(2L)
                .departmentCode(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .build();

        when(timeScheduleRepository.findByProjectCodeAndIsActive(101L, "Y"))
                .thenReturn(Optional.of(projectSchedule));

        TimeSchedule result = calculationService.findApplicableSchedule(1L, 101L);

        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(1L);
        assertThat(result.getRequiredHours()).isEqualByComparingTo(new BigDecimal("9.00"));
        verify(timeScheduleRepository, never()).findByDepartmentCodeAndIsActive(any(), any());
    }

    @Test
    @DisplayName("Find schedule should use department schedule when no project schedule")
    void findApplicableSchedule_DepartmentSchedule_WhenNoProject() {
        TimeSchedule deptSchedule = TimeSchedule.builder()
                .scheduleId(2L)
                .departmentCode(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .build();

        when(timeScheduleRepository.findByProjectCodeAndIsActive(101L, "Y"))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findByDepartmentCodeAndIsActive(1L, "Y"))
                .thenReturn(Optional.of(deptSchedule));

        TimeSchedule result = calculationService.findApplicableSchedule(1L, 101L);

        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Find schedule should use default schedule when no project or department schedule")
    void findApplicableSchedule_DefaultSchedule_WhenNoProjectOrDept() {
        TimeSchedule defaultSchedule = TimeSchedule.builder()
                .scheduleId(3L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .build();

        when(timeScheduleRepository.findByProjectCodeAndIsActive(101L, "Y"))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findByDepartmentCodeAndIsActive(1L, "Y"))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findDefaultSchedule())
                .thenReturn(Optional.of(defaultSchedule));

        TimeSchedule result = calculationService.findApplicableSchedule(1L, 101L);

        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Find schedule should return null when no schedule found")
    void findApplicableSchedule_NoSchedule_ReturnsNull() {
        when(timeScheduleRepository.findByProjectCodeAndIsActive(101L, "Y"))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findByDepartmentCodeAndIsActive(1L, "Y"))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findDefaultSchedule())
                .thenReturn(Optional.empty());

        TimeSchedule result = calculationService.findApplicableSchedule(1L, 101L);

        assertThat(result).isNull();
    }

    // ==================== Calculate Attendance Hours Tests ====================

    @Test
    @DisplayName("Calculate attendance hours for standard day should set all fields correctly")
    void calculateAttendanceHours_StandardDay_SetsAllFields() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                .build();

        when(holidayRepository.isHoliday(testDate)).thenReturn(false);
        when(weekendDayRepository.isWeekendDay(anyInt())).thenReturn(false);
        when(timeScheduleRepository.findByProjectCodeAndIsActive(any(), any()))
                .thenReturn(Optional.of(testSchedule));

        calculationService.calculateAttendanceHours(attendance, 1L, 101L);

        assertThat(attendance.getIsHolidayWork()).isEqualTo("N");
        assertThat(attendance.getIsWeekendWork()).isEqualTo("N");
        assertThat(attendance.getScheduledHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(attendance.getWorkingHours()).isNotNull();
    }

    @Test
    @DisplayName("Calculate attendance hours for holiday should set holiday flag")
    void calculateAttendanceHours_Holiday_SetsHolidayFlag() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                .build();

        when(holidayRepository.isHoliday(testDate)).thenReturn(true);
        when(weekendDayRepository.isWeekendDay(anyInt())).thenReturn(false);
        when(timeScheduleRepository.findByProjectCodeAndIsActive(any(), any()))
                .thenReturn(Optional.of(testSchedule));

        calculationService.calculateAttendanceHours(attendance, 1L, 101L);

        assertThat(attendance.getIsHolidayWork()).isEqualTo("Y");
        assertThat(attendance.getIsWeekendWork()).isEqualTo("N");
    }

    @Test
    @DisplayName("Calculate attendance hours for weekend should set weekend flag")
    void calculateAttendanceHours_Weekend_SetsWeekendFlag() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                .build();

        when(holidayRepository.isHoliday(testDate)).thenReturn(false);
        when(weekendDayRepository.isWeekendDay(anyInt())).thenReturn(true);
        when(timeScheduleRepository.findByProjectCodeAndIsActive(any(), any()))
                .thenReturn(Optional.of(testSchedule));

        calculationService.calculateAttendanceHours(attendance, 1L, 101L);

        assertThat(attendance.getIsHolidayWork()).isEqualTo("N");
        assertThat(attendance.getIsWeekendWork()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Calculate attendance hours with only entry time should calculate delay only")
    void calculateAttendanceHours_OnlyEntryTime_CalculatesDelayOnly() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 30))) // 30 mins late
                .exitTime(null)
                .build();

        when(holidayRepository.isHoliday(testDate)).thenReturn(false);
        when(weekendDayRepository.isWeekendDay(anyInt())).thenReturn(false);
        when(timeScheduleRepository.findByProjectCodeAndIsActive(any(), any()))
                .thenReturn(Optional.of(testSchedule));

        calculationService.calculateAttendanceHours(attendance, 1L, 101L);

        assertThat(attendance.getWorkingHours()).isNull();
        assertThat(attendance.getDelayedCalc()).isNotNull();
        assertThat(attendance.getEarlyOutCalc()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate attendance hours with null attendance should not throw exception")
    void calculateAttendanceHours_NullAttendance_NoException() {
        calculationService.calculateAttendanceHours(null, 1L, 101L);

        // Should not throw exception, just return
        verify(holidayRepository, never()).isHoliday(any());
    }

    @Test
    @DisplayName("Calculate attendance hours should use default schedule when none found")
    void calculateAttendanceHours_NoSchedule_UsesDefault() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                .build();

        when(holidayRepository.isHoliday(testDate)).thenReturn(false);
        when(weekendDayRepository.isWeekendDay(anyInt())).thenReturn(false);
        when(timeScheduleRepository.findByProjectCodeAndIsActive(any(), any()))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findByDepartmentCodeAndIsActive(any(), any()))
                .thenReturn(Optional.empty());
        when(timeScheduleRepository.findDefaultSchedule())
                .thenReturn(Optional.empty());

        calculationService.calculateAttendanceHours(attendance, 1L, 101L);

        // Should use default schedule (8:00-17:00, 8 hours)
        assertThat(attendance.getScheduledHours()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    // ==================== Grace Period Tests ====================

    @Test
    @DisplayName("Check within grace period when entry exactly at scheduled start")
    void isWithinGracePeriod_ExactlyOnTime_ReturnsTrue() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .attendanceDate(testDate)
                .build();

        boolean result = calculationService.isWithinGracePeriod(attendance, testSchedule);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Check within grace period when entry within 15 minutes")
    void isWithinGracePeriod_Within15Minutes_ReturnsTrue() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 15)))
                .attendanceDate(testDate)
                .build();

        boolean result = calculationService.isWithinGracePeriod(attendance, testSchedule);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Check within grace period when entry after grace period")
    void isWithinGracePeriod_AfterGracePeriod_ReturnsFalse() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 16)))
                .attendanceDate(testDate)
                .build();

        boolean result = calculationService.isWithinGracePeriod(attendance, testSchedule);

        assertThat(result).isFalse();
    }

    // ==================== Minutes Late Calculation Tests ====================

    @Test
    @DisplayName("Calculate minutes late when on time should return 0")
    void calculateMinutesLate_OnTime_ReturnsZero() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .attendanceDate(testDate)
                .build();

        Integer minutesLate = calculationService.calculateMinutesLate(attendance, testSchedule);

        assertThat(minutesLate).isEqualTo(0);
    }

    @Test
    @DisplayName("Calculate minutes late when 30 minutes late should return 30")
    void calculateMinutesLate_30MinutesLate_Returns30() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 30)))
                .attendanceDate(testDate)
                .build();

        Integer minutesLate = calculationService.calculateMinutesLate(attendance, testSchedule);

        assertThat(minutesLate).isEqualTo(30);
    }

    @Test
    @DisplayName("Calculate minutes late with null attendance should return 0")
    void calculateMinutesLate_NullAttendance_ReturnsZero() {
        Integer minutesLate = calculationService.calculateMinutesLate(null, testSchedule);

        assertThat(minutesLate).isEqualTo(0);
    }
}
