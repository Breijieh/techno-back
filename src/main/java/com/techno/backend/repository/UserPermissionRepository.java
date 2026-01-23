package com.techno.backend.repository;

import com.techno.backend.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserPermission entity
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    
    /**
     * Find all permissions for a user
     */
    List<UserPermission> findByUserId(Long userId);
    
    /**
     * Find specific permission for user and menu
     */
    Optional<UserPermission> findByUserIdAndMenuId(Long userId, Long menuId);
    
    /**
     * Delete all permissions for a user
     */
    void deleteByUserId(Long userId);
}

