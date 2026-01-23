package com.techno.backend.repository;

import com.techno.backend.entity.Role;
import com.techno.backend.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for RolePermission entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    /**
     * Find permissions by role
     * @param role the role to search for
     * @return Optional containing the permissions if found
     */
    Optional<RolePermission> findByRole(Role role);
    
    /**
     * Find permissions by role ID
     * @param roleId the role ID to search for
     * @return Optional containing the permissions if found
     */
    Optional<RolePermission> findByRole_RoleId(Long roleId);
    
    /**
     * Delete permissions by role
     * @param role the role whose permissions should be deleted
     */
    void deleteByRole(Role role);
}

