package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.HolidayRequest;
import com.techno.backend.dto.HolidayResponse;
import com.techno.backend.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Holiday Management.
 * Provides endpoints for managing Saudi Arabia public holidays.
 *
 * Base URL: /api/holidays
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
@Slf4j
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * Get all active holidays for a specific year.
     *
     * GET /api/holidays/year/{year}
     *
     * @param year Year (e.g., 2025)
     * @return List of holidays
     */
    @GetMapping("/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidaysByYear(@PathVariable Integer year) {
        log.info("GET /api/holidays/year/{}", year);

        List<HolidayResponse> response = holidayService.getHolidaysByYear(year);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¹Ø·Ù„Ø§Øª Ù„Ø¹Ø§Ù… %d Ø¨Ù†Ø¬Ø§Ø­", year),
                response
        ));
    }

    /**
     * Get all holidays within a date range.
     *
     * GET /api/holidays/range?startDate=2025-01-01&endDate=2025-12-31
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of holidays
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/holidays/range?startDate={}&endDate={}", startDate, endDate);

        List<HolidayResponse> response = holidayService.getHolidaysByDateRange(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¹Ø·Ù„Ø§Øª Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Get holiday by ID.
     *
     * GET /api/holidays/{id}
     *
     * @param id Holiday ID
     * @return Holiday details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<HolidayResponse>> getHolidayById(@PathVariable Long id) {
        log.info("GET /api/holidays/{}", id);

        HolidayResponse response = holidayService.getHolidayById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¹Ø·Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Get upcoming holidays.
     *
     * GET /api/holidays/upcoming?limit=10
     *
     * @param limit Maximum number of holidays to return (default: 10)
     * @return List of upcoming holidays
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getUpcomingHolidays(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("GET /api/holidays/upcoming?limit={}", limit);

        List<HolidayResponse> response = holidayService.getUpcomingHolidays(limit);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¹Ø·Ù„Ø§Øª Ø§Ù„Ù‚Ø§Ø¯Ù…Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Check if a specific date is a holiday.
     *
     * GET /api/holidays/check?date=2025-09-23
     *
     * @param date Date to check
     * @return Boolean indicating if the date is a holiday
     */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Boolean>> checkIfHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/holidays/check?date={}", date);

        boolean isHoliday = holidayService.isHoliday(date);

        return ResponseEntity.ok(ApiResponse.success(
                isHoliday ? "Ø§Ù„ØªØ§Ø±ÙŠØ® Ø¹Ø·Ù„Ø©" : "Ø§Ù„ØªØ§Ø±ÙŠØ® Ù„ÙŠØ³ Ø¹Ø·Ù„Ø©",
                isHoliday
        ));
    }

    /**
     * Get holiday for a specific date.
     *
     * GET /api/holidays/date/{date}
     *
     * @param date Date to check
     * @return Holiday details or null if not a holiday
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<HolidayResponse>> getHolidayByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/holidays/date/{}", date);

        HolidayResponse response = holidayService.getHolidayByDate(date);

        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø¹Ø·Ù„Ø© Ù„Ù‡Ø°Ø§ Ø§Ù„ØªØ§Ø±ÙŠØ®",
                    null
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø§Ø³ØªØ±Ø¬Ø§Ø¹ Ø§Ù„Ø¹Ø·Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Create new holiday.
     *
     * POST /api/holidays
     *
     * @param request Holiday creation request
     * @return Created holiday
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<HolidayResponse>> createHoliday(
            @Valid @RequestBody HolidayRequest request) {
        log.info("POST /api/holidays - Creating holiday: {}", request.getHolidayName());

        HolidayResponse response = holidayService.createHoliday(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ø§Ù„Ø¹Ø·Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Update existing holiday.
     *
     * PUT /api/holidays/{id}
     *
     * @param id Holiday ID
     * @param request Update request
     * @return Updated holiday
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<HolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request) {
        log.info("PUT /api/holidays/{} - Updating holiday", id);

        HolidayResponse response = holidayService.updateHoliday(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ø¹Ø·Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                response
        ));
    }

    /**
     * Delete holiday.
     *
     * DELETE /api/holidays/{id}
     *
     * @param id Holiday ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        log.info("DELETE /api/holidays/{}", id);

        holidayService.deleteHoliday(id);

        return ResponseEntity.ok(ApiResponse.success(
                "ØªÙ… Ø­Ø°Ù Ø§Ù„Ø¹Ø·Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­",
                null
        ));
    }
}

