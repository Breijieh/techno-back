package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.dto.paymentrequest.PaymentProcessRequest;
import com.techno.backend.dto.paymentrequest.PaymentRequestDto;
import com.techno.backend.dto.paymentrequest.PaymentRequestResponse;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.ProjectPaymentProcess;
import com.techno.backend.entity.ProjectPaymentRequest;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.ProjectPaymentProcessRepository;
import com.techno.backend.repository.ProjectPaymentRequestRepository;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for Project Payment Request management.
 * Handles supplier payment requests with multi-level approval workflow.
 *
 * Approval Flow:
 * Level 1: Project Manager
 * Level 2: Regional Manager
 * Level 3: Finance Manager
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentRequestService {

        private final ProjectPaymentRequestRepository requestRepository;
        private final ProjectPaymentProcessRepository processRepository;
        private final ProjectRepository projectRepository;
        private final SupplierRepository supplierRepository;
        private final EmployeeRepository employeeRepository;
        private final ApprovalWorkflowService approvalWorkflowService;
        private final ApplicationEventPublisher eventPublisher;

        /**
         * Submit a new payment request.
         *
         * @param request     Payment request DTO
         * @param requestedBy Employee submitting the request
         * @return Created payment request
         */
        @Transactional
        public PaymentRequestResponse submitPaymentRequest(PaymentRequestDto request, Long requestedBy) {
                log.info("Submitting payment request for project: {}, amount: {}",
                                request.getProjectCode(), request.getPaymentAmount());

                // Validate project exists
                Project project = projectRepository.findById(request.getProjectCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project not found with code: " + request.getProjectCode()));

                // Validate requester exists
                Employee requester = employeeRepository.findById(requestedBy)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found with ID: " + requestedBy));

                // Validate payment amount doesn't exceed project total
                if (project.getTotalProjectAmount() != null &&
                                request.getPaymentAmount().compareTo(project.getTotalProjectAmount()) > 0) {
                        throw new BadRequestException(
                                        String.format("Ù…Ø¨Ù„Øº Ø§Ù„Ø¯ÙØ¹Ø© (%.2f Ø±ÙŠØ§Ù„) Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ù‚ÙŠÙ…Ø© Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ (%.2f Ø±ÙŠØ§Ù„)",
                                                        request.getPaymentAmount(), project.getTotalProjectAmount()));
                }

                // Validate cumulative total of all payment requests (approved or pending)
                // doesn't exceed project total
                List<ProjectPaymentRequest> existingRequests = requestRepository
                                .findByProjectCode(request.getProjectCode());
                BigDecimal existingTotal = existingRequests.stream()
                                .filter(req -> !"R".equals(req.getTransStatus()) && !"Y".equals(req.getIsDeleted())) // Exclude
                                                                                                                     // rejected
                                                                                                                     // and
                                                                                                                     // deleted
                                .map(ProjectPaymentRequest::getPaymentAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal newTotal = existingTotal.add(request.getPaymentAmount());

                if (project.getTotalProjectAmount() != null &&
                                newTotal.compareTo(project.getTotalProjectAmount()) > 0) {
                        BigDecimal remaining = project.getTotalProjectAmount().subtract(existingTotal);
                        throw new BadRequestException(
                                        String.format("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø¯ÙØ¹ (%.2f Ø±ÙŠØ§Ù„) Ø³ÙŠØªØ¬Ø§ÙˆØ² Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ (%.2f Ø±ÙŠØ§Ù„). " +
                                                        "Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ù‚ØµÙ‰ Ø§Ù„Ù…Ø³Ù…ÙˆØ­ Ø¨Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨: %.2f Ø±ÙŠØ§Ù„",
                                                        newTotal, project.getTotalProjectAmount(),
                                                        remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining
                                                                        : BigDecimal.ZERO));
                }

                // Initialize approval workflow
                ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                                "PROJ_PAYMENT",
                                requestedBy,
                                requester.getPrimaryDeptCode(),
                                request.getProjectCode());

                // Map approval workflow status "N" (Needs approval) to "P" (Pending)
                String transStatus = "N".equals(approvalInfo.getTransStatus()) ? "P" : approvalInfo.getTransStatus();

                // Create payment request
                ProjectPaymentRequest paymentRequest = ProjectPaymentRequest.builder()
                                .projectCode(request.getProjectCode())
                                .supplierCode(request.getSupplierCode())
                                .requestDate(request.getRequestDate())
                                .paymentAmount(request.getPaymentAmount())
                                .paymentPurpose(request.getPaymentPurpose())
                                .transStatus(transStatus)
                                .nextApproval(approvalInfo.getNextApproval())
                                .nextAppLevel(approvalInfo.getNextAppLevel())
                                .requestedBy(requestedBy)
                                .attachmentPath(request.getAttachmentPath())
                                .isProcessed("N")
                                .isDeleted("N")
                                .build();

                paymentRequest = requestRepository.save(paymentRequest);
                log.info("Payment request submitted: {}, Next approver: {}",
                                paymentRequest.getRequestNo(), paymentRequest.getNextApproval());

                // Publish PAYMENT_REQUEST_SUBMITTED notification
                Map<String, Object> variables = new HashMap<>();
                variables.put("projectName", project.getProjectName());
                variables.put("paymentAmount", request.getPaymentAmount().toString());
                variables.put("paymentPurpose", request.getPaymentPurpose());
                variables.put("requesterName", requester.getEmployeeName());
                variables.put("linkUrl", "/payment-requests/" + paymentRequest.getRequestNo());

                eventPublisher.publishEvent(new NotificationEvent(
                                this,
                                NotificationEventType.PAYMENT_REQUEST_SUBMITTED,
                                approvalInfo.getNextApproval(),
                                NotificationPriority.MEDIUM,
                                "PAYMENT_REQUEST",
                                paymentRequest.getRequestNo(),
                                variables));

                return mapToResponse(paymentRequest, project);
        }

        /**
         * Approve a payment request.
         *
         * @param requestNo  Request number
         * @param approverId Approver employee number
         * @return Updated payment request
         */
        @Transactional
        public PaymentRequestResponse approvePaymentRequest(Long requestNo, Long approverId) {
                log.info("Approving payment request: {} by approver: {}", requestNo, approverId);

                // Find request
                ProjectPaymentRequest request = requestRepository.findById(requestNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment request not found with ID: " + requestNo));

                // Validate request is pending
                if (!request.isPending()) {
                        throw new BadRequestException("Ø·Ù„Ø¨ Ø§Ù„Ø¯ÙØ¹ Ù„ÙŠØ³ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±");
                }

                // Validate approver is the next approver
                if (!approverId.equals(request.getNextApproval())) {
                        throw new BadRequestException("Ø£Ù†Øª Ù„Ø³Øª Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ø§Ù„Ù…Ø¹ÙŠÙ† Ù„Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨");
                }

                // Extract values to avoid final variable issues in lambdas
                Long requestedBy = request.getRequestedBy();
                Integer currentLevel = request.getNextAppLevel();

                // Get employee info for approval workflow
                Employee employee = employeeRepository.findById(requestedBy)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found: " + requestedBy));

                // Move to next approval level
                ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                                "PROJ_PAYMENT",
                                currentLevel,
                                requestedBy,
                                employee.getPrimaryDeptCode(),
                                request.getProjectCode());

                // Update request with approval info
                request.approve(approverId, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
                request.setTransStatus(nextLevel.getTransStatus());

                // Get project before save to avoid final variable issue
                Long projectCode = request.getProjectCode();

                request = requestRepository.save(request);
                log.info("Payment request approved. Status: {}, Next approver: {}",
                                request.getTransStatus(), request.getNextApproval());

                // Publish notification event (intermediate or final approval)
                Project project = projectRepository.findById(projectCode).orElse(null);

                Map<String, Object> variables = new HashMap<>();
                if (project != null)
                        variables.put("projectName", project.getProjectName());
                variables.put("paymentAmount", request.getPaymentAmount().toString());
                variables.put("paymentPurpose", request.getPaymentPurpose());
                variables.put("linkUrl", "/payment-requests/" + requestNo);

                if (nextLevel.getNextApproval() == null) {
                        // Final approval - notify requester
                        eventPublisher.publishEvent(new NotificationEvent(
                                        this,
                                        NotificationEventType.PAYMENT_REQUEST_APPROVED_FINAL,
                                        requestedBy,
                                        NotificationPriority.HIGH,
                                        "PAYMENT_REQUEST",
                                        requestNo,
                                        variables));
                } else {
                        // Intermediate approval - notify next approver
                        eventPublisher.publishEvent(new NotificationEvent(
                                        this,
                                        NotificationEventType.PAYMENT_REQUEST_APPROVED_INTERMEDIATE,
                                        nextLevel.getNextApproval(),
                                        NotificationPriority.MEDIUM,
                                        "PAYMENT_REQUEST",
                                        requestNo,
                                        variables));
                }

                return mapToResponse(request, project);
        }

        /**
         * Reject a payment request.
         *
         * @param requestNo  Request number
         * @param approverId Approver employee number
         * @param reason     Rejection reason
         * @return Updated payment request
         */
        @Transactional
        public PaymentRequestResponse rejectPaymentRequest(Long requestNo, Long approverId, String reason) {
                log.info("Rejecting payment request: {} by approver: {}", requestNo, approverId);

                // Find request
                ProjectPaymentRequest request = requestRepository.findById(requestNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment request not found with ID: " + requestNo));

                // Validate request is pending
                if (!request.isPending()) {
                        throw new BadRequestException("Ø·Ù„Ø¨ Ø§Ù„Ø¯ÙØ¹ Ù„ÙŠØ³ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±");
                }

                // Validate approver is the next approver
                if (!approverId.equals(request.getNextApproval())) {
                        throw new BadRequestException("Ø£Ù†Øª Ù„Ø³Øª Ø§Ù„Ù…ÙˆØ§ÙÙ‚ Ø§Ù„Ù…Ø¹ÙŠÙ† Ù„Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨");
                }

                // Reject request
                request.reject(approverId, reason);

                request = requestRepository.save(request);
                log.info("Payment request rejected: {}", requestNo);

                // Publish rejection notification
                Project project = projectRepository.findById(request.getProjectCode()).orElse(null);

                Map<String, Object> variables = new HashMap<>();
                if (project != null)
                        variables.put("projectName", project.getProjectName());
                variables.put("paymentAmount", request.getPaymentAmount().toString());
                variables.put("paymentPurpose", request.getPaymentPurpose());
                variables.put("rejectionReason", reason != null ? reason : "");
                variables.put("linkUrl", "/payment-requests/" + requestNo);

                eventPublisher.publishEvent(new NotificationEvent(
                                this,
                                NotificationEventType.PAYMENT_REQUEST_REJECTED,
                                request.getRequestedBy(),
                                NotificationPriority.HIGH,
                                "PAYMENT_REQUEST",
                                requestNo,
                                variables));

                return mapToResponse(request, project);
        }

        /**
         * Process an approved payment request (finance processes the actual payment).
         *
         * @param requestNo      Request number
         * @param processRequest Payment processing details
         * @param processedBy    Finance employee number
         * @return Success message
         */
        @Transactional
        public void processPayment(Long requestNo, PaymentProcessRequest processRequest, Long processedBy) {
                log.info("Processing payment for request: {}", requestNo);

                // Find request
                ProjectPaymentRequest request = requestRepository.findById(requestNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment request not found with ID: " + requestNo));

                // Validate request is approved
                if (!request.isApproved()) {
                        throw new BadRequestException("ÙŠØ¬Ø¨ Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© Ø¹Ù„Ù‰ Ø·Ù„Ø¨ Ø§Ù„Ø¯ÙØ¹ Ù‚Ø¨Ù„ Ø§Ù„Ù…Ø¹Ø§Ù„Ø¬Ø©");
                }

                // Validate not already processed
                if (request.isProcessed()) {
                        throw new BadRequestException("ØªÙ…Øª Ù…Ø¹Ø§Ù„Ø¬Ø© Ø·Ù„Ø¨ Ø§Ù„Ø¯ÙØ¹ Ø¨Ø§Ù„ÙØ¹Ù„");
                }

                // Check if already has a payment process record
                if (processRepository.existsByRequestNo(requestNo)) {
                        throw new BadRequestException("Ø³Ø¬Ù„ Ù…Ø¹Ø§Ù„Ø¬Ø© Ø§Ù„Ø¯ÙØ¹ Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨");
                }

                // Create payment process record
                ProjectPaymentProcess process = ProjectPaymentProcess.builder()
                                .requestNo(requestNo)
                                .paymentDate(processRequest.getPaymentDate())
                                .paidAmount(processRequest.getPaidAmount())
                                .paymentMethod(processRequest.getPaymentMethod())
                                .referenceNo(processRequest.getReferenceNo())
                                .bankName(processRequest.getBankName())
                                .processNotes(processRequest.getProcessNotes())
                                .processedBy(processedBy)
                                .isDeleted("N")
                                .build();

                processRepository.save(process);

                // Mark request as processed
                request.markAsProcessed();
                requestRepository.save(request);

                log.info("Payment processed successfully for request: {}", requestNo);

                // Note: No notification event for payment processing as it's a Finance internal
                // action
                // Requester was already notified when final approval was granted
        }

        /**
         * Get payment request by ID.
         *
         * @param requestNo Request number
         * @return Payment request details
         */
        @Transactional(readOnly = true)
        public PaymentRequestResponse getRequestById(Long requestNo) {
                log.debug("Fetching payment request: {}", requestNo);

                ProjectPaymentRequest request = requestRepository.findById(requestNo)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment request not found with ID: " + requestNo));

                Project project = projectRepository.findById(request.getProjectCode()).orElse(null);
                return mapToResponse(request, project);
        }

        /**
         * Get pending requests for a specific approver.
         *
         * @param approverId Approver employee number
         * @return List of pending requests
         */
        @Transactional(readOnly = true)
        public List<PaymentRequestResponse> getPendingRequestsByApprover(Long approverId) {
                log.debug("Fetching pending requests for approver: {}", approverId);

                List<ProjectPaymentRequest> requests = requestRepository.findPendingRequestsByApprover(approverId);
                return requests.stream()
                                .map(request -> {
                                        Project project = projectRepository.findById(request.getProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(request, project);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get all requests for a specific project.
         *
         * @param projectCode Project code
         * @return List of payment requests
         */
        @Transactional(readOnly = true)
        public List<PaymentRequestResponse> getRequestsByProject(Long projectCode) {
                log.debug("Fetching payment requests for project: {}", projectCode);

                // Validate project exists
                Project project = projectRepository.findById(projectCode)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project not found with code: " + projectCode));

                List<ProjectPaymentRequest> requests = requestRepository.findByProjectCode(projectCode);
                return requests.stream()
                                .map(request -> mapToResponse(request, project))
                                .collect(Collectors.toList());
        }

        /**
         * Get approved but not processed requests.
         *
         * @return List of approved pending processing requests
         */
        @Transactional(readOnly = true)
        public List<PaymentRequestResponse> getApprovedNotProcessed() {
                log.debug("Fetching approved but not processed requests");

                List<ProjectPaymentRequest> requests = requestRepository.findApprovedNotProcessed();
                return requests.stream()
                                .map(request -> {
                                        Project project = projectRepository.findById(request.getProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(request, project);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get all pending payment requests (Admin view).
         *
         * @return List of all pending payment requests
         */
        @Transactional(readOnly = true)
        public List<PaymentRequestResponse> getAllPendingRequests() {
                log.debug("Fetching all pending payment requests (Admin view)");

                List<ProjectPaymentRequest> requests = requestRepository.findByStatus("P");
                return requests.stream()
                                .map(request -> {
                                        Project project = projectRepository.findById(request.getProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(request, project);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Get all payment requests (excluding deleted).
         * Used by General Manager and Admin to view all requests.
         *
         * @return List of all payment requests
         */
        @Transactional(readOnly = true)
        public List<PaymentRequestResponse> getAllPaymentRequests() {
                log.debug("Fetching all payment requests");

                List<ProjectPaymentRequest> requests = requestRepository.findAll()
                                .stream()
                                .filter(request -> !"Y".equals(request.getIsDeleted()))
                                .collect(Collectors.toList());

                return requests.stream()
                                .map(request -> {
                                        Project project = projectRepository.findById(request.getProjectCode())
                                                        .orElse(null);
                                        return mapToResponse(request, project);
                                })
                                .collect(Collectors.toList());
        }

        // ==================== Mapping Methods ====================

        /**
         * Map ProjectPaymentRequest entity to PaymentRequestResponse DTO.
         */
        private PaymentRequestResponse mapToResponse(ProjectPaymentRequest request, Project project) {
                PaymentRequestResponse response = PaymentRequestResponse.builder()
                                .requestNo(request.getRequestNo())
                                .projectCode(request.getProjectCode())
                                .supplierCode(request.getSupplierCode())
                                .requestDate(request.getRequestDate())
                                .paymentAmount(request.getPaymentAmount())
                                .paymentPurpose(request.getPaymentPurpose())
                                .transStatus(request.getTransStatus())
                                .nextApproval(request.getNextApproval())
                                .nextAppLevel(request.getNextAppLevel())
                                .approvedBy(request.getApprovedBy())
                                .approvedDate(request.getApprovedDate())
                                .rejectionReason(request.getRejectionReason())
                                .requestedBy(request.getRequestedBy())
                                .attachmentPath(request.getAttachmentPath())
                                .isProcessed(request.getIsProcessed())
                                .isDeleted(request.getIsDeleted())
                                .isPending(request.isPending())
                                .isApproved(request.isApproved())
                                .isRejected(request.isRejected())
                                .isProcessedFlag(request.isProcessed())
                                .createdDate(request.getCreatedDate())
                                .createdBy(request.getCreatedBy())
                                .modifiedDate(request.getModifiedDate())
                                .modifiedBy(request.getModifiedBy())
                                .build();

                // Add project details if available
                if (project != null) {
                        response.setProjectName(project.getProjectName());
                        response.setProjectName(project.getProjectName());
                }

                // Add supplier name if available
                if (request.getSupplierCode() != null) {
                        supplierRepository.findById(request.getSupplierCode())
                                        .ifPresent(supplier -> response.setSupplierName(supplier.getSupplierName()));
                }

                // Add approver name if available
                if (request.getNextApproval() != null) {
                        employeeRepository.findById(request.getNextApproval())
                                        .ifPresent(emp -> response.setNextApproverName(emp.getEmployeeName()));
                }

                // Add requester name if available
                if (request.getRequestedBy() != null) {
                        employeeRepository.findById(request.getRequestedBy())
                                        .ifPresent(emp -> response.setRequesterName(emp.getEmployeeName()));
                }

                return response;
        }
}

