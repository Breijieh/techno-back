package com.techno.backend.service;

import com.techno.backend.dto.AttendanceResponse;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test for schedule recalculation on historical attendance records.
 * 
 * This test verifies that when schedules are assigned to projects AFTER
 * attendance records are created, the system correctly recalculates
 * scheduledHours and shortageHours when fetching attendance history.
 * 
 * Scenarios covered:
 * 1. Historical record with null projectCode - should find project from labor
 * assignment
 * 2. Historical record with projectCode - should use project-specific schedule
 * 3. Schedule assigned after attendance creation - should recalculate correctly
 * 4. Multiple schedules (project > department > default) priority
 * 
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Schedule Recalculation Tests")
class AttendanceScheduleRecalculationTest {

        @Mock
        private AttendanceRepository attendanceRepository;

        @Mock
        private EmployeeRepository employeeRepository;

        @Mock
        private ProjectRepository projectRepository;

        @Mock
        private TimeScheduleRepository timeScheduleRepository;

        @Mock
        private com.techno.backend.repository.ProjectLaborAssignmentRepository assignmentRepository;

        @InjectMocks
        private AttendanceService attendanceService;

        private Employee testEmployee;
        private Project testProject;
        private TimeSchedule defaultSchedule;
        private TimeSchedule projectSchedule;
        private TimeSchedule departmentSchedule;
        private LocalDate testDate;
        private ProjectLaborAssignment laborAssignment;

        @BeforeEach
        void setUp() {
                testDate = LocalDate.of(2026, 1, 19);

                // Setup test employee
                testEmployee = Employee.builder()
                                .employeeNo(11L)
                                .employeeName("Test Employee")
                                .employmentStatus("ACTIVE")
                                .primaryDeptCode(1L)
                                .primaryProjectCode(null) // No primary project initially
                                .build();

                // Setup test project
                testProject = Project.builder()
                                .projectCode(3L)
                                .projectName("Test Project")
                                .projectStatus("ACTIVE")
                                .build();

                // Setup default schedule (8 hours)
                defaultSchedule = TimeSchedule.builder()
                                .scheduleId(1L)
                                .scheduleName("Default 8-Hour Schedule")
                                .projectCode(null)
                                .departmentCode(null)
                                .scheduledStartTime(LocalTime.of(8, 0))
                                .scheduledEndTime(LocalTime.of(17, 0))
                                .requiredHours(new BigDecimal("8.00"))
                                .isActive("Y")
                                .build();

                // Setup project-specific schedule (0.10 hours = 6 minutes)
                projectSchedule = TimeSchedule.builder()
                                .scheduleId(2L)
                                .scheduleName("Project Short Schedule")
                                .projectCode(3L) // Assigned to project 3
                                .departmentCode(null)
                                .scheduledStartTime(LocalTime.of(1, 18))
                                .scheduledEndTime(LocalTime.of(1, 22))
                                .requiredHours(new BigDecimal("0.10"))
                                .isActive("Y")
                                .build();

                // Setup department schedule (9 hours)
                departmentSchedule = TimeSchedule.builder()
                                .scheduleId(3L)
                                .scheduleName("Department Schedule")
                                .projectCode(null)
                                .departmentCode(1L)
                                .scheduledStartTime(LocalTime.of(7, 0))
                                .scheduledEndTime(LocalTime.of(16, 0))
                                .requiredHours(new BigDecimal("9.00"))
                                .isActive("Y")
                                .build();

                // Setup labor assignment (employee assigned to project 3
                laborAssignment = ProjectLaborAssignment.builder()
                                .assignmentNo(9L)
                                .employeeNo(11L)
                                .projectCode(3L)
                                .startDate(LocalDate.of(2026, 1, 18))
                                .endDate(LocalDate.of(2026, 1, 30))
                                .assignmentStatus("ACTIVE")
                                .isDeleted("N")
                                .build();
        }

        @Test
        @DisplayName("Recalculate scheduledHours for historical record with null projectCode using labor assignment")
        void getEmployeeAttendance_NullProjectCode_UsesLaborAssignmentForSchedule() {
                // Given: Historical attendance record with null projectCode
                AttendanceTransaction historicalAttendance = AttendanceTransaction.builder()
                                .transactionId(12L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(null) // No project assigned at creation time
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(5, 6)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(5, 11)))
                                .workingHours(new BigDecimal("0.08")) // 5 minutes
                                .scheduledHours(new BigDecimal("8.00")) // Old default value
                                .shortageHours(new BigDecimal("7.92")) // Old calculation
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Collections.singletonList(historicalAttendance), pageable, 1);

                // Mock: Employee exists
                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));

                // Mock: Find labor assignment for the date
                when(assignmentRepository.findActiveAssignmentsByEmployee(11L))
                                .thenReturn(Collections.singletonList(laborAssignment));

                // Mock: Find project from labor assignment
                when(projectRepository.findById(3L)).thenReturn(Optional.of(testProject));

                // Mock: Find project-specific schedule
                when(timeScheduleRepository.findByProjectCode(3L))
                                .thenReturn(Collections.singletonList(projectSchedule));

                // Mock: Repository returns attendance page
                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(1), testDate.plusDays(1), pageable);

                // Then: scheduledHours should be recalculated to project schedule (0.10 hours)
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);

                AttendanceResponse response = result.getContent().get(0);
                assertThat(response.getProjectCode()).isEqualTo(3L); // Should find from labor assignment
                assertThat(response.getScheduledHours()).isEqualByComparingTo(new BigDecimal("0.10")); // Project
                                                                                                       // schedule
                assertThat(response.getShortageHours()).isEqualByComparingTo(new BigDecimal("0.02")); // 0.10 - 0.08 =
                                                                                                      // 0.02

                // Verify: Labor assignment was queried
                verify(assignmentRepository, atLeastOnce()).findActiveAssignmentsByEmployee(11L);
                // Verify: Project-specific schedule was queried
                verify(timeScheduleRepository, atLeastOnce()).findByProjectCode(3L);
        }

        @Test
        @DisplayName("Recalculate scheduledHours for historical record with projectCode using project schedule")
        void getEmployeeAttendance_WithProjectCode_UsesProjectSchedule() {
                // Given: Historical attendance record with projectCode
                AttendanceTransaction historicalAttendance = AttendanceTransaction.builder()
                                .transactionId(13L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(3L) // Project was assigned at creation
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(5, 6)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(5, 11)))
                                .workingHours(new BigDecimal("0.08"))
                                .scheduledHours(new BigDecimal("8.00")) // Old default, but project schedule exists now
                                .shortageHours(new BigDecimal("7.92"))
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Collections.singletonList(historicalAttendance), pageable, 1);

                // Mock: Employee exists
                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));

                // Mock: Project exists
                when(projectRepository.findById(3L)).thenReturn(Optional.of(testProject));

                // Mock: Find project-specific schedule
                when(timeScheduleRepository.findByProjectCode(3L))
                                .thenReturn(Collections.singletonList(projectSchedule));

                // Mock: Repository returns attendance page
                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(1), testDate.plusDays(1), pageable);

                // Then: scheduledHours should be recalculated to project schedule
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);

                AttendanceResponse response = result.getContent().get(0);
                assertThat(response.getProjectCode()).isEqualTo(3L);
                assertThat(response.getScheduledHours()).isEqualByComparingTo(new BigDecimal("0.10")); // Project
                                                                                                       // schedule
                assertThat(response.getShortageHours()).isEqualByComparingTo(new BigDecimal("0.02")); // Recalculated

                // Verify: Project-specific schedule was queried
                verify(timeScheduleRepository, atLeastOnce()).findByProjectCode(3L);
        }

        @Test
        @DisplayName("Recalculate scheduledHours falls back to department schedule when no project schedule")
        void getEmployeeAttendance_NoProjectSchedule_FallsBackToDepartmentSchedule() {
                // Given: Attendance with projectCode but no project-specific schedule
                AttendanceTransaction attendance = AttendanceTransaction.builder()
                                .transactionId(14L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(3L)
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                                .workingHours(new BigDecimal("9.00"))
                                .scheduledHours(new BigDecimal("8.00")) // Old default
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Collections.singletonList(attendance), pageable, 1);

                // Mock: Employee with department
                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));
                when(projectRepository.findById(3L)).thenReturn(Optional.of(testProject));

                // Mock: No project schedule, but department schedule exists
                when(timeScheduleRepository.findByProjectCode(3L))
                                .thenReturn(Collections.emptyList());
                when(timeScheduleRepository.findByDepartmentCodeAndIsActive(1L, "Y"))
                                .thenReturn(Optional.of(departmentSchedule));

                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(1), testDate.plusDays(1), pageable);

                // Then: Should use department schedule (9.00 hours)
                assertThat(result.getContent()).hasSize(1);
                AttendanceResponse response = result.getContent().get(0);
                assertThat(response.getScheduledHours()).isEqualByComparingTo(new BigDecimal("9.00"));
        }

        @Test
        @DisplayName("Recalculate scheduledHours falls back to default schedule when no project or department schedule")
        void getEmployeeAttendance_NoProjectOrDeptSchedule_FallsBackToDefault() {
                // Given: Attendance with no project or department schedule
                AttendanceTransaction attendance = AttendanceTransaction.builder()
                                .transactionId(15L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(null)
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                                .workingHours(new BigDecimal("8.00"))
                                .scheduledHours(new BigDecimal("8.00"))
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Collections.singletonList(attendance), pageable, 1);

                // Mock: Employee with no primary project
                testEmployee.setPrimaryProjectCode(null);
                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));

                // Mock: No labor assignments
                when(assignmentRepository.findActiveAssignmentsByEmployee(11L))
                                .thenReturn(Collections.emptyList());

                // Mock: No project or department schedule, but default exists
                when(timeScheduleRepository.findDefaultSchedule())
                                .thenReturn(Optional.of(defaultSchedule));

                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(1), testDate.plusDays(1), pageable);

                // Then: Should use default schedule (8.00 hours)
                assertThat(result.getContent()).hasSize(1);
                AttendanceResponse response = result.getContent().get(0);
                assertThat(response.getScheduledHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        }

        @Test
        @DisplayName("Recalculate shortageHours based on recalculated scheduledHours")
        void getEmployeeAttendance_RecalculatesShortageHours_BasedOnNewSchedule() {
                // Given: Historical record with old scheduledHours (8.00) but new schedule is
                // 0.10
                AttendanceTransaction attendance = AttendanceTransaction.builder()
                                .transactionId(16L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(3L)
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(5, 6)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(5, 11)))
                                .workingHours(new BigDecimal("0.08")) // 5 minutes worked
                                .scheduledHours(new BigDecimal("8.00")) // Old: 8 hours
                                .shortageHours(new BigDecimal("7.92")) // Old: 8.00 - 0.08 = 7.92
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Collections.singletonList(attendance), pageable, 1);

                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));
                when(projectRepository.findById(3L)).thenReturn(Optional.of(testProject));
                when(timeScheduleRepository.findByProjectCode(3L))
                                .thenReturn(Collections.singletonList(projectSchedule));

                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(1), testDate.plusDays(1), pageable);

                // Then: shortageHours should be recalculated based on new schedule
                assertThat(result.getContent()).hasSize(1);
                AttendanceResponse response = result.getContent().get(0);

                // New calculation: 0.10 (scheduled) - 0.08 (worked) = 0.02 (shortage)
                assertThat(response.getScheduledHours()).isEqualByComparingTo(new BigDecimal("0.10"));
                assertThat(response.getShortageHours()).isEqualByComparingTo(new BigDecimal("0.02")); // Recalculated
        }

        @Test
        @DisplayName("Multiple attendance records with different project assignments recalculate correctly")
        void getEmployeeAttendance_MultipleRecords_EachRecalculatesCorrectly() {
                // Given: Multiple historical records with different scenarios
                AttendanceTransaction record1 = AttendanceTransaction.builder()
                                .transactionId(17L)
                                .employeeNo(11L)
                                .attendanceDate(testDate)
                                .projectCode(3L) // Has project
                                .entryTime(LocalDateTime.of(testDate, LocalTime.of(5, 6)))
                                .exitTime(LocalDateTime.of(testDate, LocalTime.of(5, 11)))
                                .workingHours(new BigDecimal("0.08"))
                                .scheduledHours(new BigDecimal("8.00")) // Old
                                .build();

                AttendanceTransaction record2 = AttendanceTransaction.builder()
                                .transactionId(18L)
                                .employeeNo(11L)
                                .attendanceDate(testDate.minusDays(1))
                                .projectCode(null) // No project, should use labor assignment
                                .entryTime(LocalDateTime.of(testDate.minusDays(1), LocalTime.of(8, 0)))
                                .exitTime(LocalDateTime.of(testDate.minusDays(1), LocalTime.of(17, 0)))
                                .workingHours(new BigDecimal("9.00"))
                                .scheduledHours(new BigDecimal("8.00")) // Old default
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceTransaction> attendancePage = new PageImpl<>(
                                Arrays.asList(record1, record2), pageable, 2);

                when(employeeRepository.findById(11L)).thenReturn(Optional.of(testEmployee));
                when(projectRepository.findById(3L)).thenReturn(Optional.of(testProject));
                when(timeScheduleRepository.findByProjectCode(3L))
                                .thenReturn(Collections.singletonList(projectSchedule));
                when(assignmentRepository.findActiveAssignmentsByEmployee(11L))
                                .thenReturn(Collections.singletonList(laborAssignment));

                when(attendanceRepository.findByEmployeeNoAndDateRange(
                                eq(11L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                                .thenReturn(attendancePage);

                // When: Fetch attendance history
                Page<AttendanceResponse> result = attendanceService.getEmployeeAttendance(
                                11L, testDate.minusDays(2), testDate.plusDays(1), pageable);

                // Then: Both records should be recalculated correctly
                assertThat(result.getContent()).hasSize(2);

                // Record 1: Has projectCode, uses project schedule
                AttendanceResponse response1 = result.getContent().get(0);
                assertThat(response1.getProjectCode()).isEqualTo(3L);
                assertThat(response1.getScheduledHours()).isEqualByComparingTo(new BigDecimal("0.10"));

                // Record 2: No projectCode, finds from labor assignment, uses project schedule
                AttendanceResponse response2 = result.getContent().get(1);
                assertThat(response2.getProjectCode()).isEqualTo(3L); // Found from labor assignment
                assertThat(response2.getScheduledHours()).isEqualByComparingTo(new BigDecimal("0.10"));
        }
}
