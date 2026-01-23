package com.techno.backend.service;

import com.techno.backend.constant.DepartmentConstants;
import com.techno.backend.dto.DepartmentRequest;
import com.techno.backend.dto.DepartmentResponse;
import com.techno.backend.entity.Department;
import com.techno.backend.entity.UserAccount;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.DepartmentRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Department Service
 * Handles department management operations including hierarchy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserService userService;

    /**
     * Get all departments
     * 
     * @return List of all departments
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findByIsActive(DepartmentConstants.ACTIVE);
        return departments.stream()
                .map(dept -> mapToResponse(dept, false)) // No parent object needed in list view
                .collect(Collectors.toList());
    }

    /**
     * Get department by ID
     * 
     * @param id the department code
     * @return DepartmentResponse
     * @throws ResourceNotFoundException if department not found
     */
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + id));
        return mapToResponse(department);
    }

    /**
     * Create new department
     * 
     * @param request the department request
     * @return DepartmentResponse
     * @throws BadRequestException if parent department doesn't exist or circular
     *                             reference detected
     */
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        // Validate parent department if provided
        if (request.getParentDeptCode() != null) {
            if (!departmentRepository.existsByDeptCode(request.getParentDeptCode())) {
                throw new BadRequestException("Ø§Ù„Ù‚Ø³Ù… Ø§Ù„Ø±Ø¦ÙŠØ³ÙŠ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getParentDeptCode());
            }
        }

        // Validate manager if provided
        if (request.getDeptMgrCode() != null) {
            try {
                userService.findByEmployeeNo(request.getDeptMgrCode());
            } catch (ResourceNotFoundException e) {
                throw new BadRequestException("Ø§Ù„Ù…Ø¯ÙŠØ± ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù: " + request.getDeptMgrCode());
            }
        }

        Department department = Department.builder()
                .deptName(request.getDeptName())
                .parentDeptCode(request.getParentDeptCode())
                .deptMgrCode(request.getDeptMgrCode())
                .isActive(DepartmentConstants.ACTIVE)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created successfully: {} ({})", saved.getDeptName(), saved.getDeptCode());
        return mapToResponse(saved);
    }

    /**
     * Update department
     * 
     * @param id      the department code
     * @param request the department request
     * @return DepartmentResponse
     * @throws ResourceNotFoundException if department not found
     * @throws BadRequestException       if parent department doesn't exist or
     *                                   circular reference detected
     */
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + id));

        // Validate parent department if provided
        if (request.getParentDeptCode() != null) {
            if (!departmentRepository.existsByDeptCode(request.getParentDeptCode())) {
                throw new BadRequestException("Ø§Ù„Ù‚Ø³Ù… Ø§Ù„Ø±Ø¦ÙŠØ³ÙŠ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getParentDeptCode());
            }

            // Prevent circular reference: parent cannot be the department itself
            if (request.getParentDeptCode().equals(id)) {
                throw new BadRequestException("Ø§Ù„Ù‚Ø³Ù… Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø±Ø¦ÙŠØ³Ù‡ Ø§Ù„Ø®Ø§Øµ");
            }

            // Prevent circular reference: check if parent is a descendant of current
            // department
            if (isDescendant(id, request.getParentDeptCode())) {
                throw new BadRequestException("ØªÙ… Ø§ÙƒØªØ´Ø§Ù Ù…Ø±Ø¬Ø¹ Ø¯Ø§Ø¦Ø±ÙŠ: Ø§Ù„Ù‚Ø³Ù… Ø§Ù„Ø±Ø¦ÙŠØ³ÙŠ Ù‡Ùˆ Ø³Ù„ÙŠÙ„");
            }
        }

        // Validate manager if provided
        if (request.getDeptMgrCode() != null) {
            try {
                userService.findByEmployeeNo(request.getDeptMgrCode());
            } catch (ResourceNotFoundException e) {
                throw new BadRequestException("Ø§Ù„Ù…Ø¯ÙŠØ± ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù: " + request.getDeptMgrCode());
            }
        }

        department.setDeptName(request.getDeptName());
        department.setParentDeptCode(request.getParentDeptCode());
        department.setDeptMgrCode(request.getDeptMgrCode());

        Department saved = departmentRepository.save(department);
        log.info("Department updated successfully: {} ({})", saved.getDeptName(), saved.getDeptCode());
        return mapToResponse(saved);
    }

    /**
     * Delete department (soft delete)
     * 
     * @param id the department code
     * @throws ResourceNotFoundException if department not found
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + id));

        // Check if there are active sub-departments
        List<Department> activeSubDepartments = departmentRepository.findByParentDeptCodeAndIsActive(id,
                DepartmentConstants.ACTIVE);
        if (!activeSubDepartments.isEmpty()) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø§Ù„Ù‚Ø³Ù… Ù„ÙˆØ¬ÙˆØ¯ Ø£Ù‚Ø³Ø§Ù… ØªØ§Ø¨Ø¹Ø© Ù†Ø´Ø·Ø©. ÙŠØ±Ø¬Ù‰ Ø­Ø°Ù Ø§Ù„Ø£Ù‚Ø³Ø§Ù… Ø§Ù„ØªØ§Ø¨Ø¹Ø© Ø£ÙˆÙ„Ø§Ù‹.");
        }

        // Check if there are active employees in this department
        Long activeEmployees = employeeRepository.countByPrimaryDeptCode(id);
        if (activeEmployees > 0) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø§Ù„Ù‚Ø³Ù… Ù„ÙˆØ¬ÙˆØ¯ Ù…ÙˆØ¸ÙÙŠÙ† Ù†Ø´Ø·ÙŠÙ† Ù…Ø±ØªØ¨Ø·ÙŠÙ† Ø¨Ù‡ (" + activeEmployees
                    + " Ù…ÙˆØ¸Ù). ÙŠØ±Ø¬Ù‰ Ù†Ù‚Ù„ Ø§Ù„Ù…ÙˆØ¸ÙÙŠÙ† Ø£ÙˆÙ„Ø§Ù‹.");
        }

        department.setIsActive(DepartmentConstants.INACTIVE);
        departmentRepository.save(department);
        log.info("Department deleted (deactivated): {} ({})", department.getDeptName(), department.getDeptCode());
    }

    /**
     * Get department hierarchy as tree structure
     * 
     * @return List of root departments with nested children
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentHierarchy() {
        List<Department> rootDepartments = departmentRepository
                .findByParentDeptCodeIsNullAndIsActive(DepartmentConstants.ACTIVE);
        return rootDepartments.stream()
                .map(dept -> buildHierarchyTree(dept))
                .collect(Collectors.toList());
    }

    /**
     * Get department hierarchy as flat list
     * 
     * @return List of all departments with parent references
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentHierarchyFlat() {
        List<Department> departments = departmentRepository.findByIsActive(DepartmentConstants.ACTIVE);
        return departments.stream()
                .map(dept -> mapToResponse(dept, false)) // Parent code already in response
                .collect(Collectors.toList());
    }

    /**
     * Get department manager information
     * 
     * @param id the department code
     * @return DepartmentResponse with manager info
     * @throws ResourceNotFoundException if department not found
     */
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentManager(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + id));

        DepartmentResponse response = mapToResponse(department);

        // If manager exists, get manager name
        if (department.getDeptMgrCode() != null) {
            try {
                UserAccount manager = userService.findByEmployeeNo(department.getDeptMgrCode());
                response.setManagerName(manager.getUsername());
            } catch (ResourceNotFoundException e) {
                log.warn("Manager not found for department {}: {}", id, department.getDeptMgrCode());
            }
        }

        return response;
    }

    /**
     * Build hierarchy tree recursively
     */
    private DepartmentResponse buildHierarchyTree(Department department) {
        DepartmentResponse response = mapToResponse(department, false); // Tree structure handles hierarchy

        List<Department> children = departmentRepository.findByParentDeptCodeAndIsActive(
                department.getDeptCode(), DepartmentConstants.ACTIVE);

        List<DepartmentResponse> childResponses = children.stream()
                .map(this::buildHierarchyTree)
                .collect(Collectors.toList());

        response.setChildren(childResponses);
        return response;
    }

    /**
     * Check if a department is a descendant of another
     */
    private boolean isDescendant(Long ancestorId, Long descendantId) {
        Department current = departmentRepository.findById(descendantId).orElse(null);
        if (current == null) {
            return false;
        }

        while (current.getParentDeptCode() != null) {
            if (current.getParentDeptCode().equals(ancestorId)) {
                return true;
            }
            current = departmentRepository.findById(current.getParentDeptCode()).orElse(null);
            if (current == null) {
                break;
            }
        }

        return false;
    }

    /**
     * Map Department entity to DepartmentResponse DTO
     *
     * @param department    the department entity
     * @param includeParent whether to include parent department (non-recursive)
     * @return DepartmentResponse DTO
     */
    private DepartmentResponse mapToResponse(Department department, boolean includeParent) {
        DepartmentResponse.DepartmentResponseBuilder builder = DepartmentResponse.builder()
                .deptCode(department.getDeptCode())
                .deptName(department.getDeptName())
                .parentDeptCode(department.getParentDeptCode())
                .deptMgrCode(department.getDeptMgrCode())
                .isActive(department.getIsActive())
                .createdDate(department.getCreatedDate())
                .createdBy(department.getCreatedBy())
                .modifiedDate(department.getModifiedDate())
                .modifiedBy(department.getModifiedBy());

        // Set parent department if requested (NON-RECURSIVE - only basic fields)
        // This prevents N+1 query problems and stack overflow with deep hierarchies
        if (includeParent && department.getParentDeptCode() != null) {
            departmentRepository.findById(department.getParentDeptCode())
                    .ifPresent(parent -> {
                        // Build parent WITHOUT recursive call - prevents N+1 and stack overflow
                        DepartmentResponse parentResponse = DepartmentResponse.builder()
                                .deptCode(parent.getDeptCode())
                                .deptName(parent.getDeptName())
                                .parentDeptCode(parent.getParentDeptCode())
                                .deptMgrCode(parent.getDeptMgrCode())
                                .isActive(parent.getIsActive())
                                .build();
                        builder.parentDepartment(parentResponse);
                    });
        }

        // Set manager name if exists
        if (department.getDeptMgrCode() != null) {
            try {
                UserAccount manager = userService.findByEmployeeNo(department.getDeptMgrCode());
                builder.managerName(manager.getUsername());
            } catch (ResourceNotFoundException e) {
                log.debug("Manager not found for department {}: {}", department.getDeptCode(),
                        department.getDeptMgrCode());
            }
        }

        return builder.build();
    }

    /**
     * Map Department entity to DepartmentResponse DTO with parent information
     *
     * @param department the department entity
     * @return DepartmentResponse DTO with parent loaded
     */
    private DepartmentResponse mapToResponse(Department department) {
        return mapToResponse(department, true);
    }
}

