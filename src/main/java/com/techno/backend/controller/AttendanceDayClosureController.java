package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.AttendanceDayClosure;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.service.AttendanceDayClosureService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for managing attendance day closures.
 *
 * Provides endpoints for:
 * - Listing attendance day closures
 * - Closing attendance days
 * - Reopening closed attendance days
 *
 * Access Control:
 * - All endpoints require ADMIN or HR_MANAGER role
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Slf4j
@RestController
@RequestMapping("/attendance-closure")
@RequiredArgsConstructor
public class AttendanceDayClosureController {

    private final AttendanceDayClosureService closureService;
    private final EmployeeRepository employeeRepository;

    /**
     * Get all attendance day closures with optional filters.
     *
     * @param startDate Start date (optional) - filters by attendanceDate
     * @param endDate End date (optional) - filters by attendanceDate
     * @param isClosed Closure status (Y/N) - optional
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: attendanceDate)
     * @param sortDirection Sort direction (default: desc)
     * @return Page of closures with employee names
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AttendanceDayClosureDetailsResponse>>> getAllClosures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String isClosed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("GET /attendance-closure/list - startDate={}, endDate={}, isClosed={}",
                startDate, endDate, isClosed);

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AttendanceDayClosure> closurePage = closureService.getAllClosures(
                startDate, endDate, isClosed, pageable
        );

        // Convert to DTO with employee names
        Page<AttendanceDayClosureDetailsResponse> response = closurePage.map(this::toDetailsResponse);

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع إغلاقات الحضور بنجاح",
                response
        ));
    }

    /**
     * Get closure for a specific date.
     *
     * @param date Attendance date in ISO format (YYYY-MM-DD)
     * @return Closure details, or null if not found
     */
    @GetMapping("/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDayClosureDetailsResponse>> getClosureByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /attendance-closure/{}", date);

        AttendanceDayClosure closure = closureService.getClosureByDate(date);

        if (closure == null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "لم يتم العثور على إغلاق للتاريخ: " + date,
                    null
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "تم استرجاع الإغلاق بنجاح",
                toDetailsResponse(closure)
        ));
    }

    /**
     * Close attendance for a specific date.
     *
     * @param request Close request DTO
     * @return Created or updated closure details
     */
    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDayClosureDetailsResponse>> closeDay(
            @Valid @RequestBody CloseAttendanceDayRequest request) {
        log.info("POST /attendance-closure/close - Date: {}, Closed by: {}",
                request.attendanceDate, request.closedBy);

        Long closedBy = request.closedBy != null ? request.closedBy : getCurrentEmployeeNo();

        AttendanceDayClosure closure = closureService.closeDay(
                request.attendanceDate,
                closedBy,
                request.notes
        );

        return ResponseEntity.ok(ApiResponse.success(
                "تم إغلاق يوم الحضور بنجاح",
                toDetailsResponse(closure)
        ));
    }

    /**
     * Reopen attendance for a specific date.
     *
     * @param request Reopen request DTO
     * @return Updated closure details
     */
    @PostMapping("/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDayClosureDetailsResponse>> reopenDay(
            @Valid @RequestBody ReopenAttendanceDayRequest request) {
        log.info("POST /attendance-closure/reopen - Date: {}, Reopened by: {}",
                request.attendanceDate, request.reopenedBy);

        Long reopenedBy = request.reopenedBy != null ? request.reopenedBy : getCurrentEmployeeNo();

        AttendanceDayClosure closure = closureService.reopenDay(
                request.attendanceDate,
                reopenedBy,
                request.notes
        );

        return ResponseEntity.ok(ApiResponse.success(
                "تم إعادة فتح يوم الحضور بنجاح",
                toDetailsResponse(closure)
        ));
    }

    // ==================== Helper Methods ====================

    /**
     * Get current authenticated user's employee number.
     */
    private Long getCurrentEmployeeNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authentication found in security context");
            throw new com.techno.backend.exception.ResourceNotFoundException("المستخدم غير مصادق عليه");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse employee number from authentication: {}", authentication.getName());
            throw new com.techno.backend.exception.ResourceNotFoundException("رقم الموظف غير صالح في سياق المصادقة");
        }
    }

    /**
     * Converts an AttendanceDayClosure entity to its DTO representation.
     */
    private AttendanceDayClosureDetailsResponse toDetailsResponse(AttendanceDayClosure closure) {
        String closedByName = null;
        if (closure.getClosedBy() != null) {
            closedByName = employeeRepository.findById(closure.getClosedBy())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        String reopenedByName = null;
        if (closure.getReopenedBy() != null) {
            reopenedByName = employeeRepository.findById(closure.getReopenedBy())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        return new AttendanceDayClosureDetailsResponse(
                closure.getClosureId(),
                closure.getAttendanceDate(),
                closure.isClosed(),
                closure.getClosedBy(),
                closedByName,
                closure.getClosedDate(),
                closure.getReopenedBy(),
                reopenedByName,
                closure.getReopenedDate(),
                closure.getNotes()
        );
    }

    // ==================== Request/Response DTOs ====================

    public record CloseAttendanceDayRequest(
            @NotNull(message = "تاريخ الحضور مطلوب")
            LocalDate attendanceDate,

            Long closedBy,

            @Size(max = 1000, message = "الملاحظات لا يمكن أن تتجاوز 1000 حرف")
            String notes
    ) {}

    public record ReopenAttendanceDayRequest(
            @NotNull(message = "تاريخ الحضور مطلوب")
            LocalDate attendanceDate,

            Long reopenedBy,

            @Size(max = 1000, message = "الملاحظات لا يمكن أن تتجاوز 1000 حرف")
            String notes
    ) {}

    public record AttendanceDayClosureDetailsResponse(
            Long closureId,
            LocalDate attendanceDate,
            Boolean isClosed,
            Long closedBy,
            String closedByName,
            java.time.LocalDateTime closedDate,
            Long reopenedBy,
            String reopenedByName,
            java.time.LocalDateTime reopenedDate,
            String notes
    ) {}
}

