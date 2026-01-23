package com.techno.backend.repository;

import com.techno.backend.entity.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ContractType entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface ContractTypeRepository extends JpaRepository<ContractType, String> {

    /**
     * Find contract types by active status
     * 
     * @param isActive the active status ('Y' or 'N')
     * @return List of contract types
     */
    List<ContractType> findByIsActive(Character isActive);

    /**
     * Check if contract type code exists
     * 
     * @param contractTypeCode the contract type code to check
     * @return true if contract type exists
     */
    boolean existsByContractTypeCode(String contractTypeCode);
}
