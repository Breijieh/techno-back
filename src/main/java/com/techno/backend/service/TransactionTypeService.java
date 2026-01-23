package com.techno.backend.service;

import com.techno.backend.dto.TransactionTypeRequest;
import com.techno.backend.dto.TransactionTypeResponse;
import com.techno.backend.entity.TransactionType;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for TransactionType management.
 * Handles business logic for transaction type CRUD operations.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;

    /**
     * Get all active transaction types
     */
    @Transactional(readOnly = true)
    public List<TransactionTypeResponse> getAllTransactionTypes() {
        log.info("Fetching all active transaction types");
        List<TransactionType> transactionTypes = transactionTypeRepository.findAllActive();
        return transactionTypes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all allowance types
     */
    @Transactional(readOnly = true)
    public List<TransactionTypeResponse> getAllowances() {
        log.info("Fetching all allowance types");
        List<TransactionType> allowances = transactionTypeRepository.findAllAllowances();
        return allowances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all deduction types
     */
    @Transactional(readOnly = true)
    public List<TransactionTypeResponse> getDeductions() {
        log.info("Fetching all deduction types");
        List<TransactionType> deductions = transactionTypeRepository.findAllDeductions();
        return deductions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction type by code
     */
    @Transactional(readOnly = true)
    public TransactionTypeResponse getTransactionTypeByCode(Long typeCode) {
        log.info("Fetching transaction type by code: {}", typeCode);
        TransactionType transactionType = findTransactionTypeByCodeOrThrow(typeCode);
        return mapToResponse(transactionType);
    }

    /**
     * Create new transaction type
     */
    @Transactional
    public TransactionTypeResponse createTransactionType(TransactionTypeRequest request) {
        log.info("Creating new transaction type: {}", request.getTypeName());

        // Validate type code uniqueness
        if (transactionTypeRepository.existsByTypeCode(request.getTypeCode())) {
            throw new BadRequestException("Ø±Ù…Ø² Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getTypeCode());
        }

        TransactionType transactionType = mapToEntity(request);
        transactionType = transactionTypeRepository.save(transactionType);

        log.info("Transaction type created successfully with code: {}", transactionType.getTypeCode());
        return mapToResponse(transactionType);
    }

    /**
     * Update existing transaction type
     */
    @Transactional
    public TransactionTypeResponse updateTransactionType(Long typeCode, TransactionTypeRequest request) {
        log.info("Updating transaction type code: {}", typeCode);

        TransactionType transactionType = findTransactionTypeByCodeOrThrow(typeCode);

        // Validate if type code is being changed
        if (!typeCode.equals(request.getTypeCode())) {
            if (transactionTypeRepository.existsByTypeCode(request.getTypeCode())) {
                throw new BadRequestException("Ø±Ù…Ø² Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getTypeCode());
            }
        }

        updateTransactionTypeFields(transactionType, request);
        transactionType = transactionTypeRepository.save(transactionType);

        log.info("Transaction type updated successfully: {}", typeCode);
        return mapToResponse(transactionType);
    }

    /**
     * Delete (deactivate) transaction type
     */
    @Transactional
    public void deleteTransactionType(Long typeCode) {
        log.info("Deactivating transaction type code: {}", typeCode);

        TransactionType transactionType = findTransactionTypeByCodeOrThrow(typeCode);
        transactionType.deactivate();
        transactionTypeRepository.save(transactionType);

        log.info("Transaction type deactivated successfully: {}", typeCode);
    }

    /**
     * Activate transaction type
     */
    @Transactional
    public TransactionTypeResponse activateTransactionType(Long typeCode) {
        log.info("Activating transaction type code: {}", typeCode);

        TransactionType transactionType = findTransactionTypeByCodeOrThrow(typeCode);
        transactionType.activate();
        transactionType = transactionTypeRepository.save(transactionType);

        log.info("Transaction type activated successfully: {}", typeCode);
        return mapToResponse(transactionType);
    }

    // ===== Private Helper Methods =====

    /**
     * Find transaction type by code or throw exception
     */
    private TransactionType findTransactionTypeByCodeOrThrow(Long typeCode) {
        return transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + typeCode));
    }

    /**
     * Map TransactionType entity to TransactionTypeResponse DTO
     */
    private TransactionTypeResponse mapToResponse(TransactionType transactionType) {
        return TransactionTypeResponse.builder()
                .typeCode(transactionType.getTypeCode())
                .typeName(transactionType.getTypeName())
                .typeName(transactionType.getTypeName())
                .allowanceDeduction(transactionType.getAllowanceDeduction())
                .isSystemGenerated(transactionType.getIsSystemGenerated())
                .isActive(transactionType.getIsActive())
                .createdDate(transactionType.getCreatedDate())
                .build();
    }

    /**
     * Map TransactionTypeRequest DTO to TransactionType entity
     */
    private TransactionType mapToEntity(TransactionTypeRequest request) {
        return TransactionType.builder()
                .typeCode(request.getTypeCode())
                .typeName(request.getTypeName())
                .typeName(request.getTypeName())
                .allowanceDeduction(request.getAllowanceDeduction())
                .isSystemGenerated(request.getIsSystemGenerated())
                .isActive(request.getIsActive())
                .build();
    }

    /**
     * Update transaction type fields from request
     */
    private void updateTransactionTypeFields(TransactionType transactionType, TransactionTypeRequest request) {
        transactionType.setTypeName(request.getTypeName());
        transactionType.setTypeName(request.getTypeName());
        transactionType.setAllowanceDeduction(request.getAllowanceDeduction());
        transactionType.setIsSystemGenerated(request.getIsSystemGenerated());
        transactionType.setIsActive(request.getIsActive());
    }
}

