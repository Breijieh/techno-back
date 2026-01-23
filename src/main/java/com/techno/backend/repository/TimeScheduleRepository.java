package com.techno.backend.repository;

import com.techno.backend.entity.TimeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TimeSchedule entity.
 * Provides database access methods for managing work time schedules.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Repository
public interface TimeScheduleRepository extends JpaRepository<TimeSchedule, Long> {

    /**
     * Find time schedule by department code.
     * Used to determine the work schedule for employees in a specific department.
     *
     * @param departmentCode Department code
     * @return Optional time schedule
     */
    Optional<TimeSchedule> findByDepartmentCodeAndIsActive(Long departmentCode, String isActive);

    /**
     * Find time schedule by project code.
     * Used to determine the work schedule for employees on a specific project.
     *
     * @param projectCode Project code
     * @return Optional time schedule
     */
    Optional<TimeSchedule> findByProjectCodeAndIsActive(Long projectCode, String isActive);

    /**
     * Find all active time schedules.
     *
     * @return List of active schedules
     */
    @Query("SELECT t FROM TimeSchedule t WHERE t.isActive = 'Y' ORDER BY t.scheduleName ASC")
    List<TimeSchedule> findAllActiveSchedules();

    /**
     * Find time schedule by department or project.
     * Project-specific schedule takes precedence over department schedule.
     *
     * @param departmentCode Department code (nullable)
     * @param projectCode Project code (nullable)
     * @return Optional time schedule
     */
    @Query("SELECT t FROM TimeSchedule t WHERE t.isActive = 'Y' AND " +
           "(t.projectCode = :projectCode OR (t.departmentCode = :departmentCode AND t.projectCode IS NULL)) " +
           "ORDER BY t.projectCode DESC LIMIT 1")
    Optional<TimeSchedule> findScheduleByDepartmentOrProject(
            @Param("departmentCode") Long departmentCode,
            @Param("projectCode") Long projectCode);

    /**
     * Find all schedules for a specific department (including general schedules).
     *
     * @param departmentCode Department code
     * @return List of schedules
     */
    @Query("SELECT t FROM TimeSchedule t WHERE " +
           "t.departmentCode = :departmentCode AND t.isActive = 'Y' " +
           "ORDER BY t.scheduleName ASC")
    List<TimeSchedule> findByDepartmentCode(@Param("departmentCode") Long departmentCode);

    /**
     * Find all schedules for a specific project.
     *
     * @param projectCode Project code
     * @return List of schedules
     */
    @Query("SELECT t FROM TimeSchedule t WHERE " +
           "t.projectCode = :projectCode AND t.isActive = 'Y' " +
           "ORDER BY t.scheduleName ASC")
    List<TimeSchedule> findByProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find default/general schedule (not tied to specific department or project).
     *
     * @return Optional default schedule
     */
    @Query("SELECT t FROM TimeSchedule t WHERE " +
           "t.departmentCode IS NULL AND t.projectCode IS NULL AND t.isActive = 'Y' " +
           "ORDER BY t.scheduleId ASC LIMIT 1")
    Optional<TimeSchedule> findDefaultSchedule();

    /**
     * Check if a schedule exists for a department.
     *
     * @param departmentCode Department code
     * @return true if schedule exists
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TimeSchedule t " +
           "WHERE t.departmentCode = :departmentCode AND t.isActive = 'Y'")
    boolean existsByDepartmentCode(@Param("departmentCode") Long departmentCode);

    /**
     * Check if a schedule exists for a project.
     *
     * @param projectCode Project code
     * @return true if schedule exists
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TimeSchedule t " +
           "WHERE t.projectCode = :projectCode AND t.isActive = 'Y'")
    boolean existsByProjectCode(@Param("projectCode") Long projectCode);
}
