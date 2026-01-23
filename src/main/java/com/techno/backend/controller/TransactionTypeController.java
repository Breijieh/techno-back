package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.TransactionTypeRequest;
import com.techno.backend.dto.TransactionTypeResponse;
import com.techno.backend.service.TransactionTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Transaction Type Management.
 * Provides endpoints for managing payroll transaction types (allowances and deductions).
 *
 * Base URL: /api/transaction-types
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@RestController
@RequestMapping("/transaction-types")
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    /**
     * Get all active transaction types
     *
     * @return List of all transaction types
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransactionTypeResponse>>> getAllTransactionTypes() {
        log.info("GET /api/transaction-types");

        List<TransactionTypeResponse> response = transactionTypeService.getAllTransactionTypes();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø£Ù†ÙˆØ§Ø¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Get all allowance types
     *
     * @return List of allowance types
     */
    @GetMapping("/allowances")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransactionTypeResponse>>> getAllowances() {
        log.info("GET /api/transaction-types/allowances");

        List<TransactionTypeResponse> response = transactionTypeService.getAllowances();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø£Ù†ÙˆØ§Ø¹ Ø§Ù„Ø¨Ø¯Ù„Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Get all deduction types
     *
     * @return List of deduction types
     */
    @GetMapping("/deductions")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<TransactionTypeResponse>>> getDeductions() {
        log.info("GET /api/transaction-types/deductions");

        List<TransactionTypeResponse> response = transactionTypeService.getDeductions();

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø£Ù†ÙˆØ§Ø¹ Ø§Ù„Ø®ØµÙˆÙ…Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Get transaction type by code
     *
     * @param code Transaction type code
     * @return Transaction type details
     */
    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<ApiResponse<TransactionTypeResponse>> getTransactionTypeByCode(@PathVariable Long code) {
        log.info("GET /api/transaction-types/{}", code);

        TransactionTypeResponse response = transactionTypeService.getTransactionTypeByCode(code);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Create new transaction type
     *
     * @param request Transaction type creation request
     * @return Created transaction type
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionTypeResponse>> createTransactionType(
            @Valid @RequestBody TransactionTypeRequest request) {

        log.info("POST /api/transaction-types - Creating: {}", request.getTypeName());

        TransactionTypeResponse response = transactionTypeService.createTransactionType(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Update existing transaction type
     *
     * @param code Transaction type code
     * @param request Transaction type update request
     * @return Updated transaction type
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionTypeResponse>> updateTransactionType(
            @PathVariable Long code,
            @Valid @RequestBody TransactionTypeRequest request) {

        log.info("PUT /api/transaction-types/{}", code);

        TransactionTypeResponse response = transactionTypeService.updateTransactionType(code, request);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… ØªØ­Ø¯ÙŠØ« Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Delete (deactivate) transaction type
     *
     * @param code Transaction type code
     * @return Success response
     */
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTransactionType(@PathVariable Long code) {
        log.info("DELETE /api/transaction-types/{}", code);

        transactionTypeService.deleteTransactionType(code);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø­Ø°Ù Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                null
        ));
    }

    /**
     * Activate transaction type
     *
     * @param code Transaction type code
     * @return Activated transaction type
     */
    @PostMapping("/{code}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionTypeResponse>> activateTransactionType(@PathVariable Long code) {
        log.info("POST /api/transaction-types/{}/activate", code);

        TransactionTypeResponse response = transactionTypeService.activateTransactionType(code);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… ØªÙØ¹ÙŠÙ„ Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }
}

