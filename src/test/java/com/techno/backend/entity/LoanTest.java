package com.techno.backend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Loan entity.
 * Tests payment deduction logic and helper methods.
 *
 * @author Techno HR System - Testing Suite
 */
@DisplayName("Loan Entity Tests")
class LoanTest {

    private Loan loan;

    @BeforeEach
    void setUp() {
        loan = Loan.builder()
                .loanId(1L)
                .employeeNo(100L)
                .loanAmount(new BigDecimal("10000.0000"))
                .noOfInstallments(10)
                .installmentAmount(new BigDecimal("1000.0000"))
                .remainingBalance(new BigDecimal("10000.0000"))
                .transStatus("A")
                .isActive("Y")
                .build();
    }

    // ==================== DEDUCT PAYMENT TESTS ====================

    @Nested
    @DisplayName("deductPayment() Tests")
    class DeductPaymentTests {

        @Test
        @DisplayName("Normal deduction reduces remaining balance")
        void testDeductPayment_Normal_ReducesBalance() {
            // Arrange
            BigDecimal payment = new BigDecimal("1000.0000");

            // Act
            loan.deductPayment(payment);

            // Assert
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("9000.0000"));
            assertThat(loan.getIsActive()).isEqualTo("Y"); // Still active
        }

        @Test
        @DisplayName("Multiple deductions reduce balance correctly")
        void testDeductPayment_Multiple_ReducesCorrectly() {
            // Act
            loan.deductPayment(new BigDecimal("1000.0000"));
            loan.deductPayment(new BigDecimal("1000.0000"));
            loan.deductPayment(new BigDecimal("1000.0000"));

            // Assert
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("7000.0000"));
        }

        @Test
        @DisplayName("Full payment sets balance to zero and marks inactive")
        void testDeductPayment_FullPayment_MarksInactive() {
            // Act - Pay all 10,000
            loan.deductPayment(new BigDecimal("10000.0000"));

            // Assert
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(loan.getIsActive()).isEqualTo("N");
        }

        @Test
        @DisplayName("Overpayment sets balance to zero (not negative)")
        void testDeductPayment_Overpay_BalanceZero() {
            // Act - Pay more than remaining
            loan.deductPayment(new BigDecimal("15000.0000"));

            // Assert
            // Perfection: Balance should never dip below zero
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(loan.getIsActive()).isEqualTo("N");
        }

        @Test
        @DisplayName("Null payment throws exception")
        void testDeductPayment_NullPayment_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> loan.deductPayment(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment amount cannot be null");
        }

        @Test
        @DisplayName("Last installment payment marks loan inactive")
        void testDeductPayment_LastInstallment_MarksInactive() {
            // Arrange - Set to last installment amount
            loan.setRemainingBalance(new BigDecimal("1000.0000"));

            // Act
            loan.deductPayment(new BigDecimal("1000.0000"));

            // Assert
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(loan.getIsActive()).isEqualTo("N");
        }

        @Test
        @DisplayName("Deduction with null remaining balance initializes from loan amount")
        void testDeductPayment_NullBalance_InitializesFromLoanAmount() {
            // Arrange
            loan.setRemainingBalance(null);

            // Act
            loan.deductPayment(new BigDecimal("1000.0000"));

            // Assert
            // Should initialize from loanAmount (10000) then subtract 1000 = 9000
            assertThat(loan.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("9000.0000"));
        }
    }

    // ==================== HELPER METHOD TESTS ====================

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isFullyPaid returns true when balance is zero")
        void testIsFullyPaid_BalanceZero_ReturnsTrue() {
            loan.setRemainingBalance(BigDecimal.ZERO);
            assertThat(loan.isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("isFullyPaid returns false when balance is positive")
        void testIsFullyPaid_BalancePositive_ReturnsFalse() {
            loan.setRemainingBalance(new BigDecimal("5000.0000"));
            assertThat(loan.isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("isFullyPaid returns false when balance is null")
        void testIsFullyPaid_BalanceNull_ReturnsFalse() {
            loan.setRemainingBalance(null);
            assertThat(loan.isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("isActiveLoan returns true when isActive = Y")
        void testIsActiveLoan_FlagY_ReturnsTrue() {
            loan.setIsActive("Y");
            assertThat(loan.isActiveLoan()).isTrue();
        }

        @Test
        @DisplayName("isActiveLoan returns false when isActive = N")
        void testIsActiveLoan_FlagN_ReturnsFalse() {
            loan.setIsActive("N");
            assertThat(loan.isActiveLoan()).isFalse();
        }

        @Test
        @DisplayName("isApproved returns true when transStatus = A")
        void testIsApproved_StatusA_ReturnsTrue() {
            loan.setTransStatus("A");
            assertThat(loan.isApproved()).isTrue();
        }

        @Test
        @DisplayName("isPending returns true when transStatus = N")
        void testIsPending_StatusN_ReturnsTrue() {
            loan.setTransStatus("N");
            assertThat(loan.isPending()).isTrue();
        }

        @Test
        @DisplayName("isRejected returns true when transStatus = R")
        void testIsRejected_StatusR_ReturnsTrue() {
            loan.setTransStatus("R");
            assertThat(loan.isRejected()).isTrue();
        }
    }
}
