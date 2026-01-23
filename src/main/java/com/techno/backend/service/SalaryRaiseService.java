package com.techno.backend.service;

import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmpPayrollTransaction;
import com.techno.backend.entity.SalaryBreakdownPercentage;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.EmpPayrollTransactionRepository;
import com.techno.backend.repository.SalaryBreakdownPercentageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for processing salary raises.
 *
 * When a salary raise is approved, this service:
 * 1. Updates employee's base monthly_salary
 * 2. Deactivates old payroll transactions
 * 3. Creates new payroll transactions with recalculated amounts
 *    based on salary breakdown percentages
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 7 - Payroll Approval
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryRaiseService {

    private final EmployeeRepository employeeRepository;
    private final EmpPayrollTransactionRepository payrollTransactionRepository;
    private final SalaryBreakdownPercentageRepository salaryBreakdownRepository;

    /**
     * Process a salary raise for an employee.
     *
     * This method:
     * - Validates the new salary is greater than current
     * - Updates employee.monthly_salary
     * - Deactivates old payroll transactions (sets isActive='N')
     * - Creates new payroll transactions based on nationality breakdown percentages
     *
     * @param employeeNo Employee number
     * @param newSalary New monthly salary amount
     * @param effectiveDate Date when raise takes effect (default: today)
     * @param reason Reason for salary raise
     * @return Updated employee entity
     */
    @Transactional
    public Employee processSalaryRaise(Long employeeNo, BigDecimal newSalary,
                                      LocalDate effectiveDate, String reason) {
        log.info("Processing salary raise for employee {} to {} effective {}",
                employeeNo, newSalary, effectiveDate);

        // Validations
        if (newSalary == null || newSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("الراتب الجديد يجب أن يكون موجباً");
        }
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }

        // Load employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("الموظف غير موجود: " + employeeNo));

        BigDecimal oldSalary = employee.getMonthlySalary();
        if (newSalary.compareTo(oldSalary) <= 0) {
            throw new RuntimeException("الراتب الجديد يجب أن يكون أكبر من الراتب الحالي " + oldSalary);
        }

        // Update employee salary
        employee.setMonthlySalary(newSalary);
        employee = employeeRepository.save(employee);
        log.info("Updated employee {} salary from {} to {}", employeeNo, oldSalary, newSalary);

        // Deactivate current payroll transactions
        List<EmpPayrollTransaction> currentTransactions =
                payrollTransactionRepository.findActiveByEmployeeNo(employeeNo);

        if (!currentTransactions.isEmpty()) {
            for (EmpPayrollTransaction transaction : currentTransactions) {
                transaction.setIsActive("N");
                payrollTransactionRepository.save(transaction);
            }
            log.info("Deactivated {} existing payroll transactions for employee {}",
                    currentTransactions.size(), employeeNo);
        }

        // Create new payroll transactions based on nationality breakdown
        List<SalaryBreakdownPercentage> breakdowns =
                salaryBreakdownRepository.findByEmployeeCategory(employee.getEmployeeCategory());

        if (breakdowns.isEmpty()) {
            // Fallback: Create single transaction for full salary if no breakdown defined
            log.warn("No salary breakdown found for category {}. Creating single transaction.",
                    employee.getEmployeeCategory());
            createPayrollTransaction(employeeNo, 1L, newSalary, BigDecimal.ONE, effectiveDate);
        } else {
            // Create transaction for each component
            for (SalaryBreakdownPercentage breakdown : breakdowns) {
                BigDecimal componentAmount = newSalary
                        .multiply(breakdown.getSalaryPercentage())
                        .setScale(4, RoundingMode.HALF_UP);

                createPayrollTransaction(employeeNo, breakdown.getTransTypeCode(),
                        componentAmount, breakdown.getSalaryPercentage(), effectiveDate);

                log.debug("Created payroll transaction: type={}, amount={}, percentage={}",
                        breakdown.getTransTypeCode(), componentAmount, breakdown.getSalaryPercentage());
            }
            log.info("Created {} new payroll transactions for employee {}", breakdowns.size(), employeeNo);
        }

        log.info("Salary raise completed successfully for employee {}. Old: {}, New: {}, Reason: {}",
                employeeNo, oldSalary, newSalary, reason);

        return employee;
    }

    /**
     * Create a payroll transaction.
     */
    private void createPayrollTransaction(Long employeeNo, Long typeCode,
                                         BigDecimal amount, BigDecimal percentage,
                                         LocalDate effectiveDate) {
        EmpPayrollTransaction transaction = EmpPayrollTransaction.builder()
                .employeeNo(employeeNo)
                .transTypeCode(typeCode)
                .transAmount(amount)
                .transPercentage(percentage)
                .effectiveDate(effectiveDate)
                .isActive("Y")
                .build();

        payrollTransactionRepository.save(transaction);
    }

    /**
     * Calculate the raise percentage between old and new salary.
     *
     * @param oldSalary Current salary
     * @param newSalary New salary
     * @return Percentage increase (e.g., 10.5 for 10.5%)
     */
    public BigDecimal calculateRaisePercentage(BigDecimal oldSalary, BigDecimal newSalary) {
        if (oldSalary == null || oldSalary.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return newSalary.subtract(oldSalary)
                .divide(oldSalary, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate new salary from old salary and raise percentage.
     *
     * @param oldSalary Current salary
     * @param raisePercentage Percentage to increase (e.g., 10 for 10%)
     * @return New salary amount
     */
    public BigDecimal calculateNewSalary(BigDecimal oldSalary, BigDecimal raisePercentage) {
        if (oldSalary == null || raisePercentage == null) {
            throw new RuntimeException("الراتب القديم ونسبة الزيادة مطلوبان");
        }
        BigDecimal multiplier = BigDecimal.ONE.add(
                raisePercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        return oldSalary.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
    }
}
