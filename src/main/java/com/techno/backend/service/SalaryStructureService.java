package com.techno.backend.service;

import com.techno.backend.dto.SalaryBreakdownRequest;
import com.techno.backend.dto.SalaryBreakdownResponse;
import com.techno.backend.entity.SalaryBreakdownPercentage;
import com.techno.backend.entity.TransactionType;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.SalaryBreakdownPercentageRepository;
import com.techno.backend.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Salary Structure management.
 * Handles business logic for salary breakdown percentages.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryStructureService {

    private final SalaryBreakdownPercentageRepository salaryBreakdownRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    /**
     * Get all active salary breakdown percentages
     */
    @Transactional(readOnly = true)
    public List<SalaryBreakdownResponse> getAllSalaryBreakdowns() {
        log.info("Fetching all active salary breakdown percentages");
        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownRepository.findAllActive();
        return breakdowns.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get salary breakdown for Saudi employees
     */
    @Transactional(readOnly = true)
    public List<SalaryBreakdownResponse> getSaudiBreakdown() {
        log.info("Fetching Saudi employee salary breakdown");
        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownRepository.findSaudiBreakdown();
        return breakdowns.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get salary breakdown for Foreign employees
     */
    @Transactional(readOnly = true)
    public List<SalaryBreakdownResponse> getForeignBreakdown() {
        log.info("Fetching Foreign employee salary breakdown");
        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownRepository.findForeignBreakdown();
        return breakdowns.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get salary breakdown by employee category
     */
    @Transactional(readOnly = true)
    public List<SalaryBreakdownResponse> getBreakdownByCategory(String category) {
        log.info("Fetching salary breakdown for category: {}", category);

        if (!"S".equals(category) && !"F".equals(category)) {
            throw new BadRequestException(
                    "ÙØ¦Ø© Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± ØµØ§Ù„Ø­Ø©. ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† 'S' (Ø³Ø¹ÙˆØ¯ÙŠ) Ø£Ùˆ 'F' (Ø£Ø¬Ù†Ø¨ÙŠ)");
        }

        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownRepository.findByEmployeeCategory(category);
        return breakdowns.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create or update salary breakdown percentage
     */
    @Transactional
    public SalaryBreakdownResponse saveSalaryBreakdown(SalaryBreakdownRequest request) {
        log.info("Saving salary breakdown for category: {}, type: {}",
                request.getEmployeeCategory(), request.getTransTypeCode());

        // Validate employee category
        if (!"S".equals(request.getEmployeeCategory()) && !"F".equals(request.getEmployeeCategory())) {
            throw new BadRequestException(
                    "ÙØ¦Ø© Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± ØµØ§Ù„Ø­Ø©. ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† 'S' (Ø³Ø¹ÙˆØ¯ÙŠ) Ø£Ùˆ 'F' (Ø£Ø¬Ù†Ø¨ÙŠ)");
        }

        // Validate transaction type exists
        transactionTypeRepository.findById(request.getTransTypeCode())
                .orElseThrow(() -> new BadRequestException(
                        "Ø±Ù…Ø² Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± ØµØ§Ù„Ø­: " + request.getTransTypeCode()));

        // Check if breakdown already exists (including soft-deleted records)
        // This ensures we don't violate the unique constraint on (category,
        // transTypeCode)
        SalaryBreakdownPercentage breakdown = salaryBreakdownRepository
                .findByEmployeeCategoryAndTransTypeCodeIncludingDeleted(request.getEmployeeCategory(),
                        request.getTransTypeCode())
                .orElse(null);

        if (breakdown != null) {
            // Update existing (including restoring if it was soft-deleted)
            log.info("Updating existing salary breakdown (serNo: {}, wasDeleted: {})",
                    breakdown.getSerNo(), breakdown.isDeleted());
            breakdown.setSalaryPercentage(request.getSalaryPercentage());
            breakdown.restore(); // Mark as active if it was deleted
        } else {
            // Create new
            log.info("Creating new salary breakdown");
            breakdown = mapToEntity(request);
        }

        // Validate total percentage doesn't exceed 100%
        validateTotalPercentage(request.getEmployeeCategory(), breakdown);

        breakdown = salaryBreakdownRepository.save(breakdown);
        log.info("Salary breakdown saved successfully");

        return mapToResponse(breakdown);
    }

    /**
     * Delete (soft delete) salary breakdown
     */
    @Transactional
    public void deleteSalaryBreakdown(Long serNo) {
        log.info("Deleting salary breakdown with serial number: {}", serNo);

        SalaryBreakdownPercentage breakdown = salaryBreakdownRepository.findById(serNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ØªÙØ§ØµÙŠÙ„ Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + serNo));

        breakdown.delete();
        salaryBreakdownRepository.save(breakdown);

        log.info("Salary breakdown deleted successfully: {}", serNo);
    }

    /**
     * Restore deleted salary breakdown
     */
    @Transactional
    public SalaryBreakdownResponse restoreSalaryBreakdown(Long serNo) {
        log.info("Restoring salary breakdown with serial number: {}", serNo);

        SalaryBreakdownPercentage breakdown = salaryBreakdownRepository.findById(serNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ØªÙØ§ØµÙŠÙ„ Ø§Ù„Ø±Ø§ØªØ¨ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + serNo));

        breakdown.restore();
        breakdown = salaryBreakdownRepository.save(breakdown);

        log.info("Salary breakdown restored successfully: {}", serNo);
        return mapToResponse(breakdown);
    }

    /**
     * Validate that total percentage for a category doesn't exceed 100%
     */
    private void validateTotalPercentage(String category, SalaryBreakdownPercentage newBreakdown) {
        List<SalaryBreakdownPercentage> existingBreakdowns = salaryBreakdownRepository.findByEmployeeCategory(category);

        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (SalaryBreakdownPercentage breakdown : existingBreakdowns) {
            // Skip the breakdown being updated
            if (newBreakdown.getSerNo() != null && breakdown.getSerNo().equals(newBreakdown.getSerNo())) {
                continue;
            }
            totalPercentage = totalPercentage.add(breakdown.getSalaryPercentage());
        }

        // Add the new/updated percentage
        totalPercentage = totalPercentage.add(newBreakdown.getSalaryPercentage());

        if (totalPercentage.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException(
                    String.format(
                            "Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ù†Ø³Ø¨Ø© Ø§Ù„Ø±Ø§ØªØ¨ ÙŠØªØ¬Ø§ÙˆØ² 100%%. Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø­Ø§Ù„ÙŠ: %.2f%%",
                            totalPercentage.multiply(BigDecimal.valueOf(100))));
        }

        log.info("Total percentage validation passed: {}%",
                totalPercentage.multiply(BigDecimal.valueOf(100)));
    }

    // ===== Private Helper Methods =====

    /**
     * Map SalaryBreakdownPercentage entity to SalaryBreakdownResponse DTO
     */
    private SalaryBreakdownResponse mapToResponse(SalaryBreakdownPercentage breakdown) {
        SalaryBreakdownResponse response = SalaryBreakdownResponse.builder()
                .serNo(breakdown.getSerNo())
                .employeeCategory(breakdown.getEmployeeCategory())
                .transTypeCode(breakdown.getTransTypeCode())
                .salaryPercentage(breakdown.getSalaryPercentage())
                .isDeleted(breakdown.getIsDeleted())
                .build();

        // Add transaction type info if available
        if (breakdown.getTransactionType() != null) {
            response.setTransTypeName(breakdown.getTransactionType().getTypeName());
        }

        return response;
    }

    /**
     * Map SalaryBreakdownRequest DTO to SalaryBreakdownPercentage entity
     */
    private SalaryBreakdownPercentage mapToEntity(SalaryBreakdownRequest request) {
        return SalaryBreakdownPercentage.builder()
                .employeeCategory(request.getEmployeeCategory())
                .transTypeCode(request.getTransTypeCode())
                .salaryPercentage(request.getSalaryPercentage())
                .isDeleted("N")
                .build();
    }
}
