package com.techno.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides endpoints to check application and database status
 */
@RestController
@RequestMapping("/public/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "Techno ERP Backend");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    /**
     * Database connectivity check
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseCheck() {
        Map<String, Object> response = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            response.put("database", isValid ? "UP" : "DOWN");
            response.put("status", isValid ? "CONNECTED" : "DISCONNECTED");
            response.put("timestamp", LocalDateTime.now());

            if (isValid) {
                response.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
                response.put("databaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
                response.put("driverName", connection.getMetaData().getDriverName());
                response.put("url", connection.getMetaData().getURL());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("database", "DOWN");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * System information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        Map<String, Object> response = new HashMap<>();

        // Application info
        Map<String, Object> app = new HashMap<>();
        app.put("name", "Techno ERP System");
        app.put("version", "1.0.0");
        app.put("description", "Complete ERP system for managing 500+ employees");
        response.put("application", app);

        // System info
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("osArch", System.getProperty("os.arch"));
        response.put("system", system);

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        memory.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        response.put("memory", memory);

        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
