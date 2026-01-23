package com.techno.backend.service;

import com.techno.backend.entity.AttendanceDayClosure;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.AttendanceDayClosureRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for managing attendance day closures.
 *
 * Handles:
 * - Closing attendance days to prevent modifications
 * - Reopening closed days to allow modifications
 * - Validation of closure rules
 * - Querying closure status
 *
 * Business Rules:
 * - One closure per day (unique constraint on attendanceDate)
 * - Cannot close future dates
 * - Cannot close past dates if future dates are already closed (prevents gaps)
 * - Closed days prevent attendance record modifications
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceDayClosureService {

    private final AttendanceDayClosureRepository closureRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Get all attendance day closures with optional filters.
     *
     * @param startDate Start date (optional) - filters by attendanceDate
     * @param endDate End date (optional) - filters by attendanceDate
     * @param isClosed Closure status (Y/N) - optional
     * @param pageable Pagination parameters
     * @return Page of closures
     */
    @Transactional(readOnly = true)
    public Page<AttendanceDayClosure> getAllClosures(LocalDate startDate, LocalDate endDate,
                                                      String isClosed, Pageable pageable) {
        log.debug("Fetching all attendance closures with filters: startDate={}, endDate={}, isClosed={}",
                startDate, endDate, isClosed);

        return closureRepository.findAllWithFilters(startDate, endDate, isClosed, pageable);
    }

    /**
     * Get closure for a specific date.
     *
     * @param attendanceDate Attendance date
     * @return Closure record, or null if not found
     */
    @Transactional(readOnly = true)
    public AttendanceDayClosure getClosureByDate(LocalDate attendanceDate) {
        return closureRepository.findByAttendanceDate(attendanceDate)
                .orElse(null);
    }

    /**
     * Check if a date is closed.
     *
     * @param attendanceDate Attendance date to check
     * @return true if the date is closed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDateClosed(LocalDate attendanceDate) {
        return closureRepository.isDateClosed(attendanceDate);
    }

    /**
     * Close attendance for a specific date.
     *
     * Validates:
     * - Date is not in the future
     * - Date is not already closed
     * - No future dates are already closed (prevents gaps)
     *
     * @param attendanceDate Date to close
     * @param closedBy Employee number who is closing
     * @param notes Optional notes
     * @return Created or updated closure record
     */
    @Transactional
    public AttendanceDayClosure closeDay(LocalDate attendanceDate, Long closedBy, String notes) {
        log.info("Closing attendance day: {}, closed by: {}", attendanceDate, closedBy);

        // Validate date is not in the future
        if (attendanceDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("لا يمكن إغلاق التواريخ المستقبلية");
        }

        // Check if already closed
        Optional<AttendanceDayClosure> existing = closureRepository.findByAttendanceDate(attendanceDate);
        if (existing.isPresent() && existing.get().isClosed()) {
            throw new BadRequestException("يوم الحضور " + attendanceDate + " مغلق بالفعل");
        }

        // Validate no future dates are already closed (prevents gaps)
        LocalDate latestClosedDate = closureRepository.findLatestClosedDate();
        if (latestClosedDate != null && latestClosedDate.isAfter(attendanceDate)) {
            throw new BadRequestException(
                    "لا يمكن إغلاق التاريخ " + attendanceDate + " لأن التاريخ المستقبلي " + latestClosedDate +
                            " مغلق بالفعل. أغلاق التواريخ بشكل متسلسل.");
        }

        // Validate employee exists
        if (closedBy != null) {
            employeeRepository.findById(closedBy)
                    .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود: " + closedBy));
        }

        AttendanceDayClosure closure;
        if (existing.isPresent()) {
            // Update existing record
            closure = existing.get();
            closure.close(closedBy, notes);
        } else {
            // Create new record
            closure = AttendanceDayClosure.builder()
                    .attendanceDate(attendanceDate)
                    .build();
            closure.close(closedBy, notes);
        }

        closure = closureRepository.save(closure);
        log.info("Attendance day {} closed successfully by {}", attendanceDate, closedBy);

        return closure;
    }

    /**
     * Reopen attendance for a specific date.
     *
     * Validates:
     * - Closure exists for the date
     * - Date is currently closed
     *
     * @param attendanceDate Date to reopen
     * @param reopenedBy Employee number who is reopening
     * @param notes Optional notes
     * @return Updated closure record
     */
    @Transactional
    public AttendanceDayClosure reopenDay(LocalDate attendanceDate, Long reopenedBy, String notes) {
        log.info("Reopening attendance day: {}, reopened by: {}", attendanceDate, reopenedBy);

        // Find existing closure
        AttendanceDayClosure closure = closureRepository.findByAttendanceDate(attendanceDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "لم يتم العثور على سجل إغلاق للتاريخ: " + attendanceDate));

        // Validate it's currently closed
        if (!closure.isClosed()) {
            throw new BadRequestException("يوم الحضور " + attendanceDate + " غير مغلق");
        }

        // Validate employee exists
        if (reopenedBy != null) {
            employeeRepository.findById(reopenedBy)
                    .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود: " + reopenedBy));
        }

        // Reopen the day
        closure.reopen(reopenedBy, notes);
        closure = closureRepository.save(closure);

        log.info("Attendance day {} reopened successfully by {}", attendanceDate, reopenedBy);

        return closure;
    }
}

