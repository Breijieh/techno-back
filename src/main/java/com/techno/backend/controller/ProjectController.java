package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.project.ProjectRequest;
import com.techno.backend.dto.project.ProjectResponse;
import com.techno.backend.dto.project.ProjectSummary;
import com.techno.backend.dto.project.ProjectUpdateRequest;
import com.techno.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Project Management.
 * Provides endpoints for project CRUD operations, search, and GPS-based queries.
 *
 * Base URL: /api/projects
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Get all projects with pagination and sorting.
     *
     * GET /api/projects?page=0&size=20&sortBy=projectCode&sortDirection=asc
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: projectCode)
     * @param sortDirection Sort direction (default: asc)
     * @return Paginated list of project summaries
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<ProjectSummary>>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "projectCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("GET /api/projects - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                page, size, sortBy, sortDirection);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProjectSummary> projects = projectService.getAllProjects(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…Ø´Ø§Ø±ÙŠØ¹ Ø¨Ù†Ø¬Ø§Ø­",
                projects
        ));
    }

    /**
     * Get active projects only.
     *
     * GET /api/projects/active
     *
     * @return List of active projects
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectSummary>>> getActiveProjects() {
        log.info("GET /api/projects/active");

        List<ProjectSummary> projects = projectService.getActiveProjects();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…Ø´Ø§Ø±ÙŠØ¹ Ø§Ù„Ù†Ø´Ø·Ø© Ø¨Ù†Ø¬Ø§Ø­",
                projects
        ));
    }

    /**
     * Get ongoing projects (currently within start and end dates).
     *
     * GET /api/projects/ongoing
     *
     * @return List of ongoing projects
     */
    @GetMapping("/ongoing")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectSummary>>> getOngoingProjects() {
        log.info("GET /api/projects/ongoing");

        List<ProjectSummary> projects = projectService.getOngoingProjects();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…Ø´Ø§Ø±ÙŠØ¹ Ø§Ù„Ø¬Ø§Ø±ÙŠØ© Ø¨Ù†Ø¬Ø§Ø­",
                projects
        ));
    }

    /**
     * Search projects by text (name, address, etc.).
     *
     * GET /api/projects/search?q=riyadh&page=0&size=20
     *
     * @param searchText Search query
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Matching projects
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<ProjectSummary>>> searchProjects(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/projects/search - query: {}, page: {}, size: {}", q, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectSummary> projects = projectService.searchProjects(q, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Ø§ÙƒØªÙ…Ù„ Ø§Ù„Ø¨Ø­Ø« Ø¨Ù†Ø¬Ø§Ø­",
                projects
        ));
    }

    /**
     * Get projects with GPS coordinates configured.
     *
     * GET /api/projects/with-gps
     *
     * @return List of projects with GPS
     */
    @GetMapping("/with-gps")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectSummary>>> getProjectsWithGps() {
        log.info("GET /api/projects/with-gps");

        List<ProjectSummary> projects = projectService.getProjectsWithGps();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…Ø´Ø§Ø±ÙŠØ¹ Ù…Ø¹ GPS Ø¨Ù†Ø¬Ø§Ø­",
                projects
        ));
    }

    /**
     * Get project by code.
     *
     * GET /api/projects/{id}
     *
     * @param id Project code
     * @return Project details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable Long id) {
        log.info("GET /api/projects/{}", id);

        ProjectResponse project = projectService.getProjectById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع المشروع بنجاح",
                project
        ));
    }

    /**
     * Create a new project.
     *
     * POST /api/projects
     *
     * @param request Project creation request
     * @return Created project details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request) {

        log.info("POST /api/projects - Creating project: {}", request.getProjectName());

        ProjectResponse project = projectService.createProject(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­",
                project
        ));
    }

    /**
     * Update an existing project.
     *
     * PUT /api/projects/{id}
     *
     * @param id Project code
     * @param request Update request
     * @return Updated project details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {

        log.info("PUT /api/projects/{} - Updating project", id);

        ProjectResponse project = projectService.updateProject(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­",
                project
        ));
    }

    /**
     * Deactivate a project (soft delete).
     *
     * DELETE /api/projects/{id}
     *
     * @param id Project code
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateProject(@PathVariable Long id) {
        log.info("DELETE /api/projects/{} - Deactivating project", id);

        projectService.deactivateProject(id);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­",
                null
        ));
    }

    /**
     * Assign a schedule to a project.
     *
     * PUT /api/projects/{id}/schedule
     *
     * @param id Project code
     * @param scheduleId Schedule ID to assign (can be null to remove assignment)
     * @return Success message
     */
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> assignScheduleToProject(
            @PathVariable Long id,
            @RequestParam(required = false) Long scheduleId) {
        log.info("PUT /api/projects/{}/schedule - Assigning schedule {} to project", id, scheduleId);

        projectService.assignSchedule(id, scheduleId);

        String message = scheduleId != null 
                ? "تم تعيين الجدول الزمني للمشروع بنجاح"
                : "تم إزالة الجدول الزمني من المشروع بنجاح";

        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}

