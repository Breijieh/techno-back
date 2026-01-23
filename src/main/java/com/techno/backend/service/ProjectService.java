package com.techno.backend.service;

import com.techno.backend.dto.project.ProjectRequest;
import com.techno.backend.dto.project.ProjectResponse;
import com.techno.backend.dto.project.ProjectSummary;
import com.techno.backend.dto.project.ProjectUpdateRequest;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Project;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.repository.TimeScheduleRepository;
import com.techno.backend.entity.TimeSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Project management.
 * Handles business logic for project CRUD operations, GPS tracking, and project
 * queries.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final TimeScheduleRepository timeScheduleRepository;

    /**
     * Create a new project.
     *
     * @param request Project creation request
     * @return Created project response
     */
    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        log.info("Creating new project: {}", request.getProjectName());

        // Validate dates
        if (!request.hasValidDates()) {
            throw new BadRequestException(
                    "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ Ø£Ùˆ ÙŠØ³Ø§ÙˆÙŠ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        // Validate GPS coordinates consistency
        if (!request.hasValidGpsCoordinates()) {
            throw new BadRequestException(
                    "Ø¥Ø°Ø§ ØªÙ… ØªÙˆÙÙŠØ± Ø¥Ø­Ø¯Ø§Ø«ÙŠØ§Øª GPSØŒ ÙŠØ¬Ø¨ ØªÙˆÙÙŠØ± Ø¬Ù…ÙŠØ¹ Ø§Ù„Ø­Ù‚ÙˆÙ„ Ø§Ù„Ø«Ù„Ø§Ø«Ø© (Ø®Ø· Ø§Ù„Ø¹Ø±Ø¶ØŒ Ø®Ø· Ø§Ù„Ø·ÙˆÙ„ØŒ Ù†ØµÙ Ø§Ù„Ù‚Ø·Ø±)");
        }

        // Validate project manager if provided
        if (request.getProjectMgr() != null) {
            Employee manager = employeeRepository.findById(request.getProjectMgr())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ù…Ø¯ÙŠØ± Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectMgr()));
            log.debug("Assigned project manager: {}", manager.getEmployeeName());
        }

        // Create project entity
        Project project = Project.builder()
                .projectName(request.getProjectName())
                .projectName(request.getProjectName())
                .projectAddress(request.getProjectAddress())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalProjectAmount(request.getTotalProjectAmount())
                .projectProfitMargin(request.getProjectProfitMargin())
                .projectProfitMargin(request.getProjectProfitMargin())
                .projectLatitude(roundCoordinate(request.getProjectLatitude()))
                .projectLongitude(roundCoordinate(request.getProjectLongitude()))
                .gpsRadiusMeters(request.getGpsRadiusMeters())
                .gpsRadiusMeters(request.getGpsRadiusMeters())
                .requireGpsCheck(request.getRequireGpsCheck() != null ? request.getRequireGpsCheck() : "Y")
                .noOfPayments(request.getNoOfPayments())
                .firstDownPaymentDate(request.getFirstDownPaymentDate())
                .projectMgr(request.getProjectMgr())
                .technoSuffix(request.getTechnoSuffix())
                .projectStatus(request.getProjectStatus() != null ? request.getProjectStatus() : "ACTIVE")
                .build();

        project = projectRepository.save(project);
        log.info("Project created successfully with code: {}", project.getProjectCode());

        // Assign schedule to project if provided
        assignScheduleToProject(project.getProjectCode(), request.getScheduleId());

        return mapToResponse(project);
    }

    /**
     * Update an existing project.
     *
     * @param projectCode Project code
     * @param request     Update request
     * @return Updated project response
     */
    @Transactional
    public ProjectResponse updateProject(Long projectCode, ProjectUpdateRequest request) {
        log.info("Updating project: {}", projectCode);

        // Validate update request has changes
        if (!request.hasUpdates()) {
            throw new BadRequestException("Ù„Ù… ÙŠØªÙ… ØªÙˆÙÙŠØ± Ø­Ù‚ÙˆÙ„ Ù„Ù„ØªØ­Ø¯ÙŠØ«");
        }

        // Validate GPS update consistency
        if (!request.hasValidGpsUpdate()) {
            throw new BadRequestException(
                    "Ø¥Ø°Ø§ ØªÙ… ØªØ­Ø¯ÙŠØ« Ø¥Ø­Ø¯Ø§Ø«ÙŠØ§Øª GPSØŒ ÙŠØ¬Ø¨ ØªÙˆÙÙŠØ± Ø¬Ù…ÙŠØ¹ Ø§Ù„Ø­Ù‚ÙˆÙ„ Ø§Ù„Ø«Ù„Ø§Ø«Ø© (Ø®Ø· Ø§Ù„Ø¹Ø±Ø¶ØŒ Ø®Ø· Ø§Ù„Ø·ÙˆÙ„ØŒ Ù†ØµÙ Ø§Ù„Ù‚Ø·Ø±)");
        }

        // Validate date update consistency
        if (!request.hasValidDateUpdate()) {
            throw new BadRequestException(
                    "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ Ø£Ùˆ ÙŠØ³Ø§ÙˆÙŠ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        // Find existing project
        Project project = projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + projectCode));

        // Update fields if provided
        if (request.getProjectName() != null) {
            project.setProjectName(request.getProjectName());
        }
        if (request.getProjectName() != null) {
            project.setProjectName(request.getProjectName());
        }
        if (request.getProjectAddress() != null) {
            project.setProjectAddress(request.getProjectAddress());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }
        if (request.getTotalProjectAmount() != null) {
            project.setTotalProjectAmount(request.getTotalProjectAmount());
        }
        if (request.getProjectProfitMargin() != null) {
            project.setProjectProfitMargin(request.getProjectProfitMargin());
        }
        if (request.getProjectLatitude() != null) {
            project.setProjectLatitude(roundCoordinate(request.getProjectLatitude()));
        }
        if (request.getProjectLongitude() != null) {
            project.setProjectLongitude(roundCoordinate(request.getProjectLongitude()));
        }
        if (request.getGpsRadiusMeters() != null) {
            project.setGpsRadiusMeters(request.getGpsRadiusMeters());
        }
        if (request.getRequireGpsCheck() != null) {
            project.setRequireGpsCheck(request.getRequireGpsCheck());
        }
        if (request.getNoOfPayments() != null) {
            project.setNoOfPayments(request.getNoOfPayments());
        }
        if (request.getFirstDownPaymentDate() != null) {
            project.setFirstDownPaymentDate(request.getFirstDownPaymentDate());
        }
        if (request.getProjectMgr() != null) {
            // Validate manager exists
            employeeRepository.findById(request.getProjectMgr())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ù…Ø¯ÙŠØ± Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectMgr()));
            project.setProjectMgr(request.getProjectMgr());
        }
        if (request.getTechnoSuffix() != null) {
            project.setTechnoSuffix(request.getTechnoSuffix());
        }
        if (request.getProjectStatus() != null) {
            project.setProjectStatus(request.getProjectStatus());
        }

        // Validate updated dates
        if (project.getStartDate() != null && project.getEndDate() != null &&
                project.getEndDate().isBefore(project.getStartDate())) {
            throw new BadRequestException(
                    "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ Ø£Ùˆ ÙŠØ³Ø§ÙˆÙŠ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        project = projectRepository.save(project);
        log.info("Project {} updated successfully", projectCode);

        // Assign schedule to project if scheduleId is provided in update request
        // If scheduleId is in the request (even if null), update the assignment
        if (request.getScheduleId() != null) {
            assignScheduleToProject(project.getProjectCode(), request.getScheduleId());
        }

        return mapToResponse(project);
    }

    /**
     * Get project by code.
     *
     * @param projectCode Project code
     * @return Project response
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectCode) {
        log.debug("Fetching project: {}", projectCode);

        Project project = projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + projectCode));

        return mapToResponse(project);
    }

    /**
     * Get all projects with pagination.
     *
     * @param pageable Pagination information
     * @return Page of project summaries
     */
    @Transactional(readOnly = true)
    public Page<ProjectSummary> getAllProjects(Pageable pageable) {
        log.debug("Fetching all projects - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Project> projectPage = projectRepository.findAll(pageable);
        return projectPage.map(this::mapToSummary);
    }

    /**
     * Get only active projects.
     *
     * @return List of active project summaries
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> getActiveProjects() {
        log.debug("Fetching active projects");

        List<Project> activeProjects = projectRepository.findByProjectStatus("ACTIVE");
        return activeProjects.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get ongoing projects (between start and end dates).
     *
     * @return List of ongoing project summaries
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> getOngoingProjects() {
        log.debug("Fetching ongoing projects");

        List<Project> ongoingProjects = projectRepository.findOngoingProjects(java.time.LocalDate.now());
        return ongoingProjects.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Search projects by text (name, address, etc.).
     *
     * @param searchText Search text
     * @param pageable   Pagination information
     * @return Page of matching project summaries
     */
    @Transactional(readOnly = true)
    public Page<ProjectSummary> searchProjects(String searchText, Pageable pageable) {
        log.debug("Searching projects with text: {}", searchText);

        Page<Project> projectPage = projectRepository.searchByText(searchText, pageable);
        return projectPage.map(this::mapToSummary);
    }

    /**
     * Get projects with GPS coordinates configured.
     *
     * @return List of projects with GPS
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> getProjectsWithGps() {
        log.debug("Fetching projects with GPS coordinates");

        List<Project> projectsWithGps = projectRepository.findProjectsWithGps();
        return projectsWithGps.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a project (soft delete).
     *
     * @param projectCode Project code
     */
    @Transactional
    public void deactivateProject(Long projectCode) {
        log.info("Deactivating project: {}", projectCode);

        Project project = projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + projectCode));

        // Check if project has active employees
        long activeEmployees = employeeRepository.countByPrimaryProjectCode(projectCode);
        if (activeEmployees > 0) {
            throw new BadRequestException(
                    "Cannot deactivate project with " + activeEmployees + " active employees assigned");
        }

        project.setProjectStatus("CANCELLED");
        projectRepository.save(project);

        log.info("Project {} deactivated successfully", projectCode);
    }

    // ==================== Mapping Methods ====================

    /**
     * Map Project entity to ProjectResponse DTO.
     */
    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = ProjectResponse.builder()
                .projectCode(project.getProjectCode())
                .projectName(project.getProjectName())
                .projectAddress(project.getProjectAddress())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .totalProjectAmount(project.getTotalProjectAmount())
                .projectProfitMargin(project.getProjectProfitMargin())
                .projectLatitude(project.getProjectLatitude())
                .projectLongitude(project.getProjectLongitude())
                .gpsRadiusMeters(project.getGpsRadiusMeters())
                .requireGpsCheck(project.getRequireGpsCheck())
                .hasGpsCoordinates(project.hasGpsCoordinates())
                .noOfPayments(project.getNoOfPayments())
                .firstDownPaymentDate(project.getFirstDownPaymentDate())
                .projectMgr(project.getProjectMgr())
                .technoSuffix(project.getTechnoSuffix())
                .projectStatus(project.getProjectStatus())
                .durationDays(project.getDurationDays())
                .remainingDays(project.getRemainingDays())
                .completionPercentage(project.getCompletionPercentage())
                .isActive(project.isActive())
                .isCompleted(project.isCompleted())
                .isOngoing(project.isOngoing())
                .hasStarted(project.hasStarted())
                .hasEnded(project.hasEnded())
                .createdDate(project.getCreatedDate())
                .createdBy(project.getCreatedBy())
                .modifiedDate(project.getModifiedDate())
                .modifiedBy(project.getModifiedBy())
                .build();

        // Add project manager details if available
        if (project.getProjectManager() != null) {
            response.setProjectManagerName(project.getProjectManager().getEmployeeName());
            response.setProjectManagerName(project.getProjectManager().getEmployeeName());
        }

        // Find and set schedule ID assigned to this project
        List<TimeSchedule> projectSchedules = timeScheduleRepository.findByProjectCode(project.getProjectCode());
        if (!projectSchedules.isEmpty()) {
            // Get the first active schedule assigned to this project
            TimeSchedule assignedSchedule = projectSchedules.stream()
                    .filter(schedule -> "Y".equals(schedule.getIsActive()))
                    .findFirst()
                    .orElse(projectSchedules.get(0)); // Fallback to first schedule if no active one
            response.setScheduleId(assignedSchedule.getScheduleId());
            log.info("Found schedule {} assigned to project {}", assignedSchedule.getScheduleId(), project.getProjectCode());
        } else {
            // Explicitly set to null so frontend knows no schedule is assigned
            response.setScheduleId(null);
            log.debug("No schedule assigned to project {} - scheduleId set to null", project.getProjectCode());
        }

        return response;
    }

    /**
     * Map Project entity to ProjectSummary DTO (lightweight).
     */
    private ProjectSummary mapToSummary(Project project) {
        ProjectSummary summary = ProjectSummary.builder()
                .projectCode(project.getProjectCode())
                .projectName(project.getProjectName())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .totalProjectAmount(project.getTotalProjectAmount())
                .projectStatus(project.getProjectStatus())
                .projectMgr(project.getProjectMgr())
                .projectAddress(project.getProjectAddress())
                .hasGpsCoordinates(project.hasGpsCoordinates())
                .durationDays(project.getDurationDays())
                .remainingDays(project.getRemainingDays())
                .completionPercentage(project.getCompletionPercentage())
                .isActive(project.isActive())
                .isOngoing(project.isOngoing())
                .build();

        // Add project manager name if available
        if (project.getProjectManager() != null) {
            summary.setProjectManagerName(project.getProjectManager().getEmployeeName());
        }

        return summary;
    }

    /**
     * Assigns a time schedule to a project.
     * Removes any existing schedule assignment for this project first.
     *
     * @param projectCode Project code
     * @param scheduleId  Schedule ID to assign (null to remove assignment)
     */
    /**
     * Helper to assign a schedule to a project.
     * Removes any existing schedule assignment for the project first.
     *
     * @param projectCode Project code
     * @param scheduleId  Schedule ID to assign (null to remove all assignments)
     */
    private void assignScheduleToProject(Long projectCode, Long scheduleId) {
        // First, remove any existing schedule assignment for this project
        List<TimeSchedule> existingSchedules = timeScheduleRepository.findByProjectCode(projectCode);
        for (TimeSchedule existingSchedule : existingSchedules) {
            existingSchedule.setProjectCode(null);
            timeScheduleRepository.save(existingSchedule);
            log.info("Removed schedule {} from project {}", existingSchedule.getScheduleId(), projectCode);
        }

        // If scheduleId is null, we're just removing assignments (already done above)
        if (scheduleId == null) {
            log.info("Removed all schedule assignments from project {}", projectCode);
            return;
        }

        // Assign the new schedule to the project
        Optional<TimeSchedule> scheduleOpt = timeScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isPresent()) {
            TimeSchedule schedule = scheduleOpt.get();
            // Check if schedule is already assigned to another project
            if (schedule.getProjectCode() != null && !schedule.getProjectCode().equals(projectCode)) {
                log.warn("Schedule {} is already assigned to project {}. Reassigning to project {}.",
                        schedule.getScheduleId(), schedule.getProjectCode(), projectCode);
            }
            schedule.setProjectCode(projectCode);
            timeScheduleRepository.save(schedule);
            log.info("Assigned schedule {} ({} hours) to project {}", 
                    schedule.getScheduleId(), schedule.getRequiredHours(), projectCode);
        } else {
            log.warn("Schedule {} not found, skipping assignment to project {}", scheduleId, projectCode);
            throw new ResourceNotFoundException("الجدول الزمني غير موجود برقم: " + scheduleId);
        }
    }

    /**
     * Public method to assign a schedule to a project.
     * Can be called directly or through API endpoint.
     *
     * @param projectCode Project code
     * @param scheduleId  Schedule ID to assign (null to remove assignment)
     */
    @Transactional
    public void assignSchedule(Long projectCode, Long scheduleId) {
        // Validate project exists
        if (!projectRepository.existsById(projectCode)) {
            throw new ResourceNotFoundException("المشروع غير موجود برقم: " + projectCode);
        }
        
        assignScheduleToProject(projectCode, scheduleId);
    }

    /**
     * Helper to round coordinates to 8 decimal places to match DB schema.
     */
    private BigDecimal roundCoordinate(BigDecimal value) {
        if (value == null)
            return null;
        return value.setScale(8, java.math.RoundingMode.HALF_UP);
    }
}
