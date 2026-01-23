package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.StoreTransferRequest;
import com.techno.backend.dto.warehouse.StoreTransferResponse;
import com.techno.backend.service.StoreTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/transfers")
@RequiredArgsConstructor
@Slf4j
public class StoreTransferController {

    private final StoreTransferService storeTransferService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreTransferResponse>> createStoreTransfer(
            @Valid @RequestBody StoreTransferRequest request) {
        log.info("REST request to create store transfer from store: {} to store: {}", 
                request.getFromStoreCode(), request.getToStoreCode());

        StoreTransferResponse response = storeTransferService.createTransfer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء نقل المتجر بنجاح", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<StoreTransferResponse>>> getAllStoreTransfers(
            @RequestParam(required = false) Long fromStoreCode,
            @RequestParam(required = false) Long toStoreCode,
            @RequestParam(required = false) String status) {
        log.info("REST request to get all store transfers - fromStoreCode: {}, toStoreCode: {}, status: {}", 
                fromStoreCode, toStoreCode, status);

        List<StoreTransferResponse> transfers = storeTransferService.getAllStoreTransfers();

        // Filter by fromStoreCode if provided
        if (fromStoreCode != null) {
            transfers = transfers.stream()
                    .filter(t -> t.getFromStoreCode().equals(fromStoreCode))
                    .toList();
        }

        // Filter by toStoreCode if provided
        if (toStoreCode != null) {
            transfers = transfers.stream()
                    .filter(t -> t.getToStoreCode().equals(toStoreCode))
                    .toList();
        }

        // Filter by status if provided
        if (status != null) {
            transfers = transfers.stream()
                    .filter(t -> t.getTransferStatus().equals(status))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(transfers));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StoreTransferResponse>> getStoreTransferById(@PathVariable Long id) {
        log.info("REST request to get store transfer: {}", id);

        StoreTransferResponse response = storeTransferService.getStoreTransferById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreTransferResponse>> updateStoreTransfer(
            @PathVariable Long id,
            @Valid @RequestBody StoreTransferRequest request) {
        log.info("REST request to update store transfer: {}", id);

        StoreTransferResponse response = storeTransferService.updateStoreTransfer(id, request);

        return ResponseEntity.ok(ApiResponse.success("تم تحديث نقل المتجر بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStoreTransfer(@PathVariable Long id) {
        log.info("REST request to delete store transfer: {}", id);

        storeTransferService.deleteStoreTransfer(id);

        return ResponseEntity.ok(ApiResponse.success("تم حذف نقل المتجر بنجاح", null));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreTransferResponse>> completeStoreTransfer(@PathVariable Long id) {
        log.info("REST request to complete store transfer: {}", id);

        StoreTransferResponse response = storeTransferService.completeTransfer(id);

        return ResponseEntity.ok(ApiResponse.success("تم إكمال نقل المتجر بنجاح", response));
    }
}

