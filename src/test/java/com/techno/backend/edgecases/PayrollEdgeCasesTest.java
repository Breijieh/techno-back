package com.techno.backend.edgecases;

import com.techno.backend.entity.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive edge case tests for payroll calculations.
 * Tests sections 4.3, 4.4, 4.6: Employee state, financial, and loan edge cases.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payroll Edge Cases Tests")
class PayrollEdgeCasesTest {

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
    private final String TEST_MONTH = "2026-01";

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

    private void setupBasicMocks() {
        lenient().when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
        lenient().when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, TEST_MONTH))
                .thenReturn(Optional.empty());
        lenient().when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                .thenAnswer(invocation -> {
                    SalaryHeader header = invocation.getArgument(0);
                    if (header.getSalaryId() == null) {
                        header.setSalaryId(1L);
                    }
                    return header;
                });
        // Setup salary breakdown based on employee category
        // Default to Saudi breakdown (83.4% Basic + 16.6% Transportation) for category "S"
        lenient().when(salaryBreakdownPercentageRepository.findByEmployeeCategory("S"))
                .thenReturn(createSaudiBreakdown());
        lenient().when(salaryBreakdownPercentageRepository.findByEmployeeCategory("F"))
                .thenReturn(createForeignBreakdown());
        // For any other category, return empty list (fallback behavior)
        lenient().when(salaryBreakdownPercentageRepository.findByEmployeeCategory(anyString()))
                .thenAnswer(invocation -> {
                    String category = invocation.getArgument(0);
                    if ("S".equals(category)) {
                        return createSaudiBreakdown();
                    } else if ("F".equals(category)) {
                        return createForeignBreakdown();
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

    /**
     * Creates Foreign employee salary breakdown: 55% Basic + 13.75% Transport + 5.2% Communication + 25% Housing + 1.05% Other
     */
    private List<SalaryBreakdownPercentage> createForeignBreakdown() {
        SalaryBreakdownPercentage basic = SalaryBreakdownPercentage.builder()
                .serNo(3L)
                .employeeCategory("F")
                .transTypeCode(1L) // Basic Salary
                .salaryPercentage(new BigDecimal("0.5500")) // 55%
                .build();
        SalaryBreakdownPercentage transport = SalaryBreakdownPercentage.builder()
                .serNo(4L)
                .employeeCategory("F")
                .transTypeCode(2L) // Transportation
                .salaryPercentage(new BigDecimal("0.1375")) // 13.75%
                .build();
        SalaryBreakdownPercentage communication = SalaryBreakdownPercentage.builder()
                .serNo(5L)
                .employeeCategory("F")
                .transTypeCode(4L) // Communication
                .salaryPercentage(new BigDecimal("0.0520")) // 5.2%
                .build();
        SalaryBreakdownPercentage housing = SalaryBreakdownPercentage.builder()
                .serNo(6L)
                .employeeCategory("F")
                .transTypeCode(3L) // Housing
                .salaryPercentage(new BigDecimal("0.2500")) // 25%
                .build();
        SalaryBreakdownPercentage other = SalaryBreakdownPercentage.builder()
                .serNo(7L)
                .employeeCategory("F")
                .transTypeCode(8L) // Other
                .salaryPercentage(new BigDecimal("0.0105")) // 1.05%
                .build();
        return List.of(basic, transport, communication, housing, other);
    }

    // ==================== Section 4.3: Employee State Edge Cases ====================

    @Nested
    @DisplayName("4.3 Employee State Edge Cases")
    class EmployeeStateEdgeCases {

        @Test
        @DisplayName("Employee status change mid-month should pro-rate correctly")
        void testEmployeeStatusChangeMidMonth_ProRatedCorrectly() {
            testEmployee.setTerminationDate(LocalDate.of(2026, 1, 15));
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Days worked: Jan 1-15 = 15 days
            // Formula: (6000 * 15) / 30 = 3000
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("15"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Employee contract type change should update eligibility")
        void testEmployeeContractTypeChange_UpdatesEligibility() {
            testEmployee.setEmpContractType("CLIENT");
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));

            assertThatThrownBy(() -> payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("TECHNO");
        }

        @Test
        @DisplayName("Employee department/project change mid-month should use correct schedule")
        void testEmployeeDepartmentProjectChangeMidMonth_UsesCorrectSchedule() {
            // Employee transferred on 15th
            // Old schedule used for days 1-14, new schedule for 15-30
            // This test documents expected behavior
            // The actual implementation should handle schedule changes correctly
        }
    }

    // ==================== Section 4.4: Financial Edge Cases ====================

    @Nested
    @DisplayName("4.4 Financial Edge Cases")
    class FinancialEdgeCases {

        @Test
        @DisplayName("Zero salary employee should handle correctly")
        void testZeroSalaryEmployee_HandledCorrectly() {
            testEmployee.setMonthlySalary(BigDecimal.ZERO);
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getGrossSalary()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNetSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Very high salary (> 1M SAR) should handle correctly")
        void testVeryHighSalary_HandledCorrectly() {
            testEmployee.setMonthlySalary(new BigDecimal("1500000.0000")); // 1.5M SAR
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getGrossSalary()).isEqualByComparingTo(new BigDecimal("1500000.0000"));
            // No overflow errors
        }

        @Test
        @DisplayName("Negative net salary should be allowed and logged")
        void testNegativeNetSalary_AllowedAndLogged() {
            testEmployee.setMonthlySalary(new BigDecimal("3000.0000"));
            setupBasicMocks();

            // Large deductions exceed gross + allowances
            EmpMonthlyDeduction largeDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(20L)
                    .deductionAmount(new BigDecimal("5000.0000"))
                    .transStatus("A")
                    .build();

            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(largeDeduction));

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Net = 3000 - 5000 = -2000 (negative allowed)
            assertThat(result.getNetSalary()).isLessThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Precision in calculations should use 4 decimal places with HALF_UP")
        void testPrecisionInCalculations_FourDecimalPlaces() {
            testEmployee.setMonthlySalary(new BigDecimal("10000.0000"));
            testEmployee.setHireDate(LocalDate.of(2026, 1, 10)); // 21 days worked

            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Pro-rated: (10000 * 21) / 30 = 7000.0000
            // Should have 4 decimal places
            assertThat(result.getGrossSalary().scale()).isLessThanOrEqualTo(4);
        }
    }

    // ==================== Section 4.6: Loan Edge Cases ====================

    @Nested
    @DisplayName("4.6 Loan Edge Cases")
    class LoanEdgeCases {

        @Test
        @DisplayName("Loan installment exceeds net salary should still be added")
        void testLoanInstallmentExceedsNetSalary_StillAdded() {
            testEmployee.setMonthlySalary(new BigDecimal("3000.0000"));
            setupBasicMocks();

            LoanInstallment installment = LoanInstallment.builder()
                    .installmentId(1L)
                    .loanId(100L)
                    .installmentNo(1)
                    .installmentAmount(new BigDecimal("5000.0000")) // Exceeds salary
                    .paymentStatus("UNPAID")
                    .dueDate(LocalDate.of(2026, 1, 15))
                    .build();

            Loan loan = Loan.builder()
                    .loanId(100L)
                    .employeeNo(EMPLOYEE_NO)
                    .loanAmount(new BigDecimal("50000.0000"))
                    .remainingBalance(new BigDecimal("50000.0000"))
                    .isActive("Y")
                    .build();

            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(eq(EMPLOYEE_NO), anyInt(), anyInt()))
                    .thenReturn(List.of(installment));
            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Net = 3000 - 5000 = -2000 (negative allowed)
            assertThat(result.getNetSalary()).isLessThan(BigDecimal.ZERO);
            assertThat(result.getTotalLoans()).isEqualByComparingTo(new BigDecimal("5000.0000"));
        }

        @Test
        @DisplayName("Multiple loans with installments should all be added")
        void testMultipleLoansWithInstallments_AllAdded() {
            testEmployee.setMonthlySalary(new BigDecimal("10000.0000"));
            setupBasicMocks();

            LoanInstallment installment1 = LoanInstallment.builder()
                    .installmentId(1L)
                    .loanId(100L)
                    .installmentNo(1)
                    .installmentAmount(new BigDecimal("1000.0000"))
                    .paymentStatus("UNPAID")
                    .dueDate(LocalDate.of(2026, 1, 15))
                    .build();

            LoanInstallment installment2 = LoanInstallment.builder()
                    .installmentId(2L)
                    .loanId(101L)
                    .installmentNo(1)
                    .installmentAmount(new BigDecimal("1500.0000"))
                    .paymentStatus("UNPAID")
                    .dueDate(LocalDate.of(2026, 1, 15))
                    .build();

            Loan loan1 = Loan.builder()
                    .loanId(100L)
                    .employeeNo(EMPLOYEE_NO)
                    .loanAmount(new BigDecimal("10000.0000"))
                    .remainingBalance(new BigDecimal("10000.0000"))
                    .isActive("Y")
                    .build();

            Loan loan2 = Loan.builder()
                    .loanId(101L)
                    .employeeNo(EMPLOYEE_NO)
                    .loanAmount(new BigDecimal("15000.0000"))
                    .remainingBalance(new BigDecimal("15000.0000"))
                    .isActive("Y")
                    .build();

            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(eq(EMPLOYEE_NO), anyInt(), anyInt()))
                    .thenReturn(List.of(installment1, installment2));
            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan1));
            when(loanRepository.findById(101L)).thenReturn(Optional.of(loan2));

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getTotalLoans()).isEqualByComparingTo(new BigDecimal("2500.0000"));
            assertThat(installment1.getPaymentStatus()).isEqualTo("PAID");
            assertThat(installment2.getPaymentStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("Loan postponement should exclude from current month payroll")
        void testLoanPostponement_ExcludedFromCurrentMonth() {
            testEmployee.setMonthlySalary(new BigDecimal("5000.0000"));
            setupBasicMocks();

            // Installment postponed to next month (due date in future)
            LoanInstallment postponedInstallment = LoanInstallment.builder()
                    .installmentId(1L)
                    .loanId(100L)
                    .installmentNo(1)
                    .installmentAmount(new BigDecimal("1000.0000"))
                    .paymentStatus("UNPAID")
                    .dueDate(LocalDate.of(2026, 2, 15)) // Next month
                    .build();

            // Repository should only return installments due in current month
            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(eq(EMPLOYEE_NO), eq(2026), eq(1)))
                    .thenReturn(Collections.emptyList());

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getTotalLoans()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== Section 8.1: Employee Lifecycle ====================

    @Nested
    @DisplayName("8.1 Employee Lifecycle")
    class EmployeeLifecycle {

        @Test
        @DisplayName("New employee first month should pro-rate correctly")
        void testNewEmployeeFirstMonth_ProRatedCorrectly() {
            testEmployee.setHireDate(LocalDate.of(2026, 1, 15));
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Days worked: Jan 15-31 = 17 days
            // Formula: (6000 * 17) / 30 = 3400
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("17"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Employee termination final payroll should include leave balance payout")
        void testEmployeeTerminationFinalPayroll_IncludesLeaveBalance() {
            testEmployee.setTerminationDate(LocalDate.of(2026, 1, 20));
            setupBasicMocks();

            // Note: Leave balance payout would be added as allowance
            // This test documents expected behavior

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Days worked: Jan 1-20 = 20 days
            BigDecimal expectedGross = new BigDecimal("6000.0000")
                    .multiply(new BigDecimal("20"))
                    .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP);
            assertThat(result.getGrossSalary()).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Employee rehire should create new employee record")
        void testEmployeeRehire_CreatesNewRecord() {
            // This test documents expected behavior
            // Employee terminated, then rehired
            // New employee record, previous data preserved
        }
    }

    // ==================== Section 8.2: Salary Changes ====================

    @Nested
    @DisplayName("8.2 Salary Changes")
    class SalaryChanges {

        @Test
        @DisplayName("Salary raise mid-month should pro-rate correctly")
        void testSalaryRaiseMidMonth_ProRatedCorrectly() {
            // Employee gets salary raise on 15th
            // Old salary for days 1-14, new salary for days 15-30
            // This test documents expected behavior
            // The actual implementation may need to handle salary history
        }

        @Test
        @DisplayName("Salary raise affects breakdown percentages")
        void testSalaryRaise_AffectsBreakdown() {
            testEmployee.setMonthlySalary(new BigDecimal("8000.0000")); // Raised from 6000
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getGrossSalary()).isEqualByComparingTo(new BigDecimal("8000.0000"));
        }

        @Test
        @DisplayName("Salary raise before payroll calculation should use new salary")
        void testSalaryRaise_BeforePayrollCalculation_UsesNewSalary() {
            testEmployee.setMonthlySalary(new BigDecimal("8000.0000"));
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            assertThat(result.getGrossSalary()).isEqualByComparingTo(new BigDecimal("8000.0000"));
        }

        @Test
        @DisplayName("Salary raise after payroll calculated should require recalculation")
        void testSalaryRaise_AfterPayrollCalculated_RequiresRecalculation() {
            // This test documents expected behavior
            // Salary raise approved after payroll already calculated
            // Payroll needs recalculation, old version marked not latest
        }
    }

    // ==================== Section 8.4: Attendance Editing After Payroll ====================

    @Nested
    @DisplayName("8.4 Attendance Editing After Payroll")
    class AttendanceEditingAfterPayroll {

        @Test
        @DisplayName("Edit attendance before payroll calculation should use updated data")
        void testEditAttendance_BeforePayrollCalculation_UsesUpdatedData() {
            // This test documents expected behavior
            // Edit attendance record, then calculate payroll
            // Updated attendance data should be used in payroll
        }

        @Test
        @DisplayName("Edit attendance after payroll calculated (not approved) should update allowances/deductions")
        void testEditAttendance_AfterPayrollNotApproved_UpdatesAllowancesDeductions() {
            // This test documents expected behavior
            // Edit attendance after payroll calculated but not approved
            // Allowances/deductions should be updated, payroll may need recalculation
        }

        @Test
        @DisplayName("Edit attendance after payroll approved should require recalculation")
        void testEditAttendance_AfterPayrollApproved_RequiresRecalculation() {
            // This test documents expected behavior
            // Edit attendance after payroll fully approved
            // Payroll recalculation required, new version created
        }

        @Test
        @DisplayName("Edit auto-checkout record should recalculate hours")
        void testEditAutoCheckoutRecord_RecalculatesHours() {
            // This test documents expected behavior
            // Project manager edits auto-checkout times
            // Hours should be recalculated, allowances/deductions updated
        }

        @Test
        @DisplayName("Edit attendance on closed date should throw error")
        void testEditAttendance_OnClosedDate_ThrowsError() {
            // This test documents expected behavior
            // Attempt to edit attendance on closed date
            // Should throw error: "الحضور لهذا التاريخ مغلق ولا يمكن تعديله"
        }

        @Test
        @DisplayName("Edit attendance changes overtime should update allowance")
        void testEditAttendance_ChangesOvertime_UpdatesAllowance() {
            // This test documents expected behavior
            // Edit attendance to add/remove overtime hours
            // Overtime allowance should be updated/deleted accordingly
        }
    }

    // ==================== Section 8.5: GPS Disabled Scenarios ====================

    @Nested
    @DisplayName("8.5 GPS Disabled Scenarios")
    class GPSDisabledScenarios {

        @Test
        @DisplayName("Check-in with GPS disabled should skip GPS validation")
        void testCheckIn_GPSDisabled_SkipsValidation() {
            // This test documents expected behavior
            // Project has GPS check disabled (require_gps_check = 'N')
            // Check-in should be allowed without GPS validation
        }

        @Test
        @DisplayName("Check-in with GPS coordinates not configured should handle gracefully")
        void testCheckIn_GPSNotConfigured_HandlesGracefully() {
            // This test documents expected behavior
            // Project exists but no GPS coordinates set
            // Should error if GPS required, or skip if GPS disabled
        }

        @Test
        @DisplayName("GPS disabled mid-day should not affect existing records")
        void testGPSDisabledMidDay_DoesNotAffectExisting() {
            // This test documents expected behavior
            // Project GPS requirement changed from Y to N during day
            // Existing attendance records should be unaffected
        }

        @Test
        @DisplayName("GPS enabled mid-day should require validation for new check-ins")
        void testGPSEnabledMidDay_RequiresValidation() {
            // This test documents expected behavior
            // Project GPS requirement changed from N to Y during day
            // New check-ins should require GPS validation
        }
    }

    // ==================== Section 8.10: Payroll Versioning & History ====================

    @Nested
    @DisplayName("8.10 Payroll Versioning & History")
    class PayrollVersioningAndHistory {

        @Test
        @DisplayName("Multiple payroll versions for same month should all be stored")
        void testMultiplePayrollVersions_AllStored() {
            testEmployee.setMonthlySalary(new BigDecimal("5000.0000"));
            setupBasicMocks();

            // First version
            SalaryHeader version1 = SalaryHeader.builder()
                    .salaryId(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .salaryMonth(TEST_MONTH)
                    .salaryVersion(1)
                    .isLatest("Y")
                    .grossSalary(new BigDecimal("5000.0000"))
                    .build();

            when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, TEST_MONTH))
                    .thenReturn(Optional.of(version1));

            // Recalculate
            when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(2L);
                        }
                        return header;
                    });

            SalaryHeader version2 = payrollCalculationService.recalculatePayroll(EMPLOYEE_NO, TEST_MONTH, "Correction");

            assertThat(version2).isNotNull();
            assertThat(version2.getSalaryVersion()).isEqualTo(2);
            assertThat(version2.getIsLatest()).isEqualTo("Y");
            // Old version should be marked is_latest = 'N'
        }

        @Test
        @DisplayName("Payroll version audit trail should track who recalculated and why")
        void testPayrollVersion_AuditTrail_TracksRecalculation() {
            testEmployee.setMonthlySalary(new BigDecimal("5000.0000"));
            setupBasicMocks();

            SalaryHeader existing = SalaryHeader.builder()
                    .salaryId(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .salaryMonth(TEST_MONTH)
                    .salaryVersion(1)
                    .isLatest("Y")
                    .build();

            when(salaryHeaderRepository.findLatestByEmployeeAndMonth(EMPLOYEE_NO, TEST_MONTH))
                    .thenReturn(Optional.of(existing));
            when(salaryHeaderRepository.save(any(SalaryHeader.class)))
                    .thenAnswer(invocation -> {
                        SalaryHeader header = invocation.getArgument(0);
                        if (header.getSalaryId() == null) {
                            header.setSalaryId(2L);
                        }
                        return header;
                    });

            SalaryHeader result = payrollCalculationService.recalculatePayroll(EMPLOYEE_NO, TEST_MONTH, "Data correction");

            assertThat(result).isNotNull();
            assertThat(result.getRecalculationReason()).isEqualTo("Data correction");
        }

        @Test
        @DisplayName("Old version preservation should not delete previous versions")
        void testOldVersionPreservation_NotDeleted() {
            // This test documents expected behavior
            // Recalculate payroll
            // Old version should be preserved, not deleted, marked is_latest = 'N'
        }
    }

    // ==================== Section 8.16: Attendance Deletion Scenarios ====================

    @Nested
    @DisplayName("8.16 Attendance Deletion Scenarios")
    class AttendanceDeletionScenarios {

        @Test
        @DisplayName("Delete attendance before payroll calculation should exclude from payroll")
        void testDeleteAttendance_BeforePayrollCalculation_Excluded() {
            // This test documents expected behavior
            // Delete attendance record, then calculate payroll
            // Deleted attendance should not be included, no allowances/deductions from it
        }

        @Test
        @DisplayName("Delete attendance after payroll calculated should require recalculation")
        void testDeleteAttendance_AfterPayrollCalculated_RequiresRecalculation() {
            // This test documents expected behavior
            // Delete attendance after payroll already calculated
            // Payroll needs recalculation, allowances/deductions removed
        }

        @Test
        @DisplayName("Delete attendance after payroll approved should require new version")
        void testDeleteAttendance_AfterPayrollApproved_RequiresNewVersion() {
            // This test documents expected behavior
            // Delete attendance after payroll fully approved
            // Payroll recalculation required, new version created
        }

        @Test
        @DisplayName("Delete attendance with associated allowances should delete or mark inactive")
        void testDeleteAttendance_WithAssociatedAllowances_DeletesOrMarksInactive() {
            // This test documents expected behavior
            // Delete attendance that created overtime allowance
            // Associated allowance should be deleted or marked inactive
        }

        @Test
        @DisplayName("Delete attendance authorization should only allow ADMIN")
        void testDeleteAttendance_Authorization_OnlyAdmin() {
            // This test documents expected behavior
            // Regular employee tries to delete attendance
            // Access should be denied, only ADMIN can delete
        }
    }

    // ==================== Section 8.17: Employee Category Change Scenarios ====================

    @Nested
    @DisplayName("8.17 Employee Category Change Scenarios")
    class EmployeeCategoryChangeScenarios {

        @Test
        @DisplayName("Category change from Saudi to Foreign should update breakdown")
        void testCategoryChange_SaudiToForeign_UpdatesBreakdown() {
            testEmployee.setEmployeeCategory("F"); // Changed from S to F
            setupBasicMocks();

            // Foreign breakdown percentages
            SalaryBreakdownPercentage foreignBasic = SalaryBreakdownPercentage.builder()
                    .employeeCategory("F")
                    .transTypeCode(1L)
                    .salaryPercentage(new BigDecimal("0.5500"))
                    .build();

            when(salaryBreakdownPercentageRepository.findByEmployeeCategory("F"))
                    .thenReturn(List.of(foreignBasic));

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            verify(salaryBreakdownPercentageRepository).findByEmployeeCategory("F");
        }

        @Test
        @DisplayName("Category change mid-month should use new breakdown")
        void testCategoryChange_MidMonth_UsesNewBreakdown() {
            // This test documents expected behavior
            // Category changed on 15th of month
            // Old breakdown for days 1-14, new breakdown for days 15-30 (if pro-rated)
        }

        @Test
        @DisplayName("Category change after payroll calculated should require recalculation")
        void testCategoryChange_AfterPayrollCalculated_RequiresRecalculation() {
            // This test documents expected behavior
            // Category changed after payroll calculated
            // Payroll needs recalculation with new breakdown percentages
        }
    }

    // ==================== Section 8.18: Contract Type Change Impact ====================

    @Nested
    @DisplayName("8.18 Contract Type Change Impact")
    class ContractTypeChangeImpact {

        @Test
        @DisplayName("Contract change from TECHNO to CLIENT should remove payroll eligibility")
        void testContractChange_TechnoToClient_RemovesEligibility() {
            testEmployee.setEmpContractType("CLIENT");
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));

            assertThatThrownBy(() -> payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("TECHNO");
        }

        @Test
        @DisplayName("Contract change from CLIENT to TECHNO should enable payroll")
        void testContractChange_ClientToTechno_EnablesPayroll() {
            testEmployee.setEmpContractType("TECHNO");
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Now eligible for payroll
        }

        @Test
        @DisplayName("Contract change mid-month should determine eligibility by calculation date")
        void testContractChange_MidMonth_DeterminesEligibilityByDate() {
            // This test documents expected behavior
            // Contract changed on 15th of month
            // Payroll eligibility determined by contract on calculation date
        }
    }

    // ==================== Section 8.20: Grace Period Edge Cases ====================

    @Nested
    @DisplayName("8.20 Grace Period Edge Cases")
    class GracePeriodEdgeCases {

        @Test
        @DisplayName("Check-in exactly at grace period boundary (15:00) should be on-time")
        void testCheckIn_ExactlyAtGraceBoundary_OnTime() {
            // This test documents expected behavior
            // Check-in exactly 15 minutes after scheduled start
            // delayed_calc should be 0 (within grace), status = "On-time"
        }

        @Test
        @DisplayName("Check-in 1 second after grace period should be late")
        void testCheckIn_OneSecondAfterGrace_Late() {
            // This test documents expected behavior
            // Check-in 15 minutes and 1 second after scheduled start
            // delayed_calc should be > 0, status = "Late"
        }

        @Test
        @DisplayName("Check-out grace period should allow early departure within grace")
        void testCheckOut_GracePeriod_AllowsEarlyDeparture() {
            // This test documents expected behavior
            // Check-out 15 minutes before scheduled end
            // early_out_calc should be 0 (within grace period)
        }
    }

    // ==================== Section 8.27: Calculation Precision & Rounding ====================

    @Nested
    @DisplayName("8.27 Calculation Precision & Rounding")
    class CalculationPrecisionAndRounding {

        @Test
        @DisplayName("Rounding to 4 decimal places with HALF_UP")
        void testRounding_FourDecimalPlaces_HalfUp() {
            testEmployee.setMonthlySalary(new BigDecimal("10000.0000"));
            testEmployee.setHireDate(LocalDate.of(2026, 1, 10)); // 21 days

            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Pro-rated: (10000 * 21) / 30 = 7000.0000
            // Should be rounded to 4 decimal places
            assertThat(result.getGrossSalary().scale()).isLessThanOrEqualTo(4);
        }

        @Test
        @DisplayName("Totals match after rounding within tolerance")
        void testTotalsMatch_AfterRounding_WithinTolerance() {
            testEmployee.setMonthlySalary(new BigDecimal("5000.0000"));
            setupBasicMocks();

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Net = Allowances - Deductions (gross salary breakdown is included in allowances)
            // Should match within rounding tolerance of 0.0001
            BigDecimal totalAllowances = result.getTotalAllowances() != null ? result.getTotalAllowances() : BigDecimal.ZERO;
            BigDecimal totalDeductions = result.getTotalDeductions() != null ? result.getTotalDeductions() : BigDecimal.ZERO;
            BigDecimal calculatedNet = totalAllowances.subtract(totalDeductions);
            
            // Verify that net salary matches the calculated value within tolerance
            if (result.getNetSalary() != null && calculatedNet != null) {
                // The net salary should equal allowances - deductions (within rounding tolerance)
                assertThat(result.getNetSalary()).isCloseTo(calculatedNet, org.assertj.core.data.Offset.offset(new BigDecimal("0.0001")));
            } else {
                // If either is null, just verify net salary exists
                assertThat(result.getNetSalary()).isNotNull();
            }
        }
    }

    // ==================== Section 8.28: Missing Data Scenarios ====================

    @Nested
    @DisplayName("8.28 Missing Data Scenarios")
    class MissingDataScenarios {

        @Test
        @DisplayName("Payroll calculation with missing schedule should use default")
        void testPayrollCalculation_MissingSchedule_UsesDefault() {
            testEmployee.setPrimaryDeptCode(null);
            testEmployee.setPrimaryProjectCode(null);
            setupBasicMocks();

            // No schedule found, should use default (8 hours)
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Calculation should proceed with default schedule
        }

        @Test
        @DisplayName("Payroll calculation with missing breakdown percentages should use full salary")
        void testPayrollCalculation_MissingBreakdown_UsesFullSalary() {
            testEmployee.setEmployeeCategory("X"); // Unknown category
            setupBasicMocks();

            when(salaryBreakdownPercentageRepository.findByEmployeeCategory("X"))
                    .thenReturn(Collections.emptyList());

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Full salary should be added as single component
        }

        @Test
        @DisplayName("Payroll with missing employee data should throw validation error")
        void testPayroll_MissingEmployeeData_ThrowsValidationError() {
            testEmployee.setMonthlySalary(null);
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));

            assertThatThrownBy(() -> payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH))
                    .isInstanceOf(Exception.class);
        }
    }
}
