package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SystemSupportController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/fix-admin")
    public com.techno.backend.dto.ApiResponse<String> fixAdminData() {
        try {
            // 1. Ensure Admin Employee Exists
            Integer empCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM employees_details WHERE national_id = '1000000000'", Integer.class);

            Long employeeNo;
            if (empCount != null && empCount == 0) {
                jdbcTemplate.update(
                        "INSERT INTO employees_details (employee_ar_name, employee_en_name, national_id, nationality, "
                                +
                                "employee_category, hire_date, employment_status, emp_contract_type, monthly_salary, " +
                                "email, mobile, created_date, modified_date) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        "مدير النظام", "System Administrator", "1000000000", "Saudi",
                        "S", java.sql.Date.valueOf(java.time.LocalDate.now()), "ACTIVE", "TECHNO", 10000.00,
                        "admin@technoerp.com", "0500000000");
            }

            // Get the employee_no
            employeeNo = jdbcTemplate.queryForObject(
                    "SELECT employee_no FROM employees_details WHERE national_id = '1000000000'", Long.class);

            // 2. Link Admin User
            int updated = jdbcTemplate.update(
                    "UPDATE user_accounts SET employee_no = ? WHERE username = 'admin'",
                    employeeNo);

            return com.techno.backend.dto.ApiResponse.success(
                    "Admin user fixed. Linked to Employee ID: " + employeeNo + ". Rows updated: " + updated, null);
        } catch (Exception e) {
            return com.techno.backend.dto.ApiResponse.error("Failed to fix admin: " + e.getMessage());
        }
    }
}
