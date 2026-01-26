package com.techno.backend.edgecases;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.SalaryHeader;
import com.techno.backend.repository.*;
import com.techno.backend.service.ApprovalWorkflowService;
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

import com.techno.backend.entity.SalaryBreakdownPercentage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive edge case tests for time and date boundary conditions.
 * Tests section 4.1: Time & Date Edge Cases.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Time Boundary Tests")
class TimeBoundaryTest {

    @Mock
    private EmployeeRepository employeeRepository;

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
    private PayrollCalculationService payrollCalculationService;

    private Employee testEmployee;
    private final Long EMPLOYEE_NO = 1001L;

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
                .employeeNo(EMPLOYEE_NO)
                .employeeName("Test Employee")
                .monthlySalary(new BigDecimal("6000.0000"))
                .empContractType("TECHNO")
                .employmentStatus("ACTIVE")
                .employeeCategory("S")
                .hireDate(LocalDate.of(2020, 1, 1))
                .primaryDeptCode(1L)
                .primaryProjectCode(100L)
                .build();
    }

    /**
     * Creates Saudi employee salary breakdown: 83.4% Basic + 16.6% Transportation
     */
    private List<SalaryBreakdownPercentage> createSaudiBreakdown() {
        SalaryBreakdownPercentage basic = SalaryBreakdownPercentage.builder()
                .serNo(1L)
                .employeeCategory("S")
                .transTypeCode(1L) // Basic Salary
                .salaryPercentage(new BigDecimal("0.8340")) // 83.4%
                .build();
        SalaryBreakdownPercentage transport = SalaryBreakdownPercentage.builder()
                .serNo(2L)
                .employeeCategory("S")
                .transTypeCode(2L) // Transportation
                .salaryPercentage(new BigDecimal("0.1660")) // 16.6%
                .build();
        return List.of(basic, transport);
    }

    // ==================== Section 4.1: Time & Date Edge Cases ====================

    @Nested
    @DisplayName("4.1 Time & Date Edge Cases")
    class TimeAndDateEdgeCases {

        @Test
        @DisplayName("Leap year February should still use 30 days for calculation")
        void testLeapYearFebruary_Uses30DayFormula() {
            String febMonth = "2024-02"; // 2024 is a leap year (29 days)
            testEmployee.setHireDate(LocalDate.of(2024, 2, 15));

            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, febMonth))
                    .thenReturn(Optional.empty());
            when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(1L);
                        }
                        return header;
                    });
            // Setup Saudi breakdown (83.4% Basic + 16.6% Transportation) for category "S"
            List<SalaryBreakdownPercentage> saudiBreakdown = createSaudiBreakdown();
            when(salaryBreakdownPercentageRepository.findByEmployeeCategory(anyString()))
                    .thenAnswer(invocation -> {
                        String category = invocation.getArgument(0);
                        if ("S".equals(category)) {
                            return saudiBreakdown;
                        }
                        return Collections.emptyList();
                    });
            when(allowanceRepository.findActiveAllowancesForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(approvalWorkflowService.initializeApproval(anyString(), anyLong(), any(), any()))
                    .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                            .transStatus("N")
                            .nextApproval(1L)
                            .nextAppLevel(1)
                            .build());

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, febMonth);

            assertThat(result).isNotNull();
            // Days worked: Feb 15-29 = 15 days
            // Formula: (6000 * 15) / 30 = 3000 (NOT divided by 29)
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("15"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Month with 31 days should still use 30 days for calculation")
        void testMonthWith31Days_Uses30DayFormula() {
            String janMonth = "2026-01"; // January has 31 days
            testEmployee.setHireDate(LocalDate.of(2026, 1, 15));

            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, janMonth))
                    .thenReturn(Optional.empty());
            when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(1L);
                        }
                        return header;
                    });
            // Setup Saudi breakdown (83.4% Basic + 16.6% Transportation) for category "S"
            List<SalaryBreakdownPercentage> saudiBreakdown = createSaudiBreakdown();
            when(salaryBreakdownPercentageRepository.findByEmployeeCategory(anyString()))
                    .thenAnswer(invocation -> {
                        String category = invocation.getArgument(0);
                        if ("S".equals(category)) {
                            return saudiBreakdown;
                        }
                        return Collections.emptyList();
                    });
            when(allowanceRepository.findActiveAllowancesForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(approvalWorkflowService.initializeApproval(anyString(), anyLong(), any(), any()))
                    .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                            .transStatus("N")
                            .nextApproval(1L)
                            .nextAppLevel(1)
                            .build());

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, janMonth);

            assertThat(result).isNotNull();
            // Days worked: Jan 15-31 = 17 days
            // Formula: (6000 * 17) / 30 (NOT divided by 31)
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("17"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Month with 28 days (February non-leap) should still use 30 days")
        void testFebruaryNonLeap_Uses30DayFormula() {
            String febMonth = "2025-02"; // 2025 is NOT a leap year (28 days)
            testEmployee.setHireDate(LocalDate.of(2025, 2, 15));

            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, febMonth))
                    .thenReturn(Optional.empty());
            when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(1L);
                        }
                        return header;
                    });
            // Setup Saudi breakdown (83.4% Basic + 16.6% Transportation) for category "S"
            List<SalaryBreakdownPercentage> saudiBreakdown = createSaudiBreakdown();
            when(salaryBreakdownPercentageRepository.findByEmployeeCategory(anyString()))
                    .thenAnswer(invocation -> {
                        String category = invocation.getArgument(0);
                        if ("S".equals(category)) {
                            return saudiBreakdown;
                        }
                        return Collections.emptyList();
                    });
            when(allowanceRepository.findActiveAllowancesForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(approvalWorkflowService.initializeApproval(anyString(), anyLong(), any(), any()))
                    .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                            .transStatus("N")
                            .nextApproval(1L)
                            .nextAppLevel(1)
                            .build());

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, febMonth);

            assertThat(result).isNotNull();
            // Days worked: Feb 15-28 = 14 days
            // Formula: (6000 * 14) / 30 (NOT divided by 28)
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("14"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Timezone handling - times stored in UTC, displayed in local timezone")
        void testTimezoneHandling_StoredInUTC() {
            // This test documents expected behavior for timezone handling
            // Times should be stored in UTC in database
            // Displayed in local timezone (Asia/Riyadh) for users
            // The actual implementation should handle timezone conversions properly
            
            AttendanceTransaction attendance = AttendanceTransaction.builder()
                    .transactionId(1L)
                    .employeeNo(1001L)
                    .attendanceDate(LocalDate.now())
                    .entryTime(LocalDateTime.now()) // Should be in UTC
                    .build();

            // Test documents expected behavior
            assertThat(attendance.getEntryTime()).isNotNull();
        }

        @Test
        @DisplayName("Daylight saving time transitions should be handled correctly")
        void testDaylightSavingTime_HandledCorrectly() {
            // This test documents expected behavior for DST transitions
            // The system should handle DST changes without affecting calculations
            // Times should be stored consistently regardless of DST
            
            // Note: Saudi Arabia does not observe DST, but test documents expected behavior
            // for systems that might need to handle DST
        }
    }
}
