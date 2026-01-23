package com.techno.backend.service;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating warehouse-related reports.
 *
 * Implements 4 warehouse reports:
 * 1. Current Stock Levels - All items with balance, highlights low stock
 * 2. Stock Movement - All receipts, issues, transfers with running balance
 * 3. Purchase Orders - All POs with status
 * 4. Low Stock Alert - Items below minimum/reorder level
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseReportService {

    private final StoreBalanceRepository balanceRepository;
    private final StoreTransactionRepository transactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectStoreRepository storeRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    /**
     * Generate Current Stock Levels Report.
     *
     * Shows all items with their current balance in warehouses.
     * Highlights items below reorder level.
     *
     * @param request Report request with optional projectCode, storeCode, categoryCode filters
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateCurrentStockLevels(ReportRequest request) {
        log.info("Generating Current Stock Levels Report");

        List<StoreBalance> balances;
        
        if (request.getProjectCode() != null) {
            // Filter by project (store belongs to project)
            List<ProjectStore> stores = storeRepository.findByProjectCode(request.getProjectCode());
            List<Long> storeCodes = stores.stream()
                    .map(ProjectStore::getStoreCode)
                    .collect(Collectors.toList());
            balances = balanceRepository.findAllWithStock().stream()
                    .filter(b -> !b.getIsDeleted())
                    .filter(b -> storeCodes.contains(b.getStore().getStoreCode()))
                    .collect(Collectors.toList());
        } else if (request.getAdditionalFilters() != null && 
                   request.getAdditionalFilters().containsKey("storeCode")) {
            Long storeCode = ((Number) request.getAdditionalFilters().get("storeCode")).longValue();
            balances = balanceRepository.findByStoreCode(storeCode);
        } else {
            balances = balanceRepository.findAllWithStock();
        }

        // Filter by category if provided
        if (request.getAdditionalFilters() != null && 
            request.getAdditionalFilters().containsKey("categoryCode")) {
            Long categoryCode = ((Number) request.getAdditionalFilters().get("categoryCode")).longValue();
            balances = balances.stream()
                    .filter(b -> b.getItem().getCategory().getCategoryCode().equals(categoryCode))
                    .collect(Collectors.toList());
        }

        String title = "ØªÙ‚Ø±ÙŠØ± Ù…Ø³ØªÙˆÙŠØ§Øª Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø§Ù„Ø­Ø§Ù„ÙŠØ©";
        List<String> headers = Arrays.asList(
                "Ø±Ù…Ø² Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø§Ø³Ù… Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø±Ù…Ø² Ø§Ù„ØµÙ†Ù",
                "Ø§Ø³Ù… Ø§Ù„ØµÙ†Ù",
                "Ø§Ù„ÙØ¦Ø©",
                "Ø§Ù„ÙˆØ­Ø¯Ø©",
                "Ø§Ù„ÙƒÙ…ÙŠØ© Ø§Ù„Ù…ØªÙˆÙØ±Ø©",
                "Ø§Ù„Ù…Ø­Ø¬ÙˆØ²Ø©",
                "Ø§Ù„Ù…ØªØ§Ø­Ø©",
                "Ù…Ø³ØªÙˆÙ‰ Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ø·Ù„Ø¨",
                "Status"
        );

        List<List<Object>> data = new ArrayList<>();
        int lowStockCount = 0;

        for (StoreBalance balance : balances) {
            StoreItem item = balance.getItem();
            ProjectStore store = balance.getStore();
            BigDecimal reorderLevel = item.getReorderLevel();
            boolean isLowStock = reorderLevel != null && 
                               balance.getQuantityOnHand().compareTo(reorderLevel) <= 0;

            if (isLowStock) {
                lowStockCount++;
            }

            data.add(Arrays.asList(
                    store.getStoreCode(),
                    store.getStoreName(),
                    item.getItemCode(),
                    item.getItemName(),
                    item.getCategory().getCategoryName(),
                    item.getUnitOfMeasure(),
                    balance.getQuantityOnHand(),
                    balance.getQuantityReserved(),
                    balance.getAvailableQuantity(),
                    reorderLevel != null ? reorderLevel : "ØºÙŠØ± Ù…ØªØ§Ø­",
                    isLowStock ? "LOW STOCK" : "OK"
            ));
        }

        // Sort by store, then item name
        data.sort((a, b) -> {
            int storeCompare = ((Long) a.get(0)).compareTo((Long) b.get(0));
            if (storeCompare != 0) return storeCompare;
            return ((String) a.get(3)).compareTo((String) b.get(3));
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø£ØµÙ†Ø§Ù", balances.size());
        metadata.put("Ø£ØµÙ†Ø§Ù Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø§Ù„Ù…Ù†Ø®ÙØ¶", lowStockCount);
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Stock Movement Report.
     *
     * Shows all receipts, issues, and transfers with running balance.
     *
     * @param request Report request with date range, optional projectCode, storeCode, itemCode
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateStockMovement(ReportRequest request) {
        log.info("Generating Stock Movement Report");

        validateDateRange(request);

        List<StoreTransaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> !t.getIsDeleted())
                .filter(t -> {
                    LocalDate transDate = t.getTransactionDate().toLocalDate();
                    return !transDate.isBefore(request.getStartDate()) && 
                           !transDate.isAfter(request.getEndDate());
                })
                .collect(Collectors.toList());

        // Filter by project/store if provided
        if (request.getProjectCode() != null) {
            List<ProjectStore> stores = storeRepository.findByProjectCode(request.getProjectCode());
            List<Long> storeCodes = stores.stream()
                    .map(ProjectStore::getStoreCode)
                    .collect(Collectors.toList());
            transactions = transactions.stream()
                    .filter(t -> storeCodes.contains(t.getStore().getStoreCode()))
                    .collect(Collectors.toList());
        } else if (request.getAdditionalFilters() != null && 
                   request.getAdditionalFilters().containsKey("storeCode")) {
            Long storeCode = ((Number) request.getAdditionalFilters().get("storeCode")).longValue();
            transactions = transactions.stream()
                    .filter(t -> t.getStore().getStoreCode().equals(storeCode))
                    .collect(Collectors.toList());
        }

        // Filter by item if provided
        if (request.getAdditionalFilters() != null && 
            request.getAdditionalFilters().containsKey("itemCode")) {
            Long itemCode = ((Number) request.getAdditionalFilters().get("itemCode")).longValue();
            transactions = transactions.stream()
                    .filter(t -> t.getItem().getItemCode().equals(itemCode))
                    .collect(Collectors.toList());
        }

        // Sort by date, then store, then item
        transactions.sort((a, b) -> {
            int dateCompare = a.getTransactionDate().compareTo(b.getTransactionDate());
            if (dateCompare != 0) return dateCompare;
            int storeCompare = a.getStore().getStoreCode().compareTo(b.getStore().getStoreCode());
            if (storeCompare != 0) return storeCompare;
            return a.getItem().getItemCode().compareTo(b.getItem().getItemCode());
        });

        String title = "ØªÙ‚Ø±ÙŠØ± Ø­Ø±ÙƒØ© Ø§Ù„Ù…Ø®Ø²ÙˆÙ†";
        List<String> headers = Arrays.asList(
                "Ø§Ù„ØªØ§Ø±ÙŠØ®",
                "Ø§Ù„ÙˆÙ‚Øª",
                "Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø§Ù„ØµÙ†Ù",
                "Ø§Ù„Ù†ÙˆØ¹",
                "Ø§Ù„ÙƒÙ…ÙŠØ©",
                "Ø§Ù„Ø±ØµÙŠØ¯ Ø¨Ø¹Ø¯",
                "Ø§Ù„Ù…Ø±Ø¬Ø¹",
                "Ù…Ù„Ø§Ø­Ø¸Ø§Øª"
        );

        List<List<Object>> data = new ArrayList<>();

        for (StoreTransaction transaction : transactions) {
            data.add(Arrays.asList(
                    transaction.getTransactionDate().toLocalDate(),
                    transaction.getTransactionDate().toLocalTime(),
                    transaction.getStore().getStoreName(),
                    transaction.getItem().getItemName(),
                    transaction.getTransactionType(),
                    transaction.getQuantity(),
                    transaction.getBalanceAfter(),
                    transaction.getReferenceType() != null ? 
                        transaction.getReferenceType() + " #" + transaction.getReferenceId() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                    transaction.getNotes() != null ? transaction.getNotes() : ""
            ));
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø¹Ø§Ù…Ù„Ø§Øª", transactions.size());
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Purchase Orders Report.
     *
     * Shows all purchase orders with status.
     *
     * @param request Report request with date range and optional status filter
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generatePurchaseOrders(ReportRequest request) {
        log.info("Generating Purchase Orders Report");

        validateDateRange(request);

        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> !po.getIsDeleted())
                .filter(po -> {
                    LocalDate poDate = po.getPoDate();
                    return !poDate.isBefore(request.getStartDate()) && 
                           !poDate.isAfter(request.getEndDate());
                })
                .collect(Collectors.toList());

        // Filter by status if provided
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            purchaseOrders = purchaseOrders.stream()
                    .filter(po -> request.getStatus().equalsIgnoreCase(po.getPoStatus()))
                    .collect(Collectors.toList());
        }

        // Sort by date descending
        purchaseOrders.sort((a, b) -> b.getPoDate().compareTo(a.getPoDate()));

        String title = "ØªÙ‚Ø±ÙŠØ± Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡",
                "ØªØ§Ø±ÙŠØ® Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡",
                "Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø§Ù„Ù…ÙˆØ±Ø¯",
                "Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø¨Ù„Øº",
                "Ø§Ù„Ø­Ø§Ù„Ø©",
                "Ø¹Ø¯Ø¯ Ø§Ù„Ø£Ø³Ø·Ø±"
        );

        List<List<Object>> data = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrder po : purchaseOrders) {
            data.add(Arrays.asList(
                    po.getPoNumber(),
                    po.getPoDate(),
                    po.getStore().getStoreName(),
                    po.getSupplierName(),
                    po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO,
                    po.getPoStatus(),
                    po.getOrderLines() != null ? po.getOrderLines().size() : 0
            ));

            if (po.getTotalAmount() != null) {
                totalAmount = totalAmount.add(po.getTotalAmount());
            }
        }

        // Add totals row
        data.add(Arrays.asList(
                "",
                "",
                "TOTAL",
                "",
                totalAmount,
                "",
                purchaseOrders.size()
        ));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡", purchaseOrders.size());
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø¨Ù„Øº", totalAmount);
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", request.getStartDate() + " Ø¥Ù„Ù‰ " + request.getEndDate());
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Low Stock Alert Report.
     *
     * Shows items below minimum/reorder level.
     *
     * @param request Report request with optional projectCode, storeCode filters
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLowStockAlert(ReportRequest request) {
        log.info("Generating Low Stock Alert Report");

        List<StoreBalance> lowStockBalances;

        // Get items below reorder level
        if (request.getProjectCode() != null) {
            List<ProjectStore> stores = storeRepository.findByProjectCode(request.getProjectCode());
            List<Long> storeCodes = stores.stream()
                    .map(ProjectStore::getStoreCode)
                    .collect(Collectors.toList());
            lowStockBalances = balanceRepository.findAllWithStock().stream()
                    .filter(b -> !b.getIsDeleted())
                    .filter(b -> storeCodes.contains(b.getStore().getStoreCode()))
                    .filter(b -> {
                        StoreItem item = b.getItem();
                        return item.getReorderLevel() != null && 
                               b.getQuantityOnHand().compareTo(item.getReorderLevel()) <= 0;
                    })
                    .collect(Collectors.toList());
        } else if (request.getAdditionalFilters() != null && 
                   request.getAdditionalFilters().containsKey("storeCode")) {
            Long storeCode = ((Number) request.getAdditionalFilters().get("storeCode")).longValue();
            lowStockBalances = balanceRepository.findByStoreCode(storeCode).stream()
                    .filter(b -> {
                        StoreItem item = b.getItem();
                        return item.getReorderLevel() != null && 
                               b.getQuantityOnHand().compareTo(item.getReorderLevel()) <= 0;
                    })
                    .collect(Collectors.toList());
        } else {
            lowStockBalances = balanceRepository.findItemsBelowReorderLevel();
        }

        String title = "ØªÙ‚Ø±ÙŠØ± ØªÙ†Ø¨ÙŠÙ‡ Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø§Ù„Ù…Ù†Ø®ÙØ¶";
        List<String> headers = Arrays.asList(
                "Ø±Ù…Ø² Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø§Ø³Ù… Ø§Ù„Ù…Ø®Ø²Ù†",
                "Ø±Ù…Ø² Ø§Ù„ØµÙ†Ù",
                "Ø§Ø³Ù… Ø§Ù„ØµÙ†Ù",
                "Ø§Ù„ÙØ¦Ø©",
                "Ø§Ù„ÙˆØ­Ø¯Ø©",
                "Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø§Ù„Ø­Ø§Ù„ÙŠ",
                "Ù…Ø³ØªÙˆÙ‰ Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ø·Ù„Ø¨",
                "Ø§Ù„Ù†Ù‚Øµ",
                "Status"
        );

        List<List<Object>> data = new ArrayList<>();

        for (StoreBalance balance : lowStockBalances) {
            StoreItem item = balance.getItem();
            ProjectStore store = balance.getStore();
            BigDecimal reorderLevel = item.getReorderLevel();
            BigDecimal shortage = reorderLevel.subtract(balance.getQuantityOnHand());

            data.add(Arrays.asList(
                    store.getStoreCode(),
                    store.getStoreName(),
                    item.getItemCode(),
                    item.getItemName(),
                    item.getCategory().getCategoryName(),
                    item.getUnitOfMeasure(),
                    balance.getQuantityOnHand(),
                    reorderLevel,
                    shortage,
                    balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) == 0 ? "OUT OF STOCK" : "LOW STOCK"
            ));
        }

        // Sort by shortage (highest first)
        data.sort((a, b) -> {
            BigDecimal shortageA = (BigDecimal) a.get(8);
            BigDecimal shortageB = (BigDecimal) b.get(8);
            return shortageB.compareTo(shortageA);
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø£ØµÙ†Ø§Ù Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø§Ù„Ù…Ù†Ø®ÙØ¶", lowStockBalances.size());
        metadata.put("Ø£ØµÙ†Ø§Ù Ù†ÙØ¯ Ø§Ù„Ù…Ø®Ø²ÙˆÙ†", lowStockBalances.stream()
                .filter(b -> b.getQuantityOnHand().compareTo(BigDecimal.ZERO) == 0)
                .count());
        metadata.put("Generated On", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    // Helper methods

    private void validateDateRange(ReportRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ ÙˆØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡ Ù…Ø·Ù„ÙˆØ¨Ø§Ù†");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("ØªØ§Ø±ÙŠØ® Ø§Ù„Ø¨Ø¯Ø¡ Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø£Ù† ÙŠÙƒÙˆÙ† Ø¨Ø¹Ø¯ ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡");
        }
    }

    private byte[] generateReport(String title, List<String> headers, List<List<Object>> data,
                                  Map<String, Object> metadata, ReportRequest request) {
        if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
            return excelReportService.generateReport(title, headers, data, metadata);
        } else {
            return pdfReportService.generateReport(title, headers, data, metadata);
        }
    }
}


