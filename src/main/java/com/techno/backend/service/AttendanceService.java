package com.techno.backend.service;

import com.techno.backend.dto.*;
import com.techno.backend.entity.AttendanceTransaction;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.EmployeeLeave;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.TimeSchedule;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmployeeLeaveRepository;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.util.AttendanceCalculator;
import com.techno.backend.util.GPSCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for Attendance Management.
 * Handles business logic for employee check-in, check-out, and attendance
 * tracking.
 *
 * Features:
 * - GPS-based check-in/out validation
 * - Automatic hours calculation (working, overtime, delays, etc.)
 * - Manual attendance entry by HR administrators
 * - Holiday and weekend work detection
 * - Attendance reports and queries
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final AttendanceCalculationService calculationService;
    private final AttendanceAllowanceDeductionService attendanceAllowanceDeductionService;
    private final AttendanceDayClosureService closureService;
    private final EmployeeLeaveRepository leaveRepository;
    private final com.techno.backend.repository.ProjectLaborAssignmentRepository assignmentRepository;
    private final com.techno.backend.repository.HolidayRepository holidayRepository;

    /**
     * Employee checks in with GPS validation.
     *
     * Validates:
     * - Employee exists and is active
     * - Project exists and is active
     * - Employee hasn't already checked in today
     * - GPS coordinates are within allowed radius (if required)
     *
     * @param employeeNo Employee number (from JWT token)
     * @param request    Check-in request with GPS coordinates
     * @return Check-in response with entry details
     * @throws ResourceNotFoundException if employee or project not found
     * @throws BadRequestException       if validation fails
     */
    @Transactional
    public CheckInResponse checkIn(Long employeeNo, CheckInRequest request) {
        log.info("Processing check-in for employee {} at project {}", employeeNo, request.getProjectCode());

        // Validate employee
        Employee employee = findEmployeeOrThrow(employeeNo);
        validateEmployeeActive(employee);

        // Validate project
        Project project = findProjectOrThrow(request.getProjectCode());

        // Check if already checked in today
        LocalDate today = LocalDate.now();
        if (attendanceRepository.hasCheckedInToday(employeeNo, today)) {
            log.warn("Employee {} already checked in today ({})", employeeNo, today);
            throw new BadRequestException("لقد قمت بتسجيل الدخول اليوم بالفعل. يرجى تسجيل الخروج أولاً.");
        }

        // Validate attendance date is not closed
        if (closureService.isDateClosed(today)) {
            throw new BadRequestException("الحضور لهذا التاريخ مغلق ولا يمكن تعديله");
        }

        // Validate check-in time is not after scheduled end time
        TimeSchedule schedule = calculationService.findApplicableSchedule(
                employee.getPrimaryDeptCode(), request.getProjectCode());
        if (schedule == null) {
            // If no schedule found, create default 8-hour schedule (08:00-17:00)
            schedule = TimeSchedule.builder()
                    .scheduleName("الجدول الافتراضي")
                    .scheduledStartTime(LocalTime.of(8, 0))
                    .scheduledEndTime(LocalTime.of(17, 0))
                    .requiredHours(new BigDecimal("8.00"))
                    .gracePeriodMinutes(15)
                    .isActive("Y")
                    .build();
        }
        
        LocalTime currentTime = LocalDateTime.now().toLocalTime();
        LocalTime scheduledEndTime = schedule.getScheduledEndTime();
        
        // Check if current time is after scheduled end time
        boolean isAfterEndTime = false;
        if (schedule.crossesMidnight()) {
            // For midnight-crossing schedules (e.g., 22:00 to 06:00)
            // The shift ends at 06:00 the next day
            // Check-in is invalid if current time is after end time AND before start time
            // (meaning we're past the end time on the next day, e.g., 07:00 when schedule is 22:00-06:00)
            if (currentTime.isAfter(scheduledEndTime) && currentTime.isBefore(schedule.getScheduledStartTime())) {
                isAfterEndTime = true;
            }
        } else {
            // For normal schedules (e.g., 08:00 to 17:00)
            // Check-in is invalid if current time is after end time
            if (currentTime.isAfter(scheduledEndTime)) {
                isAfterEndTime = true;
            }
        }
        
        if (isAfterEndTime) {
            String endTimeStr = scheduledEndTime.toString().substring(0, 5); // Format as HH:mm
            log.warn("Check-in rejected for employee {}: current time {} is after scheduled end time {}",
                    employeeNo, currentTime, scheduledEndTime);
            throw new BadRequestException(
                    String.format("لا يمكن تسجيل الدخول بعد انتهاء وقت العمل المقرر (%s). وقت انتهاء العمل: %s",
                            endTimeStr, endTimeStr));
        }

        // Validate GPS coordinates if required
        Double distanceMeters = null;
        if ("Y".equals(project.getRequireGpsCheck())) {
            validateGPSLocation(request.getLatitude(), request.getLongitude(),
                    project.getProjectLatitude(), project.getProjectLongitude(),
                    project.getGpsRadiusMeters(), project.getProjectName());

            // Calculate actual distance
            distanceMeters = GPSCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    project.getProjectLatitude(), project.getProjectLongitude());
        }

        // Create attendance record
        AttendanceTransaction attendance = AttendanceTransaction.builder()
                .employeeNo(employeeNo)
                .attendanceDate(today)
                .projectCode(request.getProjectCode())
                .entryTime(LocalDateTime.now())
                .entryLatitude(request.getLatitude())
                .entryLongitude(request.getLongitude())
                .entryDistanceMeters(distanceMeters)
                .absenceFlag("N")
                .isAutoCheckout("N")
                .isManualEntry("N")
                .build();

        // Set flags based on date (holiday/weekend detection)
        calculationService.calculateAttendanceHours(
                attendance,
                employee.getPrimaryDeptCode(),
                request.getProjectCode());

        // Save attendance record
        attendance = attendanceRepository.save(attendance);

        log.info("Check-in successful for employee {} at project {}. Transaction ID: {}",
                employeeNo, request.getProjectCode(), attendance.getTransactionId());

        // Build response
        return buildCheckInResponse(attendance, employee, project);
    }

    /**
     * Employee checks out with GPS validation and automatic calculations.
     *
     * @param employeeNo Employee number (from JWT token)
     * @param request    Check-out request with GPS coordinates
     * @return Check-out response with calculated hours
     * @throws ResourceNotFoundException if attendance record not found
     * @throws BadRequestException       if validation fails
     */
    @Transactional
    public CheckOutResponse checkOut(Long employeeNo, CheckOutRequest request) {
        log.info("Processing check-out for employee {}", employeeNo);

        // Find today's attendance record
        AttendanceTransaction attendance;
        if (request.getTransactionId() != null) {
            attendance = findAttendanceOrThrow(request.getTransactionId());
            if (!attendance.getEmployeeNo().equals(employeeNo)) {
                throw new BadRequestException("سجل الحضور هذا لا ينتمي إليك");
            }
        } else {
            LocalDate today = LocalDate.now();
            attendance = attendanceRepository.findByEmployeeNoAndAttendanceDate(employeeNo, today)
                    .orElseThrow(() -> new BadRequestException(
                            "لم يتم العثور على سجل تسجيل دخول لليوم. يرجى تسجيل الدخول أولاً."));
        }

        // Validate not already checked out
        if (attendance.hasCheckedOut()) {
            log.warn("Employee {} already checked out for transaction {}", employeeNo, attendance.getTransactionId());
            throw new BadRequestException("لقد قمت بتسجيل الخروج اليوم بالفعل.");
        }

        // Validate attendance date is not closed
        if (closureService.isDateClosed(attendance.getAttendanceDate())) {
            throw new BadRequestException("الحضور لهذا التاريخ مغلق ولا يمكن تعديله");
        }

        // Get project for GPS validation
        Project project = findProjectOrThrow(attendance.getProjectCode());
        Employee employee = findEmployeeOrThrow(employeeNo);

        // Validate GPS coordinates if required
        Double distanceMeters = null;
        if ("Y".equals(project.getRequireGpsCheck())) {
            validateGPSLocation(request.getLatitude(), request.getLongitude(),
                    project.getProjectLatitude(), project.getProjectLongitude(),
                    project.getGpsRadiusMeters(), project.getProjectName());

            distanceMeters = GPSCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    project.getProjectLatitude(), project.getProjectLongitude());
        }

        // Update attendance with exit details
        attendance.setExitTime(LocalDateTime.now());
        attendance.setExitLatitude(request.getLatitude());
        attendance.setExitLongitude(request.getLongitude());
        attendance.setExitDistanceMeters(distanceMeters);

        // Recalculate all hours (working, overtime, delays, etc.)
        calculationService.calculateAttendanceHours(
                attendance,
                employee.getPrimaryDeptCode(),
                attendance.getProjectCode());

        // Auto-create allowances and deductions from attendance calculations (Phase 6)
        attendanceAllowanceDeductionService.processAttendanceForAllowancesDeductions(attendance);

        // Save updated record
        attendance = attendanceRepository.save(attendance);

        log.info("Check-out successful for employee {}. Working hours: {}, Overtime: {}",
                employeeNo, attendance.getWorkingHours(), attendance.getOvertimeCalc());

        return buildCheckOutResponse(attendance, employee, project);
    }

    /**
     * HR creates or updates manual attendance entry.
     * Requires ADMIN or HR role.
     *
     * @param request Manual attendance request
     * @return Complete attendance response
     */
    @Transactional
    public AttendanceResponse createManualAttendance(ManualAttendanceRequest request) {
        log.info("Creating manual attendance for employee {} on {}",
                request.getEmployeeNo(), request.getAttendanceDate());

        // Validate employee
        Employee employee = findEmployeeOrThrow(request.getEmployeeNo());

        // Validate attendance date is not closed
        if (closureService.isDateClosed(request.getAttendanceDate())) {
            throw new BadRequestException("الحضور لهذا التاريخ مغلق ولا يمكن تعديله");
        }

        // Check if attendance already exists for this date
        Optional<AttendanceTransaction> existing = attendanceRepository
                .findByEmployeeNoAndAttendanceDate(request.getEmployeeNo(), request.getAttendanceDate());

        if (existing.isPresent()) {
            throw new BadRequestException(
                    String.format("سجل الحضور موجود بالفعل للموظف %d في %s. استخدم التحديث بدلاً من ذلك.",
                            request.getEmployeeNo(), request.getAttendanceDate()));
        }

        // Validate project if provided
        Project project = null;
        if (request.getProjectCode() != null) {
            project = findProjectOrThrow(request.getProjectCode());
        }

        // Create attendance record
        AttendanceTransaction attendance = buildManualAttendance(request, employee);

        // Calculate hours if not provided manually
        if (request.getWorkingHours() == null || request.getOvertimeCalc() == null) {
            calculationService.calculateAttendanceHours(
                    attendance,
                    employee.getPrimaryDeptCode(),
                    request.getProjectCode());
        }

        // Save record
        attendance = attendanceRepository.save(attendance);

        log.info("Manual attendance created successfully. Transaction ID: {}", attendance.getTransactionId());

        return mapToResponse(attendance, employee, project);
    }

    /**
     * Update existing attendance record.
     * Requires ADMIN, HR_MANAGER, or PROJECT_MANAGER (for auto-checkout edits).
     *
     * Special handling for auto-checkout records:
     * - Project Managers can edit auto-checkout times for employees in their projects
     * - After edit, marks as manually edited in notes field
     * - Recalculates all hours
     *
     * @param transactionId Transaction ID
     * @param request       Update request
     * @param editorEmployeeNo Employee number of person making the edit (for audit)
     * @return Updated attendance response
     */
    @Transactional
    public AttendanceResponse updateAttendance(Long transactionId, ManualAttendanceRequest request, Long editorEmployeeNo) {
        log.info("Updating attendance transaction {} by editor {}", transactionId, editorEmployeeNo);

        AttendanceTransaction attendance = findAttendanceOrThrow(transactionId);
        Employee employee = findEmployeeOrThrow(attendance.getEmployeeNo());

        // Validate attendance date is not closed
        if (closureService.isDateClosed(attendance.getAttendanceDate())) {
            throw new BadRequestException("الحضور لهذا التاريخ مغلق ولا يمكن تعديله");
        }

        // Check if this is an auto-checkout record being edited
        boolean isAutoCheckoutEdit = "Y".equals(attendance.getIsAutoCheckout()) && 
                                     (request.getExitTime() != null || request.getEntryTime() != null);

        if (isAutoCheckoutEdit) {
            log.info("Editing auto-checkout record {} for employee {}", transactionId, attendance.getEmployeeNo());
            // Mark that auto-checkout was manually edited
            String editNote = String.format("Auto-checkout edited by employee %d on %s. Original: Entry=%s, Exit=%s",
                    editorEmployeeNo, LocalDateTime.now(), attendance.getEntryTime(), attendance.getExitTime());
            if (attendance.getNotes() != null && !attendance.getNotes().isEmpty()) {
                attendance.setNotes(attendance.getNotes() + "\n" + editNote);
            } else {
                attendance.setNotes(editNote);
            }
            // Optionally mark as no longer auto-checkout (or keep flag for audit)
            // attendance.setIsAutoCheckout("N"); // Uncomment if you want to remove auto-checkout flag
        }

        Project project = null;
        if (request.getProjectCode() != null) {
            project = findProjectOrThrow(request.getProjectCode());
            attendance.setProjectCode(request.getProjectCode());
        }

        // Update fields from request
        updateAttendanceFromRequest(attendance, request);

        // Always recalculate hours after edit (especially important for auto-checkout edits)
        calculationService.calculateAttendanceHours(
                attendance,
                employee.getPrimaryDeptCode(),
                attendance.getProjectCode());

        // Re-process allowances/deductions after recalculation
        attendanceAllowanceDeductionService.processAttendanceForAllowancesDeductions(attendance);

        attendance = attendanceRepository.save(attendance);

        log.info("Attendance updated successfully. Transaction ID: {}, Auto-checkout edit: {}", 
                transactionId, isAutoCheckoutEdit);

        return mapToResponse(attendance, employee, project);
    }

    /**
     * Get attendance record by transaction ID.
     *
     * @param transactionId Transaction ID
     * @return Attendance response
     */
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long transactionId) {
        log.debug("Fetching attendance by ID: {}", transactionId);

        AttendanceTransaction attendance = findAttendanceOrThrow(transactionId);
        Employee employee = findEmployeeOrThrow(attendance.getEmployeeNo());
        Project project = attendance.getProjectCode() != null
                ? projectRepository.findById(attendance.getProjectCode()).orElse(null)
                : null;

        return mapToResponse(attendance, employee, project);
    }

    /**
     * Get attendance records for an employee within a date range.
     *
     * @param employeeNo Employee number
     * @param startDate  Start date
     * @param endDate    End date
     * @param pageable   Pagination
     * @return Page of attendance records
     */
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getEmployeeAttendance(Long employeeNo, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        log.debug("Fetching attendance for employee {} from {} to {}", employeeNo, startDate, endDate);

        Employee employee = findEmployeeOrThrow(employeeNo);

        Page<AttendanceTransaction> attendancePage = attendanceRepository
                .findByEmployeeNoAndDateRange(employeeNo, startDate, endDate, pageable);

        return attendancePage.map(attendance -> {
            Project project = attendance.getProjectCode() != null
                    ? projectRepository.findById(attendance.getProjectCode()).orElse(null)
                    : null;
            return mapToResponse(attendance, employee, project);
        });
    }

    /**
     * Get attendance record for a specific employee and date.
     *
     * @param employeeNo Employee number
     * @param date       Date
     * @return Attendance response or null if not found
     */
    @Transactional(readOnly = true)
    public AttendanceResponse getTodayAttendance(Long employeeNo, LocalDate date) {
        return attendanceRepository.findByEmployeeNoAndAttendanceDate(employeeNo, date)
                .map(attendance -> {
                    Employee employee = findEmployeeOrThrow(employeeNo);
                    Project project = attendance.getProjectCode() != null
                            ? projectRepository.findById(attendance.getProjectCode()).orElse(null)
                            : null;
                    return mapToResponse(attendance, employee, project);
                })
                .orElse(null);
    }

    /**
     * Get all attendance records with optional filters.
     * Used for listing all attendance records in the attendance tracking page.
     *
     * @param startDate   Start date (optional)
     * @param endDate     End date (optional)
     * @param employeeNo  Employee number (optional)
     * @param projectCode Project code (optional)
     * @param pageable    Pagination parameters
     * @return Page of attendance records
     */
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAllAttendance(LocalDate startDate, LocalDate endDate,
            Long employeeNo, Long projectCode,
            Pageable pageable) {
        log.debug("Fetching all attendance records from {} to {}, employee: {}, project: {}",
                startDate, endDate, employeeNo, projectCode);

        Page<AttendanceTransaction> attendancePage = attendanceRepository
                .findAllByDateRange(startDate, endDate, employeeNo, projectCode, pageable);

        // Extract unique employee numbers and project codes
        java.util.Set<Long> employeeNos = new java.util.HashSet<>();
        java.util.Set<Long> projectCodes = new java.util.HashSet<>();

        attendancePage.getContent().forEach(attendance -> {
            employeeNos.add(attendance.getEmployeeNo());
            if (attendance.getProjectCode() != null) {
                projectCodes.add(attendance.getProjectCode());
            }
        });

        // Fetch all employees and projects in bulk
        java.util.Map<Long, Employee> employeeMap = new java.util.HashMap<>();
        if (!employeeNos.isEmpty()) {
            employeeRepository.findAllById(employeeNos).forEach(emp -> employeeMap.put(emp.getEmployeeNo(), emp));
        }

        java.util.Map<Long, Project> projectMap = new java.util.HashMap<>();
        if (!projectCodes.isEmpty()) {
            projectRepository.findAllById(projectCodes).forEach(proj -> projectMap.put(proj.getProjectCode(), proj));
        }

        // Map attendance records to responses
        return attendancePage.map(attendance -> {
            Employee employee = employeeMap.get(attendance.getEmployeeNo());
            if (employee == null) {
                log.warn("Employee {} not found for attendance record {}", attendance.getEmployeeNo(),
                        attendance.getTransactionId());
                // Create a minimal employee object to avoid null pointer
                employee = Employee.builder()
                        .employeeNo(attendance.getEmployeeNo())
                        .employeeName("غير معروف")
                        .build();
            }
            Project project = attendance.getProjectCode() != null
                    ? projectMap.get(attendance.getProjectCode())
                    : null;
            return mapToResponse(attendance, employee, project);
        });
    }

    @Transactional(readOnly = true)
    public DailyOverviewDto getDailyOverview(Long employeeNo) {
        log.info("Getting daily overview for employee {}", employeeNo);

        Employee employee = findEmployeeOrThrow(employeeNo);
        LocalDate today = LocalDate.now();

        // 1. Check for today's attendance
        Optional<AttendanceTransaction> attendanceOpt = attendanceRepository
                .findByEmployeeNoAndAttendanceDate(employeeNo, today);

        // 2. Check for holiday
        Optional<com.techno.backend.entity.Holiday> holidayOpt = holidayRepository.findByHolidayDate(today);

        // 3. Check for weekend
        boolean isWeekend = calculationService.isWeekendDate(today);

        // 4. Get active project
        Project project = null;
        if (attendanceOpt.isPresent() && attendanceOpt.get().getProjectCode() != null) {
            project = projectRepository.findById(attendanceOpt.get().getProjectCode()).orElse(null);
        } else if (employee.getPrimaryProjectCode() != null) {
            project = projectRepository.findById(employee.getPrimaryProjectCode()).orElse(null);
        } else {
            // Priority 3: Check for active assignment if Priority 1 & 2 are null
            List<com.techno.backend.entity.ProjectLaborAssignment> activeAssignments = assignmentRepository
                    .findActiveAssignmentsByEmployee(employeeNo);

            for (com.techno.backend.entity.ProjectLaborAssignment assignment : activeAssignments) {
                if ((assignment.getStartDate().isBefore(today) || assignment.getStartDate().isEqual(today)) &&
                        (assignment.getEndDate() == null || assignment.getEndDate().isAfter(today)
                                || assignment.getEndDate().isEqual(today))) {
                    project = projectRepository.findById(assignment.getProjectCode()).orElse(null);
                    if (project != null)
                        break;
                }
            }
        }

        String statusAr = "يوم عمل";
        String statusColor = "#10B981"; // Success green

        if (holidayOpt.isPresent()) {
            statusAr = "عطلة رسمية: " + holidayOpt.get().getHolidayName();
            statusColor = "#F59E0B"; // Holiday orange
        } else if (isWeekend) {
            statusAr = "عطلة نهاية الأسبوع";
            statusColor = "#6B7280"; // Weekend gray
        } else if (project == null) {
            statusAr = "قيد الانتظار للتكليف بمشروع";
            statusColor = "#3B82F6"; // Info blue (more formal/less alert)
        }

        DailyOverviewDto overview = DailyOverviewDto.builder()
                .date(today)
                .dayName(today.getDayOfWeek().name())
                .isHoliday(holidayOpt.isPresent())
                .holidayName(holidayOpt.map(com.techno.backend.entity.Holiday::getHolidayName).orElse(null))
                .isWeekend(isWeekend)
                .isWorkDay(!isWeekend && !holidayOpt.isPresent())
                .projectName(project != null ? project.getProjectName() : null)
                .projectCode(project != null ? project.getProjectCode() : null)
                .statusAr(statusAr)
                .statusColor(statusColor)
                .build();

        final Project finalProject = project;
        attendanceOpt.ifPresent(a -> overview.setAttendance(mapToResponse(a, employee, finalProject)));

        return overview;
    }

    /**
     * Get employee timesheet for a specific month.
     * Returns day-by-day attendance status with summary statistics.
     *
     * @param employeeNo Employee number
     * @param month      Month in YYYY-MM format
     * @return Timesheet response with calendar data
     */
    @Transactional(readOnly = true)
    public TimesheetResponse getEmployeeTimesheet(Long employeeNo, String month) {
        log.info("Getting timesheet for employee {} for month {}", employeeNo, month);

        // Parse month
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Get employee
        Employee employee = findEmployeeOrThrow(employeeNo);

        // Get all attendance records for the month
        List<AttendanceTransaction> attendances = attendanceRepository
                .findByEmployeeNoAndDateRange(employeeNo, monthStart, monthEnd, Pageable.unpaged())
                .getContent();

        // Get approved leaves for the month
        List<EmployeeLeave> leaves = leaveRepository.findOverlappingLeaves(employeeNo, monthStart, monthEnd)
                .stream()
                .filter(leave -> "A".equals(leave.getTransStatus()))
                .collect(Collectors.toList());

        // Get project assignments for the month to check for "No Project" status
        List<com.techno.backend.entity.ProjectLaborAssignment> assignments = assignmentRepository
                .findOverlappingAssignments(employeeNo, monthStart, monthEnd);

        // Build day-by-day calendar
        List<TimesheetResponse.TimesheetDay> days = new ArrayList<>();
        int present = 0;
        int absent = 0;
        int onLeave = 0;
        int late = 0;
        int weekends = 0;
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        BigDecimal totalLateHours = BigDecimal.ZERO;

        // Create a map of attendance by date
        Map<LocalDate, AttendanceTransaction> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(AttendanceTransaction::getAttendanceDate, a -> a));

        // Create a set of leave dates
        Set<LocalDate> leaveDates = new HashSet<>();
        for (EmployeeLeave leave : leaves) {
            LocalDate from = leave.getLeaveFromDate();
            LocalDate to = leave.getLeaveToDate();
            LocalDate current = from;
            while (!current.isAfter(to)) {
                if (!current.isBefore(monthStart) && !current.isAfter(monthEnd)) {
                    leaveDates.add(current);
                }
                current = current.plusDays(1);
            }
        }

        // Process each day of the month
        LocalDate currentDate = monthStart;
        while (!currentDate.isAfter(monthEnd)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY;
            boolean isHoliday = calculationService.isHolidayDate(currentDate);

            TimesheetResponse.TimesheetDay day = TimesheetResponse.TimesheetDay.builder()
                    .date(currentDate)
                    .day(currentDate.getDayOfMonth())
                    .build();

            if (isWeekend || isHoliday) {
                day.setStatus("Weekend");
                day.setColor("#F3F4F6");
                day.setTextColor("#6B7280");
                weekends++;
            } else if (leaveDates.contains(currentDate)) {
                day.setStatus("Leave");
                day.setColor("#FEF3C7");
                day.setTextColor("#92400E");
                onLeave++;
            } else {
                AttendanceTransaction attendance = attendanceMap.get(currentDate);
                if (attendance != null) {
                    if ("Y".equals(attendance.getAbsenceFlag())) {
                        day.setStatus("غائب");
                        day.setColor("#FEE2E2");
                        day.setTextColor("#991B1B");
                        absent++;
                    } else {
                        boolean wasLate = attendance.getDelayedCalc() != null &&
                                attendance.getDelayedCalc().compareTo(BigDecimal.ZERO) > 0;
                        day.setStatus(wasLate ? "متأخر" : "حاضر");
                        day.setColor(wasLate ? "#FED7AA" : "#D1FAE5");
                        day.setTextColor(wasLate ? "#9A3412" : "#065F46");
                        day.setEntryTime(attendance.getEntryTime());
                        day.setExitTime(attendance.getExitTime());
                        day.setWorkingHours(attendance.getWorkingHours());
                        day.setOvertimeHours(attendance.getOvertimeCalc());
                        day.setIsLate(wasLate);

                        present++;
                        if (wasLate) {
                            late++;
                            if (attendance.getDelayedCalc() != null) {
                                totalLateHours = totalLateHours.add(attendance.getDelayedCalc());
                            }
                        }
                        if (attendance.getOvertimeCalc() != null) {
                            totalOvertimeHours = totalOvertimeHours.add(attendance.getOvertimeCalc());
                        }
                    }
                } else {
                    // No attendance record - check if it's a future date
                    if (currentDate.isAfter(LocalDate.now())) {
                        day.setStatus("Future");
                        day.setColor("#FFFFFF");
                        day.setTextColor("#9CA3AF");
                    } else {
                        // Check if employee had a project assignment on this date
                        final LocalDate finalCurrentDate = currentDate;
                        boolean hasProject = assignments.stream()
                                .anyMatch(a -> !finalCurrentDate.isBefore(a.getStartDate()) &&
                                        (a.getEndDate() == null || !finalCurrentDate.isAfter(a.getEndDate())));

                        if (hasProject) {
                            day.setStatus("غائب");
                            day.setColor("#FEE2E2");
                            day.setTextColor("#991B1B");
                            absent++;
                        } else {
                            day.setStatus("لا يوجد مشروع");
                            day.setColor("#F3F4F6"); // Gray background (similar to weekend but distinct context)
                            day.setTextColor("#4B5563"); // Gray text
                            // Do not increment absent count
                        }
                    }
                }
            }

            days.add(day);
            currentDate = currentDate.plusDays(1);
        }

        return TimesheetResponse.builder()
                .employeeNo(employeeNo)
                .employeeName(employee.getEmployeeName())
                .month(month)
                .totalDays(days.size())
                .present(present)
                .absent(absent)
                .onLeave(onLeave)
                .late(late)
                .weekends(weekends)
                .totalOvertimeHours(totalOvertimeHours)
                .totalLateHours(totalLateHours)
                .days(days)
                .build();
    }

    /**
     * Delete attendance record (soft delete by marking as inactive).
     * Requires ADMIN role.
     *
     * @param transactionId Transaction ID
     */
    @Transactional
    public void deleteAttendance(Long transactionId) {
        log.info("Deleting attendance transaction {}", transactionId);

        AttendanceTransaction attendance = findAttendanceOrThrow(transactionId);
        attendanceRepository.delete(attendance);

        log.info("Attendance deleted successfully");
    }

    // ==================== Helper Methods ====================

    private Employee findEmployeeOrThrow(Long employeeNo) {
        return employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود برقم: " + employeeNo));
    }

    private Project findProjectOrThrow(Long projectCode) {
        return projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("المشروع غير موجود برقم: " + projectCode));
    }

    private AttendanceTransaction findAttendanceOrThrow(Long transactionId) {
        return attendanceRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("سجل الحضور غير موجود برقم: " + transactionId));
    }

    private void validateEmployeeActive(Employee employee) {
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new BadRequestException("حساب الموظف الخاص بك غير نشط. يرجى الاتصال بالموارد البشرية.");
        }
    }

    private void validateGPSLocation(java.math.BigDecimal empLat, java.math.BigDecimal empLon,
            java.math.BigDecimal projLat, java.math.BigDecimal projLon,
            Integer radiusMeters, String projectName) {

        if (projLat == null || projLon == null) {
            throw new BadRequestException("التحقق من GPS مطلوب، لكن إحداثيات GPS للمشروع غير مُعدة.");
        }

        if (!GPSCalculator.isValidCoordinates(empLat, empLon)) {
            throw new BadRequestException("إحداثيات GPS المقدمة غير صالحة.");
        }

        boolean withinRadius = GPSCalculator.isWithinRadius(empLat, empLon, projLat, projLon, radiusMeters);
        if (!withinRadius) {
            Double distance = GPSCalculator.calculateDistance(empLat, empLon, projLat, projLon);
            String formattedDistance = GPSCalculator.formatDistance(distance);

            throw new BadRequestException(
                    String.format("فشل التحقق من GPS. أنت على بعد %s من %s (المسموح: %d م)",
                            formattedDistance, projectName, radiusMeters));
        }
    }

    private CheckInResponse buildCheckInResponse(AttendanceTransaction attendance,
            Employee employee, Project project) {
        TimeSchedule schedule = calculationService.findApplicableSchedule(
                employee.getPrimaryDeptCode(), project.getProjectCode());

        Integer minutesLate = 0;
        Boolean withinGracePeriod = true;
        if (schedule != null) {
            withinGracePeriod = calculationService.isWithinGracePeriod(attendance, schedule);
            minutesLate = calculationService.calculateMinutesLate(attendance, schedule);
        }

        return CheckInResponse.builder()
                .transactionId(attendance.getTransactionId())
                .employeeNo(employee.getEmployeeNo())
                .employeeName(employee.getEmployeeName())
                .attendanceDate(attendance.getAttendanceDate())
                .projectCode(project.getProjectCode())
                .projectNameAr(project.getProjectName())
                .projectNameEn(project.getProjectName())
                .entryTime(attendance.getEntryTime())
                .entryLatitude(attendance.getEntryLatitude())
                .entryLongitude(attendance.getEntryLongitude())
                .entryDistanceMeters(attendance.getEntryDistanceMeters())
                .scheduledStartTime(schedule != null ? schedule.getScheduledStartTime().toString() : null)
                .withinGracePeriod(withinGracePeriod)
                .minutesLate(minutesLate)
                .message("تم تسجيل الدخول بنجاح!")
                .build();
    }

    private CheckOutResponse buildCheckOutResponse(AttendanceTransaction attendance,
            Employee employee, Project project) {
        String summary = String.format("تم العمل لمدة %s ساعة%s",
                attendance.getWorkingHours(),
                attendance.getOvertimeCalc().compareTo(java.math.BigDecimal.ZERO) > 0
                        ? "، " + attendance.getOvertimeCalc() + " ساعة إضافية"
                        : "");

        return CheckOutResponse.builder()
                .transactionId(attendance.getTransactionId())
                .employeeNo(employee.getEmployeeNo())
                .employeeName(employee.getEmployeeName())
                .attendanceDate(attendance.getAttendanceDate())
                .projectCode(project.getProjectCode())
                .projectNameAr(project.getProjectName())
                .projectNameEn(project.getProjectName())
                .entryTime(attendance.getEntryTime())
                .exitTime(attendance.getExitTime())
                .exitLatitude(attendance.getExitLatitude())
                .exitLongitude(attendance.getExitLongitude())
                .exitDistanceMeters(attendance.getExitDistanceMeters())
                .workingHours(attendance.getWorkingHours())
                .scheduledHours(attendance.getScheduledHours())
                .overtimeCalc(attendance.getOvertimeCalc())
                .delayedCalc(attendance.getDelayedCalc())
                .earlyOutCalc(attendance.getEarlyOutCalc())
                .shortageHours(attendance.getShortageHours())
                .isHolidayWork("Y".equals(attendance.getIsHolidayWork()))
                .isWeekendWork("Y".equals(attendance.getIsWeekendWork()))
                .message("تم تسجيل الخروج بنجاح!")
                .summary(summary)
                .build();
    }

    private AttendanceTransaction buildManualAttendance(ManualAttendanceRequest request, Employee employee) {
        return AttendanceTransaction.builder()
                .employeeNo(request.getEmployeeNo())
                .attendanceDate(request.getAttendanceDate())
                .projectCode(request.getProjectCode())
                .entryTime(request.getEntryTime())
                .entryLatitude(request.getEntryLatitude())
                .entryLongitude(request.getEntryLongitude())
                .exitTime(request.getExitTime())
                .exitLatitude(request.getExitLatitude())
                .exitLongitude(request.getExitLongitude())
                .workingHours(request.getWorkingHours())
                .overtimeCalc(request.getOvertimeCalc())
                .delayedCalc(request.getDelayedCalc())
                .earlyOutCalc(request.getEarlyOutCalc())
                .shortageHours(request.getShortageHours())
                .absenceFlag(request.getAbsenceFlag() != null ? request.getAbsenceFlag() : "N")
                .absenceReason(request.getAbsenceReason())
                .isAutoCheckout("N")
                .isManualEntry("Y")
                .notes(request.getNotes())
                .build();
    }

    private void updateAttendanceFromRequest(AttendanceTransaction attendance, ManualAttendanceRequest request) {
        if (request.getEntryTime() != null)
            attendance.setEntryTime(request.getEntryTime());
        if (request.getExitTime() != null)
            attendance.setExitTime(request.getExitTime());
        if (request.getWorkingHours() != null)
            attendance.setWorkingHours(request.getWorkingHours());
        if (request.getOvertimeCalc() != null)
            attendance.setOvertimeCalc(request.getOvertimeCalc());
        if (request.getAbsenceFlag() != null)
            attendance.setAbsenceFlag(request.getAbsenceFlag());
        if (request.getAbsenceReason() != null)
            attendance.setAbsenceReason(request.getAbsenceReason());
        if (request.getNotes() != null)
            attendance.setNotes(request.getNotes());
    }

    /**
     * Recalculate scheduledHours for an attendance record based on current schedule assignment.
     * This method always uses the current schedule configuration, ensuring accuracy
     * even if schedules were assigned after the attendance record was created.
     * 
     * Priority for project lookup (if attendance.projectCode is null):
     * 1. Employee's primary project code
     * 2. Active labor assignment for the attendance date
     * 
     * Priority for schedule lookup:
     * Project schedule > Department schedule > Default schedule
     * 
     * @param attendance The attendance transaction
     * @param employee The employee record
     * @return Recalculated scheduledHours value (never null)
     */
    private BigDecimal recalculateScheduledHours(AttendanceTransaction attendance, Employee employee) {
        // Priority 1: Use projectCode from attendance record (most accurate for that specific day)
        Long projectCode = attendance.getProjectCode();
        
        // If attendance record has no projectCode, try to find it from employee's assignments
        if (projectCode == null && employee != null) {
            // Priority 2: Try employee's primary project code
            if (employee.getPrimaryProjectCode() != null) {
                projectCode = employee.getPrimaryProjectCode();
                log.debug("Using primary project code {} for transaction {} (employee {})",
                        projectCode, attendance.getTransactionId(), employee.getEmployeeNo());
            } else {
                // Priority 3: Check for active labor assignment for the attendance date
                try {
                    List<com.techno.backend.entity.ProjectLaborAssignment> activeAssignments = 
                            assignmentRepository.findActiveAssignmentsByEmployee(employee.getEmployeeNo());
                    
                    LocalDate attendanceDate = attendance.getAttendanceDate();
                    for (com.techno.backend.entity.ProjectLaborAssignment assignment : activeAssignments) {
                        if ((assignment.getStartDate().isBefore(attendanceDate) || assignment.getStartDate().isEqual(attendanceDate)) &&
                                (assignment.getEndDate() == null || assignment.getEndDate().isAfter(attendanceDate)
                                        || assignment.getEndDate().isEqual(attendanceDate))) {
                            projectCode = assignment.getProjectCode();
                            log.debug("Found active assignment: project {} for transaction {} (employee {}, date {})",
                                    projectCode, attendance.getTransactionId(), employee.getEmployeeNo(), attendanceDate);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error finding labor assignment for transaction {}: {}", 
                            attendance.getTransactionId(), e.getMessage());
                }
            }
        }
        
        Long departmentCode = employee != null ? employee.getPrimaryDeptCode() : null;
        
        log.info("Recalculating scheduledHours for transaction {}: projectCode={}, departmentCode={}, employeeNo={}",
                attendance.getTransactionId(), projectCode, departmentCode, 
                employee != null ? employee.getEmployeeNo() : null);
        
        try {
            // Find applicable schedule using the same priority logic as during check-in
            TimeSchedule schedule = calculationService.findApplicableSchedule(departmentCode, projectCode);
            
            if (schedule != null && schedule.getRequiredHours() != null) {
                BigDecimal calculatedHours = schedule.getRequiredHours();
                log.info("Recalculated scheduledHours for transaction {}: {} -> {} (from schedule: ID={}, Name={}, Project={}, Dept={}, RequiredHours={})",
                        attendance.getTransactionId(), attendance.getScheduledHours(), calculatedHours,
                        schedule.getScheduleId(), schedule.getScheduleName(),
                        schedule.getProjectCode(), schedule.getDepartmentCode(), schedule.getRequiredHours());
                return calculatedHours;
            } else {
                // No schedule found - use default 8-hour schedule
                log.warn("No schedule found for transaction {} (Project: {}, Dept: {}). Using default 8.00 hours. " +
                        "Please check if a schedule with project_code={} exists and is active in the time_schedule table.",
                        attendance.getTransactionId(), projectCode, departmentCode, projectCode);
                return new BigDecimal("8.00");
            }
        } catch (Exception e) {
            log.warn("Error recalculating scheduledHours for transaction {} (Project: {}, Dept: {}): {}. Using stored value: {}",
                    attendance.getTransactionId(), projectCode, departmentCode, e.getMessage(), 
                    attendance.getScheduledHours());
            // Fall back to stored value on error, or default 8.00 if stored value is also null
            if (attendance.getScheduledHours() != null) {
                return attendance.getScheduledHours();
            }
            return new BigDecimal("8.00");
        }
    }

    private AttendanceResponse mapToResponse(AttendanceTransaction attendance, Employee employee, Project project) {
        // If project is null but we can find it from employee assignments, look it up
        Long projectCode = attendance.getProjectCode();
        if (project == null && projectCode == null && employee != null) {
            // Try to find project code using same logic as recalculateScheduledHours
            if (employee.getPrimaryProjectCode() != null) {
                projectCode = employee.getPrimaryProjectCode();
            } else {
                // Check for active labor assignment for the attendance date
                try {
                    List<com.techno.backend.entity.ProjectLaborAssignment> activeAssignments = 
                            assignmentRepository.findActiveAssignmentsByEmployee(employee.getEmployeeNo());
                    
                    LocalDate attendanceDate = attendance.getAttendanceDate();
                    for (com.techno.backend.entity.ProjectLaborAssignment assignment : activeAssignments) {
                        if ((assignment.getStartDate().isBefore(attendanceDate) || assignment.getStartDate().isEqual(attendanceDate)) &&
                                (assignment.getEndDate() == null || assignment.getEndDate().isAfter(attendanceDate)
                                        || assignment.getEndDate().isEqual(attendanceDate))) {
                            projectCode = assignment.getProjectCode();
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error finding project for response mapping: {}", e.getMessage());
                }
            }
            
            // If we found a projectCode, fetch the project entity
            if (projectCode != null) {
                project = projectRepository.findById(projectCode).orElse(null);
            }
        }
        
        // Always recalculate scheduledHours based on current schedule assignment
        // This ensures old records show the correct schedule even if schedule was assigned later
        BigDecimal recalculatedScheduledHours = recalculateScheduledHours(attendance, employee);
        
        // Recalculate shortageHours based on recalculated scheduledHours
        // Shortage = Scheduled Hours - Working Hours
        BigDecimal recalculatedShortageHours = null;
        if (attendance.getWorkingHours() != null && recalculatedScheduledHours != null) {
            recalculatedShortageHours = AttendanceCalculator.calculateShortageHours(
                    attendance.getWorkingHours(),
                    recalculatedScheduledHours
            );
        }
        
        // Use found projectCode if attendance record has null (for display purposes)
        Long responseProjectCode = attendance.getProjectCode() != null ? attendance.getProjectCode() : projectCode;
        
        return AttendanceResponse.builder()
                .transactionId(attendance.getTransactionId())
                .employeeNo(employee.getEmployeeNo())
                .employeeName(employee.getEmployeeName())
                .employeeName(employee.getEmployeeName())
                .attendanceDate(attendance.getAttendanceDate())
                .dayOfWeek(attendance.getAttendanceDate().getDayOfWeek().getValue())
                .dayName(attendance.getAttendanceDate().getDayOfWeek().name())
                .projectCode(responseProjectCode)
                .projectNameAr(project != null ? project.getProjectName() : null)
                .projectNameEn(project != null ? project.getProjectName() : null)
                .entryTime(attendance.getEntryTime())
                .entryLatitude(attendance.getEntryLatitude())
                .entryLongitude(attendance.getEntryLongitude())
                .entryDistanceMeters(attendance.getEntryDistanceMeters())
                .exitTime(attendance.getExitTime())
                .exitLatitude(attendance.getExitLatitude())
                .exitLongitude(attendance.getExitLongitude())
                .exitDistanceMeters(attendance.getExitDistanceMeters())
                .scheduledHours(recalculatedScheduledHours) // Use recalculated value
                .workingHours(attendance.getWorkingHours())
                .overtimeCalc(attendance.getOvertimeCalc())
                .delayedCalc(attendance.getDelayedCalc())
                .earlyOutCalc(attendance.getEarlyOutCalc())
                .shortageHours(recalculatedShortageHours != null ? recalculatedShortageHours : attendance.getShortageHours()) // Use recalculated value
                .absenceFlag(attendance.getAbsenceFlag())
                .absenceReason(attendance.getAbsenceReason())
                .isAutoCheckout(attendance.getIsAutoCheckout())
                .isHolidayWork(attendance.getIsHolidayWork())
                .isWeekendWork(attendance.getIsWeekendWork())
                .isManualEntry(attendance.getIsManualEntry())
                .notes(attendance.getNotes())
                .createdDate(attendance.getCreatedDate())
                .createdBy(attendance.getCreatedBy() != null ? attendance.getCreatedBy().toString() : null)
                .modifiedDate(attendance.getModifiedDate())
                .modifiedBy(attendance.getModifiedBy() != null ? attendance.getModifiedBy().toString() : null)
                .build();
    }
}
