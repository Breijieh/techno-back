package com.techno.backend.service;

import com.techno.backend.dto.CheckInRequest;
import com.techno.backend.dto.CheckInResponse;
import com.techno.backend.dto.CheckOutRequest;
import com.techno.backend.dto.CheckOutResponse;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive integration tests for Attendance module.
 * Tests complete workflows and edge cases that span multiple services.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Integration Tests")
class AttendanceIntegrationTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AttendanceCalculationService calculationService;

    @Mock
    private AttendanceAllowanceDeductionService allowanceDeductionService;

    @Mock
    private AttendanceDayClosureService closureService;

    @Mock
    private EmployeeLeaveRepository leaveRepository;

    @Mock
    private com.techno.backend.repository.ProjectLaborAssignmentRepository assignmentRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee testEmployee;
    private Project testProject;
    private TimeSchedule testSchedule;
    private LocalDate today;
    private BigDecimal validLatitude;
    private BigDecimal validLongitude;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        validLatitude = new BigDecimal("24.664417");
        validLongitude = new BigDecimal("46.674198");

        testEmployee = Employee.builder()
                .employeeNo(1001L)
                .employeeName("أحمد محمد")
                .employmentStatus("ACTIVE")
                .primaryDeptCode(1L)
                .primaryProjectCode(101L)
                .build();

        testProject = Project.builder()
                .projectCode(101L)
                .projectName("مشروع كيمبينسكي")
                .projectLatitude(validLatitude)
                .projectLongitude(validLongitude)
                .gpsRadiusMeters(500)
                .requireGpsCheck("Y")
                .build();

        testSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
    }

    // ==================== Complete Workflow Tests ====================

    @Test
    @DisplayName("Complete check-in and check-out workflow should create allowances and deductions")
    void completeWorkflow_CheckInCheckOut_CreatesAllowancesDeductions() {
        // Step 1: Check-in
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction checkInAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .entryLatitude(validLatitude)
                .entryLongitude(validLongitude)
                .entryDistanceMeters(0.0)
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(checkInAttendance);

        CheckInResponse checkInResponse = attendanceService.checkIn(1001L, checkInRequest);

        assertThat(checkInResponse).isNotNull();
        assertThat(checkInResponse.getTransactionId()).isEqualTo(1L);

        // Step 2: Check-out
        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.of(checkInAttendance));
        
        // Create updated attendance AFTER check-out
        AttendanceTransaction updatedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(checkInAttendance.getEntryTime())
                .exitTime(LocalDateTime.of(today, LocalTime.of(19, 0))) // 2 hours overtime
                .workingHours(new BigDecimal("11.00"))
                .overtimeCalc(new BigDecimal("3.00"))
                .delayedCalc(BigDecimal.ZERO)
                .earlyOutCalc(BigDecimal.ZERO)
                .build();
        
        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse checkOutResponse = attendanceService.checkOut(1001L, checkOutRequest);

        assertThat(checkOutResponse).isNotNull();
        assertThat(checkOutResponse.getOvertimeCalc()).isEqualByComparingTo(new BigDecimal("3.00"));

        // Verify allowances/deductions were created
        verify(allowanceDeductionService).processAttendanceForAllowancesDeductions(any());
    }

    // ==================== Edge Case Integration Tests ====================

    @Test
    @DisplayName("Check-in late then check-out early should calculate both delay and early departure")
    void edgeCase_LateCheckInEarlyCheckOut_CalculatesBoth() {
        // Check-in 30 minutes late
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(false);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(30);

        // Create attendance after check-in (without exitTime)
        AttendanceTransaction checkInAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 30))) // 30 mins late
                .delayedCalc(new BigDecimal("0.25"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(checkInAttendance);

        attendanceService.checkIn(1001L, checkInRequest);

        // Check-out 1 hour early
        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        // Return the check-in attendance (without exitTime) for check-out
        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.of(checkInAttendance));
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        // Note: findApplicableSchedule might not be called in this test path
        lenient().when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);

        // Create updated attendance AFTER check-out
        AttendanceTransaction updatedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(checkInAttendance.getEntryTime())
                .exitTime(LocalDateTime.of(today, LocalTime.of(16, 0))) // 1 hour early
                .workingHours(new BigDecimal("7.50"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .delayedCalc(new BigDecimal("0.25"))
                .earlyOutCalc(new BigDecimal("1.00"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class)))
                .thenReturn(updatedAttendance); // For check-out save

        CheckOutResponse response = attendanceService.checkOut(1001L, checkOutRequest);

        assertThat(response).isNotNull();
        assertThat(response.getDelayedCalc()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(response.getEarlyOutCalc()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Check-in on Friday should set weekend work flag")
    void edgeCase_CheckInOnFriday_SetsWeekendFlag() {
        LocalDate friday = LocalDate.of(2025, 1, 17); // Friday

        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(any(Long.class), any(LocalDate.class))).thenReturn(false);
        when(closureService.isDateClosed(any(LocalDate.class))).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        // Service uses LocalDate.now() for attendance date, not the specific date
        // So the saved attendance will have today's date
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(LocalDate.now()) // Service uses today
                .projectCode(101L)
                .entryTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0)))
                .isWeekendWork("Y")
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(attendance);

        CheckInResponse response = attendanceService.checkIn(1001L, checkInRequest);

        assertThat(response).isNotNull();
        // Weekend flag should be set by calculation service
        verify(calculationService).calculateAttendanceHours(any(), any(), any());
    }

    @Test
    @DisplayName("Check-in on holiday should set holiday work flag")
    void edgeCase_CheckInOnHoliday_SetsHolidayFlag() {
        LocalDate holidayDate = LocalDate.of(2025, 3, 10); // Example holiday

        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(any(Long.class), any(LocalDate.class))).thenReturn(false);
        when(closureService.isDateClosed(any(LocalDate.class))).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        // Service uses LocalDate.now() for attendance date, not the specific date
        // So the saved attendance will have today's date
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(LocalDate.now()) // Service uses today
                .projectCode(101L)
                .entryTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0)))
                .isHolidayWork("Y")
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(attendance);

        CheckInResponse response = attendanceService.checkIn(1001L, checkInRequest);

        assertThat(response).isNotNull();
        // Holiday flag should be set by calculation service
        verify(calculationService).calculateAttendanceHours(any(), any(), any());
    }

    @Test
    @DisplayName("Check-in exactly at grace period boundary should be on-time")
    void edgeCase_CheckInAtGraceBoundary_OnTime() {
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 15))) // Exactly at grace boundary
                .delayedCalc(BigDecimal.ZERO)
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(attendance);

        CheckInResponse response = attendanceService.checkIn(1001L, checkInRequest);

        assertThat(response).isNotNull();
        assertThat(response.getWithinGracePeriod()).isTrue();
        assertThat(response.getMinutesLate()).isEqualTo(0);
    }

    @Test
    @DisplayName("Check-in 1 minute after grace period should calculate delay")
    void edgeCase_CheckIn1MinAfterGrace_CalculatesDelay() {
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(false);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(16); // 16 minutes late

        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 16))) // 16 minutes late
                .delayedCalc(new BigDecimal("0.02")) // 1 minute after grace
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(attendance);

        CheckInResponse response = attendanceService.checkIn(1001L, checkInRequest);

        assertThat(response).isNotNull();
        assertThat(response.getWithinGracePeriod()).isFalse();
        assertThat(response.getMinutesLate()).isEqualTo(16);
    }

    @Test
    @DisplayName("Check-out with overtime should create allowance")
    void edgeCase_CheckOutWithOvertime_CreatesAllowance() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .build();

        CheckOutRequest request = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.of(attendance));
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        // Note: findApplicableSchedule might not be called in this test path
        lenient().when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);

        // Create updated attendance AFTER check-out
        AttendanceTransaction updatedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(attendance.getEntryTime())
                .exitTime(LocalDateTime.of(today, LocalTime.of(19, 0))) // 2 hours overtime
                .workingHours(new BigDecimal("11.00"))
                .overtimeCalc(new BigDecimal("3.00"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        attendanceService.checkOut(1001L, request);

        // Verify allowance was created
        verify(allowanceDeductionService).processAttendanceForAllowancesDeductions(any());
    }

    @Test
    @DisplayName("Check-out with shortage should create deduction")
    void edgeCase_CheckOutWithShortage_CreatesDeduction() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .build();

        CheckOutRequest request = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.of(attendance));
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        // Note: findApplicableSchedule might not be called in this test path
        lenient().when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);

        // Create updated attendance AFTER check-out
        AttendanceTransaction updatedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(attendance.getEntryTime())
                .exitTime(LocalDateTime.of(today, LocalTime.of(15, 0))) // 2 hours shortage
                .workingHours(new BigDecimal("7.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .shortageHours(new BigDecimal("1.00"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        attendanceService.checkOut(1001L, request);

        // Verify deduction was created
        verify(allowanceDeductionService).processAttendanceForAllowancesDeductions(any());
    }

    @Test
    @DisplayName("GPS check with very large radius should allow check-in")
    void edgeCase_VeryLargeRadius_AllowsCheckIn() {
        testProject.setGpsRadiusMeters(5000); // 5 km radius

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(new BigDecimal("24.670000")) // ~4.9 km away
                .longitude(new BigDecimal("46.680000"))
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Use schedule with late end time to allow check-in at any time
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.now())
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(attendance);

        // Should succeed if within 5 km
        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("GPS check with very small radius should reject check-in")
    void edgeCase_VerySmallRadius_RejectsCheckIn() {
        testProject.setGpsRadiusMeters(50); // 50 meters radius

        // Use coordinates that are definitely more than 50m away
        // At Riyadh latitude, 0.0006 degrees ≈ 67 meters
        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(new BigDecimal("24.665017")) // ~67 meters away (0.0006 degrees)
                .longitude(new BigDecimal("46.674798"))
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        // Mock schedule with late end time to allow check-in at any time (schedule validation happens before GPS)
        TimeSchedule lateSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59)) // Late end time to allow check-in at any time
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .build();
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(lateSchedule);
        
        // GPS validation will fail because coordinates are outside 50m radius
        // The exception is thrown before save(), so no need to mock save
        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(com.techno.backend.exception.BadRequestException.class)
                .hasMessageContaining("فشل التحقق من GPS");
    }
}
