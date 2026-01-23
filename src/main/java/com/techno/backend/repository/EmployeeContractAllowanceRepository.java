package com.techno.backend.repository;

import com.techno.backend.entity.EmployeeContractAllowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EmployeeContractAllowance entity
 * Provides database access methods for employee contract allowance management
 */
@Repository
public interface EmployeeContractAllowanceRepository extends JpaRepository<EmployeeContractAllowance, Long> {

    /**
     * Find all active allowances ordered by created date descending
     */
    List<EmployeeContractAllowance> findAllByIsActiveOrderByCreatedDateDesc(Character isActive);

    /**
     * Find all allowances ordered by created date descending (both active and inactive)
     */
    List<EmployeeContractAllowance> findAllByOrderByCreatedDateDesc();

    /**
     * Find allowances by employee number, ordered by created date descending
     */
    List<EmployeeContractAllowance> findByEmployeeNoOrderByCreatedDateDesc(Long employeeNo);

    /**
     * Find active allowances by employee number, ordered by created date descending
     */
    List<EmployeeContractAllowance> findByEmployeeNoAndIsActiveOrderByCreatedDateDesc(Long employeeNo, Character isActive);

    /**
     * Find allowance by employee number and transaction type code
     */
    Optional<EmployeeContractAllowance> findByEmployeeNoAndTransTypeCode(Long employeeNo, Long transTypeCode);

    /**
     * Check if allowance exists for employee and transaction type
     */
    boolean existsByEmployeeNoAndTransTypeCode(Long employeeNo, Long transTypeCode);

    /**
     * Check if allowance exists for employee and transaction type, excluding a specific record (for updates)
     */
    boolean existsByEmployeeNoAndTransTypeCodeAndRecordIdNot(Long employeeNo, Long transTypeCode, Long recordId);
}

