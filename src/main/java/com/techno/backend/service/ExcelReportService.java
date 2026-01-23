package com.techno.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for generating Excel (.xlsx) reports using Apache POI.
 *
 * Features:
 * - Professional Excel formatting with styled headers
 * - Auto-sized columns for readability
 * - Metadata section for report filters and parameters
 * - Support for borders, fonts, and cell alignment
 * - Freeze panes for header row
 *
 * Usage:
 * ```java
 * ExcelReportService excelService = new ExcelReportService();
 * byte[] excelFile = excelService.generateReport(
 *     "Monthly Payroll Report",
 *     Arrays.asList("Employee", "Department", "Gross", "Net"),
 *     dataRows,
 *     metadata
 * );
 * ```
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
public class ExcelReportService extends BaseReportService {

    private static final String FILE_EXTENSION = "xlsx";
    private static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Generate Excel report with professional formatting.
     *
     * @param title Report title (displayed as merged cell at top)
     * @param headers Column headers
     * @param data Report data rows
     * @param metadata Report metadata (filters, date range, etc.)
     * @return Byte array of Excel file
     */
    @Override
    public byte[] generateReport(String title, List<String> headers,
                                 List<List<Object>> data, Map<String, Object> metadata) {
        log.info("Generating Excel report: {}", title);

        // Validate parameters
        validateReportParameters(title, headers, data);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("تقرير");

            // Create styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle metadataKeyStyle = createMetadataKeyStyle(workbook);
            CellStyle metadataValueStyle = createMetadataValueStyle(workbook);

            int currentRow = 0;

            // Add title (merged across all columns)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.size() - 1));

            // Add metadata section if provided
            if (metadata != null && !metadata.isEmpty()) {
                currentRow++; // Blank row
                currentRow = addMetadataSection(sheet, metadata, currentRow,
                        metadataKeyStyle, metadataValueStyle);
                currentRow++; // Blank row after metadata
            }

            // Add headers
            Row headerRow = sheet.createRow(currentRow++);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Freeze header row
            sheet.createFreezePane(0, currentRow);

            // Add data rows
            for (List<Object> dataRow : data) {
                Row row = sheet.createRow(currentRow++);
                for (int i = 0; i < dataRow.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = dataRow.get(i);
                    setCellValue(cell, value);
                    cell.setCellStyle(dataStyle);
                }
            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // Add a bit of extra padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1024);
            }

            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();

            log.info("Excel report generated successfully: {} rows, {} columns",
                    data.size(), headers.size());

            return result;

        } catch (IOException e) {
            log.error("Failed to generate Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("فشل في إنشاء تقرير Excel", e);
        }
    }

    /**
     * Add metadata section to the Excel sheet.
     */
    private int addMetadataSection(Sheet sheet, Map<String, Object> metadata, int startRow,
                                  CellStyle keyStyle, CellStyle valueStyle) {
        int currentRow = startRow;

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            Row row = sheet.createRow(currentRow++);

            Cell keyCell = row.createCell(0);
            keyCell.setCellValue(entry.getKey() + ":");
            keyCell.setCellStyle(keyStyle);

            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(toString(entry.getValue()));
            valueCell.setCellStyle(valueStyle);
        }

        return currentRow;
    }

    /**
     * Set cell value based on object type.
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(toString(value));
        }
    }

    /**
     * Create title cell style (bold, large font, centered).
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create header cell style (bold, white text on blue background, borders).
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * Create data cell style (thin borders, left-aligned).
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * Create metadata key style (bold, gray background).
     */
    private CellStyle createMetadataKeyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * Create metadata value style (regular text).
     */
    private CellStyle createMetadataValueStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }
}
