package com.techno.backend.service;

import com.techno.backend.dto.RoleListResponse;
import com.techno.backend.dto.RoleRequest;
import com.techno.backend.dto.RoleResponse;
import com.techno.backend.entity.Role;
import com.techno.backend.entity.RolePermission;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.RolePermissionRepository;
import com.techno.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Role Service
 * Handles role and permission management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Get all roles with pagination
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (asc/desc)
     * @return RoleListResponse with paginated roles
     */
    @Transactional(readOnly = true)
    public RoleListResponse getAllRoles(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching all roles - page: {}, size: {}, sortBy: {}, direction: {}", 
                page, size, sortBy, sortDirection);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Role> rolePage = roleRepository.findAll(pageable);
        List<RoleResponse> roleResponses = rolePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return RoleListResponse.builder()
                .content(roleResponses)
                .totalElements(rolePage.getTotalElements())
                .totalPages(rolePage.getTotalPages())
                .size(rolePage.getSize())
                .number(rolePage.getNumber())
                .hasNext(rolePage.hasNext())
                .hasPrevious(rolePage.hasPrevious())
                .build();
    }

    /**
     * Get role by ID
     * 
     * @param roleId the role ID
     * @return RoleResponse
     * @throws ResourceNotFoundException if role not found
     */
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long roleId) {
        log.info("Fetching role by ID: {}", roleId);
        Role role = findRoleByIdOrThrow(roleId);
        return mapToResponse(role);
    }

    /**
     * Create a new role
     * 
     * @param request the role creation request
     * @return RoleResponse
     * @throws BadRequestException if role name already exists
     */
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        log.info("Creating new role: {}", request.getRoleName());

        // Check if role name already exists
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new BadRequestException("اسم الدور موجود بالفعل: " + request.getRoleName());
        }

        // Create role
        Role role = Role.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .isActive('Y')
                .build();

        role = roleRepository.save(role);

        // Create permissions
        RolePermission permissions = RolePermission.builder()
                .role(role)
                .canManageEmployees(booleanToChar(request.getCanManageEmployees()))
                .canManageAttendance(booleanToChar(request.getCanManageAttendance()))
                .canManageLeave(booleanToChar(request.getCanManageLeave()))
                .canManageLoans(booleanToChar(request.getCanManageLoans()))
                .canManagePayroll(booleanToChar(request.getCanManagePayroll()))
                .canManageProjects(booleanToChar(request.getCanManageProjects()))
                .canManageWarehouse(booleanToChar(request.getCanManageWarehouse()))
                .canViewReports(booleanToChar(request.getCanViewReports()))
                .canApprove(booleanToChar(request.getCanApprove()))
                .canManageSettings(booleanToChar(request.getCanManageSettings()))
                .build();

        rolePermissionRepository.save(permissions);

        log.info("Role created successfully with ID: {}", role.getRoleId());
        return mapToResponse(role);
    }

    /**
     * Update an existing role
     * 
     * @param roleId the role ID
     * @param request the update request
     * @return RoleResponse
     * @throws ResourceNotFoundException if role not found
     * @throws BadRequestException if role name already exists (excluding current role)
     */
    @Transactional
    public RoleResponse updateRole(Long roleId, RoleRequest request) {
        log.info("Updating role ID: {}", roleId);

        Role role = findRoleByIdOrThrow(roleId);

        // Check if role name is being changed and if it conflicts with another role
        if (!role.getRoleName().equals(request.getRoleName()) && 
            roleRepository.existsByRoleNameAndRoleIdNot(request.getRoleName(), roleId)) {
            throw new BadRequestException("اسم الدور موجود بالفعل: " + request.getRoleName());
        }

        // Update role
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role = roleRepository.save(role);

        // Update permissions
        RolePermission permissions = rolePermissionRepository.findByRole(role)
                .orElseThrow(() -> new ResourceNotFoundException("الصلاحيات غير موجودة للدور: " + roleId));

        permissions.setCanManageEmployees(booleanToChar(request.getCanManageEmployees()));
        permissions.setCanManageAttendance(booleanToChar(request.getCanManageAttendance()));
        permissions.setCanManageLeave(booleanToChar(request.getCanManageLeave()));
        permissions.setCanManageLoans(booleanToChar(request.getCanManageLoans()));
        permissions.setCanManagePayroll(booleanToChar(request.getCanManagePayroll()));
        permissions.setCanManageProjects(booleanToChar(request.getCanManageProjects()));
        permissions.setCanManageWarehouse(booleanToChar(request.getCanManageWarehouse()));
        permissions.setCanViewReports(booleanToChar(request.getCanViewReports()));
        permissions.setCanApprove(booleanToChar(request.getCanApprove()));
        permissions.setCanManageSettings(booleanToChar(request.getCanManageSettings()));

        rolePermissionRepository.save(permissions);

        log.info("Role updated successfully: {}", roleId);
        return mapToResponse(role);
    }

    /**
     * Delete a role (soft delete - set isActive to 'N')
     * 
     * @param roleId the role ID
     * @throws ResourceNotFoundException if role not found
     */
    @Transactional
    public void deleteRole(Long roleId) {
        log.info("Deleting role ID: {}", roleId);

        Role role = findRoleByIdOrThrow(roleId);
        
        // Soft delete - set isActive to 'N'
        role.setIsActive('N');
        roleRepository.save(role);

        log.info("Role deleted successfully: {}", roleId);
    }

    /**
     * Find role by ID or throw exception
     * 
     * @param roleId the role ID
     * @return Role
     * @throws ResourceNotFoundException if role not found
     */
    private Role findRoleByIdOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("الدور غير موجود برقم: " + roleId));
    }

    /**
     * Map Role entity to RoleResponse DTO
     * 
     * @param role the role entity
     * @return RoleResponse
     */
    private RoleResponse mapToResponse(Role role) {
        RolePermission permissions = rolePermissionRepository.findByRole(role).orElse(null);

        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .canManageEmployees(charToBoolean(permissions != null ? permissions.getCanManageEmployees() : 'N'))
                .canManageAttendance(charToBoolean(permissions != null ? permissions.getCanManageAttendance() : 'N'))
                .canManageLeave(charToBoolean(permissions != null ? permissions.getCanManageLeave() : 'N'))
                .canManageLoans(charToBoolean(permissions != null ? permissions.getCanManageLoans() : 'N'))
                .canManagePayroll(charToBoolean(permissions != null ? permissions.getCanManagePayroll() : 'N'))
                .canManageProjects(charToBoolean(permissions != null ? permissions.getCanManageProjects() : 'N'))
                .canManageWarehouse(charToBoolean(permissions != null ? permissions.getCanManageWarehouse() : 'N'))
                .canViewReports(charToBoolean(permissions != null ? permissions.getCanViewReports() : 'N'))
                .canApprove(charToBoolean(permissions != null ? permissions.getCanApprove() : 'N'))
                .canManageSettings(charToBoolean(permissions != null ? permissions.getCanManageSettings() : 'N'))
                .isActive(role.getIsActive())
                .build();
    }

    /**
     * Convert Boolean to Character ('Y' or 'N')
     */
    private Character booleanToChar(Boolean value) {
        return (value != null && value) ? 'Y' : 'N';
    }

    /**
     * Convert Character to Boolean
     */
    private Boolean charToBoolean(Character value) {
        return value != null && value == 'Y';
    }
}

