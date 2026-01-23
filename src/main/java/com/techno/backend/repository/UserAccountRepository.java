package com.techno.backend.repository;

import com.techno.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UserAccount entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<UserAccount> findByUsername(String username);
    
    /**
     * Find user by national ID
     * @param nationalId the national ID to search for
     * @return Optional containing the user if found
     */
    Optional<UserAccount> findByNationalId(String nationalId);
    
    /**
     * Find user by employee number
     * @param employeeNo the employee number to search for
     * @return Optional containing the user if found
     */
    Optional<UserAccount> findByEmployeeNo(Long employeeNo);
    
    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if national ID exists
     * @param nationalId the national ID to check
     * @return true if national ID exists
     */
    boolean existsByNationalId(String nationalId);
}

