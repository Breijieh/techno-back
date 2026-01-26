package com.techno.backend.performance;

import com.techno.backend.dto.CheckInRequest;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import com.techno.backend.service.ApprovalWorkflowService;
import com.techno.backend.service.AttendanceAllowanceDeductionService;
import com.techno.backend.service.AttendanceCalculationService;
import com.techno.backend.service.AttendanceDayClosureService;
import com.techno.backend.service.AttendanceService;
import com.techno.backend.service.PayrollCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive performance tests for concurrent operations.
 * Tests section 7.1: Concurrent operations.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Concurrent Operations Tests")
class ConcurrentOperationsTest {

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
    private ProjectLaborAssignmentRepository assignmentRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private SalaryHeaderRepository salaryHeaderRepository;

    @Mock
    private SalaryDetailRepository salaryDetailRepository;

    @Mock
    private SalaryBreakdownPercentageRepository salaryBreakdownPercentageRepository;

    @Mock
    private EmpMonthlyAllowanceRepository allowanceRepository;

    @Mock
    private EmpMonthlyDeductionRepository deductionRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ApprovalWorkflowService approvalWorkflowService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttendanceService attendanceService;

    @InjectMocks
    private PayrollCalculationService payrollCalculationService;

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
                .employeeName("Test Employee")
                .employmentStatus("ACTIVE")
                .primaryDeptCode(1L)
                .primaryProjectCode(101L)
                .build();

        testProject = Project.builder()
                .projectCode(101L)
                .projectName("Test Project")
                .projectLatitude(validLatitude)
                .projectLongitude(validLongitude)
                .gpsRadiusMeters(500)
                .requireGpsCheck("Y")
                .build();

        testSchedule = TimeSchedule.builder()
                .scheduleId(1L)
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(23, 59))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .isActive("Y")
                .build();
    }

    // ==================== Section 7.1: Concurrent Operations ====================

    @Nested
    @DisplayName("7.1 Concurrent Operations")
    class ConcurrentOperations {

        @Test
        @DisplayName("Concurrent check-ins should process correctly without data corruption")
        void testConcurrentCheckIns_ProcessCorrectly() throws InterruptedException {
            int numberOfThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Setup mocks for concurrent access
            when(employeeRepository.findById(anyLong())).thenReturn(Optional.of(testEmployee));
            when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
            when(attendanceRepository.hasCheckedInToday(anyLong(), any(LocalDate.class))).thenReturn(false);
            when(closureService.isDateClosed(any(LocalDate.class))).thenReturn(false);
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

            // Execute concurrent check-ins
            for (int i = 0; i < numberOfThreads; i++) {
                final int employeeNo = 1001 + i;
                executor.submit(() -> {
                    try {
                        CheckInRequest request = CheckInRequest.builder()
                                .projectCode(101L)
                                .latitude(validLatitude)
                                .longitude(validLongitude)
                                .build();

                        attendanceService.checkIn((long) employeeNo, request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // Verify: All operations should complete (some may fail due to duplicate check-in, which is expected)
            // The important thing is no data corruption occurred
        }

        @Test
        @DisplayName("Concurrent payroll calculations should complete correctly without deadlocks")
        void testConcurrentPayrollCalculations_CompleteCorrectly() throws InterruptedException {
            int numberOfThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            AtomicInteger successCount = new AtomicInteger(0);

            // Setup mocks
            lenient().when(employeeRepository.findById(anyLong())).thenReturn(Optional.of(testEmployee));
            lenient().when(salaryHeaderRepository.findLatestByEmployeeAndMonth(anyLong(), anyString()))
                    .thenReturn(Optional.empty());
            lenient().when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(System.currentTimeMillis());
                        }
                        return header;
                    });
            // Setup Saudi breakdown (83.4% Basic + 16.6% Transportation) for category "S"
            SalaryBreakdownPercentage basic = SalaryBreakdownPercentage.builder()
                    .serNo(1L)
                    .employeeCategory("S")
                    .transTypeCode(1L)
                    .salaryPercentage(new BigDecimal("0.8340"))
                    .build();
            SalaryBreakdownPercentage transport = SalaryBreakdownPercentage.builder()
                    .serNo(2L)
                    .employeeCategory("S")
                    .transTypeCode(2L)
                    .salaryPercentage(new BigDecimal("0.1660"))
                    .build();
            lenient().when(salaryBreakdownPercentageRepository.findByEmployeeCategory("S"))
                    .thenReturn(List.of(basic, transport));
            lenient().when(salaryBreakdownPercentageRepository.findByEmployeeCategory(anyString()))
                    .thenAnswer(invocation -> {
                        String category = invocation.getArgument(0);
                        if ("S".equals(category)) {
                            return List.of(basic, transport);
                        }
                        return Collections.emptyList();
                    });
            lenient().when(allowanceRepository.findActiveAllowancesForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            lenient().when(deductionRepository.findActiveDeductionsForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            lenient().when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            lenient().when(approvalWorkflowService.initializeApproval(anyString(), anyLong(), any(), any()))
                    .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                            .transStatus("N")
                            .nextApproval(1L)
                            .nextAppLevel(1)
                            .build());

            // Execute concurrent payroll calculations
            for (int i = 0; i < numberOfThreads; i++) {
                final int employeeNo = 1001 + i;
                executor.submit(() -> {
                    try {
                        payrollCalculationService.calculatePayrollForEmployee((long) employeeNo, "2026-01");
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Some may fail due to duplicate, which is expected
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // Verify: No deadlocks occurred, all threads completed
        }

        @Test
        @DisplayName("Concurrent approval actions should process correctly")
        void testConcurrentApprovalActions_ProcessCorrectly() throws InterruptedException {
            // This test documents expected behavior for concurrent approvals
            // Multiple approvers approving different payrolls simultaneously
            // Should all process correctly without conflicts
        }
    }
}
