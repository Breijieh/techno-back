package com.techno.backend.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.time.LocalTime;

/**
 * Safe LocalTime Converter
 * Handles invalid time values in the database gracefully by returning null
 * instead of throwing DateTimeException
 */
@Converter(autoApply = false)
public class SafeLocalTimeConverter implements AttributeConverter<LocalTime, Time> {

    private static final Logger log = LoggerFactory.getLogger(SafeLocalTimeConverter.class);

    @Override
    public Time convertToDatabaseColumn(LocalTime localTime) {
        if (localTime == null) {
            return null;
        }
        return Time.valueOf(localTime);
    }

    @Override
    public LocalTime convertToEntityAttribute(Time time) {
        if (time == null) {
            return null;
        }
        try {
            return time.toLocalTime();
        } catch (Exception e) {
            log.warn("Invalid time value in database: {}. Returning null. Error: {}", time, e.getMessage());
            return null;
        }
    }
}
