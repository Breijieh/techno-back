package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmployeeLeave;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.EmployeeLeaveRepository;
import com.techno.backend.repository.EmployeeRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for employee leave management.
 *
 * Handles:
 * - Leave request submission
 * - Multi-level approval workflow (Manager â†’ Project Manager â†’ HR)
 * - Leave balance validation and deduction
 * - Overlapping leave detection
 * - Leave history tracking
 *
 * Approval Flow for Leave (VAC):
 * - Level 1: Project Manager approval
 * - Level 2: HR Manager approval
 * - Level 3: Final approval (close level)
 *
 * Leave Balance Rules:
 * - Balance deducted only after FINAL approval
 * - Half-day leaves supported (0.5 days)
 * - Negative balance not allowed
 * - Overlapping leaves not allowed
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Leave Management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveService {

    private final EmployeeLeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final HolidayService holidayService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String REQUEST_TYPE = "VAC";

    /**
     * Submit new leave request.
     *
     * Validates:
     * - Employee exists and is active
     * - Leave dates are valid
     * - No overlapping leaves
     * - Sufficient leave balance
     *
     * @param employeeNo    Employee number
     * @param leaveFromDate Leave start date
     * @param leaveToDate   Leave end date
     * @param leaveReason   Leave reason
     * @return Created leave request
     */
    @Transactional
    public EmployeeLeave submitLeaveRequest(Long employeeNo, LocalDate leaveFromDate,
            LocalDate leaveToDate, String leaveReason) {
        log.info("Submitting leave request for employee {}: {} to {}", employeeNo, leaveFromDate, leaveToDate);

        // Validate employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new RuntimeException(
                    "ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…ÙˆØ¸Ù ÙŠÙ† Ø§Ù„Ù†Ø´Ø·ÙŠÙ† Ù Ù‚Ø· ØªÙ‚Ø¯ÙŠÙ… Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¥Ø¬Ø§Ø²Ø©");
        }

        if (employee.getEmpContractType() != null && !"TECHNO".equals(employee.getEmpContractType())) {
            throw new RuntimeException("Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù…ØªØ§Ø­Ø© Ù Ù‚Ø· Ù„Ù…ÙˆØ¸Ù ÙŠ Ø¹Ù‚ÙˆØ¯ ØªÙƒÙ†Ùˆ");
        }

        // Validate dates
        if (leaveFromDate.isAfter(leaveToDate)) {
            throw new RuntimeException(
                    "ØªØ§Ø±ÙŠØ® Ø¨Ø¯Ø§ÙŠØ© Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ù†Ù‡Ø§ÙŠØ©");
        }

        if (leaveFromDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Ù„Ø§ ÙŠÙ…ÙƒÙ† ØªÙ‚Ø¯ÙŠÙ… Ø¥Ø¬Ø§Ø²Ø© Ù„Ù„ØªÙˆØ§Ø±ÙŠØ® Ø§Ù„Ù…Ø§Ø¶ÙŠØ©");
        }

        // Calculate leave days (excluding weekends and holidays)
        BigDecimal leaveDays = calculateLeaveDays(leaveFromDate, leaveToDate);

        if (leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "ÙØªØ±Ø© Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ÙŠØ¬Ø¨ Ø£Ù† ØªØ´Ù…Ù„ Ø¹Ù„Ù‰ Ø§Ù„Ø£Ù‚Ù„ ÙŠÙˆÙ… Ø¹Ù…Ù„ ÙˆØ§Ø­Ø¯");
        }

        // Check for overlapping leaves
        List<EmployeeLeave> overlappingLeaves = leaveRepository
                .findOverlappingLeaves(employeeNo, leaveFromDate, leaveToDate);

        if (!overlappingLeaves.isEmpty()) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ÙŠØªØ¯Ø§Ø®Ù„ Ù…Ø¹ Ø¥Ø¬Ø§Ø²Ø© Ù…ÙˆØ¬ÙˆØ¯Ø© Ù…Ù† " +
                    overlappingLeaves.get(0).getLeaveFromDate() + " Ø¥Ù„Ù‰ " +
                    overlappingLeaves.get(0).getLeaveToDate());
        }

        // Check leave balance
        BigDecimal currentBalance = employee.getLeaveBalanceDays();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        if (currentBalance.compareTo(leaveDays) < 0) {
            throw new RuntimeException(String.format(
                    "Ø±ØµÙŠØ¯ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± ÙƒØ§Ù. Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %.1f ÙŠÙˆÙ…ØŒ Ø§Ù„Ù…ØªØ§Ø­: %.1f ÙŠÙˆÙ…",
                    leaveDays, currentBalance));
        }

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService
                .initializeApproval(REQUEST_TYPE, employeeNo,
                        employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        // Create leave request
        EmployeeLeave leave = EmployeeLeave.builder()
                .employeeNo(employeeNo)
                .leaveFromDate(leaveFromDate)
                .leaveToDate(leaveToDate)
                .leaveDays(leaveDays)
                .leaveReason(leaveReason)
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .nextAppLevelName(approvalInfo.getNextAppLevelName())
                .requestDate(LocalDate.now())
                .build();

        leave = leaveRepository.save(leave);

        log.info("Leave request created: ID={}, Days={}, Next Approver={}",
                leave.getLeaveId(), leaveDays, approvalInfo.getNextApproval());

        // Publish notification event for leave submission
        publishLeaveSubmittedNotification(leave, employee);

        return leave;
    }

    /**
     * Approve leave request at current approval level.
     *
     * If this is the final approval level:
     * - Set status to Approved (A)
     * - Deduct from employee leave balance
     *
     * Otherwise:
     * - Move to next approval level
     *
     * @param leaveId    Leave request ID
     * @param approverNo Approver employee number
     * @return Updated leave request
     */
    @Transactional
    public EmployeeLeave approveLeave(Long leaveId, Long approverNo) {
        log.info("Approving leave request {}, approver: {}", leaveId, approverNo);

        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leaveId));

        // Validate request is pending approval
        if (!"N".equals(leave.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©. Ø§Ù„Ø­Ø§Ù„Ø©: "
                    + leave.getTransStatus());
        }

        // Validate approver
        if (!approvalWorkflowService.canApprove(REQUEST_TYPE, leave.getNextAppLevel(),
                approverNo, leave.getNextApproval())) {
            throw new RuntimeException(
                    "Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù‡Ø°Ø§");
        }

        // Move to next level or finalize
        Employee employee = employeeRepository.findById(leave.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                REQUEST_TYPE, leave.getNextAppLevel(),
                leave.getEmployeeNo(), employee.getPrimaryDeptCode(), employee.getPrimaryProjectCode());

        leave.setTransStatus(nextLevel.getTransStatus());
        leave.setNextApproval(nextLevel.getNextApproval());
        leave.setNextAppLevel(nextLevel.getNextAppLevel());
        leave.setNextAppLevelName(nextLevel.getNextAppLevelName());

        // If fully approved, deduct from balance
        if ("A".equals(nextLevel.getTransStatus())) {
            deductLeaveBalance(employee, leave.getLeaveDays());
            leave.setApprovedDate(LocalDateTime.now());
            leave.setApprovedBy(approverNo);
            log.info("Leave fully approved. Deducted {} days from employee {} balance",
                    leave.getLeaveDays(), employee.getEmployeeNo());

            // Save and publish final approval notification
            leave = leaveRepository.save(leave);
            publishLeaveFinalApprovedNotification(leave, employee);
        } else {
            log.info("Leave moved to next approval level: {}, Next Approver: {}",
                    nextLevel.getNextAppLevel(), nextLevel.getNextApproval());

            // Save and publish intermediate approval notification
            leave = leaveRepository.save(leave);
            publishLeaveIntermediateApprovedNotification(leave, employee);
        }

        return leave;
    }

    /**
     * Reject leave request.
     *
     * Sets status to Rejected (R).
     * Leave balance is NOT deducted.
     *
     * @param leaveId         Leave request ID
     * @param approverNo      Approver employee number
     * @param rejectionReason Rejection reason
     * @return Updated leave request
     */
    @Transactional
    public EmployeeLeave rejectLeave(Long leaveId, Long approverNo, String rejectionReason) {
        log.info("Rejecting leave request {}, approver: {}", leaveId, approverNo);

        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leaveId));

        // Validate request is pending approval
        if (!"N".equals(leave.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù„ÙŠØ³ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø©");
        }

        // Validate approver
        if (!approvalWorkflowService.canApprove(REQUEST_TYPE, leave.getNextAppLevel(),
                approverNo, leave.getNextApproval())) {
            throw new RuntimeException("Ø£Ù†Øª ØºÙŠØ± Ù…ØµØ±Ø­ Ù„Ùƒ Ø¨Ø±ÙØ¶ Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ù‡Ø°Ø§");
        }

        leave.setTransStatus("R");
        leave.setNextApproval(null);
        leave.setNextAppLevel(null);
        leave.setNextAppLevelName(null);
        leave.setRejectionReason(rejectionReason);

        log.info("Leave request {} rejected", leaveId);
        leave = leaveRepository.save(leave);

        // Publish rejection notification
        Employee employee = employeeRepository.findById(leave.getEmployeeNo())
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        publishLeaveRejectedNotification(leave, employee, rejectionReason);

        return leave;
    }

    /**
     * Cancel leave request.
     *
     * Can only cancel:
     * - Own pending requests (not yet approved)
     * - Own approved requests (if leave hasn't started yet)
     *
     * If approved leave is cancelled, balance is refunded.
     *
     * @param leaveId            Leave request ID
     * @param employeeNo         Employee cancelling the request
     * @param cancellationReason Cancellation reason
     * @return Updated leave request
     */
    @Transactional
    public EmployeeLeave cancelLeaveRequest(Long leaveId, Long employeeNo, String cancellationReason) {
        log.info("Cancelling leave request {}, employee: {}", leaveId, employeeNo);

        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + leaveId));

        // Validate ownership
        if (!leave.getEmployeeNo().equals(employeeNo)) {
            throw new RuntimeException("ÙŠÙ…ÙƒÙ†Ùƒ Ø¥Ù„ØºØ§Ø¡ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ø§Ù„Ø®Ø§ØµØ© Ø¨Ùƒ ÙÙ‚Ø·");
        }

        // Validate not already cancelled or rejected
        if ("C".equals(leave.getTransStatus()) || "R".equals(leave.getTransStatus())) {
            throw new RuntimeException("Ø·Ù„Ø¨ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© ØªÙ… Ø¥Ù„ØºØ§Ø¤Ù‡ Ø£Ùˆ Ø±ÙØ¶Ù‡ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Check if leave has already started
        if (leave.getLeaveFromDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø¥Ù„ØºØ§Ø¡ Ø§Ù„Ø¥Ø¬Ø§Ø²Ø© Ø§Ù„ØªÙŠ Ø¨Ø¯Ø£Øª Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Refund balance if leave was approved
        if ("A".equals(leave.getTransStatus())) {
            Employee employee = employeeRepository.findById(employeeNo)
                    .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
            refundLeaveBalance(employee, leave.getLeaveDays());
            log.info("Refunded {} days to employee {} balance", leave.getLeaveDays(), employeeNo);
        }

        leave.setTransStatus("C");
        leave.setNextApproval(null);
        leave.setNextAppLevel(null);
        leave.setRejectionReason("Cancelled: " + cancellationReason);

        log.info("Leave request {} cancelled", leaveId);
        leave = leaveRepository.save(leave);

        // Publish cancellation notification (notify approver if pending)
        if (leave.getNextApproval() != null) {
            Employee approver = employeeRepository.findById(leave.getNextApproval())
                    .orElse(null);
            if (approver != null) {
                publishLeaveCancelledNotification(leave, employeeRepository.findById(employeeNo)
                        .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯")), approver);
            }
        }

        return leave;
    }

    /**
     * Get pending leave requests for an approver.
     *
     * @param approverNo Approver employee number
     * @return List of pending leaves
     */
    @Transactional(readOnly = true)
    public List<EmployeeLeave> getPendingLeavesForApprover(Long approverNo) {
        return leaveRepository.findPendingLeavesByApprover(approverNo);
    }

    /**
     * Get leave history for an employee.
     *
     * @param employeeNo Employee number
     * @return List of all approved leaves
     */
    @Transactional(readOnly = true)
    public List<EmployeeLeave> getEmployeeLeaveHistory(Long employeeNo) {
        return leaveRepository.findByEmployeeNoOrderByRequestDateDesc(employeeNo);
    }

    /**
     * Calculate leave balance for an employee.
     *
     * @param employeeNo Employee number
     * @return Current leave balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getLeaveBalance(Long employeeNo) {
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + employeeNo));

        return employee.getLeaveBalanceDays() != null ? employee.getLeaveBalanceDays() : BigDecimal.ZERO;
    }

    /**
     * Get all employees' leave balances with optional filters.
     * Used for listing all employees' leave balances in the leave balance page.
     *
     * @param employeeNo       Employee number - optional
     * @param departmentCode   Department code - optional
     * @param employmentStatus Employment status - optional (defaults to ACTIVE if
     *                         not provided)
     * @param pageable         Pagination parameters
     * @return Page of employees with leave balance data
     */
    @Transactional(readOnly = true)
    public Page<Employee> getAllLeaveBalances(Long employeeNo, Long departmentCode,
            String employmentStatus, Pageable pageable) {
        log.debug("Fetching all leave balances with filters: employee={}, department={}, status={}",
                employeeNo, departmentCode, employmentStatus);

        // Default to ACTIVE if no status provided
        if (employmentStatus == null) {
            employmentStatus = "ACTIVE";
        }

        Page<Employee> employeePage = employeeRepository.findAllWithLeaveBalance(
                employeeNo, departmentCode, employmentStatus, pageable);

        // Employees already have leaveBalanceDays loaded
        return employeePage;
    }

    /**
     * Get all leave requests with optional filters.
     * Used for listing all leave requests in the leave requests page.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo  Employee number - optional
     * @param startDate   Start date (optional) - filters by leaveFromDate
     * @param endDate     End date (optional) - filters by leaveToDate
     * @param pageable    Pagination parameters
     * @return Page of leave requests with employee names enriched
     */
    @Transactional(readOnly = true)
    public Page<EmployeeLeave> getAllLeaves(String transStatus, Long employeeNo,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        log.debug("Fetching all leave requests with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        Page<EmployeeLeave> leavePage = leaveRepository.findAllWithFilters(
                transStatus, employeeNo, startDate, endDate, pageable);

        // Extract unique employee numbers for bulk fetching
        Set<Long> employeeNos = leavePage.getContent().stream()
                .map(EmployeeLeave::getEmployeeNo)
                .collect(Collectors.toSet());

        // Fetch all employees in bulk
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeNos.isEmpty()) {
            employeeRepository.findAllById(employeeNos).forEach(emp -> employeeMap.put(emp.getEmployeeNo(), emp));
        }

        // Enrich leave records with employee data (lazy loading)
        // Note: The employee relationship is already defined in EmployeeLeave entity
        // We just ensure it's loaded for the response
        return leavePage.map(leave -> {
            // Ensure employee is loaded (if not already)
            if (leave.getEmployee() == null && employeeMap.containsKey(leave.getEmployeeNo())) {
                leave.setEmployee(employeeMap.get(leave.getEmployeeNo()));
            }
            return leave;
        });
    }

    // ==================== Private Helper Methods ====================

    /**
     * Calculate number of working days in leave period.
     *
     * Excludes weekends (Friday/Saturday) and public holidays.
     */
    private BigDecimal calculateLeaveDays(LocalDate fromDate, LocalDate toDate) {
        long totalDays = 0;
        LocalDate current = fromDate;

        while (!current.isAfter(toDate)) {
            // Skip weekends (Saudi: Friday=5, Saturday=6)
            int dayOfWeek = current.getDayOfWeek().getValue();
            if (dayOfWeek != 5 && dayOfWeek != 6) {
                // Skip public holidays
                if (!holidayService.isHoliday(current)) {
                    totalDays++;
                }
            }
            current = current.plusDays(1);
        }

        return BigDecimal.valueOf(totalDays);
    }

    /**
     * Deduct leave days from employee balance.
     */
    private void deductLeaveBalance(Employee employee, BigDecimal days) {
        BigDecimal currentBalance = employee.getLeaveBalanceDays() != null ? employee.getLeaveBalanceDays()
                : BigDecimal.ZERO;

        BigDecimal newBalance = currentBalance.subtract(days);
        employee.setLeaveBalanceDays(newBalance);
        employeeRepository.save(employee);

        log.debug("Leave balance updated for employee {}: {} â†’ {}",
                employee.getEmployeeNo(), currentBalance, newBalance);
    }

    /**
     * Refund leave days to employee balance.
     */
    private void refundLeaveBalance(Employee employee, BigDecimal days) {
        BigDecimal currentBalance = employee.getLeaveBalanceDays() != null ? employee.getLeaveBalanceDays()
                : BigDecimal.ZERO;

        BigDecimal newBalance = currentBalance.add(days);
        employee.setLeaveBalanceDays(newBalance);
        employeeRepository.save(employee);

        log.debug("Leave balance refunded for employee {}: {} â†’ {}",
                employee.getEmployeeNo(), currentBalance, newBalance);
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Publish notification when leave is submitted.
     */
    private void publishLeaveSubmittedNotification(EmployeeLeave leave, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("leaveFromDate", leave.getLeaveFromDate().toString());
            variables.put("leaveToDate", leave.getLeaveToDate().toString());
            variables.put("leaveDays", leave.getLeaveDays().toString());
            variables.put("leaveReason", leave.getLeaveReason() != null ? leave.getLeaveReason() : "");
            variables.put("linkUrl", "/leaves/" + leave.getLeaveId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LEAVE_SUBMITTED,
                    leave.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "LEAVE",
                    leave.getLeaveId(),
                    variables));

            log.debug("Published LEAVE_SUBMITTED notification for leave ID: {}", leave.getLeaveId());
        } catch (Exception e) {
            log.error("Failed to publish leave submitted notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when leave is approved (intermediate level).
     */
    private void publishLeaveIntermediateApprovedNotification(EmployeeLeave leave, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("leaveFromDate", leave.getLeaveFromDate().toString());
            variables.put("leaveToDate", leave.getLeaveToDate().toString());
            variables.put("leaveDays", leave.getLeaveDays().toString());
            variables.put("linkUrl", "/leaves/" + leave.getLeaveId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LEAVE_APPROVED_INTERMEDIATE,
                    leave.getNextApproval(),
                    NotificationPriority.MEDIUM,
                    "LEAVE",
                    leave.getLeaveId(),
                    variables));

            log.debug("Published LEAVE_APPROVED_INTERMEDIATE notification for leave ID: {}", leave.getLeaveId());
        } catch (Exception e) {
            log.error("Failed to publish intermediate approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when leave is fully approved.
     */
    private void publishLeaveFinalApprovedNotification(EmployeeLeave leave, Employee employee) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("leaveFromDate", leave.getLeaveFromDate().toString());
            variables.put("leaveToDate", leave.getLeaveToDate().toString());
            variables.put("leaveDays", leave.getLeaveDays().toString());
            variables.put("linkUrl", "/leaves/" + leave.getLeaveId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LEAVE_APPROVED_FINAL,
                    leave.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LEAVE",
                    leave.getLeaveId(),
                    variables));

            log.debug("Published LEAVE_APPROVED_FINAL notification for leave ID: {}", leave.getLeaveId());
        } catch (Exception e) {
            log.error("Failed to publish final approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when leave is rejected.
     */
    private void publishLeaveRejectedNotification(EmployeeLeave leave, Employee employee, String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("leaveFromDate", leave.getLeaveFromDate().toString());
            variables.put("leaveToDate", leave.getLeaveToDate().toString());
            variables.put("leaveDays", leave.getLeaveDays().toString());
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "Not specified");
            variables.put("linkUrl", "/leaves/" + leave.getLeaveId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LEAVE_REJECTED,
                    leave.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "LEAVE",
                    leave.getLeaveId(),
                    variables));

            log.debug("Published LEAVE_REJECTED notification for leave ID: {}", leave.getLeaveId());
        } catch (Exception e) {
            log.error("Failed to publish leave rejected notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish notification when leave is cancelled.
     */
    private void publishLeaveCancelledNotification(EmployeeLeave leave, Employee employee, Employee approver) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("leaveFromDate", leave.getLeaveFromDate().toString());
            variables.put("leaveToDate", leave.getLeaveToDate().toString());
            variables.put("leaveDays", leave.getLeaveDays().toString());
            variables.put("linkUrl", "/leaves/" + leave.getLeaveId());

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEventType.LEAVE_CANCELLED,
                    approver.getEmployeeNo(),
                    NotificationPriority.MEDIUM,
                    "LEAVE",
                    leave.getLeaveId(),
                    variables));

            log.debug("Published LEAVE_CANCELLED notification for leave ID: {}", leave.getLeaveId());
        } catch (Exception e) {
            log.error("Failed to publish leave cancelled notification: {}", e.getMessage(), e);
        }
    }
}
