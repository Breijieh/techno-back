package com.techno.backend.repository;

import com.techno.backend.entity.AttendanceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AttendanceTransaction entity.
 * Provides database access methods for attendance management.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceTransaction, Long>, JpaSpecificationExecutor<AttendanceTransaction> {

    /**
     * Find attendance record by employee and date.
     * Used to check if employee already has an attendance record for the day.
     *
     * @param employeeNo Employee number
     * @param attendanceDate Date of attendance
     * @return Optional attendance record
     */
    Optional<AttendanceTransaction> findByEmployeeNoAndAttendanceDate(Long employeeNo, LocalDate attendanceDate);

    /**
     * Find all attendance records for an employee within a date range.
     * Used for monthly reports and employee attendance history.
     *
     * @param employeeNo Employee number
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination parameters
     * @return Page of attendance records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate DESC")
    Page<AttendanceTransaction> findByEmployeeNoAndDateRange(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find all attendance records for a specific date.
     * Used for daily attendance reports.
     *
     * @param attendanceDate Date of attendance
     * @return List of attendance records
     */
    List<AttendanceTransaction> findByAttendanceDate(LocalDate attendanceDate);

    /**
     * Find attendance records by project and date range.
     * Used for project-specific attendance reports.
     *
     * @param projectCode Project code
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination parameters
     * @return Page of attendance records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.projectCode = :projectCode AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate DESC, a.employeeNo ASC")
    Page<AttendanceTransaction> findByProjectCodeAndDateRange(
            @Param("projectCode") Long projectCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find employees who haven't checked out yet (incomplete records).
     * Used for automatic check-out processing at end of day.
     *
     * @param attendanceDate Date to check
     * @return List of incomplete attendance records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate = :attendanceDate AND " +
           "a.entryTime IS NOT NULL AND " +
           "a.exitTime IS NULL AND " +
           "a.absenceFlag = 'N' " +
           "ORDER BY a.employeeNo ASC")
    List<AttendanceTransaction> findIncompleteAttendanceByDate(@Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find all absent employees for a specific date.
     *
     * @param attendanceDate Date to check
     * @return List of absence records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate = :attendanceDate AND " +
           "a.absenceFlag = 'Y' " +
           "ORDER BY a.employeeNo ASC")
    List<AttendanceTransaction> findAbsencesByDate(@Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find holiday work records for a specific date.
     *
     * @param attendanceDate Date to check
     * @return List of holiday work records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate = :attendanceDate AND " +
           "a.isHolidayWork = 'Y' " +
           "ORDER BY a.employeeNo ASC")
    List<AttendanceTransaction> findHolidayWorkByDate(@Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find weekend work records for a specific date.
     *
     * @param attendanceDate Date to check
     * @return List of weekend work records
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate = :attendanceDate AND " +
           "a.isWeekendWork = 'Y' " +
           "ORDER BY a.employeeNo ASC")
    List<AttendanceTransaction> findWeekendWorkByDate(@Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find employees with late arrivals (delayed_calc > 0) for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records with delays
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.delayedCalc > 0 " +
           "ORDER BY a.attendanceDate DESC, a.delayedCalc DESC")
    List<AttendanceTransaction> findLateArrivalsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find employees with early departures (early_out_calc > 0) for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records with early departures
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.earlyOutCalc > 0 " +
           "ORDER BY a.attendanceDate DESC, a.earlyOutCalc DESC")
    List<AttendanceTransaction> findEarlyDeparturesByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count total attendance records for an employee in a date range.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Count of attendance records
     */
    @Query("SELECT COUNT(a) FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.absenceFlag = 'N'")
    Long countAttendanceDays(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count absences for an employee in a date range.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Count of absences
     */
    @Query("SELECT COUNT(a) FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.absenceFlag = 'Y'")
    Long countAbsenceDays(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total working hours for an employee in a date range.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Sum of working hours (nullable if no records)
     */
    @Query("SELECT COALESCE(SUM(a.workingHours), 0) FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.absenceFlag = 'N' AND " +
           "a.workingHours IS NOT NULL")
    Double sumWorkingHours(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total overtime hours for an employee in a date range.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Sum of overtime hours
     */
    @Query("SELECT COALESCE(SUM(a.overtimeCalc), 0) FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.overtimeCalc IS NOT NULL")
    Double sumOvertimeHours(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total delayed hours for an employee in a date range.
     *
     * @param employeeNo Employee number
     * @param startDate Start date
     * @param endDate End date
     * @return Sum of delayed hours
     */
    @Query("SELECT COALESCE(SUM(a.delayedCalc), 0) FROM AttendanceTransaction a WHERE " +
           "a.employeeNo = :employeeNo AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.delayedCalc IS NOT NULL")
    Double sumDelayedHours(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find manual attendance entries (created/modified by HR).
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of manual entries
     */
    @Query("SELECT a FROM AttendanceTransaction a WHERE " +
           "a.attendanceDate BETWEEN :startDate AND :endDate AND " +
           "a.isManualEntry = 'Y' " +
           "ORDER BY a.attendanceDate DESC")
    List<AttendanceTransaction> findManualEntriesByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if an employee has already checked in today.
     *
     * @param employeeNo Employee number
     * @param attendanceDate Date to check
     * @return true if employee has checked in
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AttendanceTransaction a " +
           "WHERE a.employeeNo = :employeeNo AND a.attendanceDate = :attendanceDate AND a.entryTime IS NOT NULL")
    boolean hasCheckedInToday(@Param("employeeNo") Long employeeNo, @Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Check if an employee has already checked out today.
     *
     * @param employeeNo Employee number
     * @param attendanceDate Date to check
     * @return true if employee has checked out
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AttendanceTransaction a " +
           "WHERE a.employeeNo = :employeeNo AND a.attendanceDate = :attendanceDate AND a.exitTime IS NOT NULL")
    boolean hasCheckedOutToday(@Param("employeeNo") Long employeeNo, @Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find all attendance records with optional filters.
     * Used for listing all attendance records with date range, employee, and project filters.
     *
     * @param startDate Start date (optional, inclusive)
     * @param endDate End date (optional, inclusive)
     * @param employeeNo Employee number (optional)
     * @param projectCode Project code (optional)
     * @param pageable Pagination parameters
     * @return Page of attendance records
     */
    @Query(value = "SELECT * FROM emp_attendance_transactions WHERE " +
           "(CAST(:startDate AS date) IS NULL OR attendance_date >= CAST(:startDate AS date)) AND " +
           "(CAST(:endDate AS date) IS NULL OR attendance_date <= CAST(:endDate AS date)) AND " +
           "(CAST(:employeeNo AS bigint) IS NULL OR employee_no = CAST(:employeeNo AS bigint)) AND " +
           "(CAST(:projectCode AS bigint) IS NULL OR project_code = CAST(:projectCode AS bigint))",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM emp_attendance_transactions WHERE " +
           "(CAST(:startDate AS date) IS NULL OR attendance_date >= CAST(:startDate AS date)) AND " +
           "(CAST(:endDate AS date) IS NULL OR attendance_date <= CAST(:endDate AS date)) AND " +
           "(CAST(:employeeNo AS bigint) IS NULL OR employee_no = CAST(:employeeNo AS bigint)) AND " +
           "(CAST(:projectCode AS bigint) IS NULL OR project_code = CAST(:projectCode AS bigint))")
    Page<AttendanceTransaction> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("employeeNo") Long employeeNo,
            @Param("projectCode") Long projectCode,
            Pageable pageable);
}
