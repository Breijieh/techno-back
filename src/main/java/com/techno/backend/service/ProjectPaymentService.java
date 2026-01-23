package com.techno.backend.service;

import com.techno.backend.dto.payment.PaymentRecordRequest;
import com.techno.backend.dto.payment.PaymentScheduleRequest;
import com.techno.backend.dto.payment.PaymentScheduleResponse;
import com.techno.backend.dto.payment.PaymentUpdateRequest;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.ProjectDuePayment;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.ProjectDuePaymentRepository;
import com.techno.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Project Payment Schedule management.
 * Handles business logic for payment milestones, tracking, and recording payments.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectPaymentService {

    private final ProjectDuePaymentRepository paymentRepository;
    private final ProjectRepository projectRepository;

    /**
     * Add a payment schedule/milestone to a project.
     *
     * @param request Payment schedule request
     * @return Created payment schedule
     */
    @Transactional
    public PaymentScheduleResponse addPaymentSchedule(PaymentScheduleRequest request) {
        log.info("Adding payment schedule for project: {}, sequence: {}",
                request.getProjectCode(), request.getSequenceNo());

        // Validate project exists
        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with code: " + request.getProjectCode()));

        // Check if sequence number already exists for this project
        if (paymentRepository.existsByProjectCodeAndSequenceNo(
                request.getProjectCode(), request.getSequenceNo())) {
            throw new BadRequestException(
                    "Ø¬Ø¯ÙˆÙ„ Ø§Ù„Ø¯ÙØ¹ Ø¨Ø§Ù„ØªØ³Ù„Ø³Ù„ " + request.getSequenceNo() +
                    " Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù„Ù…Ø´Ø±ÙˆØ¹ " + request.getProjectCode());
        }

        // Validate payment amount doesn't exceed project total (for incoming payments)
        // Note: Outgoing payments might exceed project total (supplier payments, etc.)
        // This validation is primarily for incoming payments from clients
        if (request.getDueAmount().compareTo(project.getTotalProjectAmount()) > 0) {
            throw new BadRequestException(
                    String.format("Ù…Ø¨Ù„Øº Ø§Ù„Ø¯ÙØ¹ (%.2f Ø±ÙŠØ§Ù„) Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠØªØ¬Ø§ÙˆØ² Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ù‚ÙŠÙ…Ø© Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ (%.2f Ø±ÙŠØ§Ù„)",
                            request.getDueAmount(), project.getTotalProjectAmount()));
        }

        // Validate sum of all payments (existing + new) doesn't exceed project total
        List<ProjectDuePayment> existingPayments = paymentRepository
                .findByProjectCodeOrderBySequenceNoAsc(request.getProjectCode());
        
        BigDecimal existingTotal = existingPayments.stream()
                .map(ProjectDuePayment::getDueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal newTotal = existingTotal.add(request.getDueAmount());
        
        if (newTotal.compareTo(project.getTotalProjectAmount()) > 0) {
            BigDecimal remaining = project.getTotalProjectAmount().subtract(existingTotal);
            throw new BadRequestException(
                    String.format("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø¯ÙÙˆØ¹Ø§Øª (%.2f Ø±ÙŠØ§Ù„) Ø³ÙŠØªØ¬Ø§ÙˆØ² Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ (%.2f Ø±ÙŠØ§Ù„). " +
                            "Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ù‚ØµÙ‰ Ø§Ù„Ù…Ø³Ù…ÙˆØ­ Ù„Ù‡Ø°Ù‡ Ø§Ù„Ø¯ÙØ¹Ø©: %.2f Ø±ÙŠØ§Ù„",
                            newTotal, project.getTotalProjectAmount(),
                            remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO));
        }

        // Create payment schedule
        ProjectDuePayment payment = ProjectDuePayment.builder()
                .projectCode(request.getProjectCode())
                .sequenceNo(request.getSequenceNo())
                .dueDate(request.getDueDate())
                .dueAmount(request.getDueAmount())
                .paidAmount(BigDecimal.ZERO)
                .paymentStatus("PENDING")
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment schedule created with ID: {}", payment.getPaymentId());

        return mapToResponse(payment, project);
    }

    /**
     * Update payment schedule details.
     *
     * @param paymentId Payment ID
     * @param request Update request
     * @return Updated payment schedule
     */
    @Transactional
    public PaymentScheduleResponse updatePaymentSchedule(Long paymentId, PaymentUpdateRequest request) {
        log.info("Updating payment schedule: {}", paymentId);

        // Validate update request has changes
        if (!request.hasUpdates()) {
            throw new BadRequestException("Ù„Ù… ÙŠØªÙ… ØªÙˆÙÙŠØ± Ø­Ù‚ÙˆÙ„ Ù„Ù„ØªØ­Ø¯ÙŠØ«");
        }

        // Find existing payment
        ProjectDuePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment schedule not found with ID: " + paymentId));

        // Update fields if provided
        if (request.getDueDate() != null) {
            payment.setDueDate(request.getDueDate());
        }
        if (request.getDueAmount() != null) {
            // Validate new due amount is >= paid amount
            if (payment.getPaidAmount() != null &&
                request.getDueAmount().compareTo(payment.getPaidAmount()) < 0) {
                throw new BadRequestException(
                        "Ø§Ù„Ù…Ø¨Ù„Øº Ø§Ù„Ù…Ø³ØªØ­Ù‚ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø£Ù‚Ù„ Ù…Ù† Ø§Ù„Ù…Ø¨Ù„Øº Ø§Ù„Ù…Ø¯ÙÙˆØ¹ Ø¨Ø§Ù„ÙØ¹Ù„: " + payment.getPaidAmount());
            }
            payment.setDueAmount(request.getDueAmount());
            // Update status based on new amount
            payment.updateStatus();
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }

        payment = paymentRepository.save(payment);
        log.info("Payment schedule {} updated successfully", paymentId);

        Project project = projectRepository.findById(payment.getProjectCode()).orElse(null);
        return mapToResponse(payment, project);
    }

    /**
     * Record an actual payment against a payment schedule.
     *
     * @param paymentId Payment schedule ID
     * @param request Payment record request
     * @return Updated payment schedule
     */
    @Transactional
    public PaymentScheduleResponse recordPayment(Long paymentId, PaymentRecordRequest request) {
        log.info("Recording payment of {} for payment schedule: {}",
                request.getPaymentAmount(), paymentId);

        // Find existing payment
        ProjectDuePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment schedule not found with ID: " + paymentId));

        // Validate payment amount doesn't exceed remaining amount
        BigDecimal remainingAmount = payment.getRemainingAmount();
        if (request.getPaymentAmount().compareTo(remainingAmount) > 0) {
            throw new BadRequestException(
                    "Ù…Ø¨Ù„Øº Ø§Ù„Ø¯ÙØ¹ " + request.getPaymentAmount() +
                    " ÙŠØªØ¬Ø§ÙˆØ² Ø§Ù„Ù…Ø¨Ù„Øº Ø§Ù„Ù…ØªØ¨Ù‚ÙŠ " + remainingAmount);
        }

        // Record payment
        payment.recordPayment(request.getPaymentAmount());

        // Update notes if provided
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = payment.getNotes() != null ? payment.getNotes() : "";
            String newNotes = existingNotes.isEmpty() ? request.getNotes() :
                    existingNotes + "\n" + request.getNotes();
            payment.setNotes(newNotes);
        }

        payment = paymentRepository.save(payment);
        log.info("Payment recorded successfully. New status: {}, Paid amount: {}",
                payment.getPaymentStatus(), payment.getPaidAmount());

        Project project = projectRepository.findById(payment.getProjectCode()).orElse(null);
        return mapToResponse(payment, project);
    }

    /**
     * Get all payments for a specific project.
     *
     * @param projectCode Project code
     * @return List of payment schedules
     */
    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getProjectPayments(Long projectCode) {
        log.debug("Fetching payments for project: {}", projectCode);

        // Validate project exists
        Project project = projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with code: " + projectCode));

        List<ProjectDuePayment> payments = paymentRepository.findByProjectCodeOrderBySequenceNoAsc(projectCode);
        return payments.stream()
                .map(payment -> mapToResponse(payment, project))
                .collect(Collectors.toList());
    }

    /**
     * Get payment schedule by ID.
     *
     * @param paymentId Payment ID
     * @return Payment schedule details
     */
    @Transactional(readOnly = true)
    public PaymentScheduleResponse getPaymentById(Long paymentId) {
        log.debug("Fetching payment schedule: {}", paymentId);

        ProjectDuePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment schedule not found with ID: " + paymentId));

        Project project = projectRepository.findById(payment.getProjectCode()).orElse(null);
        return mapToResponse(payment, project);
    }

    /**
     * Get all overdue payments (past due date and not fully paid).
     *
     * @return List of overdue payments
     */
    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getOverduePayments() {
        log.debug("Fetching overdue payments");

        List<ProjectDuePayment> overduePayments = paymentRepository.findOverduePayments(LocalDate.now());
        return overduePayments.stream()
                .map(payment -> {
                    Project project = projectRepository.findById(payment.getProjectCode()).orElse(null);
                    return mapToResponse(payment, project);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get payments due within specified number of days.
     *
     * @param days Number of days ahead to check
     * @return List of upcoming due payments
     */
    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getPaymentsDueWithinDays(int days) {
        log.debug("Fetching payments due within {} days", days);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);

        List<ProjectDuePayment> upcomingPayments =
                paymentRepository.findPaymentsDueWithinPeriod(startDate, endDate);

        return upcomingPayments.stream()
                .map(payment -> {
                    Project project = projectRepository.findById(payment.getProjectCode()).orElse(null);
                    return mapToResponse(payment, project);
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate total remaining amount for a project.
     *
     * @param projectCode Project code
     * @return Total remaining unpaid amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProjectRemainingAmount(Long projectCode) {
        log.debug("Calculating remaining amount for project: {}", projectCode);

        List<ProjectDuePayment> payments = paymentRepository.findByProjectCodeOrderBySequenceNoAsc(projectCode);
        return payments.stream()
                .map(ProjectDuePayment::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Delete payment schedule.
     *
     * @param paymentId Payment ID
     */
    @Transactional
    public void deletePaymentSchedule(Long paymentId) {
        log.info("Deleting payment schedule: {}", paymentId);

        ProjectDuePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment schedule not found with ID: " + paymentId));

        // Check if payment has been partially or fully paid
        if (payment.getPaidAmount() != null && payment.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException(
                    "Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø¬Ø¯ÙˆÙ„ Ø§Ù„Ø¯ÙØ¹ Ø¨Ù…Ø¨Ù„Øº Ù…Ø¯ÙÙˆØ¹. ØªÙ… ØªØ³Ø¬ÙŠÙ„ Ø§Ù„Ø¯ÙØ¹ Ø¨Ø§Ù„ÙØ¹Ù„.");
        }

        paymentRepository.delete(payment);
        log.info("Payment schedule {} deleted successfully", paymentId);
    }

    // ==================== Mapping Methods ====================

    /**
     * Map ProjectDuePayment entity to PaymentScheduleResponse DTO.
     */
    private PaymentScheduleResponse mapToResponse(ProjectDuePayment payment, Project project) {
        PaymentScheduleResponse response = PaymentScheduleResponse.builder()
                .paymentId(payment.getPaymentId())
                .projectCode(payment.getProjectCode())
                .sequenceNo(payment.getSequenceNo())
                .dueDate(payment.getDueDate())
                .dueAmount(payment.getDueAmount())
                .paidAmount(payment.getPaidAmount())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .notes(payment.getNotes())
                .remainingAmount(payment.getRemainingAmount())
                .paymentPercentage(payment.getPaymentPercentage())
                .daysUntilDue(payment.getDaysUntilDue())
                .isOverdue(payment.isOverdue())
                .isPending(payment.isPending())
                .isPartial(payment.isPartial())
                .isPaid(payment.isPaid())
                .createdDate(payment.getCreatedDate())
                .createdBy(payment.getCreatedBy())
                .modifiedDate(payment.getModifiedDate())
                .modifiedBy(payment.getModifiedBy())
                .build();

        // Add project details if available
        if (project != null) {
            response.setProjectName(project.getProjectName());
            response.setProjectName(project.getProjectName());
        }

        return response;
    }
}

