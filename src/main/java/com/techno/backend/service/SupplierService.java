package com.techno.backend.service;

import com.techno.backend.dto.SupplierRequest;
import com.techno.backend.dto.SupplierResponse;
import com.techno.backend.entity.Supplier;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supplier Service
 * Handles supplier management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;

    /**
     * Get all suppliers
     * 
     * @return List of all suppliers
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        return suppliers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get supplier by ID
     * 
     * @param id the supplier ID
     * @return SupplierResponse
     * @throws ResourceNotFoundException if supplier not found
     */
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = findSupplierOrThrow(id);
        return mapToResponse(supplier);
    }

    /**
     * Create new supplier
     * 
     * @param request the supplier request
     * @return SupplierResponse
     * @throws BadRequestException if supplier name already exists
     */
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        // Check if supplier name already exists
        if (supplierRepository.existsBySupplierName(request.getSupplierName())) {
            throw new BadRequestException("Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ø§Ù„Ø§Ø³Ù… '" + request.getSupplierName() + "' Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        Supplier supplier = mapToEntity(request);
        Supplier saved = supplierRepository.save(supplier);
        
        log.info("Supplier created successfully: {} ({})", saved.getSupplierName(), saved.getSupplierId());
        return mapToResponse(saved);
    }

    /**
     * Update supplier
     * 
     * @param id      the supplier ID
     * @param request the update request
     * @return SupplierResponse
     * @throws ResourceNotFoundException if supplier not found
     * @throws BadRequestException if supplier name already exists for another supplier
     */
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findSupplierOrThrow(id);

        // Check if supplier name is being changed and if it conflicts with existing supplier
        if (!supplier.getSupplierName().equals(request.getSupplierName())) {
            if (supplierRepository.existsBySupplierNameAndSupplierIdNot(request.getSupplierName(), id)) {
                throw new BadRequestException("Ø§Ù„Ù…ÙˆØ±Ø¯ Ø¨Ø§Ù„Ø§Ø³Ù… '" + request.getSupplierName() + "' Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„");
            }
        }

        // Update supplier fields
        supplier.setSupplierName(request.getSupplierName());
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        
        // Convert Boolean to Character for isActive
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive() ? 'Y' : 'N');
        }

        Supplier updated = supplierRepository.save(supplier);
        
        log.info("Supplier updated successfully: {} ({})", updated.getSupplierName(), updated.getSupplierId());
        return mapToResponse(updated);
    }

    /**
     * Delete supplier (soft delete by setting isActive to 'N')
     * 
     * @param id the supplier ID
     * @throws ResourceNotFoundException if supplier not found
     */
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = findSupplierOrThrow(id);
        supplier.setIsActive('N');
        supplierRepository.save(supplier);
        
        log.info("Supplier deleted (soft delete) successfully: {} ({})", supplier.getSupplierName(), supplier.getSupplierId());
    }

    // ==================== Helper Methods ====================

    private Supplier findSupplierOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ±Ø¯ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + id));
    }

    private Supplier mapToEntity(SupplierRequest request) {
        Supplier.SupplierBuilder builder = Supplier.builder()
                .supplierName(request.getSupplierName())
                .supplierName(request.getSupplierName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress());

        // Convert Boolean to Character for isActive
        if (request.getIsActive() != null) {
            builder.isActive(request.getIsActive() ? 'Y' : 'N');
        } else {
            builder.isActive('Y'); // Default to active
        }

        return builder.build();
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getSupplierName())
                .supplierName(supplier.getSupplierName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .isActive(supplier.getIsActive() == 'Y') // Convert Character to Boolean
                .createdDate(supplier.getCreatedDate())
                .createdBy(supplier.getCreatedBy())
                .modifiedDate(supplier.getModifiedDate())
                .modifiedBy(supplier.getModifiedBy())
                .build();
    }
}


