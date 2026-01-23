package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.PurchaseOrderRequest;
import com.techno.backend.dto.warehouse.PurchaseOrderResponse;
import com.techno.backend.dto.warehouse.PurchaseOrderApprovalRequest;
import com.techno.backend.dto.warehouse.PurchaseOrderRejectionRequest;
import com.techno.backend.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/purchase-orders")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {
        log.info("REST request to create purchase order for store: {}", request.getStoreCode());

        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء أمر الشراء بنجاح", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getAllPurchaseOrders(
            @RequestParam(required = false) Long projectCode,
            @RequestParam(required = false) Long storeCode,
            @RequestParam(required = false) String status) {
        log.info("REST request to get all purchase orders - projectCode: {}, storeCode: {}, status: {}", 
                projectCode, storeCode, status);

        List<PurchaseOrderResponse> orders;
        if (storeCode != null) {
            orders = purchaseOrderService.getPurchaseOrdersByStore(storeCode);
        } else if (status != null) {
            orders = purchaseOrderService.getPurchaseOrdersByStatus(status);
        } else {
            orders = purchaseOrderService.getAllPurchaseOrders();
        }

        // Filter by projectCode if provided
        if (projectCode != null) {
            orders = orders.stream()
                    .filter(po -> po.getProjectCode() != null && po.getProjectCode().equals(projectCode))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable Long id) {
        log.info("REST request to get purchase order: {}", id);

        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        log.info("REST request to update purchase order: {}", id);

        PurchaseOrderResponse response = purchaseOrderService.updatePurchaseOrder(id, request);

        return ResponseEntity.ok(ApiResponse.success("تم تحديث أمر الشراء بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable Long id) {
        log.info("REST request to delete purchase order: {}", id);

        purchaseOrderService.deletePurchaseOrder(id);

        return ResponseEntity.ok(ApiResponse.success("تم حذف أمر الشراء بنجاح", null));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> submitForApproval(@PathVariable Long id) {
        log.info("REST request to submit purchase order for approval: {}", id);

        PurchaseOrderResponse response = purchaseOrderService.submitForApproval(id);

        return ResponseEntity.ok(ApiResponse.success("تم إرسال أمر الشراء للموافقة", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approvePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PurchaseOrderApprovalRequest request) {
        log.info("REST request to approve purchase order: {}", id);

        String notes = request != null ? request.getNotes() : null;
        PurchaseOrderResponse response = purchaseOrderService.approvePurchaseOrder(id, notes);

        return ResponseEntity.ok(ApiResponse.success("تم اعتماد أمر الشراء بنجاح", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> rejectPurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRejectionRequest request) {
        log.info("REST request to reject purchase order: {}", id);

        PurchaseOrderResponse response = purchaseOrderService.rejectPurchaseOrder(id, request.getNotes());

        return ResponseEntity.ok(ApiResponse.success("تم رفض أمر الشراء", response));
    }
}

