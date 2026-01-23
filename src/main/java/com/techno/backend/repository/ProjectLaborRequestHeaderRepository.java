package com.techno.backend.repository;

import com.techno.backend.entity.ProjectLaborRequestHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for ProjectLaborRequestHeader entity.
 * Provides database access methods for labor requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectLaborRequestHeaderRepository extends JpaRepository<ProjectLaborRequestHeader, Long> {

    /**
     * Find all labor requests for a specific project
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.projectCode = :projectCode AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate DESC")
    List<ProjectLaborRequestHeader> findByProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find labor requests by status
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.requestStatus = :status AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate DESC")
    List<ProjectLaborRequestHeader> findByStatus(@Param("status") String status);

    /**
     * Find open labor requests
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.requestStatus IN ('OPEN', 'PARTIAL') AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate ASC")
    List<ProjectLaborRequestHeader> findOpenRequests();

    /**
     * Find labor requests by requester
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.requestedBy = :employeeNo AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate DESC")
    List<ProjectLaborRequestHeader> findByRequestedBy(@Param("employeeNo") Long employeeNo);

    /**
     * Find labor requests by date range
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.requestDate BETWEEN :startDate AND :endDate AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate ASC")
    List<ProjectLaborRequestHeader> findByRequestDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find labor requests by project and status
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.projectCode = :projectCode AND " +
           "lr.requestStatus = :status AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.requestDate DESC")
    List<ProjectLaborRequestHeader> findByProjectCodeAndStatus(
            @Param("projectCode") Long projectCode,
            @Param("status") String status);

    /**
     * Count open requests for a project
     */
    @Query("SELECT COUNT(lr) FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.projectCode = :projectCode AND " +
           "lr.requestStatus IN ('OPEN', 'PARTIAL') AND " +
           "lr.isDeleted = 'N'")
    long countOpenRequestsByProject(@Param("projectCode") Long projectCode);

    /**
     * Find expired active requests (end date has passed but status is still OPEN or PARTIAL)
     */
    @Query("SELECT lr FROM ProjectLaborRequestHeader lr WHERE " +
           "lr.requestStatus IN ('OPEN', 'PARTIAL') AND " +
           "lr.endDate IS NOT NULL AND " +
           "lr.endDate < :currentDate AND " +
           "lr.isDeleted = 'N' " +
           "ORDER BY lr.endDate ASC")
    List<ProjectLaborRequestHeader> findExpiredActiveRequests(@Param("currentDate") LocalDate currentDate);
}
