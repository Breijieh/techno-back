package com.techno.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migration component to handle schema changes that Hibernate's ddl-auto=update
 * might miss.
 * 
 * Runs early in the application lifecycle to ensure schema is correct before
 * data.sql executes.
 */
@Component
@RequiredArgsConstructor
@Order(1) // Run before other CommandLineRunners
public class SchemaMigration implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

        private final JdbcTemplate jdbcTemplate;
        private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        /**
         * Run schema migrations as early as possible.
         * This ensures the schema is correct before data.sql executes.
         */
        @Override
        public void run(String... args) {
                log.info("Running SchemaMigration...");
                try {
                        // Ensure is_paid column exists (handles cases where table was created by
                        // Hibernate)
                        // Note: schema.sql should handle this, but this is a backup
                        String checkColumnSql = "DO $$ " +
                                        "BEGIN " +
                                        "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'eids_holidays' AND column_name = 'is_paid') THEN "
                                        +
                                        "    ALTER TABLE eids_holidays ADD COLUMN is_paid VARCHAR(1) DEFAULT 'Y'; " +
                                        "    UPDATE eids_holidays SET is_paid = 'Y' WHERE is_paid IS NULL; " +
                                        "    ALTER TABLE eids_holidays ALTER COLUMN is_paid SET NOT NULL; " +
                                        "  END IF; " +
                                        "END $$;";

                        jdbcTemplate.execute(checkColumnSql);

                        log.info("SchemaMigration completed successfully: verified is_paid column exists in eids_holidays");

                        // Add foreign key constraint to notifications table if employees_details exists
                        try {
                                // Check if employees_details table exists and foreign key doesn't exist yet
                                String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                                "WHERE table_schema = 'public' AND table_name = 'employees_details'";
                                Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

                                if (tableCount != null && tableCount > 0) {
                                        // Check if foreign key already exists
                                        String checkFkSql = "SELECT COUNT(*) FROM information_schema.table_constraints "
                                                        +
                                                        "WHERE constraint_schema = 'public' AND constraint_name = 'fk_notification_employee'";
                                        Integer fkCount = jdbcTemplate.queryForObject(checkFkSql, Integer.class);

                                        if (fkCount == null || fkCount == 0) {
                                                jdbcTemplate.execute(
                                                                "ALTER TABLE notifications ADD CONSTRAINT fk_notification_employee "
                                                                                +
                                                                                "FOREIGN KEY (employee_no) REFERENCES employees_details(employee_no)");
                                                log.info("Added foreign key constraint fk_notification_employee to notifications table");
                                        }
                                }
                        } catch (Exception e) {
                                log.debug("Could not add foreign key constraint (employees_details may not exist yet): {}",
                                                e.getMessage());
                        }

                        seedContractTypes();
                        seedAdminUserAndEmployee();

                } catch (Exception e) {
                        log.warn("SchemaMigration skipped or partially failed: {}", e.getMessage());
                }
        }

        private void seedContractTypes() {
                try {
                        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contract_types",
                                        Integer.class);
                        if (count != null && count == 0) {
                                log.info("Seeding contract types...");

                                // TECHNO
                                jdbcTemplate.execute(
                                                "INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date) "
                                                                +
                                                                "VALUES ('TECHNO', 'موظف تكنو', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)");

                                // TEMPORARY
                                jdbcTemplate.execute(
                                                "INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date) "
                                                                +
                                                                "VALUES ('TEMPORARY', 'موظف مؤقت', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)");

                                // DAILY
                                jdbcTemplate.execute(
                                                "INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date) "
                                                                +
                                                                "VALUES ('DAILY', 'عامل يومي', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)");

                                // CLIENT
                                jdbcTemplate.execute(
                                                "INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date) "
                                                                +
                                                                "VALUES ('CLIENT', 'موظف عميل', 'N', 'Y', 'Y', CURRENT_TIMESTAMP)");

                                // CONTRACTOR
                                jdbcTemplate.execute(
                                                "INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date) "
                                                                +
                                                                "VALUES ('CONTRACTOR', 'مقاول', 'N', 'N', 'Y', CURRENT_TIMESTAMP)");

                                log.info("Contract types seeded successfully");
                        } else {
                                log.info("Contract types already exist. Skipping seeding.");
                        }
                } catch (Exception e) {
                        log.error("Failed to seed contract types: {}", e.getMessage());
                }
        }

        private void seedAdminUserAndEmployee() {
                try {
                        log.info("Checking/Seeding Admin User and Employee...");

                        // --- 1. Admin User Setup ---

                        // 1.1 Ensure Admin Employee Exists
                        Integer adminEmpCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM employees_details WHERE national_id = '1000000000'",
                                        Integer.class);

                        Long adminEmployeeNo;
                        if (adminEmpCount != null && adminEmpCount == 0) {
                                log.info("Creating System Administrator employee...");
                                jdbcTemplate.update(
                                                "INSERT INTO employees_details (employee_name, national_id, nationality, "
                                                                +
                                                                "employee_category, hire_date, employment_status, emp_contract_type, monthly_salary, "
                                                                +
                                                                "email, mobile, created_date, modified_date) " +
                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                                "\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645",
                                                "1000000000", "Saudi",
                                                "S", java.sql.Date.valueOf(java.time.LocalDate.now()), "ACTIVE",
                                                "TECHNO", 0.00,
                                                "admin@technoerp.com", "0500000000");
                                log.info("System Administrator employee created successfully");
                        }

                        // Get the admin employee_no
                        adminEmployeeNo = jdbcTemplate.queryForObject(
                                        "SELECT employee_no FROM employees_details WHERE national_id = '1000000000'",
                                        Long.class);

                        // 1.2 Ensure Admin User Exists and is linked
                        Integer adminUserCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM user_accounts WHERE username = 'admin'", Integer.class);

                        if (adminUserCount != null && adminUserCount == 0) {
                                log.info("Creating admin user account...");
                                jdbcTemplate.update(
                                                "INSERT INTO user_accounts (username, password_hash, national_id, user_type, is_active, employee_no, created_date) "
                                                                +
                                                                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                                                "admin", passwordEncoder.encode("admin123"),
                                                "1000000000", "ADMIN", 'Y', adminEmployeeNo);

                        } else {
                                // Ensure existing admin user is linked to the admin employee
                                log.info("Updating admin user linkage...");

                                // 1. Update linkage and activation
                                jdbcTemplate.update(
                                                "UPDATE user_accounts SET employee_no = ?, is_active = 'Y' WHERE username = 'admin'",
                                                adminEmployeeNo);
                        }

                        // --- 2. Superadmin User Setup ---

                        // 2.1 Ensure Superadmin Employee Exists (Different identity)
                        Integer superAdminEmpCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM employees_details WHERE national_id = '1000000001'",
                                        Integer.class);

                        Long superAdminEmployeeNo;
                        if (superAdminEmpCount != null && superAdminEmpCount == 0) {
                                log.info("Creating Super System Administrator employee...");
                                jdbcTemplate.update(
                                                "INSERT INTO employees_details (employee_name, national_id, nationality, "
                                                                +
                                                                "employee_category, hire_date, employment_status, emp_contract_type, monthly_salary, "
                                                                +
                                                                "email, mobile, created_date, modified_date) " +
                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                                "\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645 \u0627\u0644\u0641\u0627\u0626\u0642",
                                                "1000000001",
                                                "Saudi",
                                                "S", java.sql.Date.valueOf(java.time.LocalDate.now()), "ACTIVE",
                                                "TECHNO", 0.00,
                                                "superadmin@technoerp.com", "0500000001");
                                log.info("Super System Administrator employee created successfully");
                        }

                        // Get the superadmin employee_no
                        superAdminEmployeeNo = jdbcTemplate.queryForObject(
                                        "SELECT employee_no FROM employees_details WHERE national_id = '1000000001'",
                                        Long.class);

                        // 2.2 Ensure Superadmin User Exists and is linked
                        Integer superadminUserCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM user_accounts WHERE username = 'superadmin'",
                                        Integer.class);

                        if (superadminUserCount != null && superadminUserCount == 0) {
                                log.info("Creating superadmin user account...");
                                // Use 'admin123' hash
                                // ($2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW)
                                jdbcTemplate.update(
                                                "INSERT INTO user_accounts (username, password_hash, national_id, user_type, is_active, employee_no, created_date) "
                                                                +
                                                                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                                                "superadmin",
                                                passwordEncoder.encode("admin123"),
                                                "1000000001", "ADMIN", 'Y', superAdminEmployeeNo);

                                log.info("Superadmin user created and linked to employee: {}", superAdminEmployeeNo);
                        } else {
                                // Ensure existing superadmin user is linked to the superadmin employee
                                log.info("Updating superadmin user linkage...");

                                // 1. Update linkage and activation
                                jdbcTemplate.update(
                                                "UPDATE user_accounts SET employee_no = ?, is_active = 'Y' WHERE username = 'superadmin'",
                                                superAdminEmployeeNo);

                                log.info("Superadmin user linked to employee: {}", superAdminEmployeeNo);
                        }

                        log.info("Admin and Superadmin User/Employee seeding completed successfully");

                } catch (Exception e) {
                        log.error("Failed to seed Admin/Superadmin User/Employee: {}", e.getMessage());
                }
        }
}
