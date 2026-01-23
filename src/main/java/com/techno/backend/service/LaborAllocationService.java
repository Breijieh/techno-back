package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.dto.labor.*;
import com.techno.backend.entity.*;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.AssignmentOverlapException;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Labor Allocation Management.
 * 
 * @since Phase 10 - Projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LaborAllocationService {

    private final ProjectLaborRequestHeaderRepository headerRepository;
    private final ProjectLaborRequestDetailRepository detailRepository;
    private final ProjectLaborAssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Transactional
    public LaborRequestResponse createLaborRequest(LaborRequestDto request, Long requestedBy) {
        log.info("Creating labor request for project: {}", request.getProjectCode());

        projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + request.getProjectCode()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        ProjectLaborRequestHeader header = ProjectLaborRequestHeader.builder()
                .projectCode(request.getProjectCode())
                .requestDate(LocalDate.now())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .requestStatus("PENDING")
                .requestedBy(requestedBy)
                .requestNotes(request.getNotes())
                .build();

        // Initialize approval workflow
        Employee employee = employeeRepository.findById(requestedBy)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + requestedBy));

        Long deptCode = employee.getPrimaryDeptCode();
        // Use project code from request
        Long projectCode = request.getProjectCode();

        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                "LABOR_REQ", requestedBy, deptCode, projectCode);

        header.setNextApproval(approvalInfo.getNextApproval());
        header.setNextAppLevel(approvalInfo.getNextAppLevel());
        header.setTransStatus(approvalInfo.getTransStatus());

        header = headerRepository.save(header);

        Long requestNo = header.getRequestNo();
        for (LaborRequestDetailDto detailDto : request.getDetails()) {
            ProjectLaborRequestDetailId id = new ProjectLaborRequestDetailId(
                    requestNo, detailDto.getSequenceNo());

            ProjectLaborRequestDetail detail = ProjectLaborRequestDetail.builder()
                    .id(id)
                    .jobTitleAr(detailDto.getJobTitleAr())
                    .jobTitleEn(detailDto.getJobTitleEn())
                    .quantity(detailDto.getQuantity())
                    .dailyRate(detailDto.getDailyRate())
                    .assignedCount(0)
                    .positionNotes(detailDto.getNotes())
                    .build();

            detailRepository.save(detail);
        }

        log.info("Labor request created: {}", requestNo);
        return getRequestById(requestNo);
    }

    @Transactional
    public LaborAssignmentResponse assignLabor(LaborAssignmentDto request, Long assignedBy) {
        log.info("Assigning employee {} to project {}", request.getEmployeeNo(), request.getProjectCode());

        Employee employee = employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeNo()));

        projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + request.getProjectCode()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        List<ProjectLaborAssignment> overlapping = assignmentRepository.findOverlappingAssignments(
                request.getEmployeeNo(), request.getStartDate(), request.getEndDate());

        if (!overlapping.isEmpty()) {
            List<AssignmentOverlapInfo> overlapInfo = buildOverlapInfo(overlapping);

            // Build detailed error message
            AssignmentOverlapInfo firstOverlap = overlapInfo.get(0);
            String projectName = firstOverlap.getProjectName() != null
                    ? firstOverlap.getProjectName()
                    : "Project #" + firstOverlap.getProjectCode();

            String message = String.format(
                    "Employee has %d overlapping assignment(s). " +
                            "Assignment #%d: Project '%s' from %s to %s. " +
                            "Please adjust dates or end the previous assignment.",
                    overlapInfo.size(),
                    firstOverlap.getAssignmentNo(),
                    projectName,
                    firstOverlap.getStartDate(),
                    firstOverlap.getEndDate());

            throw new AssignmentOverlapException(message, overlapInfo);
        }

        ProjectLaborAssignment assignment = ProjectLaborAssignment.builder()
                .employeeNo(request.getEmployeeNo())
                .projectCode(request.getProjectCode())
                .requestNo(request.getRequestNo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .dailyRate(request.getDailyRate())
                .build();

        assignment = assignmentRepository.save(assignment);

        if (request.getRequestNo() != null && request.getSequenceNo() != null) {
            ProjectLaborRequestDetailId detailId = new ProjectLaborRequestDetailId(
                    request.getRequestNo(), request.getSequenceNo());

            detailRepository.findById(detailId).ifPresent(detail -> {
                detail.setAssignedCount(detail.getAssignedCount() + 1);
                detailRepository.save(detail);
            });
        }

        log.info("Labor assigned: Assignment#{}", assignment.getAssignmentNo());
        return getAssignmentById(assignment.getAssignmentNo());
    }

    @Transactional(readOnly = true)
    public LaborRequestResponse getRequestById(Long requestNo) {
        ProjectLaborRequestHeader header = headerRepository.findById(requestNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor request not found: " + requestNo));

        List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(requestNo);
        Project project = projectRepository.findById(header.getProjectCode()).orElse(null);

        return mapHeaderToResponse(header, details, project);
    }

    /**
     * Update a labor request.
     * Can only update OPEN requests that haven't been approved yet.
     * 
     * @param requestNo Request number
     * @param request   Update request
     * @return Updated labor request
     */
    @Transactional
    public LaborRequestResponse updateLaborRequest(Long requestNo, LaborRequestDto request) {
        log.info("Updating labor request: {}", requestNo);

        ProjectLaborRequestHeader header = headerRepository.findById(requestNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor request not found: " + requestNo));

        // Can only update OPEN requests that haven't been approved or cancelled
        if (header.isCancelled()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ø·Ù„Ø¨ Ø¹Ù…Ø§Ù„Ø© Ù…Ø±ÙÙˆØ¶ Ø£Ùˆ Ù…Ù„ØºÙŠ");
        }

        if (header.getApprovedBy() != null) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ø·Ù„Ø¨ Ø¹Ù…Ø§Ù„Ø© Ù…Ø¹ØªÙ…Ø¯");
        }

        if (!"OPEN".equals(header.getRequestStatus())) {
            throw new BadRequestException(
                    "ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¹Ù…Ø§Ù„Ø© Ø§Ù„Ù…ÙØªÙˆØ­Ø© ÙÙ‚Ø·. Ø§Ù„Ø­Ø§Ù„Ø© Ø§Ù„Ø­Ø§Ù„ÙŠØ©: " + header.getRequestStatus());
        }

        // Validate project exists
        projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + request.getProjectCode()));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        // Update header fields
        header.setProjectCode(request.getProjectCode());
        header.setStartDate(request.getStartDate());
        header.setEndDate(request.getEndDate());
        if (request.getNotes() != null) {
            header.setRequestNotes(request.getNotes());
        }

        header = headerRepository.save(header);

        // Delete existing details
        List<ProjectLaborRequestDetail> existingDetails = detailRepository.findByRequestNo(requestNo);
        detailRepository.deleteAll(existingDetails);

        // Create new details
        for (LaborRequestDetailDto detailDto : request.getDetails()) {
            ProjectLaborRequestDetailId id = new ProjectLaborRequestDetailId(
                    requestNo, detailDto.getSequenceNo());

            ProjectLaborRequestDetail detail = ProjectLaborRequestDetail.builder()
                    .id(id)
                    .jobTitleAr(detailDto.getJobTitleAr())
                    .jobTitleEn(detailDto.getJobTitleEn())
                    .quantity(detailDto.getQuantity())
                    .dailyRate(detailDto.getDailyRate())
                    .assignedCount(0) // Reset assigned count when updating
                    .positionNotes(detailDto.getNotes())
                    .build();

            detailRepository.save(detail);
        }

        log.info("Labor request {} updated successfully", requestNo);

        List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(requestNo);
        Project project = projectRepository.findById(header.getProjectCode()).orElse(null);

        return mapHeaderToResponse(header, details, project);
    }

    @Transactional(readOnly = true)
    public LaborAssignmentResponse getAssignmentById(Long assignmentNo) {
        ProjectLaborAssignment assignment = assignmentRepository.findById(assignmentNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found: " + assignmentNo));

        Employee employee = employeeRepository.findById(assignment.getEmployeeNo()).orElse(null);
        Project project = projectRepository.findById(assignment.getProjectCode()).orElse(null);

        return mapAssignmentToResponse(assignment, employee, project);
    }

    @Transactional(readOnly = true)
    public List<LaborRequestResponse> getRequestsByProject(Long projectCode) {
        List<ProjectLaborRequestHeader> headers = headerRepository.findByProjectCode(projectCode);
        Project project = projectRepository.findById(projectCode).orElse(null);

        return headers.stream()
                .map(header -> {
                    List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(header.getRequestNo());
                    return mapHeaderToResponse(header, details, project);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LaborRequestResponse> getOpenRequests() {
        List<ProjectLaborRequestHeader> headers = headerRepository.findOpenRequests();

        return headers.stream()
                .map(header -> {
                    List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(header.getRequestNo());
                    Project project = projectRepository.findById(header.getProjectCode()).orElse(null);
                    return mapHeaderToResponse(header, details, project);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LaborRequestResponse> getAllLaborRequests() {
        List<ProjectLaborRequestHeader> headers = headerRepository.findAll()
                .stream()
                .filter(header -> !header.isDeleted())
                .collect(Collectors.toList());

        return headers.stream()
                .map(header -> {
                    List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(header.getRequestNo());
                    Project project = projectRepository.findById(header.getProjectCode()).orElse(null);
                    return mapHeaderToResponse(header, details, project);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LaborAssignmentResponse> getAssignmentsByProject(Long projectCode) {
        List<ProjectLaborAssignment> assignments = assignmentRepository.findByProjectCode(projectCode);
        Project project = projectRepository.findById(projectCode).orElse(null);

        return assignments.stream()
                .map(assignment -> {
                    Employee employee = employeeRepository.findById(assignment.getEmployeeNo()).orElse(null);
                    return mapAssignmentToResponse(assignment, employee, project);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LaborAssignmentResponse> getAssignmentsByEmployee(Long employeeNo) {
        List<ProjectLaborAssignment> assignments = assignmentRepository.findByEmployeeNo(employeeNo);
        Employee employee = employeeRepository.findById(employeeNo).orElse(null);

        return assignments.stream()
                .map(assignment -> {
                    Project project = projectRepository.findById(assignment.getProjectCode()).orElse(null);
                    return mapAssignmentToResponse(assignment, employee, project);
                })
                .collect(Collectors.toList());
    }

    // Mapping Methods

    private LaborRequestResponse mapHeaderToResponse(ProjectLaborRequestHeader header,
            List<ProjectLaborRequestDetail> details,
            Project project) {
        List<LaborRequestDetailResponse> detailResponses = details.stream()
                .map(this::mapDetailToResponse)
                .collect(Collectors.toList());

        int totalPositions = details.stream().mapToInt(ProjectLaborRequestDetail::getQuantity).sum();
        int totalAssigned = details.stream().mapToInt(ProjectLaborRequestDetail::getAssignedCount).sum();

        LaborRequestResponse response = LaborRequestResponse.builder()
                .requestNo(header.getRequestNo())
                .projectCode(header.getProjectCode())
                .requestDate(header.getRequestDate())
                .startDate(header.getStartDate())
                .endDate(header.getEndDate())
                .requestStatus(header.getRequestStatus())
                .requestedBy(header.getRequestedBy())
                .details(detailResponses)
                .totalPositions(totalPositions)
                .totalAssigned(totalAssigned)
                .remainingPositions(totalPositions - totalAssigned)
                .createdDate(header.getCreatedDate())
                .createdBy(header.getCreatedBy())
                .modifiedDate(header.getModifiedDate())
                .modifiedBy(header.getModifiedBy())
                .build();

        if (project != null) {
            response.setProjectName(project.getProjectName());
            response.setProjectName(project.getProjectName());
        }

        if (header.getRequestedBy() != null) {
            employeeRepository.findById(header.getRequestedBy())
                    .ifPresent(e -> response.setRequestedByName(e.getEmployeeName()));
        }

        if (header.getApprovedBy() != null) {
            response.setApprovedBy(header.getApprovedBy());
            response.setApprovalDate(header.getApprovalDate());
            employeeRepository.findById(header.getApprovedBy())
                    .ifPresent(e -> response.setApprovedByName(e.getEmployeeName()));
        }

        if (header.getRequestNotes() != null) {
            response.setNotes(header.getRequestNotes());
        }

        return response;
    }

    private LaborRequestDetailResponse mapDetailToResponse(ProjectLaborRequestDetail detail) {
        return LaborRequestDetailResponse.builder()
                .requestNo(detail.getId().getRequestNo())
                .sequenceNo(detail.getId().getSequenceNo())
                .jobTitleAr(detail.getJobTitleAr())
                .jobTitleEn(detail.getJobTitleEn())
                .quantity(detail.getQuantity())
                .dailyRate(detail.getDailyRate())
                .assignedCount(detail.getAssignedCount())
                .remainingCount(detail.getRemainingPositions())
                .notes(detail.getPositionNotes())
                .isFullyAssigned(detail.isFullyAssigned())
                .build();
    }

    private LaborAssignmentResponse mapAssignmentToResponse(ProjectLaborAssignment assignment,
            Employee employee, Project project) {
        return mapAssignmentToResponse(assignment, employee, project, null);
    }

    private LaborAssignmentResponse mapAssignmentToResponse(ProjectLaborAssignment assignment,
            Employee employee, Project project,
            Map<Long, Department> departmentsMap) {
        long totalDays = ChronoUnit.DAYS.between(assignment.getStartDate(), assignment.getEndDate()) + 1;
        BigDecimal totalCost = assignment.getDailyRate().multiply(BigDecimal.valueOf(totalDays));

        LaborAssignmentResponse response = LaborAssignmentResponse.builder()
                .assignmentNo(assignment.getAssignmentNo())
                .employeeNo(assignment.getEmployeeNo())
                .projectCode(assignment.getProjectCode())
                .requestNo(assignment.getRequestNo())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .dailyRate(assignment.getDailyRate())
                .totalDays(totalDays)
                .totalCost(totalCost)
                .isActive(assignment.isActive() && !assignment.getEndDate().isBefore(LocalDate.now()))
                .assignmentStatus(assignment.getAssignmentStatus())
                .createdDate(assignment.getCreatedDate())
                .createdBy(assignment.getCreatedBy())
                .modifiedDate(assignment.getModifiedDate())
                .modifiedBy(assignment.getModifiedBy())
                .build();

        if (employee != null) {
            response.setEmployeeName(employee.getEmployeeName());
            response.setEmployeeName(employee.getEmployeeName());
        }

        if (project != null) {
            response.setProjectName(project.getProjectName());
            response.setProjectName(project.getProjectName());
        }

        // Fetch job title (specialization) from linked labor request detail if
        // available
        // If assignment is linked to a request, fetch the detail to get job title
        if (assignment.getRequestNo() != null) {
            // Try to find detail by matching daily rate if sequenceNo is not available
            List<ProjectLaborRequestDetail> requestDetails = detailRepository
                    .findByRequestNo(assignment.getRequestNo());
            if (!requestDetails.isEmpty()) {
                // If we have details, try to match by daily rate or use the first one
                ProjectLaborRequestDetail matchingDetail = requestDetails.stream()
                        .filter(detail -> detail.getDailyRate().equals(assignment.getDailyRate()))
                        .findFirst()
                        .orElse(requestDetails.get(0));

                response.setJobTitleEn(matchingDetail.getJobTitleEn());
                response.setJobTitleAr(matchingDetail.getJobTitleAr());
                response.setSequenceNo(matchingDetail.getId().getSequenceNo());
            }
        }

        // Fallback: If no job title from request detail, use employee's department name
        // This handles direct assignments (not linked to a labor request)
        if ((response.getJobTitleEn() == null || response.getJobTitleEn().trim().isEmpty())
                && employee != null) {
            Department department = null;

            // Try to get department from map first (if provided for batch operations)
            if (departmentsMap != null && employee.getPrimaryDeptCode() != null) {
                department = departmentsMap.get(employee.getPrimaryDeptCode());
            }

            // If not in map, try to get from employee's primaryDepartment relationship
            if (department == null && employee.getPrimaryDepartment() != null) {
                department = employee.getPrimaryDepartment();
            }

            // If still not found and we have dept code, fetch from repository
            if (department == null && employee.getPrimaryDeptCode() != null) {
                department = departmentRepository.findById(employee.getPrimaryDeptCode()).orElse(null);
            }

            // Set department names if found
            if (department != null) {
                response.setJobTitleEn(department.getDeptName());
                response.setJobTitleAr(department.getDeptName());
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<LaborAssignmentResponse> getAllAssignments() {
        List<ProjectLaborAssignment> assignments = assignmentRepository.findAll()
                .stream()
                .filter(a -> !a.isDeleted())
                .collect(Collectors.toList());

        // Batch fetch employees and projects to avoid N+1 queries
        List<Long> employeeNos = assignments.stream()
                .map(ProjectLaborAssignment::getEmployeeNo)
                .distinct()
                .collect(Collectors.toList());
        List<Long> projectCodes = assignments.stream()
                .map(ProjectLaborAssignment::getProjectCode)
                .distinct()
                .collect(Collectors.toList());

        // Fetch all unique department codes from employees
        List<Employee> employees = employeeRepository.findAllById(employeeNos);
        List<Long> deptCodes = employees.stream()
                .map(Employee::getPrimaryDeptCode)
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch departments
        Map<Long, Department> departmentsMap = deptCodes.isEmpty()
                ? new HashMap<>()
                : departmentRepository.findAllById(deptCodes)
                        .stream()
                        .collect(Collectors.toMap(Department::getDeptCode, dept -> dept));

        // Create employees map for quick lookup
        Map<Long, Employee> employeesMap = employees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeNo, emp -> emp));

        // Fetch all projects
        Map<Long, Project> projectsMap = projectRepository.findAllById(projectCodes)
                .stream()
                .collect(Collectors.toMap(Project::getProjectCode, proj -> proj));

        return assignments.stream()
                .map(assignment -> {
                    Employee employee = employeesMap.get(assignment.getEmployeeNo());
                    Project project = projectsMap.get(assignment.getProjectCode());
                    return mapAssignmentToResponse(assignment, employee, project, departmentsMap);
                })
                .collect(Collectors.toList());
    }

    /**
     * Build assignment overlap information for error responses.
     * Fetches project information for all overlapping assignments.
     * 
     * @param overlapping List of overlapping assignments
     * @return List of AssignmentOverlapInfo DTOs with project names
     */
    private List<AssignmentOverlapInfo> buildOverlapInfo(List<ProjectLaborAssignment> overlapping) {
        if (overlapping == null || overlapping.isEmpty()) {
            return List.of();
        }

        // Batch fetch projects to avoid N+1 queries
        List<Long> projectCodes = overlapping.stream()
                .map(ProjectLaborAssignment::getProjectCode)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Project> projectsMap = projectRepository.findAllById(projectCodes)
                .stream()
                .collect(Collectors.toMap(Project::getProjectCode, proj -> proj));

        return overlapping.stream()
                .map(assignment -> {
                    Project project = projectsMap.get(assignment.getProjectCode());
                    return AssignmentOverlapInfo.builder()
                            .assignmentNo(assignment.getAssignmentNo())
                            .projectCode(assignment.getProjectCode())
                            .projectName(project != null ? project.getProjectName() : null)
                            .projectName(project != null ? project.getProjectName() : null)
                            .startDate(assignment.getStartDate())
                            .endDate(assignment.getEndDate())
                            .assignmentStatus(assignment.getAssignmentStatus())
                            .dailyRate(assignment.getDailyRate())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Update a labor assignment.
     * 
     * @param assignmentNo Assignment number
     * @param request      Update request
     * @param updatedBy    Employee number of updater
     * @return Updated labor assignment
     */
    @Transactional
    public LaborAssignmentResponse updateLaborAssignment(Long assignmentNo, LaborAssignmentDto request,
            Long updatedBy) {
        log.info("Updating labor assignment: {} by: {}", assignmentNo, updatedBy);

        ProjectLaborAssignment assignment = assignmentRepository.findById(assignmentNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor assignment not found: " + assignmentNo));

        if (assignment.isDeleted()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ù…Ù‡Ù…Ø© Ù…Ø­Ø°ÙˆÙØ©");
        }

        // Validate employee exists
        Employee employee = employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeNo()));

        // Validate project exists
        projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + request.getProjectCode()));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡");
        }

        // Check for overlapping assignments (excluding current assignment)
        List<ProjectLaborAssignment> overlapping = assignmentRepository.findOverlappingAssignments(
                request.getEmployeeNo(), request.getStartDate(), request.getEndDate())
                .stream()
                .filter(a -> !a.getAssignmentNo().equals(assignmentNo))
                .collect(Collectors.toList());

        if (!overlapping.isEmpty()) {
            List<AssignmentOverlapInfo> overlapInfo = buildOverlapInfo(overlapping);

            // Build detailed error message
            AssignmentOverlapInfo firstOverlap = overlapInfo.get(0);
            String projectName = firstOverlap.getProjectName() != null
                    ? firstOverlap.getProjectName()
                    : "Project #" + firstOverlap.getProjectCode();

            String message = String.format(
                    "Employee has %d overlapping assignment(s). " +
                            "Assignment #%d: Project '%s' from %s to %s. " +
                            "Please adjust dates or end the previous assignment.",
                    overlapInfo.size(),
                    firstOverlap.getAssignmentNo(),
                    projectName,
                    firstOverlap.getStartDate(),
                    firstOverlap.getEndDate());

            throw new AssignmentOverlapException(message, overlapInfo);
        }

        // Validate and set requestNo: if provided, must exist; otherwise set to null
        Long requestNoToSet = null;
        if (request.getRequestNo() != null) {
            // Check if the request exists
            if (headerRepository.existsById(request.getRequestNo())) {
                requestNoToSet = request.getRequestNo();
            } else {
                // If request doesn't exist, log a warning and set to null (clear invalid
                // reference)
                log.warn("Labor request {} not found for assignment {}. Clearing request reference.",
                        request.getRequestNo(), assignmentNo);
                requestNoToSet = null;
            }
        }

        // Update assignment fields
        assignment.setEmployeeNo(request.getEmployeeNo());
        assignment.setProjectCode(request.getProjectCode());
        assignment.setRequestNo(requestNoToSet);
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignment.setDailyRate(request.getDailyRate());
        if (request.getNotes() != null) {
            assignment.setAssignmentNotes(request.getNotes());
        }

        assignment = assignmentRepository.save(assignment);
        log.info("Labor assignment {} updated successfully", assignmentNo);

        Project project = projectRepository.findById(assignment.getProjectCode()).orElse(null);
        return mapAssignmentToResponse(assignment, employee, project);
    }

    /**
     * Delete a labor assignment (soft delete).
     * 
     * @param assignmentNo Assignment number
     * @param deletedBy    Employee number of deleter
     */
    @Transactional
    public void deleteLaborAssignment(Long assignmentNo, Long deletedBy) {
        log.info("Deleting labor assignment: {} by: {}", assignmentNo, deletedBy);

        ProjectLaborAssignment assignment = assignmentRepository.findById(assignmentNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor assignment not found: " + assignmentNo));

        if (assignment.isDeleted()) {
            throw new BadRequestException("Ø§Ù„Ù…Ù‡Ù…Ø© Ù…Ø­Ø°ÙˆÙØ© Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Soft delete
        assignment.softDelete();
        assignmentRepository.save(assignment);

        // If assignment was linked to a request detail, decrement assigned count
        if (assignment.getRequestNo() != null) {
            // Note: We would need sequenceNo to update detail, but we can skip for now
            // or find the detail by matching employee and dates
        }

        log.info("Labor assignment {} deleted successfully", assignmentNo);
    }

    /**
     * Approve a labor request.
     * Sets approvedBy, approvalDate, and updates request notes if provided.
     * 
     * @param requestNo  Request number
     * @param approverNo Approver employee number
     * @param notes      Optional approval notes
     * @return Updated labor request
     */
    @Transactional
    public LaborRequestResponse approveLaborRequest(Long requestNo, Long approverNo, String notes) {
        log.info("Approving labor request: {} by approver: {}", requestNo, approverNo);

        ProjectLaborRequestHeader header = headerRepository.findById(requestNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor request not found: " + requestNo));

        if (header.getApprovedBy() != null) {
            throw new BadRequestException("ØªÙ… Ø§Ø¹ØªÙ…Ø§Ø¯ Ø·Ù„Ø¨ Ø§Ù„Ø¹Ù…Ø§Ù„Ø© Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        if (header.isCancelled()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø§Ø¹ØªÙ…Ø§Ø¯ Ø·Ù„Ø¨ Ø¹Ù…Ø§Ù„Ø© Ù…Ù„ØºÙŠ");
        }

        if (header.isClosed()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø§Ø¹ØªÙ…Ø§Ø¯ Ø·Ù„Ø¨ Ø¹Ù…Ø§Ù„Ø© Ù…ØºÙ„Ù‚");
        }

        // Check if approver is the expected next approver
        if (header.getNextApproval() != null && !header.getNextApproval().equals(approverNo)) {
            throw new BadRequestException(
                    "Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù… ØºÙŠØ± Ù…Ø®ÙˆÙ„ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© ÙÙŠ Ù‡Ø°Ø§ Ø§Ù„Ù…Ø³ØªÙˆÙ‰. Ø§Ù„Ù…ØªÙˆÙ‚Ø¹: " + header.getNextApproval());
        }

        Employee employee = employeeRepository.findById(header.getRequestedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù Ø§Ù„Ø·Ø§Ù„Ø¨ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        Long deptCode = employee.getPrimaryDeptCode();

        // Move to next approval level
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.moveToNextLevel(
                "LABOR_REQ", header.getNextAppLevel(), header.getRequestedBy(), deptCode, header.getProjectCode());

        // Update workflow fields
        header.setNextApproval(approvalInfo.getNextApproval());
        header.setNextAppLevel(approvalInfo.getNextAppLevel());
        header.setTransStatus(approvalInfo.getTransStatus());

        // Update notes if provided
        if (notes != null && !notes.trim().isEmpty()) {
            String existingNotes = header.getRequestNotes() != null ? header.getRequestNotes() : "";
            String newNotes = existingNotes.isEmpty() ? notes : existingNotes + "\nApproval Notes: " + notes;
            header.setRequestNotes(newNotes);
        }

        if (approvalInfo.getNextApproval() == null) {
            // Final approval
            header.setApprovedBy(approverNo);
            header.setApprovalDate(LocalDate.now());
            // Change status to OPEN so assignments can start
            header.setRequestStatus("OPEN");
            log.info("Labor request {} fully approved by {}", requestNo, approverNo);
        } else {
            log.info("Labor request {} approved at level {}, moving to next level", requestNo,
                    header.getNextAppLevel());
        }

        header = headerRepository.save(header);

        List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(requestNo);
        Project project = projectRepository.findById(header.getProjectCode()).orElse(null);

        return mapHeaderToResponse(header, details, project);
    }

    /**
     * Reject a labor request.
     * Sets status to CANCELLED and updates request notes with rejection reason.
     * 
     * @param requestNo  Request number
     * @param approverNo Approver employee number
     * @param reason     Rejection reason
     * @return Updated labor request
     */
    @Transactional
    public LaborRequestResponse rejectLaborRequest(Long requestNo, Long approverNo, String reason) {
        log.info("Rejecting labor request: {} by approver: {}, reason: {}", requestNo, approverNo, reason);

        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨");
        }

        ProjectLaborRequestHeader header = headerRepository.findById(requestNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Labor request not found: " + requestNo));

        if (header.isCancelled()) {
            throw new BadRequestException("ØªÙ… Ø¥Ù„ØºØ§Ø¡ Ø·Ù„Ø¨ Ø§Ù„Ø¹Ù…Ø§Ù„Ø© Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        if (header.isClosed()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø±ÙØ¶ Ø·Ù„Ø¨ Ø¹Ù…Ø§Ù„Ø© Ù…ØºÙ„Ù‚");
        }

        // Cancel the request
        // Reject the request
        header.setTransStatus("R");
        header.setRequestStatus("CANCELLED");

        // Update notes with rejection reason
        String existingNotes = header.getRequestNotes() != null ? header.getRequestNotes() : "";
        String newNotes = existingNotes.isEmpty()
                ? "Rejected by: " + approverNo + ". Reason: " + reason
                : existingNotes + "\nRejected by: " + approverNo + ". Reason: " + reason;
        header.setRequestNotes(newNotes);

        header = headerRepository.save(header);
        log.info("Labor request {} rejected successfully by {}", requestNo, approverNo);

        List<ProjectLaborRequestDetail> details = detailRepository.findByRequestNo(requestNo);
        Project project = projectRepository.findById(header.getProjectCode()).orElse(null);

        return mapHeaderToResponse(header, details, project);
    }
}

