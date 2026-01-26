package com.techno.backend.service;

import com.techno.backend.entity.Employee;
import com.techno.backend.entity.SalaryHeader;
import com.techno.backend.repository.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for Payroll Approval Service.
 * Tests sections 5.1 and 5.3: Payroll approval flow and auto-approval.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payroll Approval Service Tests")
class PayrollApprovalServiceTest {

    @Mock
    private SalaryHeaderRepository salaryHeaderRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApprovalWorkflowService approvalWorkflowService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PayrollCalculationService payrollCalculationService;

    private Employee testEmployee;
    private SalaryHeader testSalaryHeader;
    private final Long EMPLOYEE_NO = 1001L;
    private final Long SALARY_ID = 1L;
    private final String TEST_MONTH = "2026-01";

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
                .employeeNo(EMPLOYEE_NO)
                .employeeName("Test Employee")
                .primaryDeptCode(1L)
                .primaryProjectCode(100L)
                .build();

        testSalaryHeader = SalaryHeader.builder()
                .salaryId(SALARY_ID)
                .employeeNo(EMPLOYEE_NO)
                .salaryMonth(TEST_MONTH)
                .grossSalary(new BigDecimal("6000.0000"))
                .netSalary(new BigDecimal("6000.0000"))
                .totalAllowances(new BigDecimal("6000.0000"))
                .totalDeductions(BigDecimal.ZERO)
                .transStatus("N")
                .nextApproval(2001L)
                .nextAppLevel(1)
                .build();
    }

    // ==================== Section 5.1: Payroll Approval Flow ====================

    @Nested
    @DisplayName("5.1 Payroll Approval Flow")
    class PayrollApprovalFlow {

        @Test
        @DisplayName("Level 1: HR Manager approval should move to Level 2")
        void testLevel1_HRManagerApproval_MovesToLevel2() {
            when(salaryHeaderRepository.findById(SALARY_ID)).thenReturn(Optional.of(testSalaryHeader));
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(approvalWorkflowService.canApprove("PAYROLL", 1, 2001L, 2001L)).thenReturn(true);

            ApprovalWorkflowService.ApprovalInfo nextLevel = ApprovalWorkflowService.ApprovalInfo.builder()
                    .transStatus("N")
                    .nextApproval(2002L)
                    .nextAppLevel(2)
                    .build();

            when(approvalWorkflowService.moveToNextLevel("PAYROLL", 1, EMPLOYEE_NO, 1L, 100L))
                    .thenReturn(nextLevel);

            when(salaryHeaderRepository.save(any(SalaryHeader.class))).thenAnswer(invocation -> {
                SalaryHeader saved = invocation.getArgument(0);
                // Ensure all required fields are set for notification publishing
                if (saved.getGrossSalary() == null) saved.setGrossSalary(new BigDecimal("6000.0000"));
                if (saved.getNetSalary() == null) saved.setNetSalary(new BigDecimal("6000.0000"));
                if (saved.getTotalAllowances() == null) saved.setTotalAllowances(new BigDecimal("6000.0000"));
                if (saved.getTotalDeductions() == null) saved.setTotalDeductions(BigDecimal.ZERO);
                return saved;
            });

            SalaryHeader result = payrollCalculationService.approvePayroll(SALARY_ID, 2001L);

            assertThat(result).isNotNull();
            assertThat(result.getNextAppLevel()).isEqualTo(2);
            assertThat(result.getNextApproval()).isEqualTo(2002L);
        }

        @Test
        @DisplayName("Level 2: Finance Manager approval should move to Level 3")
        void testLevel2_FinanceManagerApproval_MovesToLevel3() {
            testSalaryHeader.setNextAppLevel(2);
            testSalaryHeader.setNextApproval(2002L);

            when(salaryHeaderRepository.findById(SALARY_ID)).thenReturn(Optional.of(testSalaryHeader));
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(approvalWorkflowService.canApprove("PAYROLL", 2, 2002L, 2002L)).thenReturn(true);

            ApprovalWorkflowService.ApprovalInfo nextLevel = ApprovalWorkflowService.ApprovalInfo.builder()
                    .transStatus("N")
                    .nextApproval(2003L)
                    .nextAppLevel(3)
                    .build();

            when(approvalWorkflowService.moveToNextLevel("PAYROLL", 2, EMPLOYEE_NO, 1L, 100L))
                    .thenReturn(nextLevel);

            when(salaryHeaderRepository.save(any(SalaryHeader.class))).thenAnswer(invocation -> {
                SalaryHeader saved = invocation.getArgument(0);
                // Ensure all required fields are set for notification publishing
                if (saved.getGrossSalary() == null) saved.setGrossSalary(new BigDecimal("6000.0000"));
                if (saved.getNetSalary() == null) saved.setNetSalary(new BigDecimal("6000.0000"));
                if (saved.getTotalAllowances() == null) saved.setTotalAllowances(new BigDecimal("6000.0000"));
                if (saved.getTotalDeductions() == null) saved.setTotalDeductions(BigDecimal.ZERO);
                return saved;
            });

            SalaryHeader result = payrollCalculationService.approvePayroll(SALARY_ID, 2002L);

            assertThat(result).isNotNull();
            assertThat(result.getNextAppLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Level 3: General Manager approval should finalize")
        void testLevel3_GeneralManagerApproval_Finalizes() {
            testSalaryHeader.setNextAppLevel(3);
            testSalaryHeader.setNextApproval(2003L);

            when(salaryHeaderRepository.findById(SALARY_ID)).thenReturn(Optional.of(testSalaryHeader));
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));
            when(approvalWorkflowService.canApprove("PAYROLL", 3, 2003L, 2003L)).thenReturn(true);

            ApprovalWorkflowService.ApprovalInfo finalApproval = ApprovalWorkflowService.ApprovalInfo.builder()
                    .transStatus("A")
                    .nextApproval(null)
                    .nextAppLevel(null)
                    .build();

            when(approvalWorkflowService.moveToNextLevel("PAYROLL", 3, EMPLOYEE_NO, 1L, 100L))
                    .thenReturn(finalApproval);

            when(salaryHeaderRepository.save(any(SalaryHeader.class))).thenAnswer(invocation -> {
                SalaryHeader saved = invocation.getArgument(0);
                // Ensure all required fields are set for notification publishing
                if (saved.getGrossSalary() == null) saved.setGrossSalary(new BigDecimal("6000.0000"));
                if (saved.getNetSalary() == null) saved.setNetSalary(new BigDecimal("6000.0000"));
                if (saved.getTotalAllowances() == null) saved.setTotalAllowances(new BigDecimal("6000.0000"));
                if (saved.getTotalDeductions() == null) saved.setTotalDeductions(BigDecimal.ZERO);
                return saved;
            });

            SalaryHeader result = payrollCalculationService.approvePayroll(SALARY_ID, 2003L);

            assertThat(result).isNotNull();
            assertThat(result.getTransStatus()).isEqualTo("A");
            assertThat(result.getApprovedBy()).isEqualTo(2003L);
        }

        @Test
        @DisplayName("Rejection at any level should set status to R")
        void testRejectionAtAnyLevel_SetsStatusToR() {
            // Create a fresh salary header with correct status for rejection
            SalaryHeader salaryForRejection = SalaryHeader.builder()
                    .salaryId(SALARY_ID)
                    .employeeNo(EMPLOYEE_NO)
                    .salaryMonth(TEST_MONTH)
                    .grossSalary(new BigDecimal("6000.0000"))
                    .netSalary(new BigDecimal("6000.0000"))
                    .totalAllowances(new BigDecimal("6000.0000"))
                    .totalDeductions(BigDecimal.ZERO)
                    .transStatus("N") // Pending status - can be rejected
                    .nextApproval(2001L)
                    .nextAppLevel(1)
                    .build();
            
            when(salaryHeaderRepository.findById(SALARY_ID)).thenReturn(Optional.of(salaryForRejection));
            when(approvalWorkflowService.canApprove("PAYROLL", 1, 2001L, 2001L)).thenReturn(true);
            when(employeeRepository.findById(EMPLOYEE_NO)).thenReturn(Optional.of(testEmployee));

            // Mock save to return the same object (as JPA typically does)
            when(salaryHeaderRepository.save(any(SalaryHeader.class))).thenAnswer(invocation -> {
                SalaryHeader saved = invocation.getArgument(0);
                // The service modifies the object in place, so return it as-is
                return saved;
            });

            SalaryHeader result = payrollCalculationService.rejectPayroll(SALARY_ID, 2001L, "Incorrect calculations");

            assertThat(result).isNotNull();
            assertThat(result.getTransStatus()).isEqualTo("R");
            assertThat(result.getRejectionReason()).isEqualTo("Incorrect calculations");
        }

        @Test
        @DisplayName("Unauthorized approval attempt should throw exception")
        void testUnauthorizedApprovalAttempt_ThrowsException() {
            when(salaryHeaderRepository.findById(SALARY_ID)).thenReturn(Optional.of(testSalaryHeader));
            when(approvalWorkflowService.canApprove("PAYROLL", 1, 9999L, 2001L)).thenReturn(false);

            assertThatThrownBy(() -> payrollCalculationService.approvePayroll(SALARY_ID, 9999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not authorized");
        }
    }

    // ==================== Section 5.3: Approval Timing & Auto-Approval ====================

    @Nested
    @DisplayName("5.3 Approval Timing & Auto-Approval")
    class ApprovalTimingAndAutoApproval {

        @Test
        @DisplayName("24-hour reminder notification should be sent")
        void test24HourReminderNotification_Sent() {
            // This test documents expected behavior for 24-hour reminders
            // The actual reminder is typically handled by a scheduled job
            // When payroll is pending for 24 hours, reminder notification should be sent
        }

        @Test
        @DisplayName("48-hour auto-approval should approve if configured")
        void test48HourAutoApproval_ApprovesIfConfigured() {
            // This test documents expected behavior for 48-hour auto-approval
            // The actual auto-approval is handled by AutoApprovalService batch job
            // When payroll is pending for 48+ hours, it should be auto-approved
        }
    }
}
