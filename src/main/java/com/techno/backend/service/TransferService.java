package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.dto.transfer.TransferRequestDto;
import com.techno.backend.dto.transfer.TransferResponse;
import com.techno.backend.entity.*;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for Employee Transfer Management.
 * Handles business logic for transferring employees between projects.
 *
 * Approval Flow:
 * - Level 1: Current Project Manager (fromProject)
 * - Level 2: Target Project Manager (toProject) - Final
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransferService {

        private final ProjectTransferRequestRepository transferRepository;
        private final EmployeeRepository employeeRepository;
        private final ProjectRepository projectRepository;
        private final ApprovalWorkflowService approvalWorkflowService;
        private final ApplicationEventPublisher eventPublisher;

        /**
         * Submit a new employee transfer request.
         *
         * @param request     Transfer request DTO
         * @param requestedBy Requesting employee number
         * @return Created transfer request
         */
        @Transactional
        public TransferResponse submitTransferRequest(TransferRequestDto request, Long requestedBy) {
                log.info("Submitting transfer request - Employee: {}, From Project: {}, To Project: {}",
                                request.getEmployeeNo(), request.getFromProjectCode(), request.getToProjectCode());

                // Validate employee exists
                Employee employee = employeeRepository.findById(request.getEmployeeNo())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found: " + request.getEmployeeNo()));

                // Validate employee is currently in the fromProject
                if (!request.getFromProjectCode().equals(employee.getPrimaryProjectCode())) {
                        throw new BadRequestException(
                                        "Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…Ø¹ÙŠÙ† Ø­Ø§Ù„ÙŠØ§Ù‹ ÙÙŠ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ "
                                                        + request.getFromProjectCode());
                }

                // Validate fromProject and toProject are different
                if (request.getFromProjectCode().equals(request.getToProjectCode())) {
                        throw new BadRequestException(
                                        "Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø§Ù„Ù…ØµØ¯Ø± ÙˆØ§Ù„Ù‡Ø¯Ù Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ†Ø§ Ù†ÙØ³ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹");
                }

                // Validate both projects exist
                Project fromProject = projectRepository.findById(request.getFromProjectCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Source project not found: " + request.getFromProjectCode()));

                Project toProject = projectRepository.findById(request.getToProjectCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target project not found: " + request.getToProjectCode()));

                // Check for pending transfer for this employee
                if (transferRepository.hasPendingTransfer(request.getEmployeeNo())) {
                        throw new BadRequestException(
                                        "Ø§Ù„Ù…ÙˆØ¸Ù Ù„Ø¯ÙŠÙ‡ Ø·Ù„Ø¨ Ù†Ù‚Ù„ Ù…Ø¹Ù„Ù‚ Ø¨Ø§Ù„ÙØ¹Ù„");
                }

                // Initialize approval workflow (PROJ_TRANSFER)
                Long deptCode = employee.getPrimaryDeptCode();
                Long projectCode = request.getFromProjectCode();

                ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                                "PROJ_TRANSFER", requestedBy, deptCode, projectCode);

                // Create transfer request
                ProjectTransferRequest transfer = ProjectTransferRequest.builder()
                                .employeeNo(request.getEmployeeNo())
                                .fromProjectCode(request.getFromProjectCode())
                                .toProjectCode(request.getToProjectCode())
                                .transferDate(request.getTransferDate())
                                .transferReason(request.getTransferReason())
                                .transStatus("P")
                                .nextApproval(approvalInfo.getNextApproval())
                                .nextAppLevel(approvalInfo.getNextAppLevel())
                                .requestedBy(requestedBy)
                                .isExecuted("N")
                                .build();

                transfer = transferRepository.save(transfer);
                log.info("Transfer request created: {}, Next approver: {} at level {}",
                                transfer.getTransferNo(), approvalInfo.getNextApproval(),
                                approvalInfo.getNextAppLevel());

                // Publish notification event
                Map<String, Object> variables = new HashMap<>();
                variables.put("employeeName", employee.getEmployeeName());
                variables.put("fromProjectName", fromProject.getProjectName());
                variables.put("toProjectName", toProject.getProjectName());
                variables.put("transferDate", transfer.getTransferDate().toString());
                variables.put("linkUrl", "/transfers/" + transfer.getTransferNo());

                eventPublisher.publishEvent(new NotificationEvent(
                                this,
                                NotificationEventType.TRANSFER_SUBMITTED,
                                approvalInfo.getNextApproval(),
                                NotificationPriority.MEDIUM,
                                "TRANSFER",
                                transfer.getTransferNo(),
                                variables));

                return mapToResponse(transfer, employee, fromProject, toProject);
        }

        /**
         * Approve transfer request at current approval level.
         *
         * @param transferNo Transfer request number
         * @param approverNo Approver employee number
         * @return Updated transfer request
         */
        @Transactional
        public TransferResponse approveTransferRequest(Long transferNo, Long approverNo) {
                log.info("Approving transfer request {} by approver {}", transferNo, approverNo);

                // Find transfer request
                ProjectTransferRequest transfer = transferRepository.findById(transferNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transfer request not found: " + transferNo));

                // Validate request is pending
                if (!"P".equals(transfer.getTransStatus())) {
                        throw new BadRequestException(
                                        "Ø·Ù„Ø¨ Ø§Ù„Ù†Ù‚Ù„ Ù„ÙŠØ³ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±");
                }

                // Validate approver is the next approver
                if (!approverNo.equals(transfer.getNextApproval())) {
                        throw new BadRequestException(
                                        "Ø£Ù†Øª ØºÙŠØ± Ù…Ø®ÙˆÙ„ Ø¨Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨ ÙÙŠ Ù‡Ø°Ø§ Ø§Ù„Ù…Ø³ØªÙˆÙ‰");
                }

                // Get employee for department code
                Long requestedBy = transfer.getRequestedBy();
                Integer currentLevel = transfer.getNextAppLevel();
                Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found: " + transfer.getEmployeeNo()));

                Long deptCode = employee.getPrimaryDeptCode();
                Long projectCode = transfer.getFromProjectCode();

                // Extract values before lambda and reassignment
                Long fromProjectCode2 = transfer.getFromProjectCode();
                Long toProjectCode2 = transfer.getToProjectCode();
                LocalDate transferDate2 = transfer.getTransferDate();
                Long transferNo2 = transfer.getTransferNo();

                // Determine context project code based on current level
                // Level 1 (Source PM) -> Next is Level 2 (Target PM), so use Target Project
                // context
                Long contextProjectCode = projectCode;
                if (currentLevel != null && currentLevel == 1) {
                        contextProjectCode = toProjectCode2;
                        log.debug("Switching to Target Project context ({}) for Level 2 approval", toProjectCode2);
                }

                // Move to next approval level
                ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                                "PROJ_TRANSFER",
                                currentLevel,
                                requestedBy,
                                deptCode,
                                contextProjectCode);

                Project fromProject = projectRepository.findById(fromProjectCode2).orElse(null);
                Project toProject = projectRepository.findById(toProjectCode2).orElse(null);

                Map<String, Object> variables = new HashMap<>();
                variables.put("employeeName", employee.getEmployeeName());
                variables.put("fromProjectName", fromProject != null ? fromProject.getProjectName() : "");
                variables.put("toProjectName", toProject != null ? toProject.getProjectName() : "");
                variables.put("transferDate", transferDate2.toString());
                variables.put("linkUrl", "/transfers/" + transferNo2);

                if (nextLevel.getNextApproval() == null) {
                        // Final approval
                        transfer.approve(approverNo, null, null);
                        log.info("Transfer request {} fully approved", transferNo);

                        // Publish final approval notification
                        eventPublisher.publishEvent(new NotificationEvent(
                                        this,
                                        NotificationEventType.TRANSFER_APPROVED_FINAL,
                                        requestedBy,
                                        NotificationPriority.HIGH,
                                        "TRANSFER",
                                        transferNo,
                                        variables));
                } else {
                        // Intermediate approval - move to next level
                        transfer.approve(approverNo, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
                        log.info("Transfer request {} approved at level {}, moving to level {} with approver {}",
                                        transferNo, currentLevel, nextLevel.getNextAppLevel(),
                                        nextLevel.getNextApproval());

                        // Publish intermediate approval notification
                        eventPublisher.publishEvent(new NotificationEvent(
                                        this,
                                        NotificationEventType.TRANSFER_APPROVED_INTERMEDIATE,
                                        nextLevel.getNextApproval(),
                                        NotificationPriority.MEDIUM,
                                        "TRANSFER",
                                        transferNo,
                                        variables));
                }

                transferRepository.save(transfer);

                return mapToResponse(transfer, employee, fromProject, toProject);
        }

        /**
         * Reject transfer request.
         *
         * @param transferNo Transfer request number
         * @param approverNo Approver employee number
         * @param reason     Rejection reason
         * @return Updated transfer request
         */
        @Transactional
        public TransferResponse rejectTransferRequest(Long transferNo, Long approverNo, String reason) {
                log.info("Rejecting transfer request {} by approver {}", transferNo, approverNo);

                // Find transfer request
                ProjectTransferRequest transfer = transferRepository.findById(transferNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transfer request not found: " + transferNo));

                // Validate request is pending
                if (!"P".equals(transfer.getTransStatus())) {
                        throw new BadRequestException(
                                        "Ø·Ù„Ø¨ Ø§Ù„Ù†Ù‚Ù„ Ù„ÙŠØ³ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±");
                }

                // Validate approver is the next approver
                if (!approverNo.equals(transfer.getNextApproval())) {
                        throw new BadRequestException(
                                        "Ø£Ù†Øª ØºÙŠØ± Ù…Ø®ÙˆÙ„ Ø¨Ø±ÙØ¶ Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨ ÙÙŠ Ù‡Ø°Ø§ Ø§Ù„Ù…Ø³ØªÙˆÙ‰");
                }

                // Extract values before lambda
                Long employeeNo = transfer.getEmployeeNo();
                Long fromProjectCode = transfer.getFromProjectCode();
                Long toProjectCode = transfer.getToProjectCode();
                LocalDate transferDate = transfer.getTransferDate();
                Long requestedBy = transfer.getRequestedBy();

                // Reject the transfer
                transfer.reject(approverNo, reason);
                transferRepository.save(transfer);

                log.info("Transfer request {} rejected", transferNo);

                // Fetch related entities for notification
                Employee employee = employeeRepository.findById(employeeNo).orElse(null);
                Project fromProject = projectRepository.findById(fromProjectCode).orElse(null);
                Project toProject = projectRepository.findById(toProjectCode).orElse(null);

                Map<String, Object> variables = new HashMap<>();
                if (employee != null)
                        variables.put("employeeName", employee.getEmployeeName());
                if (fromProject != null)
                        variables.put("fromProjectName", fromProject.getProjectName());
                if (toProject != null)
                        variables.put("toProjectName", toProject.getProjectName());
                variables.put("transferDate", transferDate.toString());
                variables.put("rejectionReason", reason != null ? reason : "");
                variables.put("linkUrl", "/transfers/" + transferNo);

                // Publish rejection notification
                eventPublisher.publishEvent(new NotificationEvent(
                                this,
                                NotificationEventType.TRANSFER_REJECTED,
                                requestedBy,
                                NotificationPriority.HIGH,
                                "TRANSFER",
                                transferNo,
                                variables));

                return mapToResponse(transfer, employee, fromProject, toProject);
        }

        /**
         * Execute an approved transfer (actually move the employee).
         *
         * @param transferNo Transfer request number
         * @param executedBy Executing employee number (usually HR)
         * @return Updated transfer request
         */
        @Transactional
        public TransferResponse executeTransfer(Long transferNo, Long executedBy) {
                log.info("Executing transfer request {} by {}", transferNo, executedBy);

                // Find transfer request
                ProjectTransferRequest transfer = transferRepository.findById(transferNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transfer request not found: " + transferNo));

                // Validate request is approved
                if (!"A".equals(transfer.getTransStatus())) {
                        throw new BadRequestException(
                                        "ÙŠØ¬Ø¨ Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ø·Ù„Ø¨ Ø§Ù„Ù†Ù‚Ù„ Ù‚Ø¨Ù„ Ø§Ù„ØªÙ†ÙÙŠØ°");
                }

                // Validate not already executed
                if ("Y".equals(transfer.getIsExecuted())) {
                        throw new BadRequestException(
                                        "ØªÙ… ØªÙ†ÙÙŠØ° Ø·Ù„Ø¨ Ø§Ù„Ù†Ù‚Ù„ Ø¨Ø§Ù„ÙØ¹Ù„");
                }

                // Extract values before lambda
                Long employeeNo = transfer.getEmployeeNo();
                Long fromProjectCode = transfer.getFromProjectCode();
                Long toProjectCode = transfer.getToProjectCode();

                // Get employee
                Employee employee = employeeRepository.findById(employeeNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found: " + employeeNo));

                // Update employee's project
                employee.setPrimaryProjectCode(toProjectCode);
                employeeRepository.save(employee);

                // Mark transfer as executed
                transfer.markAsExecuted();
                transferRepository.save(transfer);

                log.info("Transfer executed successfully - Employee {} moved to project {}",
                                employee.getEmployeeNo(), toProjectCode);

                // Fetch related entities for response
                Project fromProject = projectRepository.findById(fromProjectCode).orElse(null);
                Project toProject = projectRepository.findById(toProjectCode).orElse(null);

                return mapToResponse(transfer, employee, fromProject, toProject);
        }

        /**
         * Get transfer request by ID.
         *
         * @param transferNo Transfer request number
         * @return Transfer request details
         */
        @Transactional(readOnly = true)
        public TransferResponse getRequestById(Long transferNo) {
                log.debug("Fetching transfer request: {}", transferNo);

                ProjectTransferRequest transfer = transferRepository.findById(transferNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transfer request not found: " + transferNo));

                // Fetch related entities
                Employee employee = employeeRepository.findById(transfer.getEmployeeNo()).orElse(null);
                Project fromProject = projectRepository.findById(transfer.getFromProjectCode()).orElse(null);
                Project toProject = projectRepository.findById(transfer.getToProjectCode()).orElse(null);

                return mapToResponse(transfer, employee, fromProject, toProject);
        }

        /**
         * Get pending transfer requests for approver.
         *
         * @param approverNo Approver employee number
         * @return List of pending transfer requests
         */
        @Transactional(readOnly = true)
        public List<TransferResponse> getPendingRequestsByApprover(Long approverNo) {
                log.debug("Fetching pending transfers for approver: {}", approverNo);

                List<ProjectTransferRequest> requests = transferRepository.findPendingTransfersByApprover(approverNo);

                return requests.stream()
                                .map(transfer -> {
                                        Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                                        .orElse(null);
                                        Project fromProject = projectRepository.findById(transfer.getFromProjectCode())
                                                        .orElse(null);
                                        Project toProject = projectRepository.findById(transfer.getToProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(transfer, employee, fromProject, toProject);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get all pending transfer requests (for Admins).
         *
         * @return List of all pending transfer requests
         */
        @Transactional(readOnly = true)
        public List<TransferResponse> getAllPendingRequests() {
                log.debug("Fetching all pending transfers (Admin view)");

                List<ProjectTransferRequest> requests = transferRepository.findByStatus("P");

                return requests.stream()
                                .map(transfer -> {
                                        Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                                        .orElse(null);
                                        Project fromProject = projectRepository.findById(transfer.getFromProjectCode())
                                                        .orElse(null);
                                        Project toProject = projectRepository.findById(transfer.getToProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(transfer, employee, fromProject, toProject);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get all transfer requests for a project (either source or target).
         *
         * @param projectCode Project code
         * @return List of transfer requests
         */
        @Transactional(readOnly = true)
        public List<TransferResponse> getRequestsByProject(Long projectCode) {
                log.debug("Fetching transfers for project: {}", projectCode);

                List<ProjectTransferRequest> fromTransfers = transferRepository.findByFromProjectCode(projectCode);
                List<ProjectTransferRequest> toTransfers = transferRepository.findByToProjectCode(projectCode);

                // Combine and deduplicate
                fromTransfers.addAll(toTransfers);

                return fromTransfers.stream()
                                .distinct()
                                .map(transfer -> {
                                        Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                                        .orElse(null);
                                        Project fromProject = projectRepository.findById(transfer.getFromProjectCode())
                                                        .orElse(null);
                                        Project toProject = projectRepository.findById(transfer.getToProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(transfer, employee, fromProject, toProject);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get approved transfers that have not been executed yet.
         *
         * @return List of approved pending execution transfers
         */
        @Transactional(readOnly = true)
        public List<TransferResponse> getApprovedNotExecuted() {
                log.debug("Fetching approved but not executed transfers");

                List<ProjectTransferRequest> requests = transferRepository.findApprovedNotExecuted();

                return requests.stream()
                                .map(transfer -> {
                                        Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                                        .orElse(null);
                                        Project fromProject = projectRepository.findById(transfer.getFromProjectCode())
                                                        .orElse(null);
                                        Project toProject = projectRepository.findById(transfer.getToProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(transfer, employee, fromProject, toProject);
                                })
                                .collect(Collectors.toList());
        }

        // ==================== Mapping Methods ====================

        /**
         * Map ProjectTransferRequest entity to TransferResponse DTO.
         */
        private TransferResponse mapToResponse(ProjectTransferRequest transfer, Employee employee,
                        Project fromProject, Project toProject) {
                TransferResponse response = TransferResponse.builder()
                                .transferNo(transfer.getTransferNo())
                                .employeeNo(transfer.getEmployeeNo())
                                .fromProjectCode(transfer.getFromProjectCode())
                                .toProjectCode(transfer.getToProjectCode())
                                .transferDate(transfer.getTransferDate())
                                .transferReason(transfer.getTransferReason())
                                .transStatus(transfer.getTransStatus())
                                .nextApproval(transfer.getNextApproval())
                                .nextAppLevel(transfer.getNextAppLevel())
                                .requestedBy(transfer.getRequestedBy())
                                .approvedDate(transfer.getApprovedDate() != null
                                                ? transfer.getApprovedDate().toLocalDate()
                                                : null)
                                .approvedBy(transfer.getApprovedBy())
                                .rejectionReason(transfer.getRejectionReason())
                                .executionStatus(transfer.getIsExecuted())
                                .createdDate(transfer.getCreatedDate())
                                .createdBy(transfer.getCreatedBy())
                                .modifiedDate(transfer.getModifiedDate())
                                .modifiedBy(transfer.getModifiedBy())
                                .build();

                // Add employee details if available
                if (employee != null) {
                        response.setEmployeeName(employee.getEmployeeName());
                }

                // Add from project details if available
                if (fromProject != null) {
                        response.setFromProjectName(fromProject.getProjectName());
                }

                // Add to project details if available
                if (toProject != null) {
                        response.setToProjectName(toProject.getProjectName());
                }

                // Add names for approvers/requesters
                if (transfer.getRequestedBy() != null) {
                        employeeRepository.findById(transfer.getRequestedBy())
                                        .ifPresent(e -> response.setRequestedByName(e.getEmployeeName()));
                }
                if (transfer.getNextApproval() != null) {
                        employeeRepository.findById(transfer.getNextApproval())
                                        .ifPresent(e -> response.setNextApprovalName(e.getEmployeeName()));
                }
                if (transfer.getApprovedBy() != null) {
                        employeeRepository.findById(transfer.getApprovedBy())
                                        .ifPresent(e -> response.setApprovedByName(e.getEmployeeName()));
                }

                return response;
        }

        /**
         * Get all transfer requests (non-deleted).
         *
         * @return List of all transfer requests
         */
        @Transactional(readOnly = true)
        public List<TransferResponse> getAllTransferRequests() {
                log.debug("Fetching all transfer requests");

                List<ProjectTransferRequest> requests = transferRepository.findAll().stream()
                                .filter(tr -> tr.getIsDeleted() == null || !"Y".equals(tr.getIsDeleted()))
                                .collect(Collectors.toList());

                return requests.stream()
                                .map(transfer -> {
                                        Employee employee = employeeRepository.findById(transfer.getEmployeeNo())
                                                        .orElse(null);
                                        Project fromProject = projectRepository.findById(transfer.getFromProjectCode())
                                                        .orElse(null);
                                        Project toProject = projectRepository.findById(transfer.getToProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(transfer, employee, fromProject, toProject);
                                })
                                .collect(Collectors.toList());
        }
}
