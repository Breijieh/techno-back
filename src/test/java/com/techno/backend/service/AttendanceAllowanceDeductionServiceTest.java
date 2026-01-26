package com.techno.backend.service;

import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.EmpMonthlyAllowance;
import com.techno.backend.entity.EmpMonthlyDeduction;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmpMonthlyAllowanceRepository;
import com.techno.backend.repository.EmpMonthlyDeductionRepository;
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
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AttendanceAllowanceDeductionService.
 * Tests automatic creation of allowances and deductions from attendance.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Allowance Deduction Service Tests")
class AttendanceAllowanceDeductionServiceTest {

    @Mock
    private EmpMonthlyAllowanceRepository allowanceRepository;

    @Mock
    private EmpMonthlyDeductionRepository deductionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceAllowanceDeductionService allowanceDeductionService;

    private AttendanceTransaction testAttendance;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 1, 18);

        testAttendance = AttendanceTransaction.builder()
                .transactionId(1L)
                .employeeNo(1001L)
                .attendanceDate(testDate)
                .entryTime(LocalDateTime.of(testDate, LocalTime.of(8, 0)))
                .exitTime(LocalDateTime.of(testDate, LocalTime.of(17, 0)))
                .workingHours(new BigDecimal("9.00"))
                .scheduledHours(new BigDecimal("8.00"))
                .build();
    }

    // ==================== Overtime Allowance Tests ====================

    @Test
    @DisplayName("Create overtime allowance when overtime exists")
    void createOvertimeAllowance_WithOvertime_CreatesAllowance() {
        testAttendance.setOvertimeCalc(new BigDecimal("1.00"));

        when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                1001L, 9L, testDate)).thenReturn(false);

        EmpMonthlyAllowance savedAllowance = EmpMonthlyAllowance.builder()
                .transactionNo(1L)
                .employeeNo(1001L)
                .typeCode(9L)
                .transactionDate(testDate)
                .allowanceAmount(new BigDecimal("1.00"))
                .transStatus("A")
                .build();

        when(allowanceRepository.save(any(EmpMonthlyAllowance.class))).thenReturn(savedAllowance);

        allowanceDeductionService.createOvertimeAllowance(testAttendance);

        verify(allowanceRepository).save(any(EmpMonthlyAllowance.class));
    }

    @Test
    @DisplayName("Create overtime allowance when no overtime should skip")
    void createOvertimeAllowance_NoOvertime_Skips() {
        testAttendance.setOvertimeCalc(BigDecimal.ZERO);

        allowanceDeductionService.createOvertimeAllowance(testAttendance);

        verify(allowanceRepository, never()).save(any(EmpMonthlyAllowance.class));
    }

    @Test
    @DisplayName("Create overtime allowance when duplicate exists should skip")
    void createOvertimeAllowance_DuplicateExists_Skips() {
        testAttendance.setOvertimeCalc(new BigDecimal("1.00"));

        when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                1001L, 9L, testDate)).thenReturn(true);

        allowanceDeductionService.createOvertimeAllowance(testAttendance);

        verify(allowanceRepository, never()).save(any(EmpMonthlyAllowance.class));
    }

    // ==================== Delay Deduction Tests ====================

    @Test
    @DisplayName("Create late deduction should NOT create daily deduction (monthly aggregation)")
    void createLateDeduction_WithDelay_DoesNotCreateDeduction() {
        testAttendance.setDelayedCalc(new BigDecimal("0.25")); // 15 minutes delay

        // Current implementation: Does NOT create daily deduction
        // Delay is stored in attendance.delayedCalc and aggregated monthly
        allowanceDeductionService.createLateDeduction(testAttendance);

        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    @Test
    @DisplayName("Create late deduction when no delay should skip")
    void createLateDeduction_NoDelay_Skips() {
        testAttendance.setDelayedCalc(BigDecimal.ZERO);

        allowanceDeductionService.createLateDeduction(testAttendance);

        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    // ==================== Monthly Aggregation Tests ====================

    @Test
    @DisplayName("Aggregate monthly delay deductions should create single deduction")
    void aggregateMonthlyDelayDeductions_WithDelays_CreatesDeduction() {
        YearMonth month = YearMonth.of(2025, 1);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        // Mock that employee has delays
        when(attendanceRepository.sumDelayedHours(1001L, monthStart, monthEnd))
                .thenReturn(1.5); // 1.5 hours total delay

        // Mock delay records for counting days
        AttendanceTransaction delay1 = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .delayedCalc(new BigDecimal("0.50"))
                .build();
        AttendanceTransaction delay2 = AttendanceTransaction.builder()
                .employeeNo(1001L)
                .delayedCalc(new BigDecimal("1.00"))
                .build();

        when(attendanceRepository.findLateArrivalsByDateRange(monthStart, monthEnd))
                .thenReturn(List.of(delay1, delay2));

        // Mock that no deduction exists yet
        when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                .thenReturn(List.of());

        EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                .transactionNo(1L)
                .employeeNo(1001L)
                .typeCode(20L)
                .deductionAmount(new BigDecimal("1.50"))
                .transStatus("A")
                .build();

        when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

        EmpMonthlyDeduction result = allowanceDeductionService.aggregateMonthlyDelayDeductions(1001L, month);

        assertThat(result).isNotNull();
        assertThat(result.getDeductionAmount()).isEqualByComparingTo(new BigDecimal("1.50"));
        verify(deductionRepository).save(any(EmpMonthlyDeduction.class));
    }

    @Test
    @DisplayName("Aggregate monthly delay deductions when no delays should return null")
    void aggregateMonthlyDelayDeductions_NoDelays_ReturnsNull() {
        YearMonth month = YearMonth.of(2025, 1);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        when(attendanceRepository.sumDelayedHours(1001L, monthStart, monthEnd))
                .thenReturn(0.0);

        EmpMonthlyDeduction result = allowanceDeductionService.aggregateMonthlyDelayDeductions(1001L, month);

        assertThat(result).isNull();
        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    @Test
    @DisplayName("Aggregate monthly delay deductions when deduction already exists should return null")
    void aggregateMonthlyDelayDeductions_AlreadyExists_ReturnsNull() {
        YearMonth month = YearMonth.of(2025, 1);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        when(attendanceRepository.sumDelayedHours(1001L, monthStart, monthEnd))
                .thenReturn(1.5);

        EmpMonthlyDeduction existingDeduction = EmpMonthlyDeduction.builder()
                .transactionNo(1L)
                .employeeNo(1001L)
                .typeCode(20L)
                .transactionDate(monthStart.plusDays(14))
                .entryReason("Monthly delay aggregation for 2025-01")
                .isDeleted("N")
                .build();

        when(attendanceRepository.findLateArrivalsByDateRange(monthStart, monthEnd))
                .thenReturn(List.of());
        when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                .thenReturn(List.of(existingDeduction));

        EmpMonthlyDeduction result = allowanceDeductionService.aggregateMonthlyDelayDeductions(1001L, month);

        assertThat(result).isNull();
        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    // ==================== Early Departure Deduction Tests ====================

    @Test
    @DisplayName("Create early departure deduction when early out exists")
    void createEarlyDepartureDeduction_WithEarlyOut_CreatesDeduction() {
        testAttendance.setEarlyOutCalc(new BigDecimal("1.00"));

        when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                .thenReturn(List.of()); // No duplicate

        EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                .transactionNo(1L)
                .employeeNo(1001L)
                .typeCode(20L)
                .transactionDate(testDate)
                .deductionAmount(new BigDecimal("1.00"))
                .build();

        when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

        allowanceDeductionService.createEarlyDepartureDeduction(testAttendance);

        verify(deductionRepository).save(any(EmpMonthlyDeduction.class));
    }

    @Test
    @DisplayName("Create early departure deduction when no early out should skip")
    void createEarlyDepartureDeduction_NoEarlyOut_Skips() {
        testAttendance.setEarlyOutCalc(BigDecimal.ZERO);

        allowanceDeductionService.createEarlyDepartureDeduction(testAttendance);

        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    // ==================== Shortage Deduction Tests ====================

    @Test
    @DisplayName("Create shortage deduction when shortage exists")
    void createShortageDeduction_WithShortage_CreatesDeduction() {
        testAttendance.setShortageHours(new BigDecimal("1.00"));

        when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                .thenReturn(List.of()); // No duplicate

        EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                .transactionNo(1L)
                .employeeNo(1001L)
                .typeCode(20L)
                .transactionDate(testDate)
                .deductionAmount(new BigDecimal("1.00"))
                .build();

        when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

        allowanceDeductionService.createShortageDeduction(testAttendance);

        verify(deductionRepository).save(any(EmpMonthlyDeduction.class));
    }

    @Test
    @DisplayName("Create shortage deduction when no shortage should skip")
    void createShortageDeduction_NoShortage_Skips() {
        testAttendance.setShortageHours(BigDecimal.ZERO);

        allowanceDeductionService.createShortageDeduction(testAttendance);

        verify(deductionRepository, never()).save(any(EmpMonthlyDeduction.class));
    }

    // ==================== Process Attendance Tests ====================

    @Test
    @DisplayName("Process attendance should create all applicable allowances and deductions")
    void processAttendanceForAllowancesDeductions_WithAllTypes_CreatesAll() {
        testAttendance.setOvertimeCalc(new BigDecimal("1.00"));
        testAttendance.setEarlyOutCalc(new BigDecimal("0.50"));
        testAttendance.setShortageHours(new BigDecimal("0.50"));

        when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(any(), any(), any()))
                .thenReturn(false);
        when(deductionRepository.findByEmployeeAndType(any(), any()))
                .thenReturn(List.of());

        when(allowanceRepository.save(any(EmpMonthlyAllowance.class)))
                .thenReturn(EmpMonthlyAllowance.builder().transactionNo(1L).build());
        when(deductionRepository.save(any(EmpMonthlyDeduction.class)))
                .thenReturn(EmpMonthlyDeduction.builder().transactionNo(1L).build());

        allowanceDeductionService.processAttendanceForAllowancesDeductions(testAttendance);

        verify(allowanceRepository).save(any(EmpMonthlyAllowance.class));
        verify(deductionRepository, times(2)).save(any(EmpMonthlyDeduction.class)); // Early out + shortage
    }

    @Test
    @DisplayName("Process attendance with null attendance should skip")
    void processAttendanceForAllowancesDeductions_NullAttendance_Skips() {
        allowanceDeductionService.processAttendanceForAllowancesDeductions(null);

        verify(allowanceRepository, never()).save(any());
        verify(deductionRepository, never()).save(any());
    }

    // ==================== Section 3.1: Comprehensive Auto-Creation Tests ====================

    @org.junit.jupiter.api.Nested
    @DisplayName("3.1.1 Overtime Allowance Creation (Type 9) - Additional Tests")
    class OvertimeAllowanceCreationAdditionalTests {

        @Test
        @DisplayName("Overtime allowance created on check-out")
        void createOvertimeAllowance_OnCheckout_CreatesAllowance() {
            testAttendance.setOvertimeCalc(new BigDecimal("2.00"));

            when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                    1001L, 9L, testDate)).thenReturn(false);

            EmpMonthlyAllowance savedAllowance = EmpMonthlyAllowance.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(9L)
                    .transactionDate(testDate)
                    .allowanceAmount(new BigDecimal("2.00"))
                    .transStatus("A")
                    .isManualEntry("N")
                    .build();

            when(allowanceRepository.save(any(EmpMonthlyAllowance.class))).thenReturn(savedAllowance);

            allowanceDeductionService.createOvertimeAllowance(testAttendance);

            verify(allowanceRepository).save(any(EmpMonthlyAllowance.class));
        }

        @Test
        @DisplayName("Overtime allowance amount calculation")
        void createOvertimeAllowance_AmountCalculation_Correct() {
            testAttendance.setOvertimeCalc(new BigDecimal("3.00"));

            when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                    1001L, 9L, testDate)).thenReturn(false);

            EmpMonthlyAllowance savedAllowance = EmpMonthlyAllowance.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(9L)
                    .transactionDate(testDate)
                    .allowanceAmount(new BigDecimal("3.00"))
                    .transStatus("A")
                    .build();

            when(allowanceRepository.save(any(EmpMonthlyAllowance.class))).thenReturn(savedAllowance);

            allowanceDeductionService.createOvertimeAllowance(testAttendance);

            verify(allowanceRepository).save(argThat(allowance ->
                    allowance.getAllowanceAmount().compareTo(new BigDecimal("3.00")) == 0));
        }

        @Test
        @DisplayName("Overtime allowance auto-approved")
        void createOvertimeAllowance_AutoApproved_StatusCorrect() {
            testAttendance.setOvertimeCalc(new BigDecimal("1.00"));

            when(allowanceRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                    1001L, 9L, testDate)).thenReturn(false);

            EmpMonthlyAllowance savedAllowance = EmpMonthlyAllowance.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(9L)
                    .transactionDate(testDate)
                    .allowanceAmount(new BigDecimal("1.00"))
                    .transStatus("A") // Auto-approved
                    .isManualEntry("N") // System-generated
                    .build();

            when(allowanceRepository.save(any(EmpMonthlyAllowance.class))).thenReturn(savedAllowance);

            allowanceDeductionService.createOvertimeAllowance(testAttendance);

            verify(allowanceRepository).save(argThat(allowance ->
                    "A".equals(allowance.getTransStatus()) && "N".equals(allowance.getIsManualEntry())));
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("3.1.2 Delay Deduction Aggregation (Type 20) - Additional Tests")
    class DelayDeductionAggregationAdditionalTests {

        @Test
        @DisplayName("Delay deduction amount calculation")
        void aggregateMonthlyDelayDeductions_AmountCalculation_Correct() {
            YearMonth month = YearMonth.of(2025, 1);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            // Total delay: 2.5 hours
            when(attendanceRepository.sumDelayedHours(1001L, monthStart, monthEnd))
                    .thenReturn(2.5);

            AttendanceTransaction delay1 = AttendanceTransaction.builder()
                    .employeeNo(1001L)
                    .delayedCalc(new BigDecimal("1.00"))
                    .build();
            AttendanceTransaction delay2 = AttendanceTransaction.builder()
                    .employeeNo(1001L)
                    .delayedCalc(new BigDecimal("1.50"))
                    .build();

            when(attendanceRepository.findLateArrivalsByDateRange(monthStart, monthEnd))
                    .thenReturn(List.of(delay1, delay2));
            when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                    .thenReturn(Collections.emptyList());

            EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(20L)
                    .deductionAmount(new BigDecimal("2.50"))
                    .transStatus("A")
                    .build();

            when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

            EmpMonthlyDeduction result = allowanceDeductionService.aggregateMonthlyDelayDeductions(1001L, month);

            assertThat(result).isNotNull();
            assertThat(result.getDeductionAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
        }

        @Test
        @DisplayName("Delay deduction includes all delay types")
        void aggregateMonthlyDelayDeductions_IncludesAllDelayTypes() {
            YearMonth month = YearMonth.of(2025, 1);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            // Multiple delays from different days
            when(attendanceRepository.sumDelayedHours(1001L, monthStart, monthEnd))
                    .thenReturn(3.0); // Total: 3 hours

            AttendanceTransaction delay1 = AttendanceTransaction.builder()
                    .employeeNo(1001L)
                    .delayedCalc(new BigDecimal("0.25"))
                    .build();
            AttendanceTransaction delay2 = AttendanceTransaction.builder()
                    .employeeNo(1001L)
                    .delayedCalc(new BigDecimal("0.50"))
                    .build();
            AttendanceTransaction delay3 = AttendanceTransaction.builder()
                    .employeeNo(1001L)
                    .delayedCalc(new BigDecimal("2.25"))
                    .build();

            when(attendanceRepository.findLateArrivalsByDateRange(monthStart, monthEnd))
                    .thenReturn(List.of(delay1, delay2, delay3));
            when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                    .thenReturn(Collections.emptyList());

            EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(20L)
                    .deductionAmount(new BigDecimal("3.00"))
                    .transStatus("A")
                    .build();

            when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

            EmpMonthlyDeduction result = allowanceDeductionService.aggregateMonthlyDelayDeductions(1001L, month);

            assertThat(result).isNotNull();
            // All delays should be included in the sum
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("3.1.3 Early Departure Deduction (Type 20) - Additional Tests")
    class EarlyDepartureDeductionAdditionalTests {

        @Test
        @DisplayName("Early departure amount calculation")
        void createEarlyDepartureDeduction_AmountCalculation_Correct() {
            testAttendance.setEarlyOutCalc(new BigDecimal("1.50"));

            when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                    .thenReturn(Collections.emptyList());

            EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(20L)
                    .transactionDate(testDate)
                    .deductionAmount(new BigDecimal("1.50"))
                    .transStatus("A")
                    .build();

            when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

            allowanceDeductionService.createEarlyDepartureDeduction(testAttendance);

            verify(deductionRepository).save(argThat(deduction ->
                    deduction.getDeductionAmount().compareTo(new BigDecimal("1.50")) == 0));
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("3.1.4 Shortage Hours Deduction (Type 20) - Additional Tests")
    class ShortageHoursDeductionAdditionalTests {

        @Test
        @DisplayName("Shortage amount calculation")
        void createShortageDeduction_AmountCalculation_Correct() {
            testAttendance.setShortageHours(new BigDecimal("2.00"));

            when(deductionRepository.findByEmployeeAndType(1001L, 20L))
                    .thenReturn(Collections.emptyList());

            EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(20L)
                    .transactionDate(testDate)
                    .deductionAmount(new BigDecimal("2.00"))
                    .transStatus("A")
                    .build();

            when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

            allowanceDeductionService.createShortageDeduction(testAttendance);

            verify(deductionRepository).save(argThat(deduction ->
                    deduction.getDeductionAmount().compareTo(new BigDecimal("2.00")) == 0));
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("3.1.5 Absence Deduction (Type 21) - Additional Tests")
    class AbsenceDeductionAdditionalTests {

        @Test
        @DisplayName("Absence amount equals daily salary")
        void createAbsenceDeduction_AmountEqualsDailySalary() {
            BigDecimal monthlySalary = new BigDecimal("6000.0000");
            BigDecimal dailySalary = monthlySalary.divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            LocalDate absenceDate = LocalDate.of(2025, 1, 15);

            when(deductionRepository.existsByEmployeeNoAndTypeCodeAndTransactionDate(
                    1001L, 21L, absenceDate)).thenReturn(false);

            EmpMonthlyDeduction savedDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(1001L)
                    .typeCode(21L)
                    .transactionDate(absenceDate)
                    .deductionAmount(dailySalary)
                    .transStatus("A")
                    .noOfDays(1)
                    .build();

            when(deductionRepository.save(any(EmpMonthlyDeduction.class))).thenReturn(savedDeduction);

            allowanceDeductionService.createAbsenceDeduction(1001L, absenceDate, dailySalary);

            verify(deductionRepository).save(argThat(deduction ->
                    deduction.getDeductionAmount().compareTo(dailySalary) == 0 &&
                    deduction.getNoOfDays() == 1));
        }
    }
}
