package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.EmployeeContractAllowanceRequest;
import com.techno.backend.dto.EmployeeContractAllowanceResponse;
import com.techno.backend.service.EmployeeContractAllowanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Employee Contract Allowance Controller
 * Handles employee contract allowance endpoints
 */
@RestController
@RequestMapping("/employee-contract-allowances") // No /api prefix, matches other controllers
@RequiredArgsConstructor
@Slf4j
public class EmployeeContractAllowanceController {

    private final EmployeeContractAllowanceService allowanceService;

    /**
     * GET /employee-contract-allowances
     * Get all employee contract allowances (both active and inactive)
     *
     * @return List of all allowances
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeContractAllowanceResponse>>> getAllAllowances() {
        log.info("GET /employee-contract-allowances - Fetching all allowances");
        List<EmployeeContractAllowanceResponse> allowances = allowanceService.getAllAllowances();
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع بدلات عقد الموظف بنجاح", allowances));
    }

    /**
     * GET /employee-contract-allowances/{id}
     * Get employee contract allowance by ID
     *
     * @param id the record ID
     * @return EmployeeContractAllowanceResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeContractAllowanceResponse>> getAllowanceById(@PathVariable Long id) {
        log.info("GET /employee-contract-allowances/{}", id);
        EmployeeContractAllowanceResponse allowance = allowanceService.getAllowanceById(id);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع بدل عقد الموظف بنجاح", allowance));
    }

    /**
     * GET /employee-contract-allowances/employee/{employeeNo}
     * Get allowances for a specific employee
     *
     * @param employeeNo the employee number
     * @return List of allowances for the employee
     */
    @GetMapping("/employee/{employeeNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeContractAllowanceResponse>>> getAllowancesByEmployee(
            @PathVariable Long employeeNo) {
        log.info("GET /employee-contract-allowances/employee/{}", employeeNo);
        List<EmployeeContractAllowanceResponse> allowances = allowanceService.getAllowancesByEmployee(employeeNo);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع بدلات عقد الموظف بنجاح", allowances));
    }

    /**
     * POST /employee-contract-allowances
     * Create a new employee contract allowance
     *
     * @param request the create request
     * @return Created EmployeeContractAllowanceResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeContractAllowanceResponse>> createAllowance(
            @Valid @RequestBody EmployeeContractAllowanceRequest request) {
        log.info("POST /employee-contract-allowances - Creating allowance for employee: {}, type: {}",
                request.getEmployeeNo(), request.getTransTypeCode());
        EmployeeContractAllowanceResponse allowance = allowanceService.createAllowance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء بدل عقد الموظف بنجاح", allowance));
    }

    /**
     * PUT /employee-contract-allowances/{id}
     * Update an existing employee contract allowance
     *
     * @param id      the record ID
     * @param request the update request
     * @return Updated EmployeeContractAllowanceResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeContractAllowanceResponse>> updateAllowance(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeContractAllowanceRequest request) {
        log.info("PUT /employee-contract-allowances/{} - Updating allowance", id);
        EmployeeContractAllowanceResponse allowance = allowanceService.updateAllowance(id, request);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث بدل عقد الموظف بنجاح", allowance));
    }

    /**
     * DELETE /employee-contract-allowances/{id}
     * Delete (soft delete) an employee contract allowance
     *
     * @param id the record ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAllowance(@PathVariable Long id) {
        log.info("DELETE /employee-contract-allowances/{} - Deleting allowance (soft delete)", id);
        allowanceService.deleteAllowance(id);
        return ResponseEntity.ok(ApiResponse.success("تم حذف بدل عقد الموظف بنجاح", null));
    }
}

