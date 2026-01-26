package com.techno.backend.runner;

import com.techno.backend.entity.Employee;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataFixRunner implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for invalid salary data...");

        // Threshold: 99,999,999.9999 (Max for NUMERIC(12,4))
        BigDecimal maxSalary = new BigDecimal("99999999");

        List<Employee> employees = employeeRepository.findAll();
        int fixedCount = 0;

        for (Employee emp : employees) {
            if (emp.getMonthlySalary() != null && emp.getMonthlySalary().compareTo(maxSalary) > 0) {
                log.warn("Found invalid salary for employee {} ({}): {}. Resetting to 0.",
                        emp.getEmployeeNo(), emp.getEmployeeName(), emp.getMonthlySalary());

                emp.setMonthlySalary(new BigDecimal("1.0"));
                employeeRepository.save(emp);
                fixedCount++;
            }
        }

        if (fixedCount > 0) {
            log.info("Fixed {} employees with invalid salary data.", fixedCount);
        } else {
            log.info("No invalid salary data found.");
        }
    }
}
