package com.techno.backend.repository;

import com.techno.backend.entity.WeekendDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for WeekendDay entity.
 * Provides database access methods for managing weekend configuration.
 *
 * Saudi Arabia weekends: Friday (5) and Saturday (6) in ISO-8601 format.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface WeekendDayRepository extends JpaRepository<WeekendDay, Long> {

    /**
     * Find weekend day by day of week number (1-7, ISO-8601).
     *
     * @param dayOfWeek Day of week (1=Monday, 7=Sunday)
     * @return Optional weekend day record
     */
    Optional<WeekendDay> findByDayOfWeek(Integer dayOfWeek);

    /**
     * Find all active weekend days.
     * For Saudi Arabia, this should return Friday (5) and Saturday (6).
     *
     * @return List of active weekend days
     */
    @Query("SELECT w FROM WeekendDay w WHERE w.isActive = 'Y' ORDER BY w.dayOfWeek ASC")
    List<WeekendDay> findAllActiveWeekendDays();

    /**
     * Check if a specific day of week is a weekend.
     *
     * @param dayOfWeek Day of week (1=Monday, 7=Sunday)
     * @return true if the day is an active weekend day
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WeekendDay w " +
           "WHERE w.dayOfWeek = :dayOfWeek AND w.isActive = 'Y'")
    boolean isWeekendDay(@Param("dayOfWeek") Integer dayOfWeek);

    /**
     * Get all weekend day numbers (e.g., [5, 6] for Friday and Saturday).
     *
     * @return List of weekend day numbers
     */
    @Query("SELECT w.dayOfWeek FROM WeekendDay w WHERE w.isActive = 'Y' ORDER BY w.dayOfWeek ASC")
    List<Integer> findAllWeekendDayNumbers();
}
