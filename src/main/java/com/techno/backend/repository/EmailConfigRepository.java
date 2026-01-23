package com.techno.backend.repository;

import com.techno.backend.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EmailConfig entity.
 *
 * Provides database operations for email configuration management:
 * - Retrieve active configuration
 * - Update SMTP settings
 * - Test connection settings
 * - Enable/disable email sending
 *
 * Note: System supports only one active configuration at a time.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {

    /**
     * Find the currently active email configuration
     * Only one configuration should be active at a time
     *
     * @return Optional active email configuration
     */
    @Query("SELECT e FROM EmailConfig e WHERE e.isActive = 'Y' ORDER BY e.configId DESC")
    Optional<EmailConfig> findActiveConfig();

    /**
     * Find active configuration with email sending enabled
     * Used when sending emails
     *
     * @return Optional active and enabled configuration
     */
    @Query("SELECT e FROM EmailConfig e WHERE e.isActive = 'Y' AND e.sendEmailsEnabled = 'Y'")
    Optional<EmailConfig> findActiveAndEnabledConfig();

    /**
     * Check if email sending is globally enabled
     *
     * @return true if active config exists with email sending enabled
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM EmailConfig e WHERE e.isActive = 'Y' AND e.sendEmailsEnabled = 'Y'")
    boolean isEmailSendingEnabled();

    /**
     * Deactivate all configurations
     * Used before activating a new configuration
     *
     * @return Number of configurations deactivated
     */
    @Modifying
    @Query("UPDATE EmailConfig e SET e.isActive = 'N'")
    int deactivateAll();

    /**
     * Activate a specific configuration
     * Should be called after deactivateAll()
     *
     * @param configId Configuration ID to activate
     * @return Number of configurations activated (0 or 1)
     */
    @Modifying
    @Query("UPDATE EmailConfig e SET e.isActive = 'Y' WHERE e.configId = :configId")
    int activateConfig(@Param("configId") Long configId);

    /**
     * Enable email sending for active configuration
     *
     * @return Number of configurations updated
     */
    @Modifying
    @Query("UPDATE EmailConfig e SET e.sendEmailsEnabled = 'Y' WHERE e.isActive = 'Y'")
    int enableEmailSending();

    /**
     * Disable email sending for active configuration
     *
     * @return Number of configurations updated
     */
    @Modifying
    @Query("UPDATE EmailConfig e SET e.sendEmailsEnabled = 'N' WHERE e.isActive = 'Y'")
    int disableEmailSending();

    /**
     * Check if an active configuration exists
     *
     * @return true if at least one active configuration exists
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM EmailConfig e WHERE e.isActive = 'Y'")
    boolean hasActiveConfig();

    /**
     * Count total configurations
     *
     * @return Total count of configurations
     */
    @Query("SELECT COUNT(e) FROM EmailConfig e")
    long countConfigs();
}
