package com.techno.backend.repository;

import com.techno.backend.entity.EmployeeLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for EmployeeLeave entity.
 * Handles database operations for leave requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Leave Management
 */
@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {

       /**
        * Find all leaves for a specific employee
        */
       Page<EmployeeLeave> findByEmployeeNo(Long employeeNo, Pageable pageable);

       /**
        * Find all leaves for a specific employee ordered by request date
        */
       List<EmployeeLeave> findByEmployeeNoOrderByRequestDateDesc(Long employeeNo);

       /**
        * Find leaves by employee and status
        */
       Page<EmployeeLeave> findByEmployeeNoAndTransStatus(Long employeeNo, String transStatus, Pageable pageable);

       /**
        * Find all pending leaves (status = 'N')
        */
       @Query("SELECT l FROM EmployeeLeave l WHERE l.transStatus = 'N' ORDER BY l.requestDate ASC")
       List<EmployeeLeave> findAllPendingLeaves();

       /**
        * Find pending leaves for a specific approver
        */
       @Query("SELECT l FROM EmployeeLeave l WHERE l.nextApproval = :approverId AND l.transStatus = 'N' ORDER BY l.requestDate ASC")
       List<EmployeeLeave> findPendingLeavesByApprover(@Param("approverId") Long approverId);

       /**
        * Find leaves within date range
        */
       @Query("SELECT l FROM EmployeeLeave l WHERE " +
                     "l.employeeNo = :employeeNo AND " +
                     "((l.leaveFromDate BETWEEN :startDate AND :endDate) OR " +
                     "(l.leaveToDate BETWEEN :startDate AND :endDate) OR " +
                     "(l.leaveFromDate <= :startDate AND l.leaveToDate >= :endDate))")
       List<EmployeeLeave> findOverlappingLeaves(
                     @Param("employeeNo") Long employeeNo,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * Find approved leaves for an employee
        */
       @Query("SELECT l FROM EmployeeLeave l WHERE l.employeeNo = :employeeNo AND l.transStatus = 'A' ORDER BY l.leaveFromDate DESC")
       List<EmployeeLeave> findApprovedLeavesByEmployee(@Param("employeeNo") Long employeeNo);

       /**
        * Count total leave days for employee in current year
        */
       @Query("SELECT COALESCE(SUM(l.leaveDays), 0) FROM EmployeeLeave l WHERE " +
                     "l.employeeNo = :employeeNo AND " +
                     "l.transStatus = 'A' AND " +
                     "YEAR(l.leaveFromDate) = :year")
       Double sumApprovedLeaveDaysForYear(@Param("employeeNo") Long employeeNo, @Param("year") int year);

       /**
        * Find pending leaves older than specified date (for auto-approval)
        */
       @Query("SELECT l FROM EmployeeLeave l WHERE " +
                     "l.transStatus = 'N' AND " +
                     "l.requestDate <= :cutoffDate " +
                     "ORDER BY l.requestDate ASC")
       List<EmployeeLeave> findPendingLeavesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

       /**
        * Find all leave records with optional filters for status, employee, and date
        * range.
        *
        * @param transStatus Transaction status (N/A/R) - optional
        * @param employeeNo  Employee number - optional
        * @param startDate   Start date (inclusive, optional) - filters by
        *                    leaveFromDate
        * @param endDate     End date (inclusive, optional) - filters by leaveToDate
        * @param pageable    Pagination parameters
        * @return Page of leave records
        */
       /**
        * Find all leave records filtered to TECHNO contract employees only.
        */
       @Query("SELECT l FROM EmployeeLeave l " +
                     "JOIN Employee e ON l.employeeNo = e.employeeNo " +
                     "WHERE e.empContractType = 'TECHNO' AND " +
                     "(:transStatus IS NULL OR l.transStatus = :transStatus) AND " +
                     "(:employeeNo IS NULL OR l.employeeNo = :employeeNo) AND " +
                     "(:startDate IS NULL OR l.leaveFromDate >= :startDate) AND " +
                     "(:endDate IS NULL OR l.leaveToDate <= :endDate) " +
                     "ORDER BY l.requestDate DESC, l.leaveId DESC")
       Page<EmployeeLeave> findAllWithFilters(
                     @Param("transStatus") String transStatus,
                     @Param("employeeNo") Long employeeNo,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     Pageable pageable);
}
