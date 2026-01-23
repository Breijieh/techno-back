package com.techno.backend.service;

import com.techno.backend.dto.ContractTypeRequest;
import com.techno.backend.dto.ContractTypeResponse;
import com.techno.backend.entity.ContractType;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.ContractTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract Type Service
 * Handles contract type management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractTypeService {

    private final ContractTypeRepository contractTypeRepository;

    /**
     * Get all contract types
     * 
     * @return List of all contract types
     */
    @Transactional(readOnly = true)
    public List<ContractTypeResponse> getAllContractTypes() {
        List<ContractType> contractTypes = contractTypeRepository.findAll();
        return contractTypes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get contract type by code
     * 
     * @param code the contract type code
     * @return ContractTypeResponse
     * @throws ResourceNotFoundException if contract type not found
     */
    @Transactional(readOnly = true)
    public ContractTypeResponse getContractTypeByCode(String code) {
        ContractType contractType = contractTypeRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + code));
        return mapToResponse(contractType);
    }

    /**
     * Create new contract type
     * 
     * @param request the contract type request
     * @return ContractTypeResponse
     * @throws BadRequestException if contract type code already exists
     */
    @Transactional
    public ContractTypeResponse createContractType(ContractTypeRequest request) {
        // Validate code uniqueness
        if (contractTypeRepository.existsByContractTypeCode(request.getContractTypeCode())) {
            throw new BadRequestException("Ø±Ù…Ø² Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getContractTypeCode());
        }

        ContractType contractType = ContractType.builder()
                .contractTypeCode(request.getContractTypeCode())
                .typeName(request.getTypeName())
                .typeName(request.getTypeName())
                .calculateSalary(request.getCalculateSalary() != null ? request.getCalculateSalary() : 'Y')
                .allowSelfService(request.getAllowSelfService() != null ? request.getAllowSelfService() : 'Y')
                .isActive(request.getIsActive() != null ? request.getIsActive() : 'Y')
                .build();

        ContractType saved = contractTypeRepository.save(contractType);
        log.info("Contract type created successfully: {} ({})", saved.getTypeName(), saved.getContractTypeCode());
        return mapToResponse(saved);
    }

    /**
     * Update contract type
     * 
     * @param code the contract type code
     * @param request the contract type request
     * @return ContractTypeResponse
     * @throws ResourceNotFoundException if contract type not found
     */
    @Transactional
    public ContractTypeResponse updateContractType(String code, ContractTypeRequest request) {
        ContractType contractType = contractTypeRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + code));

        // Update fields
        contractType.setTypeName(request.getTypeName());
        contractType.setTypeName(request.getTypeName());
        
        if (request.getCalculateSalary() != null) {
            contractType.setCalculateSalary(request.getCalculateSalary());
        }
        if (request.getAllowSelfService() != null) {
            contractType.setAllowSelfService(request.getAllowSelfService());
        }
        if (request.getIsActive() != null) {
            contractType.setIsActive(request.getIsActive());
        }

        ContractType saved = contractTypeRepository.save(contractType);
        log.info("Contract type updated successfully: {} ({})", saved.getTypeName(), saved.getContractTypeCode());
        return mapToResponse(saved);
    }

    /**
     * Map ContractType entity to ContractTypeResponse DTO
     */
    private ContractTypeResponse mapToResponse(ContractType contractType) {
        return ContractTypeResponse.builder()
                .contractTypeCode(contractType.getContractTypeCode())
                .typeName(contractType.getTypeName())
                .typeName(contractType.getTypeName())
                .calculateSalary(contractType.getCalculateSalary())
                .allowSelfService(contractType.getAllowSelfService())
                .isActive(contractType.getIsActive())
                .createdDate(contractType.getCreatedDate())
                .createdBy(contractType.getCreatedBy())
                .modifiedDate(contractType.getModifiedDate())
                .modifiedBy(contractType.getModifiedBy())
                .build();
    }
}



