package com.techno.backend.service;

import com.techno.backend.dto.TimeScheduleRequest;
import com.techno.backend.dto.TimeScheduleResponse;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.DepartmentRepository;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.repository.TimeScheduleRepository;
import com.techno.backend.util.AttendanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Time Schedule Management.
 * Handles business logic for time schedule CRUD operations.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TimeScheduleService {

    private final TimeScheduleRepository timeScheduleRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get all active time schedules.
     *
     * @return List of active schedules
     */
    @Transactional(readOnly = true)
    public List<TimeScheduleResponse> getAllActiveSchedules() {
        log.info("Fetching all active time schedules");

        List<TimeSchedule> schedules = timeScheduleRepository.findAllActiveSchedules();
        return schedules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get time schedule by ID.
     *
     * @param scheduleId Schedule ID
     * @return Schedule response
     */
    @Transactional(readOnly = true)
    public TimeScheduleResponse getScheduleById(Long scheduleId) {
        log.info("Fetching time schedule by ID: {}", scheduleId);

        TimeSchedule schedule = findScheduleOrThrow(scheduleId);
        return mapToResponse(schedule);
    }

    /**
     * Get schedules by department code.
     *
     * @param departmentCode Department code
     * @return List of schedules
     */
    @Transactional(readOnly = true)
    public List<TimeScheduleResponse> getSchedulesByDepartment(Long departmentCode) {
        log.info("Fetching schedules for department: {}", departmentCode);

        List<TimeSchedule> schedules = timeScheduleRepository.findByDepartmentCode(departmentCode);
        return schedules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get schedules by project code.
     *
     * @param projectCode Project code
     * @return List of schedules
     */
    @Transactional(readOnly = true)
    public List<TimeScheduleResponse> getSchedulesByProject(Long projectCode) {
        log.info("Fetching schedules for project: {}", projectCode);

        List<TimeSchedule> schedules = timeScheduleRepository.findByProjectCode(projectCode);
        return schedules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get default schedule (not tied to specific department or project).
     *
     * @return Default schedule or null if not configured
     */
    @Transactional(readOnly = true)
    public TimeScheduleResponse getDefaultSchedule() {
        log.info("Fetching default time schedule");

        Optional<TimeSchedule> schedule = timeScheduleRepository.findDefaultSchedule();
        return schedule.map(this::mapToResponse).orElse(null);
    }

    /**
     * Create new time schedule.
     *
     * @param request Schedule creation request
     * @return Created schedule
     */
    @Transactional
    public TimeScheduleResponse createSchedule(TimeScheduleRequest request) {
        log.info("Creating new time schedule: {}", request.getScheduleName());

        // Validate department if provided
        if (request.getDepartmentCode() != null) {
            if (!departmentRepository.existsById(request.getDepartmentCode())) {
                throw new BadRequestException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getDepartmentCode());
            }
        }

        // Validate project if provided
        if (request.getProjectCode() != null) {
            if (!projectRepository.existsById(request.getProjectCode())) {
                throw new BadRequestException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectCode());
            }
        }

        // Automatically calculate required hours from start and end times
        BigDecimal calculatedHours = AttendanceCalculator.calculateScheduledDuration(
                request.getScheduledStartTime(),
                request.getScheduledEndTime()
        );
        
        log.info("Calculated required hours: {} from start time {} to end time {}",
                calculatedHours, request.getScheduledStartTime(), request.getScheduledEndTime());
        
        // Override the provided requiredHours with calculated value
        request.setRequiredHours(calculatedHours);

        TimeSchedule schedule = mapToEntity(request);
        
        // Set createdBy from current user
        Long currentEmployeeNo = getCurrentEmployeeNo();
        if (currentEmployeeNo != null) {
            schedule.setCreatedBy(currentEmployeeNo);
            log.debug("Set createdBy to: {} for schedule: {}", currentEmployeeNo, request.getScheduleName());
        }
        
        schedule = timeScheduleRepository.save(schedule);

        log.info("Time schedule created successfully with ID: {}", schedule.getScheduleId());
        return mapToResponse(schedule);
    }

    /**
     * Update existing time schedule.
     *
     * @param scheduleId Schedule ID
     * @param request Update request
     * @return Updated schedule
     */
    @Transactional
    public TimeScheduleResponse updateSchedule(Long scheduleId, TimeScheduleRequest request) {
        log.info("Updating time schedule ID: {}", scheduleId);

        TimeSchedule schedule = findScheduleOrThrow(scheduleId);

        // Validate department if provided
        if (request.getDepartmentCode() != null) {
            if (!departmentRepository.existsById(request.getDepartmentCode())) {
                throw new BadRequestException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getDepartmentCode());
            }
        }

        // Validate project if provided
        if (request.getProjectCode() != null) {
            if (!projectRepository.existsById(request.getProjectCode())) {
                throw new BadRequestException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectCode());
            }
        }

        // Automatically calculate required hours from start and end times
        BigDecimal calculatedHours = AttendanceCalculator.calculateScheduledDuration(
                request.getScheduledStartTime(),
                request.getScheduledEndTime()
        );
        
        log.info("Calculated required hours: {} from start time {} to end time {}",
                calculatedHours, request.getScheduledStartTime(), request.getScheduledEndTime());
        
        // Override the provided requiredHours with calculated value
        request.setRequiredHours(calculatedHours);

        updateScheduleFromRequest(schedule, request);
        
        // Set modifiedBy from current user
        Long currentEmployeeNo = getCurrentEmployeeNo();
        if (currentEmployeeNo != null) {
            schedule.setModifiedBy(currentEmployeeNo);
            log.debug("Set modifiedBy to: {} for schedule ID: {}", currentEmployeeNo, scheduleId);
        }
        
        schedule = timeScheduleRepository.save(schedule);

        log.info("Time schedule updated successfully");
        return mapToResponse(schedule);
    }

    /**
     * Delete time schedule (soft delete by marking inactive).
     *
     * @param scheduleId Schedule ID
     */
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        log.info("Deleting time schedule ID: {}", scheduleId);

        TimeSchedule schedule = findScheduleOrThrow(scheduleId);
        timeScheduleRepository.delete(schedule);

        log.info("Time schedule deleted successfully");
    }

    // ==================== Helper Methods ====================

    private TimeSchedule findScheduleOrThrow(Long scheduleId) {
        return timeScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¬Ø¯ÙˆÙ„ Ø§Ù„ÙˆÙ‚Øª ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + scheduleId));
    }


    private TimeSchedule mapToEntity(TimeScheduleRequest request) {
        return TimeSchedule.builder()
                .scheduleName(request.getScheduleName())
                .departmentCode(request.getDepartmentCode())
                .projectCode(request.getProjectCode())
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .requiredHours(request.getRequiredHours())
                .gracePeriodMinutes(request.getGracePeriodMinutes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : "Y")
                .build();
    }

    private void updateScheduleFromRequest(TimeSchedule schedule, TimeScheduleRequest request) {
        schedule.setScheduleName(request.getScheduleName());
        schedule.setDepartmentCode(request.getDepartmentCode());
        schedule.setProjectCode(request.getProjectCode());
        schedule.setScheduledStartTime(request.getScheduledStartTime());
        schedule.setScheduledEndTime(request.getScheduledEndTime());
        schedule.setRequiredHours(request.getRequiredHours());
        schedule.setGracePeriodMinutes(request.getGracePeriodMinutes());
        if (request.getIsActive() != null) {
            schedule.setIsActive(request.getIsActive());
        }
    }

    private TimeScheduleResponse mapToResponse(TimeSchedule schedule) {
        TimeScheduleResponse.TimeScheduleResponseBuilder builder = TimeScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .scheduleName(schedule.getScheduleName())
                .departmentCode(schedule.getDepartmentCode())
                .projectCode(schedule.getProjectCode())
                .scheduledStartTime(schedule.getScheduledStartTime())
                .scheduledEndTime(schedule.getScheduledEndTime())
                .requiredHours(schedule.getRequiredHours())
                .gracePeriodMinutes(schedule.getGracePeriodMinutes())
                .isActive(schedule.getIsActive())
                .crossesMidnight(schedule.crossesMidnight())
                .graceEndTime(schedule.getGraceEndTime())
                .createdDate(schedule.getCreatedDate())
                .createdBy(schedule.getCreatedBy() != null ? schedule.getCreatedBy().toString() : null)
                .modifiedDate(schedule.getModifiedDate())
                .modifiedBy(schedule.getModifiedBy() != null ? schedule.getModifiedBy().toString() : null);

        // Add department details if available
        if (schedule.getDepartment() != null) {
            builder.departmentNameAr(schedule.getDepartment().getDeptName())
                    .departmentNameEn(schedule.getDepartment().getDeptName());
        }

        // Add project details if available
        if (schedule.getProject() != null) {
            builder.projectNameAr(schedule.getProject().getProjectName())
                    .projectNameEn(schedule.getProject().getProjectName());
        }

        return builder.build();
    }

    /**
     * Get current employee number from security context.
     *
     * @return Employee number or null if not available
     */
    private Long getCurrentEmployeeNo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authentication found in security context");
                return null;
            }

            // The JWT filter sets the principal to the employee number (Long) if available
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                log.debug("Found employee number {} from security context", principal);
                return (Long) principal;
            }

            // Fallback: try to parse as Long if it's a String representation
            if (principal instanceof String) {
                try {
                    Long employeeNo = Long.parseLong((String) principal);
                    log.debug("Parsed employee number {} from string principal", employeeNo);
                    return employeeNo;
                } catch (NumberFormatException e) {
                    log.debug("Could not parse employee number from string principal: {}", principal);
                }
            }

            // Also try authentication.getName() as fallback
            try {
                Long employeeNo = Long.parseLong(authentication.getName());
                log.debug("Parsed employee number {} from authentication name", employeeNo);
                return employeeNo;
            } catch (NumberFormatException e) {
                log.debug("Could not parse employee number from authentication name: {}", authentication.getName());
            }

            log.debug("Could not extract employee number from authentication principal: {}",
                    principal != null ? principal.getClass().getName() : "null");
            return null;
        } catch (Exception e) {
            log.warn("Error extracting employee number from security context: {}", e.getMessage());
            return null;
        }
    }
}

