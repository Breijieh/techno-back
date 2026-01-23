package com.techno.backend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LoanInstallment entity.
 * Tests payment marking and postponement logic.
 *
 * @author Techno HR System - Testing Suite
 */
@DisplayName("LoanInstallment Entity Tests")
class LoanInstallmentTest {

    private LoanInstallment installment;

    @BeforeEach
    void setUp() {
        installment = LoanInstallment.builder()
                .installmentId(1L)
                .loanId(100L)
                .installmentNo(1)
                .dueDate(LocalDate.of(2026, 1, 15))
                .installmentAmount(new BigDecimal("1000.0000"))
                .paymentStatus("UNPAID")
                .build();
    }

    // ==================== MARK AS PAID TESTS ====================

    @Nested
    @DisplayName("markAsPaid() Tests")
    class MarkAsPaidTests {

        @Test
        @DisplayName("markAsPaid sets all payment fields correctly")
        void testMarkAsPaid_SetsAllFieldsCorrectly() {
            // Arrange
            LocalDate paymentDate = LocalDate.of(2026, 1, 25);
            BigDecimal amount = new BigDecimal("1000.0000");
            String payrollMonth = "2026-01";

            // Act
            installment.markAsPaid(paymentDate, amount, payrollMonth);

            // Assert
            assertThat(installment.getPaymentStatus()).isEqualTo("PAID");
            assertThat(installment.getPaidDate()).isEqualTo(paymentDate);
            assertThat(installment.getPaidAmount()).isEqualByComparingTo(amount);
            assertThat(installment.getSalaryMonth()).isEqualTo(payrollMonth);
        }

        @Test
        @DisplayName("markAsPaid with partial amount records actual paid amount")
        void testMarkAsPaid_PartialAmount_RecordsActualAmount() {
            // Arrange
            BigDecimal partialAmount = new BigDecimal("800.0000");

            // Act
            installment.markAsPaid(LocalDate.now(), partialAmount, "2026-01");

            // Assert
            assertThat(installment.getPaidAmount()).isEqualByComparingTo(partialAmount);
            assertThat(installment.getPaymentStatus()).isEqualTo("PAID");
        }
    }

    // ==================== POSTPONE TESTS ====================

    @Nested
    @DisplayName("postpone() Tests")
    class PostponeTests {

        @Test
        @DisplayName("postpone updates due date and sets status to POSTPONED")
        void testPostpone_UpdatesDueDateAndStatus() {
            // Arrange
            LocalDate newDueDate = LocalDate.of(2026, 2, 15);

            // Act
            installment.postpone(newDueDate);

            // Assert
            assertThat(installment.getPaymentStatus()).isEqualTo("POSTPONED");
            assertThat(installment.getDueDate()).isEqualTo(newDueDate);
        }
    }

    // ==================== HELPER METHOD TESTS ====================

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isPaid returns true when status is PAID")
        void testIsPaid_StatusPaid_ReturnsTrue() {
            installment.setPaymentStatus("PAID");
            assertThat(installment.isPaid()).isTrue();
        }

        @Test
        @DisplayName("isPaid returns false when status is not PAID")
        void testIsPaid_StatusNotPaid_ReturnsFalse() {
            installment.setPaymentStatus("UNPAID");
            assertThat(installment.isPaid()).isFalse();

            installment.setPaymentStatus("POSTPONED");
            assertThat(installment.isPaid()).isFalse();
        }

        @Test
        @DisplayName("isUnpaid returns true when status is UNPAID")
        void testIsUnpaid_StatusUnpaid_ReturnsTrue() {
            installment.setPaymentStatus("UNPAID");
            assertThat(installment.isUnpaid()).isTrue();
        }

        @Test
        @DisplayName("isPostponed returns true when status is POSTPONED")
        void testIsPostponed_StatusPostponed_ReturnsTrue() {
            installment.setPaymentStatus("POSTPONED");
            assertThat(installment.isPostponed()).isTrue();
        }
    }
}
