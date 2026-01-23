package com.techno.backend.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for generating PDF reports using iText 7.
 *
 * Features:
 * - Professional PDF formatting with styled tables
 * - Configurable page size (A4 landscape by default for reports)
 * - Metadata section for report filters and parameters
 * - Colored headers and alternating row backgrounds
 * - Auto-sizing columns based on content
 * - Footer with generation timestamp
 *
 * Usage:
 * ```java
 * PdfReportService pdfService = new PdfReportService();
 * byte[] pdfFile = pdfService.generateReport(
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
public class PdfReportService extends BaseReportService {

    private static final String FILE_EXTENSION = "pdf";
    private static final String MIME_TYPE = "application/pdf";

    // Colors
    private static final DeviceRgb HEADER_BG_COLOR = new DeviceRgb(0, 51, 102); // Dark blue
    private static final DeviceRgb ALT_ROW_BG_COLOR = new DeviceRgb(240, 240, 240); // Light gray
    private static final DeviceRgb METADATA_BG_COLOR = new DeviceRgb(230, 230, 230); // Gray

    /**
     * Generate PDF report with professional formatting.
     *
     * @param title Report title
     * @param headers Column headers
     * @param data Report data rows
     * @param metadata Report metadata (filters, date range, etc.)
     * @return Byte array of PDF file
     */
    @Override
    public byte[] generateReport(String title, List<String> headers,
                                 List<List<Object>> data, Map<String, Object> metadata) {
        log.info("Generating PDF report: {}", title);

        // Validate parameters
        validateReportParameters(title, headers, data);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create PDF writer and document
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);

            // Use landscape A4 for better table fit
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());

            Document document = new Document(pdfDoc);

            // Add title
            Paragraph titlePara = new Paragraph(title)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(titlePara);

            // Add metadata section if provided
            if (metadata != null && !metadata.isEmpty()) {
                Table metadataTable = createMetadataTable(metadata);
                document.add(metadataTable);
                document.add(new Paragraph().setMarginBottom(15)); // Spacing
            }

            // Create data table
            Table dataTable = createDataTable(headers, data);
            document.add(dataTable);

            // Add footer with generation timestamp
            Paragraph footer = new Paragraph("Generated on: " + formatDateTime(LocalDateTime.now()))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20);
            document.add(footer);

            // Close document
            document.close();

            byte[] result = outputStream.toByteArray();

            log.info("PDF report generated successfully: {} rows, {} columns, {} bytes",
                    data.size(), headers.size(), result.length);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("فشل في إنشاء تقرير PDF", e);
        }
    }

    /**
     * Create metadata table showing report filters/parameters.
     */
    private Table createMetadataTable(Map<String, Object> metadata) {
        // 2-column table for metadata (key-value pairs)
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(60))
                .setHorizontalAlignment(HorizontalAlignment.LEFT)
                .setMarginBottom(10);

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            // Key cell
            Cell keyCell = new Cell()
                    .add(new Paragraph(entry.getKey() + ":").setBold())
                    .setBackgroundColor(METADATA_BG_COLOR)
                    .setPadding(5)
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));
            table.addCell(keyCell);

            // Value cell
            Cell valueCell = new Cell()
                    .add(new Paragraph(toString(entry.getValue())))
                    .setPadding(5)
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));
            table.addCell(valueCell);
        }

        return table;
    }

    /**
     * Create main data table with headers and rows.
     */
    private Table createDataTable(List<String> headers, List<List<Object>> data) {
        int columnCount = headers.size();

        // Create table with equal column widths
        Table table = new Table(UnitValue.createPercentArray(columnCount))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(10);

        // Add header row
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8)
                    .setBorder(new SolidBorder(ColorConstants.WHITE, 1));
            table.addHeaderCell(headerCell);
        }

        // Add data rows with alternating background
        for (int i = 0; i < data.size(); i++) {
            List<Object> row = data.get(i);
            boolean isAlternateRow = (i % 2 == 1);

            for (Object value : row) {
                Cell dataCell = new Cell()
                        .add(new Paragraph(toString(value)))
                        .setPadding(5)
                        .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));

                if (isAlternateRow) {
                    dataCell.setBackgroundColor(ALT_ROW_BG_COLOR);
                }

                table.addCell(dataCell);
            }
        }

        // Add empty row message if no data
        if (data.isEmpty()) {
            Cell emptyCell = new Cell(1, columnCount)
                    .add(new Paragraph("No data available").setItalic())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
            table.addCell(emptyCell);
        }

        return table;
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
