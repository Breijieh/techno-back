package com.techno.backend.controller;

import com.techno.backend.dto.*;
import com.techno.backend.entity.Attachment;
import com.techno.backend.entity.Employee;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.service.DocumentExpiryAlertService;
import com.techno.backend.service.EmployeeService;
import com.techno.backend.service.FileStorageService;
import com.techno.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.techno.backend.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Employee Management.
 * Provides endpoints for employee CRUD operations, search, and document
 * management.
 *
 * Base URL: /api/employees
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;
    private final EmployeeRepository employeeRepository;
    private final DocumentExpiryAlertService documentExpiryAlertService;
    private final UserService userService;

    /**
     * Get all employees with pagination and sorting
     *
     * @param page          Page number (default: 0)
     * @param size          Page size (default: 20)
     * @param sortBy        Sort field (default: employeeNo)
     * @param sortDirection Sort direction (default: asc)
     * @return Paginated list of employees
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeListResponse>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "employeeNo") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("GET /api/employees - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                page, size, sortBy, sortDirection);

        // For EMPLOYEE role, ensure they can only access their own employee data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EmployeeListResponse response;

        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            // Employee role - return only their own employee record
            Long currentEmployeeNo = getCurrentEmployeeNo();
            log.info("Employee role detected, returning only employee number: {}", currentEmployeeNo);

            EmployeeResponse employeeResponse = employeeService.getEmployeeById(currentEmployeeNo);
            List<EmployeeResponse> employeeList = List.of(employeeResponse);

            response = EmployeeListResponse.builder()
                    .employees(employeeList)
                    .totalElements(1L)
                    .totalPages(1)
                    .currentPage(0)
                    .pageSize(1)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        } else {
            // Other roles - return all employees as normal
            response = employeeService.getAllEmployees(page, size, sortBy, sortDirection);
        }

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Search employees with multiple filters
     *
     * @param searchRequest Search criteria
     * @return Filtered and paginated list of employees
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeListResponse>> searchEmployees(
            @RequestBody EmployeeSearchRequest searchRequest) {

        log.info("POST /api/employees/search - criteria: {}", searchRequest);

        EmployeeListResponse response = employeeService.searchEmployees(searchRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Ø§ÙƒØªÙ…Ù„ Ø§Ù„Ø¨Ø­Ø« Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Get current authenticated user's employee record
     *
     * @return Current user's employee details
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMyEmployee() {
        log.info("GET /api/employees/me - Getting current user's employee record");

        // Get current employee number from security context
        // This will throw ResourceNotFoundException if employeeNo is not found
        Long employeeNo = getCurrentEmployeeNo();

        // Fetch employee record (will throw ResourceNotFoundException if not found)
        EmployeeResponse response = employeeService.getEmployeeById(employeeNo);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø³Ø¬Ù„ Ø§Ù„Ù…ÙˆØ¸Ù Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Get employee by ID
     *
     * @param id Employee number
     * @return Employee details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Long id) {
        log.info("GET /api/employees/{}", id);

        // For EMPLOYEE role, ensure they can only access their own employee data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            Object principal = auth.getPrincipal();
            Long currentEmployeeNo = null;
            if (principal instanceof Long) {
                currentEmployeeNo = (Long) principal;
            } else if (principal instanceof String) {
                try {
                    currentEmployeeNo = Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    log.error("Invalid employee number in principal: {}", principal);
                }
            }
            if (currentEmployeeNo == null || !currentEmployeeNo.equals(id)) {
                throw new UnauthorizedException("ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø§Ù„ÙˆØµÙˆÙ„ ÙÙ‚Ø· Ø¥Ù„Ù‰ Ø¨ÙŠØ§Ù†Ø§ØªÙ‡Ù… Ø§Ù„Ø®Ø§ØµØ©");
            }
        }

        EmployeeResponse response = employeeService.getEmployeeById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ¸Ù Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Create new employee
     *
     * @param request Employee creation request
     * @return Created employee details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        log.info("POST /api/employees - Creating employee: {}", request.getEmployeeName());

        EmployeeResponse response = employeeService.createEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ø§Ù„Ù…ÙˆØ¸Ù Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Update existing employee
     *
     * @param id      Employee number
     * @param request Employee update request
     * @return Updated employee details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {

        log.info("PUT /api/employees/{} - Updating employee", id);

        EmployeeResponse response = employeeService.updateEmployee(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ù…ÙˆØ¸Ù Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Delete employee (soft delete - marks as TERMINATED)
     *
     * @param id Employee number
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        log.info("DELETE /api/employees/{}", id);

        employeeService.deleteEmployee(id);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø­Ø°Ù Ø§Ù„Ù…ÙˆØ¸Ù Ø¨Ù†Ø¬Ø§Ø­",
                null));
    }

    /**
     * Get employees by department
     *
     * @param deptCode Department code
     * @param page     Page number (default: 0)
     * @param size     Page size (default: 20)
     * @return Employees in the department
     */
    @GetMapping("/by-department/{deptCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeListResponse>> getEmployeesByDepartment(
            @PathVariable Long deptCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/employees/by-department/{}", deptCode);

        EmployeeListResponse response = employeeService.getEmployeesByDepartment(deptCode, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Get employees by project
     *
     * @param projectCode Project code
     * @param page        Page number (default: 0)
     * @param size        Page size (default: 20)
     * @return Employees in the project
     */
    @GetMapping("/by-project/{projectCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeListResponse>> getEmployeesByProject(
            @PathVariable Long projectCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/employees/by-project/{}", projectCode);

        EmployeeListResponse response = employeeService.getEmployeesByProject(projectCode, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø¨Ù†Ø¬Ø§Ø­",
                response));
    }

    /**
     * Get current employee number from security context.
     *
     * @return Employee number
     */
    private Long getCurrentEmployeeNo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
            // If principal is String (username), try to look up user
            if (principal instanceof String) {
                String username = (String) principal;
                try {
                    com.techno.backend.entity.UserAccount user = userService.findByUsername(username);
                    if (user.getEmployeeNo() != null) {
                        return user.getEmployeeNo();
                    }
                } catch (Exception e) {
                    log.error("Failed to find user or employee link for username: {}", username);
                }

                log.warn("Principal is String ({}) and no employee link found. User is not linked to an employee.",
                        principal);
                throw new ResourceNotFoundException("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø³Ø¬Ù„ Ù…ÙˆØ¸Ù Ù…Ø±ØªØ¨Ø· Ø¨Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù… Ø§Ù„Ø­Ø§Ù„ÙŠ");
            }
        }
        throw new ResourceNotFoundException("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ù…Ø¹Ù„ÙˆÙ…Ø§Øª Ø§Ù„Ù…ØµØ§Ø¯Ù‚Ø©");
    }

    /**
     * Get employees with expiring documents (passport or residency)
     *
     * @param daysThreshold Number of days to check (default: 14)
     * @param employeeNo    Optional employee number to filter by (for Employee
     *                      role, automatically set to current employee)
     * @return List of employees with expiring documents
     */
    @GetMapping("/expiring-docs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<DocumentExpiryResponse>>> getEmployeesWithExpiringDocuments(
            @RequestParam(required = false) Integer daysThreshold,
            @RequestParam(required = false) Long employeeNo) {

        log.info("GET /api/employees/expiring-docs - threshold: {} days, employeeNo: {}", daysThreshold, employeeNo);

        // For Employee role, automatically filter to their own employee number
        Long filterEmployeeNo = employeeNo;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            // Employee role - force filter to their own employee number
            filterEmployeeNo = getCurrentEmployeeNo();
            log.info("Employee role detected, filtering to employee number: {}", filterEmployeeNo);
        }

        List<DocumentExpiryResponse> response = employeeService.getEmployeesWithExpiringDocuments(daysThreshold,
                filterEmployeeNo);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("ØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ %d Ù…ÙˆØ¸ÙØ§Ù‹ Ù„Ø¯ÙŠÙ‡Ù… ÙˆØ«Ø§Ø¦Ù‚ Ù…Ù†ØªÙ‡ÙŠØ© Ø§Ù„ØµÙ„Ø§Ø­ÙŠØ©", response.size()),
                response));
    }

    /**
     * Get all documents/attachments for an employee
     *
     * @param id Employee number
     * @return List of employee attachments
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<Attachment>>> getEmployeeDocuments(@PathVariable Long id) {
        log.info("GET /api/employees/{}/documents", id);

        List<Attachment> attachments = fileStorageService.getEmployeeAttachments(id);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("ØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ %d ÙˆØ«ÙŠÙ‚Ø© Ù„Ù„Ù…ÙˆØ¸Ù", attachments.size()),
                attachments));
    }

    /**
     * Send reminder notification for expiring document
     *
     * @param employeeNo   Employee number
     * @param documentType Document type (PASSPORT or RESIDENCY)
     * @return Success response
     */
    @PostMapping("/{employeeNo}/documents/remind")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> sendDocumentReminder(
            @PathVariable Long employeeNo,
            @RequestParam String documentType) {
        log.info("POST /api/employees/{}/documents/remind - documentType: {}", employeeNo, documentType);

        // Validate documentType
        if (!documentType.equals("PASSPORT") && !documentType.equals("RESIDENCY")) {
            throw new IllegalArgumentException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø³ØªÙ†Ø¯ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† PASSPORT Ø£Ùˆ RESIDENCY");
        }

        // Fetch employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        // Send reminder via DocumentExpiryAlertService
        documentExpiryAlertService.sendManualDocumentReminder(employee, documentType);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("ØªÙ… Ø¥Ø±Ø³Ø§Ù„ ØªØ°ÙƒÙŠØ± Ù„ÙˆØ«ÙŠÙ‚Ø© %s Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ© Ø§Ù„ØµÙ„Ø§Ø­ÙŠØ©", documentType),
                null));
    }
}

