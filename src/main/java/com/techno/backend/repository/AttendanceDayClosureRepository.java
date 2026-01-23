package com.techno.backend.repository;

import com.techno.backend.entity.AttendanceDayClosure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AttendanceDayClosure entity.
 * Handles database operations for attendance day closures.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface AttendanceDayClosureRepository extends JpaRepository<AttendanceDayClosure, Long> {

    /**
     * Find closure for a specific attendance date
     */
    Optional<AttendanceDayClosure> findByAttendanceDate(LocalDate attendanceDate);

    /**
     * Find all closures by status (closed/open)
     */
    List<AttendanceDayClosure> findByIsClosed(String isClosed);

    /**
     * Find all closure records with optional filters for date range and status.
     *
     * @param startDate Start date (inclusive, optional) - filters by attendanceDate
     * @param endDate End date (inclusive, optional) - filters by attendanceDate
     * @param isClosed Closure status (Y/N) - optional
     * @param pageable Pagination parameters
     * @return Page of closure records
     */
    @Query("SELECT c FROM AttendanceDayClosure c WHERE " +
           "(:startDate IS NULL OR c.attendanceDate >= :startDate) AND " +
           "(:endDate IS NULL OR c.attendanceDate <= :endDate) AND " +
           "(:isClosed IS NULL OR c.isClosed = :isClosed) " +
           "ORDER BY c.attendanceDate DESC")
    Page<AttendanceDayClosure> findAllWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("isClosed") String isClosed,
            Pageable pageable);

    /**
     * Find the most recent closed date.
     * Used for validation to prevent closing past dates if future dates are already closed.
     *
     * @return Most recent closed date, or null if no closures exist
     */
    @Query("SELECT MAX(c.attendanceDate) FROM AttendanceDayClosure c WHERE c.isClosed = 'Y'")
    LocalDate findLatestClosedDate();

    /**
     * Check if a date is closed
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM AttendanceDayClosure c " +
           "WHERE c.attendanceDate = :date AND c.isClosed = 'Y'")
    boolean isDateClosed(@Param("date") LocalDate date);
}

