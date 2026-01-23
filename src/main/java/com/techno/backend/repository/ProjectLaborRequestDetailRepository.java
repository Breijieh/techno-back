package com.techno.backend.repository;

import com.techno.backend.entity.ProjectLaborRequestDetail;
import com.techno.backend.entity.ProjectLaborRequestDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ProjectLaborRequestDetail entity.
 * Provides database access methods for labor request details.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectLaborRequestDetailRepository
        extends JpaRepository<ProjectLaborRequestDetail, ProjectLaborRequestDetailId> {

    /**
     * Find all details for a specific labor request
     */
    @Query("SELECT ld FROM ProjectLaborRequestDetail ld WHERE " +
           "ld.requestNo = :requestNo " +
           "ORDER BY ld.sequenceNo ASC")
    List<ProjectLaborRequestDetail> findByRequestNo(@Param("requestNo") Long requestNo);

    /**
     * Find unfulfilled detail lines (assigned < quantity)
     */
    @Query("SELECT ld FROM ProjectLaborRequestDetail ld WHERE " +
           "ld.requestNo = :requestNo AND " +
           "ld.assignedCount < ld.quantity " +
           "ORDER BY ld.sequenceNo ASC")
    List<ProjectLaborRequestDetail> findUnfulfilledDetails(@Param("requestNo") Long requestNo);

    /**
     * Find specific detail by request and sequence
     */
    @Query("SELECT ld FROM ProjectLaborRequestDetail ld WHERE " +
           "ld.requestNo = :requestNo AND " +
           "ld.sequenceNo = :sequenceNo")
    ProjectLaborRequestDetail findByRequestNoAndSequenceNo(
            @Param("requestNo") Long requestNo,
            @Param("sequenceNo") Integer sequenceNo);

    /**
     * Count total positions in a request
     */
    @Query("SELECT SUM(ld.quantity) FROM ProjectLaborRequestDetail ld WHERE " +
           "ld.requestNo = :requestNo")
    Integer sumQuantityByRequestNo(@Param("requestNo") Long requestNo);

    /**
     * Count total assigned workers in a request
     */
    @Query("SELECT SUM(ld.assignedCount) FROM ProjectLaborRequestDetail ld WHERE " +
           "ld.requestNo = :requestNo")
    Integer sumAssignedCountByRequestNo(@Param("requestNo") Long requestNo);

    /**
     * Delete all details for a specific labor request
     */
    @Modifying
    @Query("DELETE FROM ProjectLaborRequestDetail ld WHERE ld.id.requestNo = :requestNo")
    void deleteByRequestNo(@Param("requestNo") Long requestNo);
}
