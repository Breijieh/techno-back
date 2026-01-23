package com.techno.backend.controller;

import com.techno.backend.entity.Employee;
import com.techno.backend.service.SalaryRaiseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Controller for salary raise processing.
 *
 * Provides endpoints for:
 * - Processing approved salary raises
 * - Calculating raise percentages
 * - Calculating new salary from percentage
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 7 - Payroll Approval
 */
@RestController
@RequestMapping("/salary-raise")
@RequiredArgsConstructor
@Slf4j
public class SalaryRaiseController {

        private final SalaryRaiseService salaryRaiseService;

        /**
         * Process a salary raise for an employee.
         *
         * POST /api/salary-raise
         *
         * This endpoint:
         * - Updates employee's base salary
         * - Deactivates old payroll transactions
         * - Creates new payroll transactions with new amounts
         *
         * @param request Salary raise request
         * @return Response with updated employee data
         */
        @PostMapping
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
        public ResponseEntity<SalaryRaiseResponse> processSalaryRaise(
                        @Valid @RequestBody SalaryRaiseRequest request) {
                log.info("POST /api/salary-raise - Employee: {}, New Salary: {}, Effective: {}",
                                request.employeeNo(), request.newSalary(), request.effectiveDate());

                Employee employee = salaryRaiseService.processSalaryRaise(
                                request.employeeNo(),
                                request.newSalary(),
                                request.effectiveDate(),
                                request.reason());

                BigDecimal oldSalary = request.oldSalary(); // Sent from frontend for display
                BigDecimal raisePercentage = salaryRaiseService.calculateRaisePercentage(
                                oldSalary, request.newSalary());

                SalaryRaiseResponse response = new SalaryRaiseResponse(
                                employee.getEmployeeNo(),
                                employee.getEmployeeName(),
                                oldSalary,
                                employee.getMonthlySalary(),
                                raisePercentage,
                                request.effectiveDate(),
                                request.reason(),
                                "ØªÙ… Ù…Ø¹Ø§Ù„Ø¬Ø© Ø²ÙŠØ§Ø¯Ø© Ø§Ù„Ø±Ø§ØªØ¨ Ø¨Ù†Ø¬Ø§Ø­");

                return ResponseEntity.ok(response);
        }

        /**
         * Calculate the raise percentage between two salary amounts.
         *
         * GET /api/salary-raise/calculate-percentage
         *
         * @param oldSalary Current salary
         * @param newSalary New salary
         * @return Percentage increase
         */
        @GetMapping("/calculate-percentage")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
        public ResponseEntity<RaisePercentageResponse> calculateRaisePercentage(
                        @RequestParam BigDecimal oldSalary,
                        @RequestParam BigDecimal newSalary) {
                log.info("GET /api/salary-raise/calculate-percentage - Old: {}, New: {}",
                                oldSalary, newSalary);

                BigDecimal percentage = salaryRaiseService.calculateRaisePercentage(oldSalary, newSalary);
                BigDecimal difference = newSalary.subtract(oldSalary);

                RaisePercentageResponse response = new RaisePercentageResponse(
                                oldSalary, newSalary, difference, percentage);

                return ResponseEntity.ok(response);
        }

        /**
         * Calculate new salary from old salary and raise percentage.
         *
         * GET /api/salary-raise/calculate-new-salary
         *
         * @param oldSalary       Current salary
         * @param raisePercentage Percentage to increase
         * @return New calculated salary
         */
        @GetMapping("/calculate-new-salary")
        @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
        public ResponseEntity<NewSalaryCalculationResponse> calculateNewSalary(
                        @RequestParam BigDecimal oldSalary,
                        @RequestParam BigDecimal raisePercentage) {
                log.info("GET /api/salary-raise/calculate-new-salary - Old: {}, Percentage: {}",
                                oldSalary, raisePercentage);

                BigDecimal newSalary = salaryRaiseService.calculateNewSalary(oldSalary, raisePercentage);
                BigDecimal difference = newSalary.subtract(oldSalary);

                NewSalaryCalculationResponse response = new NewSalaryCalculationResponse(
                                oldSalary, raisePercentage, newSalary, difference);

                return ResponseEntity.ok(response);
        }

        // ==================== Request/Response DTOs ====================

        public record SalaryRaiseRequest(
                        @NotNull(message = "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ø·Ù„ÙˆØ¨") Long employeeNo,
                        @NotNull(message = "Ø§Ù„Ø±Ø§ØªØ¨ Ø§Ù„Ù‚Ø¯ÙŠÙ… Ù…Ø·Ù„ÙˆØ¨") @Positive BigDecimal oldSalary,
                        @NotNull(message = "Ø§Ù„Ø±Ø§ØªØ¨ Ø§Ù„Ø¬Ø¯ÙŠØ¯ Ù…Ø·Ù„ÙˆØ¨") @Positive BigDecimal newSalary,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
                        @NotBlank(message = "Ø§Ù„Ø³Ø¨Ø¨ Ù…Ø·Ù„ÙˆØ¨") String reason) {
        }

        public record SalaryRaiseResponse(
                        Long employeeNo,
                        String employeeName,
                        BigDecimal oldSalary,
                        BigDecimal newSalary,
                        BigDecimal raisePercentage,
                        LocalDate effectiveDate,
                        String reason,
                        String message) {
        }

        public record RaisePercentageResponse(
                        BigDecimal oldSalary,
                        BigDecimal newSalary,
                        BigDecimal difference,
                        BigDecimal raisePercentage) {
        }

        public record NewSalaryCalculationResponse(
                        BigDecimal oldSalary,
                        BigDecimal raisePercentage,
                        BigDecimal newSalary,
                        BigDecimal difference) {
        }
}

