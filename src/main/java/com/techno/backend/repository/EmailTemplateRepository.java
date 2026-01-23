package com.techno.backend.repository;

import com.techno.backend.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for EmailTemplate entity.
 *
 * Provides database operations for email template management:
 * - Find templates by code
 * - Query templates by category
 * - List active templates
 * - Template CRUD operations
 *
 * Templates are identified by unique template codes that match
 * NotificationEventType constants.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    /**
     * Find template by unique template code
     * Used when sending notifications
     *
     * @param templateCode Template code (e.g., LEAVE_SUBMITTED)
     * @return Optional email template
     */
    Optional<EmailTemplate> findByTemplateCode(String templateCode);

    /**
     * Find active template by code
     * Only active templates are used for sending emails
     *
     * @param templateCode Template code
     * @return Optional active template
     */
    @Query("SELECT e FROM EmailTemplate e WHERE e.templateCode = :templateCode AND e.isActive = 'Y'")
    Optional<EmailTemplate> findActiveTemplateByCode(@Param("templateCode") String templateCode);

    /**
     * Find all templates in a specific category
     *
     * @param category Template category (LEAVE, LOAN, PAYROLL, etc.)
     * @return List of templates in category
     */
    List<EmailTemplate> findByTemplateCategory(String category);

    /**
     * Find all active templates in a specific category
     *
     * @param category Template category
     * @return List of active templates
     */
    @Query("SELECT e FROM EmailTemplate e WHERE e.templateCategory = :category AND e.isActive = 'Y'")
    List<EmailTemplate> findActiveTemplatesByCategory(@Param("category") String category);

    /**
     * Find all active templates
     *
     * @return List of all active templates
     */
    @Query("SELECT e FROM EmailTemplate e WHERE e.isActive = 'Y' ORDER BY e.templateCategory, e.templateCode")
    List<EmailTemplate> findAllActiveTemplates();

    /**
     * Find all templates ordered by category and code
     *
     * @return List of all templates
     */
    @Query("SELECT e FROM EmailTemplate e ORDER BY e.templateCategory, e.templateCode")
    List<EmailTemplate> findAllOrderedTemplates();

    /**
     * Check if a template exists by code
     *
     * @param templateCode Template code
     * @return true if template exists
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * Check if an active template exists by code
     *
     * @param templateCode Template code
     * @return true if active template exists
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM EmailTemplate e WHERE e.templateCode = :templateCode AND e.isActive = 'Y'")
    boolean existsActiveTemplateByCode(@Param("templateCode") String templateCode);

    /**
     * Count templates by category
     *
     * @param category Template category
     * @return Count of templates in category
     */
    long countByTemplateCategory(String category);

    /**
     * Count all active templates
     *
     * @return Count of active templates
     */
    @Query("SELECT COUNT(e) FROM EmailTemplate e WHERE e.isActive = 'Y'")
    long countActiveTemplates();

    /**
     * Get distinct categories
     *
     * @return List of unique categories
     */
    @Query("SELECT DISTINCT e.templateCategory FROM EmailTemplate e ORDER BY e.templateCategory")
    List<String> findDistinctCategories();
}
