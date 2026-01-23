package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.constants.NotificationPriority;
import com.techno.backend.dto.ManualAttendanceRequest;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.ProjectLaborAssignment;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.ManualAttendanceRequestRepository;
import com.techno.backend.repository.ProjectLaborAssignmentRepository;
import com.techno.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing manual attendance requests with approval workflow.
 *
 * Handles:
 * - Employee submission of manual attendance requests
 * - Approval/rejection workflow
 * - Automatic creation of attendance records upon approval
 *
 * Approval Flow:
 * - Uses ApprovalWorkflowService with request type "MANUAL_ATTENDANCE"
 * - Configured in requests_approval_set table
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualAttendanceRequestService {

    private final ManualAttendanceRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final AttendanceService attendanceService;
    private final AttendanceCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectRepository projectRepository;
    private final ProjectLaborAssignmentRepository assignmentRepository;

    private static final String REQUEST_TYPE = "MANUAL_ATTENDANCE";
    private static final int GRACE_PERIOD_MINUTES = 60; // 60 minutes grace period for manual requests

    /**
     * Submit a new manual attendance request.
     *
     * Validates:
     * - Employee exists and is active
     * - No duplicate request for same date
     * - Entry time is before exit time
     *
     * @param employeeNo Employee number
     * @param attendanceDate Attendance date
     * @param entryTime Entry time (HH:mm format)
     * @param exitTime Exit time (HH:mm format)
     * @param reason Reason for manual attendance
     * @param requestedBy Employee who submitted the request
     * @return Created request
     */
    @Transactional
    public com.techno.backend.entity.ManualAttendanceRequest submitRequest(Long employeeNo, LocalDate attendanceDate,
                                                  LocalTime entryTime, LocalTime exitTime,
                                                  String reason, Long requestedBy) {
        log.info("Submitting manual attendance request for employee {} on {}", employeeNo, attendanceDate);

        // Validate employee
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود: " + employeeNo));

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new BadRequestException("يمكن للموظفين النشطين فقط تقديم طلبات الحضور اليدوي");
        }

        // Check for duplicate request (block if pending or approved, allow if rejected)
        Optional<com.techno.backend.entity.ManualAttendanceRequest> existingRequest =
                requestRepository.findNonRejectedRequestByEmployeeAndDate(employeeNo, attendanceDate);
        
        if (existingRequest.isPresent()) {
            com.techno.backend.entity.ManualAttendanceRequest request = existingRequest.get();
            String statusMessage = "N".equals(request.getTransStatus()) ? "قيد الانتظار" :
                                   "A".equals(request.getTransStatus()) ? "معتمد" : "غير معروف";
            throw new BadRequestException(
                    String.format("طلب حضور يدوي موجود بالفعل للموظف %d في تاريخ %s (الحالة: %s). " +
                            "لا يمكن تقديم طلب جديد إلا بعد رفض الطلب السابق.",
                            employeeNo, attendanceDate, statusMessage));
        }

        // Validate entry time is before exit time
        if (entryTime.isAfter(exitTime) || entryTime.equals(exitTime)) {
            throw new BadRequestException("وقت الدخول يجب أن يكون قبل وقت الخروج");
        }

        // Validate 60-minute grace period for manual requests
        // Allow submission within 60 minutes after scheduled end time
        TimeSchedule schedule = calculationService.findApplicableSchedule(
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode());

        if (schedule != null && schedule.getScheduledEndTime() != null) {
            LocalDateTime scheduledEndDateTime = LocalDateTime.of(attendanceDate, schedule.getScheduledEndTime());
            LocalDateTime gracePeriodEnd = scheduledEndDateTime.plusMinutes(GRACE_PERIOD_MINUTES);
            LocalDateTime now = LocalDateTime.now();

            // If request is for today or yesterday, check grace period
            if (attendanceDate.equals(LocalDate.now()) || attendanceDate.equals(LocalDate.now().minusDays(1))) {
                if (now.isAfter(gracePeriodEnd)) {
                    log.warn("Manual attendance request submitted after 60-minute grace period. " +
                            "Employee: {}, Date: {}, Scheduled End: {}, Current Time: {}, Grace End: {}",
                            employeeNo, attendanceDate, scheduledEndDateTime, now, gracePeriodEnd);
                    // Allow with warning - the request will still be processed but may require higher approval
                    // You can add additional logic here if needed (e.g., require special approval level)
                } else {
                    log.debug("Manual attendance request within grace period. " +
                            "Employee: {}, Date: {}, Scheduled End: {}, Current Time: {}",
                            employeeNo, attendanceDate, scheduledEndDateTime, now);
                }
            }
        }

        // Initialize approval workflow
        ApprovalWorkflowService.ApprovalInfo approvalInfo = approvalWorkflowService.initializeApproval(
                REQUEST_TYPE,
                employeeNo,
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode()
        );

        // Create request
        com.techno.backend.entity.ManualAttendanceRequest request = com.techno.backend.entity.ManualAttendanceRequest.builder()
                .employeeNo(employeeNo)
                .attendanceDate(attendanceDate)
                .entryTime(entryTime)
                .exitTime(exitTime)
                .reason(reason)
                .requestDate(LocalDate.now())
                .requestedBy(requestedBy)
                .transStatus(approvalInfo.getTransStatus())
                .nextApproval(approvalInfo.getNextApproval())
                .nextAppLevel(approvalInfo.getNextAppLevel())
                .build();

        request = requestRepository.save(request);

        log.info("Manual attendance request created: ID={}, Next Approver={}",
                request.getRequestId(), approvalInfo.getNextApproval());

        return request;
    }

    /**
     * Get all manual attendance requests with optional filters.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo Employee number - optional
     * @param startDate Start date (optional) - filters by attendanceDate
     * @param endDate End date (optional) - filters by attendanceDate
     * @param pageable Pagination parameters
     * @return Page of requests
     */
    @Transactional(readOnly = true)
    public Page<com.techno.backend.entity.ManualAttendanceRequest> getAllRequests(String transStatus, Long employeeNo,
                                                         LocalDate startDate, LocalDate endDate,
                                                         Pageable pageable) {
        log.debug("Fetching all manual attendance requests with filters: status={}, employee={}, startDate={}, endDate={}",
                transStatus, employeeNo, startDate, endDate);

        return requestRepository.findAllWithFilters(transStatus, employeeNo, startDate, endDate, pageable);
    }

    /**
     * Get request by ID.
     *
     * @param requestId Request ID
     * @return Request details
     */
    @Transactional(readOnly = true)
    public com.techno.backend.entity.ManualAttendanceRequest getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("طلب الحضور اليدوي غير موجود: " + requestId));
    }

    /**
     * Approve request at current approval level.
     *
     * If this is the final approval level:
     * - Set status to Approved (A)
     * - Create actual attendance record using AttendanceService
     *
     * Otherwise:
     * - Move to next approval level
     *
     * @param requestId Request ID
     * @param approverNo Employee number of approver
     * @return Updated request
     */
    @Transactional
    public com.techno.backend.entity.ManualAttendanceRequest approveRequest(Long requestId, Long approverNo) {
        log.info("Approving manual attendance request {}, approver: {}", requestId, approverNo);

        com.techno.backend.entity.ManualAttendanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("طلب الحضور اليدوي غير موجود: " + requestId));

        // Validate not already processed
        if ("A".equals(request.getTransStatus())) {
            throw new BadRequestException("الطلب معتمد بالفعل");
        }
        if ("R".equals(request.getTransStatus())) {
            throw new BadRequestException("لا يمكن الموافقة على الطلب المرفوض");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                request.getNextAppLevel(),
                approverNo,
                request.getNextApproval())) {
            throw new BadRequestException(
                    "الموظف " + approverNo + " غير مصرح له بالموافقة على هذا الطلب");
        }

        // Get employee for approval workflow
        Employee employee = employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود: " + request.getEmployeeNo()));

        // Move to next approval level
        ApprovalWorkflowService.ApprovalInfo nextLevel = approvalWorkflowService.moveToNextLevel(
                REQUEST_TYPE,
                request.getNextAppLevel(),
                request.getEmployeeNo(),
                employee.getPrimaryDeptCode(),
                employee.getPrimaryProjectCode()
        );

        // Update request with new approval info
        request.approve(approverNo, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
        request.setTransStatus(nextLevel.getTransStatus());

        // If fully approved, create attendance record
        if ("A".equals(nextLevel.getTransStatus())) {
            request.setApprovedBy(approverNo);
            request.setApprovedDate(LocalDateTime.now());
            log.info("Request {} fully approved, creating attendance record", requestId);

            // Create attendance record (save request first to ensure it's persisted)
            com.techno.backend.entity.ManualAttendanceRequest savedRequest = requestRepository.save(request);
            createAttendanceFromRequest(savedRequest);

            return savedRequest;
        } else {
            log.info("Request {} moved to next approval level {}, next approver: {}",
                    requestId, nextLevel.getNextAppLevel(), nextLevel.getNextApproval());
            return requestRepository.save(request);
        }
    }

    /**
     * Reject request.
     *
     * @param requestId Request ID
     * @param approverNo Employee number of approver
     * @param rejectionReason Reason for rejection
     * @return Updated request
     */
    @Transactional
    public com.techno.backend.entity.ManualAttendanceRequest rejectRequest(Long requestId, Long approverNo, String rejectionReason) {
        log.info("Rejecting manual attendance request {}, approver: {}", requestId, approverNo);

        com.techno.backend.entity.ManualAttendanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("طلب الحضور اليدوي غير موجود: " + requestId));

        // Validate not already processed
        if ("A".equals(request.getTransStatus())) {
            throw new BadRequestException("لا يمكن رفض الطلب المعتمد");
        }
        if ("R".equals(request.getTransStatus())) {
            throw new BadRequestException("الطلب مرفوض بالفعل");
        }

        // Validate approver authorization
        if (!approvalWorkflowService.canApprove(
                REQUEST_TYPE,
                request.getNextAppLevel(),
                approverNo,
                request.getNextApproval())) {
            throw new BadRequestException(
                    "الموظف " + approverNo + " غير مصرح له برفض هذا الطلب");
        }

        // Validate rejection reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("سبب الرفض مطلوب");
        }

        // Reject the request
        request.reject(approverNo, rejectionReason);
        request = requestRepository.save(request);

        log.info("Request {} rejected by {}", requestId, approverNo);

        // Publish notification to employee
        try {
            Employee employee = employeeRepository.findById(request.getEmployeeNo())
                    .orElse(null);
            if (employee != null) {
                publishRejectionNotification(request, employee, rejectionReason);
            }
        } catch (Exception e) {
            log.error("Failed to publish rejection notification for request {}: {}", requestId, e.getMessage(), e);
            // Don't fail the rejection if notification fails
        }

        return request;
    }

    /**
     * Create attendance record from approved request.
     *
     * Converts ManualAttendanceRequest entity to ManualAttendanceRequest DTO
     * and calls AttendanceService.createManualAttendance.
     *
     * @param request Approved manual attendance request
     */
    private void createAttendanceFromRequest(com.techno.backend.entity.ManualAttendanceRequest request) {
        log.info("Creating attendance record from approved request {}", request.getRequestId());

        // Convert LocalTime to LocalDateTime by combining with attendanceDate
        LocalDateTime entryDateTime = LocalDateTime.of(request.getAttendanceDate(), request.getEntryTime());
        LocalDateTime exitDateTime = LocalDateTime.of(request.getAttendanceDate(), request.getExitTime());

        // If exit time is before entry time (e.g., 17:00 entry, 08:00 exit), assume next day
        if (exitDateTime.isBefore(entryDateTime) || exitDateTime.equals(entryDateTime)) {
            exitDateTime = exitDateTime.plusDays(1);
        }

        // Get employee to get project code
        Employee employee = employeeRepository.findById(request.getEmployeeNo())
                .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود: " + request.getEmployeeNo()));

        // Find project for the attendance date using the same priority as getDailyOverview:
        // Priority 1: Employee's primary project
        // Priority 2: Active labor assignment for the date
        Long projectCode = null;
        if (employee.getPrimaryProjectCode() != null) {
            projectCode = employee.getPrimaryProjectCode();
            log.debug("Using primary project code {} for employee {} on date {}",
                    projectCode, request.getEmployeeNo(), request.getAttendanceDate());
        } else {
            // Priority 2: Check for active assignment for the specific date
            List<ProjectLaborAssignment> activeAssignments = assignmentRepository
                    .findActiveAssignmentsByEmployee(request.getEmployeeNo());

            LocalDate attendanceDate = request.getAttendanceDate();
            for (ProjectLaborAssignment assignment : activeAssignments) {
                if ((assignment.getStartDate().isBefore(attendanceDate) || assignment.getStartDate().isEqual(attendanceDate)) &&
                        (assignment.getEndDate() == null || assignment.getEndDate().isAfter(attendanceDate)
                                || assignment.getEndDate().isEqual(attendanceDate))) {
                    projectCode = assignment.getProjectCode();
                    log.debug("Found active assignment: project {} for employee {} on date {}",
                            projectCode, request.getEmployeeNo(), attendanceDate);
                    break;
                }
            }
        }

        // Build DTO for attendance service
        ManualAttendanceRequest attendanceDTO = ManualAttendanceRequest.builder()
                .employeeNo(request.getEmployeeNo())
                .attendanceDate(request.getAttendanceDate())
                .projectCode(projectCode) // Use found project code (may be null)
                .entryTime(entryDateTime)
                .exitTime(exitDateTime)
                .absenceFlag("N")
                .notes("Created from manual attendance request #" + request.getRequestId() + ": " + request.getReason())
                .build();

        // Create attendance record
        try {
            attendanceService.createManualAttendance(attendanceDTO);
            log.info("Attendance record created successfully from request {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to create attendance record from request {}: {}", request.getRequestId(), e.getMessage(), e);
            throw new BadRequestException("فشل إنشاء سجل الحضور: " + e.getMessage());
        }
    }

    /**
     * Publish notification when manual attendance request is rejected.
     *
     * @param request Rejected request
     * @param employee Employee who submitted the request
     * @param rejectionReason Reason for rejection
     */
    private void publishRejectionNotification(com.techno.backend.entity.ManualAttendanceRequest request,
                                               Employee employee, String rejectionReason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("employeeName", employee.getEmployeeName());
            variables.put("attendanceDate", request.getAttendanceDate().toString());
            variables.put("entryTime", request.getEntryTime().toString());
            variables.put("exitTime", request.getExitTime().toString());
            variables.put("rejectionReason", rejectionReason);
            variables.put("requestId", request.getRequestId().toString());
            variables.put("linkUrl", "/manual-attendance/" + request.getRequestId());

            NotificationEvent event = new NotificationEvent(
                    this,
                    NotificationEventType.MANUAL_ATTENDANCE_REJECTED,
                    request.getEmployeeNo(),
                    NotificationPriority.HIGH,
                    "MANUAL_ATTENDANCE",
                    request.getRequestId(),
                    variables);

            eventPublisher.publishEvent(event);
            log.info("Published MANUAL_ATTENDANCE_REJECTED notification event: requestId={}, employeeNo={}, date={}, reason={}",
                    request.getRequestId(), request.getEmployeeNo(), request.getAttendanceDate(), rejectionReason);
        } catch (Exception e) {
            log.error("Failed to publish manual attendance rejection notification for request {}: {}",
                    request.getRequestId(), e.getMessage(), e);
        }
    }
}

