package com.techno.backend.service;

import com.techno.backend.dto.HolidayRequest;
import com.techno.backend.dto.HolidayResponse;
import com.techno.backend.entity.Holiday;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Holiday Management.
 * Handles business logic for Saudi Arabia public holiday CRUD operations.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    /**
     * Get all active holidays for a specific year.
     *
     * @param year Year (e.g., 2025)
     * @return List of holidays
     */
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysByYear(Integer year) {
        log.info("Fetching holidays for year: {}", year);

        List<Holiday> holidays = holidayRepository.findActiveHolidaysByYear(year);
        return holidays.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all holidays within a date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of holidays
     */
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching holidays from {} to {}", startDate, endDate);

        List<Holiday> holidays = holidayRepository.findHolidaysByDateRange(startDate, endDate);
        return holidays.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get holiday by ID.
     *
     * @param holidayId Holiday ID
     * @return Holiday response
     */
    @Transactional(readOnly = true)
    public HolidayResponse getHolidayById(Long holidayId) {
        log.info("Fetching holiday by ID: {}", holidayId);

        Holiday holiday = findHolidayOrThrow(holidayId);
        return mapToResponse(holiday);
    }

    /**
     * Get upcoming holidays.
     *
     * @param limit Maximum number of holidays to return (default: 10)
     * @return List of upcoming holidays
     */
    @Transactional(readOnly = true)
    public List<HolidayResponse> getUpcomingHolidays(Integer limit) {
        log.info("Fetching upcoming {} holidays", limit);

        LocalDate today = LocalDate.now();
        int maxHolidays = (limit != null && limit > 0) ? limit : 10;

        List<Holiday> holidays = holidayRepository.findUpcomingHolidays(today, maxHolidays);
        return holidays.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific date is a holiday.
     *
     * @param date Date to check
     * @return true if the date is a holiday
     */
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        log.debug("Checking if {} is a holiday", date);
        return holidayRepository.isHoliday(date);
    }

    /**
     * Get holiday for a specific date.
     *
     * @param date Date to check
     * @return Holiday response or null if not a holiday
     */
    @Transactional(readOnly = true)
    public HolidayResponse getHolidayByDate(LocalDate date) {
        log.info("Fetching holiday for date: {}", date);

        Optional<Holiday> holiday = holidayRepository.findByHolidayDate(date);
        return holiday.map(this::mapToResponse).orElse(null);
    }

    /**
     * Create new holiday.
     *
     * @param request Holiday creation request
     * @return Created holiday
     */
    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {
        log.info("Creating new holiday: {} on {}", request.getHolidayName(), request.getHolidayDate());

        // Check if holiday already exists for this date
        Optional<Holiday> existing = holidayRepository.findByHolidayDate(request.getHolidayDate());
        if (existing.isPresent()) {
            throw new BadRequestException("Ø§Ù„Ø¹Ø·Ù„Ø© Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù‡Ø°Ø§ Ø§Ù„ØªØ§Ø±ÙŠØ®: " + request.getHolidayDate());
        }

        Holiday holiday = mapToEntity(request);
        holiday = holidayRepository.save(holiday);

        log.info("Holiday created successfully with ID: {}", holiday.getHolidayId());
        return mapToResponse(holiday);
    }

    /**
     * Update existing holiday.
     *
     * @param holidayId Holiday ID
     * @param request   Update request
     * @return Updated holiday
     */
    @Transactional
    public HolidayResponse updateHoliday(Long holidayId, HolidayRequest request) {
        log.info("Updating holiday ID: {}", holidayId);

        Holiday holiday = findHolidayOrThrow(holidayId);

        // Check if changing the date would conflict with another holiday
        if (!holiday.getHolidayDate().equals(request.getHolidayDate())) {
            Optional<Holiday> existing = holidayRepository.findByHolidayDate(request.getHolidayDate());
            if (existing.isPresent() && !existing.get().getHolidayId().equals(holidayId)) {
                throw new BadRequestException("Ø¹Ø·Ù„Ø© Ø£Ø®Ø±Ù‰ Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø§Ù„ÙØ¹Ù„ Ù„Ù‡Ø°Ø§ Ø§Ù„ØªØ§Ø±ÙŠØ®: " + request.getHolidayDate());
            }
        }

        updateHolidayFromRequest(holiday, request);
        holiday = holidayRepository.save(holiday);

        log.info("Holiday updated successfully");
        return mapToResponse(holiday);
    }

    /**
     * Delete holiday.
     *
     * @param holidayId Holiday ID
     */
    @Transactional
    public void deleteHoliday(Long holidayId) {
        log.info("Deleting holiday ID: {}", holidayId);

        Holiday holiday = findHolidayOrThrow(holidayId);
        holidayRepository.delete(holiday);

        log.info("Holiday deleted successfully");
    }

    // ==================== Helper Methods ====================

    private Holiday findHolidayOrThrow(Long holidayId) {
        return holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ø¹Ø·Ù„Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + holidayId));
    }

    private Holiday mapToEntity(HolidayRequest request) {
        return Holiday.builder()
                .holidayDate(request.getHolidayDate())
                .holidayName(request.getHolidayName())
                .holidayName(request.getHolidayName())
                .holidayYear(request.getHolidayYear())
                .isRecurring(request.getIsRecurring())
                .isActive(request.getIsActive() != null ? request.getIsActive() : "Y")
                .isPaid(request.getIsPaid() != null ? request.getIsPaid() : "Y")
                .build();
    }

    private void updateHolidayFromRequest(Holiday holiday, HolidayRequest request) {
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setHolidayName(request.getHolidayName());
        holiday.setHolidayName(request.getHolidayName());
        holiday.setHolidayYear(request.getHolidayYear());
        holiday.setIsRecurring(request.getIsRecurring());
        if (request.getIsActive() != null) {
            holiday.setIsActive(request.getIsActive());
        }
        if (request.getIsPaid() != null) {
            holiday.setIsPaid(request.getIsPaid());
        }
    }

    private HolidayResponse mapToResponse(Holiday holiday) {
        return HolidayResponse.builder()
                .holidayId(holiday.getHolidayId())
                .holidayDate(holiday.getHolidayDate())
                .holidayName(holiday.getHolidayName())
                .holidayName(holiday.getHolidayName())
                .holidayYear(holiday.getHolidayYear())
                .isRecurring(holiday.getIsRecurring())
                .isActive(holiday.getIsActive())
                .isPaid(holiday.getIsPaid())
                .dayOfWeek(holiday.getHolidayDate().getDayOfWeek().getValue())
                .dayName(holiday.getHolidayDate().getDayOfWeek().name())
                .createdDate(holiday.getCreatedDate())
                .createdBy(holiday.getCreatedBy() != null ? holiday.getCreatedBy().toString() : null)
                .modifiedDate(holiday.getModifiedDate())
                .modifiedBy(holiday.getModifiedBy() != null ? holiday.getModifiedBy().toString() : null)
                .build();
    }
}

