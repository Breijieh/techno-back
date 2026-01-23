package com.techno.backend.repository;

import com.techno.backend.entity.ProjectTransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for ProjectTransferRequest entity.
 * Provides database access methods for employee transfer requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectTransferRequestRepository extends JpaRepository<ProjectTransferRequest, Long> {

    /**
     * Find all transfer requests for an employee
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.employeeNo = :employeeNo AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate DESC")
    List<ProjectTransferRequest> findByEmployeeNo(@Param("employeeNo") Long employeeNo);

    /**
     * Find transfers from a specific project
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.fromProjectCode = :projectCode AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate DESC")
    List<ProjectTransferRequest> findByFromProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find transfers to a specific project
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.toProjectCode = :projectCode AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate DESC")
    List<ProjectTransferRequest> findByToProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find pending transfer requests for a specific approver
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.nextApproval = :approverId AND " +
           "tr.transStatus = 'P' AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate ASC")
    List<ProjectTransferRequest> findPendingTransfersByApprover(@Param("approverId") Long approverId);

    /**
     * Find approved but not executed transfers
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.transStatus = 'A' AND " +
           "tr.isExecuted = 'N' AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate ASC")
    List<ProjectTransferRequest> findApprovedNotExecuted();

    /**
     * Find transfers by status
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.transStatus = :status AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate DESC")
    List<ProjectTransferRequest> findByStatus(@Param("status") String status);

    /**
     * Check if employee has pending transfer
     */
    @Query("SELECT COUNT(tr) > 0 FROM ProjectTransferRequest tr WHERE " +
           "tr.employeeNo = :employeeNo AND " +
           "tr.transStatus = 'P' AND " +
           "tr.isDeleted = 'N'")
    boolean hasPendingTransfer(@Param("employeeNo") Long employeeNo);

    /**
     * Find transfers between two projects
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.fromProjectCode = :fromProject AND " +
           "tr.toProjectCode = :toProject AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate DESC")
    List<ProjectTransferRequest> findTransfersBetweenProjects(
            @Param("fromProject") Long fromProject,
            @Param("toProject") Long toProject);

    /**
     * Find transfers by date range
     */
    @Query("SELECT tr FROM ProjectTransferRequest tr WHERE " +
           "tr.transferDate BETWEEN :startDate AND :endDate AND " +
           "tr.isDeleted = 'N' " +
           "ORDER BY tr.transferDate ASC")
    List<ProjectTransferRequest> findByTransferDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
