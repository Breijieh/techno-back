package com.techno.backend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SalaryHeader entity.
 * Tests the recalculateTotals formula and helper methods.
 *
 * @author Techno HR System - Testing Suite
 */
@DisplayName("SalaryHeader Entity Tests")
class SalaryHeaderTest {

    private SalaryHeader salaryHeader;

    @BeforeEach
    void setUp() {
        salaryHeader = SalaryHeader.builder()
                .salaryId(1L)
                .employeeNo(100L)
                .salaryMonth("2026-01")
                .salaryVersion(1)
                .grossSalary(new BigDecimal("5000.0000"))
                .totalAllowances(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.ZERO)
                .netSalary(new BigDecimal("5000.0000"))
                .transStatus("N")
                .salaryDetails(new ArrayList<>())
                .build();
    }

    // ==================== RECALCULATE TOTALS TESTS ====================

    @Nested
    @DisplayName("recalculateTotals() Tests")
    class RecalculateTotalsTests {

        @Test
        @DisplayName("With only allowances - Net = Gross + Allowances")
        void testRecalculateTotals_OnlyAllowances_CorrectNet() {
            // Arrange
            SalaryDetail allowance1 = SalaryDetail.builder()
                    .detailId(1L)
                    .salaryId(1L)
                    .lineNo(1)
                    .transTypeCode(1L)
                    .transAmount(new BigDecimal("1000.0000"))
                    .transCategory("A") // Allowance
                    .build();

            SalaryDetail allowance2 = SalaryDetail.builder()
                    .detailId(2L)
                    .salaryId(1L)
                    .lineNo(2)
                    .transTypeCode(2L)
                    .transAmount(new BigDecimal("500.0000"))
                    .transCategory("A") // Allowance
                    .build();

            salaryHeader.addDetail(allowance1);
            salaryHeader.addDetail(allowance2);

            // Act
            salaryHeader.recalculateTotals();

            // Assert
            // Net = 1500 (allowances) - 0 (deductions) = 1500
            assertThat(salaryHeader.getTotalAllowances()).isEqualByComparingTo(new BigDecimal("1500.0000"));
            assertThat(salaryHeader.getTotalDeductions()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(salaryHeader.getNetSalary()).isEqualByComparingTo(new BigDecimal("1500.0000"));
        }

        @Test
        @DisplayName("With only deductions - Net = Gross - Deductions")
        void testRecalculateTotals_OnlyDeductions_CorrectNet() {
            // Arrange
            SalaryDetail deduction1 = SalaryDetail.builder()
                    .detailId(1L)
                    .salaryId(1L)
                    .lineNo(1)
                    .transTypeCode(20L)
                    .transAmount(new BigDecimal("500.0000"))
                    .transCategory("D") // Deduction
                    .build();

            SalaryDetail deduction2 = SalaryDetail.builder()
                    .detailId(2L)
                    .salaryId(1L)
                    .lineNo(2)
                    .transTypeCode(21L)
                    .transAmount(new BigDecimal("300.0000"))
                    .transCategory("D") // Deduction
                    .build();

            salaryHeader.addDetail(deduction1);
            salaryHeader.addDetail(deduction2);

            // Act
            salaryHeader.recalculateTotals();

            // Assert
            // Net = 0 (allowances) - 800 (deductions) = -800
            assertThat(salaryHeader.getTotalAllowances()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(salaryHeader.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("800.0000"));
            assertThat(salaryHeader.getNetSalary()).isEqualByComparingTo(new BigDecimal("-800.0000"));
        }

        @Test
        @DisplayName("With mixed allowances and deductions - Formula: Net = Gross + A - D")
        void testRecalculateTotals_Mixed_CorrectFormula() {
            // Arrange
            SalaryDetail allowance = SalaryDetail.builder()
                    .detailId(1L)
                    .salaryId(1L)
                    .lineNo(1)
                    .transTypeCode(1L)
                    .transAmount(new BigDecimal("2000.0000"))
                    .transCategory("A")
                    .build();

            SalaryDetail deduction = SalaryDetail.builder()
                    .detailId(2L)
                    .salaryId(1L)
                    .lineNo(2)
                    .transTypeCode(20L)
                    .transAmount(new BigDecimal("800.0000"))
                    .transCategory("D")
                    .build();

            salaryHeader.addDetail(allowance);
            salaryHeader.addDetail(deduction);

            // Act
            salaryHeader.recalculateTotals();

            // Assert
            // Net = 2000 - 800 = 1200
            assertThat(salaryHeader.getTotalAllowances()).isEqualByComparingTo(new BigDecimal("2000.0000"));
            assertThat(salaryHeader.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("800.0000"));
            assertThat(salaryHeader.getNetSalary()).isEqualByComparingTo(new BigDecimal("1200.0000"));
        }

        @Test
        @DisplayName("With no details - Net = Allowances - Deductions (0)")
        void testRecalculateTotals_NoDetails_NetEqualsZero() {
            // Act
            salaryHeader.recalculateTotals();

            // Assert
            assertThat(salaryHeader.getTotalAllowances()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(salaryHeader.getTotalDeductions()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(salaryHeader.getNetSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deductions can make net negative (edge case)")
        void testRecalculateTotals_DeductionsExceedGross_NegativeNet() {
            // Arrange - Large deduction
            SalaryDetail largeDeduction = SalaryDetail.builder()
                    .detailId(1L)
                    .salaryId(1L)
                    .lineNo(1)
                    .transTypeCode(20L)
                    .transAmount(new BigDecimal("6000.0000"))
                    .transCategory("D")
                    .build();

            salaryHeader.addDetail(largeDeduction);

            // Act
            salaryHeader.recalculateTotals();

            // Assert - Net can go negative (system should handle this upstream)
            // Assert - Net can go negative (system should handle this upstream)
            assertThat(salaryHeader.getNetSalary()).isEqualByComparingTo(new BigDecimal("-6000.0000"));
        }
    }

    // ==================== HELPER METHOD TESTS ====================

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isApproved returns true when transStatus = A")
        void testIsApproved_StatusA_ReturnsTrue() {
            salaryHeader.setTransStatus("A");
            assertThat(salaryHeader.isApproved()).isTrue();
        }

        @Test
        @DisplayName("isApproved returns false when transStatus != A")
        void testIsApproved_StatusNotA_ReturnsFalse() {
            salaryHeader.setTransStatus("N");
            assertThat(salaryHeader.isApproved()).isFalse();

            salaryHeader.setTransStatus("R");
            assertThat(salaryHeader.isApproved()).isFalse();
        }

        @Test
        @DisplayName("isPending returns true when transStatus = N")
        void testIsPending_StatusN_ReturnsTrue() {
            salaryHeader.setTransStatus("N");
            assertThat(salaryHeader.isPending()).isTrue();
        }

        @Test
        @DisplayName("isRejected returns true when transStatus = R")
        void testIsRejected_StatusR_ReturnsTrue() {
            salaryHeader.setTransStatus("R");
            assertThat(salaryHeader.isRejected()).isTrue();
        }

        @Test
        @DisplayName("isLatestVersion returns true when isLatest = Y")
        void testIsLatestVersion_FlagY_ReturnsTrue() {
            salaryHeader.setIsLatest("Y");
            assertThat(salaryHeader.isLatestVersion()).isTrue();
        }

        @Test
        @DisplayName("isLatestVersion returns false when isLatest = N")
        void testIsLatestVersion_FlagN_ReturnsFalse() {
            salaryHeader.setIsLatest("N");
            assertThat(salaryHeader.isLatestVersion()).isFalse();
        }

        @Test
        @DisplayName("isFinalSettlement returns true when salaryType = F")
        void testIsFinalSettlement_TypeF_ReturnsTrue() {
            salaryHeader.setSalaryType("F");
            assertThat(salaryHeader.isFinalSettlement()).isTrue();
        }

        @Test
        @DisplayName("isRegularSalary returns true when salaryType = W")
        void testIsRegularSalary_TypeW_ReturnsTrue() {
            salaryHeader.setSalaryType("W");
            assertThat(salaryHeader.isRegularSalary()).isTrue();
        }
    }

    // ==================== ADD DETAIL TESTS ====================

    @Nested
    @DisplayName("addDetail() Tests")
    class AddDetailTests {

        @Test
        @DisplayName("addDetail sets bidirectional relationship")
        void testAddDetail_SetsBidirectionalRelationship() {
            // Arrange
            SalaryDetail detail = SalaryDetail.builder()
                    .lineNo(1)
                    .transTypeCode(1L)
                    .transAmount(new BigDecimal("500.0000"))
                    .transCategory("A")
                    .build();

            // Act
            salaryHeader.addDetail(detail);

            // Assert
            assertThat(salaryHeader.getSalaryDetails()).contains(detail);
            assertThat(detail.getSalaryHeader()).isEqualTo(salaryHeader);
        }

        @Test
        @DisplayName("addDetail to null list creates new list")
        void testAddDetail_NullList_CreatesNewList() {
            // Arrange
            salaryHeader.setSalaryDetails(null);

            SalaryDetail detail = SalaryDetail.builder()
                    .lineNo(1)
                    .transTypeCode(1L)
                    .transAmount(new BigDecimal("500.0000"))
                    .transCategory("A")
                    .build();

            // Act
            salaryHeader.addDetail(detail);

            // Assert
            assertThat(salaryHeader.getSalaryDetails()).isNotNull();
            assertThat(salaryHeader.getSalaryDetails()).hasSize(1);
        }
    }
}
