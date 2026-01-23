package com.techno.backend.repository;

import com.techno.backend.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Holiday entity.
 * Provides database access methods for managing Saudi Arabia public holidays.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * Find holiday by specific date.
     * Used to check if a given date is a public holiday.
     *
     * @param holidayDate Date to check
     * @return Optional holiday record
     */
    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

    /**
     * Find all active holidays for a specific year.
     *
     * @param year Year (e.g., 2025)
     * @return List of holidays
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayYear = :year AND h.isActive = 'Y' ORDER BY h.holidayDate ASC")
    List<Holiday> findActiveHolidaysByYear(@Param("year") Integer year);

    /**
     * Find all holidays within a date range.
     * Used for calendar views and reports.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of holidays
     */
    @Query("SELECT h FROM Holiday h WHERE " +
           "h.holidayDate BETWEEN :startDate AND :endDate AND " +
           "h.isActive = 'Y' " +
           "ORDER BY h.holidayDate ASC")
    List<Holiday> findHolidaysByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if a specific date is a holiday.
     *
     * @param date Date to check
     * @return true if the date is an active holiday
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Holiday h " +
           "WHERE h.holidayDate = :date AND h.isActive = 'Y'")
    boolean isHoliday(@Param("date") LocalDate date);

    /**
     * Find recurring holidays.
     * Used for annual holiday planning.
     *
     * @return List of recurring holidays
     */
    @Query("SELECT h FROM Holiday h WHERE h.isRecurring = 'Y' AND h.isActive = 'Y' ORDER BY h.holidayDate ASC")
    List<Holiday> findRecurringHolidays();

    /**
     * Find non-recurring holidays (Eids based on Hijri calendar).
     *
     * @return List of non-recurring holidays
     */
    @Query("SELECT h FROM Holiday h WHERE h.isRecurring = 'N' AND h.isActive = 'Y' ORDER BY h.holidayDate ASC")
    List<Holiday> findNonRecurringHolidays();

    /**
     * Count holidays for a specific year.
     *
     * @param year Year
     * @return Count of active holidays
     */
    @Query("SELECT COUNT(h) FROM Holiday h WHERE h.holidayYear = :year AND h.isActive = 'Y'")
    Long countHolidaysByYear(@Param("year") Integer year);

    /**
     * Find upcoming holidays (after a specific date).
     *
     * @param fromDate Starting date
     * @param limit Maximum number of holidays to return
     * @return List of upcoming holidays
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate >= :fromDate AND h.isActive = 'Y' " +
           "ORDER BY h.holidayDate ASC LIMIT :limit")
    List<Holiday> findUpcomingHolidays(@Param("fromDate") LocalDate fromDate, @Param("limit") int limit);
}
