package com.techno.backend.service;

import com.techno.backend.dto.warehouse.GoodsReceiptRequest;
import com.techno.backend.dto.warehouse.GoodsReceiptResponse;
import com.techno.backend.dto.warehouse.GoodsReceiptLineRequest;
import com.techno.backend.dto.warehouse.GoodsReceiptLineResponse;
import com.techno.backend.entity.*;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository receiptRepository;
    private final ProjectStoreRepository storeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StoreItemRepository itemRepository;
    private final StoreBalanceRepository balanceRepository;
    private final StoreTransactionRepository transactionRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * CRITICAL: Balance tracking algorithm - INCREASE balance
     * This method is ACID-compliant and prevents data corruption
     */
    @Transactional
    public void updateStoreBalance(ProjectStore store, StoreItem item, BigDecimal quantity,
                                     String transactionType, Long referenceId) {
        log.info("Updating store balance: store={}, item={}, quantity={}, type={}",
                store.getStoreCode(), item.getItemCode(), quantity, transactionType);

        // Find or create balance
        StoreBalance balance = balanceRepository
                .findByStoreAndItem(store.getStoreCode(), item.getItemCode())
                .orElseGet(() -> {
                    log.info("Creating new balance record for store={}, item={}",
                            store.getStoreCode(), item.getItemCode());
                    StoreBalance newBalance = StoreBalance.builder()
                            .store(store)
                            .item(item)
                            .quantityOnHand(BigDecimal.ZERO)
                            .quantityReserved(BigDecimal.ZERO)
                            .build();
                    return balanceRepository.save(newBalance);
                });

        // Calculate new balance
        BigDecimal oldBalance = balance.getQuantityOnHand();
        BigDecimal newBalance = oldBalance.add(quantity);

        // CRITICAL: Prevent negative balances
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    String.format("Ø§Ù„Ø¹Ù…Ù„ÙŠØ© Ø³ØªØ¤Ø¯ÙŠ Ø¥Ù„Ù‰ Ø±ØµÙŠØ¯ Ø³Ø§Ù„Ø¨. Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                            oldBalance, quantity.abs()));
        }

        // Update balance
        balance.setQuantityOnHand(newBalance);
        balance.setLastTransactionDate(LocalDateTime.now());
        balanceRepository.save(balance);

        // Create transaction log (audit trail)
        StoreTransaction transaction = StoreTransaction.builder()
                .store(store)
                .item(item)
                .transactionType(transactionType)
                .transactionDate(LocalDateTime.now())
                .quantity(quantity)
                .referenceType("GOODS_RECEIPT")
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .build();
        transactionRepository.save(transaction);

        log.info("Balance updated successfully: oldBalance={}, newBalance={}, item={}",
                oldBalance, newBalance, item.getItemName());
    }

    @Transactional
    public GoodsReceiptResponse createGoodsReceipt(GoodsReceiptRequest request) {
        log.info("Creating goods receipt for store: {}", request.getStoreCode());

        // Validate store exists
        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Generate receipt number
        String receiptNumber = generateReceiptNumber();

        // Handle PO if provided
        PurchaseOrder po = null;
        if (request.getPurchaseOrderId() != null) {
            po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

            if (!"APPROVED".equals(po.getPoStatus())) {
                throw new BadRequestException("ÙŠØ¬Ø¨ Ø§Ø¹ØªÙ…Ø§Ø¯ Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ Ù‚Ø¨Ù„ Ø§Ù„Ø§Ø³ØªÙ„Ø§Ù…");
            }
        }

        // Get current user's employee number from security context
        Long receivedBy = getCurrentEmployeeNo();
        log.info("Creating goods receipt with receivedBy (employee number): {}", receivedBy);
        
        // Validate that the employee exists
        if (receivedBy != null && !employeeRepository.existsById(receivedBy)) {
            log.warn("Employee {} not found in database, but continuing with receipt creation", receivedBy);
        }

        // Build receipt entity
        GoodsReceipt receipt = GoodsReceipt.builder()
                .receiptNumber(receiptNumber)
                .store(store)
                .receiptDate(request.getReceiptDate())
                .receiptType(request.getReceiptType())
                .purchaseOrder(po)
                .notes(request.getNotes())
                .isDeleted(false)
                .build();

        // Set createdBy (which represents receivedBy - the person who received the goods)
        receipt.setCreatedBy(receivedBy);
        log.info("Set createdBy to: {} for goods receipt: {}", receipt.getCreatedBy(), receiptNumber);

        // Add receipt lines
        for (GoodsReceiptLineRequest lineRequest : request.getReceiptLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            GoodsReceiptLine line = GoodsReceiptLine.builder()
                    .goodsReceipt(receipt)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            receipt.addReceiptLine(line);
        }

        // Save receipt
        receipt = receiptRepository.save(receipt);

        // Process each line - UPDATE BALANCES
        for (GoodsReceiptLine line : receipt.getReceiptLines()) {
            if (line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† ÙƒÙ…ÙŠØ© Ø§Ù„Ø§Ø³ØªÙ„Ø§Ù… Ø£ÙƒØ¨Ø± Ù…Ù† Ø§Ù„ØµÙØ±");
            }

            StoreItem item = line.getItem();
            // CRITICAL: Update balance (RECEIPT = ADD quantity)
            updateStoreBalance(store, item, line.getQuantity(), "RECEIPT", receipt.getReceiptId());
        }

        // Update PO status if applicable
        if (receipt.getPurchaseOrder() != null) {
            PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
            purchaseOrder.setPoStatus("RECEIVED");
            purchaseOrderRepository.save(purchaseOrder);
        }

        log.info("Goods receipt created successfully with {} lines", receipt.getReceiptLines().size());
        return mapToResponse(receipt);
    }

    private String generateReceiptNumber() {
        // Generate receipt number in format: GR-YYYYMMDD-XXXX
        String prefix = "GR-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long maxId = receiptRepository.count();
        return prefix + String.format("%04d", maxId + 1);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getGoodsReceiptById(Long receiptId) {
        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø§Ø³ØªÙ„Ø§Ù… Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        return mapToResponse(receipt);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getAllGoodsReceipts() {
        List<GoodsReceipt> receipts = receiptRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .collect(Collectors.toList());
        return receipts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GoodsReceiptResponse updateGoodsReceipt(Long receiptId, GoodsReceiptRequest request) {
        log.info("Updating goods receipt: {}", receiptId);

        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø§Ø³ØªÙ„Ø§Ù… Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Note: Updating receipts may affect balances. For now, we'll allow updates
        // but in production, you might want to restrict this based on business rules

        // Validate store exists
        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Handle PO if provided
        PurchaseOrder po = null;
        if (request.getPurchaseOrderId() != null) {
            po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        }

        receipt.setStore(store);
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setReceiptType(request.getReceiptType());
        receipt.setPurchaseOrder(po);
        receipt.setNotes(request.getNotes());

        // Clear and update receipt lines
        receipt.getReceiptLines().clear();
        for (GoodsReceiptLineRequest lineRequest : request.getReceiptLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            GoodsReceiptLine line = GoodsReceiptLine.builder()
                    .goodsReceipt(receipt)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            receipt.addReceiptLine(line);
        }

        receipt = receiptRepository.save(receipt);
        log.info("Goods receipt updated successfully");
        return mapToResponse(receipt);
    }

    @Transactional
    public void deleteGoodsReceipt(Long receiptId) {
        log.info("Deleting goods receipt: {}", receiptId);

        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø§Ø³ØªÙ„Ø§Ù… Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        receipt.setIsDeleted(true);
        receiptRepository.save(receipt);
        log.info("Goods receipt deleted successfully");
    }

    private GoodsReceiptResponse mapToResponse(GoodsReceipt receipt) {
        GoodsReceiptResponse.GoodsReceiptResponseBuilder builder = GoodsReceiptResponse.builder()
                .receiptId(receipt.getReceiptId())
                .receiptNumber(receipt.getReceiptNumber())
                .receiptDate(receipt.getReceiptDate())
                .receiptType(receipt.getReceiptType())
                .notes(receipt.getNotes())
                .createdDate(receipt.getCreatedDate())
                .createdBy(receipt.getCreatedBy())
                .modifiedDate(receipt.getModifiedDate())
                .modifiedBy(receipt.getModifiedBy());

        if (receipt.getStore() != null) {
            builder.storeCode(receipt.getStore().getStoreCode())
                    .storeName(receipt.getStore().getStoreName())
                    .storeName(receipt.getStore().getStoreName())
                    .storeName(receipt.getStore().getStoreName());

            if (receipt.getStore().getProject() != null) {
                builder.projectCode(receipt.getStore().getProject().getProjectCode())
                        .projectName(receipt.getStore().getProject().getProjectName())
                        .projectName(receipt.getStore().getProject().getProjectName())
                        .projectName(receipt.getStore().getProject().getProjectName());
            }
        }

        if (receipt.getPurchaseOrder() != null) {
            builder.purchaseOrderId(receipt.getPurchaseOrder().getPoId())
                    .purchaseOrderNumber(receipt.getPurchaseOrder().getPoNumber());
        }

        if (receipt.getReceiptLines() != null && !receipt.getReceiptLines().isEmpty()) {
            List<GoodsReceiptLineResponse> lineResponses = receipt.getReceiptLines().stream()
                    .filter(line -> !Boolean.TRUE.equals(line.getIsDeleted()))
                    .map(this::mapLineToResponse)
                    .collect(Collectors.toList());
            builder.receiptLines(lineResponses);
        }

        // Map createdBy to receivedBy (the person who created the receipt is the one who received it)
        Long receivedBy = receipt.getCreatedBy();
        if (receivedBy != null) {
            builder.receivedBy(receivedBy);
            // Fetch employee name if available
            employeeRepository.findById(receivedBy)
                    .ifPresent(employee -> builder.receivedByName(employee.getEmployeeName()));
        }

        return builder.build();
    }

    /**
     * Get current employee number from security context
     * The JWT filter sets the principal to the employee number (Long) if available
     */
    private Long getCurrentEmployeeNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authentication found in security context");
            throw new RuntimeException("Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù… ØºÙŠØ± Ù…ØµØ§Ø¯Ù‚ Ø¹Ù„ÙŠÙ‡");
        }

        // The JWT filter sets the principal to the employee number (Long) if available
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            log.debug("Found employee number {} from security context", principal);
            return (Long) principal;
        }

        // Fallback: try to parse as Long if it's a String representation
        if (principal instanceof String) {
            try {
                Long employeeNo = Long.parseLong((String) principal);
                log.debug("Parsed employee number {} from string principal", employeeNo);
                return employeeNo;
            } catch (NumberFormatException e) {
                log.warn("Could not parse employee number from string principal: {}", principal);
            }
        }

        log.error("Could not extract employee number from authentication principal: {}", principal.getClass().getName());
        throw new RuntimeException("ØªØ¹Ø°Ø± Ø§Ø³ØªØ®Ø±Ø§Ø¬ Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù Ù…Ù† Ø§Ù„Ù…ØµØ§Ø¯Ù‚Ø©");
    }

    private GoodsReceiptLineResponse mapLineToResponse(GoodsReceiptLine line) {
        GoodsReceiptLineResponse.GoodsReceiptLineResponseBuilder builder = GoodsReceiptLineResponse.builder()
                .lineId(line.getLineId())
                .quantity(line.getQuantity())
                .notes(line.getNotes());

        if (line.getItem() != null) {
            builder.itemCode(line.getItem().getItemCode())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName());
        }

        // If receipt has PO, try to get unit price from PO line
        if (line.getGoodsReceipt().getPurchaseOrder() != null) {
            PurchaseOrder po = line.getGoodsReceipt().getPurchaseOrder();
            
            // Ensure orderLines are loaded (might be lazy)
            if (po.getOrderLines() == null || po.getOrderLines().isEmpty()) {
                // Fetch PO with orderLines if not loaded
                po = purchaseOrderRepository.findById(po.getPoId())
                        .orElse(null);
            }
            
            if (po != null && po.getOrderLines() != null) {
                po.getOrderLines().stream()
                        .filter(poLine -> poLine.getItem() != null && 
                                poLine.getItem().getItemCode().equals(line.getItem().getItemCode()))
                        .filter(poLine -> !Boolean.TRUE.equals(poLine.getIsDeleted()))
                        .findFirst()
                        .ifPresent(poLine -> {
                            builder.unitPrice(poLine.getUnitPrice());
                            if (poLine.getUnitPrice() != null && line.getQuantity() != null) {
                                builder.lineTotal(line.getQuantity().multiply(poLine.getUnitPrice()));
                            }
                        });
            }
        }

        return builder.build();
    }
}

