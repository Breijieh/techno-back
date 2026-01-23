package com.techno.backend.repository;

import com.techno.backend.entity.RequestsApprovalSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RequestsApprovalSet entity.
 * Handles database operations for approval workflow configuration.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Approval System
 */
@Repository
public interface RequestsApprovalSetRepository extends JpaRepository<RequestsApprovalSet, Long> {

    /**
     * Find approval workflow for a specific request type
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE " +
           "a.requestType = :requestType AND " +
           "a.isActive = 'Y' " +
           "ORDER BY a.levelNo ASC")
    List<RequestsApprovalSet> findActiveApprovalFlowByRequestType(@Param("requestType") String requestType);

    /**
     * Find specific approval level for request type
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE " +
           "a.requestType = :requestType AND " +
           "a.levelNo = :levelNo AND " +
           "a.isActive = 'Y'")
    Optional<RequestsApprovalSet> findByRequestTypeAndLevel(
            @Param("requestType") String requestType,
            @Param("levelNo") Integer levelNo);

    /**
     * Find approval workflow for department-specific request type
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE " +
           "a.requestType = :requestType AND " +
           "(a.departmentCode = :departmentCode OR a.departmentCode IS NULL) AND " +
           "a.isActive = 'Y' " +
           "ORDER BY a.departmentCode DESC NULLS LAST, a.levelNo ASC")
    List<RequestsApprovalSet> findApprovalFlowByRequestTypeAndDepartment(
            @Param("requestType") String requestType,
            @Param("departmentCode") Long departmentCode);

    /**
     * Find approval workflow for project-specific request type
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE " +
           "a.requestType = :requestType AND " +
           "(a.projectCode = :projectCode OR a.projectCode IS NULL) AND " +
           "a.isActive = 'Y' " +
           "ORDER BY a.projectCode DESC NULLS LAST, a.levelNo ASC")
    List<RequestsApprovalSet> findApprovalFlowByRequestTypeAndProject(
            @Param("requestType") String requestType,
            @Param("projectCode") Long projectCode);

    /**
     * Find final approval level for request type
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE " +
           "a.requestType = :requestType AND " +
           "a.closeLevel = 'Y' AND " +
           "a.isActive = 'Y'")
    Optional<RequestsApprovalSet> findFinalApprovalLevel(@Param("requestType") String requestType);

    /**
     * Find all active approval configurations
     */
    @Query("SELECT a FROM RequestsApprovalSet a WHERE a.isActive = 'Y' ORDER BY a.requestType, a.levelNo")
    List<RequestsApprovalSet> findAllActiveApprovalConfigurations();

    /**
     * Check if request type exists
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM RequestsApprovalSet a WHERE a.requestType = :requestType AND a.isActive = 'Y'")
    boolean existsByRequestType(@Param("requestType") String requestType);
}
