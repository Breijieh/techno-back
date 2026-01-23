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
     * One-time method to initialize existing employees with a starting balance.
     * Use with caution.
     */
    @Transactional
    public void initializeExistingEmployees(BigDecimal startingBalance) {
        log.info("Initializing existing employees with leave balance: {}", startingBalance);
        List<Employee> employees = employeeRepository.findAll();
        for (Employee employee : employees) {
            employee.setLeaveBalanceDays(startingBalance);
        }
        employeeRepository.saveAll(employees);
        log.info("Initialized {} employees.", employees.size());
    }
}
