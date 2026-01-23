package com.techno.backend.repository;

import com.techno.backend.entity.ProjectLaborAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for ProjectLaborAssignment entity.
 * Provides database access methods for labor assignments.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectLaborAssignmentRepository extends JpaRepository<ProjectLaborAssignment, Long> {

    /**
     * Find all assignments for a specific employee
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.employeeNo = :employeeNo AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate DESC")
    List<ProjectLaborAssignment> findByEmployeeNo(@Param("employeeNo") Long employeeNo);

    /**
     * Find all assignments for a specific project
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.projectCode = :projectCode AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate DESC")
    List<ProjectLaborAssignment> findByProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find active assignments for a project
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.projectCode = :projectCode AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate ASC")
    List<ProjectLaborAssignment> findActiveAssignmentsByProject(@Param("projectCode") Long projectCode);

    /**
     * Find active assignments for an employee
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.employeeNo = :employeeNo AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate ASC")
    List<ProjectLaborAssignment> findActiveAssignmentsByEmployee(@Param("employeeNo") Long employeeNo);

    /**
     * Find assignments for a labor request
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.requestNo = :requestNo AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate ASC")
    List<ProjectLaborAssignment> findByRequestNo(@Param("requestNo") Long requestNo);

    /**
     * Find assignments by status
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.assignmentStatus = :status AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate DESC")
    List<ProjectLaborAssignment> findByStatus(@Param("status") String status);

    /**
     * Find overlapping assignments for an employee (critical for validation)
     * Checks if there's any assignment that overlaps with the given date range
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.employeeNo = :employeeNo AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N' AND " +
           "la.startDate <= :endDate AND " +
           "(la.endDate IS NULL OR la.endDate >= :startDate)")
    List<ProjectLaborAssignment> findOverlappingAssignments(
            @Param("employeeNo") Long employeeNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if employee has any active assignments
     */
    @Query("SELECT COUNT(la) > 0 FROM ProjectLaborAssignment la WHERE " +
           "la.employeeNo = :employeeNo AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N'")
    boolean hasActiveAssignment(@Param("employeeNo") Long employeeNo);

    /**
     * Find assignments ending within date range
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.endDate BETWEEN :startDate AND :endDate AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.endDate ASC")
    List<ProjectLaborAssignment> findAssignmentsEndingBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count active assignments for a project
     */
    @Query("SELECT COUNT(la) FROM ProjectLaborAssignment la WHERE " +
           "la.projectCode = :projectCode AND " +
           "la.assignmentStatus = 'ACTIVE' AND " +
           "la.isDeleted = 'N'")
    long countActiveAssignmentsByProject(@Param("projectCode") Long projectCode);

    /**
     * Find assignments within date range
     */
    @Query("SELECT la FROM ProjectLaborAssignment la WHERE " +
           "la.startDate <= :endDate AND " +
           "(la.endDate IS NULL OR la.endDate >= :startDate) AND " +
           "la.isDeleted = 'N' " +
           "ORDER BY la.startDate ASC")
    List<ProjectLaborAssignment> findAssignmentsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
