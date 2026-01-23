package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.DepartmentRequest;
import com.techno.backend.dto.DepartmentResponse;
import com.techno.backend.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Department Controller
 * Handles department-related endpoints
 */
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * GET /api/departments
     * List all departments
     * 
     * @return List of all departments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        log.debug("Fetching all departments");
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø£Ù‚Ø³Ø§Ù… Ø¨Ù†Ø¬Ø§Ø­", departments));
    }

    /**
     * GET /api/departments/{id}
     * Get department by ID
     * 
     * @param id the department code
     * @return DepartmentResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        log.debug("Fetching department with code: {}", id);
        DepartmentResponse department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù‚Ø³Ù… Ø¨Ù†Ø¬Ø§Ø­", department));
    }

    /**
     * POST /api/departments
     * Create new department (admin only)
     * 
     * @param request the department request
     * @return DepartmentResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        log.info("Creating department: {}", request.getDeptName());
        DepartmentResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ø§Ù„Ù‚Ø³Ù… Ø¨Ù†Ø¬Ø§Ø­", department));
    }

    /**
     * PUT /api/departments/{id}
     * Update department (admin only)
     * 
     * @param id the department code
     * @param request the department request
     * @return DepartmentResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        log.info("Updating department with code: {}", id);
        DepartmentResponse department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ù‚Ø³Ù… Ø¨Ù†Ø¬Ø§Ø­", department));
    }

    /**
     * DELETE /api/departments/{id}
     * Delete department (admin only - soft delete)
     * 
     * @param id the department code
     * @return success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteDepartment(@PathVariable Long id) {
        log.info("Deleting department with code: {}", id);
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø­Ø°Ù Ø§Ù„Ù‚Ø³Ù… Ø¨Ù†Ø¬Ø§Ø­", "ØªÙ… Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ø§Ù„Ù‚Ø³Ù…"));
    }

    /**
     * GET /api/departments/hierarchy
     * Get department hierarchy as tree structure
     * 
     * @return Tree structure of departments
     */
    @GetMapping("/hierarchy")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartmentHierarchy() {
        log.debug("Fetching department hierarchy (tree)");
        List<DepartmentResponse> hierarchy = departmentService.getDepartmentHierarchy();
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù‡ÙŠÙƒÙ„ Ø§Ù„Ø£Ù‚Ø³Ø§Ù… Ø¨Ù†Ø¬Ø§Ø­", hierarchy));
    }

    /**
     * GET /api/departments/hierarchy/flat
     * Get department hierarchy as flat list
     * 
     * @return Flat list of departments
     */
    @GetMapping("/hierarchy/flat")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartmentHierarchyFlat() {
        log.debug("Fetching department hierarchy (flat)");
        List<DepartmentResponse> departments = departmentService.getDepartmentHierarchyFlat();
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù‡ÙŠÙƒÙ„ Ø§Ù„Ø£Ù‚Ø³Ø§Ù… Ø¨Ù†Ø¬Ø§Ø­", departments));
    }

    /**
     * GET /api/departments/{id}/manager
     * Get department manager information
     * 
     * @param id the department code
     * @return DepartmentResponse with manager info
     */
    @GetMapping("/{id}/manager")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentManager(@PathVariable Long id) {
        log.debug("Fetching manager for department with code: {}", id);
        DepartmentResponse department = departmentService.getDepartmentManager(id);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù…Ø¯ÙŠØ± Ø§Ù„Ù‚Ø³Ù… Ø¨Ù†Ø¬Ø§Ø­", department));
    }
}



