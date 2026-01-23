package com.techno.backend.repository;

import com.techno.backend.entity.ProjectPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ProjectPaymentRequest entity.
 * Provides database access methods for supplier payment requests.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectPaymentRequestRepository extends JpaRepository<ProjectPaymentRequest, Long> {

    /**
     * Find all payment requests for a specific project
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.projectCode = :projectCode AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.requestDate DESC")
    List<ProjectPaymentRequest> findByProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find all payment requests for a specific supplier
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.supplierCode = :supplierCode AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.requestDate DESC")
    List<ProjectPaymentRequest> findBySupplierCode(@Param("supplierCode") Long supplierCode);

    /**
     * Find pending payment requests for a specific approver
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.nextApproval = :approverId AND " +
           "pr.transStatus = 'P' AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.requestDate ASC")
    List<ProjectPaymentRequest> findPendingRequestsByApprover(@Param("approverId") Long approverId);

    /**
     * Find all approved but not processed requests
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.transStatus = 'A' AND " +
           "pr.isProcessed = 'N' AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.approvedDate ASC")
    List<ProjectPaymentRequest> findApprovedNotProcessed();

    /**
     * Find payment requests by status
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.transStatus = :status AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.requestDate DESC")
    List<ProjectPaymentRequest> findByStatus(@Param("status") String status);

    /**
     * Find payment requests by project and status
     */
    @Query("SELECT pr FROM ProjectPaymentRequest pr WHERE " +
           "pr.projectCode = :projectCode AND " +
           "pr.transStatus = :status AND " +
           "pr.isDeleted = 'N' " +
           "ORDER BY pr.requestDate DESC")
    List<ProjectPaymentRequest> findByProjectCodeAndStatus(
            @Param("projectCode") Long projectCode,
            @Param("status") String status);

    /**
     * Count pending requests for a project
     */
    @Query("SELECT COUNT(pr) FROM ProjectPaymentRequest pr WHERE " +
           "pr.projectCode = :projectCode AND " +
           "pr.transStatus = 'P' AND " +
           "pr.isDeleted = 'N'")
    long countPendingRequestsByProject(@Param("projectCode") Long projectCode);
}
