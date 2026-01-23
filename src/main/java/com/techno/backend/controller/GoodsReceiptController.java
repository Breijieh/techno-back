package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.GoodsReceiptRequest;
import com.techno.backend.dto.warehouse.GoodsReceiptResponse;
import com.techno.backend.service.GoodsReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/receiving")
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> createGoodsReceipt(
            @Valid @RequestBody GoodsReceiptRequest request) {
        log.info("REST request to create goods receipt for store: {}", request.getStoreCode());

        GoodsReceiptResponse response = goodsReceiptService.createGoodsReceipt(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء إيصال البضائع بنجاح", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<GoodsReceiptResponse>>> getAllGoodsReceipts(
            @RequestParam(required = false) Long projectCode,
            @RequestParam(required = false) Long storeCode) {
        log.info("REST request to get all goods receipts - projectCode: {}, storeCode: {}", projectCode, storeCode);

        List<GoodsReceiptResponse> receipts = goodsReceiptService.getAllGoodsReceipts();

        // Filter by projectCode if provided
        if (projectCode != null) {
            receipts = receipts.stream()
                    .filter(r -> r.getProjectCode() != null && r.getProjectCode().equals(projectCode))
                    .toList();
        }

        // Filter by storeCode if provided
        if (storeCode != null) {
            receipts = receipts.stream()
                    .filter(r -> r.getStoreCode().equals(storeCode))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(receipts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> getGoodsReceiptById(@PathVariable Long id) {
        log.info("REST request to get goods receipt: {}", id);

        GoodsReceiptResponse response = goodsReceiptService.getGoodsReceiptById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> updateGoodsReceipt(
            @PathVariable Long id,
            @Valid @RequestBody GoodsReceiptRequest request) {
        log.info("REST request to update goods receipt: {}", id);

        GoodsReceiptResponse response = goodsReceiptService.updateGoodsReceipt(id, request);

        return ResponseEntity.ok(ApiResponse.success("تم تحديث إيصال البضائع بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGoodsReceipt(@PathVariable Long id) {
        log.info("REST request to delete goods receipt: {}", id);

        goodsReceiptService.deleteGoodsReceipt(id);

        return ResponseEntity.ok(ApiResponse.success("تم حذف إيصال البضائع بنجاح", null));
    }
}

