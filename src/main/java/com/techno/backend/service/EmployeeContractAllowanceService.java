package com.techno.backend.service;

import com.techno.backend.dto.EmployeeContractAllowanceRequest;
import com.techno.backend.dto.EmployeeContractAllowanceResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmployeeContractAllowance;
import com.techno.backend.entity.TransactionType;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmployeeContractAllowanceRepository;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Employee Contract Allowance management
 * Handles business logic for employee contract allowance operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeContractAllowanceService {

    private final EmployeeContractAllowanceRepository allowanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    /**
     * Get all employee contract allowances (both active and inactive)
     *
     * @return List of all allowances
     */
    @Transactional(readOnly = true)
    public List<EmployeeContractAllowanceResponse> getAllAllowances() {
        log.info("Fetching all employee contract allowances");
        List<EmployeeContractAllowance> allowances = allowanceRepository.findAllByOrderByCreatedDateDesc();
        return allowances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get employee contract allowance by ID
     *
     * @param recordId the record ID
     * @return EmployeeContractAllowanceResponse
     */
    @Transactional(readOnly = true)
    public EmployeeContractAllowanceResponse getAllowanceById(Long recordId) {
        log.info("Fetching employee contract allowance by ID: {}", recordId);
        EmployeeContractAllowance allowance = findAllowanceOrThrow(recordId);
        return mapToResponse(allowance);
    }

    /**
     * Get allowances for a specific employee
     *
     * @param employeeNo the employee number
     * @return List of allowances for the employee
     */
    @Transactional(readOnly = true)
    public List<EmployeeContractAllowanceResponse> getAllowancesByEmployee(Long employeeNo) {
        log.info("Fetching employee contract allowances for employee: {}", employeeNo);
        List<EmployeeContractAllowance> allowances = allowanceRepository.findByEmployeeNoAndIsActiveOrderByCreatedDateDesc(employeeNo, 'Y');
        return allowances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new employee contract allowance
     *
     * @param request the create request
     * @return created EmployeeContractAllowanceResponse
     */
    @Transactional
    public EmployeeContractAllowanceResponse createAllowance(EmployeeContractAllowanceRequest request) {
        log.info("Creating employee contract allowance - employee: {}, type: {}, percentage: {}",
                request.getEmployeeNo(), request.getTransTypeCode(), request.getSalaryPercentage());

        // Check uniqueness (employee_no + trans_type_code)
        if (allowanceRepository.existsByEmployeeNoAndTransTypeCode(request.getEmployeeNo(), request.getTransTypeCode())) {
            throw new BadRequestException(
                    "Ø§Ù„Ø¨Ø¯Ù„ Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù„Ù…ÙˆØ¸Ù " + request.getEmployeeNo() + " Ù…Ø¹ Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© " + request.getTransTypeCode());
        }

        // Verify employee exists
        employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + request.getEmployeeNo()));

        // Verify transaction type exists
        TransactionType transactionType = transactionTypeRepository.findById(request.getTransTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + request.getTransTypeCode()));

        // Verify it's an allowance type (not deduction)
        if (!"A".equals(transactionType.getAllowanceDeduction())) {
            throw new BadRequestException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù†ÙˆØ¹ Ø¨Ø¯Ù„ (A)ØŒ ÙˆÙ„ÙŠØ³ Ù†ÙˆØ¹ Ø®ØµÙ… (D)");
        }

        EmployeeContractAllowance allowance = mapToEntity(request);
        allowance = allowanceRepository.save(allowance);
        return mapToResponse(allowance);
    }

    /**
     * Update an existing employee contract allowance
     *
     * @param recordId the record ID
     * @param request  the update request
     * @return updated EmployeeContractAllowanceResponse
     */
    @Transactional
    public EmployeeContractAllowanceResponse updateAllowance(Long recordId, EmployeeContractAllowanceRequest request) {
        log.info("Updating employee contract allowance - recordId: {}, employee: {}, type: {}, percentage: {}",
                recordId, request.getEmployeeNo(), request.getTransTypeCode(), request.getSalaryPercentage());

        EmployeeContractAllowance allowance = findAllowanceOrThrow(recordId);

        // Check uniqueness excluding current record (for updates)
        if (allowanceRepository.existsByEmployeeNoAndTransTypeCodeAndRecordIdNot(
                request.getEmployeeNo(), request.getTransTypeCode(), recordId)) {
            throw new BadRequestException(
                    "Ø§Ù„Ø¨Ø¯Ù„ Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù„Ù…ÙˆØ¸Ù " + request.getEmployeeNo() + " Ù…Ø¹ Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© " + request.getTransTypeCode());
        }

        // Verify employee exists (if changed)
        if (!allowance.getEmployeeNo().equals(request.getEmployeeNo())) {
            employeeRepository.findById(request.getEmployeeNo())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + request.getEmployeeNo()));
        }

        // Verify transaction type exists (if changed)
        if (!allowance.getTransTypeCode().equals(request.getTransTypeCode())) {
            TransactionType transactionType = transactionTypeRepository.findById(request.getTransTypeCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + request.getTransTypeCode()));

            // Verify it's an allowance type
            if (!"A".equals(transactionType.getAllowanceDeduction())) {
                throw new BadRequestException("Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù†ÙˆØ¹ Ø¨Ø¯Ù„ (A)ØŒ ÙˆÙ„ÙŠØ³ Ù†ÙˆØ¹ Ø®ØµÙ… (D)");
            }
        }

        updateEntityFromRequest(allowance, request);
        allowance = allowanceRepository.save(allowance);
        return mapToResponse(allowance);
    }

    /**
     * Delete (soft delete) an employee contract allowance
     *
     * @param recordId the record ID
     */
    @Transactional
    public void deleteAllowance(Long recordId) {
        log.info("Deleting employee contract allowance - recordId: {}", recordId);
        EmployeeContractAllowance allowance = findAllowanceOrThrow(recordId);
        allowance.deactivate();
        allowanceRepository.save(allowance);
    }

    // Private helper methods

    private EmployeeContractAllowance findAllowanceOrThrow(Long recordId) {
        return allowanceRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¨Ø¯Ù„ Ø¹Ù‚Ø¯ Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + recordId));
    }

    private EmployeeContractAllowanceResponse mapToResponse(EmployeeContractAllowance allowance) {
        // Fetch employee and transaction type for names (if not already loaded)
        String employeeName = null;
        String transactionName = null;

        if (allowance.getEmployee() != null) {
            employeeName = allowance.getEmployee().getEmployeeName();
        } else {
            Employee employee = employeeRepository.findById(allowance.getEmployeeNo()).orElse(null);
            if (employee != null) {
                employeeName = employee.getEmployeeName();
            }
        }

        if (allowance.getTransactionType() != null) {
            transactionName = allowance.getTransactionType().getTypeName();
        } else {
            TransactionType transactionType = transactionTypeRepository.findById(allowance.getTransTypeCode()).orElse(null);
            if (transactionType != null) {
                transactionName = transactionType.getTypeName();
            }
        }

        return EmployeeContractAllowanceResponse.builder()
                .recordId(allowance.getRecordId())
                .employeeNo(allowance.getEmployeeNo())
                .employeeName(employeeName)
                .transTypeCode(allowance.getTransTypeCode())
                .transactionName(transactionName)
                .salaryPercentage(allowance.getSalaryPercentage())
                .isActive(allowance.isActive())
                .createdDate(allowance.getCreatedDate())
                .build();
    }

    private EmployeeContractAllowance mapToEntity(EmployeeContractAllowanceRequest request) {
        return EmployeeContractAllowance.builder()
                .employeeNo(request.getEmployeeNo())
                .transTypeCode(request.getTransTypeCode())
                .salaryPercentage(request.getSalaryPercentage())
                .isActive(request.getIsActive() ? 'Y' : 'N')
                .build();
    }

    private void updateEntityFromRequest(EmployeeContractAllowance allowance, EmployeeContractAllowanceRequest request) {
        allowance.setEmployeeNo(request.getEmployeeNo());
        allowance.setTransTypeCode(request.getTransTypeCode());
        allowance.setSalaryPercentage(request.getSalaryPercentage());
        allowance.setIsActive(request.getIsActive() ? 'Y' : 'N');
    }
}


