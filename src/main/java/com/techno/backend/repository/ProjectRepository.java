package com.techno.backend.repository;

import com.techno.backend.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Project entity.
 * Provides database access methods for project management.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all active projects
     */
    @Query("SELECT p FROM Project p WHERE p.projectStatus = 'ACTIVE' ORDER BY p.projectCode ASC")
    List<Project> findAllActive();

    /**
     * Find all active projects (paginated)
     */
    @Query("SELECT p FROM Project p WHERE p.projectStatus = 'ACTIVE' ORDER BY p.projectCode ASC")
    Page<Project> findAllActive(Pageable pageable);

    /**
     * Find projects by status
     */
    @Query("SELECT p FROM Project p WHERE p.projectStatus = :status ORDER BY p.projectCode ASC")
    List<Project> findByProjectStatus(@Param("status") String status);

    /**
     * Find projects by status (paginated)
     */
    Page<Project> findByProjectStatus(String status, Pageable pageable);

    /**
     * Find ongoing projects (between start and end dates)
     */
    @Query("SELECT p FROM Project p WHERE " +
           "p.startDate <= :today AND p.endDate >= :today AND " +
           "p.projectStatus = 'ACTIVE' " +
           "ORDER BY p.projectCode ASC")
    List<Project> findOngoingProjects(@Param("today") LocalDate today);

    /**
     * Find projects ending soon (within specified days)
     */
    @Query("SELECT p FROM Project p WHERE " +
           "p.endDate BETWEEN :today AND :endDate AND " +
           "p.projectStatus = 'ACTIVE' " +
           "ORDER BY p.endDate ASC")
    List<Project> findProjectsEndingSoon(@Param("today") LocalDate today,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Find projects by manager
     */
    @Query("SELECT p FROM Project p WHERE p.projectMgr = :managerEmployeeNo ORDER BY p.projectCode ASC")
    List<Project> findByProjectManager(@Param("managerEmployeeNo") Long managerEmployeeNo);

    /**
     * Find projects by manager (paginated)
     */
    Page<Project> findByProjectMgr(Long managerEmployeeNo, Pageable pageable);

    /**
     * Search projects by name (Arabic or English)
     */
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.projectName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(p.projectName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(p.projectAddress) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<Project> searchByText(@Param("searchText") String searchText, Pageable pageable);

    /**
     * Find projects with GPS coordinates configured
     */
    @Query("SELECT p FROM Project p WHERE " +
           "p.projectLatitude IS NOT NULL AND p.projectLongitude IS NOT NULL " +
           "ORDER BY p.projectCode ASC")
    List<Project> findProjectsWithGps();

    /**
     * Find projects requiring GPS check-in
     */
    @Query("SELECT p FROM Project p WHERE " +
           "p.requireGpsCheck = 'Y' AND p.projectStatus = 'ACTIVE' " +
           "ORDER BY p.projectCode ASC")
    List<Project> findProjectsRequiringGps();

    /**
     * Count projects by status
     */
    Long countByProjectStatus(String status);

    /**
     * Count active projects
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.projectStatus = 'ACTIVE'")
    Long countActiveProjects();

    /**
     * Count ongoing projects
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE " +
           "p.startDate <= :today AND p.endDate >= :today AND " +
           "p.projectStatus = 'ACTIVE'")
    Long countOngoingProjects(@Param("today") LocalDate today);

    /**
     * Find project by Techno suffix code
     */
    Optional<Project> findByTechnoSuffix(String technoSuffix);

    /**
     * Check if Techno suffix exists
     */
    boolean existsByTechnoSuffix(String technoSuffix);
}
