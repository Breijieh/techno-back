package com.techno.backend.repository;

import com.techno.backend.entity.ProjectDuePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for ProjectDuePayment entity.
 * Provides database access methods for project payment schedules.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectDuePaymentRepository extends JpaRepository<ProjectDuePayment, Long> {

    /**
     * Find all payments for a specific project
     */
    List<ProjectDuePayment> findByProjectCodeOrderBySequenceNoAsc(Long projectCode);

    /**
     * Find all pending payments for a project
     */
    @Query("SELECT p FROM ProjectDuePayment p WHERE " +
           "p.projectCode = :projectCode AND " +
           "p.paymentStatus = 'PENDING' " +
           "ORDER BY p.dueDate ASC")
    List<ProjectDuePayment> findPendingPaymentsByProject(@Param("projectCode") Long projectCode);

    /**
     * Find all overdue payments (past due date and not fully paid)
     */
    @Query("SELECT p FROM ProjectDuePayment p WHERE " +
           "p.dueDate < :currentDate AND " +
           "p.paymentStatus IN ('PENDING', 'PARTIAL') " +
           "ORDER BY p.dueDate ASC")
    List<ProjectDuePayment> findOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Find payments due within specified days
     */
    @Query("SELECT p FROM ProjectDuePayment p WHERE " +
           "p.dueDate BETWEEN :startDate AND :endDate AND " +
           "p.paymentStatus IN ('PENDING', 'PARTIAL') " +
           "ORDER BY p.dueDate ASC")
    List<ProjectDuePayment> findPaymentsDueWithinPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find specific payment by project and sequence
     */
    ProjectDuePayment findByProjectCodeAndSequenceNo(Long projectCode, Integer sequenceNo);

    /**
     * Check if payment exists for project and sequence
     */
    boolean existsByProjectCodeAndSequenceNo(Long projectCode, Integer sequenceNo);

    /**
     * Get count of unpaid payments for a project
     */
    @Query("SELECT COUNT(p) FROM ProjectDuePayment p WHERE " +
           "p.projectCode = :projectCode AND " +
           "p.paymentStatus IN ('PENDING', 'PARTIAL')")
    long countUnpaidPaymentsByProject(@Param("projectCode") Long projectCode);
}
