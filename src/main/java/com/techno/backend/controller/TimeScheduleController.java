package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.TimeScheduleRequest;
import com.techno.backend.dto.TimeScheduleResponse;
import com.techno.backend.service.TimeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Time Schedule Management.
 * Provides endpoints for managing work time schedules.
 *
 * Base URL: /api/schedules
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Slf4j
public class TimeScheduleController {

        private final TimeScheduleService timeScheduleService;

        /**
         * Get all active time schedules.
         *
         * GET /api/schedules
         *
         * @return List of active schedules
         */
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<TimeScheduleResponse>>> getAllActiveSchedules() {
                log.info("GET /api/schedules - Fetching all active schedules");

                List<TimeScheduleResponse> response = timeScheduleService.getAllActiveSchedules();

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع الجداول النشطة بنجاح",
                                response));
        }

        /**
         * Get time schedule by ID.
         *
         * GET /api/schedules/{id}
         *
         * @param id Schedule ID
         * @return Schedule details
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<TimeScheduleResponse>> getScheduleById(@PathVariable Long id) {
                log.info("GET /api/schedules/{}", id);

                TimeScheduleResponse response = timeScheduleService.getScheduleById(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع الجدول بنجاح",
                                response));
        }

        /**
         * Get schedules by department code.
         *
         * GET /api/schedules/department/{departmentCode}
         *
         * @param departmentCode Department code
         * @return List of schedules for the department
         */
        @GetMapping("/department/{departmentCode}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<TimeScheduleResponse>>> getSchedulesByDepartment(
                        @PathVariable Long departmentCode) {
                log.info("GET /api/schedules/department/{}", departmentCode);

                List<TimeScheduleResponse> response = timeScheduleService.getSchedulesByDepartment(departmentCode);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع جداول القسم بنجاح",
                                response));
        }

        /**
         * Get schedules by project code.
         *
         * GET /api/schedules/project/{projectCode}
         *
         * @param projectCode Project code
         * @return List of schedules for the project
         */
        @GetMapping("/project/{projectCode}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<TimeScheduleResponse>>> getSchedulesByProject(
                        @PathVariable Long projectCode) {
                log.info("GET /api/schedules/project/{}", projectCode);

                List<TimeScheduleResponse> response = timeScheduleService.getSchedulesByProject(projectCode);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع جداول المشروع بنجاح",
                                response));
        }

        /**
         * Get default schedule (not tied to specific department or project).
         *
         * GET /api/schedules/default
         *
         * @return Default schedule or null
         */
        @GetMapping("/default")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<TimeScheduleResponse>> getDefaultSchedule() {
                log.info("GET /api/schedules/default");

                TimeScheduleResponse response = timeScheduleService.getDefaultSchedule();

                if (response == null) {
                        return ResponseEntity.ok(ApiResponse.success(
                                        "لم يتم تكوين جدول افتراضي",
                                        null));
                }

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع الجدول الافتراضي بنجاح",
                                response));
        }

        /**
         * Create new time schedule.
         *
         * POST /api/schedules
         *
         * @param request Schedule creation request
         * @return Created schedule
         */
        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
        public ResponseEntity<ApiResponse<TimeScheduleResponse>> createSchedule(
                        @Valid @RequestBody TimeScheduleRequest request) {
                log.info("POST /api/schedules - Creating schedule: {}", request.getScheduleName());

                TimeScheduleResponse response = timeScheduleService.createSchedule(request);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                "تم إنشاء الجدول بنجاح",
                                response));
        }

        /**
         * Update existing time schedule.
         *
         * PUT /api/schedules/{id}
         *
         * @param id      Schedule ID
         * @param request Update request
         * @return Updated schedule
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
        public ResponseEntity<ApiResponse<TimeScheduleResponse>> updateSchedule(
                        @PathVariable Long id,
                        @Valid @RequestBody TimeScheduleRequest request) {
                log.info("PUT /api/schedules/{} - Updating schedule", id);

                TimeScheduleResponse response = timeScheduleService.updateSchedule(id, request);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم تحديث الجدول بنجاح",
                                response));
        }

        /**
         * Delete time schedule.
         *
         * DELETE /api/schedules/{id}
         *
         * @param id Schedule ID
         * @return Success message
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id) {
                log.info("DELETE /api/schedules/{}", id);

                timeScheduleService.deleteSchedule(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم حذف الجدول بنجاح",
                                null));
        }
}
