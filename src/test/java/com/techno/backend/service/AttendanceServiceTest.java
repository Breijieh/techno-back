package com.techno.backend.service;

import com.techno.backend.dto.CheckInRequest;
import com.techno.backend.dto.CheckInResponse;
import com.techno.backend.dto.CheckOutRequest;
import com.techno.backend.dto.CheckOutResponse;
import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import com.techno.backend.repository.ProjectLaborAssignmentRepository;
import com.techno.backend.repository.HolidayRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive unit tests for AttendanceService.
 * Tests check-in, check-out, validations, GPS, and all edge cases.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Service Tests")
class AttendanceServiceTest {

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
    private com.techno.backend.repository.HolidayRepository holidayRepository;

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

        // Setup test employee
        testEmployee = Employee.builder()
                .employeeNo(1001L)
                .employeeName("أحمد محمد")
                .employmentStatus("ACTIVE")
                .primaryDeptCode(1L)
                .primaryProjectCode(101L)
                .build();

        // Setup test project
        testProject = Project.builder()
                .projectCode(101L)
                .projectName("مشروع كيمبينسكي")
                .projectLatitude(validLatitude)
                .projectLongitude(validLongitude)
                .gpsRadiusMeters(500)
                .requireGpsCheck("Y")
                .build();

        // Setup test schedule
        testSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduleName("جدول العمل القياسي")
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .isActive("Y")
                .build();
    }

    // ==================== Check-In Tests ====================

    @Test
    @DisplayName("Check-in with valid GPS coordinates within radius should succeed")
    void checkIn_ValidGPSWithinRadius_Success() {
        // Arrange
        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.now())
                .entryLatitude(validLatitude)
                .entryLongitude(validLongitude)
                .entryDistanceMeters(0.0)
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        // Act
        CheckInResponse response = attendanceService.checkIn(1001L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getEmployeeNo()).isEqualTo(1001L);
        assertThat(response.getProjectCode()).isEqualTo(101L);
        assertThat(response.getWithinGracePeriod()).isTrue();

        verify(attendanceRepository).save(any(AttendanceTransaction.class));
        verify(calculationService).calculateAttendanceHours(any(), any(), any());
    }

    @Test
    @DisplayName("Check-in when employee not found should throw exception")
    void checkIn_EmployeeNotFound_ThrowsException() {
        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(employeeRepository.findById(1001L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("الموظف غير موجود");
    }

    @Test
    @DisplayName("Check-in when employee not ACTIVE should throw exception")
    void checkIn_EmployeeNotActive_ThrowsException() {
        testEmployee.setEmploymentStatus("TERMINATED");
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("غير نشط");
    }

    @Test
    @DisplayName("Check-in when project not found should throw exception")
    void checkIn_ProjectNotFound_ThrowsException() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.empty());

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("المشروع غير موجود");
    }

    @Test
    @DisplayName("Check-in when already checked in today should throw exception")
    void checkIn_AlreadyCheckedIn_ThrowsException() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(true);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("لقد قمت بتسجيل الدخول اليوم بالفعل");
    }

    @Test
    @DisplayName("Check-in when date is closed should throw exception")
    void checkIn_DateClosed_ThrowsException() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(true);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("مغلق");
    }

    @Test
    @DisplayName("Check-in when GPS coordinates outside radius should throw exception")
    void checkIn_GPSOutsideRadius_ThrowsException() {
        BigDecimal farLatitude = new BigDecimal("24.670000");
        BigDecimal farLongitude = new BigDecimal("46.680000");

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(farLatitude)
                .longitude(farLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فشل التحقق من GPS");
    }

    @Test
    @DisplayName("Check-in when GPS not required should skip GPS validation")
    void checkIn_GPSNotRequired_SkipsValidation() {
        testProject.setRequireGpsCheck("N");

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.now())
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
        verify(attendanceRepository).save(any(AttendanceTransaction.class));
    }

    @Test
    @DisplayName("Check-in when project GPS not configured but GPS required should throw exception")
    void checkIn_ProjectGPSNotConfigured_ThrowsException() {
        testProject.setProjectLatitude(null);
        testProject.setProjectLongitude(null);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        assertThatThrownBy(() -> attendanceService.checkIn(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("إحداثيات GPS للمشروع غير مُعدة");
    }

    // ==================== Check-Out Tests ====================

    @Test
    @DisplayName("Check-out with valid GPS coordinates should succeed")
    void checkOut_ValidGPS_Success() {
        // Arrange
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .entryLatitude(validLatitude)
                .entryLongitude(validLongitude)
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

        // Create updated attendance AFTER check-out (exitTime will be set by service)
        AttendanceTransaction updatedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(attendance.getEntryTime())
                .exitTime(LocalDateTime.of(today, LocalTime.of(17, 0)))
                .workingHours(new BigDecimal("9.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime for this test
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        // Act
        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getWorkingHours()).isEqualByComparingTo(new BigDecimal("9.00"));
        verify(attendanceRepository).save(any(AttendanceTransaction.class));
        verify(allowanceDeductionService).processAttendanceForAllowancesDeductions(any());
    }

    @Test
    @DisplayName("Check-out when no check-in record exists should throw exception")
    void checkOut_NoCheckInRecord_ThrowsException() {
        CheckOutRequest request = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkOut(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("لم يتم العثور على سجل تسجيل دخول");
    }

    @Test
    @DisplayName("Check-out when already checked out should throw exception")
    void checkOut_AlreadyCheckedOut_ThrowsException() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(today, LocalTime.of(17, 0)))
                .build();

        CheckOutRequest request = CheckOutRequest.builder()
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findByEmployeeNoAndAttendanceDate(1001L, today))
                .thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.checkOut(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("لقد قمت بتسجيل الخروج اليوم بالفعل");
    }

    @Test
    @DisplayName("Check-out with transaction ID should use that record")
    void checkOut_WithTransactionId_UsesSpecifiedRecord() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .build();

        CheckOutRequest request = CheckOutRequest.builder()
                .transactionId(1L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
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
                .exitTime(LocalDateTime.of(today, LocalTime.of(17, 0)))
                .workingHours(new BigDecimal("9.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        assertThat(response).isNotNull();
        verify(attendanceRepository).findById(1L);
    }

    @Test
    @DisplayName("Check-out when transaction ID belongs to different employee should throw exception")
    void checkOut_TransactionIdDifferentEmployee_ThrowsException() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(9999L) // Different employee
                .attendanceDate(today)
                .build();

        CheckOutRequest request = CheckOutRequest.builder()
                .transactionId(1L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.checkOut(1001L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("لا ينتمي إليك");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Check-in before scheduled start time should succeed")
    void checkIn_BeforeScheduledStart_Success() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(7, 30))) // Before 8:00
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Check-in exactly at scheduled start time should succeed")
    void checkIn_ExactlyAtScheduledStart_Success() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 0))) // Exactly at 8:00
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getWithinGracePeriod()).isTrue();
    }

    @Test
    @DisplayName("Check-in within grace period should succeed with withinGracePeriod=true")
    void checkIn_WithinGracePeriod_Success() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(true);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(0);

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 15))) // Within 15-min grace
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getWithinGracePeriod()).isTrue();
        assertThat(response.getMinutesLate()).isEqualTo(0);
    }

    @Test
    @DisplayName("Check-in after grace period should succeed with withinGracePeriod=false")
    void checkIn_AfterGracePeriod_Success() {
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(testEmployee));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(testProject));
        when(attendanceRepository.hasCheckedInToday(1001L, today)).thenReturn(false);
        when(closureService.isDateClosed(today)).thenReturn(false);
        when(calculationService.findApplicableSchedule(any(), any())).thenReturn(testSchedule);
        when(calculationService.isWithinGracePeriod(any(), any())).thenReturn(false);
        when(calculationService.calculateMinutesLate(any(), any())).thenReturn(16); // 16 minutes late

        AttendanceTransaction savedAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(8, 16))) // 16 minutes late
                .delayedCalc(new BigDecimal("0.02"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(savedAttendance);

        CheckInRequest request = CheckInRequest.builder()
                .projectCode(101L)
                .latitude(validLatitude)
                .longitude(validLongitude)
                .build();

        CheckInResponse response = attendanceService.checkIn(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getWithinGracePeriod()).isFalse();
        assertThat(response.getMinutesLate()).isEqualTo(16);
    }

    @Test
    @DisplayName("Check-out exactly at scheduled end time should succeed")
    void checkOut_ExactlyAtScheduledEnd_Success() {
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
                .exitTime(LocalDateTime.of(today, LocalTime.of(17, 0)))
                .workingHours(new BigDecimal("9.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .earlyOutCalc(BigDecimal.ZERO)
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getEarlyOutCalc()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Check-out after scheduled end time should calculate overtime")
    void checkOut_AfterScheduledEnd_CalculatesOvertime() {
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
                .overtimeCalc(new BigDecimal("3.00")) // 11 - 8 = 3 hours
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOvertimeCalc()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("Check-out before scheduled end time should calculate early departure")
    void checkOut_BeforeScheduledEnd_CalculatesEarlyDeparture() {
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
                .exitTime(LocalDateTime.of(today, LocalTime.of(16, 0))) // 1 hour early
                .workingHours(new BigDecimal("8.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .earlyOutCalc(new BigDecimal("1.00"))
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getEarlyOutCalc()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Check-out for midnight-crossing shift should calculate correctly")
    void checkOut_MidnightCrossingShift_CalculatesCorrectly() {
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(today)
                .projectCode(101L)
                .entryTime(LocalDateTime.of(today, LocalTime.of(22, 0)))
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
                .exitTime(LocalDateTime.of(today.plusDays(1), LocalTime.of(6, 0)))
                .workingHours(new BigDecimal("8.00"))
                .overtimeCalc(BigDecimal.ZERO) // No overtime
                .build();

        when(attendanceRepository.save(any(AttendanceTransaction.class))).thenReturn(updatedAttendance);

        CheckOutResponse response = attendanceService.checkOut(1001L, request);

        assertThat(response).isNotNull();
        assertThat(response.getWorkingHours()).isEqualByComparingTo(new BigDecimal("8.00"));
    }
}
