package com.techno.backend.repository;

import com.techno.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find role by role name
     * @param roleName the role name to search for
     * @return Optional containing the role if found
     */
    Optional<Role> findByRoleName(String roleName);
    
    /**
     * Check if role name exists
     * @param roleName the role name to check
     * @return true if role name exists
     */
    boolean existsByRoleName(String roleName);
    
    /**
     * Check if role name exists excluding a specific role ID
     * @param roleName the role name to check
     * @param roleId the role ID to exclude
     * @return true if role name exists for another role
     */
    boolean existsByRoleNameAndRoleIdNot(String roleName, Long roleId);
}

