package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.SystemLogListResponse;
import com.techno.backend.dto.SystemLogResponse;
import com.techno.backend.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * System Log Controller
 * Handles system log endpoints for viewing audit logs
 */
@RestController
@RequestMapping("/system-logs") // No /api prefix, matches other controllers
@RequiredArgsConstructor
@Slf4j
public class SystemLogController {

    private final SystemLogService systemLogService;

    /**
     * GET /system-logs
     * Get all system logs with pagination and optional filters
     *
     * @param page page number (default: 0)
     * @param size page size (default: 50)
     * @param level log level filter (INFO, WARNING, ERROR, DEBUG)
     * @param module module filter
     * @param actionType action type filter
     * @param fromDate start date filter (ISO format)
     * @param toDate end date filter (ISO format)
     * @return paginated list of system logs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemLogListResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("GET /system-logs - page: {}, size: {}, level: {}, module: {}, actionType: {}, fromDate: {}, toDate: {}",
                page, size, level, module, actionType, fromDate, toDate);

        SystemLogListResponse response = systemLogService.getAllLogs(
                page, size, level, module, actionType, fromDate, toDate);

        return ResponseEntity.ok(ApiResponse.success("تم استرجاع سجلات النظام بنجاح", response));
    }

    /**
     * GET /system-logs/{id}
     * Get system log by ID
     *
     * @param id the log ID
     * @return SystemLogResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemLogResponse>> getLogById(@PathVariable Long id) {
        log.info("GET /system-logs/{}", id);

        SystemLogResponse response = systemLogService.getLogById(id);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع سجل النظام بنجاح", response));
    }
}

