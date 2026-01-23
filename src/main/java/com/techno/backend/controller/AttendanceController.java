package com.techno.backend.controller;

import com.techno.backend.dto.*;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.service.AttendanceService;
import com.techno.backend.service.UserService;
import com.techno.backend.entity.UserAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST Controller for Attendance Management.
 * Provides endpoints for employee check-in/out, attendance tracking, and
 * reporting.
 *
 * Base URL: /api/attendance (context path /api is configured in
 * application.properties)
 *
 * Features:
 * - GPS-based check-in/out
 * - Automatic hours calculation
 * - Manual attendance entry by HR
 * - Attendance reports and queries
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {

        private final AttendanceService attendanceService;
        private final UserService userService;

        /**
         * Maps entity field names (camelCase) to database column names (snake_case).
         * Used for native query sorting where Spring Data JPA needs database column
         * names.
         *
         * @param fieldName Entity field name in camelCase (e.g., "attendanceDate")
         * @return Database column name in snake_case (e.g., "attendance_date")
         */
        private String mapFieldToColumn(String fieldName) {
                // Map common fields explicitly
                Map<String, String> fieldMapping = Map.of(
                                "attendanceDate", "attendance_date",
                                "employeeNo", "employee_no",
                                "projectCode", "project_code",
                                "entryTime", "entry_time",
                                "exitTime", "exit_time",
                                "transactionId", "transaction_id");

                // Return mapped column name or convert camelCase to snake_case automatically
                return fieldMapping.getOrDefault(fieldName,
                                fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase());
        }

        /**
         * Employee checks in with GPS validation.
         *
         * POST /api/attendance/check-in
         *
         * @param request Check-in request with GPS coordinates
         * @return Check-in response with entry details
         */
        @PostMapping("/check-in")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
                        @Valid @RequestBody CheckInRequest request) {

                Long employeeNo = getCurrentEmployeeNo();
                log.info("POST /api/attendance/check-in - Employee {} checking in at project {}",
                                employeeNo, request.getProjectCode());

                CheckInResponse response = attendanceService.checkIn(employeeNo, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                "تم تسجيل الدخول بنجاح!",
                                response));
        }

        /**
         * Employee checks out with GPS validation and automatic calculations.
         *
         * POST /api/attendance/check-out
         *
         * @param request Check-out request with GPS coordinates
         * @return Check-out response with calculated hours
         */
        @PostMapping("/check-out")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<CheckOutResponse>> checkOut(
                        @Valid @RequestBody CheckOutRequest request) {

                Long employeeNo = getCurrentEmployeeNo();
                log.info("POST /api/attendance/check-out - Employee {} checking out", employeeNo);

                CheckOutResponse response = attendanceService.checkOut(employeeNo, request);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم تسجيل الخروج بنجاح!",
                                response));
        }

        @GetMapping("/today")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<AttendanceResponse>> getTodayAttendance(
                        @RequestParam Long employeeNo,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                log.info("GET /api/attendance/today - Employee: {}, Date: {}", employeeNo, date);

                // Security check for Employee role
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (!employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                AttendanceResponse response = attendanceService.getTodayAttendance(employeeNo, date);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع سجل الحضور بنجاح",
                                response));
        }

        /**
         * Get attendance record by transaction ID.
         *
         * GET /api/attendance/{id}
         *
         * @param id Transaction ID
         * @return Attendance details
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceById(@PathVariable Long id) {
                log.info("GET /api/attendance/{}", id);

                AttendanceResponse response = attendanceService.getAttendanceById(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع سجل الحضور بنجاح",
                                response));
        }

        /**
         * Get attendance records for an employee within a date range.
         *
         * GET
         * /api/attendance/employee/{employeeNo}?startDate=2025-01-01&endDate=2025-01-31
         *
         * @param employeeNo    Employee number
         * @param startDate     Start date (optional, defaults to first day of current
         *                      month)
         * @param endDate       End date (optional, defaults to last day of current
         *                      month)
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: attendanceDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of attendance records
         */
        @GetMapping("/employee/{employeeNo}")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getEmployeeAttendance(
                        @PathVariable Long employeeNo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "attendanceDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /api/attendance/employee/{} - from {} to {}", employeeNo, startDate, endDate);

                // Default to current month if dates not provided
                if (startDate == null) {
                        startDate = LocalDate.now().withDayOfMonth(1);
                }
                if (endDate == null) {
                        endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<AttendanceResponse> response = attendanceService.getEmployeeAttendance(
                                employeeNo, startDate, endDate, pageable);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع سجلات الحضور بنجاح",
                                response));
        }

        /**
         * Get my attendance records (for current logged-in employee).
         *
         * GET /api/attendance/my-attendance?startDate=2025-01-01&endDate=2025-01-31
         *
         * @param startDate     Start date (optional)
         * @param endDate       End date (optional)
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: attendanceDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of my attendance records
         */
        @GetMapping("/my-attendance")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getMyAttendance(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "attendanceDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                Long employeeNo = getCurrentEmployeeNo();
                log.info("GET /api/attendance/my-attendance - Employee {}", employeeNo);

                // Default to current month if dates not provided
                if (startDate == null) {
                        startDate = LocalDate.now().withDayOfMonth(1);
                }
                if (endDate == null) {
                        endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
                }

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<AttendanceResponse> response = attendanceService.getEmployeeAttendance(
                                employeeNo, startDate, endDate, pageable);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع سجلات حضورك بنجاح",
                                response));
        }

        /**
         * HR creates manual attendance entry.
         * Requires ADMIN or HR role.
         *
         * POST /api/attendance/manual
         *
         * @param request Manual attendance request
         * @return Created attendance record
         */
        @PostMapping("/manual")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
        public ResponseEntity<ApiResponse<AttendanceResponse>> createManualAttendance(
                        @Valid @RequestBody ManualAttendanceRequest request) {

                log.info("POST /api/attendance/manual - Creating manual attendance for employee {} on {}",
                                request.getEmployeeNo(), request.getAttendanceDate());

                AttendanceResponse response = attendanceService.createManualAttendance(request);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                "تم إنشاء سجل حضور يدوي بنجاح",
                                response));
        }

        /**
         * Update existing attendance record.
         * Requires ADMIN, HR_MANAGER, or PROJECT_MANAGER (for auto-checkout edits).
         *
         * PUT /api/attendance/{id}
         *
         * @param id      Transaction ID
         * @param request Update request
         * @return Updated attendance record
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'PROJECT_MANAGER')")
        public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
                        @PathVariable Long id,
                        @Valid @RequestBody ManualAttendanceRequest request) {

                log.info("PUT /api/attendance/{} - Updating attendance record", id);

                Long editorEmployeeNo = getCurrentEmployeeNo();
                AttendanceResponse response = attendanceService.updateAttendance(id, request, editorEmployeeNo);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم تحديث الحضور بنجاح",
                                response));
        }

        /**
         * Delete attendance record.
         * Requires ADMIN role.
         *
         * DELETE /api/attendance/{id}
         *
         * @param id Transaction ID
         * @return Success message
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Void>> deleteAttendance(@PathVariable Long id) {
                log.info("DELETE /api/attendance/{}", id);

                attendanceService.deleteAttendance(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم حذف الحضور بنجاح",
                                null));
        }

        /**
         * Get all attendance records with optional filters.
         * Used for listing all attendance records in the attendance tracking page.
         *
         * GET
         * /api/attendance/list?startDate=2025-01-01&endDate=2025-01-31&employeeNo=123&projectCode=456
         *
         * @param startDate     Start date (optional)
         * @param endDate       End date (optional)
         * @param employeeNo    Employee number (optional)
         * @param projectCode   Project code (optional)
         * @param page          Page number (default: 0)
         * @param size          Page size (default: 20)
         * @param sortBy        Sort field (default: attendanceDate)
         * @param sortDirection Sort direction (default: desc)
         * @return Page of attendance records
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getAllAttendance(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(required = false) Long employeeNo,
                        @RequestParam(required = false) Long projectCode,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "attendanceDate") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDirection) {

                log.info("GET /api/attendance/list - from {} to {}, employee: {}, project: {}",
                                startDate, endDate, employeeNo, projectCode);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own attendance
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (employeeNo == null || !employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                                log.info("Employee role detected, filtering to employee number: {}", employeeNo);
                        }
                }

                // Map entity field name to database column name for native query sorting
                String sortColumn = mapFieldToColumn(sortBy);

                Sort sort = sortDirection.equalsIgnoreCase("asc")
                                ? Sort.by(sortColumn).ascending()
                                : Sort.by(sortColumn).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<AttendanceResponse> response = attendanceService.getAllAttendance(
                                startDate, endDate, employeeNo, projectCode, pageable);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع سجلات الحضور بنجاح",
                                response));
        }

        /**
         * Get employee timesheet for a specific month.
         * Returns day-by-day attendance calendar with status for each day.
         *
         * GET /api/attendance/timesheet?employeeNo={id}&month={YYYY-MM}
         *
         * @param employeeNo Employee number
         * @param month      Month in YYYY-MM format
         * @return Timesheet response with calendar data
         */
        @GetMapping("/timesheet")
        @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<TimesheetResponse>> getEmployeeTimesheet(
                        @RequestParam Long employeeNo,
                        @RequestParam String month) {
                log.info("GET /api/attendance/timesheet - Employee: {}, Month: {}", employeeNo, month);

                // Apply role-based filtering for Employees
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        // Employees can only see their own timesheet
                        Long currentEmployeeNo = getCurrentEmployeeNo();
                        if (!employeeNo.equals(currentEmployeeNo)) {
                                employeeNo = currentEmployeeNo;
                        }
                }

                TimesheetResponse response = attendanceService.getEmployeeTimesheet(employeeNo, month);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع جدول الحضور بنجاح",
                                response));
        }

        @GetMapping("/daily-overview")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<DailyOverviewDto>> getDailyOverview(
                        @RequestParam(required = false) Long employeeNo) {

                Long targetEmployeeNo = employeeNo;
                if (targetEmployeeNo == null) {
                        targetEmployeeNo = getCurrentEmployeeNo();
                }

                // Security check for Employee role
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
                        targetEmployeeNo = getCurrentEmployeeNo();
                }

                log.info("GET /api/attendance/daily-overview - Employee: {}", targetEmployeeNo);

                DailyOverviewDto response = attendanceService.getDailyOverview(targetEmployeeNo);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع نظرة عامة لليوم بنجاح",
                                response));
        }

        // ==================== Helper Methods ====================

        /**
         * Extracts employee number from security context.
         * Uses SecurityContextHolder to get the current authenticated user.
         *
         * @return Employee number
         * @throws RuntimeException if authentication is missing or invalid
         */
        private Long getCurrentEmployeeNo() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                        log.error("No authentication found in security context");
                        throw new ResourceNotFoundException("المستخدم غير مصادق عليه");
                }

                // Check if principal is Long (set by JwtAuthenticationFilter)
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                        return (Long) principal;
                }

                // If principal is String, try to parse it (fallback) or look up via UserService
                if (principal instanceof String) {
                        String username = (String) principal;
                        // Try to parse as Long first (legacy behavior)
                        try {
                                return Long.parseLong(username);
                        } catch (NumberFormatException e) {
                                // Not a number, try to look up user
                                try {
                                        UserAccount user = userService.findByUsername(username);
                                        if (user.getEmployeeNo() != null) {
                                                return user.getEmployeeNo();
                                        }
                                } catch (Exception ex) {
                                        log.error("Failed to find user or employee link for username: {}", username);
                                }
                        }

                        log.warn("Failed to parse employee number or find user link for: {}. User not linked to employee.",
                                        username);
                        throw new ResourceNotFoundException("لم يتم العثور على سجل موظف مرتبط بالمستخدم الحالي");
                }

                throw new ResourceNotFoundException("نوع المصادقة غير مدعوم");
        }
}
