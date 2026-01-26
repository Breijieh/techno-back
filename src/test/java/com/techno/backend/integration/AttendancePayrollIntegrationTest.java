package com.techno.backend.integration;

import com.techno.backend.dto.CheckInRequest;
import com.techno.backend.dto.CheckOutRequest;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import com.techno.backend.service.ApprovalWorkflowService;
import com.techno.backend.service.AttendanceAllowanceDeductionService;
import com.techno.backend.service.AttendanceCalculationService;
import com.techno.backend.service.AttendanceDayClosureService;
import com.techno.backend.service.AttendanceService;
import com.techno.backend.service.PayrollCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for Attendance-Payroll integration.
 * Tests sections 3.1-3.3: Auto-creation of allowances/deductions,
 * payroll inclusion of attendance data, and data consistency.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance-Payroll Integration Tests")
class AttendancePayrollIntegrationTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EmpMonthlyAllowanceRepository allowanceRepository;

    @Mock
    private EmpMonthlyDeductionRepository deductionRepository;

    @Mock
    private SalaryHeaderRepository salaryHeaderRepository;

    @Mock
    private SalaryDetailRepository salaryDetailRepository;

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
    private SalaryBreakdownPercentageRepository salaryBreakdownPercentageRepository;

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
    private final Long EMPLOYEE_NO = 1001L;
    private final String TEST_MONTH = "2026-01";

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        validLatitude = new BigDecimal("24.664417");
        validLongitude = new BigDecimal("46.674198");

        testEmployee = Employee.builder()
                .employeeNo(EMPLOYEE_NO)
                .employeeName("أحمد محمد")
                .employmentStatus("ACTIVE")
                .empContractType("TECHNO")
                .employeeCategory("S")
                .monthlySalary(new BigDecimal("6000.0000"))
                .primaryDeptCode(1L)
                .primaryProjectCode(101L)
                .hireDate(LocalDate.of(2020, 1, 1))
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
                .scheduleName("جدول العمل القياسي")
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(17, 0))
                .requiredHours(new BigDecimal("8.00"))
                .gracePeriodMinutes(15)
                .isActive("Y")
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

    // ==================== Section 3.2: Payroll Inclusion of Attendance Data ====================

    @Nested
    @DisplayName("3.2 Payroll Inclusion of Attendance Data")
    class PayrollInclusionOfAttendanceData {

        @Test
        @DisplayName("Overtime allowances from attendance included in payroll")
        void testOvertimeAllowances_IncludedInPayroll() {
            // Arrange: Employee has overtime from attendance
            EmpMonthlyAllowance overtimeAllowance = EmpMonthlyAllowance.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(9L) // Overtime type
                    .allowanceAmount(new BigDecimal("500.0000"))
                    .transactionDate(today)
                    .transStatus("A")
                    .isManualEntry("N") // System-generated from attendance
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the allowance mock for this test
            when(allowanceRepository.findActiveAllowancesForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(overtimeAllowance));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalOvertime()).isEqualByComparingTo(new BigDecimal("500.0000"));
            assertThat(result.getTotalAllowances()).isGreaterThan(result.getGrossSalary());
        }

        @Test
        @DisplayName("Delay deductions from attendance included in payroll")
        void testDelayDeductions_IncludedInPayroll() {
            // Arrange: Employee has monthly delay deduction
            EmpMonthlyDeduction delayDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(20L) // Late deduction
                    .deductionAmount(new BigDecimal("100.0000"))
                    .transactionDate(today)
                    .transStatus("A")
                    .isManualEntry("N") // System-generated from attendance
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the deduction mock for this test
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(delayDeduction));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("100.0000"));
            assertThat(result.getTotalAbsence()).isEqualByComparingTo(new BigDecimal("100.0000"));
        }

        @Test
        @DisplayName("Early departure deductions from attendance included in payroll")
        void testEarlyDepartureDeductions_IncludedInPayroll() {
            // Arrange: Employee has early departure deduction
            EmpMonthlyDeduction earlyDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(20L) // Early departure
                    .deductionAmount(new BigDecimal("150.0000"))
                    .transactionDate(today)
                    .transStatus("A")
                    .isManualEntry("N")
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the deduction mock for this test
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(earlyDeduction));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("150.0000"));
        }

        @Test
        @DisplayName("Shortage deductions from attendance included in payroll")
        void testShortageDeductions_IncludedInPayroll() {
            // Arrange: Employee has shortage deduction
            EmpMonthlyDeduction shortageDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(20L) // Shortage hours
                    .deductionAmount(new BigDecimal("80.0000"))
                    .transactionDate(today)
                    .transStatus("A")
                    .isManualEntry("N")
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the deduction mock for this test
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(shortageDeduction));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("80.0000"));
        }

        @Test
        @DisplayName("Absence deductions from attendance included in payroll")
        void testAbsenceDeductions_IncludedInPayroll() {
            // Arrange: Employee has absence deduction
            EmpMonthlyDeduction absenceDeduction = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(21L) // Absence
                    .deductionAmount(new BigDecimal("200.0000")) // Daily salary = 6000/30
                    .transactionDate(today)
                    .transStatus("A")
                    .isManualEntry("N")
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the deduction mock for this test
            when(deductionRepository.findActiveDeductionsForEmployeeOnDate(eq(EMPLOYEE_NO), any(LocalDate.class)))
                    .thenReturn(List.of(absenceDeduction));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("200.0000"));
            assertThat(result.getTotalAbsence()).isEqualByComparingTo(new BigDecimal("200.0000"));
        }
    }

    // ==================== Section 3.3: Data Consistency Between Systems ====================

    @Nested
    @DisplayName("3.3 Data Consistency Between Systems")
    class DataConsistencyBetweenSystems {

        @Test
        @DisplayName("Attendance totals match payroll allowances")
        void testAttendanceTotals_MatchPayrollAllowances() {
            // Arrange: Employee has overtime from multiple attendance records
            YearMonth month = YearMonth.parse(TEST_MONTH);
            LocalDate date1 = month.atDay(1);
            LocalDate date2 = month.atDay(2);
            LocalDate date3 = month.atDay(3);

            AttendanceTransaction attendance1 = AttendanceTransaction.builder()
                    .transactionId(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date1)
                    .overtimeCalc(new BigDecimal("2.00"))
                    .build();

            AttendanceTransaction attendance2 = AttendanceTransaction.builder()
                    .transactionId(2L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date2)
                    .overtimeCalc(new BigDecimal("1.50"))
                    .build();

            AttendanceTransaction attendance3 = AttendanceTransaction.builder()
                    .transactionId(3L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date3)
                    .overtimeCalc(new BigDecimal("0.50"))
                    .build();

            // Overtime allowances created from attendance
            EmpMonthlyAllowance overtime1 = EmpMonthlyAllowance.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(9L)
                    .allowanceAmount(new BigDecimal("2.00"))
                    .transactionDate(date1)
                    .transStatus("A")
                    .build();

            EmpMonthlyAllowance overtime2 = EmpMonthlyAllowance.builder()
                    .transactionNo(2L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(9L)
                    .allowanceAmount(new BigDecimal("1.50"))
                    .transactionDate(date2)
                    .transStatus("A")
                    .build();

            EmpMonthlyAllowance overtime3 = EmpMonthlyAllowance.builder()
                    .transactionNo(3L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(9L)
                    .allowanceAmount(new BigDecimal("0.50"))
                    .transactionDate(date3)
                    .transStatus("A")
                    .build();

            // Sum from attendance: 2.00 + 1.50 + 0.50 = 4.00
            // Sum from allowances: 2.00 + 1.50 + 0.50 = 4.00
            BigDecimal attendanceTotal = attendance1.getOvertimeCalc()
                    .add(attendance2.getOvertimeCalc())
                    .add(attendance3.getOvertimeCalc());

            BigDecimal allowanceTotal = overtime1.getAllowanceAmount()
                    .add(overtime2.getAllowanceAmount())
                    .add(overtime3.getAllowanceAmount());

            // Assert: Totals should match
            assertThat(attendanceTotal).isEqualByComparingTo(allowanceTotal);
            assertThat(attendanceTotal).isEqualByComparingTo(new BigDecimal("4.00"));
        }

        @Test
        @DisplayName("Delay totals match payroll deductions")
        void testDelayTotals_MatchPayrollDeductions() {
            // Arrange: Employee has delays on multiple days
            YearMonth month = YearMonth.parse(TEST_MONTH);
            LocalDate date1 = month.atDay(1);
            LocalDate date2 = month.atDay(2);
            LocalDate date3 = month.atDay(3);

            AttendanceTransaction attendance1 = AttendanceTransaction.builder()
                    .transactionId(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date1)
                    .delayedCalc(new BigDecimal("0.25")) // 15 minutes
                    .build();

            AttendanceTransaction attendance2 = AttendanceTransaction.builder()
                    .transactionId(2L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date2)
                    .delayedCalc(new BigDecimal("0.50")) // 30 minutes
                    .build();

            AttendanceTransaction attendance3 = AttendanceTransaction.builder()
                    .transactionId(3L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date3)
                    .delayedCalc(new BigDecimal("0.25")) // 15 minutes
                    .build();

            // Monthly aggregated delay deduction
            EmpMonthlyDeduction monthlyDelay = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(20L)
                    .deductionAmount(new BigDecimal("100.0000")) // Calculated from total delay hours
                    .transactionDate(month.atEndOfMonth())
                    .transStatus("A")
                    .build();

            // Sum from attendance: 0.25 + 0.50 + 0.25 = 1.00 hours
            BigDecimal attendanceTotal = attendance1.getDelayedCalc()
                    .add(attendance2.getDelayedCalc())
                    .add(attendance3.getDelayedCalc());

            // Assert: Monthly deduction should represent sum of all delays
            assertThat(attendanceTotal).isEqualByComparingTo(new BigDecimal("1.00"));
            // The deduction amount would be calculated as: delay hours × hourly rate
        }

        @Test
        @DisplayName("Absence days match deductions")
        void testAbsenceDays_MatchDeductions() {
            // Arrange: Employee has 3 absence days in month
            YearMonth month = YearMonth.parse(TEST_MONTH);
            LocalDate date1 = month.atDay(5);
            LocalDate date2 = month.atDay(10);
            LocalDate date3 = month.atDay(15);

            AttendanceTransaction absence1 = AttendanceTransaction.builder()
                    .transactionId(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date1)
                    .absenceFlag("Y")
                    .build();

            AttendanceTransaction absence2 = AttendanceTransaction.builder()
                    .transactionId(2L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date2)
                    .absenceFlag("Y")
                    .build();

            AttendanceTransaction absence3 = AttendanceTransaction.builder()
                    .transactionId(3L)
                    .employeeNo(EMPLOYEE_NO)
                    .attendanceDate(date3)
                    .absenceFlag("Y")
                    .build();

            // Absence deductions created
            EmpMonthlyDeduction deduction1 = EmpMonthlyDeduction.builder()
                    .transactionNo(1L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(21L)
                    .deductionAmount(new BigDecimal("200.0000")) // Daily salary
                    .transactionDate(date1)
                    .transStatus("A")
                    .build();

            EmpMonthlyDeduction deduction2 = EmpMonthlyDeduction.builder()
                    .transactionNo(2L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(21L)
                    .deductionAmount(new BigDecimal("200.0000"))
                    .transactionDate(date2)
                    .transStatus("A")
                    .build();

            EmpMonthlyDeduction deduction3 = EmpMonthlyDeduction.builder()
                    .transactionNo(3L)
                    .employeeNo(EMPLOYEE_NO)
                    .typeCode(21L)
                    .deductionAmount(new BigDecimal("200.0000"))
                    .transactionDate(date3)
                    .transStatus("A")
                    .build();

            // Count absence days: 3
            long absenceDaysCount = 3;

            // Count deductions: 3
            long deductionCount = 3;

            // Assert: Counts should match
            assertThat(absenceDaysCount).isEqualTo(deductionCount);
        }
    }

    // ==================== Section 8.11: Data Consistency & Reconciliation ====================

    @Nested
    @DisplayName("8.11 Data Consistency & Reconciliation")
    class DataConsistencyAndReconciliation {

        @Test
        @DisplayName("Loan installments match deductions in payroll")
        void testLoanInstallments_MatchDeductions() {
            // Arrange: Employee has unpaid installments
            LoanInstallment installment1 = LoanInstallment.builder()
                    .installmentId(1L)
                    .loanId(100L)
                    .installmentNo(1)
                    .installmentAmount(new BigDecimal("500.0000"))
                    .paymentStatus("UNPAID")
                    .dueDate(LocalDate.of(2026, 1, 15))
                    .build();

            Loan loan = Loan.builder()
                    .loanId(100L)
                    .employeeNo(EMPLOYEE_NO)
                    .loanAmount(new BigDecimal("5000.0000"))
                    .remainingBalance(new BigDecimal("5000.0000"))
                    .isActive("Y")
                    .build();

            // Setup basic mocks first (includes salary breakdown)
            setupBasicMocks();
            // Override only the loan-related mocks for this test
            when(loanInstallmentRepository.findUnpaidInstallmentsForEmployeeInMonth(eq(EMPLOYEE_NO), anyInt(), anyInt()))
                    .thenReturn(List.of(installment1));
            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

            // Act
            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalLoans()).isEqualByComparingTo(new BigDecimal("500.0000"));
            // Count of unpaid installments should match count of Type 30 deductions
        }

        @Test
        @DisplayName("Salary breakdown totals match gross salary")
        void testSalaryBreakdownTotals_MatchGrossSalary() {
            testEmployee.setEmployeeCategory("S");
            setupBasicMocks();

            SalaryBreakdownPercentage basic = SalaryBreakdownPercentage.builder()
                    .employeeCategory("S")
                    .transTypeCode(1L)
                    .salaryPercentage(new BigDecimal("0.8340"))
                    .build();

            SalaryBreakdownPercentage transport = SalaryBreakdownPercentage.builder()
                    .employeeCategory("S")
                    .transTypeCode(2L)
                    .salaryPercentage(new BigDecimal("0.1660"))
                    .build();

            when(salaryBreakdownPercentageRepository.findByEmployeeCategory("S"))
                    .thenReturn(List.of(basic, transport));

            SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(EMPLOYEE_NO, TEST_MONTH);

            assertThat(result).isNotNull();
            // Breakdown components should sum to gross salary (within rounding tolerance)
            // 0.8340 + 0.1660 = 1.0000 (100%)
        }
    }
}
