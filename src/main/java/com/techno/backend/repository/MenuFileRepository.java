package com.techno.backend.repository;

import com.techno.backend.entity.MenuFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MenuFile entity
 */
@Repository
public interface MenuFileRepository extends JpaRepository<MenuFile, Long> {
    
    /**
     * Find menu by code
     */
    Optional<MenuFile> findByMenuCode(String menuCode);
    
    /**
     * Find active menus
     */
    List<MenuFile> findByIsActive(Character isActive);
    
    /**
     * Find menus by parent
     */
    List<MenuFile> findByParentMenuIdAndIsActive(Long parentMenuId, Character isActive);
    
    /**
     * Find root menus (no parent)
     */
    @Query("SELECT m FROM MenuFile m WHERE m.parentMenuId IS NULL AND m.isActive = 'Y' ORDER BY m.menuOrder")
    List<MenuFile> findRootMenus();
}

