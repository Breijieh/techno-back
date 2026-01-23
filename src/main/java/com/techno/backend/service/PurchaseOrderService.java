package com.techno.backend.service;

import com.techno.backend.dto.warehouse.PurchaseOrderRequest;
import com.techno.backend.dto.warehouse.PurchaseOrderResponse;
import com.techno.backend.dto.warehouse.PurchaseOrderLineRequest;
import com.techno.backend.dto.warehouse.PurchaseOrderLineResponse;
import com.techno.backend.entity.PurchaseOrder;
import com.techno.backend.entity.PurchaseOrderLine;
import com.techno.backend.entity.ProjectStore;
import com.techno.backend.entity.StoreItem;
import com.techno.backend.entity.Employee;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.PurchaseOrderRepository;
import com.techno.backend.repository.ProjectStoreRepository;
import com.techno.backend.repository.StoreItemRepository;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectStoreRepository storeRepository;
    private final StoreItemRepository itemRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        log.info("Creating purchase order for store: {}", request.getStoreCode());

        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Generate PO number
        String poNumber = generatePONumber();

        // Get current user's employee number from security context
        Long requestedBy = getCurrentEmployeeNo();
        log.info("Creating purchase order with requestedBy (employee number): {}", requestedBy);
        
        // Validate that the employee exists
        if (requestedBy != null && !employeeRepository.existsById(requestedBy)) {
            log.warn("Employee {} not found in database, but continuing with purchase order creation", requestedBy);
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .store(store)
                .poDate(request.getPoDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .supplierName(request.getSupplierName())
                .poStatus("DRAFT")
                .approvalNotes(request.getApprovalNotes())
                .isDeleted(false)
                .build();

        // Set createdBy (which represents requestedBy)
        po.setCreatedBy(requestedBy);
        log.info("Set createdBy to: {} for purchase order: {}", po.getCreatedBy(), poNumber);

        // Add order lines
        for (PurchaseOrderLineRequest lineRequest : request.getOrderLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .purchaseOrder(po)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .unitPrice(lineRequest.getUnitPrice())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            po.addOrderLine(line);
        }

        po.calculateTotalAmount();
        po = purchaseOrderRepository.save(po);

        log.info("Purchase order created successfully: {}", po.getPoNumber());
        return mapToResponse(po);
    }

    private String generatePONumber() {
        // Generate PO number in format: PO-YYYYMMDD-XXXX
        String prefix = "PO-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long maxId = purchaseOrderRepository.count();
        return prefix + String.format("%04d", maxId + 1);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(Long poId, PurchaseOrderRequest request) {
        log.info("Updating purchase order: {}", poId);
        log.info("Update request - supplierName: {}, storeCode: {}, poDate: {}, orderLines count: {}", 
                request.getSupplierName(), request.getStoreCode(), request.getPoDate(), 
                request.getOrderLines() != null ? request.getOrderLines().size() : 0);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        log.info("Current PO state - supplierName: {}, status: {}", po.getSupplierName(), po.getPoStatus());

        if (!"DRAFT".equals(po.getPoStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ù…Ø³ÙˆØ¯Ø© ÙÙ‚Ø·");
        }

        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        po.setStore(store);
        po.setPoDate(request.getPoDate());
        po.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        po.setSupplierName(request.getSupplierName());
        po.setApprovalNotes(request.getApprovalNotes());

        log.info("After setting fields - supplierName: {}", po.getSupplierName());

        // Clear and update order lines
        po.getOrderLines().clear();
        for (PurchaseOrderLineRequest lineRequest : request.getOrderLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .purchaseOrder(po)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .unitPrice(lineRequest.getUnitPrice())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            po.addOrderLine(line);
        }

        po.calculateTotalAmount();
        log.info("Before save - supplierName: {}, totalAmount: {}, orderLines count: {}", 
                po.getSupplierName(), po.getTotalAmount(), po.getOrderLines().size());
        
        // Save the entity
        po = purchaseOrderRepository.save(po);
        
        // Verify the saved entity has the correct supplier name
        log.info("After save - supplierName: {}, poId: {}", po.getSupplierName(), po.getPoId());
        
        // Refresh from database to ensure we have the latest state
        purchaseOrderRepository.flush();
        PurchaseOrder refreshed = purchaseOrderRepository.findById(po.getPoId())
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø¹Ø¯ Ø§Ù„Ø­ÙØ¸"));
        log.info("After refresh - supplierName: {}", refreshed.getSupplierName());
        
        PurchaseOrderResponse response = mapToResponse(refreshed);
        log.info("Response mapped - supplierName: {}", response.getSupplierName());
        log.info("Purchase order updated successfully");
        return response;
    }

    @Transactional
    public PurchaseOrderResponse submitForApproval(Long poId) {
        log.info("Submitting purchase order for approval: {}", poId);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        if (!"DRAFT".equals(po.getPoStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ø¥Ø±Ø³Ø§Ù„ Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ù…Ø³ÙˆØ¯Ø© ÙÙ‚Ø·");
        }

        po.setPoStatus("PENDING_APPROVAL");
        po = purchaseOrderRepository.save(po);
        log.info("Purchase order submitted for approval");
        return mapToResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse approvePurchaseOrder(Long poId, String notes) {
        log.info("Approving purchase order: {}", poId);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        if (!"PENDING_APPROVAL".equals(po.getPoStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ø§Ø¹ØªÙ…Ø§Ø¯ Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© ÙÙ‚Ø·");
        }

        po.setPoStatus("APPROVED");
        po.setApprovalNotes(notes);
        po = purchaseOrderRepository.save(po);
        log.info("Purchase order approved");
        return mapToResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse rejectPurchaseOrder(Long poId, String notes) {
        log.info("Rejecting purchase order: {}", poId);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        if (!"PENDING_APPROVAL".equals(po.getPoStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ø±ÙØ¶ Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù†ØªØ¸Ø§Ø± Ø§Ù„Ù…ÙˆØ§ÙÙ‚Ø© ÙÙ‚Ø·");
        }

        po.setPoStatus("REJECTED");
        po.setApprovalNotes(notes);
        po = purchaseOrderRepository.save(po);
        log.info("Purchase order rejected");
        return mapToResponse(po);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        return mapToResponse(po);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll().stream()
                .filter(po -> !Boolean.TRUE.equals(po.getIsDeleted()))
                .collect(Collectors.toList());
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePurchaseOrder(Long poId) {
        log.info("Deleting purchase order: {}", poId);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø£Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Only allow deletion of DRAFT or REJECTED purchase orders
        // Once approved or received, deletion should be restricted to maintain audit trail
        if (!"DRAFT".equals(po.getPoStatus()) && !"REJECTED".equals(po.getPoStatus())) {
            throw new BadRequestException("ÙŠÙ…ÙƒÙ† Ø­Ø°Ù Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ù…Ø³ÙˆØ¯Ø© Ø£Ùˆ Ø§Ù„Ù…Ø±ÙÙˆØ¶Ø© ÙÙ‚Ø·");
        }

        po.setIsDeleted(true);
        purchaseOrderRepository.save(po);
        log.info("Purchase order deleted successfully");
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrdersByStore(Long storeCode) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findByStoreCode(storeCode);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrdersByStatus(String status) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        // Get requestedBy from createdBy (they represent the same person)
        Long requestedBy = po.getCreatedBy();
        log.debug("Mapping purchase order {} - requestedBy (createdBy): {}", po.getPoNumber(), requestedBy);
        
        // Fetch employee name if requestedBy exists
        String requestedByName = null;
        if (requestedBy != null) {
            requestedByName = employeeRepository.findById(requestedBy)
                    .map(Employee::getEmployeeName)
                    .orElse(null);
            log.debug("Found requestedByName for employee {}: {}", requestedBy, requestedByName);
        } else {
            log.warn("Purchase order {} has null createdBy, requestedBy will be null", po.getPoNumber());
        }

        PurchaseOrderResponse.PurchaseOrderResponseBuilder builder = PurchaseOrderResponse.builder()
                .poId(po.getPoId())
                .poNumber(po.getPoNumber())
                .poDate(po.getPoDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .supplierName(po.getSupplierName())
                .totalAmount(po.getTotalAmount())
                .poStatus(po.getPoStatus())
                .approvalNotes(po.getApprovalNotes())
                .requestedBy(requestedBy)
                .requestedByName(requestedByName)
                .createdDate(po.getCreatedDate())
                .createdBy(po.getCreatedBy())
                .modifiedDate(po.getModifiedDate())
                .modifiedBy(po.getModifiedBy());

        if (po.getStore() != null) {
            builder.storeCode(po.getStore().getStoreCode())
                    .storeName(po.getStore().getStoreName())
                    .storeName(po.getStore().getStoreName())
                    .storeName(po.getStore().getStoreName());

            if (po.getStore().getProject() != null) {
                builder.projectCode(po.getStore().getProject().getProjectCode())
                        .projectName(po.getStore().getProject().getProjectName())
                        .projectName(po.getStore().getProject().getProjectName())
                        .projectName(po.getStore().getProject().getProjectName());
            }
        }

        if (po.getOrderLines() != null && !po.getOrderLines().isEmpty()) {
            List<PurchaseOrderLineResponse> lineResponses = po.getOrderLines().stream()
                    .filter(line -> !Boolean.TRUE.equals(line.getIsDeleted()))
                    .map(this::mapLineToResponse)
                    .collect(Collectors.toList());
            builder.orderLines(lineResponses);
        }

        return builder.build();
    }

    /**
     * Get current authenticated user's employee number from security context.
     *
     * Extracts employee number from security context.
     * The JWT filter sets the principal to the employee number (Long) if available, otherwise username (String).
     *
     * @return Employee number from security context
     * @throws RuntimeException if authentication is missing or invalid
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
                log.error("Failed to parse employee number from principal: {}", principal);
                throw new RuntimeException("Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± ØµØ§Ù„Ø­ ÙÙŠ Ø³ÙŠØ§Ù‚ Ø§Ù„Ù…ØµØ§Ø¯Ù‚Ø©");
            }
        }

        // Also try authentication.getName() as fallback
        try {
            Long employeeNo = Long.parseLong(authentication.getName());
            log.debug("Parsed employee number {} from authentication name", employeeNo);
            return employeeNo;
        } catch (NumberFormatException e) {
            log.error("Failed to parse employee number from authentication name: {}", authentication.getName());
            throw new RuntimeException("Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± ØµØ§Ù„Ø­ ÙÙŠ Ø³ÙŠØ§Ù‚ Ø§Ù„Ù…ØµØ§Ø¯Ù‚Ø©. Ù†ÙˆØ¹ Ø§Ù„Ø±Ø¦ÙŠØ³ÙŠ: " + (principal != null ? principal.getClass().getName() : "null"));
        }
    }

    private PurchaseOrderLineResponse mapLineToResponse(PurchaseOrderLine line) {
        PurchaseOrderLineResponse.PurchaseOrderLineResponseBuilder builder = PurchaseOrderLineResponse.builder()
                .lineId(line.getLineId())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .lineTotal(line.getLineTotal())
                .notes(line.getNotes());

        if (line.getItem() != null) {
            builder.itemCode(line.getItem().getItemCode())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName());
        }

        return builder.build();
    }
}

