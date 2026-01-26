package com.techno.backend.service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.chrono.IsoChronology;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Abstract base class for all report generation services.
 *
 * Provides common functionality for:
 * - Date and currency formatting
 * - Report metadata management
 * - Header and data validation
 * - Common utility methods
 *
 * Concrete implementations:
 * - ExcelReportService: Generates Excel (.xlsx) reports using Apache POI
 * - PdfReportService: Generates PDF reports using iText 7
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Slf4j
public abstract class BaseReportService {

    /**
     * Date formatters for consistent date rendering across all reports.
     * MONTH_YEAR_FORMATTER uses Egyptian Arabic locale (ar_EG) to ensure Gregorian
     * calendar.
     */
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    protected static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")
            .withLocale(new Locale("ar", "EG")) // Egypt uses Gregorian calendar
            .withChronology(IsoChronology.INSTANCE); // Force Gregorian/ISO calendar

    /**
     * Currency formatter for Saudi Riyal (SAR) with proper thousand separators.
     * Example: 12,345.67
     */
    protected static final DecimalFormat CURRENCY_FORMATTER = createCurrencyFormatter();

    /**
     * Number formatter for quantities and counts.
     * Example: 1,234
     */
    protected static final DecimalFormat NUMBER_FORMATTER = createNumberFormatter();

    /**
     * Generate report in the format implemented by the concrete service.
     *
     * @param title    Report title
     * @param headers  Column headers
     * @param data     Report data rows
     * @param metadata Additional report metadata (filters, date range, etc.)
     * @return Byte array of the generated report file
     */
    public abstract byte[] generateReport(String title, List<String> headers,
            List<List<Object>> data, Map<String, Object> metadata);

    /**
     * Get the file extension for this report format.
     *
     * @return File extension (e.g., "xlsx", "pdf")
     */
    public abstract String getFileExtension();

    /**
     * Get the MIME type for this report format.
     *
     * @return MIME type (e.g.,
     *         "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
     */
    public abstract String getMimeType();

    /**
     * Format a LocalDate for display in reports.
     *
     * @param date Date to format
     * @return Formatted date string (dd/MM/yyyy) or empty string if null
     */
    protected String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Format a LocalDateTime for display in reports.
     *
     * @param dateTime DateTime to format
     * @return Formatted datetime string (dd/MM/yyyy HH:mm) or empty string if null
     */
    protected String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * Format a BigDecimal as currency (SAR).
     *
     * @param amount Amount to format
     * @return Formatted currency string with thousand separators or empty string if
     *         null
     */
    protected String formatCurrency(BigDecimal amount) {
        return amount != null ? CURRENCY_FORMATTER.format(amount) : "";
    }

    /**
     * Format an integer/long as a number with thousand separators.
     *
     * @param number Number to format
     * @return Formatted number string or empty string if null
     */
    protected String formatNumber(Long number) {
        return number != null ? NUMBER_FORMATTER.format(number) : "";
    }

    /**
     * Format an integer as a number with thousand separators.
     *
     * @param number Number to format
     * @return Formatted number string or empty string if null
     */
    protected String formatNumber(Integer number) {
        return number != null ? NUMBER_FORMATTER.format(number) : "";
    }

    /**
     * Convert object to string safely.
     * Handles nulls and applies appropriate formatting based on type.
     *
     * @param value Value to convert
     * @return String representation of value or empty string if null
     */
    protected String toString(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof LocalDate) {
            return formatDate((LocalDate) value);
        }

        if (value instanceof LocalDateTime) {
            return formatDateTime((LocalDateTime) value);
        }

        if (value instanceof BigDecimal) {
            return formatCurrency((BigDecimal) value);
        }

        if (value instanceof Long) {
            return formatNumber((Long) value);
        }

        if (value instanceof Integer) {
            return formatNumber((Integer) value);
        }

        return value.toString();
    }

    /**
     * Validate report parameters.
     *
     * @param title   Report title
     * @param headers Column headers
     * @param data    Report data
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateReportParameters(String title, List<String> headers, List<List<Object>> data) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("عنوان التقرير لا يمكن أن يكون فارغاً");
        }

        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("رؤوس التقرير لا يمكن أن تكون فارغة");
        }

        if (data == null) {
            throw new IllegalArgumentException("بيانات التقرير لا يمكن أن تكون فارغة");
        }

        // Validate data rows match header count
        int headerCount = headers.size();
        for (int i = 0; i < data.size(); i++) {
            List<Object> row = data.get(i);
            if (row.size() != headerCount) {
                log.warn("Row {} has {} columns but expected {} (headers count)",
                        i, row.size(), headerCount);
            }
        }
    }

    /**
     * Create currency formatter with SAR locale.
     */
    private static DecimalFormat createCurrencyFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return formatter;
    }

    /**
     * Create number formatter with thousand separators.
     */
    private static DecimalFormat createNumberFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        return new DecimalFormat("#,##0", symbols);
    }

    /**
     * Get metadata value as string.
     *
     * @param metadata Metadata map
     * @param key      Metadata key
     * @return String value or empty string if not found
     */
    protected String getMetadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return "";
        }

        Object value = metadata.get(key);
        return toString(value);
    }

    /**
     * Build report filename with timestamp.
     *
     * @param reportName Base report name
     * @return Filename with extension (e.g., "Payroll_Report_20250530_143022.xlsx")
     */
    protected String buildFilename(String reportName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedName = reportName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format("%s_%s.%s", sanitizedName, timestamp, getFileExtension());
    }
}
