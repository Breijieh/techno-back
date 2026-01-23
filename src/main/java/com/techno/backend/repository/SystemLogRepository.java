package com.techno.backend.repository;

import com.techno.backend.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SystemLog entity
 * Provides database access methods for system log management
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    /**
     * Find all logs ordered by created date descending (newest first)
     */
    Page<SystemLog> findAllByOrderByCreatedDateDesc(Pageable pageable);

    /**
     * Find logs by log level, ordered by created date descending
     */
    Page<SystemLog> findByLogLevelOrderByCreatedDateDesc(String level, Pageable pageable);

    /**
     * Find logs by module, ordered by created date descending
     */
    Page<SystemLog> findByModuleOrderByCreatedDateDesc(String module, Pageable pageable);

    /**
     * Find logs by user ID, ordered by created date descending
     */
    Page<SystemLog> findByUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);

    /**
     * Find logs by action type, ordered by created date descending
     */
    Page<SystemLog> findByActionTypeOrderByCreatedDateDesc(String actionType, Pageable pageable);

    /**
     * Find logs by date range, ordered by created date descending
     */
    Page<SystemLog> findByCreatedDateBetweenOrderByCreatedDateDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find logs with multiple filters (custom query)
     */
    @Query("SELECT sl FROM SystemLog sl WHERE " +
           "(:level IS NULL OR sl.logLevel = :level) AND " +
           "(:module IS NULL OR sl.module = :module) AND " +
           "(:actionType IS NULL OR sl.actionType = :actionType) AND " +
           "(:fromDate IS NULL OR sl.createdDate >= :fromDate) AND " +
           "(:toDate IS NULL OR sl.createdDate <= :toDate) " +
           "ORDER BY sl.createdDate DESC")
    Page<SystemLog> findByFilters(
            @Param("level") String level,
            @Param("module") String module,
            @Param("actionType") String actionType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}

