package com.techno.backend.repository;

import com.techno.backend.entity.ProjectPaymentProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProjectPaymentProcess entity.
 * Provides database access methods for payment processing records.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Repository
public interface ProjectPaymentProcessRepository extends JpaRepository<ProjectPaymentProcess, Long> {

    /**
     * Find payment process by request number
     */
    Optional<ProjectPaymentProcess> findByRequestNo(Long requestNo);

    /**
     * Find all payment processes within date range
     */
    @Query("SELECT pp FROM ProjectPaymentProcess pp WHERE " +
           "pp.paymentDate BETWEEN :startDate AND :endDate AND " +
           "pp.isDeleted = 'N' " +
           "ORDER BY pp.paymentDate DESC")
    List<ProjectPaymentProcess> findByPaymentDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all payments by payment method
     */
    @Query("SELECT pp FROM ProjectPaymentProcess pp WHERE " +
           "pp.paymentMethod = :method AND " +
           "pp.isDeleted = 'N' " +
           "ORDER BY pp.paymentDate DESC")
    List<ProjectPaymentProcess> findByPaymentMethod(@Param("method") String method);

    /**
     * Find all payments processed by specific employee
     */
    @Query("SELECT pp FROM ProjectPaymentProcess pp WHERE " +
           "pp.processedBy = :employeeNo AND " +
           "pp.isDeleted = 'N' " +
           "ORDER BY pp.paymentDate DESC")
    List<ProjectPaymentProcess> findByProcessedBy(@Param("employeeNo") Long employeeNo);

    /**
     * Check if request has been processed
     */
    boolean existsByRequestNo(Long requestNo);
}
