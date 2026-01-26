package com.techno.backend.service;

import com.techno.backend.entity.*;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Deep Testing")
class LoanServiceTest {

        @Mock
        private LoanRepository loanRepository;
        @Mock
        private LoanInstallmentRepository installmentRepository;
        @Mock
        private LoanPostponementRequestRepository postponementRepository;
        @Mock
        private EmployeeRepository employeeRepository;
        @Mock
        private ApprovalWorkflowService approvalWorkflowService;
        @Mock
        private ApplicationEventPublisher eventPublisher;

        @InjectMocks
        private LoanService loanService;

        private Employee testEmployee;
        private final Long EMPLOYEE_NO = 1026L;

        @BeforeEach
        void setUp() {
                testEmployee = new Employee();
                testEmployee.setEmployeeNo(EMPLOYEE_NO);
                testEmployee.setEmployeeName("Ahmad Loaner");
                testEmployee.setEmploymentStatus("ACTIVE");
                testEmployee.setMonthlySalary(new BigDecimal("10000.0000"));
                testEmployee.setPrimaryDeptCode(10L);
                testEmployee.setPrimaryProjectCode(100L);
        }

        @Nested
        @DisplayName("Loan Submission Tests")
        class SubmissionTests {

                @Test
                @DisplayName("Submit Valid Loan - Success")
                void testSubmitLoan_Success() {
                        BigDecimal amount = new BigDecimal("12000.0000");
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(loanRepository.findActiveLoansByEmployee(EMPLOYEE_NO)).thenReturn(Collections.emptyList());
                        when(approvalWorkflowService.initializeApproval(any(), any(), any(), any()))
                                        .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                                                        .transStatus("N").nextApproval(1001L).nextAppLevel(1).build());
                        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        Loan result = loanService.submitLoanRequest(EMPLOYEE_NO, amount, 12,
                                        LocalDate.now().plusMonths(2));

                        assertThat(result).isNotNull();
                        assertThat(result.getLoanAmount()).isEqualByComparingTo(amount);
                        verify(loanRepository).save(any());
                }

                @Test
                @DisplayName("Submit Loan - Inactive employee")
                void testSubmitLoan_InactiveEmployee() {
                        testEmployee.setEmploymentStatus("TERMINATED");
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        assertThatThrownBy(() -> loanService.submitLoanRequest(EMPLOYEE_NO, new BigDecimal("1000"), 10,
                                        LocalDate.now().plusMonths(2)))
                                        .isInstanceOf(BadRequestException.class);
                }
        }

        @Nested
        @DisplayName("Approval Workflow Tests")
        class ApprovalTests {

                @Test
                @DisplayName("Approve Final - Generates Schedule")
                @SuppressWarnings("unchecked")
                void testApproveFinal() {
                        Loan loan = createPendingLoan(1L, EMPLOYEE_NO, new BigDecimal("12000"), 12);
                        loan.setNextAppLevel(2);
                        loan.setNextApproval(1002L);

                        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(approvalWorkflowService.canApprove(any(), any(), any(), any())).thenReturn(true);
                        when(approvalWorkflowService.moveToNextLevel(any(), any(), any(), any(), any()))
                                        .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder().transStatus("A")
                                                        .build());
                        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        loanService.approveLoan(1L, 1002L);

                        assertThat(loan.getTransStatus()).isEqualTo("A");

                        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
                        verify(installmentRepository).saveAll(captor.capture());
                        assertThat(captor.getValue()).hasSize(12);
                }

                private Loan createPendingLoan(Long id, Long empNo, BigDecimal amount, int installments) {
                        return Loan.builder()
                                        .loanId(id)
                                        .employeeNo(empNo)
                                        .loanAmount(amount)
                                        .noOfInstallments(installments)
                                        .installmentAmount(amount.divide(BigDecimal.valueOf(installments), 4,
                                                        RoundingMode.HALF_UP))
                                        .remainingBalance(amount)
                                        .transStatus("N")
                                        .nextAppLevel(1)
                                        .nextApproval(1001L)
                                        .firstInstallmentDate(LocalDate.now().plusMonths(2))
                                        .isActive("Y")
                                        .build();
                }
        }

        @Nested
        @DisplayName("Financial/Repayment Tests")
        class FinancialTests {

                @Test
                @DisplayName("Rounding - Last installment adjustment")
                @SuppressWarnings("unchecked")
                void testRounding() {
                        BigDecimal amount = new BigDecimal("1000");
                        int installmentsCount = 3;
                        // 1000 / 3 = 333.3333. Total = 333.3333 * 2 + 333.3334 = 1000.

                        Loan loan = Loan.builder()
                                        .loanId(1L)
                                        .employeeNo(EMPLOYEE_NO)
                                        .loanAmount(amount)
                                        .noOfInstallments(installmentsCount)
                                        .installmentAmount(new BigDecimal("333.3333"))
                                        .firstInstallmentDate(LocalDate.of(2026, 1, 1))
                                        .transStatus("N")
                                        .nextAppLevel(1)
                                        .nextApproval(1001L)
                                        .build();

                        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(approvalWorkflowService.canApprove(any(), any(), any(), any())).thenReturn(true);
                        when(approvalWorkflowService.moveToNextLevel(any(), any(), any(), any(), any()))
                                        .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder().transStatus("A")
                                                        .build());
                        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        loanService.approveLoan(1L, 1002L);

                        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
                        verify(installmentRepository).saveAll(captor.capture());

                        List<LoanInstallment> installments = captor.getValue();
                        assertThat(installments).hasSize(3);
                        assertThat(installments.get(2).getInstallmentAmount()).isEqualByComparingTo("333.3334");

                        BigDecimal sum = installments.stream().map(LoanInstallment::getInstallmentAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        assertThat(sum).isEqualByComparingTo("1000");
                }

                @Test
                @DisplayName("Deduct Payment")
                void testDeductPayment() {
                        Loan loan = Loan.builder()
                                        .loanId(1L)
                                        .employeeNo(EMPLOYEE_NO)
                                        .remainingBalance(new BigDecimal("5000"))
                                        .isActive("Y")
                                        .build();
                        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        loanService.deductPayment(1L, new BigDecimal("1000"));

                        assertThat(loan.getRemainingBalance()).isEqualByComparingTo("4000");
                }
        }

        @Nested
        @DisplayName("Postponement Tests")
        class PostponementTests {

                @Test
                @DisplayName("Submit Postponement")
                void testSubmitPostponement() {
                        Loan loan = Loan.builder()
                                        .loanId(1L)
                                        .employeeNo(EMPLOYEE_NO)
                                        .loanAmount(new BigDecimal("10000.0000"))
                                        .isActive("Y")
                                        .build();
                        LoanInstallment inst = LoanInstallment.builder()
                                        .installmentId(10L)
                                        .loanId(1L)
                                        .paymentStatus("UNPAID")
                                        .dueDate(LocalDate.now())
                                        .build();
                        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                        when(installmentRepository.findById(10L)).thenReturn(Optional.of(inst));
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(approvalWorkflowService.initializeApproval(any(), any(), any(), any()))
                                        .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                                                        .transStatus("N")
                                                        .nextApproval(1001L)
                                                        .nextAppLevel(1)
                                                        .build());
                        when(postponementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        LoanPostponementRequest req = loanService.submitPostponementRequest(1L, 10L,
                                        LocalDate.now().plusMonths(3), "Reason");

                        assertThat(req.getTransStatus()).isEqualTo("N");
                        verify(postponementRepository).save(any());
                }

                @Test
                @DisplayName("Approve Postponement")
                void testApprovePostponement() {
                        LocalDate currentDueDate = LocalDate.of(2026, 4, 1);
                        LocalDate newDueDate = LocalDate.of(2026, 5, 1);
                        LoanPostponementRequest req = LoanPostponementRequest.builder()
                                        .requestId(100L)
                                        .loanId(1L)
                                        .installmentId(10L)
                                        .currentDueDate(currentDueDate)
                                        .newDueDate(newDueDate)
                                        .transStatus("N")
                                        .build();
                        Loan loan = Loan.builder()
                                        .loanId(1L)
                                        .employeeNo(EMPLOYEE_NO)
                                        .loanAmount(new BigDecimal("10000.0000"))
                                        .build();
                        LoanInstallment inst = LoanInstallment.builder()
                                        .installmentId(10L)
                                        .loanId(1L)
                                        .dueDate(currentDueDate)
                                        .build();

                        when(postponementRepository.findById(100L)).thenReturn(Optional.of(req));
                        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                        when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
                        when(approvalWorkflowService.canApprove(any(), any(), any(), any())).thenReturn(true);
                        when(approvalWorkflowService.moveToNextLevel(any(), any(), any(), any(), any()))
                                        .thenReturn(ApprovalWorkflowService.ApprovalInfo.builder()
                                                        .transStatus("A")
                                                        .nextApproval(1003L)
                                                        .nextAppLevel(3)
                                                        .build());
                        when(installmentRepository.findById(10L)).thenReturn(Optional.of(inst));
                        when(postponementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                        loanService.approvePostponement(100L, 1002L);

                        assertThat(inst.getDueDate()).isEqualTo(newDueDate);
                        assertThat(inst.getPaymentStatus()).isEqualTo("POSTPONED");
                }
        }
}
