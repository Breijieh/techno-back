package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.EmpMonthlyDeduction;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.TransactionType;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmpMonthlyDeductionRepository;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing employee monthly deductions.
 *
 * Handles:
 * - Manual deduction submission with approval workflow
 * - Deduction approval/rejection by authorized approvers
 * - Querying deductions by employee, date range, or approval status
 * - Soft deletion of pending deductions
 * - Cancellation of active deductions
 *
 * Manual deductions require Finance Manager approval before being applied to
 * payroll.
 * Auto-generated deductions (from attendance/loans) are pre-approved.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 6 - Allowances & Deductions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeductionService {

    private final EmpMonthlyDeductionRepository deductionRepository;
    private final EmployeeRepository employeeRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String REQUEST_TYPE = "DEDUCT";

    /**
     * Submit a manual deduction request.
     * Requires Finance Manager approval before being applied.
     *
     * @param employeeNo      Employee number
     * @param typeCode        Transaction type code (20-29 for deductions)
     * @param transactionDate Date of deduction
     * @param amount          Deduction amount
     * @param notes           Optional notes/reason
     * @return Created deduction record
     */
    @Transactional
    public EmpMonthlyDeduction submitDeduction(Long employeeNo, Long typeCode,
            LocalDate transactionDate,
            BigDecimal amount, String notes) {
        log.info("Submitting deduction for employee {}, type {}, amount {}",
                employeeNo, typeCode, amount);

        // Validate employee exists
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + employeeNo));

        // Validate transaction type
        TransactionType transactionType = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + typeCode));

        // Validate it's a deduction type
        if (!"D".equals(transactionType.getAllowanceDeduction())) {
            throw new BadRequestException(
                    "Ø±Ù…Ø² Ø§Ù„Ù†ÙˆØ¹ " + typeCode + " Ù„ÙŠØ³ Ù†ÙˆØ¹ Ø®ØµÙ…");
        }

        // Validate type code range (20-29 for deductions)
        if (typeCode < 20 || typeCode > 29) {
            throw new BadRequestException(
                    "Deduction type code must be between 20 and 29");
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Ù…Ø¨Ù„Øº Ø§Ù„Ø®ØµÙ… ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù…ÙˆØ¬Ø¨Ø§Ù‹");
        }

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                REQUEST_TYPE,
                employeeNo,
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        // Create deduction record
        EmpMonthlyDeduction deduction = EmpMonthlyDeduction.builder()
                .employeeNo(employeeNo)
                .typeCode(typeCode)
                .transactionDate(transactionDate)
                .deductionAmount(amount)
                .entryReason(notes)
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .isManualEntry("Y") // Manual entry
                .isDeleted("N")
                .build();

        deduction = deductionRepository.save(deduction);

        log.info("Deduction submitted successfully: {}", deduction.getTransactionNo());

        // Publish deduction submitted notification
        publishDeductionSubmittedNotification(deduction, employee, transactionType);

        return deduction;
    }

    /**
     * Approve a deduction request.
     * Only authorized approvers at the current approval level can approve.
     *
     * @param deductionId Deduction transaction number
     * @param approverNo  Employee number of approver
     * @return Updated deduction record
     */
    @Transactional
    public EmpMonthlyDeduction approveDeduction(Long deductionId, Long approverNo) {
        log.info("Approving deduction {} by approver {}", deductionId, approverNo);

        // Validate deduction exists
        EmpMonthlyDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø®ØµÙ… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + deductionId));

        // Validate not already processed
        if ("A".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø¹ØªÙ…Ø¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("R".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("C".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ù„ØºÙ‰");
        }
        if ("Y".equals(deduction.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø­Ø°ÙˆÙ");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                deduction.getNextAppLevel(),
                approverNo,
                deduction.getNextApproval())) {
            throw new BadRequestException(
                    "Ø§Ù„Ù…ÙˆØ¸Ù " + approverNo + " ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ù‡ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ù‡Ø°Ø§ Ø§Ù„Ø®ØµÙ…");
        }

        // Get employee for dept/project info
        Long employeeNo = deduction.getEmployeeNo();
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        // Move to next approval level
        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                REQUEST_TYPE,
                deduction.getNextAppLevel(),
                deduction.getEmployeeNo(),
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        // Update deduction with new approval info
        deduction.setTransStatus(nextLevel.getTransStatus());
        deduction.setNextApproval(nextLevel.getNextApproval());
        deduction.setNextAppLevel(nextLevel.getNextAppLevel());

        // If fully approved, set approval details
        if ("A".equals(nextLevel.getTransStatus())) {
            deduction.setApprovedBy(approverNo);
            deduction.setApprovedDate(LocalDateTime.now());
            log.info("Deduction {} fully approved by {}", deductionId, approverNo);

            // Save and publish approval notification
            deduction = deductionRepository.save(deduction);
            TransactionType transactionType = transactionTypeRepository.findById(deduction.getTypeCode())
                    .orElse(null);
            publishDeductionApprovedNotification(deduction, employee, transactionType);
        } else {
            log.info("Deduction {} moved to next approval level {}, next approver: {}",
                    deductionId, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
            deduction = deductionRepository.save(deduction);
        }

        return deduction;
    }

    /**
     * Reject a deduction request.
     *
     * @param deductionId     Deduction transaction number
     * @param approverNo      Employee number of approver
     * @param rejectionReason Reason for rejection
     * @return Updated deduction record
     */
    @Transactional
    public EmpMonthlyDeduction rejectDeduction(Long deductionId, Long approverNo,
            String rejectionReason) {
        log.info("Rejecting deduction {} by approver {}", deductionId, approverNo);

        // Validate deduction exists
        EmpMonthlyDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø®ØµÙ… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + deductionId));

        // Validate not already processed
        if ("A".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø±ÙØ¶ Ø§Ù„Ø®ØµÙ… Ø§Ù„Ù…Ø¹ØªÙ…Ø¯");
        }
        if ("R".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("C".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ù„ØºÙ‰");
        }
        if ("Y".equals(deduction.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø­Ø°ÙˆÙ");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                deduction.getNextAppLevel(),
                approverNo,
                deduction.getNextApproval())) {
            throw new BadRequestException(
                    "Ø§Ù„Ù…ÙˆØ¸Ù " + approverNo + " ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ù‡ Ø¨Ø±ÙØ¶ Ù‡Ø°Ø§ Ø§Ù„Ø®ØµÙ…");
        }

        // Validate rejection reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨");
        }

        // Reject the deduction
        deduction.setTransStatus("R");
        deduction.setRejectionReason(rejectionReason);
        deduction.setApprovedBy(approverNo);
        deduction.setApprovedDate(LocalDateTime.now());

        deduction = deductionRepository.save(deduction);
        log.info("Deduction {} rejected by {}", deductionId, approverNo);

        // Publish rejection notification
        Employee employee = employeeRepository.findById(deduction.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        TransactionType transactionType = transactionTypeRepository.findById(deduction.getTypeCode())
                .orElse(null);
        publishDeductionRejectedNotification(deduction, employee, transactionType, rejectionReason);

        return deduction;
    }

    /**
     * Cancel an active deduction.
     * Only active deductions can be cancelled.
     *
     * @param deductionId Deduction transaction number
     * @param requestorNo Employee number requesting cancellation
     */
    @Transactional
    public void cancelDeduction(Long deductionId, Long requestorNo) {
        log.info("Cancelling deduction {} by {}", deductionId, requestorNo);

        // Validate deduction exists
        EmpMonthlyDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø®ØµÙ… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + deductionId));

        // Validate deduction is active
        if (!"A".equals(deduction.getTransStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ø¥Ù„ØºØ§Ø¡ Ø§Ù„Ø®ØµÙˆÙ…Ø§Øª Ø§Ù„Ù†Ø´Ø·Ø© ÙÙ‚Ø·");
        }

        // Validate not already deleted
        if ("Y".equals(deduction.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø­Ø°ÙˆÙ");
        }

        // Cancel deduction
        deduction.setTransStatus("C");
        deductionRepository.save(deduction);

        log.info("Deduction {} cancelled by {}", deductionId, requestorNo);
    }

    /**
     * Delete (soft delete) a pending deduction.
     * Only pending deductions can be deleted.
     *
     * @param deductionId Deduction transaction number
     * @param requestorNo Employee number requesting deletion
     */
    @Transactional
    public void deleteDeduction(Long deductionId, Long requestorNo) {
        log.info("Deleting deduction {} by {}", deductionId, requestorNo);

        // Validate deduction exists
        EmpMonthlyDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø®ØµÙ… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + deductionId));

        // Validate deduction is pending (not approved)
        if ("A".equals(deduction.getTransStatus())) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø§Ù„Ø®ØµÙ… Ø§Ù„Ù…Ø¹ØªÙ…Ø¯. Ø§Ø³ØªØ®Ø¯Ù… Ø§Ù„Ø¥Ù„ØºØ§Ø¡ Ø¨Ø¯Ù„Ø§Ù‹ Ù…Ù† Ø°Ù„Ùƒ.");
        }

        // Validate not already deleted
        if ("Y".equals(deduction.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø®ØµÙ… Ù…Ø­Ø°ÙˆÙ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Soft delete
        deduction.setIsDeleted("Y");
        deductionRepository.save(deduction);

        log.info("Deduction {} soft deleted by {}", deductionId, requestorNo);
    }

    /**
     * Get all deductions for an employee.
     *
     * @param employeeNo Employee number
     * @return List of deductions
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyDeduction> getEmployeeDeductions(Long employeeNo) {
        log.debug("Getting deductions for employee {}", employeeNo);
        return deductionRepository.findByEmployeeNoAndIsDeleted(employeeNo, "N");
    }

    /**
     * Get deductions by date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of deductions in date range
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyDeduction> getDeductionsByDateRange(LocalDate startDate,
            LocalDate endDate) {
        log.debug("Getting deductions between {} and {}", startDate, endDate);
        return deductionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Get pending deductions for an approver.
     *
     * @param approverId Approver employee number
     * @return List of pending deductions
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyDeduction> getPendingDeductions(Long approverId) {
        log.debug("Getting pending deductions for approver {}", approverId);
        return deductionRepository.findPendingDeductionsByApprover(approverId);
    }

    /**
     * Get deduction by ID.
     *
     * @param deductionId Deduction transaction number
     * @return Deduction record
     */
    @Transactional(readOnly = true)
    public EmpMonthlyDeduction getDeductionById(Long deductionId) {
        return deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø®ØµÙ… ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + deductionId));
    }

    /**
     * Get all deductions with optional filters.
     * Used for listing all deductions in the deduction requests page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo  Employee number - optional
     * @param startDate   Start date (optional) - filters by transactionDate
     * @param endDate     End date (optional) - filters by transactionDate
     * @param pageable    Pagination parameters
     * @return Page of deduction records with employee data loaded
     */
    @Transactional(readOnly = true)
    public Page<EmpMonthlyDeduction> getAllDeductions(String transStatus, Long employeeNo,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all deductions with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        // Provide default dates if null to simplify JPQL query
        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.of(2099, 12, 31);

        return deductionRepository.findAllWithFilters(
                transStatus, employeeNo, effectiveStartDate, effectiveEndDate, pageable);
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when deduction is submitted.
     */
    private void publishDeductionSubmittedNotification(EmpMonthlyDeduction deduction,
            Employee employee,
            TransactionType transactionType) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("deductionType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("deductionAmount", deduction.getDeductionAmount().toString());
            variables.put("transactionDate", deduction.getTransactionDate().toString());
            variables.put("notes", deduction.getEntryReason() != null ? deduction.getEntryReason() : "");
            variables.put("linkUrl", "/deductions/" + deduction.getTransactionNo());

            // Notify the next approver
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.DEDUCTION_SUBMITTED,
                    deduction.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "DEDUCTION",
                    deduction.getTransactionNo(),
                    variables));

            log.debug("Published DEDUCTION_SUBMITTED notification for deduction {}", deduction.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish deduction submitted notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when deduction is approved.
     */
    private void publishDeductionApprovedNotification(EmpMonthlyDeduction deduction,
            Employee employee,
            TransactionType transactionType) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("deductionType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("deductionAmount", deduction.getDeductionAmount().toString());
            variables.put("transactionDate", deduction.getTransactionDate().toString());
            variables.put("linkUrl", "/deductions/" + deduction.getTransactionNo());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.DEDUCTION_APPROVED,
                    deduction.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "DEDUCTION",
                    deduction.getTransactionNo(),
                    variables));

            log.debug("Published DEDUCTION_APPROVED notification for deduction {}", deduction.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish deduction approved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when deduction is rejected.
     */
    private void publishDeductionRejectedNotification(EmpMonthlyDeduction deduction,
            Employee employee,
            TransactionType transactionType,
            String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("deductionType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("deductionAmount", deduction.getDeductionAmount().toString());
            variables.put("transactionDate", deduction.getTransactionDate().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "");
            variables.put("linkUrl", "/deductions/" + deduction.getTransactionNo());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.DEDUCTION_REJECTED,
                    deduction.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "DEDUCTION",
                    deduction.getTransactionNo(),
                    variables));

            log.debug("Published DEDUCTION_REJECTED notification for deduction {}", deduction.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish deduction rejected notification: {}", e.getMessage(), e);
        }
    }
}

