package com.techno.backend.repository;

import com.techno.backend.entity.ManualAttendanceRequest;
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
 * Repository interface for ManualAttendanceRequest entity.
 * Handles database operations for manual attendance requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface ManualAttendanceRequestRepository extends JpaRepository<ManualAttendanceRequest, Long> {

    /**
     * Find all manual attendance requests for a specific employee
     */
    Page<ManualAttendanceRequest> findByEmployeeNo(Long employeeNo, Pageable pageable);

    /**
     * Find requests by employee and status
     */
    Page<ManualAttendanceRequest> findByEmployeeNoAndTransStatus(Long employeeNo, String transStatus, Pageable pageable);

    /**
     * Find all pending requests (status = 'N')
     */
    @Query("SELECT r FROM ManualAttendanceRequest r WHERE r.transStatus = 'N' ORDER BY r.requestDate ASC")
    List<ManualAttendanceRequest> findAllPendingRequests();

    /**
     * Find pending requests for a specific approver
     */
    @Query("SELECT r FROM ManualAttendanceRequest r WHERE r.nextApproval = :approverId AND r.transStatus = 'N' ORDER BY r.requestDate ASC")
    List<ManualAttendanceRequest> findPendingRequestsByApprover(@Param("approverId") Long approverId);

    /**
     * Find request by employee and attendance date (for duplicate checking)
     */
    Optional<ManualAttendanceRequest> findByEmployeeNoAndAttendanceDate(Long employeeNo, LocalDate attendanceDate);

    /**
     * Find non-rejected request by employee and attendance date
     * Used to check if employee already has a pending or approved request for the date
     * Rejected requests are excluded to allow resubmission
     */
    @Query("SELECT r FROM ManualAttendanceRequest r WHERE r.employeeNo = :employeeNo " +
           "AND r.attendanceDate = :attendanceDate AND r.transStatus != 'R'")
    Optional<ManualAttendanceRequest> findNonRejectedRequestByEmployeeAndDate(
            @Param("employeeNo") Long employeeNo,
            @Param("attendanceDate") LocalDate attendanceDate);

    /**
     * Find all manual attendance request records with optional filters for status, employee, and date range.
     *
     * @param transStatus Transaction status (N/A/R) - optional
     * @param employeeNo Employee number - optional
     * @param startDate Start date (inclusive, optional) - filters by attendanceDate
     * @param endDate End date (inclusive, optional) - filters by attendanceDate
     * @param pageable Pagination parameters
     * @return Page of manual attendance request records
     */
    @Query("SELECT r FROM ManualAttendanceRequest r WHERE " +
           "(:transStatus IS NULL OR r.transStatus = :transStatus) AND " +
           "(:employeeNo IS NULL OR r.employeeNo = :employeeNo) AND " +
           "(:startDate IS NULL OR r.attendanceDate >= :startDate) AND " +
           "(:endDate IS NULL OR r.attendanceDate <= :endDate) " +
           "ORDER BY r.requestDate DESC, r.requestId DESC")
    Page<ManualAttendanceRequest> findAllWithFilters(
            @Param("transStatus") String transStatus,
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}

