package com.techno.backend.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.EmpMonthlyAllowance;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.TransactionType;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmpMonthlyAllowanceRepository;
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
 * Service for managing employee monthly allowances.
 *
 * Handles:
 * - Manual allowance submission with approval workflow
 * - Allowance approval/rejection by authorized approvers
 * - Querying allowances by employee, date range, or approval status
 * - Soft deletion of pending allowances
 *
 * Manual allowances require HR Manager approval before being applied to
 * payroll.
 * Auto-generated allowances (from attendance) are pre-approved.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 6 - Allowances & Deductions
 */
@Service
@RequiredArgsConstructor
public class AllowanceService {

    private static final Logger log = LoggerFactory.getLogger(AllowanceService.class);

    private final EmpMonthlyAllowanceRepository allowanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String REQUEST_TYPE = "ALLOW";

    /**
     * Submit a manual allowance request.
     * Requires HR Manager approval before being applied.
     *
     * @param employeeNo      Employee number
     * @param typeCode        Transaction type code (10-19 for allowances)
     * @param transactionDate Date of allowance
     * @param amount          Allowance amount
     * @param notes           Optional notes/reason
     * @return Created allowance record
     */
    @Transactional
    public EmpMonthlyAllowance submitAllowance(Long employeeNo, Long typeCode,
            LocalDate transactionDate,
            BigDecimal amount, String notes) {
        log.info("Submitting allowance for employee {}, type {}, amount {}",
                employeeNo, typeCode, amount);

        // Validate employee exists
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + employeeNo));

        // Validate transaction type
        TransactionType transactionType = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ù†ÙˆØ¹ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + typeCode));

        // Validate it's an allowance type
        if (!"A".equals(transactionType.getAllowanceDeduction())) {
            throw new BadRequestException(
                    "Ø±Ù…Ø² Ø§Ù„Ù†ÙˆØ¹ " + typeCode + " Ù„ÙŠØ³ Ù†ÙˆØ¹ Ø¨Ø¯Ù„");
        }

        // Validate type code range (10-19 for allowances)
        if (typeCode < 10 || typeCode > 19) {
            throw new BadRequestException(
                    "Allowance type code must be between 10 and 19");
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Ù…Ø¨Ù„Øº Ø§Ù„Ø¨Ø¯Ù„ ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ù…ÙˆØ¬Ø¨Ø§Ù‹");
        }

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                REQUEST_TYPE,
                employeeNo,
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        // Create allowance record
        EmpMonthlyAllowance allowance = EmpMonthlyAllowance.builder()
                .employeeNo(employeeNo)
                .typeCode(typeCode)
                .transactionDate(transactionDate)
                .allowanceAmount(amount)
                .entryReason(notes)
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .isManualEntry("Y") // Manual entry
                .periodicalAllowance("N")
                .isDeleted("N")
                .build();

        allowance = allowanceRepository.save(allowance);

        log.info("Allowance submitted successfully: {}", allowance.getTransactionNo());

        // Publish allowance submitted notification
        publishAllowanceSubmittedNotification(allowance, employee, transactionType);

        return allowance;
    }

    /**
     * Approve an allowance request.
     * Only authorized approvers at the current approval level can approve.
     *
     * @param allowanceId Allowance transaction number
     * @param approverNo  Employee number of approver
     * @return Updated allowance record
     */
    @Transactional
    public EmpMonthlyAllowance approveAllowance(Long allowanceId, Long approverNo) {
        log.info("Approving allowance {} by approver {}", allowanceId, approverNo);

        // Validate allowance exists
        EmpMonthlyAllowance allowance = allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø¨Ø¯Ù„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + allowanceId));

        // Validate not already processed
        if ("A".equals(allowance.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø¹ØªÙ…Ø¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("R".equals(allowance.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("Y".equals(allowance.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø­Ø°ÙˆÙ");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                allowance.getNextAppLevel(),
                approverNo,
                allowance.getNextApproval())) {
            throw new BadRequestException(
                    "Ø§Ù„Ù…ÙˆØ¸Ù " + approverNo + " ØºÙŠØ± Ù…Ø®ÙˆÙ„ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ù‡Ø°Ø§ Ø§Ù„Ø¨Ø¯Ù„");
        }

        // Get employee for dept/project info
        Long employeeNo = allowance.getEmployeeNo();
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeNo));

        // Move to next approval level
        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                REQUEST_TYPE,
                allowance.getNextAppLevel(),
                allowance.getEmployeeNo(),
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        // Update allowance with new approval info
        allowance.setTransStatus(nextLevel.getTransStatus());
        allowance.setNextApproval(nextLevel.getNextApproval());
        allowance.setNextAppLevel(nextLevel.getNextAppLevel());

        // If fully approved, set approval details
        if ("A".equals(nextLevel.getTransStatus())) {
            allowance.setApprovedBy(approverNo);
            allowance.setApprovedDate(LocalDateTime.now());
            log.info("Allowance {} fully approved by {}", allowanceId, approverNo);

            // Save and publish approval notification
            allowance = allowanceRepository.save(allowance);
            TransactionType transactionType = transactionTypeRepository.findById(allowance.getTypeCode())
                    .orElse(null);
            publishAllowanceApprovedNotification(allowance, employee, transactionType);
        } else {
            log.info("Allowance {} moved to next approval level {}, next approver: {}",
                    allowanceId, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
            allowance = allowanceRepository.save(allowance);
        }

        return allowance;
    }

    /**
     * Reject an allowance request.
     *
     * @param allowanceId     Allowance transaction number
     * @param approverNo      Employee number of approver
     * @param rejectionReason Reason for rejection
     * @return Updated allowance record
     */
    @Transactional
    public EmpMonthlyAllowance rejectAllowance(Long allowanceId, Long approverNo,
            String rejectionReason) {
        log.info("Rejecting allowance {} by approver {}", allowanceId, approverNo);

        // Validate allowance exists
        EmpMonthlyAllowance allowance = allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø¨Ø¯Ù„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + allowanceId));

        // Validate not already processed
        if ("A".equals(allowance.getTransStatus())) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø±ÙØ¶ Ø§Ù„Ø¨Ø¯Ù„ Ø§Ù„Ù…Ø¹ØªÙ…Ø¯");
        }
        if ("R".equals(allowance.getTransStatus())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø±ÙÙˆØ¶ Ø¨Ø§Ù„ÙØ¹Ù„");
        }
        if ("Y".equals(allowance.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø­Ø°ÙˆÙ");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                allowance.getNextAppLevel(),
                approverNo,
                allowance.getNextApproval())) {
            throw new BadRequestException(
                    "Ø§Ù„Ù…ÙˆØ¸Ù " + approverNo + " ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ù‡ Ø¨Ø±ÙØ¶ Ù‡Ø°Ø§ Ø§Ù„Ø¨Ø¯Ù„");
        }

        // Validate rejection reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("Ø³Ø¨Ø¨ Ø§Ù„Ø±ÙØ¶ Ù…Ø·Ù„ÙˆØ¨");
        }

        // Reject the allowance
        allowance.setTransStatus("R");
        allowance.setRejectionReason(rejectionReason);
        allowance.setApprovedBy(approverNo);
        allowance.setApprovedDate(LocalDateTime.now());

        allowance = allowanceRepository.save(allowance);
        log.info("Allowance {} rejected by {}", allowanceId, approverNo);

        // Publish rejection notification
        Employee employee = employeeRepository.findById(allowance.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        TransactionType transactionType = transactionTypeRepository.findById(allowance.getTypeCode())
                .orElse(null);
        publishAllowanceRejectedNotification(allowance, employee, transactionType, rejectionReason);

        return allowance;
    }

    /**
     * Delete (soft delete) a pending allowance.
     * Only pending allowances can be deleted.
     *
     * @param allowanceId Allowance transaction number
     * @param requestorNo Employee number requesting deletion
     */
    @Transactional
    public void deleteAllowance(Long allowanceId, Long requestorNo) {
        log.info("Deleting allowance {} by {}", allowanceId, requestorNo);

        // Validate allowance exists
        EmpMonthlyAllowance allowance = allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø¨Ø¯Ù„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + allowanceId));

        // Validate allowance is pending (not approved)
        if ("A".equals(allowance.getTransStatus())) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø§Ù„Ø¨Ø¯Ù„ Ø§Ù„Ù…Ø¹ØªÙ…Ø¯");
        }

        // Validate not already deleted
        if ("Y".equals(allowance.getIsDeleted())) {
            throw new BadRequestException("Ø§Ù„Ø¨Ø¯Ù„ Ù…Ø­Ø°ÙˆÙ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Soft delete
        allowance.setIsDeleted("Y");
        allowanceRepository.save(allowance);

        log.info("Allowance {} soft deleted by {}", allowanceId, requestorNo);
    }

    /**
     * Get all allowances for an employee.
     *
     * @param employeeNo Employee number
     * @return List of allowances
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyAllowance> getEmployeeAllowances(Long employeeNo) {
        log.debug("Getting allowances for employee {}", employeeNo);
        return allowanceRepository.findByEmployeeNoAndIsDeleted(employeeNo, "N");
    }

    /**
     * Get allowances by date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of allowances in date range
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyAllowance> getAllowancesByDateRange(LocalDate startDate,
            LocalDate endDate) {
        log.debug("Getting allowances between {} and {}", startDate, endDate);
        return allowanceRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Get pending allowances for an approver.
     *
     * @param approverId Approver employee number
     * @return List of pending allowances
     */
    @Transactional(readOnly = true)
    public List<EmpMonthlyAllowance> getPendingAllowances(Long approverId) {
        log.debug("Getting pending allowances for approver {}", approverId);
        return allowanceRepository.findPendingAllowancesByApprover(approverId);
    }

    /**
     * Get allowance by ID.
     *
     * @param allowanceId Allowance transaction number
     * @return Allowance record
     */
    @Transactional(readOnly = true)
    public EmpMonthlyAllowance getAllowanceById(Long allowanceId) {
        return allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ø§Ù„Ø¨Ø¯Ù„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + allowanceId));
    }

    /**
     * Get all allowances with optional filters.
     * Used for listing all allowances in the allowance requests page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo  Employee number - optional
     * @param startDate   Start date (optional) - filters by transactionDate
     * @param endDate     End date (optional) - filters by transactionDate
     * @param pageable    Pagination parameters
     * @return Page of allowance records with employee data loaded
     */

    @Transactional(readOnly = true)
    public Page<EmpMonthlyAllowance> getAllAllowances(String transStatus, Long employeeNo,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all allowances with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        // Provide default dates if null to simplify JPQL query
        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.of(2099, 12, 31);

        return allowanceRepository.findAllWithFilters(
                transStatus, employeeNo, effectiveStartDate, effectiveEndDate, pageable);
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when allowance is submitted.
     */
    private void publishAllowanceSubmittedNotification(EmpMonthlyAllowance allowance,
            Employee employee,
            TransactionType transactionType) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("allowanceType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("allowanceAmount", allowance.getAllowanceAmount().toString());
            variables.put("transactionDate", allowance.getTransactionDate().toString());
            variables.put("notes", allowance.getEntryReason() != null ? allowance.getEntryReason() : "");
            variables.put("linkUrl", "/allowances/" + allowance.getTransactionNo());

            // Notify the next approver
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.ALLOWANCE_SUBMITTED,
                    allowance.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "ALLOWANCE",
                    allowance.getTransactionNo(),
                    variables));

            log.debug("Published ALLOWANCE_SUBMITTED notification for allowance {}", allowance.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish allowance submitted notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when allowance is approved.
     */
    private void publishAllowanceApprovedNotification(EmpMonthlyAllowance allowance,
            Employee employee,
            TransactionType transactionType) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("allowanceType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("allowanceAmount", allowance.getAllowanceAmount().toString());
            variables.put("transactionDate", allowance.getTransactionDate().toString());
            variables.put("linkUrl", "/allowances/" + allowance.getTransactionNo());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.ALLOWANCE_APPROVED,
                    allowance.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "ALLOWANCE",
                    allowance.getTransactionNo(),
                    variables));

            log.debug("Published ALLOWANCE_APPROVED notification for allowance {}", allowance.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish allowance approved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when allowance is rejected.
     */
    private void publishAllowanceRejectedNotification(EmpMonthlyAllowance allowance,
            Employee employee,
            TransactionType transactionType,
            String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("allowanceType", transactionType != null ? transactionType.getTypeName() : "ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ");
            variables.put("allowanceAmount", allowance.getAllowanceAmount().toString());
            variables.put("transactionDate", allowance.getTransactionDate().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "");
            variables.put("linkUrl", "/allowances/" + allowance.getTransactionNo());

            // Notify the employee
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.ALLOWANCE_REJECTED,
                    allowance.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "ALLOWANCE",
                    allowance.getTransactionNo(),
                    variables));

            log.debug("Published ALLOWANCE_REJECTED notification for allowance {}", allowance.getTransactionNo());
        } catch (Exception e) {
            log.error("Failed to publish allowance rejected notification: {}", e.getMessage(), e);
        }
    }
}

