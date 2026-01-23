package com.techno.backend.controller;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.service.ExcelReportService;
import com.techno.backend.service.PdfReportService;
import com.techno.backend.service.WarehouseReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Warehouse Reports.
 *
 * Endpoints:
 * - POST /api/reports/warehouse/stock-levels - Current Stock Levels
 * - POST /api/reports/warehouse/stock-movement - Stock Movement
 * - POST /api/reports/warehouse/purchase-orders - Purchase Orders
 * - POST /api/reports/warehouse/low-stock-alert - Low Stock Alert
 *
 * All endpoints support both PDF and Excel formats via the 'format' parameter.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@RestController
@RequestMapping("/reports/warehouse")
@Slf4j
public class WarehouseReportController extends BaseReportController {

    private final WarehouseReportService warehouseReportService;

    public WarehouseReportController(ExcelReportService excelReportService,
                                    PdfReportService pdfReportService,
                                    WarehouseReportService warehouseReportService) {
        super(excelReportService, pdfReportService);
        this.warehouseReportService = warehouseReportService;
    }

    /**
     * Generate Current Stock Levels Report.
     *
     * Shows all items with their current balance in warehouses.
     *
     * @param request Report request with optional projectCode, storeCode, categoryCode filters
     * @return PDF or Excel file for download
     */
    @PostMapping("/stock-levels")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateStockLevels(@RequestBody ReportRequest request) {
        log.info("Request for Current Stock Levels Report: projectCode={}, format={}",
                request.getProjectCode(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = warehouseReportService.generateCurrentStockLevels(request);

        String filename = buildFilename("مستويات_المخزون_الحالية",
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Stock Movement Report.
     *
     * Shows all receipts, issues, and transfers with running balance.
     *
     * @param request Report request with date range, optional projectCode, storeCode, itemCode
     * @return PDF or Excel file for download
     */
    @PostMapping("/stock-movement")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> generateStockMovement(@RequestBody ReportRequest request) {
        log.info("Request for Stock Movement Report: {} to {}, format={}",
                request.getStartDate(), request.getEndDate(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = warehouseReportService.generateStockMovement(request);

        String filename = buildFilename("حركة_المخزون_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Purchase Orders Report.
     *
     * Shows all purchase orders with status.
     *
     * @param request Report request with date range and optional status filter
     * @return PDF or Excel file for download
     */
    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER', 'FINANCE_MANAGER')")
    public ResponseEntity<byte[]> generatePurchaseOrders(@RequestBody ReportRequest request) {
        log.info("Request for Purchase Orders Report: {} to {}, status={}, format={}",
                request.getStartDate(), request.getEndDate(), request.getStatus(), request.getFormat());

        validateRequest(request);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("تاريخ البداية وتاريخ النهاية مطلوبان");
        }

        byte[] reportContent = warehouseReportService.generatePurchaseOrders(request);

        String filename = buildFilename("أوامر_الشراء_" +
                request.getStartDate() + "_إلى_" + request.getEndDate(),
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Generate Low Stock Alert Report.
     *
     * Shows items below minimum/reorder level.
     *
     * @param request Report request with optional projectCode, storeCode filters
     * @return PDF or Excel file for download
     */
    @PostMapping("/low-stock-alert")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER', 'PROCUREMENT')")
    public ResponseEntity<byte[]> generateLowStockAlert(@RequestBody ReportRequest request) {
        log.info("Request for Low Stock Alert Report: projectCode={}, format={}",
                request.getProjectCode(), request.getFormat());

        validateRequest(request);

        byte[] reportContent = warehouseReportService.generateLowStockAlert(request);

        String filename = buildFilename("تنبيه_المخزون_المنخفض",
                request.getNormalizedFormat());

        return buildResponse(reportContent, filename,
                getMimeType(request.getNormalizedFormat()));
    }

    /**
     * Build filename with timestamp and extension.
     */
    private String buildFilename(String baseName, String format) {
        String extension = "EXCEL".equalsIgnoreCase(format) ? "xlsx" : "pdf";
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format("%s_%s.%s", sanitizedName, timestamp, extension);
    }

    /**
     * Build HTTP response with file download headers.
     */
    private ResponseEntity<byte[]> buildResponse(byte[] content, String filename, String mimeType) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(mimeType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    /**
     * Get MIME type based on format.
     */
    private String getMimeType(String format) {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/pdf";
        }
    }
}

