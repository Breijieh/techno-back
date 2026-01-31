package com.techno.backend.service;

import com.techno.backend.entity.Employee;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for automated leave accrual.
 * 
 * Rules:
 * - Every ACTIVE employee earns 30 days of leave per year.
 * - Accrual happens on the 1st of January every year at 00:01 AM.
 * - Unused leave balance is carried over (not reset).
 * 
 * @author Techno HR System
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveAccrualService {

    private final EmployeeRepository employeeRepository;

    /**
     * Annual leave accrual task.
     * Runs at 00:01 AM on the 1st day of January every year.
     * Cron: "0 1 0 1 1 *"
     */
    @Scheduled(cron = "0 1 0 1 1 *", zone = "Asia/Riyadh")
    @Transactional
    public void performAnnualAccrual() {
        log.info("Starting annual leave accrual job...");

        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
        if (activeEmployees.isEmpty()) {
            log.info("No active employees found for accrual.");
            return;
        }

        BigDecimal accrualAmount = BigDecimal.valueOf(30.0);
        int count = 0;

        for (Employee employee : activeEmployees) {
            BigDecimal currentBalance = employee.getLeaveBalanceDays() != null
                    ? employee.getLeaveBalanceDays()
                    : BigDecimal.ZERO;

            BigDecimal newBalance = currentBalance.add(accrualAmount);
            employee.setLeaveBalanceDays(newBalance);
            count++;
        }

        employeeRepository.saveAll(activeEmployees);
        log.info("Annual leave accrual completed. Updated {} employees with {} days each.", count, accrualAmount);
    }

    /**
     * Add annual leave allowance to all active employees.
     * Adds the specified amount to existing balance (supports carryover).
     * For example: if employee has 15 days remaining and you add 30, they get 45.
     */
    @Transactional
    public void addAnnualAllowanceToEmployees(BigDecimal allowanceAmount) {
        log.info("Adding {} days annual leave allowance to all active employees", allowanceAmount);

        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
        if (activeEmployees.isEmpty()) {
            log.info("No active employees found to add allowance.");
            return;
        }

        int count = 0;
        for (Employee employee : activeEmployees) {
            BigDecimal currentBalance = employee.getLeaveBalanceDays() != null
                    ? employee.getLeaveBalanceDays()
                    : BigDecimal.ZERO;

            BigDecimal newBalance = currentBalance.add(allowanceAmount);
            employee.setLeaveBalanceDays(newBalance);

            log.debug("Employee {}: {} + {} = {} days",
                    employee.getEmployeeNo(), currentBalance, allowanceAmount, newBalance);
            count++;
        }

        employeeRepository.saveAll(activeEmployees);
        log.info("Added {} days allowance to {} active employees.", allowanceAmount, count);
    }

    /**
     * Initialize employees who have null/zero balance to a starting value.
     * Use this for new employees who weren't properly initialized.
     */
    @Transactional
    public void initializeEmployeesWithZeroBalance(BigDecimal startingBalance) {
        log.info("Initializing employees with zero/null balance to: {}", startingBalance);

        List<Employee> employees = employeeRepository.findAllActiveEmployees();
        int count = 0;

        for (Employee employee : employees) {
            BigDecimal currentBalance = employee.getLeaveBalanceDays();
            if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) == 0) {
                employee.setLeaveBalanceDays(startingBalance);
                count++;
                log.debug("Initialized employee {} with {} days", employee.getEmployeeNo(), startingBalance);
            }
        }

        if (count > 0) {
            employeeRepository.saveAll(employees);
        }
        log.info("Initialized {} employees with zero balance.", count);
    }
}
