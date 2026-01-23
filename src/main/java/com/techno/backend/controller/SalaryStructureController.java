package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.SalaryBreakdownRequest;
import com.techno.backend.dto.SalaryBreakdownResponse;
import com.techno.backend.service.SalaryStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Salary Structure Management.
 * Provides endpoints for managing salary breakdown percentages.
 *
     * Base URL: /salary-structure
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@RestController
@RequestMapping("/salary-structure")
@RequiredArgsConstructor
@Slf4j
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    /**
     * Get all active salary breakdown percentages
     *
     * @return List of all salary breakdowns
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<SalaryBreakdownResponse>>> getAllSalaryBreakdowns() {
        log.info("GET /salary-structure");

        List<SalaryBreakdownResponse> response = salaryStructureService.getAllSalaryBreakdowns();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تفاصيل الراتب بنجاح",
                response
        ));
    }

    /**
     * Get salary breakdown for Saudi employees
     *
     * @return Saudi employee salary breakdown
     */
    @GetMapping("/saudi")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<SalaryBreakdownResponse>>> getSaudiBreakdown() {
        log.info("GET /salary-structure/saudi");

        List<SalaryBreakdownResponse> response = salaryStructureService.getSaudiBreakdown();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تفاصيل راتب السعوديين بنجاح",
                response
        ));
    }

    /**
     * Get salary breakdown for Foreign employees
     *
     * @return Foreign employee salary breakdown
     */
    @GetMapping("/foreign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<SalaryBreakdownResponse>>> getForeignBreakdown() {
        log.info("GET /salary-structure/foreign");

        List<SalaryBreakdownResponse> response = salaryStructureService.getForeignBreakdown();

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تفاصيل راتب الأجانب بنجاح",
                response
        ));
    }

    /**
     * Get salary breakdown by employee category
     *
     * @param category Employee category (S or F)
     * @return Salary breakdown for the category
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<SalaryBreakdownResponse>>> getBreakdownByCategory(
            @PathVariable String category) {

        log.info("GET /salary-structure/category/{}", category);

        List<SalaryBreakdownResponse> response = salaryStructureService.getBreakdownByCategory(category);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تفاصيل الراتب بنجاح",
                response
        ));
    }

    /**
     * Create or update salary breakdown percentage
     *
     * @param request Salary breakdown request
     * @return Saved salary breakdown
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SalaryBreakdownResponse>> saveSalaryBreakdown(
            @Valid @RequestBody SalaryBreakdownRequest request) {

        log.info("POST /salary-structure - category: {}, type: {}",
                request.getEmployeeCategory(), request.getTransTypeCode());

        SalaryBreakdownResponse response = salaryStructureService.saveSalaryBreakdown(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "تم حفظ تفاصيل الراتب بنجاح",
                response
        ));
    }

    /**
     * Delete (soft delete) salary breakdown
     *
     * @param serNo Serial number
     * @return Success response
     */
    @DeleteMapping("/{serNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSalaryBreakdown(@PathVariable Long serNo) {
        log.info("DELETE /salary-structure/{}", serNo);

        salaryStructureService.deleteSalaryBreakdown(serNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم حذف تفاصيل الراتب بنجاح",
                null
        ));
    }

    /**
     * Restore deleted salary breakdown
     *
     * @param serNo Serial number
     * @return Restored salary breakdown
     */
    @PostMapping("/{serNo}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SalaryBreakdownResponse>> restoreSalaryBreakdown(@PathVariable Long serNo) {
        log.info("POST /salary-structure/{}/restore", serNo);

        SalaryBreakdownResponse response = salaryStructureService.restoreSalaryBreakdown(serNo);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع تفاصيل الراتب بنجاح",
                response
        ));
    }
}
