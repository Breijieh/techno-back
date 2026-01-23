package com.techno.backend.service;

import com.techno.backend.dto.SystemLogListResponse;
import com.techno.backend.dto.SystemLogResponse;
import com.techno.backend.entity.SystemLog;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for System Log management
 * Handles business logic for system log operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    /**
     * Get all system logs with pagination and optional filters
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @param level log level filter (INFO, WARNING, ERROR, DEBUG) - optional
     * @param module module filter - optional
     * @param actionType action type filter - optional
     * @param fromDate start date filter - optional
     * @param toDate end date filter - optional
     * @return paginated list of system logs
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getAllLogs(
            int page,
            int size,
            String level,
            String module,
            String actionType,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.info("Fetching system logs - page: {}, size: {}, level: {}, module: {}, actionType: {}, fromDate: {}, toDate: {}",
                page, size, level, module, actionType, fromDate, toDate);

        // Create pageable with sort by createdDate DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<SystemLog> logPage;

        // Use custom query if any filters are provided
        if (level != null || module != null || actionType != null || fromDate != null || toDate != null) {
            logPage = systemLogRepository.findByFilters(level, module, actionType, fromDate, toDate, pageable);
        } else {
            // No filters, get all logs
            logPage = systemLogRepository.findAllByOrderByCreatedDateDesc(pageable);
        }

        // Map entities to DTOs
        List<SystemLogResponse> logResponses = logPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return SystemLogListResponse.fromPage(logPage, logResponses);
    }

    /**
     * Get system log by ID
     *
     * @param logId the log ID
     * @return SystemLogResponse
     */
    @Transactional(readOnly = true)
    public SystemLogResponse getLogById(Long logId) {
        log.info("Fetching system log by ID: {}", logId);

        SystemLog systemLog = systemLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("سجل النظام غير موجود برقم: " + logId));

        return mapToResponse(systemLog);
    }

    /**
     * Create a new system log entry
     * This method is typically called internally (e.g., from AOP audit aspects)
     *
     * @param systemLog the system log entity to create
     * @return created SystemLogResponse
     */
    @Transactional
    public SystemLogResponse createLog(SystemLog systemLog) {
        log.info("Creating system log - module: {}, action: {}, level: {}",
                systemLog.getModule(), systemLog.getActionType(), systemLog.getLogLevel());

        SystemLog savedLog = systemLogRepository.save(systemLog);
        return mapToResponse(savedLog);
    }

    /**
     * Map SystemLog entity to SystemLogResponse DTO
     *
     * @param systemLog the entity
     * @return SystemLogResponse DTO
     */
    private SystemLogResponse mapToResponse(SystemLog systemLog) {
        return SystemLogResponse.builder()
                .logId(systemLog.getLogId())
                .userId(systemLog.getUserId())
                .actionType(systemLog.getActionType())
                .module(systemLog.getModule())
                .description(systemLog.getDescription())
                .level(systemLog.getLogLevel())
                .timestamp(systemLog.getCreatedDate())
                .ipAddress(systemLog.getIpAddress())
                .build();
    }
}

