package com.techno.backend.integration;

import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import com.techno.backend.service.ApprovalWorkflowService;
import com.techno.backend.service.PayrollCalculationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Payroll system.
 * Tests full calculation flow with real database (H2).
 *
 * NOTE: Disabled due to Java 25 incompatibility with Mockito/ByteBuddy.
 * Re-enable when using Java 17/21 or when Mockito fully supports Java 25.
 *
 * @author Techno HR System - Testing Suite
 */

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Payroll Integration Tests")
class PayrollIntegrationTest {

        @Autowired
        private PayrollCalculationService payrollCalculationService;

        @Autowired
        private EmployeeRepository employeeRepository;

        @Autowired
        private SalaryHeaderRepository salaryHeaderRepository;

        @Autowired
        private SalaryDetailRepository salaryDetailRepository;

        @Autowired
        private LoanRepository loanRepository;

        @Autowired
        private LoanInstallmentRepository loanInstallmentRepository;

        @Autowired
        private ContractTypeRepository contractTypeRepository;

        @Autowired
        private DepartmentRepository departmentRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private TransactionTypeRepository transactionTypeRepository;

        @MockBean
        private ApprovalWorkflowService approvalWorkflowService;

        @MockBean
        private ApplicationEventPublisher eventPublisher;

        private Employee testEmployee;
        private final String TEST_MONTH = "2026-02";
        private Long validDeptCode;
        private Long validProjectCode;

        @BeforeEach
        void setUp() {
                // Mock approval workflow
                when(approvalWorkflowService.initializeApproval(anyString(), anyLong(), any(), any()))
                                .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                                                .transStatus("N")
                                                .nextApproval(1L)
                                                .nextAppLevel(1)
                                                .build());

                // Clean test data
                salaryDetailRepository.deleteAll();
                salaryHeaderRepository.deleteAll();
                loanInstallmentRepository.deleteAll();
                loanRepository.deleteAll();
                employeeRepository.deleteAll();
                contractTypeRepository.deleteAll();
                departmentRepository.deleteAll();
                projectRepository.deleteAll();
                transactionTypeRepository.deleteAll();

                // Seed Validation Data
                ContractType ct = ContractType.builder()
                                .contractTypeCode("TECHNO")
                                .typeName("Techno Contract")
                                .isActive('Y')
                                .allowSelfService('Y')
                                .calculateSalary('Y')
                                .build();
                contractTypeRepository.save(ct);

                Department dept = Department.builder()
                                .deptCode(1L) // ID is usually generated, but if we can't set it, we rely on returned
                                              // object.
                                // But Dept uses Identity generation, so we might need to rely on saving and
                                // using the ID.
                                // However, integration test helper hardcodes 1L.
                                // H2 usually resets ID if we delete all? No.
                                // We should use the saved entity ID in helper.
                                // But helper uses hardcoded 1L.
                                .deptName("IT Department")
                                .isActive('Y')
                                .build();
                // For H2 with IDENTITY, we can't force ID 1L unless we use SQL.
                // But let's try just saving.
                dept = departmentRepository.save(dept);
                this.validDeptCode = dept.getDeptCode();

                Project proj = Project.builder()
                                // .projectCode(100L) // Ignored by Identity
                                .projectName("Main Project")
                                .startDate(LocalDate.of(2025, 1, 1))
                                .endDate(LocalDate.of(2030, 12, 31))
                                .totalProjectAmount(new BigDecimal("1000000.0000"))
                                .requireGpsCheck("N")
                                .build();
                proj = projectRepository.save(proj);
                this.validProjectCode = proj.getProjectCode();

                // Seed Basic Salary Type (Critical for breakdown)
                TransactionType basicSalary = TransactionType.builder()
                                .typeCode(1L)
                                .typeName("Basic Salary")
                                .allowanceDeduction("A")
                                .isSystemGenerated("Y")
                                .isActive("Y")
                                .build();
                transactionTypeRepository.save(basicSalary);

                TransactionType loanInstallment = TransactionType.builder()
                                .typeCode(30L)
                                .typeName("Loan Installment")
                                .allowanceDeduction("D")
                                .isSystemGenerated("Y")
                                .isActive("Y")
                                .build();
                transactionTypeRepository.save(loanInstallment);

                // Update helper to use these IDs if necessary?
                // The TestHelper 'createAndSaveEmployee' hardcodes 1L and 100L.
                // We should update it to use dept.getDeptCode() and proj.getProjectCode()
                // locally?
                // Or use SQL to force IDs.
                // Let's rely on saving. If H2 starts at 1, we are good.
        }

        /**
         * Helper to create and save a test employee
         */
        private Employee createAndSaveEmployee(Long employeeNo, String name, BigDecimal salary) {
                Employee employee = new Employee();
                employee.setEmployeeNo(employeeNo);
                employee.setEmployeeName(name);
                employee.setMonthlySalary(salary);
                employee.setEmpContractType("TECHNO");
                employee.setEmploymentStatus("ACTIVE");
                employee.setHireDate(LocalDate.of(2020, 1, 1));
                employee.setHireDate(LocalDate.of(2020, 1, 1));
                // Category set later
                // employee.setEmployeeCategory("SAUDI");
                employee.setPrimaryDeptCode(this.validDeptCode);
                employee.setPrimaryProjectCode(this.validProjectCode);
                employee.setNationality("Saudi");
                employee.setNationalId("100000000" + employeeNo);
                employee.setEmployeeCategory("S");

                // Check if exists first
                return employeeRepository.findById(employeeNo)
                                .orElseGet(() -> employeeRepository.save(employee));
        }

        /**
         * Helper to create and save a loan with installments
         */
        private Loan createAndSaveLoan(Long employeeNo, BigDecimal amount, int installments) {
                Loan loan = Loan.builder()
                                .employeeNo(employeeNo)
                                .loanAmount(amount)
                                .noOfInstallments(installments)
                                .installmentAmount(amount.divide(new BigDecimal(installments), 4,
                                                java.math.RoundingMode.HALF_UP))
                                .remainingBalance(amount)
                                .firstInstallmentDate(LocalDate.of(2026, 2, 1))
                                .transStatus("A")
                                .isActive("Y")
                                .requestDate(LocalDate.of(2026, 1, 1))
                                .build();
                loan = loanRepository.save(loan);

                // Create installments
                for (int i = 1; i <= installments; i++) {
                        LoanInstallment installment = LoanInstallment.builder()
                                        .loanId(loan.getLoanId())
                                        .installmentNo(i)
                                        .installmentAmount(loan.getInstallmentAmount())
                                        .dueDate(LocalDate.of(2026, 1 + i, 15))
                                        .paymentStatus("UNPAID")
                                        .build();
                        loanInstallmentRepository.save(installment);
                }

                return loan;
        }

        // ==================== INTEGRATION TEST: FULL PAYROLL CALCULATION
        // ====================

        @Test
        @Order(1)
        @DisplayName("Full payroll calculation flow with database")
        @Transactional
        void testFullPayrollCalculation_EndToEnd() {
                // Arrange
                testEmployee = createAndSaveEmployee(1001L, "Integration Test Employee", new BigDecimal("6000.0000"));

                // Act
                SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(
                                testEmployee.getEmployeeNo(), TEST_MONTH);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getSalaryId()).isNotNull();
                assertThat(result.getEmployeeNo()).isEqualTo(testEmployee.getEmployeeNo());
                assertThat(result.getSalaryMonth()).isEqualTo(TEST_MONTH);
                assertThat(result.getGrossSalary()).isEqualByComparingTo(new BigDecimal("6000.0000"));
                assertThat(result.getNetSalary()).isEqualByComparingTo(new BigDecimal("6000.0000"));
                assertThat(result.getTransStatus()).isEqualTo("N"); // Pending approval

                // Verify saved to database
                SalaryHeader fromDb = salaryHeaderRepository.findById(result.getSalaryId()).orElse(null);
                assertThat(fromDb).isNotNull();
                assertThat(fromDb.getNetSalary()).isEqualByComparingTo(new BigDecimal("6000.0000"));
        }

        // ==================== INTEGRATION TEST: LOAN-PAYROLL INTEGRATION
        // ====================

        @Test
        @Order(2)
        @DisplayName("Loan installment deduction during payroll calculation")
        @Transactional
        void testLoanPayrollIntegration_InstallmentDeducted() {
                // Arrange
                testEmployee = createAndSaveEmployee(1002L, "Loan Test Employee", new BigDecimal("8000.0000"));
                Loan loan = createAndSaveLoan(testEmployee.getEmployeeNo(), new BigDecimal("10000.0000"), 10);

                // Get first installment for Feb 2026
                List<LoanInstallment> installments = loanInstallmentRepository
                                .findByLoanIdOrderByInstallmentNoAsc(loan.getLoanId());
                // Update first installment to Feb
                LoanInstallment febInstallment = installments.get(0);
                febInstallment.setDueDate(LocalDate.of(2026, 2, 15));
                loanInstallmentRepository.save(febInstallment);

                // Act
                SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(
                                testEmployee.getEmployeeNo(), TEST_MONTH);

                // Assert - Verify deduction
                assertThat(result.getGrossSalary()).isEqualByComparingTo(new BigDecimal("8000.0000"));
                assertThat(result.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("1000.0000"));
                assertThat(result.getNetSalary()).isEqualByComparingTo(new BigDecimal("7000.0000"));

                // Verify installment marked as PAID
                LoanInstallment paidInstallment = loanInstallmentRepository.findById(febInstallment.getInstallmentId())
                                .orElse(null);
                assertThat(paidInstallment).isNotNull();
                assertThat(paidInstallment.getPaymentStatus()).isEqualTo("PAID");

                // Verify loan balance reduced
                Loan updatedLoan = loanRepository.findById(loan.getLoanId()).orElse(null);
                assertThat(updatedLoan).isNotNull();
                assertThat(updatedLoan.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("9000.0000"));
        }

        // ==================== INTEGRATION TEST: PRORATING ====================

        @Test
        @Order(3)
        @DisplayName("Mid-month hire prorating with database")
        @Transactional
        void testProrating_MidMonthHire_SavesCorrectly() {
                // Arrange - Employee hired on Feb 15
                final Employee newHireToSave = new Employee();
                newHireToSave.setEmployeeNo(1003L);
                newHireToSave.setEmployeeName("New Hire Employee");
                newHireToSave.setMonthlySalary(new BigDecimal("6000.0000"));
                newHireToSave.setEmpContractType("TECHNO");
                newHireToSave.setEmploymentStatus("ACTIVE");
                newHireToSave.setHireDate(LocalDate.of(2026, 2, 15)); // Mid-month
                newHireToSave.setPrimaryDeptCode(this.validDeptCode);
                newHireToSave.setPrimaryProjectCode(this.validProjectCode);
                newHireToSave.setNationality("Saudi");
                newHireToSave.setNationalId("1000001003");
                newHireToSave.setEmployeeCategory("S");

                Employee newHire = employeeRepository.findById(1003L)
                                .orElseGet(() -> employeeRepository.save(newHireToSave));

                // Act
                SalaryHeader result = payrollCalculationService.calculatePayrollForEmployee(
                                newHire.getEmployeeNo(), TEST_MONTH);

                // Assert - Prorated for ~14 days (Feb 15-28 in Feb 2026)
                // Calculation depends on actual month days but formula is (salary * days) / 30
                assertThat(result.getGrossSalary()).isLessThan(new BigDecimal("6000.0000"));
                assertThat(result.getGrossSalary()).isGreaterThan(new BigDecimal("0.0000"));
        }

        // ==================== INTEGRATION TEST: DUPLICATE CALCULATION PREVENTION
        // ====================

        @Test
        @Order(4)
        @DisplayName("Duplicate calculation should throw exception")
        @Transactional
        void testDuplicateCalculation_ThrowsException() {
                // Arrange
                testEmployee = createAndSaveEmployee(1004L, "Duplicate Test Employee", new BigDecimal("5000.0000"));

                // First calculation
                payrollCalculationService.calculatePayrollForEmployee(testEmployee.getEmployeeNo(), TEST_MONTH);

                // Act & Assert - Second calculation should fail
                assertThatThrownBy(
                                () -> payrollCalculationService
                                                .calculatePayrollForEmployee(testEmployee.getEmployeeNo(), TEST_MONTH))
                                .isInstanceOf(RuntimeException.class);
        }

        // ==================== INTEGRATION TEST: RECALCULATION ====================

        @Test
        @Order(5)
        @DisplayName("Recalculation creates new version")
        @Transactional
        void testRecalculation_CreatesNewVersion() {
                // Arrange
                testEmployee = createAndSaveEmployee(1005L, "Recalc Test Employee", new BigDecimal("5000.0000"));

                // First calculation
                SalaryHeader v1 = payrollCalculationService.calculatePayrollForEmployee(
                                testEmployee.getEmployeeNo(), TEST_MONTH);

                // Act - Recalculate
                SalaryHeader v2 = payrollCalculationService.recalculatePayroll(
                                testEmployee.getEmployeeNo(), TEST_MONTH, "Testing recalculation");

                // Assert
                assertThat(v2.getSalaryVersion()).isEqualTo(2);
                assertThat(v2.getIsLatest()).isEqualTo("Y");
                assertThat(v2.getRecalculationReason()).isEqualTo("Testing recalculation");

                // Verify v1 marked as not latest
                SalaryHeader v1Updated = salaryHeaderRepository.findById(v1.getSalaryId()).orElse(null);
                assertThat(v1Updated).isNotNull();
                assertThat(v1Updated.getIsLatest()).isEqualTo("N");
        }
}
