package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.ContractTypeRequest;
import com.techno.backend.dto.ContractTypeResponse;
import com.techno.backend.service.ContractTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contract Type Controller
 * Handles contract type-related endpoints
 */
@RestController
@RequestMapping("/contract-types")
@RequiredArgsConstructor
@Slf4j
public class ContractTypeController {

    private final ContractTypeService contractTypeService;

    /**
     * GET /api/contract-types
     * List all contract types
     * 
     * @return List of all contract types
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractTypeResponse>>> getAllContractTypes() {
        log.debug("Fetching all contract types");
        List<ContractTypeResponse> contractTypes = contractTypeService.getAllContractTypes();
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø£Ù†ÙˆØ§Ø¹ Ø§Ù„Ø¹Ù‚ÙˆØ¯ Ø¨Ù†Ø¬Ø§Ø­", contractTypes));
    }

    /**
     * GET /api/contract-types/{code}
     * Get contract type by code
     * 
     * @param code the contract type code
     * @return ContractTypeResponse
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<ContractTypeResponse>> getContractTypeByCode(@PathVariable String code) {
        log.debug("Fetching contract type with code: {}", code);
        ContractTypeResponse contractType = contractTypeService.getContractTypeByCode(code);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ Ø¨Ù†Ø¬Ø§Ø­", contractType));
    }

    /**
     * POST /api/contract-types
     * Create new contract type (admin only)
     * 
     * @param request the contract type request
     * @return ContractTypeResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContractTypeResponse>> createContractType(
            @Valid @RequestBody ContractTypeRequest request) {
        log.info("Creating contract type: {} ({})", request.getTypeName(), request.getContractTypeCode());
        ContractTypeResponse contractType = contractTypeService.createContractType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ Ø¨Ù†Ø¬Ø§Ø­", contractType));
    }

    /**
     * PUT /api/contract-types/{code}
     * Update contract type (admin only)
     * 
     * @param code the contract type code
     * @param request the contract type request
     * @return ContractTypeResponse
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContractTypeResponse>> updateContractType(
            @PathVariable String code,
            @Valid @RequestBody ContractTypeRequest request) {
        log.info("Updating contract type with code: {}", code);
        ContractTypeResponse contractType = contractTypeService.updateContractType(code, request);
        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ Ø¨Ù†Ø¬Ø§Ø­", contractType));
    }
}



