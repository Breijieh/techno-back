package com.techno.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Custom database initializer that ensures schema is correct.
 * Runs after schema.sql to ensure the is_paid column exists before data.sql
 * executes.
 */
@Component
@Order(0) // Run as early as possible
public class DatabaseInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private boolean initialized = false;

    @Autowired
    public DatabaseInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Run migrations on startup.
     */
    @Override
    public void afterPropertiesSet() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Skip SQL scripts in test mode (H2 database)
        if (isH2Database()) {
            log.info("Skipping database initialization scripts in test mode (H2 database)");
            return;
        }

        try {
            log.info("Starting manual database initialization...");

            // 1. Run Migration Script (Schema Changes)
            executeScript("migrate-language-fields.sql");

            // 2. Run Data Seed Script (Data Insertion)
            executeScript("data-seed.sql");

            // 3. Run legacy fix for is_paid column
            try {
                jdbcTemplate
                        .execute("ALTER TABLE eids_holidays ADD COLUMN IF NOT EXISTS is_paid VARCHAR(1) DEFAULT 'Y'");
                jdbcTemplate.execute("UPDATE eids_holidays SET is_paid = 'Y' WHERE is_paid IS NULL");
                log.debug("Verified is_paid column exists in eids_holidays table");
            } catch (Exception e) {
                log.debug("Could not verify/add is_paid column: {}", e.getMessage());
            }

            log.info("Database initialization completed.");

        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if the current database is H2 (used in tests).
     */
    private boolean isH2Database() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            return "H2".equalsIgnoreCase(databaseProductName);
        } catch (Exception e) {
            log.debug("Could not determine database type: {}", e.getMessage());
            return false;
        }
    }

    private void executeScript(String filename) {
        try {
            log.info("Executing script: {}", filename);
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(
                    filename);

            if (resource.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(resource.getInputStream(),
                                java.nio.charset.StandardCharsets.UTF_8))) {

                    String sql = reader.lines().collect(java.util.stream.Collectors.joining("\n"));

                    
                    // Handle DO blocks (PostgreSQL) - they must be executed as single statements
                    // If SQL contains DO $$ block, execute as single statement
                    String sqlUpper = sql.toUpperCase().trim();
                    if (sqlUpper.startsWith("DO $$") || sqlUpper.startsWith("--") && sqlUpper.contains("DO $$")) {
                        // Execute entire SQL as single statement (DO block)
                        try {
                            jdbcTemplate.execute(sql);
                            log.info("Executed DO block from {}", filename);
                        } catch (Exception e) {
                            log.warn("Error executing DO block in {}: {}", filename, e.getMessage());
                        }
                        return;
                    }

                    // Split by semicolon to get individual statements
                    String[] statements = sql.split(";");

                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        
                        // Skip empty statements
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        
                        // Skip comment-only lines (lines starting with --)
                        if (trimmed.startsWith("--")) {
                            continue;
                        }
                        
                        // Skip multi-line comments (/* ... */)
                        if (trimmed.startsWith("/*") || trimmed.contains("/*")) {
                            continue;
                        }

                        try {
                            // log.debug("Executing SQL: {}", trimmed);
                            jdbcTemplate.execute(trimmed);
                        } catch (Exception e) {
                            // Log but continue, as some changes might already be applied (idempotency)
                            log.warn("Error executing statement in {}: {}. Error: {}",
                                    filename,
                                    trimmed.substring(0, Math.min(trimmed.length(), 50)) + "...",
                                    e.getMessage());
                        }
                    }
                    log.info("Script {} executed successfully.", filename);
                }
            } else {
                log.warn("Script {} not found!", filename);
            }
        } catch (Exception e) {
            log.error("Failed to execute script {}: {}", filename, e.getMessage());
        }
    }
}
