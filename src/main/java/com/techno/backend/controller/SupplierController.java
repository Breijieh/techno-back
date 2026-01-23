package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.SupplierRequest;
import com.techno.backend.dto.SupplierResponse;
import com.techno.backend.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supplier Controller
 * Handles supplier-related endpoints
 */
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * GET /api/suppliers
     * List all suppliers
     * 
     * @return List of all suppliers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers() {
        log.info("GET /api/suppliers - Fetching all suppliers");
        List<SupplierResponse> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ±Ø¯ÙŠÙ† Ø¨Ù†Ø¬Ø§Ø­", suppliers));
    }

    /**
     * GET /api/suppliers/{id}
     * Get supplier by ID
     * 
     * @param id the supplier ID
     * @return SupplierResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplierById(@PathVariable Long id) {
        log.info("GET /api/suppliers/{}", id);
        SupplierResponse supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ù†Ø¬Ø§Ø­", supplier));
    }

    /**
     * POST /api/suppliers
     * Create new supplier
     * 
     * @param request the supplier request
     * @return SupplierResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request) {
        log.info("POST /api/suppliers - Creating supplier: {}", request.getSupplierName());
        SupplierResponse supplier = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ù†Ø¬Ø§Ø­", supplier));
    }

    /**
     * PUT /api/suppliers/{id}
     * Update supplier
     * 
     * @param id the supplier ID
     * @param request the supplier request
     * @return SupplierResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        log.info("PUT /api/suppliers/{} - Updating supplier", id);
        SupplierResponse supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ù†Ø¬Ø§Ø­", supplier));
    }

    /**
     * DELETE /api/suppliers/{id}
     * Delete supplier (soft delete)
     * 
     * @param id the supplier ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        log.info("DELETE /api/suppliers/{}", id);
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø­Ø°Ù Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ù†Ø¬Ø§Ø­", null));
    }
}


