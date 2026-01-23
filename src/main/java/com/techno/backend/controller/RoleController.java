package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.RoleListResponse;
import com.techno.backend.dto.RoleRequest;
import com.techno.backend.dto.RoleResponse;
import com.techno.backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Role Controller
 * Handles role and permission management endpoints
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    /**
     * GET /api/roles
     * Get all roles with pagination
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param sortBy sort field (default: roleId)
     * @param sortDirection sort direction (default: asc)
     * @return paginated list of roles
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<RoleListResponse>> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "roleId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        log.info("GET /api/roles - page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                page, size, sortBy, sortDirection);
        
        RoleListResponse response = roleService.getAllRoles(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع الأدوار بنجاح", response));
    }

    /**
     * GET /api/roles/{id}
     * Get role by ID
     * 
     * @param id the role ID
     * @return RoleResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        log.info("GET /api/roles/{}", id);
        
        RoleResponse response = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع الدور بنجاح", response));
    }

    /**
     * POST /api/roles
     * Create new role
     * 
     * @param request the role creation request
     * @return created RoleResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        log.info("POST /api/roles - request: {}", request);
        
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء الدور بنجاح", response));
    }

    /**
     * PUT /api/roles/{id}
     * Update role
     * 
     * @param id the role ID
     * @param request the update request
     * @return updated RoleResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        log.info("PUT /api/roles/{} - request: {}", id, request);
        
        RoleResponse response = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الدور بنجاح", response));
    }

    /**
     * DELETE /api/roles/{id}
     * Delete role
     * 
     * @param id the role ID
     * @return success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        log.info("DELETE /api/roles/{}", id);
        
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("تم حذف الدور بنجاح", null));
    }
}

